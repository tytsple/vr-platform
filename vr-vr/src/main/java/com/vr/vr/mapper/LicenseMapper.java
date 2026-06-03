package com.vr.vr.mapper;

import com.vr.vr.domain.License;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LicenseMapper {
    @Select({"<script>",
        "SELECT id, tenant_id, application_id, granted, quota_type, quota_limit, quota_used, start_date, end_date, created_at FROM licenses",
        "<if test='tenantId != null and tenantId != 0'>WHERE tenant_id=#{tenantId}</if>",
        "ORDER BY id</script>"})
    List<License> selectLicenseList(@Param("tenantId") Long tenantId);

    @Select("SELECT id, tenant_id, application_id, granted, quota_type, quota_limit, quota_used, start_date, end_date, created_at FROM licenses WHERE id=#{id}")
    License selectLicenseById(Long id);

    @Select("SELECT id, tenant_id, application_id, granted, quota_type, quota_limit, quota_used, start_date, end_date, created_at FROM licenses WHERE tenant_id=#{tenantId} AND application_id=#{appId}")
    License selectLicenseByTenantAndApp(@Param("tenantId") Long tenantId, @Param("appId") Long appId);

    @Insert("INSERT INTO licenses (tenant_id, application_id, granted, quota_type, quota_limit, quota_used, start_date, end_date) VALUES (#{tenantId}, #{applicationId}, #{granted}, #{quotaType}, #{quotaLimit}, #{quotaUsed}, #{startDate}, #{endDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLicense(License license);

    @Update("UPDATE licenses SET granted=#{granted}, quota_type=#{quotaType}, quota_limit=#{quotaLimit}, quota_used=#{quotaUsed}, start_date=#{startDate}, end_date=#{endDate} WHERE id=#{id}")
    int updateLicense(License license);

    @Delete("DELETE FROM licenses WHERE id=#{id}")
    int deleteLicenseById(Long id);

    @Update("UPDATE licenses SET quota_used=#{used} WHERE id=#{id}")
    int updateQuotaUsed(@Param("id") Long id, @Param("used") Long used);

    @Update("UPDATE licenses SET quota_used = quota_used + 1 WHERE id = #{id} AND quota_limit > quota_used")
    int incrementQuotaUsed(Long id);
}
