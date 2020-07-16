package com.wanghb.test;

import com.alibaba.fastjson.JSON;
import com.wanghb.test.utils.NetUtils;
import com.wanghb.test.utils.RequestMetrics;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.influx.InfluxMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToDoubleFunction;

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
            /*BigDecimal bigDecimal = new BigDecimal(0);
            meterRegistry.gauge("gauge", bigDecimal);
            DistributionSummary summary = meterRegistry.summary("summary");
            summary.record(323);
            Timer timer = meterRegistry.timer("timer");
            timer.record(() -> 1);
            timer.record(() -> System.out.println(1));
            timer.record(Duration.ZERO);
            Counter counter = meterRegistry.counter("counter");
            counter.increment();
            AtomicInteger n = new AtomicInteger(0);
            //这里ToDoubleFunction匿名实现其实可以使用Lambda表达式简化为AtomicInteger::get
            FunctionCounter.builder("functionCounter", n, new ToDoubleFunction<AtomicInteger>() {
                @Override
                public double applyAsDouble(AtomicInteger value) {
                    return value.get();
                }
            }).baseUnit("function")
                    .description("functionCounter")
                    .tag("createOrder", "CHANNEL-A")
                    .register(meterRegistry);
            FunctionTimer.builder("functionTimer", n, null, null, null)
//                    .baseUnit("function")
                    .description("functionCounter")
                    .tag("createOrder", "CHANNEL-A")
                    .register(meterRegistry);*/
        };
    }

    @Bean
    public RequestMetrics requestMetrics(Environment env){
        return RequestMetrics.buildByEnv(env);
    }

    /**----------------------- test ------------------------------------*/

    public static void distributionSummary1(){

        DistributionSummary summary = DistributionSummary.builder("summary")
                .tag("summary", "summarySample")
                .description("summary sample test")
                .register(new SimpleMeterRegistry());
        summary.record(2D);
        summary.record(3D);
        summary.record(4D);
        System.out.println(summary.count());
        System.out.println(JSON.toJSONString(summary.measure()));
        System.out.println(summary.max());
        System.out.println(summary.mean());
        System.out.println(summary.totalAmount());
    }
    public static void timer1(){
        Timer timer = Timer.builder("timer")
                .tag("timer", "timersample")
                .description("timer sample test.")
                .register(new SimpleMeterRegistry());
        for(int i=0; i<2; i++) {
            timer.record(() -> {
                createOrder();
            });
        }
        System.out.println(timer.count());
        System.out.println(timer.measure());
        System.out.println(timer.totalTime(TimeUnit.SECONDS));
        System.out.println(timer.mean(TimeUnit.SECONDS));
        System.out.println(timer.max(TimeUnit.SECONDS));
    }
    private static void createOrder() {
        try {
            TimeUnit.SECONDS.sleep(5); //模拟方法耗时
        } catch (InterruptedException e) {
            //no-operation
        }
    }
}
