package com.example.demo.aop.logging;

import com.example.demo.utils.JsonF;
import net.logstash.logback.argument.StructuredArguments;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Aspect
public class LoggingAspect {


    @Pointcut("within(@org.springframework.stereotype.Repository *)" +
            " || within(@org.springframework.stereotype.Service *)" +
            " || within(@org.springframework.web.bind.annotation.RestController *)")
    public void springBeanPointcut() {
    }

    @Pointcut("within(com.example.demo.controller..*)")
    public void applicationPackagePointcut() {
    }

    private Optional<LocationAwareLogger> getLocationAwareLogger(JoinPoint joinPoint) {
        Logger logger = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
        return (logger instanceof LocationAwareLogger)
                ? Optional.of((LocationAwareLogger) logger)
                : Optional.empty();
    }

    private Logger getFallbackLogger(JoinPoint joinPoint) {
        return LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
    }

    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        Optional<LocationAwareLogger> optLogger = getLocationAwareLogger(joinPoint);
        Logger logger = optLogger.orElseGet(() -> (LocationAwareLogger) getFallbackLogger(joinPoint));
        String method = joinPoint.getSignature().toShortString();
        try {
            if (optLogger.isPresent()) {
                logger.error("[{}()] [EXCEPTION]", method,
                        StructuredArguments.keyValue("exception", e.getClass().getSimpleName()),
                        StructuredArguments.keyValue("ex_message", e.getMessage()),
                        e);
            } else {
                logger.error("[{}()] [EXCEPTION]: {}", method, e.getMessage(), e);
            }
        } catch (Exception loggingError) {
            logger.error("LoggingAspect failed to log exception for method {}: {}", method, loggingError.getMessage());
        }
    }

    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        Optional<LocationAwareLogger> optLogger = getLocationAwareLogger(joinPoint);
        Logger logger = optLogger.orElseGet(() -> (LocationAwareLogger) getFallbackLogger(joinPoint));

        String requestJson = (args.length > 0) ? toSafeJson(args) : null;

        try {
            if (requestJson != null) {
                logger.info("[{}] [REQUEST]: {}", method, StructuredArguments.keyValue("request", requestJson));
            } else {
                logger.info("[{}] [REQUEST]", method);
            }
        } catch (Exception logEx) {
            logger.warn("Failed to log request for {}: {}", method, logEx.getMessage());
        }
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;
        String responseJson = JsonF.toJson(result);
        try {
            logger.info("[{}()] [RESPONSE]", method,
                    StructuredArguments.keyValue("response", responseJson),
                    StructuredArguments.keyValue("duration_ms", duration));
        } catch (Exception logEx) {
            logger.warn("Failed to log response for {}: {}", method, logEx.getMessage());
        }
        return result;
    }

    private String toSafeJson(Object[] args) {
        Object[] safeArgs = Arrays.stream(args)
                .map(a -> (a instanceof MultipartFile f)
                        ? Map.of("MultipartFile", Objects.requireNonNull(f.getOriginalFilename()), "size", f.getSize())
                        : a)
                .toArray();
        return JsonF.toJson(safeArgs);
    }
}
