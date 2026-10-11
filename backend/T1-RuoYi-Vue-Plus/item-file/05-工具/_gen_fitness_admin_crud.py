from pathlib import Path

ROOT = Path(r"e:/work/2026/javaWeb-code/T1-management/backend/T1-RuoYi-Vue-Plus")
JAVA_BASE = ROOT / "ruoyi-modules/ruoyi-system/src/main/java/org/dromara/fitness"
RES_BASE = ROOT / "ruoyi-modules/ruoyi-system/src/main/resources/mapper/fitness"


modules = [
    {
        "key": "store",
        "title": "门店管理",
        "title_short": "门店",
        "path": "store",
        "permission": "fitness:store",
        "controller": "FitnessStoreController",
        "service": "IFitnessStoreService",
        "service_impl": "FitnessStoreServiceImpl",
        "mapper": "FitnessStoreMapper",
        "entity": "FitStore",
        "bo": "FitnessStoreBo",
        "vo": "FitnessStoreVo",
        "table": "fit_store",
        "id_field": "storeId",
        "id_column": "store_id",
        "status_field": "status",
        "status_column": "status",
        "status_message": "门店状态更新失败",
        "unique_checks": [
            {"field": "storeCode", "message": "门店编码已存在", "blankable": False},
        ],
        "entity_fields": [
            {"name": "storeId", "type": "Long", "comment": "门店ID", "id": True},
            {"name": "storeCode", "type": "String", "comment": "门店编码"},
            {"name": "storeName", "type": "String", "comment": "门店名称"},
            {"name": "storeShortName", "type": "String", "comment": "门店简称"},
            {"name": "contactName", "type": "String", "comment": "联系人"},
            {"name": "contactPhone", "type": "String", "comment": "联系电话"},
            {"name": "provinceCode", "type": "String", "comment": "省编码"},
            {"name": "cityCode", "type": "String", "comment": "市编码"},
            {"name": "districtCode", "type": "String", "comment": "区县编码"},
            {"name": "address", "type": "String", "comment": "详细地址"},
            {"name": "longitude", "type": "BigDecimal", "comment": "经度"},
            {"name": "latitude", "type": "BigDecimal", "comment": "纬度"},
            {"name": "businessHours", "type": "String", "comment": "营业时间"},
            {"name": "coverUrl", "type": "String", "comment": "门店封面图"},
            {"name": "status", "type": "String", "comment": "状态（0正常 1停用）"},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "version", "type": "Integer", "comment": "版本号", "version": True},
            {"name": "delFlag", "type": "String", "comment": "删除标记（0存在 2删除）", "logic": True},
        ],
        "bo_fields": [
            {"name": "storeId", "type": "Long", "comment": "门店ID", "edit_required": True},
            {"name": "storeCode", "type": "String", "comment": "门店编码", "required": True},
            {"name": "storeName", "type": "String", "comment": "门店名称", "required": True},
            {"name": "storeShortName", "type": "String", "comment": "门店简称"},
            {"name": "contactName", "type": "String", "comment": "联系人"},
            {"name": "contactPhone", "type": "String", "comment": "联系电话"},
            {"name": "provinceCode", "type": "String", "comment": "省编码"},
            {"name": "cityCode", "type": "String", "comment": "市编码"},
            {"name": "districtCode", "type": "String", "comment": "区县编码"},
            {"name": "address", "type": "String", "comment": "详细地址"},
            {"name": "longitude", "type": "BigDecimal", "comment": "经度"},
            {"name": "latitude", "type": "BigDecimal", "comment": "纬度"},
            {"name": "businessHours", "type": "String", "comment": "营业时间"},
            {"name": "coverUrl", "type": "String", "comment": "门店封面图"},
            {"name": "status", "type": "String", "comment": "状态（0正常 1停用）"},
            {"name": "remark", "type": "String", "comment": "备注"},
        ],
        "vo_fields": [
            {"name": "storeId", "type": "Long", "comment": "门店ID", "excel": "门店ID"},
            {"name": "storeCode", "type": "String", "comment": "门店编码", "excel": "门店编码"},
            {"name": "storeName", "type": "String", "comment": "门店名称", "excel": "门店名称"},
            {"name": "storeShortName", "type": "String", "comment": "门店简称", "excel": "门店简称"},
            {"name": "contactName", "type": "String", "comment": "联系人", "excel": "联系人"},
            {"name": "contactPhone", "type": "String", "comment": "联系电话", "excel": "联系电话"},
            {"name": "businessHours", "type": "String", "comment": "营业时间", "excel": "营业时间"},
            {"name": "status", "type": "String", "comment": "状态（0正常 1停用）", "excel": "状态", "status_dict": True},
            {"name": "createTime", "type": "Date", "comment": "创建时间", "excel": "创建时间"},
            {"name": "provinceCode", "type": "String", "comment": "省编码"},
            {"name": "cityCode", "type": "String", "comment": "市编码"},
            {"name": "districtCode", "type": "String", "comment": "区县编码"},
            {"name": "address", "type": "String", "comment": "详细地址"},
            {"name": "longitude", "type": "BigDecimal", "comment": "经度"},
            {"name": "latitude", "type": "BigDecimal", "comment": "纬度"},
            {"name": "coverUrl", "type": "String", "comment": "门店封面图"},
            {"name": "remark", "type": "String", "comment": "备注"},
        ],
        "joins": [],
        "select_columns": [
            "t.store_id AS storeId",
            "t.store_code AS storeCode",
            "t.store_name AS storeName",
            "t.store_short_name AS storeShortName",
            "t.contact_name AS contactName",
            "t.contact_phone AS contactPhone",
            "t.province_code AS provinceCode",
            "t.city_code AS cityCode",
            "t.district_code AS districtCode",
            "t.address AS address",
            "t.longitude AS longitude",
            "t.latitude AS latitude",
            "t.business_hours AS businessHours",
            "t.cover_url AS coverUrl",
            "t.status AS status",
            "t.remark AS remark",
            "t.create_time AS createTime",
        ],
        "where_lines": [
            "<if test=\"bo.storeName != null and bo.storeName != ''\">AND t.store_name LIKE CONCAT('%', #{bo.storeName}, '%')</if>",
            "<if test=\"bo.contactPhone != null and bo.contactPhone != ''\">AND t.contact_phone = #{bo.contactPhone}</if>",
            "<if test=\"bo.status != null and bo.status != ''\">AND t.status = #{bo.status}</if>",
        ],
        "order_by": "ORDER BY t.create_time DESC, t.store_id DESC",
        "detail_method": "selectStoreById",
        "page_method": "selectPageStoreList",
        "list_method": "selectStoreList",
    },
    {
        "key": "member",
        "title": "会员管理",
        "title_short": "会员",
        "path": "member",
        "permission": "fitness:member",
        "controller": "FitnessMemberController",
        "service": "IFitnessMemberService",
        "service_impl": "FitnessMemberServiceImpl",
        "mapper": "FitnessMemberMapper",
        "entity": "FitMember",
        "bo": "FitnessMemberBo",
        "vo": "FitnessMemberVo",
        "table": "fit_member",
        "id_field": "memberId",
        "id_column": "member_id",
        "unique_checks": [
            {"field": "memberNo", "message": "会员编号已存在", "blankable": False},
            {"field": "mobile", "message": "手机号已存在", "blankable": True},
        ],
        "entity_fields": [
            {"name": "memberId", "type": "Long", "comment": "会员ID", "id": True},
            {"name": "memberNo", "type": "String", "comment": "会员编号"},
            {"name": "homeStoreId", "type": "Long", "comment": "归属门店ID"},
            {"name": "mobile", "type": "String", "comment": "手机号"},
            {"name": "realName", "type": "String", "comment": "真实姓名"},
            {"name": "nickname", "type": "String", "comment": "昵称"},
            {"name": "gender", "type": "String", "comment": "性别（0未知 1男 2女）"},
            {"name": "birthday", "type": "Date", "comment": "生日"},
            {"name": "avatarUrl", "type": "String", "comment": "头像地址"},
            {"name": "wxOpenid", "type": "String", "comment": "微信openid"},
            {"name": "wxUnionid", "type": "String", "comment": "微信unionid"},
            {"name": "registerSource", "type": "String", "comment": "注册来源"},
            {"name": "memberStatus", "type": "String", "comment": "会员状态"},
            {"name": "lastStoreId", "type": "Long", "comment": "最近消费门店ID"},
            {"name": "lastVisitTime", "type": "Date", "comment": "最近到店时间"},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "version", "type": "Integer", "comment": "版本号", "version": True},
            {"name": "delFlag", "type": "String", "comment": "删除标记（0存在 2删除）", "logic": True},
        ],
        "bo_fields": [
            {"name": "memberId", "type": "Long", "comment": "会员ID", "edit_required": True},
            {"name": "memberNo", "type": "String", "comment": "会员编号", "required": True},
            {"name": "homeStoreId", "type": "Long", "comment": "归属门店ID"},
            {"name": "mobile", "type": "String", "comment": "手机号"},
            {"name": "realName", "type": "String", "comment": "真实姓名"},
            {"name": "nickname", "type": "String", "comment": "昵称"},
            {"name": "gender", "type": "String", "comment": "性别（0未知 1男 2女）"},
            {"name": "birthday", "type": "Date", "comment": "生日"},
            {"name": "avatarUrl", "type": "String", "comment": "头像地址"},
            {"name": "registerSource", "type": "String", "comment": "注册来源"},
            {"name": "memberStatus", "type": "String", "comment": "会员状态"},
            {"name": "lastStoreId", "type": "Long", "comment": "最近消费门店ID"},
            {"name": "lastVisitTime", "type": "Date", "comment": "最近到店时间"},
            {"name": "remark", "type": "String", "comment": "备注"},
        ],
        "vo_fields": [
            {"name": "memberId", "type": "Long", "comment": "会员ID", "excel": "会员ID"},
            {"name": "memberNo", "type": "String", "comment": "会员编号", "excel": "会员编号"},
            {"name": "realName", "type": "String", "comment": "真实姓名", "excel": "真实姓名"},
            {"name": "nickname", "type": "String", "comment": "昵称", "excel": "昵称"},
            {"name": "mobile", "type": "String", "comment": "手机号", "excel": "手机号"},
            {"name": "gender", "type": "String", "comment": "性别（0未知 1男 2女）", "excel": "性别"},
            {"name": "homeStoreId", "type": "Long", "comment": "归属门店ID"},
            {"name": "homeStoreName", "type": "String", "comment": "归属门店名称", "excel": "归属门店"},
            {"name": "memberStatus", "type": "String", "comment": "会员状态", "excel": "会员状态"},
            {"name": "lastStoreId", "type": "Long", "comment": "最近消费门店ID"},
            {"name": "lastStoreName", "type": "String", "comment": "最近消费门店名称"},
            {"name": "lastVisitTime", "type": "Date", "comment": "最近到店时间", "excel": "最近到店时间"},
            {"name": "birthday", "type": "Date", "comment": "生日"},
            {"name": "avatarUrl", "type": "String", "comment": "头像地址"},
            {"name": "registerSource", "type": "String", "comment": "注册来源"},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "createTime", "type": "Date", "comment": "创建时间", "excel": "创建时间"},
        ],
        "joins": [
            "LEFT JOIN fit_store hs ON hs.store_id = t.home_store_id AND hs.del_flag = '0'",
            "LEFT JOIN fit_store ls ON ls.store_id = t.last_store_id AND ls.del_flag = '0'",
        ],
        "select_columns": [
            "t.member_id AS memberId",
            "t.member_no AS memberNo",
            "t.home_store_id AS homeStoreId",
            "hs.store_name AS homeStoreName",
            "t.mobile AS mobile",
            "t.real_name AS realName",
            "t.nickname AS nickname",
            "t.gender AS gender",
            "t.birthday AS birthday",
            "t.avatar_url AS avatarUrl",
            "t.register_source AS registerSource",
            "t.member_status AS memberStatus",
            "t.last_store_id AS lastStoreId",
            "ls.store_name AS lastStoreName",
            "t.last_visit_time AS lastVisitTime",
            "t.remark AS remark",
            "t.create_time AS createTime",
        ],
        "where_lines": [
            "<if test=\"bo.memberNo != null and bo.memberNo != ''\">AND t.member_no LIKE CONCAT('%', #{bo.memberNo}, '%')</if>",
            "<if test=\"bo.realName != null and bo.realName != ''\">AND t.real_name LIKE CONCAT('%', #{bo.realName}, '%')</if>",
            "<if test=\"bo.mobile != null and bo.mobile != ''\">AND t.mobile LIKE CONCAT('%', #{bo.mobile}, '%')</if>",
            "<if test=\"bo.homeStoreId != null\">AND t.home_store_id = #{bo.homeStoreId}</if>",
            "<if test=\"bo.memberStatus != null and bo.memberStatus != ''\">AND t.member_status = #{bo.memberStatus}</if>",
        ],
        "order_by": "ORDER BY t.create_time DESC, t.member_id DESC",
        "detail_method": "selectMemberById",
        "page_method": "selectPageMemberList",
        "list_method": "selectMemberList",
    },
    {
        "key": "coach",
        "title": "教练管理",
        "title_short": "教练",
        "path": "coach",
        "permission": "fitness:coach",
        "controller": "FitnessCoachController",
        "service": "IFitnessCoachService",
        "service_impl": "FitnessCoachServiceImpl",
        "mapper": "FitnessCoachMapper",
        "entity": "FitCoach",
        "bo": "FitnessCoachBo",
        "vo": "FitnessCoachVo",
        "table": "fit_coach",
        "id_field": "coachId",
        "id_column": "coach_id",
        "unique_checks": [
            {"field": "userId", "message": "后台账号已绑定其他教练", "blankable": True},
            {"field": "mobile", "message": "手机号已存在", "blankable": True},
        ],
        "entity_fields": [
            {"name": "coachId", "type": "Long", "comment": "教练ID", "id": True},
            {"name": "userId", "type": "Long", "comment": "关联后台用户ID"},
            {"name": "storeId", "type": "Long", "comment": "所属门店ID"},
            {"name": "coachName", "type": "String", "comment": "教练姓名"},
            {"name": "mobile", "type": "String", "comment": "手机号"},
            {"name": "gender", "type": "String", "comment": "性别（0未知 1男 2女）"},
            {"name": "avatarUrl", "type": "String", "comment": "头像地址"},
            {"name": "skillTags", "type": "String", "comment": "擅长标签"},
            {"name": "levelCode", "type": "String", "comment": "教练等级"},
            {"name": "intro", "type": "String", "comment": "教练简介"},
            {"name": "hireStatus", "type": "String", "comment": "任职状态（0在职 1停用）"},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "version", "type": "Integer", "comment": "版本号", "version": True},
            {"name": "delFlag", "type": "String", "comment": "删除标记（0存在 2删除）", "logic": True},
        ],
        "bo_fields": [
            {"name": "coachId", "type": "Long", "comment": "教练ID", "edit_required": True},
            {"name": "userId", "type": "Long", "comment": "关联后台用户ID"},
            {"name": "storeId", "type": "Long", "comment": "所属门店ID"},
            {"name": "coachName", "type": "String", "comment": "教练姓名", "required": True},
            {"name": "mobile", "type": "String", "comment": "手机号"},
            {"name": "gender", "type": "String", "comment": "性别（0未知 1男 2女）"},
            {"name": "avatarUrl", "type": "String", "comment": "头像地址"},
            {"name": "skillTags", "type": "String", "comment": "擅长标签"},
            {"name": "levelCode", "type": "String", "comment": "教练等级"},
            {"name": "intro", "type": "String", "comment": "教练简介"},
            {"name": "hireStatus", "type": "String", "comment": "任职状态（0在职 1停用）"},
            {"name": "remark", "type": "String", "comment": "备注"},
        ],
        "vo_fields": [
            {"name": "coachId", "type": "Long", "comment": "教练ID", "excel": "教练ID"},
            {"name": "userId", "type": "Long", "comment": "关联后台用户ID"},
            {"name": "userName", "type": "String", "comment": "后台账号", "excel": "后台账号"},
            {"name": "nickName", "type": "String", "comment": "后台昵称"},
            {"name": "storeId", "type": "Long", "comment": "所属门店ID"},
            {"name": "storeName", "type": "String", "comment": "所属门店名称", "excel": "所属门店"},
            {"name": "coachName", "type": "String", "comment": "教练姓名", "excel": "教练姓名"},
            {"name": "mobile", "type": "String", "comment": "手机号", "excel": "手机号"},
            {"name": "gender", "type": "String", "comment": "性别（0未知 1男 2女）", "excel": "性别"},
            {"name": "avatarUrl", "type": "String", "comment": "头像地址"},
            {"name": "skillTags", "type": "String", "comment": "擅长标签", "excel": "擅长标签"},
            {"name": "levelCode", "type": "String", "comment": "教练等级", "excel": "教练等级"},
            {"name": "intro", "type": "String", "comment": "教练简介"},
            {"name": "hireStatus", "type": "String", "comment": "任职状态（0在职 1停用）", "excel": "任职状态"},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "createTime", "type": "Date", "comment": "创建时间", "excel": "创建时间"},
        ],
        "joins": [
            "LEFT JOIN fit_store st ON st.store_id = t.store_id AND st.del_flag = '0'",
            "LEFT JOIN sys_user u ON u.user_id = t.user_id AND u.del_flag = '0'",
        ],
        "select_columns": [
            "t.coach_id AS coachId",
            "t.user_id AS userId",
            "u.user_name AS userName",
            "u.nick_name AS nickName",
            "t.store_id AS storeId",
            "st.store_name AS storeName",
            "t.coach_name AS coachName",
            "t.mobile AS mobile",
            "t.gender AS gender",
            "t.avatar_url AS avatarUrl",
            "t.skill_tags AS skillTags",
            "t.level_code AS levelCode",
            "t.intro AS intro",
            "t.hire_status AS hireStatus",
            "t.remark AS remark",
            "t.create_time AS createTime",
        ],
        "where_lines": [
            "<if test=\"bo.coachName != null and bo.coachName != ''\">AND t.coach_name LIKE CONCAT('%', #{bo.coachName}, '%')</if>",
            "<if test=\"bo.mobile != null and bo.mobile != ''\">AND t.mobile LIKE CONCAT('%', #{bo.mobile}, '%')</if>",
            "<if test=\"bo.storeId != null\">AND t.store_id = #{bo.storeId}</if>",
            "<if test=\"bo.hireStatus != null and bo.hireStatus != ''\">AND t.hire_status = #{bo.hireStatus}</if>",
        ],
        "order_by": "ORDER BY t.create_time DESC, t.coach_id DESC",
        "detail_method": "selectCoachById",
        "page_method": "selectPageCoachList",
        "list_method": "selectCoachList",
    },
    {
        "key": "course",
        "title": "课程管理",
        "title_short": "课程",
        "path": "course",
        "permission": "fitness:course",
        "controller": "FitnessCourseController",
        "service": "IFitnessCourseService",
        "service_impl": "FitnessCourseServiceImpl",
        "mapper": "FitnessCourseMapper",
        "entity": "FitCourse",
        "bo": "FitnessCourseBo",
        "vo": "FitnessCourseVo",
        "table": "fit_course",
        "id_field": "courseId",
        "id_column": "course_id",
        "status_field": "status",
        "status_column": "status",
        "status_message": "课程状态更新失败",
        "unique_checks": [],
        "entity_fields": [
            {"name": "courseId", "type": "Long", "comment": "课程ID", "id": True},
            {"name": "courseName", "type": "String", "comment": "课程名称"},
            {"name": "courseType", "type": "String", "comment": "课程类型"},
            {"name": "storeId", "type": "Long", "comment": "归属门店ID"},
            {"name": "defaultCoachId", "type": "Long", "comment": "默认教练ID"},
            {"name": "durationMinutes", "type": "Integer", "comment": "课程时长（分钟）"},
            {"name": "capacity", "type": "Integer", "comment": "默认人数上限"},
            {"name": "coverUrl", "type": "String", "comment": "课程封面图"},
            {"name": "description", "type": "String", "comment": "课程介绍"},
            {"name": "supportCardType", "type": "String", "comment": "支持卡项类型"},
            {"name": "status", "type": "String", "comment": "状态（0上架 1下架）"},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "version", "type": "Integer", "comment": "版本号", "version": True},
            {"name": "delFlag", "type": "String", "comment": "删除标记（0存在 2删除）", "logic": True},
        ],
        "bo_fields": [
            {"name": "courseId", "type": "Long", "comment": "课程ID", "edit_required": True},
            {"name": "courseName", "type": "String", "comment": "课程名称", "required": True},
            {"name": "courseType", "type": "String", "comment": "课程类型"},
            {"name": "storeId", "type": "Long", "comment": "归属门店ID"},
            {"name": "defaultCoachId", "type": "Long", "comment": "默认教练ID"},
            {"name": "durationMinutes", "type": "Integer", "comment": "课程时长（分钟）"},
            {"name": "capacity", "type": "Integer", "comment": "默认人数上限"},
            {"name": "coverUrl", "type": "String", "comment": "课程封面图"},
            {"name": "description", "type": "String", "comment": "课程介绍"},
            {"name": "supportCardType", "type": "String", "comment": "支持卡项类型"},
            {"name": "status", "type": "String", "comment": "状态（0上架 1下架）"},
            {"name": "remark", "type": "String", "comment": "备注"},
        ],
        "vo_fields": [
            {"name": "courseId", "type": "Long", "comment": "课程ID", "excel": "课程ID"},
            {"name": "courseName", "type": "String", "comment": "课程名称", "excel": "课程名称"},
            {"name": "courseType", "type": "String", "comment": "课程类型", "excel": "课程类型"},
            {"name": "storeId", "type": "Long", "comment": "归属门店ID"},
            {"name": "storeName", "type": "String", "comment": "归属门店名称", "excel": "归属门店"},
            {"name": "defaultCoachId", "type": "Long", "comment": "默认教练ID"},
            {"name": "coachName", "type": "String", "comment": "默认教练名称", "excel": "默认教练"},
            {"name": "durationMinutes", "type": "Integer", "comment": "课程时长（分钟）", "excel": "课程时长"},
            {"name": "capacity", "type": "Integer", "comment": "默认人数上限", "excel": "人数上限"},
            {"name": "coverUrl", "type": "String", "comment": "课程封面图"},
            {"name": "description", "type": "String", "comment": "课程介绍"},
            {"name": "supportCardType", "type": "String", "comment": "支持卡项类型", "excel": "支持卡项类型"},
            {"name": "status", "type": "String", "comment": "状态（0上架 1下架）", "excel": "状态", "status_dict": True},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "createTime", "type": "Date", "comment": "创建时间", "excel": "创建时间"},
        ],
        "joins": [
            "LEFT JOIN fit_store st ON st.store_id = t.store_id AND st.del_flag = '0'",
            "LEFT JOIN fit_coach coach ON coach.coach_id = t.default_coach_id AND coach.del_flag = '0'",
        ],
        "select_columns": [
            "t.course_id AS courseId",
            "t.course_name AS courseName",
            "t.course_type AS courseType",
            "t.store_id AS storeId",
            "st.store_name AS storeName",
            "t.default_coach_id AS defaultCoachId",
            "coach.coach_name AS coachName",
            "t.duration_minutes AS durationMinutes",
            "t.capacity AS capacity",
            "t.cover_url AS coverUrl",
            "t.description AS description",
            "t.support_card_type AS supportCardType",
            "t.status AS status",
            "t.remark AS remark",
            "t.create_time AS createTime",
        ],
        "where_lines": [
            "<if test=\"bo.courseName != null and bo.courseName != ''\">AND t.course_name LIKE CONCAT('%', #{bo.courseName}, '%')</if>",
            "<if test=\"bo.courseType != null and bo.courseType != ''\">AND t.course_type = #{bo.courseType}</if>",
            "<if test=\"bo.storeId != null\">AND t.store_id = #{bo.storeId}</if>",
            "<if test=\"bo.status != null and bo.status != ''\">AND t.status = #{bo.status}</if>",
        ],
        "order_by": "ORDER BY t.create_time DESC, t.course_id DESC",
        "detail_method": "selectCourseById",
        "page_method": "selectPageCourseList",
        "list_method": "selectCourseList",
    },
    {
        "key": "cardplan",
        "title": "卡项方案管理",
        "title_short": "卡项方案",
        "path": "cardPlan",
        "permission": "fitness:cardPlan",
        "controller": "FitnessCardPlanController",
        "service": "IFitnessCardPlanService",
        "service_impl": "FitnessCardPlanServiceImpl",
        "mapper": "FitnessCardPlanMapper",
        "entity": "FitCardPlan",
        "bo": "FitnessCardPlanBo",
        "vo": "FitnessCardPlanVo",
        "table": "fit_card_plan",
        "id_field": "planId",
        "id_column": "plan_id",
        "status_field": "status",
        "status_column": "status",
        "status_message": "卡项方案状态更新失败",
        "unique_checks": [],
        "entity_fields": [
            {"name": "planId", "type": "Long", "comment": "卡项方案ID", "id": True},
            {"name": "storeId", "type": "Long", "comment": "适用门店ID"},
            {"name": "planName", "type": "String", "comment": "卡项名称"},
            {"name": "planType", "type": "String", "comment": "卡项类型"},
            {"name": "salePrice", "type": "BigDecimal", "comment": "销售价"},
            {"name": "originalPrice", "type": "BigDecimal", "comment": "原价"},
            {"name": "validDays", "type": "Integer", "comment": "有效天数"},
            {"name": "totalTimes", "type": "Integer", "comment": "总次数"},
            {"name": "giftTimes", "type": "Integer", "comment": "赠送次数"},
            {"name": "storedAmount", "type": "BigDecimal", "comment": "储值金额"},
            {"name": "supportCourseType", "type": "String", "comment": "支持课程类型"},
            {"name": "isPrivateBindCoach", "type": "String", "comment": "是否绑定教练（0否 1是）"},
            {"name": "sortNo", "type": "Integer", "comment": "排序号"},
            {"name": "status", "type": "String", "comment": "状态（0上架 1下架）"},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "version", "type": "Integer", "comment": "版本号", "version": True},
            {"name": "delFlag", "type": "String", "comment": "删除标记（0存在 2删除）", "logic": True},
        ],
        "bo_fields": [
            {"name": "planId", "type": "Long", "comment": "卡项方案ID", "edit_required": True},
            {"name": "storeId", "type": "Long", "comment": "适用门店ID"},
            {"name": "planName", "type": "String", "comment": "卡项名称", "required": True},
            {"name": "planType", "type": "String", "comment": "卡项类型", "required": True},
            {"name": "salePrice", "type": "BigDecimal", "comment": "销售价"},
            {"name": "originalPrice", "type": "BigDecimal", "comment": "原价"},
            {"name": "validDays", "type": "Integer", "comment": "有效天数"},
            {"name": "totalTimes", "type": "Integer", "comment": "总次数"},
            {"name": "giftTimes", "type": "Integer", "comment": "赠送次数"},
            {"name": "storedAmount", "type": "BigDecimal", "comment": "储值金额"},
            {"name": "supportCourseType", "type": "String", "comment": "支持课程类型"},
            {"name": "isPrivateBindCoach", "type": "String", "comment": "是否绑定教练（0否 1是）"},
            {"name": "sortNo", "type": "Integer", "comment": "排序号"},
            {"name": "status", "type": "String", "comment": "状态（0上架 1下架）"},
            {"name": "remark", "type": "String", "comment": "备注"},
        ],
        "vo_fields": [
            {"name": "planId", "type": "Long", "comment": "卡项方案ID", "excel": "方案ID"},
            {"name": "storeId", "type": "Long", "comment": "适用门店ID"},
            {"name": "storeName", "type": "String", "comment": "适用门店名称", "excel": "适用门店"},
            {"name": "planName", "type": "String", "comment": "卡项名称", "excel": "卡项名称"},
            {"name": "planType", "type": "String", "comment": "卡项类型", "excel": "卡项类型"},
            {"name": "salePrice", "type": "BigDecimal", "comment": "销售价", "excel": "销售价"},
            {"name": "originalPrice", "type": "BigDecimal", "comment": "原价", "excel": "原价"},
            {"name": "validDays", "type": "Integer", "comment": "有效天数", "excel": "有效天数"},
            {"name": "totalTimes", "type": "Integer", "comment": "总次数", "excel": "总次数"},
            {"name": "giftTimes", "type": "Integer", "comment": "赠送次数", "excel": "赠送次数"},
            {"name": "storedAmount", "type": "BigDecimal", "comment": "储值金额", "excel": "储值金额"},
            {"name": "supportCourseType", "type": "String", "comment": "支持课程类型", "excel": "支持课程类型"},
            {"name": "isPrivateBindCoach", "type": "String", "comment": "是否绑定教练（0否 1是）", "excel": "绑定教练"},
            {"name": "sortNo", "type": "Integer", "comment": "排序号", "excel": "排序号"},
            {"name": "status", "type": "String", "comment": "状态（0上架 1下架）", "excel": "状态", "status_dict": True},
            {"name": "remark", "type": "String", "comment": "备注"},
            {"name": "createTime", "type": "Date", "comment": "创建时间", "excel": "创建时间"},
        ],
        "joins": [
            "LEFT JOIN fit_store st ON st.store_id = t.store_id AND st.del_flag = '0'",
        ],
        "select_columns": [
            "t.plan_id AS planId",
            "t.store_id AS storeId",
            "st.store_name AS storeName",
            "t.plan_name AS planName",
            "t.plan_type AS planType",
            "t.sale_price AS salePrice",
            "t.original_price AS originalPrice",
            "t.valid_days AS validDays",
            "t.total_times AS totalTimes",
            "t.gift_times AS giftTimes",
            "t.stored_amount AS storedAmount",
            "t.support_course_type AS supportCourseType",
            "t.is_private_bind_coach AS isPrivateBindCoach",
            "t.sort_no AS sortNo",
            "t.status AS status",
            "t.remark AS remark",
            "t.create_time AS createTime",
        ],
        "where_lines": [
            "<if test=\"bo.planName != null and bo.planName != ''\">AND t.plan_name LIKE CONCAT('%', #{bo.planName}, '%')</if>",
            "<if test=\"bo.planType != null and bo.planType != ''\">AND t.plan_type = #{bo.planType}</if>",
            "<if test=\"bo.storeId != null\">AND t.store_id = #{bo.storeId}</if>",
            "<if test=\"bo.status != null and bo.status != ''\">AND t.status = #{bo.status}</if>",
        ],
        "order_by": "ORDER BY t.sort_no ASC, t.create_time DESC, t.plan_id DESC",
        "detail_method": "selectCardPlanById",
        "page_method": "selectPageCardPlanList",
        "list_method": "selectCardPlanList",
    },
]


