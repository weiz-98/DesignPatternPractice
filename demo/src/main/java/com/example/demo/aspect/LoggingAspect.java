package com.example.demo.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(1)
public class LoggingAspect {

    /**
     * 使用 Spring 容器管理的 ObjectMapper（已含 JavaTimeModule）
     */
    private final ObjectMapper objectMapper;

    /**
     * 長字串截斷上限（避免日誌爆量）
     */
    private static final int MAX_JSON_LENGTH = 5_000;

    /* ---------- Pointcuts ---------- */

    /**
     * DAO：com.example.demo.rule 及子包下所有 public 方法
     */
    @Pointcut("execution(public * com.example.demo.rule..*(..))")
    private void daoLayer() {
    }

    /**
     * Service：com.example.demo.service 及子包（排除 DAO）
     */
    @Pointcut("execution(public * com.example.demo.service..*(..))"
            + " && !within(com.example.demo.rule..*)")
    private void serviceLayer() {
    }

    /**
     * Controller：所有 @RestController / @Controller 的 public 方法
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    private void controllerLayer() {
    }

    @Around("daoLayer()")
    public Object logDao(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return proceedAndLog(pjp, "DAO");
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            log.info("[DAO COST] {}.{}() cost={} ms",
                    simpleClass(pjp), methodName(pjp), costMs);
        }
    }


    @Around("serviceLayer()")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return proceedAndLog(pjp, "SRV");
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            log.info("[SRV COST] {}.{}() cost={} ms",
                    simpleClass(pjp), methodName(pjp), costMs);
        }
    }

    @Around("controllerLayer()")
    public Object logController(ProceedingJoinPoint pjp) throws Throwable {
        return proceedAndLog(pjp, "CTL");
    }


    private Object proceedAndLog(ProceedingJoinPoint pjp, String tier) throws Throwable {
        String cls = simpleClass(pjp);
        String mtd = methodName(pjp);

        String argsJson = safeJson(filterArgs(pjp.getArgs()));
        log.info("[{} IN ] {}.{} args={}", tier, cls, mtd, argsJson);

        try {
            Object result = pjp.proceed();
            log.info("[{} OUT] {}.{} result={}", tier, cls, mtd, safeJson(result));
            return result;
        } catch (Throwable ex) {
            log.error("[{} ERR] {}.{} threw {} args={} msg={}",
                    tier, cls, mtd, ex.getClass().getSimpleName(), argsJson, ex.getMessage(), ex);
            throw ex;
        }
    }

    /* ---------- Utils ---------- */

    private String safeJson(Object obj) {
        String json;
        try {
            json = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            json = String.valueOf(obj);
        }
        if (json.length() > MAX_JSON_LENGTH) {
            return json.substring(0, MAX_JSON_LENGTH) + "... (truncated)";
        }
        return json;
    }

    /**
     * 排除難序列化或無意義的型別（可自行擴充）
     */
    private Object[] filterArgs(Object[] args) {
        return Arrays.stream(args)
                .filter(a -> !(a instanceof HttpServletRequest
                        || a instanceof HttpServletResponse
                        || a instanceof MultipartFile))
                .toArray();
    }

    private String simpleClass(ProceedingJoinPoint pjp) {
        return pjp.getTarget().getClass().getSimpleName();
    }

    private String methodName(ProceedingJoinPoint pjp) {
        return ((MethodSignature) pjp.getSignature()).getMethod().getName();
    }
}
