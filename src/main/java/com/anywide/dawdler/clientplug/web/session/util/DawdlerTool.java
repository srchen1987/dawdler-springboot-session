package com.anywide.dawdler.clientplug.web.session.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author jackson.song
 * @Title DawdlerTool.java
 * @Description DawdlerTool
 * @date 2022年11月9日
 */
public class DawdlerTool {

	public static String getCurrentPath() {
		try {
			try {
				return URLDecoder.decode(Thread.currentThread().getContextClassLoader().getResource("").getPath(),
						"utf-8");
			} catch (UnsupportedEncodingException e) {
			}
			return Thread.currentThread().getContextClassLoader().getResource("").getPath().replace("%20", " ");
		} catch (Exception e) {
			return null;
		}

	}

}