COMMON_ENTITY_IMPORTS = {
    "Long": None,
    "String": None,
    "Integer": None,
    "BigDecimal": "java.math.BigDecimal",
    "Date": "java.util.Date",
}

COMMON_VO_IMPORTS = {
    "Long": None,
    "String": None,
    "Integer": None,
    "BigDecimal": "java.math.BigDecimal",
    "Date": "java.util.Date",
}

COMMON_BO_IMPORTS = COMMON_VO_IMPORTS.copy()


def camel_to_words(name: str) -> str:
    return name[0].lower() + name[1:]



def write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding='utf-8')


def build_entity(module):
    imports = {
        "com.baomidou.mybatisplus.annotation.TableId",
        "com.baomidou.mybatisplus.annotation.TableLogic",
        "com.baomidou.mybatisplus.annotation.TableName",
        "com.baomidou.mybatisplus.annotation.Version",
        "lombok.Data",
        "lombok.EqualsAndHashCode",
        "org.dromara.common.tenant.core.TenantEntity",
        "java.io.Serial",
    }
    for field in module["entity_fields"]:
        imp = COMMON_ENTITY_IMPORTS.get(field["type"])
        if imp:
            imports.add(imp)
    lines = [
        "package org.dromara.fitness.domain;",
        "",
    ]
    for item in sorted(imports):
        lines.append(f"import {item};")
    lines += [
        "",
        "/**",
        f" * {module['title_short']}实体对象 {module['table']}",
        " *",
        " * @author CodeBuddy",
        " */",
        "@Data",
        "@EqualsAndHashCode(callSuper = true)",
        f"@TableName(\"{module['table']}\")",
        f"public class {module['entity']} extends TenantEntity {{",
        "",
        "    @Serial",
        "    private static final long serialVersionUID = 1L;",
        "",
    ]
    for field in module["entity_fields"]:
        lines.append("    /**")
        lines.append(f"     * {field['comment']}")
        lines.append("     */")
        if field.get("id"):
            lines.append(f"    @TableId(value = \"{module['id_column']}\")")
        if field.get("logic"):
            lines.append('    @TableLogic(value = "0", delval = "2")')
        if field.get("version"):
            lines.append("    @Version")
        lines.append(f"    private {field['type']} {field['name']};")
        lines.append("")
    lines.append("}")
    return "\n".join(lines) + "\n"


