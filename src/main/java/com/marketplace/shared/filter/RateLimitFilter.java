package com.marketplace.shared.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.shared.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter implements Ordered {

	private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 160;
	private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	private final ProxyManager<String> proxyManager;
	private final RateLimitProperties properties;
	private final ObjectMapper objectMapper;
	private final Supplier<BucketConfiguration> defaultConfigSupplier;
	private final Map<String, Supplier<BucketConfiguration>> configByGroup = new ConcurrentHashMap<>();

	public RateLimitFilter(ProxyManager<String> proxyManager,
			RateLimitProperties properties,
			ObjectMapper objectMapper,
			Supplier<BucketConfiguration> defaultConfigSupplier) {
		this.proxyManager = proxyManager;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.defaultConfigSupplier = defaultConfigSupplier;
		initConfigByGroup();
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	private void initConfigByGroup() {
		configByGroup.put("auth", buildConfig(properties.getAuth()));
		configByGroup.put("orders", buildConfig(properties.getOrders()));
		configByGroup.put("payments", buildConfig(properties.getPayments()));
		configByGroup.put("reviews", buildConfig(properties.getReviews()));
		configByGroup.put("uploads", buildConfig(properties.getUploads()));
		configByGroup.put("password", buildConfig(properties.getPassword()));
	}

	private Supplier<BucketConfiguration> buildConfig(RateLimitProperties.EndpointLimit limit) {
		return () -> BucketConfiguration.builder()
				.addLimit(Bandwidth.classic(
						limit.getCapacity(),
						Refill.greedy(limit.getRefillTokens(),
								Duration.ofMinutes(limit.getRefillDurationMinutes()))))
				.build();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestGroup = resolveGroup(request);

		if (requestGroup == null) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientKey = resolveClientKey(request, requestGroup);
		Supplier<BucketConfiguration> configSupplier = configByGroup.getOrDefault(requestGroup, defaultConfigSupplier);

		Bucket bucket = proxyManager.builder().build(clientKey, configSupplier);
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		if (probe.isConsumed()) {
			response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
			filterChain.doFilter(request, response);
		} else {
			long retryAfterSeconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
			response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);

			Map<String, Object> errorBody = Map.of(
					"status", 429,
					"error", "Too Many Requests",
					"message", "Rate limit exceeded. Please try again later.",
					"path", request.getRequestURI(),
					"timestamp", java.time.Instant.now());

			response.getWriter().write(objectMapper.writeValueAsString(errorBody));
		}
	}

	private String resolveGroup(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String method = request.getMethod();

		if ("PUT".equals(method) && uri.equals("/api/v1/users/profile/password")) {
			return "password";
		}
		if ("POST".equals(method) && uri.equals("/api/v1/auth/reset-password")) {
			return "password";
		}
		if (uri.startsWith("/api/v1/auth/") && !uri.equals("/api/v1/auth/refresh")) {
			return "auth";
		}
		if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
			if (uri.startsWith("/api/v1/orders")) {
				return "orders";
			}
			if (uri.startsWith("/api/v1/payments")) {
				return "payments";
			}
			if (uri.matches("^/api/v1/products/\\d+/reviews$") || uri.startsWith("/api/v1/reviews")) {
				return "reviews";
			}
			if (uri.matches(".*/upload-url$")) {
				return "uploads";
			}
		}
		return null;
	}

	private String resolveClientKey(HttpServletRequest request, String group) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
			return "user:" + auth.getName() + ":" + group;
		}
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isBlank()) {
			ip = request.getRemoteAddr();
		} else {
			ip = ip.split(",")[0].trim();
		}
		return "ip:" + ip + ":" + group;
	}
}
