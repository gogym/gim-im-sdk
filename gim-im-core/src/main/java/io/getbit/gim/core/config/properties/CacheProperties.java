package io.getbit.gim.core.config.properties;

import lombok.Getter;
import lombok.Setter;

/**
 * CacheProperties.java
 *
 * 本地缓存配置
 *
 * @author gogym
 */
@Getter
@Setter
public class CacheProperties {

    /** 本地缓存最大容量 */
    private int maxSize = 100_000;

    /** 本地缓存过期时间（秒） */
    private int expireSeconds = 300;
}
