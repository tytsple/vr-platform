package com.vr.common.exception;

public class ServiceException extends RuntimeException {
    private final int code;
    public ServiceException(String message) { this(500, message); }
    public ServiceException(int code, String message) { super(message); this.code = code; }
    public ServiceException(String message, Throwable cause) { this(500, message, cause); }
    public ServiceException(int code, String message, Throwable cause) { super(message, cause); this.code = code; }
    public int getCode() { return code; }
}
