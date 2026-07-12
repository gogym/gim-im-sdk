package io.getbit.gim.core.spi;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 默认用户上下文解析器 — 从 HTTP 请求头 X-User-Id 获取用户 ID
 * 使用方可实现自己的 ImUserContextResolver Bean 覆盖此默认行为
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
