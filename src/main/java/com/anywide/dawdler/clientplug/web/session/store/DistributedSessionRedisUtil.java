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
package com.anywide.dawdler.clientplug.web.session.store;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.anywide.dawdler.clientplug.web.session.conf.JedisConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.Pool;

/**
 * @author jackson.song
 * @version V1.0
 * @Title DistributedSessionRedisUtil.java
 * @Description redis操作类
 * @date 2016年6月16日
 * @email suxuan696@gmail.com
 */
public final class DistributedSessionRedisUtil {
	/**
	 * 初始化Redis连接池
	 */
	private static volatile Pool<Jedis> jedisPool = null;




	public static Pool<Jedis> getJedisPool(JedisConfig jedisConfig) {
		if (jedisPool == null) {
			synchronized (DistributedSessionRedisUtil.class) {
				if (jedisPool == null) {
					init(jedisConfig);
				}
			}
		}
		return jedisPool;
	}

	private static void init(JedisConfig jedisConfig) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(jedisConfig.getMaxActive());
		poolConfig.setMaxIdle(jedisConfig.getMaxIdle());
		poolConfig.setMaxWaitMillis(jedisConfig.getMaxWait());
		poolConfig.setTestOnBorrow(jedisConfig.getTestOnBorrow());
		String masterName = jedisConfig.getMasterName();
		String sentinels = jedisConfig.getSentinels();
		String userName = jedisConfig.getUserName();
		if (masterName != null && sentinels != null) {
			String[] sentinelsArray = sentinels.split(",");
			Set<String> sentinelsSet = Arrays.stream(sentinelsArray).collect(Collectors.toSet());
			jedisPool = new JedisSentinelPool(masterName, sentinelsSet, poolConfig, jedisConfig.getTimeout(), userName, jedisConfig.getDatabase());
		} else {
			jedisPool = new JedisPool(poolConfig, jedisConfig.getAddr(), jedisConfig.getPort(),
					jedisConfig.getTimeout(), userName, jedisConfig.getDatabase());
		}
	}

}