package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.domain.FitCourseSchedule;
import org.dromara.fitness.domain.bo.FitnessScheduleStatusBo;
import org.dromara.fitness.mapper.FitnessScheduleMapper;
import org.dromara.fitness.service.impl.FitnessScheduleServiceImpl;
import org.dromara.fitness.service.support.FitnessOperationsService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class FitnessScheduleStatusTest {
    private final FitnessOperationsService operations = mock(FitnessOperationsService.class);
    private final FitnessScheduleMapper mapper = mock(FitnessScheduleMapper.class);
    private final FitnessScheduleServiceImpl service = new FitnessScheduleServiceImpl(operations, mapper);

    private FitCourseSchedule existing(String status) {
        var row = new FitCourseSchedule();
        row.setScheduleId(11L);
        row.setStartTime(new Date());
        row.setScheduleStatus(status);
        when(operations.lockSchedule(11L)).thenReturn(row);
        return row;
    }
    private int change(String expected, String target) {
        return service.changeStatus(new FitnessScheduleStatusBo(target, List.of(new FitnessScheduleStatusBo.SelectedSchedule(11L, expected))));
    }

    @Test void replayIsANoOpAndAStaleSelectionCannotOverwriteANewerState() {
        existing("closed");
        assertEquals(0, change("open", "closed"));
        assertThrows(ServiceException.class, () -> change("open", "draft"));
        verifyNoInteractions(mapper);
    }

    @Test void activeReservationsCannotBeHiddenAsDraftOrCancelled() {
        existing("open").setReservedCount(1);
        assertThrows(ServiceException.class, () -> change("open", "cancelled"));
        assertThrows(ServiceException.class, () -> change("open", "draft"));
        verifyNoInteractions(mapper);
    }
}
