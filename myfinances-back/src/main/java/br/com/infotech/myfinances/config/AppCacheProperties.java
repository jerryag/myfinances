package br.com.infotech.myfinances.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    private Map<String, CacheConfigProps> configs = new HashMap<>();

    @Data
    public static class CacheConfigProps {
        private int expireAfterWriteMinutes;
        private int maximumSize;
    }
}
