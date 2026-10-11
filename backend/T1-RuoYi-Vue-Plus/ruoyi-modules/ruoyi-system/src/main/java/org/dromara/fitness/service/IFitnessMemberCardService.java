package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessMemberCardBo;
import org.dromara.fitness.domain.vo.FitnessMemberCardVo;

import java.util.List;

/**
 * 会员持卡 Service 接口
 *
 * @author CodeBuddy
 */
public interface IFitnessMemberCardService {

    FitnessMemberCardVo queryById(Long cardId);

    TableDataInfo<FitnessMemberCardVo> queryPageList(FitnessMemberCardBo bo, PageQuery pageQuery);

    List<FitnessMemberCardVo> queryList(FitnessMemberCardBo bo);

    Boolean updateStatus(Long cardId, String cardStatus);

    Boolean adjustRights(Long cardId, FitnessMemberCardBo bo);
}
