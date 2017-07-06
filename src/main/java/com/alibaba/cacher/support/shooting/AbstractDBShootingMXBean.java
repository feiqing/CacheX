package com.alibaba.cacher.support.shooting;

import com.alibaba.cacher.ShootingMXBean;
import com.alibaba.cacher.domain.Pair;
import org.springframework.jdbc.core.JdbcOperations;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
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

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("cacher:shooting-db-writer");
        thread.setDaemon(true);
        return thread;
    });

    private BlockingQueue<Pair<String, Integer>> hitQueue = new LinkedBlockingQueue<>();

    private BlockingQueue<Pair<String, Integer>> requireQueue = new LinkedBlockingQueue<>();

    private final Lock lock = new ReentrantLock();

    private JdbcOperations jdbcOperations;

    private Properties configs;

    private volatile boolean isShutdown = false;

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
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("sql.yaml");
        this.configs = new Yaml().loadAs(resource, Properties.class);

        this.jdbcOperations = operationsSupplier(dbPath).get();
        executor.submit(() -> {
            while (!isShutdown) {
                dumpToDB(hitQueue, "hit_count");
                dumpToDB(requireQueue, "require_count");
            }
        });
    }

    @Override
    public void hitIncr(String pattern, int count) {
        hitQueue.add(Pair.of(pattern, count));
    }

    @Override
    public void requireIncr(String pattern, int count) {
        requireQueue.add(Pair.of(pattern, count));
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

    private void dumpToDB(BlockingQueue<Pair<String, Integer>> queue, String column) {
        long times = 0;
        Pair<String, Integer> head;

        // 将queue中所有的 || 前100条数据聚合到一个暂存Map中
        Map<String, AtomicLong> holdMap = new HashMap<>();
        while ((head = queue.poll()) != null && times <= 100) {
            holdMap
                    .computeIfAbsent(head.getLeft(), (key) -> new AtomicLong(0L))
                    .addAndGet(head.getRight());
            ++times;
        }

        holdMap.forEach((pattern, count) -> countAddCas(column, pattern, count.get()));
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
        String selectAllQuery = configs.getProperty("select_all");
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
