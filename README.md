# dawdler-springboot-session

超高性能分布式session的实现,地表最快🚀🚀🚀 完全替代 spring-redis-session
dawdler-springboot-session基于 [dawdler-client-plug-session](https://github.com/srchen1987/dawdler-series/tree/master/dawdler/dawdler-client-plug-session) 改造成 springboot版本

## 快速入门

在springboot的启动类中 加入扫描com.anywide.dawdler.clientplug.web.session包 demo如下：

  ```java
  @EnableDiscoveryClient
  @SpringBootApplication
  public class GatewayServiceApplication {
  public static void main(String[] args) {
         SpringApplication.run(GatewayServiceApplication.class, args);
      }
  }
  ```

## 配置文件 identityConfig.properties

```properties
cookieName=_dawdler_key #cookie中存放名称
domain= #域,默认为空 为客户端请求过来的域名
path=/ #cookie的path
secure=false #是否为https
maxInactiveInterval=1800 #过期时间 单位为秒数 默认30分钟
maxSize=65525 #jvm堆中最大的个数
useToken=true #是否允许使用token  如果为是 支持uri后面传入token参数 或 http head头中传入token参数
```

以上配置文件优先读取项目中的配置文件，读取不到会读取jar包中的.

## 配置文件 application.yml或application.xml(支持统一配置中心)

```properties
#######################
session-redis.masterName=masterName #哨兵模式下的masterName (注意：哨兵与单机只能用一种,用单机就不能配置此项)
session-redis.sentinels=192.168.0.2:26379,192.168.0.3:26379,192.168.0.4:26379 #哨兵列表(注意：哨兵与单机只能用一种,用单机就不能配置此项)
#######################
session-redis.addr=127.0.0.1 #单机ip
session-redis.port=6379 #单机端口
######################
session-redis.userName=userName #redis账号
session-redis.auth=password #密码
session-redis.max_active=20 #最大连接数
session-redis.max_idle=8 #最大空闲数
session-redis.max_wait=10000 #最大等待时长(单位毫秒)
session-redis.timeout=10000 #超时时间(单位毫秒)
session-redis.test_on_borrow=false #获取连接时是否验证连接有效
session-redis.database=0 #使用指定数据槽
```
