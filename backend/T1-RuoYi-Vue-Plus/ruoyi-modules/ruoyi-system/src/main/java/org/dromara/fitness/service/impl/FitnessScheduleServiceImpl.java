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
import org.dromara.fitness.domain.vo.FitnessScheduleVo;
import org.dromara.fitness.mapper.FitnessScheduleMapper;
import org.dromara.fitness.service.IFitnessScheduleService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;
import org.springframework.stereotype.Service;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        if (reserved > 0 && !Objects.equals(db.getScheduleStatus(), bo.getScheduleStatus())
            && !java.util.Set.of("open", "closed", "finished").contains(Objects.toString(bo.getScheduleStatus(), "")))
            throw new ServiceException("有预约历史的场次不能取消，请处理预约后结束场次");
        if ("finished".equals(db.getScheduleStatus()) && !"finished".equals(bo.getScheduleStatus()))
            throw new ServiceException("已结束场次不能重新开放，请新建场次");
        if ("finished".equals(bo.getScheduleStatus())) operations.requireScheduleCanFinish(db);
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
    public Boolean cancelSchedule(Long scheduleId, String cancelReason) {
        FitCourseSchedule current = operations.lockSchedule(scheduleId);
        if (current.getReservedCount() != null && current.getReservedCount() > 0) throw new ServiceException("请先取消并处理此场次的预约，再取消排课");
        FitCourseSchedule update = new FitCourseSchedule();
        update.setScheduleId(scheduleId);
        update.setScheduleStatus("cancelled");
        if (StringUtils.isNotBlank(cancelReason)) {
            update.setRemark(cancelReason);
        }
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean finishSchedule(Long scheduleId) {
        FitCourseSchedule current = operations.lockSchedule(scheduleId);
        if ("finished".equals(current.getScheduleStatus())) return true;
        operations.requireScheduleCanFinish(current);
        current.setScheduleStatus("finished");
        if (baseMapper.updateById(current) != 1) throw new ServiceException("场次状态已变化，请刷新后重试");
        operations.audit(null, current.getStoreId(), scheduleId, null, null, "admin_finish_schedule", "web", "全部预约已完成核销或登记缺席/请假，保留历史人数");
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
