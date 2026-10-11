package org.dromara.common.job.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 本地定时任务开关配置。
 *
 * 说明：
 * 1. 统一启用 Spring Scheduling 能力；
 * 2. 具体任务是否执行由任务类内的配置开关控制，
 *    与 SnailJob 可并存且互斥执行。
 */
@AutoConfiguration
@EnableScheduling
public class LocalScheduleConfig {
}
