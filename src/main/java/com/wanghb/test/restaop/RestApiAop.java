package com.wanghb.test.restaop;

import com.alibaba.fastjson.JSON;
import com.wanghb.test.utils.RequestMetrics;
import com.wanghb.test.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
//@Order(-10000)
@Slf4j
public class RestApiAop {

    private static final String pointCutPattern =
            "execution(public * com.wanghb.*.controller..*.*(..))";

    @Pointcut(pointCutPattern)
    public void apiAspect() {
    }

    @Around("apiAspect()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        long time = System.currentTimeMillis();
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = ((MethodSignature) signature);
//        //策略1，如果没有声明RestMonitor不进行监控
//        if(!methodSignature.getMethod().getDeclaringClass().isAnnotationPresent(RestMonitor.class)
//                && methodSignature.getMethod().getAnnotation(RestMonitor.class) != null){
//            return joinPoint.proceed();
//        }
        //策略2，如果是拦截方法就进行监控
        if(methodSignature.getMethod().getAnnotation(RequestMapping.class) == null
                && methodSignature.getMethod().getAnnotation(GetMapping.class) == null
                && methodSignature.getMethod().getAnnotation(PostMapping.class) == null){
            return joinPoint.proceed();
        }
        Exception ex = null;
        Result result = null;

        try {
            //进行方法调用
            Object requestResult = joinPoint.proceed();
            result = Result.ok();
            return requestResult;
        } catch (Exception e) {
            ex = e;
            result = Result.res(500, "服务器异常");
            throw e;
        } finally {
            writeLog(request, time, result, ex);
        }

    }

    protected void writeLog(HttpServletRequest request, long time, Result result, Exception e) {
    	String logStr = getLogStr(request, time, result);
    	log.info(logStr);
        if(e != null) {
        	log.error(logStr, e);
        } else {
            log.debug(logStr, JSON.toJSONString(result));
        }
    }

    private String getLogStr(HttpServletRequest request, long time, Result result) {
        StringBuilder logStrBuilder = new StringBuilder(result == null ? String.valueOf(Result.COMMONFAILCODE) : String.valueOf(result.getRetCode()));
        logStrBuilder.append("|").append(System.currentTimeMillis() - time);
        logStrBuilder.append("|").append(RequestUtils.getRemoteAddr(request) == null ? "" : RequestUtils.getRemoteAddr(request));
        logStrBuilder.append("|").append(request.getMethod());
        logStrBuilder.append("|").append(request.getRequestURI());
        logStrBuilder.append("|").append(RequestUtils.getRequestLogStr(request));
        Object uri = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if(uri != null){
            RequestMetrics.getInstance().doCount(
                    "HttpRestApi",
                    uri.toString(),
                    System.currentTimeMillis() - time,
                    result != null && result.isSucc()
            );
        }
        return logStrBuilder.toString();
    }

}