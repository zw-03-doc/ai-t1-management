package org.dromara.test.fitness;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.fitness.utils.FitnessBookingPolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class FitnessBookingPolicyTest {
    private Date at(String iso) { return Date.from(Instant.parse(iso)); }

    @Test void sameDayBookingStartsAtShanghaiMidnight() {
        assertEquals(at("2026-10-25T16:00:00Z"), FitnessBookingPolicy.bookingOpensAt(at("2026-10-26T10:00:00Z"), 0));
        assertEquals(at("2026-10-18T16:00:00Z"), FitnessBookingPolicy.bookingOpensAt(at("2026-10-26T10:00:00Z"), 7));
    }

    @Test void cutoffIsExclusiveWhileOpeningIsInclusive() {
        Date open = at("2026-10-26T00:00:00Z"), close = at("2026-10-26T01:00:00Z");
        assertDoesNotThrow(() -> FitnessBookingPolicy.validateWindow(open, open, close, "预约"));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.validateWindow(close, open, close, "预约"));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.validateWindow(new Date(open.getTime()-1), open, close, "预约"));
    }

    @Test void scheduleUsesShanghaiDateAndAllowsCrossMidnightSession() {
        Date day = at("2026-10-25T16:00:00Z");
        assertDoesNotThrow(() -> FitnessBookingPolicy.validateScheduleDate(day, at("2026-10-26T15:30:00Z"), at("2026-10-26T16:30:00Z")));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.validateScheduleDate(day, at("2026-10-26T16:30:00Z"), at("2026-10-26T17:30:00Z")));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.validateScheduleDate(day, day, day));
    }

    @Test void decimalPriceRejectsNegativeAndFractionalCents() {
        assertEquals(BigDecimal.ZERO, FitnessBookingPolicy.price(BigDecimal.ZERO));
        assertEquals(new BigDecimal("199.90"), FitnessBookingPolicy.price(new BigDecimal("199.90")));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.price(new BigDecimal("-1")));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.price(new BigDecimal("19.999")));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.price(new BigDecimal("1000000")));
    }

    @Test void rejectsUnboundedCapacityAndCalendarRanges() {
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.range(0, 20, 1, 1000, "人数"));
        assertThrows(ServiceException.class, () -> FitnessBookingPolicy.range(366, 30, 1, 365, "天数"));
        assertEquals(20, FitnessBookingPolicy.range(null, 20, 1, 1000, "人数"));
    }
}
