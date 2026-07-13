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
            @Value("${app.cache.dashboard.ttl-minutes:5}") long dashboardTtlMinutes,
            @Value("${app.cache.dashboard.max-size:100}") long dashboardMaxSize,
            @Value("${app.cache.business-profiles.max-size:100}") long businessProfilesMaxSize,
            @Value("${app.cache.invoice-settings.max-size:100}") long invoiceSettingsMaxSize
    ) {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache("dashboardStats", Caffeine.newBuilder()
                .maximumSize(dashboardMaxSize)
                .expireAfterWrite(Duration.ofMinutes(dashboardTtlMinutes))
                .build());

        manager.registerCustomCache("businessProfiles", Caffeine.newBuilder()
                .maximumSize(businessProfilesMaxSize)
                .expireAfterWrite(Duration.ofHours(1))
                .build());

        manager.registerCustomCache("invoiceSettings", Caffeine.newBuilder()
                .maximumSize(invoiceSettingsMaxSize)
                .expireAfterWrite(Duration.ofHours(1))
                .build());

        return manager;
    }
}
