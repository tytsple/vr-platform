package com.vr.system.mapper;

import com.vr.system.domain.SysOperLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface SysOperLogMapper {
    @Insert("INSERT INTO sys_oper_log (title, business_type, method, request_method, operator_type, oper_name, oper_url, oper_ip, oper_param, json_result, status, error_msg, cost_time, oper_time) VALUES (#{title}, #{businessType}, #{method}, #{requestMethod}, #{operatorType}, #{operName}, #{operUrl}, #{operIp}, #{operParam}, #{jsonResult}, #{status}, #{errorMsg}, #{costTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "operId")
    int insertOperLog(SysOperLog log);
}
