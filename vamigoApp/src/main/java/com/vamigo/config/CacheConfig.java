package com.vamigo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PHOTON_SEARCH_CACHE = "photonSearchCache";
    public static final String OSRM_ROUTE_CACHE = "osrmRouteCache";

    @Bean
    public CacheManager cacheManager() {
        var manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache(PHOTON_SEARCH_CACHE, 3, TimeUnit.DAYS, 10_000),
                buildCache(OSRM_ROUTE_CACHE, 1, TimeUnit.DAYS, 5_000)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, long duration, TimeUnit unit, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(duration, unit)
                .maximumSize(maxSize)
                .build());
    }
}
