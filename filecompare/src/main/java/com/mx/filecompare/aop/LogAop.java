package com.mx.filecompare.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Component
@Slf4j
@Aspect
public class LogAop {

    @Pointcut("execution(public * com.mx.filecompare.controller..*.*(..))")
    public void log(){

    }

    @Before("log()")
    public void before(JoinPoint joinPoint){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        log.info("请求URL: {}", request.getRequestURL());
        log.info("请求IP: {}", request.getRemoteAddr());
        log.info("方法参数: {}", Arrays.toString(joinPoint.getArgs()));
        log.info("请求类名: {}", joinPoint.getSignature().getDeclaringTypeName());
        log.info("请求方法名: {}", joinPoint.getSignature().getName());
        long time = System.currentTimeMillis();
        request.setAttribute("REQUEST_TIME", time);
    }

    @AfterReturning("log()")
    public void after(JoinPoint joinPoint){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        long endTime = System.currentTimeMillis();
        long startTime = (long) request.getAttribute("REQUEST_TIME");
        log.info("执行时间: {}毫秒", endTime-startTime);
    }

    @Around("log()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = joinPoint.proceed();
        log.info("执行结果: {}", obj.toString());
        return obj;
    }
}