def build_bo(module):
    imports = {
        "io.github.linpeilie.annotations.AutoMapper",
        "jakarta.validation.constraints.NotBlank",
        "jakarta.validation.constraints.NotNull",
        "lombok.Data",
        "lombok.EqualsAndHashCode",
        "org.dromara.common.core.validate.AddGroup",
        "org.dromara.common.core.validate.EditGroup",
        "org.dromara.common.mybatis.core.domain.BaseEntity",
        f"org.dromara.fitness.domain.{module['entity']}",
    }
    for field in module["bo_fields"]:
        imp = COMMON_BO_IMPORTS.get(field["type"])
        if imp:
            imports.add(imp)
    lines = [
        f"package org.dromara.fitness.domain.bo;",
        "",
    ]
    for item in sorted(imports):
        lines.append(f"import {item};")
    lines += [
        "",
        "/**",
        f" * {module['title_short']}业务对象 {module['table']}",
        " *",
        " * @author CodeBuddy",
        " */",
        "@Data",
        "@EqualsAndHashCode(callSuper = true)",
        f"@AutoMapper(target = {module['entity']}.class, reverseConvertGenerate = false)",
        f"public class {module['bo']} extends BaseEntity {{",
        "",
    ]
    for field in module["bo_fields"]:
        lines.append("    /**")
        lines.append(f"     * {field['comment']}")
        lines.append("     */")
        if field.get("edit_required"):
            lines.append(f"    @NotNull(message = \"{field['comment']}不能为空\", groups = {{ EditGroup.class }})")
        if field.get("required"):
            lines.append(f"    @NotBlank(message = \"{field['comment']}不能为空\", groups = {{ AddGroup.class, EditGroup.class }})")
        lines.append(f"    private {field['type']} {field['name']};")
        lines.append("")
    lines.append("}")
    return "\n".join(lines) + "\n"


