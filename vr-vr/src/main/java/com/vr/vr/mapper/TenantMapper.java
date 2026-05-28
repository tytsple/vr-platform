package com.vr.vr.mapper;

import com.vr.vr.domain.Tenant;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TenantMapper {
    @Select("SELECT id, name, contact_info, created_at FROM tenants ORDER BY id")
    List<Tenant> selectTenantList();

    @Select("SELECT id, name, contact_info, created_at FROM tenants WHERE id=#{id}")
    Tenant selectTenantById(Long id);

    @Insert("INSERT INTO tenants (name, contact_info) VALUES (#{name}, #{contactInfo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTenant(Tenant tenant);

    @Update("UPDATE tenants SET name=#{name}, contact_info=#{contactInfo} WHERE id=#{id}")
    int updateTenant(Tenant tenant);

    @Delete("DELETE FROM tenants WHERE id=#{id}")
    int deleteTenantById(Long id);
}
