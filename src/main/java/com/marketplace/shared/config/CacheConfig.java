package com.marketplace.shared.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
@EnableCaching
public class CacheConfig {

    private static final String CACHE_PRODUCTS = "products";
    private static final String CACHE_PRODUCT_BY_ID = "productById";
    private static final String CACHE_CATEGORIES_BY_ID = "categoriesById";
    private static final String CACHE_CATEGORIES_ALL = "categoriesAll";

    private static final String CACHE_ANALYTICS_REVENUE = "analyticsRevenue";
    private static final String CACHE_ANALYTICS_ORDERS = "analyticsOrders";
    private static final String CACHE_ANALYTICS_USERS = "analyticsUsers";
    private static final String CACHE_ANALYTICS_PRODUCTS = "analyticsProducts";


    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer())
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(RedisSerializer.json())
                        )
                        .disableCachingNullValues();


        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .transactionAware()

                .withCacheConfiguration(
                        CACHE_PRODUCTS,
                        defaultConfig.entryTtl(Duration.ofMinutes(5))
                )

                .withCacheConfiguration(
                        CACHE_PRODUCT_BY_ID,
                        defaultConfig.entryTtl(Duration.ofMinutes(10))
                )

                .withCacheConfiguration(
                        CACHE_CATEGORIES_BY_ID,
                        defaultConfig.entryTtl(Duration.ofMinutes(15))
                )

                .withCacheConfiguration(
                        CACHE_CATEGORIES_ALL,
                        defaultConfig.entryTtl(Duration.ofMinutes(15))
                )

                .withCacheConfiguration(
                        CACHE_ANALYTICS_REVENUE,
                        defaultConfig.entryTtl(Duration.ofMinutes(5))
                )

                .withCacheConfiguration(
                        CACHE_ANALYTICS_ORDERS,
                        defaultConfig.entryTtl(Duration.ofMinutes(5))
                )

                .withCacheConfiguration(
                        CACHE_ANALYTICS_USERS,
                        defaultConfig.entryTtl(Duration.ofMinutes(5))
                )

                .withCacheConfiguration(
                        CACHE_ANALYTICS_PRODUCTS,
                        defaultConfig.entryTtl(Duration.ofMinutes(5))
                )

                .build();
    }
}
