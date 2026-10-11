## 第一阶段菜单 SQL 与权限标识草案

### 一、文档说明
本文档用于把《第一阶段_业务设计.md》中确定的一期后台菜单结构，进一步落实成可执行的 **`sys_menu` 菜单 SQL 草案** 与 **权限标识草案**。

适用范围：
- `RuoYi-Vue-Plus` 后台菜单配置
- 管理端路由与组件映射
- 一期按钮权限规划

### 二、当前真实状态（2026-05-18）
- 本文档当前仍然是 **菜单与权限草案**，不代表已全部导库或已完成前端交付。
- 当前后端真实已落地的 PC 业务接口已覆盖：`首页经营概览`、`门店管理`、`会员管理`、`教练管理`、`课程管理`、`卡项方案管理`、`排课管理`、`预约管理`、`订单管理`、`会员持卡管理`、`签到核销`、`报表中心`、`订单明细`、`退款`。
- 当前前端状态应区分”仓库存在”与”运行态验证”：外部 `T-plus-ui` 仓已有对应实现或近期修复信息，但本次 backend workspace 未完成完整运行态回归。
- 因此本文中的 `component` 与 `route` 应视为 **菜单落地建议**，不是当前仓内已全部验证通过的最终路径。

### 三、统一规则

#### 1. 路由规则
在 `sys_menu` 中：
- **一级目录 `path`** 保存一级路径片段，例如：`fitness-store`
- **二级菜单 `path`** 保存业务路径片段，例如：`storeManage`
- **前端实际访问地址** 为：`/一级路径/二级路径`

例如：
- 一级路径：`fitness-store`
- 二级路径：`storeManage`
- 实际访问地址：`/fitness-store/storeManage`

#### 2. 组件路径规则
当前建议统一采用：
- `fitness/home/index`
- `fitness/store/index`
- `fitness/coach/index`
- `fitness/member/index`
- `fitness/memberCard/index`
- `fitness/course/index`
- `fitness/schedule/index`
- `fitness/reservation/index`
- `fitness/checkin/index`
- `fitness/cardPlan/index`
- `fitness/order/index`

> 说明：`会员权益 / 会员持卡` 当前建议统一收敛到 `fitness/memberCard/index`，不再同时维护 `fitness/memberCard/manage` 的重复组件口径。

#### 3. 权限标识规则
统一使用：

```text
fitness:业务名:动作
```

当前建议与真实后端权限对齐的典型示例：
- `fitness:store:list`
- `fitness:store:query`
- `fitness:store:add`
- `fitness:store:edit`
- `fitness:store:remove`
- `fitness:memberCard:manage`
- `fitness:checkin:edit`
- `fitness:order:edit`

> 说明：页面权限与按钮权限建议统一以 controller 上的 `@SaCheckPermission` 为准，不再继续沿用历史草案中的 `fitness:checkin:checkin`、`fitness:order:refund`、`fitness:memberCard:freeze` 等旧动作名。

### 四、当前建议的一期菜单归口

#### 1. 建议首批落库的菜单
| 一级目录 | 二级菜单 | 建议路由 | 建议组件 | 页面权限 |
| --- | --- | --- | --- | --- |
| 首页 | 首页 | `/fitness-home` | `fitness/home/index` | `fitness:home:view` |
| 门店管理 | 门店管理 | `/fitness-store/storeManage` | `fitness/store/index` | `fitness:store:list` |
| 人员管理 | 教练管理 | `/fitness-staff/coachManage` | `fitness/coach/index` | `fitness:coach:list` |
| 会员管理 | 会员管理 | `/fitness-member/memberManage` | `fitness/member/index` | `fitness:member:list` |
| 会员管理 | 会员权益 | `/fitness-member/memberRights` | `fitness/memberCard/index` | `fitness:memberCard:list` |
| 课程中心 | 课程管理 | `/fitness-course/courseManage` | `fitness/course/index` | `fitness:course:list` |
| 课程中心 | 排课管理 | `/fitness-course/scheduleManage` | `fitness/schedule/index` | `fitness:schedule:list` |
| 课程中心 | 预约记录 | `/fitness-course/reservationRecord` | `fitness/reservation/index` | `fitness:reservation:list` |
| 课程中心 | 签到核销 | `/fitness-course/checkinManage` | `fitness/checkin/index` | `fitness:checkin:list` |
| 卡项中心 | 卡项方案 | `/fitness-card/cardPlanManage` | `fitness/cardPlan/index` | `fitness:cardPlan:list` |
| 报表中心 | 报表查询 | `/fitness-report/reportQuery` | `fitness/report/index` | `fitness:report:list` |
| 订单中心 | 订单管理 | `/fitness-order/orderManage` | `fitness/order/index` | `fitness:order:list` |
| 订单中心 | 订单明细 | `/fitness-order/orderItemManage` | `fitness/orderItem/index` | `fitness:orderItem:list` |

