package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessStoreBo;
import org.dromara.fitness.domain.vo.FitnessStoreVo;

import java.util.Collection;
import java.util.List;

/**
 * 门店 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessStoreService {

    FitnessStoreVo queryById(Long storeId);

    TableDataInfo<FitnessStoreVo> queryPageList(FitnessStoreBo bo, PageQuery pageQuery);

    List<FitnessStoreVo> queryList(FitnessStoreBo bo);

    Boolean insertByBo(FitnessStoreBo bo);

    Boolean updateByBo(FitnessStoreBo bo);

    Boolean updateStatus(Long storeId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
