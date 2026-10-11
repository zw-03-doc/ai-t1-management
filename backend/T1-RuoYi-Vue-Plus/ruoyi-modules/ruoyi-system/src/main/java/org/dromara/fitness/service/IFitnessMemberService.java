package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessMemberBo;
import org.dromara.fitness.domain.vo.FitnessMemberVo;

import java.util.Collection;
import java.util.List;

/**
 * 会员 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessMemberService {

    FitnessMemberVo queryById(Long memberId);

    TableDataInfo<FitnessMemberVo> queryPageList(FitnessMemberBo bo, PageQuery pageQuery);

    List<FitnessMemberVo> queryList(FitnessMemberBo bo);

    Boolean insertByBo(FitnessMemberBo bo);

    Boolean updateByBo(FitnessMemberBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
