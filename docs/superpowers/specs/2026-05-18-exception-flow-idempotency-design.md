# T1 健身房管理系统 — 异常流与幂等处理设计

## 一、概述

在现有 `@RepeatSubmit`（防重复提交）、`@Version`（乐观锁）基础上，补充分布式锁、乐观锁冲突自动重试、核心API限流、移动端网络层增强，实现三端协同的异常流与幂等防护体系。

## 二、防护层次架构

```
客户端层                      API/服务层                     数据层
┌─────────────┐     ┌────────────────────────┐     ┌──────────┐
│ 管理端       │     │ ① @RateLimiter 限流    │     │ MySQL    │
│ request.ts   │────▶│ ② @RepeatSubmit 防重   │────▶│ @Version │
├─────────────┤     │ ③ Lock4j 分布式锁       │     │ 乐观锁   │
│ 会员小程序   │     │ ④ 乐观锁自动重试         │     └──────────┘
│ request.js  │────▶│ ⑤ 业务异常统一处理       │     ┌──────────┐
├─────────────┤     └────────────────────────┘     │ Redis    │
│ 教练App     │                                     │ 锁/限流  │
│ request.js  │────▶  (含移动端防重+重试)           │ 幂等键   │
└─────────────┘                                     └──────────┘
```

### 防护层次（由外到内）
| 层次 | 组件 | 职责 | 超时/降级 |
|------|------|------|-----------|
| L1 | `@RateLimiter` | 拦截流量突刺 | 返回 503 "访问过于频繁" |
| L2 | `@RepeatSubmit` | 防短时重复请求 | 返回 "不允许重复提交" |
| L3 | `Lock4j` | 关键路径串行化 | 超时返回 503 "业务处理中" |
| L4 | `@Version` + 自动重试 | 数据版本冲突兜底 | 重试3次后报错 |
| L5 | 客户端防重 | 前端拦截重复请求 | 本地提示不发送请求 |

## 三、后端详细设计

### 3.1 Lock4j 分布式锁

利用项目中已有的 Lock4j + Redisson，在关键写路径添加 `@Lock4j`。

#### 锁Key规范
```
{业务域}:{操作}:{业务ID}
```

#### 锁配置
| 参数 | 值 | 说明 |
|------|-----|------|
| acquireTimeout | 3000ms | 等待锁超时 |
| expireTimeout | 30000ms (默认) | 锁自动释放 |

#### 完整加锁清单

**预约流程**：
- `FitnessReservationServiceImpl.add()` → `reservation:create:{scheduleId}:{memberId}`
- `FitnessScheduleServiceImpl.updateReservedCount()` → `schedule:count:{id}`

**核销流程**：
- `FitnessCheckinServiceImpl.confirm()` → `checkin:confirm:{id}`
- `FitnessCheckinServiceImpl.exception()` → `checkin:exception:{id}`
- `FitnessCoachAppServiceImpl.checkinConfirm()` → `checkin:confirm:{recordId}`
- `FitnessCoachAppServiceImpl.checkinException()` → `checkin:exception:{recordId}`
- `FitnessCoachAppServiceImpl.consumeMemberCard()` → `card:consume:{memberCardId}`

**订单流程**：
- `FitnessOrderServiceImpl.add()` → `order:create:{memberId}`
- `FitnessOrderServiceImpl.pay()` → `order:pay:{id}`
- `FitnessOrderServiceImpl.refund()` → `order:refund:{id}`

**会员卡流程**：
- `FitnessMemberCardServiceImpl.adjustCard()` → `card:adjust:{id}`
- `FitnessMemberCardServiceImpl.updateStatus()` → `card:status:{id}`

### 3.2 乐观锁冲突兜底

#### 全局异常处理
在 `GlobalExceptionHandler` 中新增处理器：

```java
@ExceptionHandler(OptimisticLockerFailureException.class)
public R<Void> handleOptimisticLockerFailure(OptimisticLockerFailureException e) {
    log.warn("乐观锁冲突: {}", e.getMessage());
    return R.fail(HttpStatus.CONFLICT, "数据已被其他操作修改，请刷新后重试");
}
```

#### 自动重试机制
使用 `spring-retry`（若项目已引入则直接使用，否则手动实现简单重试）：

```java
// 在需要重试的 Service 方法上加 @Retryable
@Retryable(
    retryFor = OptimisticLockerFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
```

若不想引入新依赖，在 Service 层手动重试（偏好方案，减少依赖）：

```java
private <T> T retryOnConflict(int maxRetries, Supplier<T> fn) {
    for (int i = 0; i < maxRetries; i++) {
        try {
            return fn.get();
        } catch (OptimisticLockerFailureException e) {
            if (i == maxRetries - 1) throw e;
            Thread.sleep(100L * (i + 1));
        }
    }
    throw new OptimisticLockerFailureException("重试耗尽");
}
```

