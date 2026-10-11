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
import org.dromara.fitness.domain.FitStore;
import org.dromara.fitness.domain.bo.FitnessStoreBo;
import org.dromara.fitness.domain.vo.FitnessStoreVo;
import org.dromara.fitness.mapper.FitnessStoreMapper;
import org.dromara.fitness.service.IFitnessStoreService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 门店 Service 实现
 *
 * @author CodeBuddy
 */
@RequiredArgsConstructor
@Service
public class FitnessStoreServiceImpl implements IFitnessStoreService {

    private final FitnessStoreMapper baseMapper;

    @Override
    public FitnessStoreVo queryById(Long storeId) {
        return baseMapper.selectStoreById(storeId);
    }

    @Override
    public TableDataInfo<FitnessStoreVo> queryPageList(FitnessStoreBo bo, PageQuery pageQuery) {
        return TableDataInfo.build(baseMapper.selectPageStoreList(pageQuery.build(), bo));
    }

    @Override
    public List<FitnessStoreVo> queryList(FitnessStoreBo bo) {
        return baseMapper.selectStoreList(bo);
    }

    @Override
    public Boolean insertByBo(FitnessStoreBo bo) {
        FitStore add = MapstructUtils.convert(bo, FitStore.class);
        add.setStoreId(IdGeneratorUtil.nextLongId());
        add.setTenantId(currentTenantId());
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setStoreId(add.getStoreId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(FitnessStoreBo bo) {
        FitStore update = MapstructUtils.convert(bo, FitStore.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean updateStatus(Long storeId, String status) {
        FitStore update = new FitStore();
        update.setStoreId(storeId);
        update.setStatus(status);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<FitStore>().in(FitStore::getStoreId, ids));
            if (count != ids.size()) {
                throw new ServiceException("部分数据不存在或已被删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void validEntityBeforeSave(FitStore entity) {
        if (baseMapper.exists(new LambdaQueryWrapper<FitStore>().eq(FitStore::getStoreCode, entity.getStoreCode()).ne(entity.getStoreId() != null, FitStore::getStoreId, entity.getStoreId()))) {
            throw new ServiceException("门店编码已存在");
        }
    }

    private String currentTenantId() {
        return StringUtils.defaultIfBlank(TenantHelper.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
    }

}
