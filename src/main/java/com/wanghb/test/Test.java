package com.wanghb.test;

import com.alibaba.fastjson.JSON;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;

/**
 * @author wanghb
 * @since 2020/7/16 4:41 下午
 */
public class Test {

    public static void main(String[] args) {
//        distributionSummary1();
        timer1();
    }

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
