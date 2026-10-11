## 荆州市某健身房项目第一阶段教练 App 接口文档

### 一、文档说明
本文件面向 **教练 App 端**，用于沉淀当前一期教练端后端接口的真实实现口径。

当前代码位置：
- `ruoyi-system/src/main/java/org/dromara/fitness/controller/FitnessCoachAppController.java`
- `ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessCoachAppServiceImpl.java`
- `ruoyi-system/src/main/resources/mapper/fitness/FitnessCoachAppMapper.xml`

推荐前缀：`/fitness/coachApp`

### 二、当前实现边界
#### 1. 已落地范围
当前已落地的教练端能力包括：
- 教练登录与退出登录
- 工作台首页聚合
- 我的排课与排课详情
- 待核销列表与核销详情
- 确认核销与标记异常
- 我的学员与学员详情
- 我的资料查看与编辑
- 修改密码
- 消息列表聚合

#### 2. 当前兼容实现说明
- 教练登录复用 `sys_user + fit_coach` 关联数据。
- 当前消息列表为聚合结果，不依赖独立消息表。
- 当前待核销状态由 `checkin_status + operator_user_id` 推导。
- 当前核销通过 `reservationId` 定位签到记录，不要求前端额外传 `checkinId`。

### 三、接口总览
| 状态 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 已落地 | `POST` | `/fitness/coachApp/auth/login` | 教练登录 |
| 已落地 | `POST` | `/fitness/coachApp/auth/logout` | 教练退出登录 |
| 已落地 | `GET` | `/fitness/coachApp/home/overview` | 教练工作台首页 |
| 已落地 | `GET` | `/fitness/coachApp/schedule/my` | 我的排课 |
| 已落地 | `GET` | `/fitness/coachApp/schedule/{scheduleId}` | 排课详情 |
| 已落地 | `GET` | `/fitness/coachApp/checkin/pending` | 待核销列表 |
| 已落地 | `GET` | `/fitness/coachApp/checkin/{reservationId}` | 核销详情 |
| 已落地 | `POST` | `/fitness/coachApp/checkin/confirm` | 确认核销 |
| 已落地 | `POST` | `/fitness/coachApp/checkin/exception` | 标记异常签到 |
| 已落地 | `GET` | `/fitness/coachApp/student/my` | 我的学员列表 |
| 已落地 | `GET` | `/fitness/coachApp/student/{memberId}` | 学员详情 |
| 已落地 | `GET` | `/fitness/coachApp/mine/profile` | 我的资料 |
| 已落地 | `PUT` | `/fitness/coachApp/mine/profile` | 编辑我的资料 |
| 已落地 | `PUT` | `/fitness/coachApp/mine/password` | 修改密码 |
| 已落地 | `GET` | `/fitness/coachApp/message/list` | 我的消息 |

### 四、鉴权与状态约定
#### 1. 鉴权方式
除 `auth/login` 外，其余接口统一使用：
- `Authorization: Bearer {coachToken}`

#### 2. 通用响应结构
- **单对象**：`R<T>`
- **分页列表**：`TableDataInfo<T>`
- **动作类**：`R<Void>` 或 `R<T>`

#### 3. 当前核销状态口径
| 条件 | 返回状态 |
| --- | --- |
| `checkin_status = success` 且 `operator_user_id is null` | `pending_confirm` |
| `checkin_status = success` 且 `operator_user_id is not null` | `confirmed` |
| `checkin_status = reversed` | `exception` |

### 五、核心接口整理
#### 1. `POST /fitness/coachApp/auth/login`
- **说明**：教练使用后台关联账号登录移动端
- **主要表**：`sys_user`、`fit_coach`

##### 请求体字段
| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | `string` | 是 | 后台账号 |
| `password` | `string` | 是 | 登录密码 |

##### 响应字段摘要
- `accessToken`
- `expireIn`
- `clientId`
- `profile`
  - `tenantId`
  - `userId`
  - `userName`
  - `nickName`
  - `phonenumber`
  - `coachId`
  - `storeId`
  - `storeName`
  - `coachName`
  - `mobile`
  - `avatarUrl`
  - `skillTags`
  - `levelCode`
  - `intro`
  - `hireStatus`

#### 2. `POST /fitness/coachApp/auth/logout`
- **说明**：退出当前教练登录态

#### 3. `GET /fitness/coachApp/home/overview`
- **说明**：教练首页聚合数据
- **主要表**：`fit_course_schedule`、`fit_checkin_record`、`fit_member_card`

##### 当前返回结构
- `stats`
  - `todayScheduleCount`
  - `pendingCheckinCount`
  - `confirmedCount`
  - `totalStudentCount`
  - `weekSessionCount`
- `todayScheduleList`
- `pendingList`
- `messageList`

#### 4. `GET /fitness/coachApp/schedule/my`
##### 查询参数
- `scope`：支持 `today / tomorrow / week / all`
- `scheduleDate`
- `pageNum`
- `pageSize`

