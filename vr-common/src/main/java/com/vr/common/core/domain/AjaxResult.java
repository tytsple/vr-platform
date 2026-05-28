package com.vr.common.core.domain;

import java.util.HashMap;

public class AjaxResult extends HashMap<String, Object> {

    public static final String CODE_TAG = "code";
    public static final String MSG_TAG = "msg";
    public static final String DATA_TAG = "data";

    public AjaxResult() {}

    public AjaxResult(int code, String msg, Object data) {
        put(CODE_TAG, code);
        put(MSG_TAG, msg);
        put(DATA_TAG, data);
    }

    public static AjaxResult success() {
        return new AjaxResult(200, "操作成功", null);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(200, "操作成功", data);
    }

    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(200, msg, data);
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(500, msg, null);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    public int getCode() { return (int) get(CODE_TAG); }
    public String getMsg() { return (String) get(MSG_TAG); }
    public Object getData() { return get(DATA_TAG); }
}
