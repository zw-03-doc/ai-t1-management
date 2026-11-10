-- The existing bind_course_id holds the first course for older clients.
CREATE TABLE IF NOT EXISTS fit_card_plan_extra_course (
    tenant_id VARCHAR(20) NOT NULL,
    plan_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    sort_no INT NOT NULL,
    PRIMARY KEY (tenant_id, plan_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
