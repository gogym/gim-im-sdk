package io.getbit.gim.core.spi;

/**
 * ImRedisAdapter.java
 *
 * SPI接口：Redis操作适配器
 * 使用方可自行实现此接口对接不同的Redis客户端（Jedis / Lettuce / Redisson 等）
 *
 * @author gogym
 */
public interface ImRedisAdapter {

    /**
     * SET with expiration
     *
     * @param key     Redis key
     * @param seconds TTL in seconds
     * @param value   value
     */
    void setex(String key, int seconds, String value);

    /**
     * GET
     *
     * @param key Redis key
     * @return value, or null if not exists
     */
    String get(String key);

    /**
     * DELETE
     *
     * @param key Redis key
     */
    void del(String key);

    /**
     * PUBLISH message to a Redis channel
     *
     * @param channel Redis Pub/Sub channel
     * @param message message body
     */
    void publish(String channel, String message);

    /**
     * SET a key without expiration
     *
     * @param key   Redis key
     * @param value value
     */
    void set(String key, String value);

    /**
     * SET with millisecond expiration
     *
     * @param key    Redis key
     * @param millis TTL in milliseconds
     * @param value  value
     */
    default void psetex(String key, long millis, String value) {
        // default: convert to seconds
        setex(key, (int) (millis / 1000), value);
    }
}
