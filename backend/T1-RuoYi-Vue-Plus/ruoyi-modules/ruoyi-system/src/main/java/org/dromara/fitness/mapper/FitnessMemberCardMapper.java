package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitMemberCard;
import org.dromara.fitness.domain.bo.FitnessMemberCardBo;
import org.dromara.fitness.domain.vo.FitnessMemberCardVo;

import java.util.List;

/**
 * 会员持卡 Mapper
 *
 * @author CodeBuddy
 */
public interface FitnessMemberCardMapper extends BaseMapperPlus<FitMemberCard, FitnessMemberCardVo> {

    Page<FitnessMemberCardVo> selectPageMemberCardList(@Param("page") Page<FitMemberCard> page,
                                                       @Param("bo") FitnessMemberCardBo bo);

    List<FitnessMemberCardVo> selectMemberCardList(@Param("bo") FitnessMemberCardBo bo);

    FitnessMemberCardVo selectMemberCardById(@Param("cardId") Long cardId);
}
