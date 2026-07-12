package io.getbit.gim.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * GimProperties.java
 *
 * GIM SDK 统一配置属性类
 * 配置前缀: "gim"
 *
 * @author gogym
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gim")
public class GimProperties {

    // ==================== 嵌套配置类 ====================

    /** Netty服务器配置 */
    private NettyProperties netty = new NettyProperties();

    /** 本地缓存配置 */
    private CacheProperties cache = new CacheProperties();

    /** 消息发送配置 */
    private MessageProperties msg = new MessageProperties();

    // ==================== 心跳配置 ====================

    /** 是否开启心跳 */
    private boolean enableHeartBeat = true;

    /** 心跳间隔（秒） */
    private Integer heartBeatInterval = 30;

    // ==================== 离线消息配置 ====================

    /** 是否开启离线消息 */
    private boolean enableOffline = false;

    // ==================== 集群配置 ====================

    /**
     * 服务器ID（集群环境标识）
     * 如果未配置，将自动生成一个随机ID
     */
    private String serverId;

    /**
     * 获取服务器ID
     * 如果未配置则自动生成随机ID
     */
    public String getServerId() {
        if (serverId == null || serverId.isEmpty()) {
            serverId = "server-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return serverId;
    }

    /** 是否开启集群模式 */
    private boolean enableCluster = false;

    // ==================== 自动重发配置 ====================

    /** 是否开启自动重发 */
    private boolean autoRewrite = false;

    /** 重发次数 */
    private Integer reWriteNum = 3;

    /** 重发间隔（毫秒） */
    private Long reWriteDelay = 1000L;
}
