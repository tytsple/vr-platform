package com.vr.system.service;

import com.vr.system.domain.SysUser;
import com.vr.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<SysUser> selectUserList() { return userMapper.selectUserList(); }
    public SysUser selectUserById(Long id) { return userMapper.selectUserById(id); }
    public SysUser selectUserByUserName(String userName) { return userMapper.selectUserByUserName(userName); }
    public List<String> selectRoleKeysByUserId(Long userId) { return userMapper.selectRoleKeysByUserId(userId); }
    public Long selectTenantIdByUserId(Long userId) { return userMapper.selectTenantIdByUserId(userId); }

    public int insertUser(SysUser user) {
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userMapper.insertUser(user);
    }

    public int updateUser(SysUser user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userMapper.updateUser(user);
    }

    public int deleteUserById(Long id) { return userMapper.deleteUserById(id); }
}