#### 2. 建议保留但暂不作为”已落地能力”宣称的菜单
| 模块 | 当前建议 |
| --- | --- |
| 商户管理 | 继续保留为预留菜单，不作为一期真实已落地能力 |
| 门店设置 | 继续保留为扩展菜单，不作为当前真实接口能力 |
| 员工管理 / 角色权限 / 部门岗位 | 建议优先复用若依系统菜单，不单独宣称为 `fitness` 已落地业务模块 |

### 五、菜单 SQL 草案（按当前真实状态整理）

#### 1. 一级菜单 SQL
```sql
-- =========================
-- 一期健身房业务菜单（一级菜单）
-- =========================
insert into sys_menu values('7000', '首页',     '0', '20', 'fitness-home',   'fitness/home/index', '', 1, 0, 'C', '0', '0', 'fitness:home:view', 'dashboard', 103, 1, sysdate(), null, null, '健身房首页菜单');
insert into sys_menu values('7100', '门店管理', '0', '21', 'fitness-store',  null,                 '', 1, 0, 'M', '0', '0', '',                  'shop',      103, 1, sysdate(), null, null, '门店管理目录');
insert into sys_menu values('7200', '人员管理', '0', '22', 'fitness-staff',  null,                 '', 1, 0, 'M', '0', '0', '',                  'peoples',   103, 1, sysdate(), null, null, '人员管理目录');
insert into sys_menu values('7300', '会员管理', '0', '23', 'fitness-member', null,                 '', 1, 0, 'M', '0', '0', '',                  'user',      103, 1, sysdate(), null, null, '会员管理目录');
insert into sys_menu values('7400', '课程中心', '0', '24', 'fitness-course', null,                 '', 1, 0, 'M', '0', '0', '',                  'education', 103, 1, sysdate(), null, null, '课程中心目录');
insert into sys_menu values('7500', '卡项中心', '0', '25', 'fitness-card',   null,                 '', 1, 0, 'M', '0', '0', '',                  'list',      103, 1, sysdate(), null, null, '卡项中心目录');
insert into sys_menu values('7600', '订单中心', '0', '26', 'fitness-order',  null,                 '', 1, 0, 'M', '0', '0', '',                  'money',     103, 1, sysdate(), null, null, '订单中心目录');
insert into sys_menu values('7700', '报表中心', '0', '27', 'fitness-report', null,               '', 1, 0, 'M', '0', '0', '',                  'chart',     103, 1, sysdate(), null, null, '报表中心目录');
```