def build_vo(module):
    imports = {
        "cn.idev.excel.annotation.ExcelIgnoreUnannotated",
        "cn.idev.excel.annotation.ExcelProperty",
        "io.github.linpeilie.annotations.AutoMapper",
        "lombok.Data",
        f"org.dromara.fitness.domain.{module['entity']}",
        "java.io.Serial",
        "java.io.Serializable",
    }
    use_status_dict = any(field.get("status_dict") for field in module["vo_fields"])
    if use_status_dict:
        imports.add("org.dromara.common.excel.annotation.ExcelDictFormat")
        imports.add("org.dromara.common.excel.convert.ExcelDictConvert")
    for field in module["vo_fields"]:
        imp = COMMON_VO_IMPORTS.get(field["type"])
        if imp:
            imports.add(imp)
    lines = [
        f"package org.dromara.fitness.domain.vo;",
        "",
    ]
    for item in sorted(imports):
        lines.append(f"import {item};")
    lines += [
        "",
        "/**",
        f" * {module['title_short']}视图对象 {module['table']}",
        " *",
        " * @author CodeBuddy",
        " */",
        "@Data",
        "@ExcelIgnoreUnannotated",
        f"@AutoMapper(target = {module['entity']}.class)",
        f"public class {module['vo']} implements Serializable {{",
        "",
        "    @Serial",
        "    private static final long serialVersionUID = 1L;",
        "",
    ]
    for field in module["vo_fields"]:
        lines.append("    /**")
        lines.append(f"     * {field['comment']}")
        lines.append("     */")
        if field.get("excel"):
            if field.get("status_dict"):
                lines.append(f"    @ExcelProperty(value = \"{field['excel']}\", converter = ExcelDictConvert.class)")
                lines.append('    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")')
            else:
                lines.append(f"    @ExcelProperty(value = \"{field['excel']}\")")
        lines.append(f"    private {field['type']} {field['name']};")
        lines.append("")
    lines.append("}")
    return "\n".join(lines) + "\n"


