package org.dromara.test.fitness;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.dromara.fitness.domain.bo.FitnessMiniBo;
import org.dromara.fitness.mapper.FitnessMiniMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class FitnessMemberScheduleQueryTest {
    @Test
    void tenantRewritePreservesScheduleFiltersAndOrdering() throws Exception {
        var config = new Configuration();
        String resource = "mapper/fitness/FitnessMiniMapper.xml";
        try (var mapping = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(mapping, config, resource, config.getSqlFragments()).parse();
        }
        var statement = config.getMappedStatement(FitnessMiniMapper.class.getName() + ".selectScheduleList");
        var tenant = new TenantLineInnerInterceptor(() -> new StringValue("000000"));
        for (int filters = 0; filters < 32; filters++) {
            var query = new FitnessMiniBo.ScheduleQueryBo();
            if ((filters & 1) != 0) query.setCourseId(101L);
            if ((filters & 2) != 0) query.setStoreId(201L);
            if ((filters & 4) != 0) query.setCoachId(301L);
            if ((filters & 8) != 0) query.setCourseType("group");
            if ((filters & 16) != 0) query.setScheduleDate(new Date());
            var bound = statement.getBoundSql(Map.of("memberId", 1L, "bo", query));
            String sql = tenant.parserSingle(bound.getSql(), null);
            assertEquals(bound.getParameterMappings().size(), sql.chars().filter(c -> c == '?').count(), sql);
            assertTrue(sql.contains("ss.member_id = ?"), sql);
            assertTrue(sql.contains("tenant_id = '000000'"), sql);
            assertTrue(sql.contains("ORDER BY s.start_time ASC"), sql);
        }
    }
}
