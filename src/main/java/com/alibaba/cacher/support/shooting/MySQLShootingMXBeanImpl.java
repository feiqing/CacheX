package com.alibaba.cacher.support.shooting;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author jifang.zjf
 * @since 2017/7/10 下午6:50.
 */
public class MySQLShootingMXBeanImpl extends AbstractDBShootingMXBean {

    public MySQLShootingMXBeanImpl(String host, long port, String username, String password) {
        this(host, port,
                System.getProperty("product.name", "unnamed"),
                username, password);
    }

    public MySQLShootingMXBeanImpl(String host, long port, String database, String username, String password) {
        super(database, newMap(
                "host", host,
                "port", port,
                "username", username,
                "password", password
        ));
    }

    private MySQLShootingMXBeanImpl(String hotsPortDataBase) {
        super(hotsPortDataBase, Collections.emptyMap());
    }

    @Override
    protected Supplier<JdbcOperations> jdbcOperationsSupplier(String dbPath, Map<String, Object> context) {
        return () -> {
            SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setUrl(String.format("jdbc:mysql://%s:%s/%s", context.get("host"), context.get("port"), dbPath));
            dataSource.setUsername((String) context.get("username"));
            dataSource.setPassword((String) context.get("password"));

            JdbcTemplate template = new JdbcTemplate(dataSource);
            template.execute("CREATE TABLE IF NOT EXISTS t_hit_rate(" +
                    "id BIGINT     PRIMARY KEY AUTO_INCREMENT," +
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
            dataDO.setRequireCount((Long) result.get("require_count"));
            dataDO.setHitCount((Long) result.get("hit_count"));
            dataDO.setPattern((String) result.get("pattern"));
            dataDO.setVersion((Long) result.get("version"));

            return dataDO;
        });
    }

    private static Map<String, Object> newMap(Object... keyValues) {
        Map<String, Object> map = new HashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object value = keyValues[i + 1];

            map.put(key, value);
        }

        return map;
    }
}
