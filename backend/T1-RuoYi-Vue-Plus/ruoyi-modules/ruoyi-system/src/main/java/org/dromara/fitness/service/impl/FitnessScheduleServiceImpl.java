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
        add.setReservedCount(bo.getReservedCount() == null ? 0 : bo.getReservedCount());
        add.setSignedCount(bo.getSignedCount() == null ? 0 : bo.getSignedCount());
        add.setScheduleStatus(StringUtils.defaultIfBlank(bo.getScheduleStatus(), "draft"));
        add.setScheduleMode(resolveScheduleMode(bo));
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
        validBeforeSave(bo);
        FitCourseSchedule db = baseMapper.selectById(bo.getScheduleId());
        if (db == null) {
            throw new ServiceException("排课不存在");
        }
        FitCourseSchedule update = MapstructUtils.convert(bo, FitCourseSchedule.class);
        update.setScheduleNo(FitnessBizCodeUtils.fillOrKeep(bo.getScheduleNo(), db.getScheduleNo(), "SCH"));
        update.setScheduleMode(resolveScheduleMode(bo));
        validScheduleNoUnique(update.getScheduleNo(), bo.getScheduleId());
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            syncScheduleStudents(update.getScheduleId(), db.getTenantId(), bo.getStudentMemberIds());
        }
        return flag;
    }

    @Override
    public Boolean cancelSchedule(Long scheduleId, String cancelReason) {
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
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitCourseSchedule>().in(FitCourseSchedule::getScheduleId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分排课不存在或已被删除");
            }
        }
        ids.forEach(baseMapper::deleteScheduleStudents);
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void validBeforeSave(FitnessScheduleBo bo) {
        if (bo.getStartTime() != null && bo.getEndTime() != null && DateUtil.compare(bo.getEndTime(), bo.getStartTime()) <= 0) {
            throw new ServiceException("结束时间必须晚于开始时间");
        }
        String courseType = baseMapper.selectCourseTypeById(bo.getCourseId());
        if (StringUtils.isBlank(courseType)) {
            throw new ServiceException("课程不存在或已下架");
        }
        List<Long> memberIds = cleanMemberIds(bo.getStudentMemberIds());
        if (isPrivateOrSmall(courseType)) {
            if (CollUtil.isEmpty(memberIds)) {
                throw new ServiceException("私教/小班排课需至少选择1名学员");
            }
            if (memberIds.size() > 5) {
                throw new ServiceException("私教/小班排课最多选择5名学员");
            }
            bo.setCapacity(memberIds.size());
        }
        bo.setStudentMemberIds(memberIds);
    }

    private String resolveScheduleMode(FitnessScheduleBo bo) {
        String courseType = baseMapper.selectCourseTypeById(bo.getCourseId());
        if (Objects.equals("private", courseType)) {
            return MODE_PRIVATE;
        }
        if (Objects.equals("small", courseType)) {
            return MODE_SMALL_GROUP;
        }
        return StringUtils.defaultIfBlank(bo.getScheduleMode(), MODE_NORMAL);
    }

    private boolean isPrivateOrSmall(String courseType) {
        return Objects.equals("private", courseType) || Objects.equals("small", courseType);
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
