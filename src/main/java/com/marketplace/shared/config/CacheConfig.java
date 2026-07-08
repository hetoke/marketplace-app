package com.marketplace.shared.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketplace.admin.dto.OrderAnalyticsResponse;
import com.marketplace.admin.dto.ProductAnalyticsResponse;
import com.marketplace.admin.dto.RevenueAnalyticsResponse;
import com.marketplace.admin.dto.UserAnalyticsResponse;
import com.marketplace.product.dto.CategoryResponse;
import com.marketplace.product.dto.ProductResponse;
import com.marketplace.shared.dto.PageResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
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
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .transactionAware()
                .withCacheConfiguration(CACHE_PRODUCTS, cacheConfig(defaultConfig,
                        Duration.ofMinutes(5), objectMapper,
                        typeFactory.constructParametricType(PageResponse.class, ProductResponse.class)))
                .withCacheConfiguration(CACHE_PRODUCT_BY_ID, cacheConfig(defaultConfig,
                        Duration.ofMinutes(10), objectMapper,
                        typeFactory.constructType(ProductResponse.class)))
                .withCacheConfiguration(CACHE_CATEGORIES_BY_ID, cacheConfig(defaultConfig,
                        Duration.ofMinutes(15), objectMapper,
                        typeFactory.constructType(CategoryResponse.class)))
                .withCacheConfiguration(CACHE_CATEGORIES_ALL, cacheConfig(defaultConfig,
                        Duration.ofMinutes(15), objectMapper,
                        typeFactory.constructParametricType(List.class, CategoryResponse.class)))
                .withCacheConfiguration(CACHE_ANALYTICS_REVENUE, cacheConfig(defaultConfig,
                        Duration.ofMinutes(5), objectMapper,
                        typeFactory.constructType(RevenueAnalyticsResponse.class)))
                .withCacheConfiguration(CACHE_ANALYTICS_ORDERS, cacheConfig(defaultConfig,
                        Duration.ofMinutes(5), objectMapper,
                        typeFactory.constructType(OrderAnalyticsResponse.class)))
                .withCacheConfiguration(CACHE_ANALYTICS_USERS, cacheConfig(defaultConfig,
                        Duration.ofMinutes(5), objectMapper,
                        typeFactory.constructType(UserAnalyticsResponse.class)))
                .withCacheConfiguration(CACHE_ANALYTICS_PRODUCTS, cacheConfig(defaultConfig,
                        Duration.ofMinutes(5), objectMapper,
                        typeFactory.constructType(ProductAnalyticsResponse.class)))
                .build();
    }

    private RedisCacheConfiguration cacheConfig(RedisCacheConfiguration defaultConfig,
                                                 Duration ttl,
                                                 ObjectMapper objectMapper,
                                                 JavaType valueType) {
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, valueType);
        return defaultConfig
                .entryTtl(ttl)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
