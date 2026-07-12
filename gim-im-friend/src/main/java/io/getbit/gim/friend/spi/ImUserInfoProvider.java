package io.getbit.gim.friend.spi;

/**
 * SPI：用户信息提供者
 * Friend插件通过此接口获取用户昵称、头像等信息
 *
 * @author gogym
 */
public interface ImUserInfoProvider {

    /**
     * 根据userId获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，不存在返回null
     */
    ImUserInfo getUserById(Long userId);

    /**
     * 用户信息DTO
     */
    record ImUserInfo(Long userId, String nickname, String avatar, String account) {}
}
