package com.wanghb.test;

import com.wanghb.test.utils.NetUtils;
import com.wanghb.test.utils.RequestMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

@SpringBootApplication
public class MonitorApplication {

    @Value("${server.port}")
    private int serverPort;

    @Value("${spring.application.name}")
    private String applicationName;

    @Resource
    protected Environment env;

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }


    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer(MeterRegistry meterRegistry) {
        return myMeterRegistry -> {
            String hostAddress = NetUtils.getLocalAddress().getHostAddress();
            String port = env.getProperty("server.port");
            String appName = env.getProperty("spring.application.name");
            meterRegistry.config().commonTags("appName", appName, "host", hostAddress, "port", port);
        };
    }

    @Bean
    public RequestMetrics requestMetrics(Environment env){
        return RequestMetrics.buildByEnv(env);
    }
}
