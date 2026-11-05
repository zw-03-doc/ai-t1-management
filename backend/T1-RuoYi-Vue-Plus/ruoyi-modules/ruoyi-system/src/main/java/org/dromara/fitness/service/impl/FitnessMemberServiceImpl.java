package org.dromara.fitness.service.impl;

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
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.bo.FitnessMemberBo;
import org.dromara.fitness.domain.vo.FitnessMemberVo;
import org.dromara.fitness.mapper.FitnessMemberMapper;
import org.dromara.fitness.service.IFitnessMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.dromara.fitness.service.support.FitnessSessionRevoker;
import java.util.Set;

import java.util.Collection;
import java.util.List;

/**
 * 会员 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessMemberServiceImpl implements IFitnessMemberService {

    private final FitnessMemberMapper baseMapper;

    @Override
    public FitnessMemberVo queryById(Long memberId) {
        return baseMapper.selectMemberById(memberId);
    }

    @Override
    public TableDataInfo<FitnessMemberVo> queryPageList(FitnessMemberBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageMemberList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessMemberVo> queryList(FitnessMemberBo bo) {
        return baseMapper.selectMemberList(bo);
    }

    @Override
    public Boolean insertByBo(FitnessMemberBo bo) {
        FitMember add = MapstructUtils.convert(bo, FitMember.class);
        add.setMemberId(IdGeneratorUtil.nextLongId());
        add.setTenantId(currentTenantId());
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setMemberId(add.getMemberId());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FitnessMemberBo bo) {
        FitMember existing = baseMapper.selectOne(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberId, bo.getMemberId()).last("FOR UPDATE"));
        if (existing == null) throw new ServiceException("会员不存在");
        FitMember update = MapstructUtils.convert(bo, FitMember.class);
        validEntityBeforeSave(update);
        boolean changed = baseMapper.updateById(update) > 0;
        if (changed && !"normal".equals(update.getMemberStatus())) FitnessSessionRevoker.member(existing.getTenantId(), existing.getMemberId());
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        var members = baseMapper.selectList(new LambdaQueryWrapper<FitMember>().in(FitMember::getMemberId, ids).orderByAsc(FitMember::getMemberId).last("FOR UPDATE"));
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitMember>().in(FitMember::getMemberId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分数据不存在或已被删除");
            }
        }
        boolean deleted = baseMapper.deleteByIds(ids) > 0;
        if (deleted) members.forEach(member -> FitnessSessionRevoker.member(member.getTenantId(), member.getMemberId()));
        return deleted;
    }

    private void validEntityBeforeSave(FitMember entity) {
        if (entity.getMemberStatus() == null || !Set.of("normal", "frozen", "lost").contains(entity.getMemberStatus()))
            throw new ServiceException("请选择有效会员状态");
        if (baseMapper.exists(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMemberNo, entity.getMemberNo()).ne(entity.getMemberId() != null, FitMember::getMemberId, entity.getMemberId()))) {
            throw new ServiceException("会员编号已存在");
        }
        if (StringUtils.isNotBlank(entity.getMobile()) && baseMapper.exists(new LambdaQueryWrapper<FitMember>().eq(FitMember::getMobile, entity.getMobile()).ne(entity.getMemberId() != null, FitMember::getMemberId, entity.getMemberId()))) {
            throw new ServiceException("手机号已存在");
        }
    }

    private String currentTenantId() {
        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }

}
