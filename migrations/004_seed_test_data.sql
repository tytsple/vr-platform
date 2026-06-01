-- Seed test data: demo tenant + tenant user + operator user
-- Run AFTER 003_data_migration.sql

-- 1. 测试租户
INSERT INTO tenants (name, contact_info) VALUES
    ('演示租户', 'contact@demo.com')
ON CONFLICT DO NOTHING;

-- 2. 租户用户 (密码: tenant123)
INSERT INTO sys_user (user_name, nick_name, password, status) VALUES
    ('tenant', '租户用户', '$2a$10$kGC8yoXcnNnp/hwfVX.UuO2hRpyhp6g3ZEGUhFjWEYMT1ehnBIzle', '0')
ON CONFLICT (user_name) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
    SELECT (SELECT user_id FROM sys_user WHERE user_name='tenant'),
           (SELECT role_id FROM sys_role WHERE role_key='tenant')
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role WHERE user_id=(SELECT user_id FROM sys_user WHERE user_name='tenant')
);

INSERT INTO sys_user_tenant (user_id, tenant_id)
    SELECT (SELECT user_id FROM sys_user WHERE user_name='tenant'),
           (SELECT id FROM tenants WHERE name='演示租户')
ON CONFLICT (user_id) DO NOTHING;

-- 3. 运维用户 (密码: operator123)
INSERT INTO sys_user (user_name, nick_name, password, status) VALUES
    ('operator', '运维人员', '$2a$10$kGC8yoXcnNnp/hwfVX.UuO2hRpyhp6g3ZEGUhFjWEYMT1ehnBIzle', '0')
ON CONFLICT (user_name) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
    SELECT (SELECT user_id FROM sys_user WHERE user_name='operator'),
           (SELECT role_id FROM sys_role WHERE role_key='operator')
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role WHERE user_id=(SELECT user_id FROM sys_user WHERE user_name='operator')
);

-- 4. 给演示租户分配场地
INSERT INTO venues (tenant_id, name, address, controller_token) VALUES
    ((SELECT id FROM tenants WHERE name='演示租户'), '演示场地A', '北京市朝阳区', NULL),
    ((SELECT id FROM tenants WHERE name='演示租户'), '演示场地B', '上海市浦东新区', NULL)
ON CONFLICT DO NOTHING;
