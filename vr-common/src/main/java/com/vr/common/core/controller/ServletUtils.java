package com.vr.common.core.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

public class ServletUtils {

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getRequest();
    }

    public static HttpServletResponse getResponse() {
        return ((ServletRequestAttributes) Objects.requireNonNull(
            RequestContextHolder.getRequestAttributes())).getResponse();
    }

    public static String getParameter(String name) {
        return getRequest().getParameter(name);
    }

    public static String getParameter(String name, String defaultValue) {
        String v = getRequest().getParameter(name);
        return v != null ? v : defaultValue;
    }

    public static int getParameterToInt(String name, int defaultValue) {
        String v = getRequest().getParameter(name);
        try { return v != null ? Integer.parseInt(v) : defaultValue; }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public static Long getParameterToLong(String name) {
        String v = getRequest().getParameter(name);
        try { return v != null ? Long.parseLong(v) : null; }
        catch (NumberFormatException e) { return null; }
    }
}
