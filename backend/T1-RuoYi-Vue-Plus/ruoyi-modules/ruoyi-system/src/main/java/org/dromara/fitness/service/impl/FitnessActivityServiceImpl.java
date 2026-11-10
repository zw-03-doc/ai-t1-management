package org.dromara.fitness.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.OssService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.FitActivity;
import org.dromara.fitness.domain.bo.FitnessActivityBo;
import org.dromara.fitness.domain.vo.FitnessActivityVo;
import org.dromara.fitness.mapper.FitnessActivityMapper;
import org.dromara.fitness.mapper.FitnessCardPlanMapper;
import org.dromara.fitness.mapper.FitnessCoachMapper;
import org.dromara.fitness.mapper.FitnessCourseMapper;
import org.dromara.fitness.service.IFitnessActivityService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 活动 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessActivityServiceImpl implements IFitnessActivityService {

    private static final String TYPE_CARD_DISCOUNT = "card_discount";
    private static final String TYPE_CUSTOM_PUSH = "custom_push";
    private static final String ACTION_DETAIL = "detail";
    private static final String ACTION_CONTACT = "contact";

    private final FitnessActivityMapper baseMapper;
    private final FitnessCardPlanMapper plans;
    private final FitnessCoachMapper coaches;
    private final FitnessCourseMapper courses;
    private final OssService ossService;

    @Override
    public FitnessActivityVo queryById(Long activityId) {
        FitnessActivityVo vo = baseMapper.selectActivityById(activityId);
        if (vo != null) {
            vo.setPlanIds(baseMapper.selectPlanIdsByActivityId(activityId));
        }
        return vo;
    }

    @Override
    public TableDataInfo<FitnessActivityVo> queryPageList(FitnessActivityBo bo, PageQuery pageQuery) {
        TableDataInfo<FitnessActivityVo> dataInfo = TableDataInfo.build(baseMapper.selectPageActivityList(pageQuery.build(), bo));
        fillPlanIds(dataInfo.getRows());
        return dataInfo;
    }

    @Override
    public List<FitnessActivityVo> queryList(FitnessActivityBo bo) {
        List<FitnessActivityVo> list = baseMapper.selectActivityList(bo);
        fillPlanIds(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(FitnessActivityBo bo) {
        FitActivity add = MapstructUtils.convert(bo, FitActivity.class);
        add.setActivityId(IdGeneratorUtil.nextLongId());
        add.setTenantId(currentTenantId());
        add.setActivityNo(FitnessBizCodeUtils.defaultIfBlank(bo.getActivityNo(), "ACT"));
        fillDefaultValue(add, bo);
        validEntityBeforeSave(add, bo);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            syncPlanRefs(add.getActivityId(), bo.getPlanIds());
            bo.setActivityId(add.getActivityId());
            bo.setActivityNo(add.getActivityNo());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FitnessActivityBo bo) {
        FitActivity db = baseMapper.selectById(bo.getActivityId());
        if (db == null) {
            throw new ServiceException("活动不存在");
        }
        FitActivity update = MapstructUtils.convert(bo, FitActivity.class);
        update.setActivityNo(FitnessBizCodeUtils.fillOrKeep(bo.getActivityNo(), db.getActivityNo(), "ACT"));
        fillDefaultValue(update, bo);
        validEntityBeforeSave(update, bo);
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            syncPlanRefs(update.getActivityId(), bo.getPlanIds());
        }
        return flag;
    }

    @Override
    public Boolean updateStatus(Long activityId, String status) {
        if (!"0".equals(status) && !"1".equals(status)) throw new ServiceException("活动状态无效");
        return baseMapper.update(null, new LambdaUpdateWrapper<FitActivity>()
            .eq(FitActivity::getActivityId, activityId).set(FitActivity::getStatus, status)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitActivity>().in(FitActivity::getActivityId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分活动不存在或已删除");
            }
        }
        ids.forEach(baseMapper::deleteActivityPlanRefs);
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void fillPlanIds(List<FitnessActivityVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        rows.forEach(item -> item.setPlanIds(baseMapper.selectPlanIdsByActivityId(item.getActivityId())));
    }

    private void syncPlanRefs(Long activityId, List<Long> planIds) {
        baseMapper.deleteActivityPlanRefs(activityId);
        if (CollUtil.isEmpty(planIds)) {
            return;
        }
        List<Long> cleanIds = planIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollUtil.isNotEmpty(cleanIds)) {
            baseMapper.batchInsertActivityPlanRefs(activityId, cleanIds);
        }
    }

    private void fillDefaultValue(FitActivity entity, FitnessActivityBo bo) {
        entity.setCoverUrl(resolveOssUrl(entity.getCoverUrl()));
        entity.setDetailImageUrl(resolveOssUrl(entity.getDetailImageUrl()));

        if (StringUtils.isBlank(entity.getWeeklyFeatured())) {
            entity.setWeeklyFeatured("0");
        }
        if (StringUtils.isBlank(entity.getActivityType())) {
            entity.setActivityType(TYPE_CARD_DISCOUNT);
        }
        if (StringUtils.isBlank(entity.getActionType())) {
            entity.setActionType(Objects.equals(entity.getActivityType(), TYPE_CUSTOM_PUSH) ? ACTION_CONTACT : ACTION_DETAIL);
        }
        if (entity.getSortNo() == null) {
            entity.setSortNo(0);
        }
        if (StringUtils.isBlank(entity.getStatus())) {
            entity.setStatus("0");
        }
        if (entity.getCountdownEndTime() == null) {
            entity.setCountdownEndTime(entity.getEndTime());
        }
        if (bo != null && bo.getPlanIds() == null) {
            bo.setPlanIds(new ArrayList<>());
        }
    }

    private void validEntityBeforeSave(FitActivity entity, FitnessActivityBo bo) {
        if (entity.getStartTime() == null || entity.getEndTime() == null || entity.getStartTime().after(entity.getEndTime())) {
            throw new ServiceException("活动时间范围不合法");
        }
        if (entity.getCountdownEndTime() != null && entity.getCountdownEndTime().before(entity.getStartTime())) {
            throw new ServiceException("倒计时截止时间不能早于活动开始时间");
        }
        if (StringUtils.isBlank(entity.getCoverUrl())) {
            throw new ServiceException("活动封面图不能为空");
        }
        if (!Objects.equals(entity.getActivityType(), TYPE_CARD_DISCOUNT) && !Objects.equals(entity.getActivityType(), TYPE_CUSTOM_PUSH)) {
            throw new ServiceException("活动类型不合法");
        }
        String action = entity.getActionType();
        if (!java.util.Set.of(ACTION_DETAIL, ACTION_CONTACT, "catalog", "plan", "coach", "course").contains(action))
            throw new ServiceException("请选择有效的活动入口");
        if (ACTION_DETAIL.equals(action) || "plan".equals(action)) {
            var ids = bo.getPlanIds().stream().filter(Objects::nonNull).distinct().toList();
            if (ids.isEmpty() || ("plan".equals(action) && ids.size() != 1))
                throw new ServiceException("plan".equals(action) ? "卡项详情请选择一个卡项" : "活动价目表至少选择一个卡项");
            for (Long id : ids) {
                var plan = plans.selectById(id);
                if (plan == null || !sameStore(entity.getStoreId(), plan.getStoreId()) || ("0".equals(entity.getStatus()) && !"0".equals(plan.getStatus())))
                    throw new ServiceException("请选择适用门店已上架的卡项");
            }
        } else bo.setPlanIds(List.of());
        if ("course".equals(action)) {
            var course = entity.getCourseId() == null ? null : courses.selectById(entity.getCourseId());
            if (course == null || !sameStore(entity.getStoreId(), course.getStoreId()) || ("0".equals(entity.getStatus()) && !"0".equals(course.getStatus())))
                throw new ServiceException("请选择适用门店已上架的课程");
        } else entity.setCourseId(null);
        if (entity.getContactCoachId() != null || "coach".equals(action)) {
            var coach = entity.getContactCoachId() == null ? null : coaches.selectById(entity.getContactCoachId());
            if (coach == null || !"0".equals(coach.getHireStatus()) || !sameStore(entity.getStoreId(), coach.getStoreId()))
                throw new ServiceException("请选择适用门店的在职教练");
            if (StringUtils.isBlank(coach.getContactWechat()) && StringUtils.isBlank(coach.getContactQrUrl()))
                throw new ServiceException("请先在教练资料中填写咨询微信号或上传咨询二维码");
        }
        if (StringUtils.isNotBlank(entity.getActivityNo()) && baseMapper.exists(new LambdaQueryWrapper<FitActivity>()
            .eq(FitActivity::getActivityNo, entity.getActivityNo())
            .ne(Objects.nonNull(entity.getActivityId()), FitActivity::getActivityId, entity.getActivityId()))) {
            throw new ServiceException("活动编号已存在");
        }
    }

    private boolean sameStore(Long activityStore, Long resourceStore) {
        return activityStore == null || resourceStore == null || activityStore.equals(resourceStore);
    }

    private String resolveOssUrl(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.contains("http://") || text.contains("https://")) {
            return text;
        }
        if (text.matches("^[0-9]+(,[0-9]+)*$")) {
            String url = ossService.selectUrlByIds(text);
            if (StringUtils.isNotBlank(url)) {
                return StringUtils.split(url, ",")[0];

            }
        }
        return text;
    }

    private String currentTenantId() {
        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }
}
