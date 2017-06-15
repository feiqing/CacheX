package com.alibaba.cacher.support.shooting;

import com.alibaba.cacher.shooting.ShootingMXBean;
import org.springframework.jdbc.core.JdbcOperations;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jifang.zjf
 * @since 2017/6/12 下午12:55.
 */
public abstract class AbstractDBShootingMXBean implements ShootingMXBean {

    private static final long _5S = 5 * 1000;

    private long hitLastTime = System.currentTimeMillis() - _5S;

    private long requireLastTime = System.currentTimeMillis() - _5S;

    private ConcurrentMap<String, AtomicLong> hitMapTS = new ConcurrentHashMap<>();

    private ConcurrentMap<String, AtomicLong> requireMapTS = new ConcurrentHashMap<>();

    private final Lock lock = new ReentrantLock();

    private JdbcOperations jdbcOperations;

    private Properties configs;

    /**
     * 1. create JdbcOperations
     * 2. init db(like: load sql script, create table, init table...)
     *
     * @param dbPath :EmbeddedDatabase file temporary storage directory.
     * @return
     */
    protected abstract Supplier<JdbcOperations> operationsSupplier(String dbPath);

    /**
     * convert DB Map Result to DataDO(Stream)
     *
     * @param mapResults: {@code List<Map<String, Object>>} result from query DB.
     * @return
     */
    protected abstract Stream<DataDO> transferResults(List<Map<String, Object>> mapResults);

    public AbstractDBShootingMXBean(String dbPath) {
        InputStream yamlStream = ClassLoader.getSystemResourceAsStream("shooting_sql.yaml");
        this.configs = new Yaml().loadAs(yamlStream, Properties.class);

        this.jdbcOperations = operationsSupplier(dbPath).get();
    }

    @Override
    public void hitIncr(String pattern, int count) {
        hitMapTS.computeIfAbsent(pattern,
                (k) -> new AtomicLong(0L))
                .addAndGet(count);

        long currentTime = System.currentTimeMillis();
        if (currentTime - hitLastTime >= _5S) {
            countAddCas("hit_count", pattern, count);
            hitMapTS.clear();

            hitLastTime = currentTime;
        }
    }

    @Override
    public void requireIncr(String pattern, int count) {
        requireMapTS.computeIfAbsent(pattern,
                (k) -> new AtomicLong(0L))
                .addAndGet(count);

        long currentTime = System.currentTimeMillis();
        if (currentTime - requireLastTime >= _5S) {
            countAddCas("require_count", pattern, count);
            requireMapTS.clear();

            requireLastTime = currentTime;
        }
    }

    @Override
    public Map<String, ShootingDO> getShooting() {
        List<DataDO> dataDOS = queryAll();
        AtomicLong statisticsHit = new AtomicLong(0);
        AtomicLong statisticsRequired = new AtomicLong(0);

        // 汇总各pattern命中率
        Map<String, ShootingDO> result = dataDOS.stream().collect(Collectors.toMap(DataDO::getPattern, (dataDO) -> {
            statisticsHit.addAndGet(dataDO.hitCount);
            statisticsRequired.addAndGet(dataDO.requireCount);
            return ShootingDO.newInstance(dataDO.hitCount, dataDO.requireCount);
        }, ShootingDO::mergeShootingDO, LinkedHashMap::new));

        // 汇总全局命中率
        result.put(getSummaryName(), ShootingDO.newInstance(statisticsHit.get(), statisticsRequired.get()));

        return result;
    }

    @Override
    public void reset(String pattern) {
        jdbcOperations.update(configs.getProperty("delete"), pattern);
    }

    @Override
    public void resetAll() {
        jdbcOperations.update(configs.getProperty("truncate"));
    }

    private void countAddCas(String column, String pattern, long count) {
        Optional<DataDO> dataOptional = queryObject(pattern);

        // 如果原先该pattern有对应的值, 则update
        if (dataOptional.isPresent()) {
            DataDO dataDO = dataOptional.get();
            while (update(column, pattern, getObjectCount(dataDO, column, count), dataDO.version) <= 0) {
                dataDO = queryObject(pattern).get();
            }
        } else {
            // 首次插入
            lock.lock();
            try {
                // double check, 防止多线程并发查询到尚未插入
                dataOptional = queryObject(pattern);
                if (dataOptional.isPresent()) {
                    update(column, pattern, count, dataOptional.get().version);
                } else {
                    insert(column, pattern, count);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private Optional<DataDO> queryObject(String pattern) {
        String selectSql = configs.getProperty("select");
        List<Map<String, Object>> mapResults = jdbcOperations.queryForList(selectSql, pattern);

        return transferResults(mapResults).findFirst();
    }

    private List<DataDO> queryAll() {
        String selectAllQuery = configs.getProperty("selectAll");
        List<Map<String, Object>> mapResults = jdbcOperations.queryForList(selectAllQuery);

        return transferResults(mapResults).collect(Collectors.toList());
    }

    private int insert(String column, String pattern, long count) {
        String insertSql = String.format(configs.getProperty("insert"), column);

        return jdbcOperations.update(insertSql, pattern, count);
    }

    private int update(String column, String pattern, long count, long version) {
        String updateSql = String.format(configs.getProperty("update"), column);

        return jdbcOperations.update(updateSql, count, pattern, version);
    }

    private long getObjectCount(DataDO data, String column, long countOffset) {
        long lastCount = column.equals("hit_count") ? data.hitCount : data.requireCount;

        return lastCount + countOffset;
    }

    protected static final class DataDO {

        private String pattern;

        private long hitCount;

        private long requireCount;

        private long version;

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }

        public void setHitCount(long hitCount) {
            this.hitCount = hitCount;
        }

        public void setRequireCount(long requireCount) {
            this.requireCount = requireCount;
        }

        public void setVersion(long version) {
            this.version = version;
        }
    }
}
