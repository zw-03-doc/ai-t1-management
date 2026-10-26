# 训练计划、实际训练与权限

训练计划不等于约课。计划安排了哪天练什么，课程预约占用具体门店/教练/场次的名额；调整计划不会修改预约或签到。

## 数据与规则

- `fit_training_plan` 保存会员归属、作者身份、私有/公开免费状态及循环定义。循环长度包含休息日，必须逐日配置。默认三分化采用推、拉、腿、休息四天循环；五分化采用胸、背、腿、肩、手臂、休息、休息七天循环。目标重量默认留空。
- `fit_training_workout` 保存启动时生成的每一天、原定日期、当前日期和动作目标快照。日历日期统一按北京时间解释。一次可生成不超过 366 天，前端应允许用户选择循环次数。已有计划不能再次启动；再次开展可复制为新计划。
- 已启动计划允许编辑名称、动作要领、组数和目标重量；修改仅应用于尚未完成的日程。已完成训练的动作及目标快照保持原样。已启动计划不能改变循环长度，应复制新计划。
- 顺延将指定日期及之后的未完成训练和休息日整体向后移动，保留原定日期及训练顺序；已完成训练不移动，不自动补一份训练。移动结果若撞到已完成日程则拒绝，避免静默覆盖。
- `fit_training_set` 保存每组实际记录。目标值与实际值分开，`warmup` 为热身组、`working` 为正式组。重量允许 kg/lb，保留输入单位并用十进制精确换算 kg；100 lb = 45.359237 kg。用户可修改本次实际记录，整次替换与审计同一事务提交。
- 图表按每次训练和动作返回正式组最大重量、正式组总容量（kg × 次数）、正式组数及热身组数；热身组不计入正式组重量与容量。动作标识应保持稳定，不同器械或负重含义应使用不同标识。
- 所有变更先锁定所属计划，再写入日程/实际组/审计，避免启动、顺延、编辑及记录相互覆盖。数据库对计划序号和训练组序号设唯一键。
- `fit_training_audit` 记录创建、编辑、启动、顺延、实际训练记录修正及媒体上传；记录真实操作者与被操作会员。数据库与 Java 进程统一使用 Asia/Shanghai，审计时间按北京时间存储和展示。

## 访问范围

- 会员只可操作本人计划和训练记录。公开模板仅公开定义，不公开原会员身份、实际记录或统计。
- 教练可以使用微信端或网页端登录。同租户、同门店、未停用教练且会员有当前有效的绑定教练会员卡，方可查看/定制该会员计划。过期、停用或解除绑定即时失去权限。
- 管理员为超级管理员、租户管理员或明确拥有 `fitness:training:manage` 权限的后台账号。该权限授予其当前租户范围的训练管理和审计能力。
- 每条查询和修改显式带登录租户，不能从客户端指定其他租户。公开模板也限定当前租户；系统内置的两套模板按请求生成，无会员私有数据。

## 接口

以下接口统一返回现有的 `R` 结构，业务数据位于 `data`。均要求登录；网页和小程序共用接口。

| 方法和地址 | 数据/作用 |
|---|---|
| GET `/fitness/training/templates` | 默认三/五分化及当前租户公开免费定义；`templateId` 标识模板 |
| GET `/fitness/training/plans?memberId=` | 会员默认本人；教练需指定绑定会员；管理员可按会员过滤。最多最近 200 个 |
| GET `/fitness/training/plans/{planId}` | 计划详情，包含 `days` |
| POST `/fitness/training/plans` | 创建草稿，见下方定义 |
| PUT `/fitness/training/plans/{planId}` | 更新定义，不能转移所属会员 |
| POST `/fitness/training/plans/{planId}/start` | `{startDate:"2026-10-26",cycles:4}`；返回生成的日程 |
| POST `/fitness/training/plans/{planId}/shift` | `{fromDate:"2026-10-26",days:1}`；返回顺延后的日程 |
| GET `/fitness/training/plans/{planId}/workouts` | 日程列表，含 `workoutId/plannedDate/originalDate/title/status/exercises/sets` |
| PUT `/fitness/training/workouts/{workoutId}/log` | 整次保存实际组，见下方示例 |
| GET `/fitness/training/plans/{planId}/stats` | `{unit:"kg",completedWorkouts,points:[...]}`；点含 workoutId、date、exerciseKey、exerciseName、maxWorkingWeightKg、workingVolumeKg、workingSets、warmupSets |
| GET `/fitness/training/audit?planId=` | 仅管理员；最多最近 500 条 |
| POST `/fitness/training/media` | multipart 的 `planId` 和 `file`；先创建计划再上传 |
| GET `/fitness/training/media/{assetId}` | 受登录及计划权限控制的文件流 |

