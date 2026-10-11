package org.dromara.fitness.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.fitness.domain.bo.FitnessReservationBo;
import org.dromara.fitness.domain.vo.FitnessReservationVo;

import java.util.Collection;
import java.util.List;

public interface IFitnessReservationService {

    FitnessReservationVo queryById(Long reservationId);

    TableDataInfo<FitnessReservationVo> queryPageList(FitnessReservationBo bo, PageQuery pageQuery);

    List<FitnessReservationVo> queryList(FitnessReservationBo bo);

    Boolean insertByBo(FitnessReservationBo bo);

    Boolean updateByBo(FitnessReservationBo bo);

    Boolean cancelReservation(Long reservationId, String cancelReason);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
