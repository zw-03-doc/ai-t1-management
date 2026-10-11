package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitStore;
import org.dromara.fitness.domain.bo.FitnessStoreBo;
import org.dromara.fitness.domain.vo.FitnessStoreVo;

/**
 * 门店 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessStoreMapper extends BaseMapperPlus<FitStore, FitnessStoreVo> {

    Page<FitnessStoreVo> selectPageStoreList(@Param("page") Page<FitStore> page, @Param("bo") FitnessStoreBo bo);

    java.util.List<FitnessStoreVo> selectStoreList(@Param("bo") FitnessStoreBo bo);

    FitnessStoreVo selectStoreById(@Param("storeId") Long storeId);

}
