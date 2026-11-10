package org.dromara.fitness.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.FitCourseSchedule;
import org.dromara.fitness.domain.bo.FitnessScheduleBo;
import org.dromara.fitness.domain.bo.FitnessScheduleStatusBo;
import org.dromara.fitness.domain.vo.FitnessScheduleVo;
import org.dromara.fitness.mapper.FitnessScheduleMapper;
import org.dromara.fitness.service.IFitnessScheduleService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 排课 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessScheduleServiceImpl implements IFitnessScheduleService {

    private static final String MODE_NORMAL = "normal";
    private static final String MODE_PRIVATE = "private";
    private static final String MODE_SMALL_GROUP = "small_group";

    private final FitnessOperationsService operations;
    private final FitnessScheduleMapper baseMapper;
    private static final Map<String, String> STATUS_NAMES = Map.of(
        "draft", "草稿", "open", "已开放", "closed", "已关闭", "finished", "已结束", "cancelled", "已取消");

    @Override
    public FitnessScheduleVo queryById(Long scheduleId) {
        FitnessScheduleVo vo = baseMapper.selectScheduleById(scheduleId);
        fillStudentIds(vo);
        return vo;
    }

    @Override
    public TableDataInfo<FitnessScheduleVo> queryPageList(FitnessScheduleBo bo, PageQuery pageQuery) {
        TableDataInfo<FitnessScheduleVo> dataInfo = TableDataInfo.build(baseMapper.selectPageScheduleList(pageQuery.build(), bo));
        fillStudentIds(dataInfo.getRows());
        return dataInfo;
    }

    @Override
    public List<FitnessScheduleVo> queryList(FitnessScheduleBo bo) {
        List<FitnessScheduleVo> list = baseMapper.selectScheduleList(bo);
        fillStudentIds(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(FitnessScheduleBo bo) {
        validBeforeSave(bo);
        FitCourseSchedule add = MapstructUtils.convert(bo, FitCourseSchedule.class);
        add.setScheduleId(IdGeneratorUtil.nextLongId());
        add.setScheduleNo(FitnessBizCodeUtils.defaultIfBlank(bo.getScheduleNo(), "SCH"));
        add.setTenantId(currentTenantId());
        add.setReservedCount(0);
        add.setSignedCount(0);
        add.setScheduleStatus(StringUtils.defaultIfBlank(bo.getScheduleStatus(), "draft"));
        add.setScheduleMode(bo.getScheduleMode());
        if ("finished".equals(add.getScheduleStatus())) operations.requireScheduleCanFinish(add);
        validScheduleNoUnique(add.getScheduleNo(), null);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            syncScheduleStudents(add.getScheduleId(), add.getTenantId(), bo.getStudentMemberIds());
            bo.setScheduleId(add.getScheduleId());
            bo.setScheduleNo(add.getScheduleNo());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBatch(List<FitnessScheduleBo> rows) {
        for (int i = 0; i < rows.size(); i++) {
            FitnessScheduleBo row = rows.get(i);
            row.setScheduleId(null);
            try {
                if (!insertByBo(row)) throw new ServiceException("创建失败");
            } catch (ServiceException error) {
                throw new ServiceException("第" + (i + 1) + "场：" + error.getMessage());
            }
        }
    }

    @Override
    @Lock4j(keys = {"'schedule:update:' + #bo.scheduleId"}, acquireTimeout = 3000)
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FitnessScheduleBo bo) {
        FitCourseSchedule db = operations.lockSchedule(bo.getScheduleId());
        validBeforeSave(bo);
        if (db == null) {
            throw new ServiceException("排课不存在");
        }
        int reserved = db.getReservedCount() == null ? 0 : db.getReservedCount();
        if (bo.getCapacity() < reserved) throw new ServiceException("名额不得小于已预约人数");
        if (reserved > 0 && (!Objects.equals(db.getStoreId(), bo.getStoreId()) || !Objects.equals(db.getCourseId(), bo.getCourseId())
            || !Objects.equals(db.getCoachId(), bo.getCoachId()) || !Objects.equals(db.getStartTime(), bo.getStartTime())
            || !Objects.equals(db.getEndTime(), bo.getEndTime())))
            throw new ServiceException("已有预约的场次不能直接变更门店、课程、教练或时间，请取消预约后重新排课");
        validateStatusChange(db, bo.getScheduleStatus());
        if (reserved > 0 && (!Objects.equals(db.getScheduleMode(), bo.getScheduleMode())
            || !new java.util.HashSet<>(baseMapper.selectScheduleStudentIds(db.getScheduleId())).equals(new java.util.HashSet<>(bo.getStudentMemberIds()))))
            throw new ServiceException("已有预约，不能直接修改公开范围或定向名单");
        FitCourseSchedule update = MapstructUtils.convert(bo, FitCourseSchedule.class);
        update.setReservedCount(db.getReservedCount());
        update.setSignedCount(db.getSignedCount());
        update.setVersion(db.getVersion());
        update.setScheduleNo(FitnessBizCodeUtils.fillOrKeep(bo.getScheduleNo(), db.getScheduleNo(), "SCH"));
        update.setScheduleMode(bo.getScheduleMode());
        validScheduleNoUnique(update.getScheduleNo(), bo.getScheduleId());
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            syncScheduleStudents(update.getScheduleId(), db.getTenantId(), bo.getStudentMemberIds());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(FitnessScheduleStatusBo batch) {
        String target = batch.targetStatus();
        if (!STATUS_NAMES.containsKey(target)) throw new ServiceException("请选择有效的排课状态");
        int changed = 0;
        var sorted = batch.rows().stream().sorted(Comparator.comparing(FitnessScheduleStatusBo.SelectedSchedule::scheduleId)).toList();
        for (var row : sorted) {
            FitCourseSchedule current = operations.lockSchedule(row.scheduleId());
            if (target.equals(current.getScheduleStatus())) continue;
            try {
                if (!row.expectedStatus().equals(current.getScheduleStatus())) throw new ServiceException("状态已变化，请刷新后重新选择");
                // Re-check the current coach, roster and time slot before accepting bookings
                // or putting a previously cancelled slot back on the coach's schedule.
                if ("open".equals(target) || ("cancelled".equals(current.getScheduleStatus()) && !"finished".equals(target))) {
                    var saved = new FitnessScheduleBo();
                    BeanUtils.copyProperties(current, saved);
                    saved.setScheduleStatus(target);
                    saved.setStudentMemberIds(baseMapper.selectScheduleStudentIds(current.getScheduleId()));
                    validBeforeSave(saved);
                }
                applyStatus(current, target, null);
                changed++;
            } catch (ServiceException error) {
                throw new ServiceException(DateUtil.formatDateTime(current.getStartTime()) + " 场次：" + error.getMessage() + "。本次变更未保存");
            }
        }
        return changed;
    }

    private void validateStatusChange(FitCourseSchedule current, String target) {
        if ("finished".equals(current.getScheduleStatus()) && !"finished".equals(target))
            throw new ServiceException("已结束场次不能变更状态");
        if (("draft".equals(target) || "cancelled".equals(target)) && !target.equals(current.getScheduleStatus())
            && ((current.getReservedCount() != null && current.getReservedCount() > 0)
            || (current.getSignedCount() != null && current.getSignedCount() > 0)))
            throw new ServiceException("已有预约或签到，不能转为草稿或取消场次");
        if ("finished".equals(target)) operations.requireScheduleCanFinish(current);
    }

    private void applyStatus(FitCourseSchedule current, String target, String cancelReason) {
        String previous = current.getScheduleStatus();
        if (target.equals(previous)) return;
        validateStatusChange(current, target);
        FitCourseSchedule update = new FitCourseSchedule();
        update.setScheduleId(current.getScheduleId());
        update.setVersion(current.getVersion());
        update.setScheduleStatus(target);
        if (StringUtils.isNotBlank(cancelReason)) update.setRemark(cancelReason);
        if (baseMapper.updateById(update) != 1) throw new ServiceException("场次状态已变化，请刷新后重试");
        String detail = STATUS_NAMES.getOrDefault(previous, previous) + " → " + STATUS_NAMES.get(target);
        if (StringUtils.isNotBlank(cancelReason)) detail += "，" + cancelReason;
        operations.audit(null, current.getStoreId(), current.getScheduleId(), null, null,
            "finished".equals(target) ? "admin_finish_schedule" : "admin_schedule_status", "web", detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelSchedule(Long scheduleId, String cancelReason) {
        applyStatus(operations.lockSchedule(scheduleId), "cancelled", cancelReason);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean finishSchedule(Long scheduleId) {
        applyStatus(operations.lockSchedule(scheduleId), "finished", null);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitCourseSchedule>().in(FitCourseSchedule::getScheduleId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分排课不存在或已被删除");
            }
        }
        for (Long id : ids) {
            FitCourseSchedule current = operations.lockSchedule(id);
            if (current.getReservedCount() != null && current.getReservedCount() > 0) throw new ServiceException("有预约记录的场次不能删除");
        }
        ids.forEach(operations::requireNoScheduleHistory);
        ids.forEach(baseMapper::deleteScheduleStudents);
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void validBeforeSave(FitnessScheduleBo bo) {
        bo.setStudentMemberIds(cleanMemberIds(bo.getStudentMemberIds()));
        operations.prepareSchedule(bo);
    }

    private List<Long> cleanMemberIds(List<Long> memberIds) {
        if (CollUtil.isEmpty(memberIds)) {
            return new ArrayList<>();
        }
        return memberIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private void syncScheduleStudents(Long scheduleId, String tenantId, List<Long> memberIds) {
        baseMapper.deleteScheduleStudents(scheduleId);
        if (CollUtil.isEmpty(memberIds)) {
            return;
        }
        baseMapper.batchInsertScheduleStudents(scheduleId,
            StringUtils.defaultIfBlank(tenantId, currentTenantId()),
            cleanMemberIds(memberIds));
    }

    private void fillStudentIds(List<FitnessScheduleVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(this::fillStudentIds);
    }

    private void fillStudentIds(FitnessScheduleVo vo) {
        if (vo == null || vo.getScheduleId() == null) {
            return;
        }
        vo.setStudentMemberIds(baseMapper.selectScheduleStudentIds(vo.getScheduleId()));
    }

    private void validScheduleNoUnique(String scheduleNo, Long excludeScheduleId) {
        long count = baseMapper.selectCount(new LambdaQueryWrapper<FitCourseSchedule>()
            .eq(FitCourseSchedule::getScheduleNo, scheduleNo)
            .ne(excludeScheduleId != null, FitCourseSchedule::getScheduleId, excludeScheduleId));
        if (count > 0) {
            throw new ServiceException("排课编号已存在");
        }
    }

    private String currentTenantId() {
        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }
}
