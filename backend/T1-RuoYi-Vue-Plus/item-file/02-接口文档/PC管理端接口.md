## 荆州市某健身房项目第一阶段 PC 管理端接口文档

### 一、文档说明
本文件面向 **PC 管理端**，用于沉淀一期后台业务接口的当前真实口径。

适用角色：
- `tenant_admin`
- `store_manager`
- `front_desk`
- `finance_viewer`
- 部分查询接口可按权限开放给 `coach_user` 只读查看

统一说明：
- 后台登录、系统用户、角色、部门、岗位、菜单、OSS 等通用接口继续复用若依现有接口，不在本文重复定义。
- 本文只描述 `fitness` 业务接口。
- **设计范围** 可以参考需求与业务设计，但 **实现状态** 必须以当前 controller / service / mapper 代码为准。

### 二、当前真实实现状态（2026-05-18）
- 当前 PC 管理端后端已落地 `首页经营概览`、`门店管理`、`会员管理`、`教练管理`、`课程管理`、`卡项方案管理`、`排课管理`、`预约管理`、`订单管理`、`会员持卡管理`、`签到核销`、`报表中心`、`订单明细`、`退款` 等 14 组接口。
- 其中 `报表中心`、`订单明细`、`退款` 为二期新增落地，已具备独立 controller 和完整 CRUD/查询能力。
- 当前前端状态应区分”仓库存在”与”运行态已验证”：外部 `T-plus-ui` 仓已有对应实现/近期修复信息，但本次以 backend 工作区为主，未完成完整运行态回归。
- 当前待完善能力：第三方支付回调、退款流水记录与部分退款、核销撤销等后续优化能力。

### 三、通用约定

#### 1. 鉴权方式
统一使用：
- `Authorization: Bearer {token}`

#### 2. 通用响应结构
- **列表分页**：`TableDataInfo<T>`
- **单对象**：`R<T>`
- **动作结果**：`R<Void>`

#### 3. 权限口径
页面与按钮权限统一以 controller 上的 `@SaCheckPermission` 为准，不再使用旧版草案中的过期动作名。

### 四、接口模块总览
| 状态 | 模块 | 前缀 | 当前说明 |
| --- | --- | --- | --- |
| 已落地 | 首页经营概览 | `/fitness/home` | 当前仅暴露聚合概览接口 |
| 已落地 | 门店管理 | `/fitness/store` | 列表、导出、详情、新增、编辑、状态、删除 |
| 已落地 | 会员管理 | `/fitness/member` | 列表、导出、详情、新增、编辑、删除 |
| 已落地 | 教练管理 | `/fitness/coach` | 列表、导出、详情、新增、编辑、删除 |
| 已落地 | 课程管理 | `/fitness/course` | 列表、导出、详情、新增、编辑、上下架、删除 |
| 已落地 | 卡项方案管理 | `/fitness/cardPlan` | 列表、导出、详情、新增、编辑、上下架、删除 |
| 已落地 | 排课管理 | `/fitness/schedule` | 列表、导出、详情、新增、编辑、取消、删除 |
| 已落地 | 预约管理 | `/fitness/reservation` | 列表、导出、详情、新增、编辑、取消、删除 |
| 已落地 | 订单管理 | `/fitness/order` | 列表、导出、详情、新增、编辑、支付、取消、删除 |
| 已落地 | 会员持卡管理 | `/fitness/memberCard` | 列表、导出、详情、状态调整、权益调整 |
| 已落地 | 签到核销 | `/fitness/checkin` | 列表、导出、详情、确认核销、标记异常 |
| 已落地 | 报表中心 | `/fitness/report` | 列表（会员/订单/课程/教练门店）、导出、详情 |
| 已落地 | 订单明细模块 | `/fitness/orderItem` | 列表、导出、详情 |
| 已落地 | 退款回滚 | `/fitness/order/{orderId}/refund` | 已支付订单退款 |

### 五、已落地接口清单

#### 1. 首页经营概览
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/home/overview` | `fitness:home:view` | 首页统计、待办、快捷入口聚合 |

#### 2. 门店管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/store/list` | `fitness:store:list` | 门店分页查询 |
| `POST` | `/fitness/store/export` | `fitness:store:export` | 门店导出 |
| `GET` | `/fitness/store/{storeId}` | `fitness:store:query` | 门店详情 |
| `POST` | `/fitness/store` | `fitness:store:add` | 新增门店 |
| `PUT` | `/fitness/store` | `fitness:store:edit` | 编辑门店 |
| `PUT` | `/fitness/store/{storeId}/status` | `fitness:store:edit` | 门店启停 |
| `DELETE` | `/fitness/store/{ids}` | `fitness:store:remove` | 删除门店 |

