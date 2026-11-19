package com.computer.lub.backend.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.computer.lub.backend.service..*(..))")
    public Object log(ProceedingJoinPoint jp) throws Throwable {
        String method = jp.getSignature().toShortString();
        LOG.info("-> {}", method);
        long t = System.currentTimeMillis();
        try {
            Object r = jp.proceed();
            LOG.info("<- {} ({} мс)", method, System.currentTimeMillis() - t);
            return r;
        } catch (Throwable ex) {
            LOG.warn("X {} — {}", method, ex.getMessage());
            throw ex;
        }
    }
}
