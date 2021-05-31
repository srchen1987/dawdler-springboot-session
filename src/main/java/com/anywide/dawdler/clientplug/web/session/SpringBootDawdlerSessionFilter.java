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
package com.anywide.dawdler.clientplug.web.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.Properties;

import javax.annotation.Resource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.anywide.dawdler.clientplug.web.session.base.SessionIdGeneratorBase;
import com.anywide.dawdler.clientplug.web.session.base.StandardSessionIdGenerator;
import com.anywide.dawdler.clientplug.web.session.http.DawdlerHttpSession;
import com.anywide.dawdler.clientplug.web.session.message.MessageOperator;
import com.anywide.dawdler.clientplug.web.session.message.RedisMessageOperator;
import com.anywide.dawdler.clientplug.web.session.store.DistributedSessionRedisUtil;
import com.anywide.dawdler.clientplug.web.session.store.RedisSessionStore;
import com.anywide.dawdler.clientplug.web.session.store.SessionStore;
import com.anywide.dawdler.core.serializer.SerializeDecider;
import com.anywide.dawdler.core.serializer.Serializer;
import com.anywide.dawdler.util.DawdlerTool;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.Pool;
/**
 * 
 * @Title:  SpringBootDawdlerSessionFilter.java
 * @Description:    session过滤器
 * @author: jackson.song    
 * @date:   2020年9月16日  
 * @version V1.0 
 * @email: suxuan696@gmail.com
 */
@Component
public class SpringBootDawdlerSessionFilter implements Filter {
	private static Logger logger = LoggerFactory.getLogger(SpringBootDawdlerSessionFilter.class);
	private static String cookieName;
	private static String domain;
	private static String path = "/";
	private static boolean secure;
	private static int maxInactiveInterval = 1800;
	private static int maxSize = 65525;
	private static boolean useToken = false; //support request param token
	private SessionIdGeneratorBase sessionIdGenerator = new StandardSessionIdGenerator();
	AbstractDistributedSessionManager abstractDistributedSessionManager;
	private ServletContext servletContext;
	private Serializer serializer;
	private SessionStore sessionStore;
	private SessionOperator sessionOperator;
	private MessageOperator messageOperator;
	private Pool<Jedis> jedisPool;
	
	static {
		String filePath = DawdlerTool.getcurrentPath() + "identityConfig.properties";
		File file = new File(filePath);
		InputStream inStream = null;
		if(!file.isFile()) {
			logger.warn("use  default identityConfig in dawdler-session jar!");
			inStream = SpringBootDawdlerSessionFilter.class.getResourceAsStream("/identityConfig.properties");
		}else {
			try {
				inStream = new FileInputStream(filePath);
			} catch (FileNotFoundException e) {
			}
		} 
		
			Properties ps = new Properties();
			try {
			
				ps.load(inStream);
				String domainString = ps.getProperty("domain");
				if (domainString != null && !domainString.trim().equals("")) {
					domain = domainString;
				}
				
				String pathString = ps.getProperty("path");
				if (pathString != null && !pathString.trim().equals("")) {
					path = pathString;
				}
				cookieName = ps.getProperty("cookieName");
				String secureString = ps.getProperty("secure");
				if (secureString != null) {
					secure = Boolean.parseBoolean(secureString);
				}
				
				String maxInactiveIntervalString = ps.getProperty("maxInactiveInterval");
				if (maxInactiveIntervalString != null) {
					try {
						int temp = Integer.parseInt(maxInactiveIntervalString);
						if(temp>0)maxInactiveInterval = temp;
					} catch (Exception e) {
					}
				}
				
				String maxSizeString = ps.getProperty("maxSize");
				if (maxSizeString != null) {
					try {
						int temp = Integer.parseInt(maxSizeString);
						if(temp>0)maxSize = temp;
					} catch (Exception e) {
					}
				}
				
				String useTokenString = ps.getProperty("useToken");
				if (useTokenString != null) {
					try {
						useToken = Boolean.parseBoolean(useTokenString);
					} catch (Exception e) {
					}
				}
				
			} catch (Exception e) {
				logger.error("", e);
			} finally {
				if (inStream != null) {
					try {
						inStream.close();
					} catch (IOException e) {
						logger.error("", e);
					}
				}
			}
	}
	

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		jedisPool = DistributedSessionRedisUtil.getJedisPool();
		servletContext = filterConfig.getServletContext();
		abstractDistributedSessionManager = new DistributedCaffeineSessionManager(maxInactiveInterval, maxSize);
		serializer = SerializeDecider.decide((byte) 2);//默认为kroy 需要其他的可以自行扩展
		sessionStore = new RedisSessionStore(jedisPool, serializer);  
		messageOperator = new RedisMessageOperator(serializer, sessionStore, abstractDistributedSessionManager, jedisPool);
		sessionOperator = new SessionOperator(abstractDistributedSessionManager, sessionStore, messageOperator, serializer, servletContext);
		messageOperator.listenExpireAndDelAndChange(); 
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpReponse = (HttpServletResponse) response;
		try {
			httpRequest = new HttpServletRequestWrapper(httpRequest, httpReponse);
			chain.doFilter(httpRequest, httpReponse);
		}finally {
			DawdlerHttpSession session = (DawdlerHttpSession) httpRequest.getSession(false);
			if (session != null) {
				try {
					sessionStore.saveSession(session);
				} catch (Exception e) {
					logger.error("",e);
				}
				session.finish();
			}
		}

	}

	public String getCookieValue(Cookie[] cookies, String cookiename) {
		if (cookies == null)
			return null;
		try {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i].getName().equals(cookiename)) {
					return URLDecoder.decode(cookies[i].getValue().trim(), "utf-8");
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
		private HttpServletResponse response;
		private HttpServletRequest request;
		private DawdlerHttpSession session;

		public HttpServletRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
			super(request);
			this.response = response;
			this.request = request;
		}

		@Override
		public HttpSession getSession(boolean create) {
			String token = null;
			if(useToken) {
				token = request.getParameter("token");
			}
			if (session == null) {
				String sessionKey = token != null ? token : getCookieValue(request.getCookies(), cookieName);
				if (sessionKey != null) {  
					try {
						session = sessionOperator.operationSession(sessionKey, maxInactiveInterval);
					} catch (Exception e) {
						logger.error("",e);
					}
				}
			} else {
				session.setNew(false);
			}
			if(session != null && session.isValid()) {
				abstractDistributedSessionManager.removeSession(session.getId());
				session = null;
			}
			if (session == null && create) {
				String sessionKey = token != null ? token : sessionIdGenerator.generateSessionId();
				setCookie(cookieName, sessionKey); 
				this.session = sessionOperator.createLocalSession(sessionKey, maxInactiveInterval, true);
			}
			return session;
		}


		@Override
		public HttpSession getSession() {
			return this.getSession(true);
		}


		private void setCookie(String name, String value) {
			Cookie cookie = new Cookie(name, value);
			cookie.setMaxAge(-1);
			cookie.setPath(path);
			if (domain != null && domain.trim().length() > 0) {
				cookie.setDomain(domain);
			}
			cookie.setHttpOnly(true);
			cookie.setSecure(secure);
			response.addCookie(cookie);
		}
	}

	@Override
	public void destroy() {
	}
}