##### 当前返回字段
- `scheduleId`
- `storeId`、`storeName`
- `courseId`、`courseName`、`courseType`
- `coachId`、`coachName`
- `scheduleDate`
- `startTime`、`endTime`
- `capacity`
- `reservedCount`
- `signedCount`
- `scheduleStatus`

#### 5. `GET /fitness/coachApp/schedule/{scheduleId}`
返回单个排课详情对象，字段与排课列表一致。

#### 6. `GET /fitness/coachApp/checkin/pending`
- **说明**：当前接口虽然路径名是 `pending`，但实际支持按状态筛选

##### 查询参数
- `status`：支持 `pending / confirmed / exception / all`
- `pageNum`
- `pageSize`

##### 当前返回字段
- `checkinId`
- `reservationId`
- `scheduleId`
- `storeId`、`storeName`
- `courseId`、`courseName`、`courseType`
- `scheduleDate`
- `startTime`、`endTime`
- `memberId`
- `memberName`
- `memberMobile`
- `cardId`
- `cardName`
- `remainTimes`
- `checkinTime`
- `consumeTimes`
- `consumeAmount`
- `operatorUserId`
- `checkinStatus`
- `derivedCheckinStatus`
- `remark`

#### 7. `GET /fitness/coachApp/checkin/{reservationId}`
返回单次核销详情对象，字段与核销列表一致。

#### 8. `POST /fitness/coachApp/checkin/confirm`
- **说明**：确认会员到课并完成核销
- **主要动作**：
  - 更新签到记录操作人
  - 更新消耗次数 / 消耗金额
  - 回写 `fit_course_schedule.signed_count`
  - 若有会员卡，则扣减课次或余额

##### 请求体字段
| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `reservationId` | `long` | 是 | 预约 ID |
| `consumeTimes` | `int` | 否 | 次卡/课包扣次 |
| `consumeAmount` | `decimal` | 否 | 储值卡扣费 |
| `remark` | `string` | 否 | 核销备注 |

##### 当前校验
- 核销记录必须存在且属于当前教练
- 已异常记录不能再次确认
- 已确认记录不能重复确认
- 若有关联会员卡，校验剩余课次 / 余额是否足够

#### 9. `POST /fitness/coachApp/checkin/exception`
- **说明**：将当前签到记录标记为异常

##### 请求体字段
| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `reservationId` | `long` | 是 | 预约 ID |
| `remark` | `string` | 否 | 异常说明 |

##### 当前行为
- 将 `fit_checkin_record.checkin_status` 更新为 `reversed`
- 记录当前教练 `operator_user_id`

#### 10. `GET /fitness/coachApp/student/my`
##### 查询参数
- `keyword`
- `pageNum`
- `pageSize`

##### 当前返回字段
- `memberId`
- `realName`
- `nickname`
- `mobile`
- `avatarUrl`
- `lastVisitTime`
- `cardId`
- `cardName`
- `remainTimes`
- `balanceAmount`
- `nextClassTime`
- `nextCourseName`
- `totalCheckinCount`

#### 11. `GET /fitness/coachApp/student/{memberId}`
返回单个学员详情对象，字段与学员列表一致。

#### 12. `GET /fitness/coachApp/mine/profile`
##### 当前返回字段
- `tenantId`
- `userId`
- `userName`
- `nickName`
- `phonenumber`
- `coachId`
- `storeId`
- `storeName`
- `coachName`
- `mobile`
- `avatarUrl`
- `skillTags`
- `levelCode`
- `intro`
- `hireStatus`

#### 13. `PUT /fitness/coachApp/mine/profile`
##### 请求体字段
- `nickName`
- `coachName`
- `mobile`
- `avatarUrl`
- `skillTags`
- `intro`
- `remark`

##### 当前行为
- 更新 `fit_coach`
- 同步更新 `sys_user.nick_name`
- 同步更新 `sys_user.phonenumber`

#### 14. `PUT /fitness/coachApp/mine/password`
##### 请求体字段
- `oldPassword`
- `newPassword`

##### 当前行为
- 校验旧密码
- 禁止新旧密码相同
- 使用 BCrypt 更新 `sys_user.password`

#### 15. `GET /fitness/coachApp/message/list`
- **说明**：当前为聚合消息接口，不依赖独立消息表

##### 当前返回字段
- `messageType`
- `title`
- `content`
- `eventTime`
- `route`

##### 当前来源
- 待核销提醒
- 今日排课提醒

### 六、前后端联调提示
- 教练 App 页面骨架已存在，但当前页面仍以展示态和 mock 交互为主。
- 当前微信登录真实口径为：前端先调用 `wechatLogin` 用 `code/openid` 探测，若返回 `needActivation = true` 再弹窗输入手机号和激活码调用 `wechatActivate`。
- 当前登录口径应以本文件为准，不再继续使用旧版 `deviceId`、`phoneCode` 直登等未落地字段。
- 当前核销成功后，学员端应优先读取 `derivedCheckinStatus` 展示确认结果。
- 当前实现已支持异常标记与基础扣次/扣费，但**尚未把预约状态推进到 `signed`**，后续若做更完整状态机需补第二轮整理。

