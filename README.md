# dawdler-springboot-session
超高性能分布式session的实现,地表最快🚀🚀🚀 完全替代 spring-redis-session
dawdler-springboot-session基于 [dawdler-client-plug-session](https://github.com/srchen1987/dawdler-series/tree/master/dawdler/dawdler-client-plug-session) 改造成 springboot版本

#### 快速入门
在springboot的启动类中 加入扫描com.anywide.dawdler.clientplug.web.session包 demo如下：
  ```
  @EnableDiscoveryClient
  @SpringBootApplication(scanBasePackages = {"com.dawdler.gateway.boostrap","com.anywide.dawdler.clientplug.web.session"})
  public class GatewayServiceApplication {
	  public static void main(String[] args) {
         SpringApplication.run(GatewayServiceApplication.class, args);
      }
  }
  ```
  #### 配置文件 identityConfig.properties 
  ```
cookieName=_dawdler_key #cookie中存放名称
domain= #域，默认为空 为客户端请求过来的域名
path=/ #cookie的path
secure=false #是否为https
expireTime=1800 #过期时间 单位为秒数 默认30分钟
maxSize=65525 #jvm堆中最大的个数
useToken=true #是否允许使用token  如果为是 支持uri后面传入token参数 或 http head头中传入token参数
  ```
