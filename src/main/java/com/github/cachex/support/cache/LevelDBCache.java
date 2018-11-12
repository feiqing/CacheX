package com.github.cachex.support.cache;

import com.github.cachex.ICache;
import com.github.jbox.serializer.ISerializer;
import com.github.jbox.serializer.support.Hession2Serializer;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

/**
 * @author jifang.zjf
 * @since 2017/6/9 上午10:52.
 */
public class LevelDBCache implements ICache {

    private static final Logger LOGGER = LoggerFactory.getLogger(LevelDBCache.class);

    private DB levelDB;

    private ISerializer serializer;

    public LevelDBCache() throws IOException {
        this(new Hession2Serializer());
    }

    public LevelDBCache(String levelFilePath) throws IOException {
        this(levelFilePath, new Hession2Serializer());
    }

    public LevelDBCache(ISerializer serializer) throws IOException {
        this(System.getProperty("user.home") + "/.LevelDB/", serializer);
    }

    public LevelDBCache(String levelFilePath, ISerializer serializer) throws IOException {
        Options options = new Options();
        options.compressionType(CompressionType.SNAPPY);
        options.createIfMissing(true);
        this.levelDB = factory.open(new File(levelFilePath), options);
        this.serializer = Optional.ofNullable(serializer).orElseThrow(NullPointerException::new);
    }

    @Override
    public Object read(String key) {
        return serializer.deserialize(levelDB.get(key.getBytes()));
    }

    @Override
    public void write(String key, Object value, long expire) {
        levelDB.put(key.getBytes(), serializer.serialize(value));
    }

    @Override
    public Map<String, Object> read(Collection<String> keys) {
        Map<String, Object> result = new HashMap<>(keys.size());
        keys.forEach((key) -> result.put(key, read(key)));

        return result;
    }

    @Override
    public void write(Map<String, Object> keyValueMap, long expire) {
        WriteBatch updates = levelDB.createWriteBatch();

        keyValueMap.forEach((key, value) -> updates.put(key.getBytes(), serializer.serialize(value)));

        levelDB.write(updates);
    }

    @Override
    public void remove(String... keys) {
        for (String key : keys) {
            levelDB.delete(key.getBytes());
        }
    }

    @PreDestroy
    public void tearDown() {
        if (levelDB != null) {
            try {
                levelDB.close();
            } catch (IOException e) {
                LOGGER.error("LevelDB close error", e);
            }
        }
    }
}
