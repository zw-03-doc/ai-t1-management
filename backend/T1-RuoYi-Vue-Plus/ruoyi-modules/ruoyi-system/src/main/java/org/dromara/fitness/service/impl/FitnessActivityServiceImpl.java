package org.dromara.fitness.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        FitActivity update = new FitActivity();
        update.setActivityId(activityId);
        update.setStatus(status);
        return baseMapper.updateById(update) > 0;
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
        if (Objects.equals(entity.getActivityType(), TYPE_CARD_DISCOUNT)) {
            if (!Objects.equals(entity.getActionType(), ACTION_DETAIL)) {
                throw new ServiceException("优惠卡项活动动作类型仅支持查看详情");
            }
            if (bo == null || CollUtil.isEmpty(bo.getPlanIds())) {
                throw new ServiceException("优惠卡项活动至少绑定一个卡项方案");
            }
        } else {
            if (!Objects.equals(entity.getActionType(), ACTION_CONTACT)) {
                throw new ServiceException("定制推送活动动作类型仅支持联系门店");
            }
        }
        if (StringUtils.isNotBlank(entity.getActivityNo()) && baseMapper.exists(new LambdaQueryWrapper<FitActivity>()
            .eq(FitActivity::getActivityNo, entity.getActivityNo())
            .ne(Objects.nonNull(entity.getActivityId()), FitActivity::getActivityId, entity.getActivityId()))) {
            throw new ServiceException("活动编号已存在");
        }
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