def build_mapper(module):
    entity = module['entity']
    vo = module['vo']
    mapper = module['mapper']
    bo = module['bo']
    page_method = module['page_method']
    list_method = module['list_method']
    detail_method = module['detail_method']
    lines = [
        "package org.dromara.fitness.mapper;",
        "",
        "import com.baomidou.mybatisplus.extension.plugins.pagination.Page;",
        "import org.apache.ibatis.annotations.Param;",
        "import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;",
        f"import org.dromara.fitness.domain.{entity};",
        f"import org.dromara.fitness.domain.bo.{bo};",
        f"import org.dromara.fitness.domain.vo.{vo};",
        "",
        "/**",
        f" * {module['title_short']} Mapper 接口",
        " *",
        " * @author CodeBuddy",
        " */",
        f"public interface {mapper} extends BaseMapperPlus<{entity}, {vo}> {{",
        "",
        f"    Page<{vo}> {page_method}(@Param(\"page\") Page<{entity}> page, @Param(\"bo\") {bo} bo);",
        "",
        f"    java.util.List<{vo}> {list_method}(@Param(\"bo\") {bo} bo);",
        "",
        f"    {vo} {detail_method}(@Param(\"{module['id_field']}\") Long {module['id_field']});",
        "",
        "}",
    ]
    return "\n".join(lines) + "\n"


