package br.com.infotech.myfinances.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final AppCacheProperties cacheProperties;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheProperties.getConfigs().forEach((name, config) -> {
            cacheManager.registerCustomCache(name,
                    Caffeine.newBuilder()
                            .expireAfterWrite(config.getExpireAfterWriteMinutes(), TimeUnit.MINUTES)
                            .maximumSize(config.getMaximumSize())
                            .build());
        });

        return cacheManager;
    }
}
