package io.getbit.gim.core.spi;

/**
 * ImTokenVerifier.java
 *
 * SPI接口：Token验证器
 * 使用方自行提供Token校验实现（JWT / OAuth / 自定义）
 *
 * @author gogym
 */
public interface ImTokenVerifier {

    /**
     * 验证Token并提取用户ID
     *
     * @param token 客户端携带的认证令牌
     * @return 验证通过返回用户ID字符串，失败返回 null
     */
    String verifyAndExtractUserId(String token);
}
