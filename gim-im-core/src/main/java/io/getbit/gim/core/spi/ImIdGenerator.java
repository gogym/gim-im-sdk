package io.getbit.gim.core.spi;

/**
 * ImIdGenerator.java
 *
 * SPI接口：消息ID生成器
 * 使用方可对接雪花算法、UUID、数据库序列等
 *
 * @author gogym
 */
public interface ImIdGenerator {

    /**
     * 生成全局唯一消息ID
     *
     * @return 消息ID字符串
     */
    String generateMsgId();
}