#### 2. 二级菜单 SQL
```sql
-- =========================
-- 门店管理
-- =========================
insert into sys_menu values('7102', '门店管理', '7100', '2', 'storeManage', 'fitness/store/index', '', 1, 0, 'C', '0', '0', 'fitness:store:list', 'shop', 103, 1, sysdate(), null, null, '门店管理菜单');

-- =========================
-- 人员管理
-- =========================
insert into sys_menu values('7202', '教练管理', '7200', '2', 'coachManage', 'fitness/coach/index', '', 1, 0, 'C', '0', '0', 'fitness:coach:list', 'peoples', 103, 1, sysdate(), null, null, '教练管理菜单');

-- =========================
-- 会员管理
-- =========================
insert into sys_menu values('7301', '会员管理', '7300', '1', 'memberManage', 'fitness/member/index',     '', 1, 0, 'C', '0', '0', 'fitness:member:list',     'user', 103, 1, sysdate(), null, null, '会员管理菜单');
insert into sys_menu values('7302', '会员权益', '7300', '2', 'memberRights', 'fitness/memberCard/index', '', 1, 0, 'C', '0', '0', 'fitness:memberCard:list', 'list', 103, 1, sysdate(), null, null, '会员权益菜单');

-- =========================
-- 课程中心
-- =========================
insert into sys_menu values('7401', '课程管理', '7400', '1', 'courseManage',      'fitness/course/index',      '', 1, 0, 'C', '0', '0', 'fitness:course:list',      'education', 103, 1, sysdate(), null, null, '课程管理菜单');
insert into sys_menu values('7402', '排课管理', '7400', '2', 'scheduleManage',    'fitness/schedule/index',    '', 1, 0, 'C', '0', '0', 'fitness:schedule:list',    'date',      103, 1, sysdate(), null, null, '排课管理菜单');
insert into sys_menu values('7403', '预约记录', '7400', '3', 'reservationRecord', 'fitness/reservation/index', '', 1, 0, 'C', '0', '0', 'fitness:reservation:list', 'log',       103, 1, sysdate(), null, null, '预约记录菜单');
insert into sys_menu values('7404', '签到核销', '7400', '4', 'checkinManage',     'fitness/checkin/index',     '', 1, 0, 'C', '0', '0', 'fitness:checkin:list',     'form',      103, 1, sysdate(), null, null, '签到核销菜单');

-- =========================
-- 卡项中心
-- =========================
insert into sys_menu values('7501', '卡项方案', '7500', '1', 'cardPlanManage', 'fitness/cardPlan/index', '', 1, 0, 'C', '0', '0', 'fitness:cardPlan:list', 'list', 103, 1, sysdate(), null, null, '卡项方案菜单');

-- =========================
-- 订单中心
-- =========================
insert into sys_menu values('7601', '订单管理', '7600', '1', 'orderManage', 'fitness/order/index', '', 1, 0, 'C', '0', '0', 'fitness:order:list', 'money', 103, 1, sysdate(), null, null, '订单管理菜单');
insert into sys_menu values('7602', '订单明细', '7600', '2', 'orderItemManage', 'fitness/orderItem/index', '', 1, 0, 'C', '0', '0', 'fitness:orderItem:list', 'list', 103, 1, sysdate(), null, null, '订单明细菜单');

-- =========================
-- 报表中心
-- =========================
insert into sys_menu values('7701', '报表查询', '7700', '1', 'reportQuery', 'fitness/report/index', '', 1, 0, 'C', '0', '0', 'fitness:report:list', 'chart', 103, 1, sysdate(), null, null, '报表查询菜单');
```

#### 3. 隐藏路由 SQL 草案
```sql
-- =========================
-- 隐藏路由（visible=1）
-- =========================
insert into sys_menu values('7901', '门店详情',   '7100', '90', 'storeDetail/:storeId',             'fitness/store/detail',      '', 1, 1, 'C', '1', '0', 'fitness:store:query',      '#', 103, 1, sysdate(), null, null, '/fitness-store/storeManage');
insert into sys_menu values('7902', '门店编辑',   '7100', '91', 'storeEdit/:storeId',               'fitness/store/edit',        '', 1, 1, 'C', '1', '0', 'fitness:store:edit',       '#', 103, 1, sysdate(), null, null, '/fitness-store/storeManage');
insert into sys_menu values('7903', '教练详情',   '7200', '90', 'coachDetail/:coachId',             'fitness/coach/detail',      '', 1, 1, 'C', '1', '0', 'fitness:coach:query',      '#', 103, 1, sysdate(), null, null, '/fitness-staff/coachManage');
insert into sys_menu values('7904', '会员详情',   '7300', '90', 'memberDetail/:memberId',           'fitness/member/detail',     '', 1, 1, 'C', '1', '0', 'fitness:member:query',     '#', 103, 1, sysdate(), null, null, '/fitness-member/memberManage');
insert into sys_menu values('7905', '会员权益调整', '7302', '91', 'memberCardAdjust/:cardId',       'fitness/memberCard/adjust', '', 1, 1, 'C', '1', '0', 'fitness:memberCard:manage', '#', 103, 1, sysdate(), null, null, '/fitness-member/memberRights');
insert into sys_menu values('7906', '课程详情',   '7400', '90', 'courseDetail/:courseId',           'fitness/course/detail',     '', 1, 1, 'C', '1', '0', 'fitness:course:query',     '#', 103, 1, sysdate(), null, null, '/fitness-course/courseManage');
insert into sys_menu values('7907', '排课详情',   '7400', '91', 'scheduleDetail/:scheduleId',       'fitness/schedule/detail',   '', 1, 1, 'C', '1', '0', 'fitness:schedule:query',   '#', 103, 1, sysdate(), null, null, '/fitness-course/scheduleManage');
insert into sys_menu values('7908', '预约详情',   '7400', '92', 'reservationDetail/:reservationId', 'fitness/reservation/detail','', 1, 1, 'C', '1', '0', 'fitness:reservation:query', '#', 103, 1, sysdate(), null, null, '/fitness-course/reservationRecord');
insert into sys_menu values('7909', '卡项详情',   '7500', '90', 'cardPlanDetail/:planId',           'fitness/cardPlan/detail',   '', 1, 1, 'C', '1', '0', 'fitness:cardPlan:query',   '#', 103, 1, sysdate(), null, null, '/fitness-card/cardPlanManage');
insert into sys_menu values('7910', '订单详情',   '7600', '90', 'orderDetail/:orderId',             'fitness/order/detail',      '', 1, 1, 'C', '1', '0', 'fitness:order:query',      '#', 103, 1, sysdate(), null, null, '/fitness-order/orderManage');
insert into sys_menu values('7911', '新建订单',   '7600', '91', 'orderCreate',                      'fitness/order/create',      '', 1, 1, 'C', '1', '0', 'fitness:order:add',        '#', 103, 1, sysdate(), null, null, '/fitness-order/orderManage');
insert into sys_menu values('7912', '订单退款',   '7600', '92', 'orderRefund/:orderId',             'fitness/order/refund',      '', 1, 1, 'C', '1', '0', 'fitness:order:edit',       '#', 103, 1, sysdate(), null, null, '/fitness-order/orderManage');
```

