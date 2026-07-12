# GIM IM SDK

高性能、开箱即用的 IM 即时通讯 SDK，基于 **Netty + Protobuf** 构建，支持单聊、群聊、好友管理、消息存储、WebRTC 信令等功能。

## 特性

- **开箱即用** — 引入 `gim-im-starter` 即可获得完整的 IM 服务能力，包含 HTTP REST API
- **SPI 扩展** — 通过 SPI 接口灵活对接你的 Redis、Token 验证、ID 生成、用户体系
- **高性能长连接** — 基于 Netty 4 + Protobuf 二进制协议，支持心跳检测、ACK 确认
- **读扩散消息模型** — 消息本体只存一份，用户通过收件箱关联表拉取历史消息
- **集群模式** — 通过 Redis Pub/Sub 实现跨节点消息路由，水平扩展
- **插件化架构** — 9 个 Maven 模块按需引入，好友、群组、存储、MQ 均为独立插件

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.3 |
| Netty | 4.1.117.Final |
| Protobuf | 4.32.0 |
| MyBatis-Plus | 3.5.15 |
| RocketMQ | 2.3.5 |
| Caffeine | 3.1.8 |

## 模块结构

```
gim-im-sdk
├── gim-im-protocol          # Protobuf 协议定义 & 编解码
├── gim-im-core              # 核心引擎：Netty 服务器、消息路由、ACK、心跳、SPI 扩展点
├── gim-im-broker-rocketmq   # RocketMQ 消息代理（可选）
├── gim-im-storage           # 消息存储：MySQL + 消息同步/历史 API
├── gim-im-friend            # 好友管理：申请、分组、备注、17 个 HTTP API
├── gim-im-group             # 群组管理：创建、邀请、权限、禁言、18 个 HTTP API
├── gim-im-webrtc            # WebRTC 信令处理 & TURN 凭证
├── gim-im-starter           # 一键引入所有插件
└── example                  # 完整对接使用示例
```

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.getbit</groupId>
    <artifactId>gim-im-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 初始化数据库

执行 `sql/im_tables.sql` 创建所需的 9 张表：

```sql
-- 消息相关
im_message          -- 消息本体
im_user_message     -- 用户收件箱（读扩散）

-- 好友相关
im_friend           -- 好友关系
im_friend_group     -- 好友分组
im_friend_request   -- 好友申请
im_friend_request_log -- 申请消息日志

-- 群组相关
im_group            -- 群组
im_group_member     -- 群成员
im_group_join_request -- 入群申请
```

### 3. 实现 SPI 接口

SDK 定义了 **7 个 SPI 接口**，使用方只需实现并注册为 Spring Bean：

| SPI 接口 | 说明 | 是否必须 |
|----------|------|---------|
| `ImRedisAdapter` | Redis 操作适配器（set/get/del/publish） | **必须** |
| `ImTokenVerifier` | Token 验证（连接握手时校验身份） | **必须** |
| `ImIdGenerator` | 消息 ID 生成器（雪花算法等） | **必须** |
| `ImEventListener` | 事件监听（上下线、离线消息、投递失败） | 可选（所有方法有默认空实现） |
| `ImRedisSubscriber` | Redis Pub/Sub 订阅（集群模式需要） | 集群模式必须 |
| `ImUserInfoProvider` | 用户信息提供者（昵称、头像丰富化） | 可选 |
| `ImUserContextResolver` | 用户上下文解析（获取当前登录用户） | 可选（默认从 `X-User-Id` 请求头获取） |

**示例**（以 Redis 适配器为例）：

```java
@Component
public class RedisAdapterImpl implements ImRedisAdapter {

    private final StringRedisTemplate redisTemplate;

    public RedisAdapterImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setex(String key, int seconds, String value) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void del(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void publish(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }

    @Override
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }
}
```

> 完整示例请参考 `example` 模块。

### 4. 配置 application.yml

```yaml
gim:
  netty:
    port: 3333              # IM 长连接端口
    boss-threads: 1
    worker-threads: 0       # 0 = 自动检测 CPU 核心数
  enable-heart-beat: true
  heart-beat-interval: 30   # 心跳间隔（秒）
  enable-offline: false
  enable-cluster: false

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gim_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

# 可选：RocketMQ 消息异步入库
# rocketmq:
#   name-server: localhost:9876
```

### 5. 启动应用

