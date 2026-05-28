package com.vr.common.core.controller;

import com.vr.common.core.domain.AjaxResult;
import com.vr.common.core.page.TableDataInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BaseController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });
    }

    protected void startPage() {
        PageHelper.startPage(
            ServletUtils.getParameterToInt("pageNum", 1),
            ServletUtils.getParameterToInt("pageSize", 10),
            ServletUtils.getParameter("orderByColumn", "")
                + " " + ServletUtils.getParameter("isAsc", "asc")
        );
    }

    protected TableDataInfo getDataTable(List<?> list) {
        TableDataInfo rsp = new TableDataInfo();
        rsp.setRows(list);
        rsp.setTotal(new PageInfo<>(list).getTotal());
        return rsp;
    }

    protected AjaxResult success(Object data) { return AjaxResult.success(data); }
    protected AjaxResult success() { return AjaxResult.success(); }
    protected AjaxResult error(String msg) { return AjaxResult.error(msg); }
    protected AjaxResult error(int code, String msg) { return AjaxResult.error(code, msg); }
}
