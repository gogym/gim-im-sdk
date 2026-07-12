package com.example.im.spi;

import io.getbit.gim.core.spi.ImRedisAdapter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 适配器实现 — 基于 Spring Data Redis
 * <p>
 * SDK 通过此接口操作 Redis，你可以替换为 Jedis / Redisson 等任意 Redis 客户端
 */
@Component
public class RedisAdapterImpl implements ImRedisAdapter {

    private final StringRedisTemplate redisTemplate;

    public RedisAdapterImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setex(String key, int seconds, String value) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void del(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void publish(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }

    @Override
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void psetex(String key, long millis, String value) {
        redisTemplate.opsForValue().set(key, value, millis, TimeUnit.MILLISECONDS);
    }
}