```java
@SpringBootApplication
@MapperScan({
    "io.getbit.gim.friend.repository",
    "io.getbit.gim.group.repository",
    "io.getbit.gim.storage.repository"
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## HTTP API 一览

SDK 自动注册以下 REST API（所有接口均为 `POST` 方法，响应统一使用 `ApiResult<T>` 包装）：

### 好友管理 `/im/friend/*`（17 个端点）

| 端点 | 说明 |
|------|------|
| `/im/friend/list` | 获取好友列表 |
| `/im/friend/checkFriendship` | 检查好友关系 |
| `/im/friend/remark` | 设置好友备注 |
| `/im/friend/move` | 移动好友到分组 |
| `/im/friend/delete` | 删除好友 |
| `/im/friend/request` | 发送好友申请 |
| `/im/friend/request/handle` | 处理好友申请（同意/拒绝） |
| `/im/friend/request/reply` | 回复好友申请（留言） |
| `/im/friend/request/pending` | 收到的待处理申请 |
| `/im/friend/request/sent` | 我发出的申请 |
| `/im/friend/request/logs` | 申请消息日志 |
| `/im/friend/request/detail` | 申请详情 |
| `/im/friend/request/between` | 两人之间的申请历史 |
| `/im/friend/group/list` | 好友分组列表 |
| `/im/friend/group/create` | 创建好友分组 |
| `/im/friend/group/update` | 更新好友分组 |
| `/im/friend/group/delete` | 删除好友分组 |

### 群组管理 `/im/group/*`（18 个端点）

| 端点 | 说明 |
|------|------|
| `/im/group/create` | 创建群组 |
| `/im/group/info` | 获取群信息 |
| `/im/group/update` | 更新群信息 |
| `/im/group/dissolve` | 解散群组 |
| `/im/group/myGroups` | 我的群组列表 |
| `/im/group/members` | 获取群成员（含用户信息） |
| `/im/group/invite` | 邀请成员 |
| `/im/group/remove` | 移除成员 |
| `/im/group/quit` | 退出群组 |
| `/im/group/transfer` | 转让群主 |
| `/im/group/setAdmin` | 设置/取消管理员 |
| `/im/group/mute` | 禁言/解禁成员 |
| `/im/group/muteAll` | 全员禁言 |
| `/im/group/nickname` | 设置群昵称 |
| `/im/group/apply` | 申请入群 |
| `/im/group/handleJoin` | 处理入群申请 |
| `/im/group/joinRequests` | 入群申请列表 |
| `/im/group/setJoinVerify` | 设置入群验证 |

### 消息管理 `/im/message/*`（2 个端点）

| 端点 | 说明 |
|------|------|
| `/im/message/sync` | 同步离线消息（增量拉取） |
| `/im/message/history` | 查询会话消息历史（游标分页） |

### 用户身份

默认通过请求头 `X-User-Id` 获取当前用户 ID。可通过实现 `ImUserContextResolver` 自定义（如从 JWT Token、Session 中解析）。

### 统一响应格式

```json
{
    "code": "0",
    "msg": "success",
    "data": { ... }
}
```

失败时：

```json
{
    "code": "-1",
    "msg": "错误描述"
}
```

## 设计模式

### 条件装配

- Controller 仅在 Web（Servlet）环境下自动注册，非 Web 项目不受影响
- RocketMQ 仅在配置了 `rocketmq.name-server` 时激活
- 集群模式仅在 `gim.enable-cluster=true` 时启用

### SPI 扩展点

所有 SPI 均通过 `@ConditionalOnMissingBean` 保护，使用方可通过注册自定义 Bean 覆盖默认实现：

- 替换 Redis 客户端（Jedis / Redisson / Lettuce）
- 替换 Token 验证方式（JWT / OAuth2 / 自定义签名）
- 替换 ID 生成策略（雪花算法 / UUID / 数据库序列）
- 自定义用户上下文解析（JWT 解析 / Session / 自定义请求头）

### 跨模块可选依赖

使用 `@Autowired(required = false)` 实现跨模块的可选联动：

- 好友删除时自动清理会话消息（需 storage 模块）
- 群成员列表自动丰富化用户昵称头像（需实现 ImUserInfoProvider）
- 群消息读扩散自动获取群成员列表（需 group 模块）

## 构建

```bash
mvn clean install -DskipTests
```

## 示例项目

`example` 模块提供了完整的对接示例，包含所有 SPI 接口的参考实现：

```
example/src/main/java/com/example/im/
├── ExampleApplication.java        # 启动类
├── config/
│   └── MybatisPlusConfig.java     # MyBatis-Plus 分页插件
├── controller/
│   └── DemoController.java        # 业务层组合使用 SDK Service
└── spi/
    ├── RedisAdapterImpl.java      # Redis 适配器
    ├── RedisSubscriberImpl.java   # Redis Pub/Sub 订阅
    ├── TokenVerifierImpl.java     # Token 验证
    ├── IdGeneratorImpl.java       # ID 生成器
    ├── UserInfoProviderImpl.java  # 用户信息提供者
    └── ImEventListenerImpl.java   # IM 事件监听
```

## 许可证

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
