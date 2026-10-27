package org.dromara.fitness.utils;

import org.dromara.common.core.exception.ServiceException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Date;

/** All calendar dates are interpreted in the gym's Asia/Shanghai timezone. */
public final class FitnessBookingPolicy {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private FitnessBookingPolicy() { }

    public static int range(Integer value, int fallback, int min, int max, String label) {
        int result = value == null ? fallback : value;
        if (result < min || result > max) throw new ServiceException(label + "必须在" + min + "至" + max + "之间");
        return result;
    }

    public static BigDecimal price(BigDecimal value) {
        BigDecimal result = value == null ? BigDecimal.ZERO : value;
        if (result.signum() < 0 || result.compareTo(new BigDecimal("999999.99")) > 0 || result.stripTrailingZeros().scale() > 2)
            throw new ServiceException("展示价格须为0至999999.99元，最多两位小数");
        return result;
    }

    public static Date daysBefore(Date date, int days) {
        return Date.from(java.time.Instant.ofEpochMilli(date.getTime()).atZone(ZONE).minusDays(days).toInstant());
    }

    public static Date minutesBefore(Date date, int minutes) {
        return new Date(date.getTime() - minutes * 60_000L);
    }

    public static Date bookingOpensAt(Date start, int advanceDays) {
        return Date.from(java.time.Instant.ofEpochMilli(start.getTime()).atZone(ZONE).toLocalDate()
            .minusDays(advanceDays).atStartOfDay(ZONE).toInstant());
    }

    public static void validateWindow(Date now, Date opens, Date closes, String label) {
        if (opens != null && now.before(opens)) throw new ServiceException(label + "尚未开放");
        if (closes != null && !now.before(closes)) throw new ServiceException(label + "已截止");
    }

    public static void validateScheduleDate(Date scheduleDate, Date start, Date end) {
        if (scheduleDate == null || start == null || end == null || !end.after(start))
            throw new ServiceException("请设置完整的排课日期、开始及结束时间，结束时间须晚于开始时间");
        if (!java.time.Instant.ofEpochMilli(scheduleDate.getTime()).atZone(ZONE).toLocalDate().equals(java.time.Instant.ofEpochMilli(start.getTime()).atZone(ZONE).toLocalDate()))
            throw new ServiceException("排课日期必须与北京时间的开始日期一致");
        if (end.getTime() - start.getTime() > 24 * 60 * 60_000L)
            throw new ServiceException("单次排课时长不能超过24小时");
    }
}
