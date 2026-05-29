package com.vr.system.mapper;

import com.vr.system.domain.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserMapper {
    @Select("SELECT user_id, user_name, nick_name, password, status, del_flag, create_time FROM sys_user WHERE del_flag='0' ORDER BY user_id")
    List<SysUser> selectUserList();

    @Select("SELECT user_id, user_name, nick_name, password, status, del_flag, create_time FROM sys_user WHERE user_id=#{userId}")
    SysUser selectUserById(Long userId);

    @Select("SELECT user_id, user_name, nick_name, password, status, del_flag, create_time FROM sys_user WHERE user_name=#{userName}")
    SysUser selectUserByUserName(String userName);

    @Select("SELECT r.role_key FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.role_id WHERE ur.user_id = #{userId} AND r.status = '0'")
    List<String> selectRoleKeysByUserId(Long userId);

    @Select("SELECT tenant_id FROM sys_user_tenant WHERE user_id = #{userId}")
    Long selectTenantIdByUserId(Long userId);

    @Insert("INSERT INTO sys_user (user_id, user_name, nick_name, password, status, del_flag, create_time) VALUES (DEFAULT, #{userName}, #{nickName}, #{password}, #{status}, '0', NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "user_id")
    int insertUser(SysUser user);

    @Update({"<script>",
        "UPDATE sys_user SET user_name=#{userName}, nick_name=#{nickName}, status=#{status}",
        "<if test='password != null'> , password=#{password}</if>",
        "WHERE user_id=#{userId}</script>"})
    int updateUser(SysUser user);

    @Update("UPDATE sys_user SET del_flag='2' WHERE user_id=#{userId}")
    int deleteUserById(Long userId);

    @Insert("INSERT INTO sys_user_role (user_id, role_id) SELECT #{userId}, role_id FROM sys_role WHERE role_key = #{roleKey} ON CONFLICT (user_id, role_id) DO NOTHING")
    int insertUserRole(@Param("userId") Long userId, @Param("roleKey") String roleKey);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(Long userId);

    @Insert("INSERT INTO sys_user_tenant (user_id, tenant_id) VALUES (#{userId}, #{tenantId}) ON CONFLICT (user_id) DO UPDATE SET tenant_id = #{tenantId}")
    int upsertUserTenant(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM sys_user_tenant WHERE user_id = #{userId}")
    int deleteUserTenant(Long userId);
}
