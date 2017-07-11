package com.alibaba.cacher.support.shooting;

import com.google.common.base.StandardSystemProperty;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author jifang.zjf
 * @since 2017/7/10 下午5:52.
 */
public class SqliteShootingMXBeanImpl extends AbstractDBShootingMXBean {

    public SqliteShootingMXBeanImpl() {
        this(StandardSystemProperty.USER_HOME.value() + "/.sqlite.db");
    }

    public SqliteShootingMXBeanImpl(String dbPath) {
        super(dbPath, Collections.emptyMap());
    }

    @Override
    protected Supplier<JdbcOperations> jdbcOperationsSupplier(String dbPath, Map<String, Object> context) {
        return () -> {
            SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
            dataSource.setDriverClassName("org.sqlite.JDBC");
            dataSource.setUrl(String.format("jdbc:sqlite:%s", dbPath));

            JdbcTemplate template = new JdbcTemplate(dataSource);
            template.execute("CREATE TABLE IF NOT EXISTS t_hit_rate(" +
                    "id BIGINT     IDENTITY PRIMARY KEY," +
                    "pattern       VARCHAR(64) NOT NULL UNIQUE," +
                    "hit_count     BIGINT      NOT NULL     DEFAULT 0," +
                    "require_count BIGINT      NOT NULL     DEFAULT 0," +
                    "version       BIGINT      NOT NULL     DEFAULT 0)");

            return template;
        };
    }

    @Override
    protected Stream<DataDO> transferResults(List<Map<String, Object>> mapResults) {
        return mapResults.stream().map(result -> {
            DataDO dataDO = new DataDO();
            dataDO.setHitCount((Integer) result.get("hit_count"));
            dataDO.setPattern((String) result.get("pattern"));
            dataDO.setRequireCount((Integer) result.get("require_count"));
            dataDO.setVersion((Integer) result.get("version"));

            return dataDO;
        });
    }
}
