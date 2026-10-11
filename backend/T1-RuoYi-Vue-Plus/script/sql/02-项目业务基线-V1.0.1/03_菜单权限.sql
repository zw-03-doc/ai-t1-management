-- 健身房一期 PC 管理端菜单与按钮 SQL（MySQL）
-- 说明：
-- 1. 本文件默认你已先插入 7 个一级目录菜单；
-- 2. 仅补充二级菜单（C）与按钮（F）；
-- 3. component 路径已与 T-plus-ui 的 src/views/fitness/** 对齐；
-- 4. 采用固定 menu_id，支持重复执行（先删后插）。

-- ----------------------------
-- 删除旧菜单（便于重复执行）
-- ----------------------------
DELETE FROM sys_menu
WHERE menu_id IN (
  '2042601000000000101','2042601000000000102','2042601000000000103','2042601000000000104','2042601000000000105','2042601000000000106','2042601000000000107',
  '2042601000000000201','2042601000000000202','2042601000000000203','2042601000000000204','2042601000000000205','2042601000000000206',
  '2042601000000000301','2042601000000000302','2042601000000000303','2042601000000000304','2042601000000000305','2042601000000000306',
  '2042601000000000311','2042601000000000312','2042601000000000313','2042601000000000314','2042601000000000315','2042601000000000316',
  '2042601000000000401','2042601000000000402','2042601000000000403','2042601000000000404','2042601000000000405','2042601000000000406','2042601000000000407',
  '2042601000000000411','2042601000000000412','2042601000000000413','2042601000000000414','2042601000000000415','2042601000000000416','2042601000000000417',
  '2042601000000000421','2042601000000000422','2042601000000000423','2042601000000000424','2042601000000000425','2042601000000000426','2042601000000000427',
  '2042601000000000431','2042601000000000432','2042601000000000433','2042601000000000434','2042601000000000435',
  '2042601000000000501','2042601000000000502','2042601000000000503','2042601000000000504','2042601000000000505','2042601000000000506','2042601000000000507',
  '2042601000000000601','2042601000000000602','2042601000000000603','2042601000000000604','2042601000000000605','2042601000000000606','2042601000000000607','2042601000000000608','2042601000000000609',
  '2042601000000000611','2042601000000000612','2042601000000000613',
  '2042601000000000701','2042601000000000702','2042601000000000703','2042601000000000704','2042601000000000705'
);

-- ----------------------------
-- 门店管理
-- parent_id: 2042588802307977218
-- ----------------------------
INSERT INTO sys_menu VALUES ('2042601000000000101', '门店列表', '2042588802307977218', '1', 'store', 'fitness/store/index', '', 1, 0, 'C', '0', '0', 'fitness:store:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房门店列表菜单');
INSERT INTO sys_menu VALUES ('2042601000000000102', '门店查询', '2042601000000000101', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:store:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000103', '门店新增', '2042601000000000101', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:store:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000104', '门店修改', '2042601000000000101', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:store:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000105', '门店删除', '2042601000000000101', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:store:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000106', '门店导出', '2042601000000000101', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:store:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000107', '门店状态', '2042601000000000101', 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:store:edit', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 员工管理 / 教练管理
-- parent_id: 2042589474197729282
-- ----------------------------
INSERT INTO sys_menu VALUES ('2042601000000000201', '教练管理', '2042589474197729282', '1', 'coach', 'fitness/coach/index', '', 1, 0, 'C', '0', '0', 'fitness:coach:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房教练管理菜单');
INSERT INTO sys_menu VALUES ('2042601000000000202', '教练查询', '2042601000000000201', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:coach:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000203', '教练新增', '2042601000000000201', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:coach:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000204', '教练修改', '2042601000000000201', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:coach:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000205', '教练删除', '2042601000000000201', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:coach:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000206', '教练导出', '2042601000000000201', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:coach:export', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 会员管理 / 会员档案
-- parent_id: 2042589689940144129
-- ----------------------------
INSERT INTO sys_menu VALUES ('2042601000000000301', '会员档案', '2042589689940144129', '1', 'member', 'fitness/member/index', '', 1, 0, 'C', '0', '0', 'fitness:member:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房会员档案菜单');
INSERT INTO sys_menu VALUES ('2042601000000000302', '会员查询', '2042601000000000301', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:member:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000303', '会员新增', '2042601000000000301', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:member:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000304', '会员修改', '2042601000000000301', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:member:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000305', '会员删除', '2042601000000000301', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:member:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000306', '会员导出', '2042601000000000301', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:member:export', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu VALUES ('2042601000000000311', '会员持卡', '2042589689940144129', '2', 'memberCard', 'fitness/memberCard/index', '', 1, 0, 'C', '0', '0', 'fitness:memberCard:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房会员持卡菜单');
INSERT INTO sys_menu VALUES ('2042601000000000312', '会员持卡查询', '2042601000000000311', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:memberCard:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000313', '会员持卡导出', '2042601000000000311', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:memberCard:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000314', '会员持卡管理', '2042601000000000311', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:memberCard:manage', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000315', '会员持卡状态', '2042601000000000311', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:memberCard:manage', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000316', '会员持卡调整', '2042601000000000311', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:memberCard:manage', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 课程中心
-- parent_id: 2042590129557729281
-- ----------------------------
INSERT INTO sys_menu VALUES ('2042601000000000401', '课程管理', '2042590129557729281', '1', 'course', 'fitness/course/index', '', 1, 0, 'C', '0', '0', 'fitness:course:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房课程管理菜单');
INSERT INTO sys_menu VALUES ('2042601000000000402', '课程查询', '2042601000000000401', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:course:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000403', '课程新增', '2042601000000000401', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:course:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000404', '课程修改', '2042601000000000401', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:course:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000405', '课程删除', '2042601000000000401', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:course:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000406', '课程导出', '2042601000000000401', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:course:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000407', '课程状态', '2042601000000000401', 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:course:edit', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu VALUES ('2042601000000000411', '排课管理', '2042590129557729281', '2', 'schedule', 'fitness/schedule/index', '', 1, 0, 'C', '0', '0', 'fitness:schedule:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房排课管理菜单');
INSERT INTO sys_menu VALUES ('2042601000000000412', '排课查询', '2042601000000000411', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:schedule:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000413', '排课新增', '2042601000000000411', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:schedule:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000414', '排课修改', '2042601000000000411', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:schedule:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000415', '排课删除', '2042601000000000411', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:schedule:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000416', '排课导出', '2042601000000000411', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:schedule:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000417', '排课取消', '2042601000000000411', 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:schedule:edit', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu VALUES ('2042601000000000421', '预约记录', '2042590129557729281', '3', 'reservation', 'fitness/reservation/index', '', 1, 0, 'C', '0', '0', 'fitness:reservation:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房预约记录菜单');
INSERT INTO sys_menu VALUES ('2042601000000000422', '预约查询', '2042601000000000421', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:reservation:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000423', '预约新增', '2042601000000000421', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:reservation:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000424', '预约修改', '2042601000000000421', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:reservation:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000425', '预约删除', '2042601000000000421', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:reservation:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000426', '预约导出', '2042601000000000421', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:reservation:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000427', '预约取消', '2042601000000000421', 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:reservation:edit', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu VALUES ('2042601000000000431', '签到核销', '2042590129557729281', '4', 'checkin', 'fitness/checkin/index', '', 1, 0, 'C', '0', '0', 'fitness:checkin:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房签到核销菜单');
INSERT INTO sys_menu VALUES ('2042601000000000432', '签到查询', '2042601000000000431', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:checkin:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000433', '签到导出', '2042601000000000431', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:checkin:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000434', '确认核销', '2042601000000000431', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:checkin:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000435', '异常标记', '2042601000000000431', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:checkin:edit', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 卡项中心
-- parent_id: 2042590365416026113
-- ----------------------------
INSERT INTO sys_menu VALUES ('2042601000000000501', '卡项方案', '2042590365416026113', '1', 'cardPlan', 'fitness/cardPlan/index', '', 1, 0, 'C', '0', '0', 'fitness:cardPlan:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房卡项方案菜单');
INSERT INTO sys_menu VALUES ('2042601000000000502', '卡项方案查询', '2042601000000000501', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:cardPlan:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000503', '卡项方案新增', '2042601000000000501', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:cardPlan:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000504', '卡项方案修改', '2042601000000000501', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:cardPlan:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000505', '卡项方案删除', '2042601000000000501', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:cardPlan:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000506', '卡项方案导出', '2042601000000000501', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:cardPlan:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000507', '卡项方案状态', '2042601000000000501', 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:cardPlan:edit', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 订单中心
-- parent_id: 2042590527005782017
-- ----------------------------
INSERT INTO sys_menu VALUES ('2042601000000000601', '订单管理', '2042590527005782017', '1', 'order', 'fitness/order/index', '', 1, 0, 'C', '0', '0', 'fitness:order:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房订单管理菜单');
INSERT INTO sys_menu VALUES ('2042601000000000602', '订单查询', '2042601000000000601', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:order:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000603', '订单新增', '2042601000000000601', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:order:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000604', '订单修改', '2042601000000000601', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:order:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000605', '订单删除', '2042601000000000601', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:order:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000606', '订单导出', '2042601000000000601', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:order:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000607', '订单支付', '2042601000000000601', 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:order:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000608', '订单取消', '2042601000000000601', 7, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:order:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000609', '订单退款', '2042601000000000601', 8, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:order:edit', '#', 103, 1, sysdate(), NULL, NULL, '');

INSERT INTO sys_menu VALUES ('2042601000000000611', '订单明细', '2042590527005782017', '2', 'orderItem', 'fitness/orderItem/index', '', 1, 0, 'C', '0', '0', 'fitness:orderItem:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房订单明细菜单');
INSERT INTO sys_menu VALUES ('2042601000000000612', '订单明细查询', '2042601000000000611', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:orderItem:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000613', '订单明细导出', '2042601000000000611', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:orderItem:export', '#', 103, 1, sysdate(), NULL, NULL, '');

-- ----------------------------
-- 报表中心 / 经营概览
-- parent_id: 2042590627627134978
-- ----------------------------
INSERT INTO sys_menu VALUES ('2042601000000000701', '经营概览', '2042590627627134978', '1', 'overview', 'fitness/home/index', '', 1, 0, 'C', '0', '0', 'fitness:home:view', '#', 103, 1, sysdate(), NULL, NULL, '健身房经营概览菜单');
INSERT INTO sys_menu VALUES ('2042601000000000702', '经营概览查看', '2042601000000000701', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:home:view', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000703', '报表中心', '2042590627627134978', '2', 'report', 'fitness/report/index', '', 1, 0, 'C', '0', '0', 'fitness:report:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房会员/订单/课程/教练经营报表菜单');
INSERT INTO sys_menu VALUES ('2042601000000000704', '报表查询', '2042601000000000703', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:report:list', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000705', '报表导出', '2042601000000000703', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:report:export', '#', 103, 1, sysdate(), NULL, NULL, '');
