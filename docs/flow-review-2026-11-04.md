# T1 网页管理流程审查

检查日期：2026年11月4日。

目前功能划分有基础，但还不能认为五条管理流程都清晰、简明、完整。应先修复身份、关系和状态处理，再整理菜单与表单。已具备的合理规则包括门店与课程一致性、教练所属门店和在职检查、教练时间冲突、预约及签到时间窗、人数限制；训练模板到编辑、保存、开启的起始流程也较清楚。

本次是前后端代码与流程审查，部分训练交互另做离线函数复现，没有修改业务代码或生产数据，也没有完成管理员后台逐项登录点击验收。审查版本为 `feat/fitness-training-and-booking` 的 `ffea203`。公开 H5 首页内容哈希仍与上次 r4-final2 一致，这一核对仅覆盖首页产物，不能替代整站或服务端版本验收。本机原项目目录已不存在，审查副本位于本文件所在项目目录。

## 五类管理的结论

| 范围 | 判断 | 主要影响 |
| --- | --- | --- |
| 门店 | 基础建档清楚，后续管理入口不足 | 需要跳出门店再找教练、会员和课程，缺少门店上下文 |
| 教练 | 建档和门店关系基本明确，状态与账号处理不完整 | 停用旧会话、删除后恢复和学员绑定需要补齐 |
| 会员 | 档案内容较完整，身份衔接有阻断 | 后台建档与本人微信登录不能自然合并；关联搜索容易失效 |
| 个人课程 | 课程、排课、预约已分开，但核销与收尾存在断点 | 定向名单、课包使用、稍后收款、场次结束、异常恢复需优先修复 |
| 个人训练计划 | 创建起步清楚，长期使用和中断恢复不完整 | 续用、暂存、顺延、草稿保护与管理员明细不足 |

## 应修复的问题

### 01 会员认领

处理级别：优先。

后台先建会员、填写手机号并开卡后，本人首次微信登录会新建另一会员；授权相同手机号时又被判定重复。原卡项、预约和教练关系留在旧档案，无法自然接到本人账号。

建议：手机号经微信验证后，应提供认领已有档案的安全流程；仅合并符合条件的未认领档案，并记录操作。

