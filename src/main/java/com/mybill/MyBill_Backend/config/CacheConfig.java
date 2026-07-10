package com.mybill.MyBill_Backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${app.cache.dashboard.ttl-minutes:5}") long dashboardTtlMinutes
    ) {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache("dashboardStats", Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(dashboardTtlMinutes))
                .build());

        manager.registerCustomCache("businessProfiles", Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(1))
                .build());

        manager.registerCustomCache("invoiceSettings", Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofHours(1))
                .build());

        manager.registerCustomCache("emailTemplates", Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofHours(24))
                .build());

        return manager;
    }
}