计划创建/编辑：

```json
{
  "memberId": 100,
  "name": "我的推拉腿",
  "description": "按四天循环",
  "visibility": "private",
  "cycleDays": 1,
  "days": [{
    "dayNumber": 1,
    "title": "推",
    "restDay": false,
    "exercises": [{
      "key": "bench",
      "name": "卧推",
      "instructions": "自己的动作要领",
      "imageUrls": [],
      "videoUrl": "",
      "targets": [
        {"setType":"warmup","reps":12,"weight":20,"unit":"kg"},
        {"setType":"working","reps":10,"weight":40,"unit":"kg","rpe":7}
      ]
    }]
  }]
}
```

示例为一个训练日的数据结构；多日循环时 `days` 数量必须等于 `cycleDays`。休息日配置 `restDay:true,exercises:[]`。会员可省略 `memberId`。

实际训练保存：

```json
{
  "performedDate":"2026-10-26",
  "sets":[
    {"exerciseKey":"bench","setType":"warmup","reps":12,"weight":20,"unit":"kg"},
    {"exerciseKey":"bench","setType":"working","reps":10,"weight":100,"unit":"lb","rpe":7.5}
  ]
}
```

动作名称由服务端从快照取得，客户端不能伪造其他动作。实际日期不得早于计划开始或晚于北京时间今天。

## 图片及短视频

上传返回 `{assetId,planId,url,mediaType,size}`。媒体地址为受保护的相对路径，不能直接作为永久公开链接。网页通过带认证头的 fetch 读取为 Blob URL；小程序通过带认证头的 `uni.downloadFile` 获取本地临时路径供图片/视频组件显示。公开计划的媒体仍需同租户登录，但不要求会员绑定。复制公开模板时，服务端把附件授权引用保存到新计划；源模板后来改为私有，不影响已经合法复制的计划附件，副本的权限随副本计划变化。附件引用不会授予源计划的实际记录或统计权限。

只接受文件头匹配的 JPEG、PNG、WebP 或 MP4，拒绝 SVG/HTML。图片限 10MB、视频限 30MB。文件名由服务器随机生成，原始文件名不参与路径。响应设置 `nosniff` 和 `no-store`，不会把私人附件缓存为公开资源。可引用外部 HTTPS 动作资料；服务端不抓取这些地址。

部署时设置 `FITNESS_MEDIA_PATH=/app/media` 并挂载持久卷。默认本地路径为 `./data/training-media`。Spring multipart 限制需至少 30MB/35MB，代理请求体上限至少 35MB。服务器解析 MP4 的 moov/mvhd 元数据，拒绝超过 60 秒、零时长或缺少有效时长的文件；支持版本 0/1 电影头，不会启动外部解码进程。未提供视频转码。

## 初始化与验证

执行 `backend/T1-RuoYi-Vue-Plus/script/sql/03-业务版本升级/V1.2.1_训练计划.sql`。脚本以 `CREATE TABLE IF NOT EXISTS` 和条件插入菜单执行，不删除旧业务数据。媒体目录需要备份，SQL 数据库备份不会包含附件本身。

定向测试命令（在后端 Maven 根目录）：

```text
mvn -pl ruoyi-admin -am test -Dtest=FitnessTraining*Test -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false -Dprofiles.active=training
```

测试覆盖 kg/lb 精度、循环/休息日完整性、跨月跨年顺延、格式伪装媒体、MP4 时长与失败上传清理、上传审计、公开模板附件复制授权、会员隔离、教练绑定、租户媒体隔离、启动日程生成、已完成快照保护、记录中非法动作拒绝。完整事务/行锁并发仍须在实际 MySQL 环境联调验证，单元测试不能替代上线前数据库验证。
