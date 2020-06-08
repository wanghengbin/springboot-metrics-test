package com.wanghb.test.utils;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

public class RequestUtils {

	public static String getRemoteAddr(HttpServletRequest request) {
		
		if(request == null) {
			return null;
		}
		
		String[] ipHeaders = new String[] {"REMOTEIP", "X_REAL_IP", "JY_CLIENT_IP"};
		
		String ip = null;
		for(int i = 0, len = ipHeaders.length; i < len; i++) {
			ip = request.getHeader(ipHeaders[i]);
			if (!IpUtils.isIpv4(ip)) {
				ip = null;
				continue;
			}
			break;
		}

		if (ip == null) {
			ip = request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
		}
		
		if (!IpUtils.isIpv4(ip) || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("127.0.0.1")) {
			ip = getIpFromXForwardedFor(request, -1);
		}
		
		if (!IpUtils.isIpv4(ip)) {
			ip = "127.0.0.1";
		}
		
		return ip;
		
	}


	public static String getRequestLogStr(HttpServletRequest request) {

		Map<String, String[]> params = request.getParameterMap();
		if(params == null) {
			return "";
		}

		StringBuilder logStr = new StringBuilder("");

		for(Map.Entry<String, String[]> e : params.entrySet()) {

			String value = null;
			if(e.getValue() == null) {
				value = "";
			} else if(e.getValue().length == 1) {
				value = e.getValue()[0];
			} else {
				value = Arrays.toString(e.getValue());
			}

			logStr.append(e.getKey() == null ? "" : e.getKey()).append("=").append(value).append("&");
		}

		if(logStr.length() > 0) {
			logStr.setLength(logStr.length() - 1);
		}

		return logStr.toString();
	}
	
	private static String getIpFromXForwardedFor(HttpServletRequest request, int index) {
		
		String[] ips = getIpsFromXForwardedFor(request);
		if(ips == null || ips.length <= 0) {
			return null;
		}
		
		if(index < 0) {
			index = ips.length + index;
		}
		
		if(index < 0 || index > ips.length) {
			return null;
		}
		
		String ip = ips[index];
		if(!IpUtils.isIpv4(ip)) {
			return null;
		}
		
		return ip;
	}
	
	private static String[] getIpsFromXForwardedFor(HttpServletRequest request) {
		
		String ipsStr = getHeaderValue(request, "X-Forwarded-For", true);
		if(StringUtils.isBlank(ipsStr)) {
			return null;
		}
		
		return ipsStr.split("\\,");
	}
	
	public static String getHeaderValue(HttpServletRequest request, String name, boolean ignoreNameCase) {
		
		if(ignoreNameCase == false) {
			return request.getHeader(name);
		}
		
		Enumeration<String> headerNames = request.getHeaderNames();
		if(headerNames == null) {
			return null;
		}

		String value = null;
		name = name.toLowerCase();
		
		while(headerNames.hasMoreElements()) {
			
			String headerName = headerNames.nextElement();
			if(headerName.toLowerCase().equals(name)) {
				value = request.getHeader(headerName);
				break;
			}
		}
		return value;
	}
	
}