def build_service(module):
    lines = [
        "package org.dromara.fitness.service;",
        "",
        "import org.dromara.common.mybatis.core.page.PageQuery;",
        "import org.dromara.common.mybatis.core.page.TableDataInfo;",
        f"import org.dromara.fitness.domain.bo.{module['bo']};",
        f"import org.dromara.fitness.domain.vo.{module['vo']};",
        "",
        "import java.util.Collection;",
        "import java.util.List;",
        "",
        "/**",
        f" * {module['title_short']} Service 接口",
        " *",
        " * @author CodeBuddy",
        " */",
        f"public interface {module['service']} {{",
        "",
        f"    {module['vo']} queryById(Long {module['id_field']});",
        "",
        f"    TableDataInfo<{module['vo']}> queryPageList({module['bo']} bo, PageQuery pageQuery);",
        "",
        f"    List<{module['vo']}> queryList({module['bo']} bo);",
        "",
        f"    Boolean insertByBo({module['bo']} bo);",
        "",
        f"    Boolean updateByBo({module['bo']} bo);",
        "",
    ]
    if module.get('status_field'):
        lines += [
            f"    Boolean updateStatus(Long {module['id_field']}, String {module['status_field']});",
            "",
        ]
    lines += [
        f"    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);",
        "",
        "}",
    ]
    return "\n".join(lines) + "\n"


