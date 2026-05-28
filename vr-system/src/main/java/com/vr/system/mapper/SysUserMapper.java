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

    @Insert("INSERT INTO sys_user (user_id, user_name, nick_name, password, status, del_flag, create_time) VALUES (DEFAULT, #{userName}, #{nickName}, #{password}, '0', '0', NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "user_id")
    int insertUser(SysUser user);

    @Update("UPDATE sys_user SET user_name=#{userName}, nick_name=#{nickName}, password=#{password}, status=#{status} WHERE user_id=#{userId}")
    int updateUser(SysUser user);

    @Update("UPDATE sys_user SET del_flag='2' WHERE user_id=#{userId}")
    int deleteUserById(Long userId);
}