#### 3. 教练管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/coach/list` | `fitness:coach:list` | 教练分页查询 |
| `POST` | `/fitness/coach/export` | `fitness:coach:export` | 教练导出 |
| `GET` | `/fitness/coach/{coachId}` | `fitness:coach:query` | 教练详情 |
| `POST` | `/fitness/coach` | `fitness:coach:add` | 新增教练 |
| `PUT` | `/fitness/coach` | `fitness:coach:edit` | 编辑教练 |
| `DELETE` | `/fitness/coach/{ids}` | `fitness:coach:remove` | 删除教练 |

#### 4. 会员管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/member/list` | `fitness:member:list` | 会员分页查询 |
| `POST` | `/fitness/member/export` | `fitness:member:export` | 会员导出 |
| `GET` | `/fitness/member/{memberId}` | `fitness:member:query` | 会员详情 |
| `POST` | `/fitness/member` | `fitness:member:add` | 新增会员 |
| `PUT` | `/fitness/member` | `fitness:member:edit` | 编辑会员 |
| `DELETE` | `/fitness/member/{ids}` | `fitness:member:remove` | 删除会员 |

#### 5. 课程管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/course/list` | `fitness:course:list` | 课程分页查询 |
| `POST` | `/fitness/course/export` | `fitness:course:export` | 课程导出 |
| `GET` | `/fitness/course/{courseId}` | `fitness:course:query` | 课程详情 |
| `POST` | `/fitness/course` | `fitness:course:add` | 新增课程 |
| `PUT` | `/fitness/course` | `fitness:course:edit` | 编辑课程 |
| `PUT` | `/fitness/course/{courseId}/status` | `fitness:course:edit` | 课程上下架 |
| `DELETE` | `/fitness/course/{ids}` | `fitness:course:remove` | 删除课程 |

#### 6. 卡项方案管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/cardPlan/list` | `fitness:cardPlan:list` | 卡项方案分页查询 |
| `POST` | `/fitness/cardPlan/export` | `fitness:cardPlan:export` | 卡项方案导出 |
| `GET` | `/fitness/cardPlan/{planId}` | `fitness:cardPlan:query` | 卡项方案详情 |
| `POST` | `/fitness/cardPlan` | `fitness:cardPlan:add` | 新增卡项方案 |
| `PUT` | `/fitness/cardPlan` | `fitness:cardPlan:edit` | 编辑卡项方案 |
| `PUT` | `/fitness/cardPlan/{planId}/status` | `fitness:cardPlan:edit` | 卡项上下架 |
| `DELETE` | `/fitness/cardPlan/{ids}` | `fitness:cardPlan:remove` | 删除卡项方案 |

#### 7. 排课管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/schedule/list` | `fitness:schedule:list` | 排课分页查询 |
| `POST` | `/fitness/schedule/export` | `fitness:schedule:export` | 排课导出 |
| `GET` | `/fitness/schedule/{scheduleId}` | `fitness:schedule:query` | 排课详情 |
| `POST` | `/fitness/schedule` | `fitness:schedule:add` | 新增排课 |
| `PUT` | `/fitness/schedule` | `fitness:schedule:edit` | 编辑排课 |
| `PUT` | `/fitness/schedule/{scheduleId}/cancel` | `fitness:schedule:edit` | 取消排课 |
| `DELETE` | `/fitness/schedule/{ids}` | `fitness:schedule:remove` | 删除排课 |

#### 8. 预约管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/reservation/list` | `fitness:reservation:list` | 预约分页查询 |
| `POST` | `/fitness/reservation/export` | `fitness:reservation:export` | 预约导出 |
| `GET` | `/fitness/reservation/{reservationId}` | `fitness:reservation:query` | 预约详情 |
| `POST` | `/fitness/reservation` | `fitness:reservation:add` | 后台创建预约 |
| `PUT` | `/fitness/reservation` | `fitness:reservation:edit` | 编辑预约 |
| `PUT` | `/fitness/reservation/{reservationId}/cancel` | `fitness:reservation:edit` | 后台取消预约 |
| `DELETE` | `/fitness/reservation/{ids}` | `fitness:reservation:remove` | 删除预约 |

> 说明：当前真实路径是 `POST /fitness/reservation`，不再使用旧稿中的 `POST /fitness/reservation/adminCreate`。

