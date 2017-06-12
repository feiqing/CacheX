package com.alibaba.cacher.support.hitrate;

import com.alibaba.cacher.hitrate.HitRateMXBean;
import org.springframework.jdbc.core.JdbcOperations;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jifang.zjf
 * @since 2017/6/12 下午12:55.
 */
public abstract class AbstractDBHitRateMXBean implements HitRateMXBean {

    private static final long _5S = 5 * 1000;

    private long lastTime = System.currentTimeMillis();

    private ConcurrentMap<String, AtomicLong> hitMapTS = new ConcurrentHashMap<>();

    private ConcurrentMap<String, AtomicLong> requireMapTS = new ConcurrentHashMap<>();

    private final Lock lock = new ReentrantLock();

    private JdbcOperations jdbcOperations;

    private Map<String, String> configs;


    /**
     * 1. create JdbcOperations
     * 2. init db(create table)
     *
     * @param dbPath
     * @return
     */
    protected abstract Supplier<JdbcOperations> operationsSupplier(String dbPath);

    /**
     * convert DB Map Result to DataDO(Stream)
     *
     * @param mapResults
     * @return
     */
    protected abstract Stream<DataDO> processMapResults(List<Map<String, Object>> mapResults);

    @SuppressWarnings("unchecked")
    public AbstractDBHitRateMXBean(String dbPath) {
        this.configs = (Map<String, String>) new Yaml().load(ClassLoader.getSystemResourceAsStream("sql.yaml"));
        this.jdbcOperations = operationsSupplier(dbPath).get();
    }

    @Override
    public void hitIncr(String pattern, int count) {
        hitMapTS.computeIfAbsent(pattern,
                (k) -> new AtomicLong(0L))
                .addAndGet(count);

        if (isNeedPersistent()) {
            countAddCas("hit_count", pattern, count);
        }
    }

    @Override
    public void requireIncr(String pattern, int count) {
        requireMapTS.computeIfAbsent(pattern,
                (k) -> new AtomicLong(0L))
                .addAndGet(count);
        if (isNeedPersistent()) {
            countAddCas("require_count", pattern, count);
        }
    }

    @Override
    public Map<String, RateDO> getHitRate() {
        List<DataDO> dataDOS = queryAll();
        AtomicLong totalHit = new AtomicLong(0);
        AtomicLong totalRequire = new AtomicLong(0);

        Map<String, RateDO> result = dataDOS.stream().collect(Collectors.toMap((dataDO) -> dataDO.pattern, (dataDO) -> {
            totalHit.addAndGet(dataDO.hitCount);
            totalRequire.addAndGet(dataDO.requireCount);
            return RateDO.newInstance(dataDO.hitCount, dataDO.requireCount);
        }, DataDO.mergeFunction, LinkedHashMap::new));

        result.put(getSummaryName(), RateDO.newInstance(totalHit.get(), totalRequire.get()));
        return result;
    }

    @Override
    public void reset(String pattern) {
        jdbcOperations.update(configs.get("delete"), pattern);
    }

    @Override
    public void resetAll() {
        jdbcOperations.update(configs.get("truncate"));
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
        List<Map<String, Object>> mapResults = jdbcOperations.queryForList(configs.get("select"), pattern);

        return processMapResults(mapResults).findFirst();
    }

    private List<DataDO> queryAll() {
        List<Map<String, Object>> mapResults = jdbcOperations.queryForList(configs.get("selectAll"));
        return processMapResults(mapResults).collect(Collectors.toList());
    }

    private void insert(String column, String pattern, long count) {
        jdbcOperations.update(String.format(configs.get("insert"), column), pattern, count);
    }

    private int update(String column, String pattern, long count, long version) {
        return jdbcOperations.update(String.format(configs.get("update"), column), count, pattern, version);
    }

    private long getObjectCount(DataDO data, String column, long coutOffset) {
        long lastCount = column.equals("hit_count") ? data.hitCount : data.requireCount;
        return lastCount + coutOffset;
    }

    protected static final class DataDO {

        private String pattern;

        private long hitCount;

        private long requireCount;

        private long version;

        public void setPattern(String pattern) {
            this.pattern = pattern;
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

        private static final BinaryOperator<RateDO> mergeFunction = (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    private boolean isNeedPersistent() {
        boolean result = false;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime >= _5S) {
            this.lastTime = currentTime;
            result = true;
        }

        return result;
    }
}
