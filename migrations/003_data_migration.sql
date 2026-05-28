-- Data migration: old users → sys_user + sys_role + sys_user_role
-- Also seeds menu tree and role-menu assignments

-- 1. Create 3 roles (matching original Go roles)
INSERT INTO sys_role (role_name, role_key, role_sort) VALUES
    ('超级管理员', 'admin', 1),
    ('租户用户', 'tenant', 2),
    ('运维人员', 'operator', 3)
ON CONFLICT DO NOTHING;

-- 2. Migrate existing users from old 'users' table to sys_user
-- (users table from 001_init.sql — columns: id, username, password_hash, role, tenant_id)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='users') THEN
        INSERT INTO sys_user (user_id, user_name, nick_name, password, status, del_flag, create_time)
            SELECT id, username, username, password_hash, '0', '0', created_at FROM users u
            WHERE NOT EXISTS (SELECT 1 FROM sys_user su WHERE su.user_id = u.id);

        -- Map roles from old 'role' column
        INSERT INTO sys_user_role (user_id, role_id)
            SELECT u.id,
                CASE u.role
                    WHEN 'admin' THEN (SELECT role_id FROM sys_role WHERE role_key='admin')
                    WHEN 'tenant_user' THEN (SELECT role_id FROM sys_role WHERE role_key='tenant')
                    WHEN 'operator' THEN (SELECT role_id FROM sys_role WHERE role_key='operator')
                END
            FROM users u
            WHERE NOT EXISTS (SELECT 1 FROM sys_user_role sur WHERE sur.user_id = u.id);

        -- Copy tenant_id associations
        INSERT INTO sys_user_tenant (user_id, tenant_id)
            SELECT id, tenant_id FROM users WHERE tenant_id IS NOT NULL
            ON CONFLICT (user_id) DO NOTHING;
    END IF;
END $$;

-- 3. Create default admin user (if users table doesn't exist yet)
INSERT INTO sys_user (user_name, nick_name, password, status)
    VALUES ('admin', '管理员', '$2a$10$kGC8yoXcnNnp/hwfVX.UuO2hRpyhp6g3ZEGUhFjWEYMT1ehnBIzle', '0')
ON CONFLICT (user_name) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
    SELECT (SELECT user_id FROM sys_user WHERE user_name='admin'),
           (SELECT role_id FROM sys_role WHERE role_key='admin')
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id=(SELECT user_id FROM sys_user WHERE user_name='admin'));

-- 4. Seed menu tree (all VR management pages)
-- Root menus (directories)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, icon, perms) VALUES
(1,  '系统管理', 0, 1, '/system', '',     'M', 'system', ''),
(2,  'VR管理',   0, 2, '/vr',     '',     'M', 'vr',     '')
ON CONFLICT DO NOTHING;

-- System management menus
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms) VALUES
(10, '用户管理', 1, 1, 'user', 'system/user/index', 'C', 'system:user:list')
ON CONFLICT DO NOTHING;

-- VR management sub-menus (for admin)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms) VALUES
(20, '租户管理', 2, 1, 'tenant',      'vr/tenant/index',      'C', 'vr:tenant:list'),
(21, '场地管理', 2, 2, 'venue',       'vr/venue/index',       'C', 'vr:venue:list'),
(22, '应用管理', 2, 3, 'application', 'vr/application/index', 'C', 'vr:application:list'),
(23, '授权管理', 2, 4, 'license',     'vr/license/index',     'C', 'vr:license:list'),
(24, '使用统计', 2, 5, 'stats',       'vr/stats/index',       'C', 'vr:stats:list')
ON CONFLICT DO NOTHING;

-- Dashboard (admin home)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms) VALUES
(30, '首页', 0, 0, '/admin', 'admin/index', 'C', '')
ON CONFLICT DO NOTHING;

-- Tenant user page
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms) VALUES
(40, '工作台', 0, 3, '/tenant', 'tenant/index', 'C', '')
ON CONFLICT DO NOTHING;

-- Operator page
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms) VALUES
(50, '运维监控', 0, 4, '/operator', 'operator/index', 'C', '')
ON CONFLICT DO NOTHING;

-- 5. Assign menus to roles
-- Admin gets ALL menus
INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT r.role_id, m.menu_id FROM sys_role r CROSS JOIN sys_menu m
    WHERE r.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- Tenant gets only workbench
INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT r.role_id, m.menu_id FROM sys_role r CROSS JOIN sys_menu m
    WHERE r.role_key = 'tenant' AND m.menu_id IN (40)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- Operator gets only monitoring
INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT r.role_id, m.menu_id FROM sys_role r CROSS JOIN sys_menu m
    WHERE r.role_key = 'operator' AND m.menu_id IN (50)
ON CONFLICT (role_id, menu_id) DO NOTHING;
