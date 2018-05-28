package com.github.cachex.core;

import com.github.cachex.ICache;
import com.github.cachex.ShootingMXBean;
import com.github.cachex.reader.AbstractCacheReader;
import com.github.cachex.reader.MultiCacheReader;
import com.github.cachex.reader.SingleCacheReader;
import com.github.cachex.utils.CacheXUtils;
import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-28 17:04:00.
 */
public class CacheXModule extends AbstractModule {

    private static final AtomicBoolean init = new AtomicBoolean(false);

    private static Injector injector;

    private CacheXConfig config;

    private CacheXModule(CacheXConfig config) {
        this.config = config;
    }

    /**
     * 所有bean的装配工作都放到这儿
     */
    @Override
    protected void configure() {
        Preconditions.checkArgument(config != null, "config param can not be null.");
        Preconditions.checkArgument(CacheXUtils.isNotEmpty(config.getCaches()), "caches param can not be empty.");

        bind(CacheXConfig.class).toInstance(config);

        // bind caches
        MapBinder<String, ICache> mapBinder = MapBinder.newMapBinder(binder(), String.class, ICache.class);
        config.getCaches().forEach((name, cache) -> mapBinder.addBinding(name).toInstance(cache));

        // bind shootingMXBean
        Optional.ofNullable(config.getShootingMXBean())
                .ifPresent(mxBean -> bind(ShootingMXBean.class).toInstance(mxBean));

        bind(AbstractCacheReader.class).annotatedWith(Names.named("singleCacheReader")).to(SingleCacheReader.class);
        bind(AbstractCacheReader.class).annotatedWith(Names.named("multiCacheReader")).to(MultiCacheReader.class);
    }

    public synchronized static CacheXCore coreInstance(CacheXConfig config) {
        if (init.compareAndSet(false, true)) {
            injector = Guice.createInjector(new CacheXModule(config));
        }

        return injector.getInstance(CacheXCore.class);
    }
}
