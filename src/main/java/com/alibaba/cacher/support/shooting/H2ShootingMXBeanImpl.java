package com.alibaba.cacher.support.shooting;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author jifang.zjf
 * @since 2017/6/8 下午9:41.
 */
public class H2ShootingMXBeanImpl extends AbstractDBShootingMXBean {

    public H2ShootingMXBeanImpl() {
        this(System.getProperty("user.home") + "/.H2/cacher");
    }

    public H2ShootingMXBeanImpl(String dbPath) {
        super(dbPath);
    }

    @Override
    protected Supplier<JdbcOperations> jdbcOperationsSupplier(String dbPath) {
        return () -> {
            SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl(String.format("jdbc:h2:%s;AUTO_SERVER=TRUE;AUTO_RECONNECT=TRUE;AUTO_SERVER=TRUE", dbPath));
            dataSource.setUsername("cacher");
            dataSource.setPassword("cacher");

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
        return mapResults.stream().map((map) -> {
            AbstractDBShootingMXBean.DataDO dataDO = new AbstractDBShootingMXBean.DataDO();
            dataDO.setPattern((String) map.get("PATTERN"));
            dataDO.setHitCount((long) map.get("HIT_COUNT"));
            dataDO.setRequireCount((long) map.get("REQUIRE_COUNT"));
            dataDO.setVersion((long) map.get("VERSION"));

            return dataDO;
        });
    }

    @PreDestroy
    public void tearDown() {
        super.tearDown();
    }
}
