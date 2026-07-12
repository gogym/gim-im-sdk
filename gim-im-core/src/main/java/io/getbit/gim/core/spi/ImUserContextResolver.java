package io.getbit.gim.core.spi;

/**
 * SPI：用户上下文解析器
 * Controller 层通过此接口获取当前登录用户 ID
 * 使用方需实现此接口并注册为 Spring Bean
 *
 * @author gogym
 */
public interface ImUserContextResolver {

    /**
     * 获取当前请求的用户 ID
     * 默认从请求头 X-User-Id 获取，使用方可自定义实现
     *
     * @return 当前用户 ID，未登录返回 null
     */
    Long getCurrentUserId();
}
