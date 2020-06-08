//package com.wanghb.test.config;
//
//import com.google.common.base.Stopwatch;
//import com.uniweibo.constants.AppProfilesNames;
//import com.uniweibo.constants.Envs;
//import com.uniweibo.constants.WebConstants;
//import com.uniweibo.service.GlobalExecutorService;
//import com.uniweibo.service.TaskExecuteLock;
//import com.uniweibo.task.bid.UnfrozenHelper;
//import com.uniweibo.util.IP;
//import com.uniweibo.util.Loggers;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Metrics;
//import org.checkerframework.checker.units.qual.A;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
//import org.springframework.context.annotation.Profile;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.session.FindByIndexNameSessionRepository;
//import org.springframework.session.data.redis.RedisOperationsSessionRepository;
//import org.springframework.stereotype.Component;
//import javax.annotation.PostConstruct;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.util.Enumeration;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
///**
// * @author emacsist
// */
//@Component
//public class AppMetrics implements MeterRegistryCustomizer {
//    private final int SESSION_KEY_LENGTH = "spring:session:sessions:2270c710-fcae-4377-bbc3-50a1359abc20".length();
//    private String SESSION_USER_KEY = "app.session.active.user";
//    private String SESSION_ALL_KEY = "app.session.active.all";
//    @Autowired
//    private RedisOperationsSessionRepository sessionRepository;
//    @Autowired
//    private GlobalExecutorService globalExecutorService;
//    @Autowired
//    private UnfrozenHelper unfrozenHelper;
//    private AtomicInteger sessionUsers = Metrics.gauge(SESSION_USER_KEY, new AtomicInteger(0));
//    private AtomicInteger sessionAll = Metrics.gauge(SESSION_ALL_KEY, new AtomicInteger(0));
//    @TaskExecuteLock
//    @Profile(AppProfilesNames.WEB)
//    @Scheduled(cron = "0 */5 * * * *")
//    public void updateSessionUserCounter() {
//        globalExecutorService.submit(() -> sessionUsers.set(getUserSession()));
//        globalExecutorService.submit(() -> sessionAll.set(getAllSession()));
//    }
//    @Value("${server.port}")
//    private int serverPort;
//    private int getAllSession() {
//        final Stopwatch stopwatch = Stopwatch.createStarted();
//        final String pattern = "spring:session:sessions:*";
//        final int count = (int) unfrozenHelper.getKeys(pattern, sessionRepository.getSessionRedisOperations()).stream().filter(key -> key.length() == SESSION_KEY_LENGTH).count();
//        Loggers.RUNNING_LOG.info("cost {}", stopwatch);
//        return count;
//    }
//    private int getUserSession() {
//        final Stopwatch stopwatch = Stopwatch.createStarted();
//        final int size = sessionRepository.findByIndexNameAndIndexValue(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, WebConstants.CURRENT_USER).size();
//        Loggers.RUNNING_LOG.info("cost {}", stopwatch);
//        return size;
//    }
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