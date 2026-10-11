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

import java.time.LocalDate;

/**
 * 私教/小班课上课提醒任务（建议每天20:00触发）
 *
 * 说明：当前版本先完成提醒状态落库与日志追踪，
 * 小程序订阅消息发送可在后续接入消息网关后补齐。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@JobExecutor(name = "fitnessClassReminderTask")
public class FitnessClassReminderTask {

    private final JdbcTemplate jdbcTemplate;

    @Value("${snail-job.enabled:false}")
    private boolean snailJobEnabled;

    @Value("${fitness.schedule.local-enabled:true}")
    private boolean localScheduleEnabled;

    /**
     * 本地定时模式（snail-job.enabled=false 时生效）
     */
    @Scheduled(cron = "${fitness.schedule.class-reminder-cron:0 0 20 * * ?}")
    public void localScheduleExecute() {
        if (!localScheduleEnabled || snailJobEnabled) {
            return;
        }
        int affected = executeCore();
        log.info("[fitnessClassReminderTask][local-scheduled] 已标记提醒人数={}", affected);
    }

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        int affected = executeCore();
        SnailJobLog.REMOTE.info("[fitnessClassReminderTask] 目标日期={}，已标记提醒人数={}。", LocalDate.now().plusDays(1), affected);
        return ExecuteResult.success("affected=" + affected);
    }

    private int executeCore() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        String updateSql = """
            UPDATE fit_schedule_student ss
            INNER JOIN fit_course_schedule s ON s.schedule_id = ss.schedule_id AND s.del_flag = '0'
            SET ss.remind_status = '1',
                ss.update_time = NOW()
            WHERE ss.del_flag = '0'
              AND ss.remind_status = '0'
              AND ss.attendance_status = 'pending'
              AND s.schedule_mode IN ('private', 'small_group')
              AND s.schedule_date = ?
              AND s.schedule_status IN ('draft', 'open')
            """;
        return jdbcTemplate.update(updateSql, targetDate);
    }
}
