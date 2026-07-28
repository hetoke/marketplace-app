package com.marketplace.shared.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class JsonCacheSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;

    public JsonCacheSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(value.getClass());
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("t", javaType.toCanonical());
            wrapper.put("v", objectMapper.readValue(
                    objectMapper.writeValueAsBytes(value), Object.class));
            return objectMapper.writeValueAsBytes(wrapper);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            Map<String, Object> wrapper = objectMapper.readValue(bytes, Map.class);
            String canonicalType = (String) wrapper.get("t");
            Object data = wrapper.get("v");
            JavaType javaType = objectMapper.getTypeFactory()
                    .constructFromCanonical(canonicalType);
            byte[] json = objectMapper.writeValueAsBytes(data);
            return objectMapper.readValue(json, javaType);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize", e);
        }
    }
}
