package org.dromara.job.snailjob;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 私教/小班排课过期任务（建议每天02:00触发）
 *
 * 规则：已结束且无签到核销记录的定向排课学员标记为过期，
 * 不触发扣卡逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@JobExecutor(name = "fitnessPrivateScheduleExpireTask")
public class FitnessPrivateScheduleExpireTask {

    private final JdbcTemplate jdbcTemplate;

    @Value("${snail-job.enabled:false}")
    private boolean snailJobEnabled;

    @Value("${fitness.schedule.local-enabled:true}")
    private boolean localScheduleEnabled;

    /**
     * 本地定时模式（snail-job.enabled=false 时生效）
     */
    @Scheduled(cron = "${fitness.schedule.private-expire-cron:0 0 2 * * ?}")
    public void localScheduleExecute() {
        if (!localScheduleEnabled || snailJobEnabled) {
            return;
        }
        Result result = executeCore();
        log.info("[fitnessPrivateScheduleExpireTask][local-scheduled] 过期学员={}，完结排课={}", result.expiredStudents, result.closedSchedules);
    }

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        Result result = executeCore();
        SnailJobLog.REMOTE.info("[fitnessPrivateScheduleExpireTask] 过期学员={}，完结排课={}。", result.expiredStudents, result.closedSchedules);
        return ExecuteResult.success("expiredStudents=" + result.expiredStudents + ", closedSchedules=" + result.closedSchedules);
    }

    private Result executeCore() {
        String expireStudentSql = """
            UPDATE fit_schedule_student ss
            INNER JOIN fit_course_schedule s ON s.schedule_id = ss.schedule_id AND s.del_flag = '0'
            LEFT JOIN fit_checkin_record cr ON cr.schedule_id = ss.schedule_id AND cr.member_id = ss.member_id AND cr.del_flag = '0' AND cr.checkin_status = 'success'
            SET ss.attendance_status = 'expired',
                ss.update_time = NOW()
            WHERE ss.del_flag = '0'
              AND ss.attendance_status = 'pending'
              AND s.schedule_mode IN ('private', 'small_group')
              AND s.end_time < NOW()
              AND cr.checkin_id IS NULL
            """;
        int expiredStudents = jdbcTemplate.update(expireStudentSql);

        String closeScheduleSql = """
            UPDATE fit_course_schedule s
            SET s.schedule_status = 'finished',
                s.update_time = NOW()
            WHERE s.del_flag = '0'
              AND s.schedule_mode IN ('private', 'small_group')
              AND s.end_time < NOW()
              AND s.schedule_status NOT IN ('finished', 'cancelled')
            """;
        int closedSchedules = jdbcTemplate.update(closeScheduleSql);
        return new Result(expiredStudents, closedSchedules);
    }

    private static class Result {
        private final int expiredStudents;
        private final int closedSchedules;

        private Result(int expiredStudents, int closedSchedules) {
            this.expiredStudents = expiredStudents;
            this.closedSchedules = closedSchedules;
        }
    }
}
