package com.marketplace.shared.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

	@Bean
	public ProxyManager<String> proxyManager(RedisConnectionFactory connectionFactory) {
		if (!(connectionFactory instanceof LettuceConnectionFactory lettuce)) {
			throw new IllegalStateException("Bucket4j requires LettuceConnectionFactory");
		}

		AbstractRedisClient nativeClient = lettuce.getNativeClient();
		RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

		return switch (nativeClient) {
			case io.lettuce.core.RedisClient client -> LettuceBasedProxyManager
					.builderFor(client.connect(codec))
					.withExpirationStrategy(
							ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
									Duration.ofMinutes(10)))
					.build();
			case RedisClusterClient client -> LettuceBasedProxyManager
					.builderFor(client.connect(codec))
					.withExpirationStrategy(
							ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
									Duration.ofMinutes(10)))
					.build();
			default -> throw new IllegalStateException(
					"Unsupported Redis client: " + nativeClient.getClass());
		};
	}

	@Bean
	public Supplier<BucketConfiguration> defaultBucketConfiguration(RateLimitProperties properties) {
		return () -> BucketConfiguration.builder()
				.addLimit(Bandwidth.classic(
						properties.getAuth().getCapacity(),
						io.github.bucket4j.Refill.greedy(
								properties.getAuth().getRefillTokens(),
								Duration.ofMinutes(properties.getAuth().getRefillDurationMinutes()))))
				.build();
	}
}