def unique_check_lines(module):
    entity = module['entity']
    field_type_map = {item['name']: item['type'] for item in module.get('entity_fields', [])}
    lines = []
    for check in module.get('unique_checks', []):
        field = check['field']
        field_cap = field[0].upper() + field[1:]
        id_cap = module['id_field'][0].upper() + module['id_field'][1:]
        field_type = field_type_map.get(field, 'String')
        guard = None
        if check.get('blankable'):
            if field_type == 'String':
                guard = f"StringUtils.isNotBlank(entity.get{field_cap}())"
            else:
                guard = f"entity.get{field_cap}() != null"
        expr = f"baseMapper.exists(new LambdaQueryWrapper<{entity}>()"
        expr += f".eq({entity}::get{field_cap}, entity.get{field_cap}())"
        expr += f".ne(entity.get{id_cap}() != null, {entity}::get{id_cap}, entity.get{id_cap}())"
        expr += ")"
        if guard:
            lines.append(f"        if ({guard} && {expr}) {{")
        else:
            lines.append(f"        if ({expr}) {{")
        lines.append(f"            throw new ServiceException(\"{check['message']}\");")
        lines.append("        }")
    return lines




def build_service_impl(module):
    entity = module['entity']
    mapper = module['mapper']
    bo = module['bo']
    vo = module['vo']
    id_field = module['id_field']
    status_field = module.get('status_field')
    lines = [
        "package org.dromara.fitness.service.impl;",
        "",
        "import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;",
        "import lombok.RequiredArgsConstructor;",
        "import org.dromara.common.core.constant.TenantConstants;",
        "import org.dromara.common.core.exception.ServiceException;",
        "import org.dromara.common.core.utils.MapstructUtils;",
        "import org.dromara.common.core.utils.StringUtils;",
        "import org.dromara.common.mybatis.core.page.PageQuery;",
        "import org.dromara.common.mybatis.core.page.TableDataInfo;",
        "import org.dromara.common.mybatis.utils.IdGeneratorUtil;",
        "import org.dromara.common.tenant.helper.TenantHelper;",
        f"import org.dromara.fitness.domain.{entity};",
        f"import org.dromara.fitness.domain.bo.{bo};",
        f"import org.dromara.fitness.domain.vo.{vo};",
        f"import org.dromara.fitness.mapper.{mapper};",
        f"import org.dromara.fitness.service.{module['service']};",
        "import org.springframework.stereotype.Service;",
        "",
        "import java.util.Collection;",
        "import java.util.List;",
        "",
        "/**",
        f" * {module['title_short']} Service 实现",
        " *",
        " * @author CodeBuddy",
        " */",
        "@RequiredArgsConstructor",
        "@Service",
        f"public class {module['service_impl']} implements {module['service']} {{",
        "",
        f"    private final {mapper} baseMapper;",
        "",
        "    @Override",
        f"    public {vo} queryById(Long {id_field}) {{",
        f"        return baseMapper.{module['detail_method']}({id_field});",
        "    }",
        "",
        "    @Override",
        f"    public TableDataInfo<{vo}> queryPageList({bo} bo, PageQuery pageQuery) {{",
        f"        return TableDataInfo.build(baseMapper.{module['page_method']}(pageQuery.build(), bo));",
        "    }",
        "",
        "    @Override",
        f"    public List<{vo}> queryList({bo} bo) {{",
        f"        return baseMapper.{module['list_method']}(bo);",
        "    }",
        "",
        "    @Override",
        f"    public Boolean insertByBo({bo} bo) {{",
        f"        {entity} add = MapstructUtils.convert(bo, {entity}.class);",
        f"        add.set{id_field[0].upper() + id_field[1:]}(IdGeneratorUtil.nextLongId());",
        "        add.setTenantId(currentTenantId());",
        "        validEntityBeforeSave(add);",
        "        boolean flag = baseMapper.insert(add) > 0;",
        "        if (flag) {",
        f"            bo.set{id_field[0].upper() + id_field[1:]}(add.get{id_field[0].upper() + id_field[1:]}());",
        "        }",
        "        return flag;",
        "    }",
        "",
        "    @Override",
        f"    public Boolean updateByBo({bo} bo) {{",
        f"        {entity} update = MapstructUtils.convert(bo, {entity}.class);",
        "        validEntityBeforeSave(update);",
        "        return baseMapper.updateById(update) > 0;",
        "    }",
        "",
    ]
    if status_field:
        cap = status_field[0].upper() + status_field[1:]
        lines += [
            "    @Override",
            f"    public Boolean updateStatus(Long {id_field}, String {status_field}) {{",
            f"        {entity} update = new {entity}();",
            f"        update.set{id_field[0].upper() + id_field[1:]}({id_field});",
            f"        update.set{cap}({status_field});",
            "        return baseMapper.updateById(update) > 0;",
            "    }",
            "",
        ]
    lines += [
        "    @Override",
        "    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {",
        "        if (isValid) {",
        f"            long count = baseMapper.selectCount(new LambdaQueryWrapper<{entity}>().in({entity}::get{id_field[0].upper() + id_field[1:]}, ids));",
        "            if (count != ids.size()) {",
        "                throw new ServiceException(\"部分数据不存在或已被删除\");",
        "            }",
        "        }",
        "        return baseMapper.deleteByIds(ids) > 0;",
        "    }",
        "",
        f"    private void validEntityBeforeSave({entity} entity) {{",
    ]
    unique_lines = unique_check_lines(module)
    if unique_lines:
        lines.extend(unique_lines)
    lines += [
        "    }",
        "",
        "    private String currentTenantId() {",
        "        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);",
        "    }",
        "",
        "}",
    ]
    return "\n".join(lines) + "\n"