代码依据：[按 openid 新建会员](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessMiniServiceImpl.java#L62)；[相同手机号被拒绝](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessMiniServiceImpl.java#L104)。

### 02 停用与冻结

处理级别：优先。

停用教练主要更新档案和账号状态，但既有教练会话的签到核销路径未重新检查在职状态。会员冻结后，既有会话的预约路径也没有当前会员状态校验。新登录会被拒绝，不等于旧会话立即失效；训练计划接口另有状态检查，不能把这一问题扩大到所有接口。

建议：停用或冻结时注销相关会话，并在预约、代签到、核销等关键操作重新检查当前状态。

代码依据：[教练状态修改](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessCoachServiceImpl.java#L100)；[缓存身份读取](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/utils/FitnessSecurityUtils.java#L82)；[确认核销](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessCoachAppServiceImpl.java#L167)；[会员预约](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessMiniServiceImpl.java#L170)。

### 03 关联选择器搜索

处理级别：优先。

搜索会员或教练时，姓名、编号和手机号同时填入同一个关键词；后台把三个条件按 AND 连接。输入姓名往往还被要求手机号包含该姓名，导致搜不到。空关键词只加载前 20 条，较早建档的人难以选中，影响开卡、排课和管理员查看训练计划。

建议：使用统一关键词按姓名、编号、手机号任一匹配，并提供继续加载和明确空结果提示。

代码依据：[选择器参数](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/components/remoteOptions.ts#L110)；[会员 AND 条件](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/resources/mapper/fitness/FitnessMemberMapper.xml#L35)；[教练 AND 条件](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/resources/mapper/fitness/FitnessCoachMapper.xml#L37)。

### 04 私教订单稍后收款

处理级别：优先。

新增需要绑定教练的卡项订单时，默认立即支付为否。保存后再确认收款，只提交支付方式，之前选择的教练未持久化；发卡时因此报该卡项要求绑定教练。默认流程无法建立会员和教练关系。

建议：保存订单时保留绑定教练；确认收款前展示会员、课包、教练和金额，并允许补齐必填关联。

代码依据：[默认表单](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/order/index.vue#L99)；[确认收款请求](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/order/index.vue#L128)；[发卡绑定校验](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessOrderServiceImpl.java#L310)。

### 05 教练删除后重新建档

处理级别：后续。

删除教练后，自动创建的系统账号仍保留。使用同一手机号重新新增时，会因系统账号用户名或手机号冲突失败；普通删除提示没有说明这个结果，也没有对应恢复入口。

建议：常规操作使用停用和恢复；删除前明确处理关联账号与历史业务，避免要求用户去系统账号页面排障。

代码依据：[教练删除](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessCoachServiceImpl.java#L127)；[创建系统账号](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessCoachServiceImpl.java#L198)。

### 06 定向排课的学员选择

处理级别：优先。

常规新增或编辑私教、小班排课使用 remoteSelectMultiple 字段，共用表单却没有该类型的渲染分支。学员不能被选中；后台允许空名单，而空名单会向所有会员开放。批量排课有独立多选控件，可规避这一条路径的问题。

建议：补齐多选控件；将公开预约与指定学员设计成明确的模式，指定模式没有学员时禁止保存。

代码依据：[学员字段](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/schedule/index.vue#L207)；[表单渲染分支](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/components/FitnessCrudPage.vue#L161)；[空名单可见条件](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/resources/mapper/fitness/FitnessMiniMapper.xml#L343)。

### 07 正常课程结束

处理级别：后续。

场次只要还有预约人数，就不能改为已结束。正常签到核销不减少预约人数，已签到预约又不能取消，所以正常完成的场次仍会提示先处理预约。

建议：允许已核销或已按缺席规则处理的场次结束；只对未处理预约给出可执行的处理入口，保留历史人数。

代码依据：[结束限制](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessScheduleServiceImpl.java#L106)；[核销更新](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessCoachAppServiceImpl.java#L188)；[已签到不能取消](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessReservationServiceImpl.java#L143)。

### 08 会员预约与卡权益

处理级别：优先。

会员端单次及批量预约仅私教、小班弹出选卡，且选卡列表排除储值卡。团课会走无卡免费模式；签到时又禁止补换卡，无法按原卡权益核销。这与线上支付无关，问题发生在线下已购权益的使用流程。

建议：所有课程统一选择可用权益或明确的无卡体验；储值、次卡、期限卡分别展示扣款、扣次或权益验证规则。

代码依据：[预约选卡条件](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/course/index.vue#L829)；[储值卡排除](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/course/index.vue#L857)；[签到不能换卡](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessMiniServiceImpl.java#L404)。

### 09 管理员储值卡核销

处理级别：后续。

管理员确认核销只提交空对象，没有金额表单。服务端把储值卡默认按一次、零元处理；常见的零课次、有余额储值卡会报剩余课次不足。教练端已有金额输入，管理员端的流程不一致。

建议：根据权益类型显示扣次或扣金额，在确认框中列出核销前后余额，统一两端校验。

代码依据：[空核销请求](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/checkin/index.vue#L75)；[扣次和金额默认值](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessCheckinServiceImpl.java#L76)。

### 10 签到异常恢复

处理级别：后续。

签到被误标异常后，管理员不能确认，教练不能重新代签，会员重试仍拿到原记录；取消预约也被已有签到阻止。页面与控制器没有恢复或撤销异常的完整入口。

建议：增加有权限的恢复审核或撤销流程，要求原因并保留审计；未恢复前给出准确的下一步。

代码依据：[异常不可核销](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessCheckinServiceImpl.java#L70)；[代签拒绝异常](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/support/FitnessOperationsService.java#L190)；[取消限制](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/support/FitnessOperationsService.java#L154)。

### 11 训练计划续用

处理级别：优先。

开启有限循环并完成全部日程后，计划仍显示进行中。重新开启入口只对草稿开放；没有追加循环、结束归档或复制私人计划入口。用户只能重新从头建计划，不能自然继续原计划。

建议：补齐待开启、进行中、已完成、已归档状态；提供继续下一轮和复制为新计划，保持历史训练记录。

代码依据：[开启入口条件](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/training/index.vue#L210)；[复制模板入口](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/training/index.vue#L1537)；[仅草稿可开启](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessTrainingService.java#L124)。

### 12 训练中途保存

处理级别：后续。

只勾选一组热身后点击保存，整个训练日会被标为已完成，并从待训练列表消失。重新打开只恢复已保存组，未完成的正式组目标不再作为记录行恢复，需要手动添加。

建议：区分暂存进度与完成本次训练；至少明确结束含义，并在继续记录时恢复未完成目标。

代码依据：[保存按钮与提示](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/training/index.vue#L1069)；[重新打开记录](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/training/index.vue#L1894)；[保存即完成](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessTrainingService.java#L221)。

### 13 缩短训练循环

处理级别：后续。

草稿第七天已有动作时，把循环从七天改为六天，再改回七天，末日配置会被直接截断，变成空休息日；没有丢弃提示。

建议：缩短前显示受影响日期并确认，取消时恢复原周期；允许撤销本次调整。

代码依据：[失焦调整](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/training/index.vue#L477)；[日数组截断](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/training/index.vue#L1572)。

### 14 训练顺延冲突

处理级别：后续。

前一天漏练而后一天已完成时，顺延一天可能落到完成记录日期。后台提示选择其他天数，但页面固定提交一天，用户没有可执行的下一步。

建议：提供目标日期或天数选择，并预览后续受影响日程；已完成记录保持原日期。

代码依据：[固定顺延一天](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-uniApp/pages/training/index.vue#L1777)；[已完成日期冲突校验](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessTrainingService.java#L163)。

### 15 管理员切换会员查看计划

处理级别：后续。

先选会员 A 再选 B，若 A 的请求更晚返回，选择框显示 B，表格却被 A 的计划覆盖。表格没有会员列，容易被当成 B 的记录。该请求竞争已用离线函数复现。

建议：取消过期请求或校验请求序号，切换时清空旧列表；详情持续显示会员姓名和计划名。

代码依据：[计划加载无响应归属检查](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/training/index.vue#L57)。

### 16 管理员训练管理范围

处理级别：清晰度。

后台训练页目前明确是只读查看和审计，不支持代编、分配或续排。实际记录只展示汇总，没有逐组重量、次数、原单位和强度的展开入口，状态和审计类型也直接显示原值。

建议：明确命名为训练计划查看；若要管理员管理计划，应另外补齐编辑与分配权限。增加逐组明细，将状态、身份和动作名称中文化。

代码依据：[只读定位](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/training/index.vue#L3)；[实际记录和审计展示](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/training/index.vue#L21)。

### 17 门店和会员的操作入口

处理级别：清晰度。

门店页主要是基础资料表，没有从该店直接查看教练、会员和课表的入口。会员详情虽聚合持卡、订单、预约和签到，但没有训练计划入口或带着当前会员直接办理下一步的操作，用户需要跨菜单重新选择。

建议：增加门店详情与会员工作台；从当前对象进入下一个操作时自动带入门店或会员。教练学员关系的成立条件也应直接说明，当前训练权限依赖有效绑定关系。

代码依据：[门店基础表](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/store/index.vue#L1)；[会员详情各标签](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/member/detail/index.vue#L55)；[有效会员与教练绑定要求](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/backend/T1-RuoYi-Vue-Plus/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness/service/impl/FitnessTrainingService.java#L345)。

### 18 后台文案和术语

处理级别：清晰度。

首页仍显示一期工作台、租户、实时数据已接入、按首页设计文档等实现说明，并把 /fitness-… 路由作为快捷入口描述。确认收款要求手填 cash / wechat 等英文代码，不符合前台人员的日常语言。

建议：删除研发说明和路由，改为具体业务说明；支付方式用现金、微信转账、银行卡等选项。统一区分课程模板、排课场次、已购课包和训练计划。

代码依据：[首页研发说明](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/index.vue#L6)；[路由直接展示](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/index.vue#L72)；[英文支付方式输入](https://github.com/zw-03-doc/ai-t1-management/blob/ffea203230a830150323e6ccd8ecc2f0b0f06a32/frontend/T-plus-ui/src/views/fitness/order/index.vue#L130)。

## 建议的业务流程

| 管理对象 | 推荐操作顺序 |
| --- | --- |
| 门店 | 建立门店 → 在门店详情管理教练、课程与营业安排 → 查看该店会员和经营状态 |
| 教练 | 建档并建立登录账号 → 绑定门店 → 开通教练身份 → 查看学员与课表 → 停用或恢复时同步权限 |
| 会员 | 手机号查重并确认认领 → 建立唯一档案 → 分配门店 → 办理课包与教练关系 → 在同一会员工作台查看预约、签到和训练计划 |
| 个人课程 | 课程模板 → 公开或定向排课 → 选择学员及可用权益 → 预约 → 到场签到 → 确认扣次或扣款 → 结束或处理异常 |
| 训练计划 | 模板或自建 → 明确所属会员 → 编辑训练日与目标 → 保存草稿 → 选择开始日期和周期 → 暂存或完成训练 → 统计 → 续排或归档 |

课程模板描述上什么课，排课场次决定在哪家店、由谁、何时上；已购课包是会员权益；训练计划是会员的个人训练安排。训练计划顺延不应自动改约课时间。这四类概念应在菜单、按钮与详情中保持一致。

## 修复和验收顺序

先处理会员认领、停用生效、关联搜索、私教订单绑定和定向排课，避免身份、授权与受众错误；随后统一卡权益核销，补齐场次结束和异常恢复；再补训练计划续用、暂存、顺延与草稿保护；最后整理门店和会员工作台、中文状态及后台文案。

修复后至少完整验收一次：前台预建会员并开卡 → 本人认领 → 私教绑定 → 定向排课 → 预约选卡 → 会员签到或教练代签 → 管理员核销 → 场次结束。同时验收团课卡、储值卡、误标异常恢复、停用既有会话、训练中途保存、周期结束续用及快速切换会员，不能只以构建通过作为流程通过。
