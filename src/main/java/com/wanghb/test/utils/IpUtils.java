package com.wanghb.test.utils;

import org.apache.commons.lang3.StringUtils;

public class IpUtils {

	public static boolean isIpv4(String ipStr) {
		return toIpv4Number(ipStr) != null;
	}
	
	public static Long toIpv4Number(String ipStr) {
		
		if(StringUtils.isBlank(ipStr)) {
			return null;
		}
		
		String[] ipSegments = ipStr.split("\\.");
		if(ipSegments == null || ipSegments.length != 4) {
			return null;
		}
		
		long ipv4Num = 0L;
		
		for(int i = 0, len = ipSegments.length; i < len; i++) {

			int ipSegment = ipSegments[i] == null ? -1 : Integer.parseInt(ipSegments[i]);
			if(ipSegment < 0 || ipSegment > 255) {
				return null;
			}
			
			ipv4Num = ipv4Num * 1000 + ipSegment;
		}
		
		return ipv4Num;
	}
}
