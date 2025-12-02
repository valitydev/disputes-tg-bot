package dev.vality.disputes.tg.bot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import dev.vality.disputes.tg.bot.config.properties.DominantCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class DominantCacheConfig {

    private final DominantCacheProperties dominantCacheProperties;

    @Bean
    @Primary
    public CacheManager providersCacheManager() {
        var caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(getCacheConfig(dominantCacheProperties.getProviders()));
        caffeineCacheManager.setCacheNames(List.of("providers"));
        return caffeineCacheManager;
    }

    @Bean
    public CacheManager terminalsCacheManager() {
        var caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(getCacheConfig(dominantCacheProperties.getTerminals()));
        caffeineCacheManager.setCacheNames(List.of("terminals"));
        return caffeineCacheManager;
    }



    private Caffeine getCacheConfig(DominantCacheProperties.CacheConfig cacheConfig) {
        return Caffeine.newBuilder()
                .expireAfterAccess(cacheConfig.getTtlSec(), TimeUnit.SECONDS)
                .maximumSize(cacheConfig.getPoolSize());
    }
}
