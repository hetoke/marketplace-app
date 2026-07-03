package com.marketplace.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
			CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
			CustomAccessDeniedHandler customAccessDeniedHandler) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
		this.customAccessDeniedHandler = customAccessDeniedHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/api/v1/webhooks/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/payments/ipn").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/avatar/upload-url").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/products/{productId}/images/upload-url").hasRole("SELLER")
						.requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/sellers/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("SELLER")
						.requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("SELLER")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("SELLER")
						.requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/payments/seller/history").hasRole("SELLER")
						.requestMatchers("/api/v1/payments/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/v1/orders/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/orders/**").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").authenticated()
						.requestMatchers("/api/v1/cart/**").hasRole("BUYER")
						.requestMatchers("/api/v1/buyers/wishlist/**").hasRole("BUYER")
						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(customAuthenticationEntryPoint)
						.accessDeniedHandler(customAccessDeniedHandler)
				)
				.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}
