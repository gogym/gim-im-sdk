package com.example.im.spi;

import io.getbit.gim.core.spi.ImIdGenerator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID 生成器实现 — 基于雪花算法（简化版）
 * <p>
 * SDK 用此接口生成消息ID和群组ID。
 * 你可以替换为雪花算法、UUID、数据库序列等。
 */
@Component
public class IdGeneratorImpl implements ImIdGenerator {

    // ===== 生产环境请使用真正的雪花算法实现（如 Huutool IdUtil、美团 Leaf 等）=====

    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis());

    @Override
    public String generateMsgId() {
        // 简化实现：时间戳 + 自增序列
        // 生产环境请替换为:
        //   return IdUtil.getSnowflakeNextIdStr();
        return String.valueOf(sequence.incrementAndGet());
    }
}