### 六、页面权限与按钮权限建议

#### 1. 页面级权限
| 模块 | 页面 | 页面权限 |
| --- | --- | --- |
| 首页 | 首页 | `fitness:home:view` |
| 门店管理 | 门店管理 | `fitness:store:list` |
| 人员管理 | 教练管理 | `fitness:coach:list` |
| 会员管理 | 会员管理 | `fitness:member:list` |
| 会员管理 | 会员权益 | `fitness:memberCard:list` |
| 课程中心 | 课程管理 | `fitness:course:list` |
| 课程中心 | 排课管理 | `fitness:schedule:list` |
| 课程中心 | 预约记录 | `fitness:reservation:list` |
| 课程中心 | 签到核销 | `fitness:checkin:list` |
| 卡项中心 | 卡项方案 | `fitness:cardPlan:list` |
| 订单中心 | 订单管理 | `fitness:order:list` |
| 订单中心 | 订单明细 | `fitness:orderItem:list` |
| 报表中心 | 报表查询 | `fitness:report:list` |

#### 2. 按钮权限建议
| 模块 | 推荐按钮权限 |
| --- | --- |
| 门店管理 | `fitness:store:query`、`fitness:store:add`、`fitness:store:edit`、`fitness:store:remove`、`fitness:store:export` |
| 教练管理 | `fitness:coach:query`、`fitness:coach:add`、`fitness:coach:edit`、`fitness:coach:remove`、`fitness:coach:export` |
| 会员管理 | `fitness:member:query`、`fitness:member:add`、`fitness:member:edit`、`fitness:member:remove`、`fitness:member:export` |
| 会员权益 | `fitness:memberCard:query`、`fitness:memberCard:manage`、`fitness:memberCard:export` |
| 课程管理 | `fitness:course:query`、`fitness:course:add`、`fitness:course:edit`、`fitness:course:remove`、`fitness:course:export` |
| 排课管理 | `fitness:schedule:query`、`fitness:schedule:add`、`fitness:schedule:edit`、`fitness:schedule:remove`、`fitness:schedule:export` |
| 预约记录 | `fitness:reservation:query`、`fitness:reservation:add`、`fitness:reservation:edit`、`fitness:reservation:remove`、`fitness:reservation:export` |
| 签到核销 | `fitness:checkin:query`、`fitness:checkin:edit`、`fitness:checkin:export` |
| 卡项方案 | `fitness:cardPlan:query`、`fitness:cardPlan:add`、`fitness:cardPlan:edit`、`fitness:cardPlan:remove`、`fitness:cardPlan:export` |
| 订单管理 | `fitness:order:query`、`fitness:order:add`、`fitness:order:edit`、`fitness:order:remove`、`fitness:order:export` |
| 订单明细 | `fitness:orderItem:query`、`fitness:orderItem:list`、`fitness:orderItem:export` |
| 报表查询 | `fitness:report:list`、`fitness:report:export` |

### 七、当前最稳妥的落地顺序
- **第一批（核心业务）**：首页、门店管理、教练管理、会员管理、课程管理、卡项方案、订单管理。
- **第二批（运营支撑）**：排课管理、预约记录、签到核销、会员权益。
- **第三批（数据与分析）**：报表中心、订单明细、退款。
- **预留**：商户管理、门店设置、员工管理、角色权限、部门岗位。

### 八、最终建议
- **菜单路径** 与 **组件路径** 可以继续作为前端设计稿推进，但不应再把它们写成”当前已经完整落地”的口径。
- **权限标识** 一律以当前 controller 的 `@SaCheckPermission` 为准。
- **商户管理、门店设置** 等预留模块当前只保留设计态，不应再与已落地模块混写。
