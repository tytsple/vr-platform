package com.vr.vr.mapper;

import com.vr.vr.domain.Session;
import com.vr.vr.domain.TenantStats;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SessionMapper {
    @Insert("INSERT INTO sessions (venue_id, application_id, version, started_at, status) VALUES (#{venueId}, #{applicationId}, #{version}, #{startedAt}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSession(Session session);

    @Select("SELECT id, venue_id, application_id, version, started_at, ended_at, status, created_at FROM sessions WHERE id=#{id}")
    Session selectSessionById(Long id);

    @Select("SELECT id, venue_id, application_id, version, started_at, ended_at, status, created_at FROM sessions WHERE venue_id=#{venueId} AND application_id=#{appId} AND status='active' LIMIT 1")
    Session findActiveSession(@Param("venueId") Long venueId, @Param("appId") Long appId);

    @Select("SELECT id, venue_id, application_id, version, started_at, ended_at, status, created_at FROM sessions WHERE venue_id=#{venueId} AND status='active'")
    List<Session> selectActiveSessionsByVenueId(Long venueId);

    @Select("SELECT id, venue_id, application_id, version, started_at, ended_at, status, created_at FROM sessions WHERE status='active'")
    List<Session> selectAllActiveSessions();

    @Update("UPDATE sessions SET status=#{status}, ended_at=#{endedAt} WHERE id=#{id}")
    int endSession(@Param("id") Long id, @Param("status") String status, @Param("endedAt") LocalDateTime endedAt);

    @Select({"<script>",
        "SELECT s.id, s.venue_id, s.application_id, s.version, s.started_at, s.ended_at, s.status, s.created_at FROM sessions s",
        "<where>",
        "<if test='venueId != null and venueId != 0'>AND s.venue_id=#{venueId}</if>",
        "<if test='appId != null and appId != 0'>AND s.application_id=#{appId}</if>",
        "<if test='status != null and status != \"\"'>AND s.status=#{status}</if>",
        "<if test='from != null'>AND s.started_at >= #{from}</if>",
        "<if test='to != null'>AND s.started_at &lt;= #{to}</if>",
        "</where>",
        "ORDER BY s.started_at DESC LIMIT #{limit}</script>"})
    List<Session> selectSessionList(@Param("venueId") Long venueId, @Param("appId") Long appId,
                                    @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                    @Param("status") String status, @Param("limit") int limit);

    @Select({"SELECT v.tenant_id AS tenant_id, s.application_id AS application_id, COUNT(*) AS count, ",
        "COALESCE(SUM(EXTRACT(EPOCH FROM (s.ended_at - s.started_at))/60), 0) AS duration_minutes ",
        "FROM sessions s JOIN venues v ON s.venue_id = v.id ",
        "WHERE s.status IN ('normal','abnormal') GROUP BY v.tenant_id, s.application_id",
        "ORDER BY v.tenant_id, s.application_id"})
    List<TenantStats> selectSessionStats();

    @Select({"SELECT v.tenant_id AS tenant_id, s.application_id AS application_id, COUNT(*) AS count, ",
        "COALESCE(SUM(EXTRACT(EPOCH FROM (s.ended_at - s.started_at))/60), 0) AS duration_minutes ",
        "FROM sessions s JOIN venues v ON s.venue_id = v.id ",
        "WHERE s.status IN ('normal','abnormal') AND v.tenant_id = #{tenantId}",
        "GROUP BY v.tenant_id, s.application_id",
        "ORDER BY v.tenant_id, s.application_id"})
    List<TenantStats> selectSessionStatsByTenantId(Long tenantId);
}
