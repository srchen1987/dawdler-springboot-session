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
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.anywide.dawdler.clientplug.web.session.base.SessionIdGeneratorBase;
import com.anywide.dawdler.clientplug.web.session.base.StandardSessionIdGenerator;
import com.anywide.dawdler.clientplug.web.session.conf.JedisConfig;
import com.anywide.dawdler.clientplug.web.session.http.DawdlerHttpSession;
import com.anywide.dawdler.clientplug.web.session.message.MessageOperator;
import com.anywide.dawdler.clientplug.web.session.message.RedisMessageOperator;
import com.anywide.dawdler.clientplug.web.session.store.DistributedSessionRedisUtil;
import com.anywide.dawdler.clientplug.web.session.store.RedisSessionStore;
import com.anywide.dawdler.clientplug.web.session.store.SessionStore;
import com.anywide.dawdler.clientplug.web.session.util.DawdlerTool;
import com.anywide.dawdler.core.serializer.SerializeDecider;
import com.anywide.dawdler.core.serializer.Serializer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

/**
 * @author jackson.song
 * @version V1.0
 * @Title DawdlerSessionFilter.java
 * @Description session过滤器
 * @date 2016年6月16日
 * @email suxuan696@gmail.com
 */
@Order(0)
@WebFilter(filterName = "DawdlerSessionFilter", urlPatterns = "/*")
public class DawdlerSessionFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(DawdlerSessionFilter.class);
	public static String cookieName;
	public static String domain;
	public static String path = "/";
	public static boolean secure;
	private static int maxInactiveInterval = 1800;
	private static int maxSize = 65525;
	private JedisConfig jedisConfig;
