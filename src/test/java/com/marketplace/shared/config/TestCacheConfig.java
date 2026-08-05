package com.marketplace.shared.config;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestCacheConfig {

	@Bean
	public CacheManager cacheManager() {
		return new NoOpCacheManager();
	}

	static class NoOpCacheManager implements CacheManager {

		@Override
		public Cache getCache(String name) {
			return new NoOpCache(name);
		}

		@Override
		public java.util.Collection<String> getCacheNames() {
			return java.util.Collections.emptyList();
		}
	}

	static class NoOpCache implements Cache {

		private final String name;
		private final ConcurrentHashMap<Object, Object> store = new ConcurrentHashMap<>();

		NoOpCache(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Object getNativeCache() {
			return store;
		}

		@Override
		public ValueWrapper get(Object key) {
			Object value = store.get(key);
			return value != null ? () -> value : null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Object key, Class<T> type) {
			Object value = store.get(key);
			return type.isInstance(value) ? (T) value : null;
		}

		@Override
		public void put(Object key, Object value) {
			store.put(key, value);
		}

		@Override
		public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
			@SuppressWarnings("unchecked")
			T value = (T) store.get(key);
			if (value != null) return value;
			try {
				value = valueLoader.call();
				store.put(key, value);
				return value;
			} catch (Exception e) {
				throw new ValueRetrievalException(key, valueLoader, e);
			}
		}

		@Override
		public void evict(Object key) {
			store.remove(key);
		}

		@Override
		public void clear() {
			store.clear();
		}
	}
}