### 3.3 核心API限流

`@RateLimiter` 加到移动端和教练端暴露的写接口：

**会员小程序接口**（FitnessMiniController）：
| 方法 | 限流配置 | 说明 |
|------|---------|------|
| reservation() | time=1, count=5, IP | 每人每秒最多5次预约操作 |
| cancelReservation() | time=1, count=5, IP | 同上 |
| createOrder() | time=1, count=3, IP | 下单限流更严格 |
| checkin() | time=1, count=3, IP | 签到限流 |
| bindPhone() | time=60, count=1, IP | 换绑频率限制 |

**教练App接口**（FitnessCoachAppController）：
| 方法 | 限流配置 | 说明 |
|------|---------|------|
| checkinConfirm() | time=1, count=5, IP | 核销确认 |
| checkinException() | time=1, count=5, IP | 标记异常 |
| consumeMemberCard() | time=1, count=5, IP | 消耗卡权益 |

## 四、移动端 uniApp 网络层增强

### 4.1 防重复提交

在 `request.js` 中添加内存级防重：

```javascript
// 内存 Map 记录最近提交
const submitCache = new Map()
const REPEAT_INTERVAL = 1000 // 1秒

function isRepeatSubmit(url, data) {
  const key = url + JSON.stringify(data || {})
  const last = submitCache.get(key)
  const now = Date.now()
  if (last && (now - last) < REPEAT_INTERVAL) return true
  submitCache.set(key, now)
  // 定期清理过期缓存
  if (submitCache.size > 50) {
    const expire = now - REPEAT_INTERVAL * 2
    for (const [k, t] of submitCache) {
      if (t < expire) submitCache.delete(k)
    }
  }
  return false
}
```

### 4.2 请求重试

对特定错误自动重试（1次）：

```javascript
const MAX_RETRIES = 1

async function requestWithRetry(config, retryCount = 0) {
  try {
    return await uniRequest(config)
  } catch (err) {
    // 超时 或 503 时重试
    if (retryCount < MAX_RETRIES && 
        (err.errMsg?.includes('timeout') || err.statusCode === 503)) {
      return requestWithRetry(config, retryCount + 1)
    }
    throw err
  }
}
```

### 4.3 超时优化
| 接口类别 | 当前 | 调整后 |
|---------|------|--------|
| 普通查询 | 15s | 10s |
| 写操作(预约/签到) | 15s | 15s（保持） |
| 文件上传 | 15s | 30s |

### 4.4 弱网提示
- 请求发起时设置 5s 无响应提示"网络较慢，请稍候"
- 超时后提示"网络请求超时，请检查网络后重试"
- 重试失败后提示"当前网络不稳定，请稍后再试"

## 五、管理端前端加固

管理端已有 50s 超时 + 500ms sessionStorage 防重，需补充：
- 网络错误时自动重试一次（502/503）
- 超时时间从 50s 缩短到 30s（正常操作不应超过 30s）
- 更好的错误提示区分（网络错误 vs 业务错误）

## 六、涉及文件清单

### 后端
| 文件 | 改动 |
|------|------|
| `ruoyi-modules/.../fitness/service/impl/FitnessReservationServiceImpl.java` | +@Lock4j |
| `ruoyi-modules/.../fitness/service/impl/FitnessScheduleServiceImpl.java` | +@Lock4j |
| `ruoyi-modules/.../fitness/service/impl/FitnessCheckinServiceImpl.java` | +@Lock4j |
| `ruoyi-modules/.../fitness/service/impl/FitnessCoachAppServiceImpl.java` | +@Lock4j |
| `ruoyi-modules/.../fitness/service/impl/FitnessOrderServiceImpl.java` | +@Lock4j |
| `ruoyi-modules/.../fitness/service/impl/FitnessMemberCardServiceImpl.java` | +@Lock4j |
| `ruoyi-modules/.../fitness/controller/FitnessMiniController.java` | +@RateLimiter |
| `ruoyi-modules/.../fitness/controller/FitnessCoachAppController.java` | +@RateLimiter |
| `ruoyi-common/.../web/handler/GlobalExceptionHandler.java` | +OptimisticLockerFailureException |
| `ruoyi-common/.../common/mybatis/handler/MybatisExceptionHandler.java` | +OptimisticLockerFailureException |

### 移动端
| 文件 | 改动 |
|------|------|
| `frontend/T-uniApp/utils/request.js` | 防重+重试+超时+弱网 |

### 管理端
| 文件 | 改动 |
|------|------|
| `frontend/T-plus-ui/src/utils/request.ts` | 超时优化+重试 |

## 七、不涉及的内容（仅记录，清晰的范围界定）
- 不引入新依赖（spring-retry 先用手动重试）
- 不改造状态机
- 不添加 SnailJob 补偿任务
- 不修改数据库表结构
- 不改业务逻辑本身
