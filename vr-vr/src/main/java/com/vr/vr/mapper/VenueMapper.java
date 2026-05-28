package com.vr.vr.mapper;

import com.vr.vr.domain.Venue;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VenueMapper {
    @Select({"<script>",
        "SELECT id, tenant_id, name, address, controller_token, created_at FROM venues",
        "<if test='tenantId != null and tenantId != 0'>WHERE tenant_id=#{tenantId}</if>",
        "ORDER BY id</script>"})
    List<Venue> selectVenueList(@Param("tenantId") Long tenantId);

    @Select("SELECT id, tenant_id, name, address, controller_token, created_at FROM venues WHERE id=#{id}")
    Venue selectVenueById(Long id);

    @Select("SELECT id, tenant_id, name, address, controller_token, created_at FROM venues WHERE controller_token=#{token}")
    Venue selectVenueByToken(String token);

    @Insert("INSERT INTO venues (tenant_id, name, address) VALUES (#{tenantId}, #{name}, #{address})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertVenue(Venue venue);

    @Update("UPDATE venues SET tenant_id=#{tenantId}, name=#{name}, address=#{address} WHERE id=#{id}")
    int updateVenue(Venue venue);

    @Delete("DELETE FROM venues WHERE id=#{id}")
    int deleteVenueById(Long id);

    @Update("UPDATE venues SET controller_token=#{token} WHERE id=#{venueId}")
    int updateControllerToken(@Param("venueId") Long venueId, @Param("token") String token);

    @Select("SELECT tenant_id FROM venues WHERE id=#{venueId}")
    Long selectTenantIdByVenueId(Long venueId);
}
