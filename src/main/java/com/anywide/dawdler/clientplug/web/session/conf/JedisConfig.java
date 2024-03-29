/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anywide.dawdler.clientplug.web.session.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * @author jackson.song
 * @version V1.0
 * @Title JedisConfig.java
 * @Description jedis配置类
 * @date 2023年6月23日
 * @email suxuan696@gmail.com
 */
@Configuration
@ConfigurationProperties(prefix = "session-redis")
public class JedisConfig {
			private String auth;
			private String userName;
			private Integer maxActive =JedisPoolConfig.DEFAULT_MAX_TOTAL;
			private Integer maxIdle = JedisPoolConfig.DEFAULT_MAX_IDLE;
			private Long maxWait = JedisPoolConfig.DEFAULT_MAX_WAIT.toMillis();
			private Integer timeout =  Protocol.DEFAULT_TIMEOUT;
			private Boolean testOnBorrow = false;
			private Integer database = 9;
			private String masterName;
			private String sentinels;
			private String addr;
			private Integer port;
			
			public String getAuth() {
				return auth;
			}
			public void setAuth(String auth) {
				this.auth = auth;
			}
			public String getUserName() {
				return userName;
			}
			public void setUserName(String userName) {
				this.userName = userName;
			}
			public Integer getMaxActive() {
				return maxActive;
			}
			public void setMaxActive(Integer maxActive) {
				this.maxActive = maxActive;
			}
			public Integer getMaxIdle() {
				return maxIdle;
			}
			public void setMaxIdle(Integer maxIdle) {
				this.maxIdle = maxIdle;
			}
			public Long getMaxWait() {
				return maxWait;
			}
			public void setMaxWait(Long maxWait) {
				this.maxWait = maxWait;
			}
			public Integer getTimeout() {
				return timeout;
			}
			public void setTimeout(Integer timeout) {
				this.timeout = timeout;
			}
			public Boolean getTestOnBorrow() {
				return testOnBorrow;
			}
			public void setTestOnBorrow(Boolean testOnBorrow) {
				this.testOnBorrow = testOnBorrow;
			}
			public Integer getDatabase() {
				return database;
			}
			public void setDatabase(Integer database) {
				this.database = database;
			}
			public String getMasterName() {
				return masterName;
			}
			public void setMasterName(String masterName) {
				this.masterName = masterName;
			}
			public String getSentinels() {
				return sentinels;
			}
			public void setSentinels(String sentinels) {
				this.sentinels = sentinels;
			}
			public String getAddr() {
				return addr;
			}
			public void setAddr(String addr) {
				this.addr = addr;
			}
			public Integer getPort() {
				return port;
			}
			public void setPort(Integer port) {
				this.port = port;
			}
			

}
