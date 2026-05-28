package com.vr.framework.aspectj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vr.common.annotation.Log;
import com.vr.framework.security.PermissionService;
import com.vr.framework.security.context.LoginUser;
import com.vr.system.domain.SysOperLog;
import com.vr.system.mapper.SysOperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SysOperLogMapper operLogMapper;

    @Around("@annotation(ann)")
    public Object around(ProceedingJoinPoint pjp, Log ann) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog operLog = new SysOperLog();
        operLog.setTitle(ann.title());
        operLog.setBusinessType(ann.businessType().name());
        operLog.setOperatorType(ann.operatorType().name());
        operLog.setMethod(pjp.getSignature().toShortString());

        LoginUser user = permissionService.getLoginUser();
        operLog.setOperName(user != null ? user.getUsername() : "anonymous");

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperIp(request.getRemoteAddr());
        }

        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;
            operLog.setStatus(0);
            operLog.setCostTime(duration);
            if (result != null) {
                try {
                    operLog.setJsonResult(jsonMapper.writeValueAsString(result));
                } catch (Exception e) {
                    operLog.setJsonResult("{\"msg\":\"result serialization failed\"}");
                }
            }
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - start;
            operLog.setStatus(1);
            operLog.setCostTime(duration);
            operLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            try {
                operLogMapper.insertOperLog(operLog);
            } catch (Exception e) {
                log.error("Failed to persist audit log: {}", e.getMessage());
            }
        }
    }
}