#### 9. 订单管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/order/list` | `fitness:order:list` | 订单分页查询 |
| `POST` | `/fitness/order/export` | `fitness:order:export` | 订单导出 |
| `GET` | `/fitness/order/{orderId}` | `fitness:order:query` | 订单详情 |
| `POST` | `/fitness/order` | `fitness:order:add` | 后台录单 |
| `PUT` | `/fitness/order` | `fitness:order:edit` | 编辑待支付订单 |
| `PUT` | `/fitness/order/{orderId}/pay` | `fitness:order:edit` | 订单确认收款 |
| `PUT` | `/fitness/order/{orderId}/cancel` | `fitness:order:edit` | 取消待支付订单 |
| `DELETE` | `/fitness/order/{ids}` | `fitness:order:remove` | 删除未支付订单 |

> 说明：当前支付接口真实方法是 `PUT`，退款接口见下方 §14。

#### 10. 会员持卡管理
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/memberCard/list` | `fitness:memberCard:list` | 会员持卡分页查询 |
| `POST` | `/fitness/memberCard/export` | `fitness:memberCard:export` | 会员持卡导出 |
| `GET` | `/fitness/memberCard/{cardId}` | `fitness:memberCard:query` | 会员持卡详情 |
| `PUT` | `/fitness/memberCard/{cardId}/status` | `fitness:memberCard:manage` | 调整卡状态 |
| `PUT` | `/fitness/memberCard/{cardId}/adjust` | `fitness:memberCard:manage` | 调整剩余次数、余额、绑定教练、有效期 |

> 说明：当前真实路径是 `PUT /fitness/memberCard/{cardId}/status`，不再使用旧稿中的 `freeze` / `activate` 拆分口径。

#### 11. 签到核销
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/checkin/list` | `fitness:checkin:list` | 签到核销分页查询 |
| `POST` | `/fitness/checkin/export` | `fitness:checkin:export` | 签到核销导出 |
| `GET` | `/fitness/checkin/{checkinId}` | `fitness:checkin:query` | 签到详情 |
| `PUT` | `/fitness/checkin/{checkinId}/confirm` | `fitness:checkin:edit` | 确认核销并扣减权益 |
| `PUT` | `/fitness/checkin/{checkinId}/exception` | `fitness:checkin:edit` | 标记异常 |

> 说明：当前真实动作是 `confirm` / `exception` 两类接口，没有独立 `summary`、`manual`、`reverse` 接口。

#### 12. 报表中心
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/report/member` | `fitness:report:list` | 会员报表 |
| `POST` | `/fitness/report/member/export` | `fitness:report:export` | 会员报表导出 |
| `GET` | `/fitness/report/order` | `fitness:report:list` | 订单报表 |
| `POST` | `/fitness/report/order/export` | `fitness:report:export` | 订单报表导出 |
| `GET` | `/fitness/report/course` | `fitness:report:list` | 课程报表 |
| `POST` | `/fitness/report/course/export` | `fitness:report:export` | 课程报表导出 |
| `GET` | `/fitness/report/coachStore` | `fitness:report:list` | 教练门店经营报表 |
| `POST` | `/fitness/report/coachStore/export` | `fitness:report:export` | 教练门店经营报表导出 |

#### 13. 订单明细
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/fitness/orderItem/list` | `fitness:orderItem:list` | 订单明细分页查询 |
| `POST` | `/fitness/orderItem/export` | `fitness:orderItem:export` | 订单明细导出 |
| `GET` | `/fitness/orderItem/{itemId}` | `fitness:orderItem:query` | 订单明细详情 |

#### 14. 退款接口
| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `PUT` | `/fitness/order/{orderId}/refund` | `fitness:order:edit` | 已支付订单退款 |

> 说明：当前退款接口为后端基础能力，第三方支付回调、退款流水记录、部分退款仍为待完善项。

### 六、当前待完善能力
以下能力当前尚未完全落地，属于后续迭代范围：
- 第三方支付回调与支付状态自动同步
- 退款流水记录与部分退款
- 核销撤销（reverse）能力

### 七、当前真实状态下的使用建议
- **接口路径** 以当前 controller 为准，不再继续沿用旧稿中的 `adminCreate`、`freeze` 等过期路径。
- **动作权限** 以 `@SaCheckPermission` 为准，不再继续使用历史草案中的旧动作名。
- **前端联调** 时应将本文视为后端真实口径，页面是否已完整可用仍需以后续前端运行态回归结果为准。