//   private static int synFlushInterval = 0;
	private static String tokenName = "token";
	private static boolean supportHead;
	private static boolean supportParam;
	public DawdlerSessionFilter(JedisConfig jedisConfig) {
		this.jedisConfig = jedisConfig;
	}
	static {
		String filePath = DawdlerTool.getCurrentPath() + "identityConfig.properties";
		File file = new File(filePath);
		InputStream inStream = null;
		if (!file.isFile()) {
			logger.warn("use  default identityConfig in dawdler-session jar!");
			inStream = DawdlerSessionFilter.class.getResourceAsStream("/identityConfig.properties");
		} else {
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
			String supportParamString = ps.getProperty("supportParam");
			if (supportParamString != null) {
				supportParam = Boolean.parseBoolean(supportParamString);
			}
			
			String supportHeadString = ps.getProperty("supportHead");
			if (supportHeadString != null) {
				supportHead = Boolean.parseBoolean(supportHeadString);
			}
			
			String tokenNameString = ps.getProperty("tokenName");
			if (tokenNameString != null && !tokenNameString.trim().equals("")) {
				tokenName = tokenNameString;
			}
			

			String maxInactiveIntervalString = ps.getProperty("maxInactiveInterval");
			if (maxInactiveIntervalString != null) {
				try {
					int temp = Integer.parseInt(maxInactiveIntervalString);
					if (temp > 0)
						maxInactiveInterval = temp;
				} catch (Exception e) {
				}
			}

			String maxSizeString = ps.getProperty("maxSize");
			if (maxSizeString != null) {
				try {
					int temp = Integer.parseInt(maxSizeString);
					if (temp > 0)
						maxSize = temp;
				} catch (Exception e) {
				}
			}

//            String synchFlushIntervalString = ps.getProperty("synchFlushInterval");
//            if (synchFlushIntervalString != null) {
//                try {
//                    int temp = Integer.parseInt(synchFlushIntervalString);
//                    if (temp > 0)
//                        synFlushInterval = temp;
//                } catch (Exception e) {
//                }
//            }
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

	private final SessionIdGeneratorBase sessionIdGenerator = new StandardSessionIdGenerator();
	AbstractDistributedSessionManager abstractDistributedSessionManager;
	private ServletContext servletContext;
	private Serializer serializer;
	private SessionStore sessionStore;
	private SessionOperator sessionOperator;
	private Pool<Jedis> jedisPool;
	private MessageOperator messageOperator;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		if (jedisPool == null)
			jedisPool = DistributedSessionRedisUtil.getJedisPool(jedisConfig);
		servletContext = filterConfig.getServletContext();
		abstractDistributedSessionManager = new DistributedCaffeineSessionManager(maxInactiveInterval, maxSize);
		if (servletContext != null) { // support webflux reactor spring-cloud-gatewy
			Object listener = servletContext
					.getAttribute(AbstractDistributedSessionManager.DISTRIBUTED_SESSION_HTTPSESSION_LISTENER);
			if (listener instanceof HttpSessionListener) {
				abstractDistributedSessionManager.setHttpSessionListener((HttpSessionListener) listener);
			}
		}

		serializer = SerializeDecider.decide((byte) 2);// 默认为kroy 需要其他的可以自行扩展
		sessionStore = new RedisSessionStore(jedisPool, serializer);
		messageOperator = new RedisMessageOperator(serializer, sessionStore, abstractDistributedSessionManager,
				jedisPool);
		sessionOperator = new SessionOperator(abstractDistributedSessionManager, sessionIdGenerator, sessionStore,
				messageOperator, serializer, servletContext);
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
		} finally {
			DawdlerHttpSession session = (DawdlerHttpSession) httpRequest.getSession(false);
			if (session != null) {
				try {
					sessionStore.saveSession(session);
				} catch (Exception e) {
					logger.error("", e);
				}
				session.finish();
				if (session.isValid()) {
					Cookie cookie = new Cookie(cookieName, null);
					cookie.setMaxAge(0);
					cookie.setPath("/");
					httpReponse.addCookie(cookie);
				}
			}
		}

	}

	public String getCookieValue(Cookie[] cookies, String cookiename) {
		if (cookies == null)
			return null;
		try {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i].getName().equals(cookiename)) {
					return cookies[i].getValue();
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	@Override
	public void destroy() {
		if (abstractDistributedSessionManager != null) {
			abstractDistributedSessionManager.close();
		}
		if (jedisPool != null) {
			jedisPool.close();
		}

	}

	class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
		private final HttpServletResponse response;
		private final HttpServletRequest request;
		private DawdlerHttpSession session;

		public HttpServletRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
			super(request);
			this.response = response;
			this.request = request;
		}

		@Override
		public HttpSession getSession(boolean create) {
			if (session == null) {
				String sessionKey;
				String token = null;
				if (supportHead) {
					token = request.getHeader(tokenName);
				}
				if (token == null && supportParam) {
					token = request.getParameter(tokenName);
				}
				if (token != null) {
					sessionKey = token;
				} else {
					sessionKey = getCookieValue(request.getCookies(), cookieName);
				}
				if (sessionKey != null) {
					try {
						session = sessionOperator.operationSession(sessionKey, maxInactiveInterval);
					} catch (Exception e) {
						logger.error("", e);
					}
				}
				
				if (sessionKey != null) {
					try {
						session = sessionOperator.operationSession(sessionKey, maxInactiveInterval);
					} catch (Exception e) {
						logger.error("", e);
					}
				}
			} else {
				session.setNew(false);
			}
			if (session != null && session.isValid()) {
				abstractDistributedSessionManager.removeSession(session.getId());
			}
			if (session == null && create) {
				String sessionKey = sessionIdGenerator.generateSessionId();
				setCookie(cookieName, sessionKey);
				this.session = sessionOperator.createLocalSession(sessionKey, maxInactiveInterval, true);
				sessionOperator.addSession(session);
				HttpSessionListener httpSessionListener = abstractDistributedSessionManager.getHttpSessionListener();
				if (httpSessionListener != null) {
					HttpSessionEvent httpSessionEvent = new HttpSessionEvent(session);
					httpSessionListener.sessionCreated(httpSessionEvent);
				}
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
			cookie.setSecure("https".equals(request.getScheme()));
			response.addCookie(cookie);
		}
	}
}
