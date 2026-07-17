package com.marketplace.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

	private boolean enabled = true;
	private EndpointLimit auth = new EndpointLimit(10, 10, 1);
	private EndpointLimit orders = new EndpointLimit(20, 20, 1);
	private EndpointLimit payments = new EndpointLimit(10, 10, 1);
	private EndpointLimit reviews = new EndpointLimit(10, 10, 1);
	private EndpointLimit uploads = new EndpointLimit(5, 5, 1);

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public EndpointLimit getAuth() { return auth; }
	public void setAuth(EndpointLimit auth) { this.auth = auth; }
	public EndpointLimit getOrders() { return orders; }
	public void setOrders(EndpointLimit orders) { this.orders = orders; }
	public EndpointLimit getPayments() { return payments; }
	public void setPayments(EndpointLimit payments) { this.payments = payments; }
	public EndpointLimit getReviews() { return reviews; }
	public void setReviews(EndpointLimit reviews) { this.reviews = reviews; }
	public EndpointLimit getUploads() { return uploads; }
	public void setUploads(EndpointLimit uploads) { this.uploads = uploads; }

	public static class EndpointLimit {
		private long capacity;
		private long refillTokens;
		private long refillDurationMinutes;

		public EndpointLimit() {}

		public EndpointLimit(long capacity, long refillTokens, long refillDurationMinutes) {
			this.capacity = capacity;
			this.refillTokens = refillTokens;
			this.refillDurationMinutes = refillDurationMinutes;
		}

		public long getCapacity() { return capacity; }
		public void setCapacity(long capacity) { this.capacity = capacity; }
		public long getRefillTokens() { return refillTokens; }
		public void setRefillTokens(long refillTokens) { this.refillTokens = refillTokens; }
		public long getRefillDurationMinutes() { return refillDurationMinutes; }
		public void setRefillDurationMinutes(long refillDurationMinutes) { this.refillDurationMinutes = refillDurationMinutes; }
	}
}
