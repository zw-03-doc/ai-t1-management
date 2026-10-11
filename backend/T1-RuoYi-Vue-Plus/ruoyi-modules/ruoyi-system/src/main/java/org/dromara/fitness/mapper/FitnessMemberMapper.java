package org.dromara.fitness.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.fitness.domain.FitMember;
import org.dromara.fitness.domain.bo.FitnessMemberBo;
import org.dromara.fitness.domain.vo.FitnessMemberVo;

/**
 * 会员 Mapper 接口
 *
 * @author CodeBuddy
 */
public interface FitnessMemberMapper extends BaseMapperPlus<FitMember, FitnessMemberVo> {

    Page<FitnessMemberVo> selectPageMemberList(@Param("page") Page<FitMember> page, @Param("bo") FitnessMemberBo bo);

    java.util.List<FitnessMemberVo> selectMemberList(@Param("bo") FitnessMemberBo bo);

    FitnessMemberVo selectMemberById(@Param("memberId") Long memberId);

}
