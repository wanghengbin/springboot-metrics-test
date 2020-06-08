//package com.wanghb.test.config;
//
//import io.micrometer.core.instrument.MeterRegistry;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
//import org.springframework.stereotype.Component;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.util.Enumeration;
//
///**
// * @author emacsist
// */
//@Component
//public class MyMetrics implements MeterRegistryCustomizer {
//
//    @Value("${server.port}")
//    private int serverPort;
//
//    @Value("${spring.application.name}")
//    private String applicationName;
//
//    @Override
//    public void customize(final MeterRegistry registry) {
//        //唯一标识, IP:port
//        registry.config().commonTags("uuid", getIpAddress() + ":" + serverPort);
//    }
//
//
//    public static String getIpAddress() {
//        try {
//            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
//            InetAddress ip = null;
//            while (allNetInterfaces.hasMoreElements()) {
//                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
//                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
//                    continue;
//                } else {
//                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
//                    while (addresses.hasMoreElements()) {
//                        ip = addresses.nextElement();
//                        if (ip != null && ip instanceof Inet4Address) {
//                            return ip.getHostAddress();
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("IP地址获取失败" + e.toString());
//        }
//        return "";
//    }
//}