def build_controller(module):
    bo = module['bo']
    vo = module['vo']
    service = module['service']
    path = module['path']
    permission = module['permission']
    title = module['title_short']
    id_field = module['id_field']
    lines = [
        "package org.dromara.fitness.controller;",
        "",
        "import cn.dev33.satoken.annotation.SaCheckPermission;",
        "import jakarta.servlet.http.HttpServletResponse;",
        "import jakarta.validation.constraints.NotEmpty;",
        "import jakarta.validation.constraints.NotNull;",
        "import lombok.RequiredArgsConstructor;",
        "import org.dromara.common.core.domain.R;",
        "import org.dromara.common.core.validate.AddGroup;",
        "import org.dromara.common.core.validate.EditGroup;",
        "import org.dromara.common.excel.utils.ExcelUtil;",
        "import org.dromara.common.idempotent.annotation.RepeatSubmit;",
        "import org.dromara.common.log.annotation.Log;",
        "import org.dromara.common.log.enums.BusinessType;",
        "import org.dromara.common.mybatis.core.page.PageQuery;",
        "import org.dromara.common.mybatis.core.page.TableDataInfo;",
        "import org.dromara.common.web.core.BaseController;",
        f"import org.dromara.fitness.domain.bo.{bo};",
        f"import org.dromara.fitness.domain.vo.{vo};",
        f"import org.dromara.fitness.service.{service};",
        "import org.springframework.validation.annotation.Validated;",
        "import org.springframework.web.bind.annotation.*;",
        "",
        "import java.util.Arrays;",
        "import java.util.List;",
        "",
        "/**",
        f" * {title} Controller",
        " *",
        " * @author CodeBuddy",
        " */",
        "@Validated",
        "@RequiredArgsConstructor",
        "@RestController",
        f"@RequestMapping(\"/fitness/{path}\")",
        f"public class {module['controller']} extends BaseController {{",
        "",
        f"    private final {service} {camel_to_words(module['service'][1:])};",
        "",
        "    @SaCheckPermission(\"" + permission + ":list\")",
        "    @GetMapping(\"/list\")",
        f"    public TableDataInfo<{vo}> list({bo} bo, PageQuery pageQuery) {{",
        f"        return {camel_to_words(module['service'][1:])}.queryPageList(bo, pageQuery);",
        "    }",
        "",
        "    @SaCheckPermission(\"" + permission + ":export\")",
        "    @Log(title = \"" + title + "\", businessType = BusinessType.EXPORT)",
        "    @PostMapping(\"/export\")",
        f"    public void export({bo} bo, HttpServletResponse response) {{",
        f"        List<{vo}> list = {camel_to_words(module['service'][1:])}.queryList(bo);",
        f"        ExcelUtil.exportExcel(list, \"{title}\", {vo}.class, response);",
        "    }",
        "",
        "    @SaCheckPermission(\"" + permission + ":query\")",
        f"    @GetMapping(\"/{{{id_field}}}\")",
        f"    public R<{vo}> getInfo(@NotNull(message = \"主键不能为空\") @PathVariable Long {id_field}) {{",
        f"        return R.ok({camel_to_words(module['service'][1:])}.queryById({id_field}));",
        "    }",
        "",
        "    @SaCheckPermission(\"" + permission + ":add\")",
        "    @Log(title = \"" + title + "\", businessType = BusinessType.INSERT)",
        "    @RepeatSubmit",
        "    @PostMapping()",
        f"    public R<Void> add(@Validated(AddGroup.class) @RequestBody {bo} bo) {{",
        f"        return toAjax({camel_to_words(module['service'][1:])}.insertByBo(bo));",
        "    }",
        "",
        "    @SaCheckPermission(\"" + permission + ":edit\")",
        "    @Log(title = \"" + title + "\", businessType = BusinessType.UPDATE)",
        "    @RepeatSubmit",
        "    @PutMapping()",
        f"    public R<Void> edit(@Validated(EditGroup.class) @RequestBody {bo} bo) {{",
        f"        return toAjax({camel_to_words(module['service'][1:])}.updateByBo(bo));",
        "    }",
        "",
    ]
    if module.get('status_field'):
        status_field = module['status_field']
        lines += [
            "    @SaCheckPermission(\"" + permission + ":edit\")",
            "    @Log(title = \"" + title + "\", businessType = BusinessType.UPDATE)",
            f"    @PutMapping(\"/{{{id_field}}}/status\")",
            f"    public R<Void> changeStatus(@PathVariable Long {id_field}, @RequestBody {bo} bo) {{",
            f"        return toAjax({camel_to_words(module['service'][1:])}.updateStatus({id_field}, bo.get{status_field[0].upper() + status_field[1:]}()));",
            "    }",
            "",
        ]
    lines += [
        "    @SaCheckPermission(\"" + permission + ":remove\")",
        "    @Log(title = \"" + title + "\", businessType = BusinessType.DELETE)",
        "    @DeleteMapping(\"/{ids}\")",
        "    public R<Void> remove(@NotEmpty(message = \"主键不能为空\") @PathVariable Long[] ids) {",
        f"        return toAjax({camel_to_words(module['service'][1:])}.deleteWithValidByIds(Arrays.asList(ids), true));",
        "    }",
        "",
        "}",
    ]
    return "\n".join(lines) + "\n"


def build_xml(module):
    lines = [
        '<?xml version="1.0" encoding="UTF-8" ?>',
        '<!DOCTYPE mapper',
        '    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"',
        '    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">',
        f'<mapper namespace="org.dromara.fitness.mapper.{module["mapper"]}">',
        '',
        '    <sql id="baseSelectColumns">',
        '        SELECT',
    ]
    for idx, col in enumerate(module['select_columns']):
        suffix = ',' if idx < len(module['select_columns']) - 1 else ''
        lines.append(f'               {col}{suffix}')
    lines += [
        '    </sql>',
        '',
        '    <sql id="baseFromWhere">',
        f'        FROM {module["table"]} t',
    ]
    for join in module['joins']:
        lines.append(f'        {join}')
    lines += [
        "        WHERE t.del_flag = '0'",
    ]
    for where in module['where_lines']:
        lines.append(f'        {where}')
    lines += [
        '    </sql>',
        '',
        f'    <select id="{module["page_method"]}" resultType="org.dromara.fitness.domain.vo.{module["vo"]}">',
        '        <include refid="baseSelectColumns"/>',
        '        <include refid="baseFromWhere"/>',
        f'        {module["order_by"]}',
        '    </select>',
        '',
        f'    <select id="{module["list_method"]}" resultType="org.dromara.fitness.domain.vo.{module["vo"]}">',
        '        <include refid="baseSelectColumns"/>',
        '        <include refid="baseFromWhere"/>',
        f'        {module["order_by"]}',
        '    </select>',
        '',
        f'    <select id="{module["detail_method"]}" resultType="org.dromara.fitness.domain.vo.{module["vo"]}">',
        '        <include refid="baseSelectColumns"/>',
        '        <include refid="baseFromWhere"/>',
        f'          AND t.{module["id_column"]} = #{{{module["id_field"]}}}',
        '        LIMIT 1',
        '    </select>',
        '',
        '</mapper>',
    ]
    return '\n'.join(lines) + '\n'



for module in modules:
    write(JAVA_BASE / 'domain' / f"{module['entity']}.java", build_entity(module))
    write(JAVA_BASE / 'domain/bo' / f"{module['bo']}.java", build_bo(module))
    write(JAVA_BASE / 'domain/vo' / f"{module['vo']}.java", build_vo(module))
    write(JAVA_BASE / 'mapper' / f"{module['mapper']}.java", build_mapper(module))
    write(JAVA_BASE / 'service' / f"{module['service']}.java", build_service(module))
    write(JAVA_BASE / 'service/impl' / f"{module['service_impl']}.java", build_service_impl(module))
    write(JAVA_BASE / 'controller' / f"{module['controller']}.java", build_controller(module))
    write(RES_BASE / f"{module['mapper']}.xml", build_xml(module))

print('generated:', ', '.join(m['key'] for m in modules))
