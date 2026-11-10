package org.dromara.fitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.dromara.fitness.utils.FitnessEntitlementPolicy;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.fitness.domain.FitCardPlan;
import org.dromara.fitness.domain.FitCourse;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.bo.FitnessCardPlanBo;
import org.dromara.fitness.domain.vo.FitnessCardPlanVo;
import org.dromara.fitness.mapper.FitnessCardPlanMapper;
import org.dromara.fitness.mapper.FitnessCourseMapper;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.service.IFitnessCardPlanService;
import org.dromara.fitness.utils.FitnessBizCodeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 卡项方案 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessCardPlanServiceImpl implements IFitnessCardPlanService {

    private static final String PLAN_TYPE_PRIVATE_PACKAGE = "private_package";
    private static final String PLAN_TYPE_SMALL_GROUP_PACKAGE = "small_group_package";

    private final FitnessCardPlanMapper baseMapper;
    private final FitnessCourseMapper courseMapper;
    private final FitnessMemberMapper memberMapper;

    @Override
    public FitnessCardPlanVo queryById(Long planId) {
        FitnessCardPlanVo vo = baseMapper.selectCardPlanById(planId);
        fillExtraFields(vo);
        return vo;
    }

    @Override
    public TableDataInfo<FitnessCardPlanVo> queryPageList(FitnessCardPlanBo bo, PageQuery pageQuery) {
        TableDataInfo<FitnessCardPlanVo> dataInfo = TableDataInfo.build(baseMapper.selectPageCardPlanList(pageQuery.build(), bo));
        fillExtraFields(dataInfo.getRows());
        return dataInfo;
    }

    @Override
    public List<FitnessCardPlanVo> queryList(FitnessCardPlanBo bo) {
        List<FitnessCardPlanVo> list = baseMapper.selectCardPlanList(bo);
        fillExtraFields(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(FitnessCardPlanBo bo) {
        validateBo(bo);

        FitCardPlan add = new FitCardPlan();
        fillEntityFromBo(add, bo);
        add.setPlanId(IdGeneratorUtil.nextLongId());
        add.setTenantId(currentTenantId());
        add.setPlanNo(FitnessBizCodeUtils.defaultIfBlank(bo.getPlanNo(), "PLN"));

        applyPackageRule(add);
        validEntityBeforeSave(add);
        validateCourseBinding(add);

        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setPlanId(add.getPlanId());
            bo.setPlanNo(add.getPlanNo());
            bo.setBindCourseId(add.getBindCourseId());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FitnessCardPlanBo bo) {
        FitCardPlan db = baseMapper.selectOne(new LambdaQueryWrapper<FitCardPlan>()
            .eq(FitCardPlan::getPlanId, bo.getPlanId()).last("FOR UPDATE"));
        if (db == null) {
            throw new ServiceException("卡项方案不存在");
        }

        validateBo(bo);

        FitCardPlan update = new FitCardPlan();
        fillEntityFromBo(update, bo);
        update.setPlanId(bo.getPlanId());
        update.setTenantId(db.getTenantId());
        update.setPlanNo(FitnessBizCodeUtils.fillOrKeep(bo.getPlanNo(), db.getPlanNo(), "PLN"));

        applyPackageRule(update);
        if (!bo.isStoreProvided()) update.setStoreId(db.getStoreId());
        if (!bo.isBindCourseProvided()) {
            update.setBindCourseId(db.getBindCourseId());
        }
        validEntityBeforeSave(update);
        validateCourseBinding(update);

        return baseMapper.update(update, new LambdaUpdateWrapper<FitCardPlan>().eq(FitCardPlan::getPlanId, db.getPlanId())
            .set(bo.isStoreProvided() && update.getStoreId() == null, FitCardPlan::getStoreId, null)
            .set(bo.isBindCourseProvided() && update.getBindCourseId() == null, FitCardPlan::getBindCourseId, null)) > 0;
    }

    @Override
    public Boolean updateStatus(Long planId, String status) {
        FitCardPlan update = new FitCardPlan();
        update.setPlanId(planId);
        update.setStatus(status);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitCardPlan>().in(FitCardPlan::getPlanId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分数据不存在或已被删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void fillEntityFromBo(FitCardPlan entity, FitnessCardPlanBo bo) {
        entity.setPlanNo(bo.getPlanNo());
        entity.setStoreId(bo.getStoreId());
        entity.setPlanName(bo.getPlanName());
        entity.setPlanType(bo.getPlanType());
        entity.setSalePrice(bo.getSalePrice());
        entity.setOriginalPrice(bo.getOriginalPrice());
        entity.setValidDays(bo.getValidDays());
        entity.setTotalTimes(bo.getTotalTimes());
        entity.setGiftTimes(bo.getGiftTimes());
        entity.setStoredAmount(bo.getStoredAmount());
        entity.setSupportCourseType(bo.getSupportCourseType());
        entity.setIsPrivateBindCoach(bo.getIsPrivateBindCoach());
        entity.setCustomCoachId(bo.getCustomCoachId());
        entity.setCustomMemberIds(joinMemberIds(bo.getCustomMemberIds()));
        entity.setBindCourseId(bo.getBindCourseId());
        entity.setSortNo(bo.getSortNo());
        entity.setStatus(bo.getStatus());
        entity.setRemark(bo.getRemark());
    }

    private void validateBo(FitnessCardPlanBo bo) {
        FitnessEntitlementPolicy.grantedTimes(bo.getTotalTimes(), bo.getGiftTimes());
        if (isPackagePlan(bo.getPlanType())) {
            if (bo.getCustomCoachId() == null) {
                throw new ServiceException("私教包/小班包必须选择教练");
            }
            List<Long> memberIds = cleanMemberIds(bo.getCustomMemberIds());
            if (memberIds.isEmpty() || memberIds.size() > 5) {
                throw new ServiceException("私教包/小班包必须选择1-5名会员");
            }
            bo.setCustomMemberIds(memberIds);
        }
    }

    private void applyPackageRule(FitCardPlan entity) {
        if (isPackagePlan(entity.getPlanType())) {
            entity.setIsPrivateBindCoach("1");
            entity.setSupportCourseType(resolveCourseTypeByPlanType(entity.getPlanType()));
            return;
        }
        entity.setCustomCoachId(null);
        entity.setCustomMemberIds(null);
    }

    private void validateCourseBinding(FitCardPlan plan) {
        if (plan.getBindCourseId() == null) return;
        FitCourse course = courseMapper.selectById(plan.getBindCourseId());
        if (course == null || !Objects.equals(course.getTenantId(), plan.getTenantId()))
            throw new ServiceException("所选课程不存在，请重新选择或清空");
        if (plan.getStoreId() != null && !Objects.equals(plan.getStoreId(), course.getStoreId()))
            throw new ServiceException("请选择方案适用门店的课程");
        if (isPackagePlan(plan.getPlanType()) && !Objects.equals(plan.getSupportCourseType(), course.getCourseType()))
            throw new ServiceException("请选择与课包类型一致的课程");
    }

    private String resolveCourseTypeByPlanType(String planType) {
        if (Objects.equals(PLAN_TYPE_PRIVATE_PACKAGE, planType)) {
            return "private";
        }
        if (Objects.equals(PLAN_TYPE_SMALL_GROUP_PACKAGE, planType)) {
            return "small";
        }
        return StringUtils.defaultIfBlank(planType, "group");
    }

    private boolean isPackagePlan(String planType) {
        return Objects.equals(PLAN_TYPE_PRIVATE_PACKAGE, planType)
            || Objects.equals(PLAN_TYPE_SMALL_GROUP_PACKAGE, planType);
    }

    private void validEntityBeforeSave(FitCardPlan entity) {
        if (StringUtils.isNotBlank(entity.getPlanNo()) && baseMapper.exists(new LambdaQueryWrapper<FitCardPlan>()
            .eq(FitCardPlan::getPlanNo, entity.getPlanNo())
            .ne(entity.getPlanId() != null, FitCardPlan::getPlanId, entity.getPlanId()))) {
            throw new ServiceException("方案编号已存在");
        }
    }

    private void fillExtraFields(List<FitnessCardPlanVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(this::fillExtraFields);
    }

    private void fillExtraFields(FitnessCardPlanVo vo) {
        if (vo == null) {
            return;
        }
        vo.setGrantedTimes(FitnessEntitlementPolicy.grantedTimes(vo.getTotalTimes(), vo.getGiftTimes()));
        List<Long> memberIds = parseMemberIds(vo.getCustomMemberIdsRaw());
        vo.setCustomMemberIds(memberIds);
        if (memberIds.isEmpty()) {
            vo.setCustomMemberNames("-");
            return;
        }
        List<FitMember> members = memberMapper.selectBatchIds(memberIds);
        if (members == null || members.isEmpty()) {
            vo.setCustomMemberNames("-");
            return;
        }
        String names = members.stream()
            .map(member -> StringUtils.defaultIfBlank(member.getRealName(), member.getNickname()))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining("、"));
        vo.setCustomMemberNames(StringUtils.defaultIfBlank(names, "-"));
    }

    private String joinMemberIds(List<Long> memberIds) {
        List<Long> clean = cleanMemberIds(memberIds);
        if (clean.isEmpty()) {
            return null;
        }
        return clean.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> cleanMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return new ArrayList<>();
        }
        return memberIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private List<Long> parseMemberIds(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        String[] split = raw.split(",");
        List<Long> result = new ArrayList<>();
        for (String value : split) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            try {
                Long id = Long.parseLong(value.trim());
                if (!result.contains(id)) {
                    result.add(id);
                }
            } catch (Exception ignore) {
                // ignore invalid id token
            }
        }
        return result;
    }


    private String currentTenantId() {
        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }

}
