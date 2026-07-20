package io.getbit.gim.starter;

import io.getbit.gim.core.spi.ImUserContextResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HeaderUserContextResolver.java
 *
 * 默认用户上下文解析器 — 从 HTTP 请求头 X-User-Id 获取用户 ID
 * 仅在 Spring Web 环境下可用
 *
 * @author gogym
 */
public class HeaderUserContextResolver implements ImUserContextResolver {

    private final String headerName;

    public HeaderUserContextResolver() {
        this("X-User-Id");
    }

    public HeaderUserContextResolver(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public Long getCurrentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        String value = attrs.getRequest().getHeader(headerName);
        if (value == null || value.isEmpty()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
