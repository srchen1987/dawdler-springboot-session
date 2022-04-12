# dawdler-springboot-session

超高性能分布式session的实现,地表最快🚀🚀🚀 完全替代 spring-redis-session
dawdler-springboot-session基于 [dawdler-client-plug-session](https://github.com/srchen1987/dawdler-series/tree/master/dawdler/dawdler-client-plug-session) 改造成 springboot版本

## 快速入门

在springboot的启动类中 加入扫描com.anywide.dawdler.clientplug.web.session包 demo如下：

  ```java
  @EnableDiscoveryClient
  @SpringBootApplication(scanBasePackages = {"com.dawdler.gateway.boostrap","com.anywide.dawdler.clientplug.web.session"})
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
expireTime=1800 #过期时间 单位为秒数 默认30分钟
maxSize=65525 #jvm堆中最大的个数
useToken=true #是否允许使用token  如果为是 支持uri后面传入token参数 或 http head头中传入token参数
```

## 配置文件 session-redis-{env}.properties

env 为springboot中指定的环境 如 uat,dev

```properties
#######################
masterName=masterName #哨兵模式下的masterName (注意：哨兵与单机只能用一种,用单机就不能配置此项)
sentinels=192.168.0.2:26379,192.168.0.3:26379,192.168.0.4:26379 #哨兵列表(注意：哨兵与单机只能用一种,用单机就不能配置此项)
#######################
addr=127.0.0.1 #单机ip
port=6379 #单机端口
######################

auth=password #密码
max_active=20 #最大连接数
max_idle=8 #最大空闲数
max_wait=10000 #最大等待时长(单位毫秒)
timeout=10000 #超时时间(单位毫秒)
test_on_borrow=false #获取连接时是否验证连接有效
database=0 #使用指定数据槽
```
以上配置文件优先读取项目中的配置文件，读取不到会读取jar包中的.
