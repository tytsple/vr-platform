CREATE TABLE IF NOT EXISTS tenants (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), contact_info TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS venues (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(255), address TEXT, controller_token VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS applications (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), description TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS application_versions (id BIGINT AUTO_INCREMENT PRIMARY KEY, application_id BIGINT, version VARCHAR(100), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS licenses (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT, application_id BIGINT, granted BOOLEAN, quota_type VARCHAR(50), quota_limit BIGINT, quota_used BIGINT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS sessions (id BIGINT AUTO_INCREMENT PRIMARY KEY, venue_id BIGINT, application_id BIGINT, version VARCHAR(100), started_at TIMESTAMP, ended_at TIMESTAMP, status VARCHAR(20) DEFAULT 'active', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

CREATE TABLE IF NOT EXISTS sys_user (user_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_name VARCHAR(30) UNIQUE, nick_name VARCHAR(30), password VARCHAR(100), status CHAR(1) DEFAULT '0', del_flag CHAR(1) DEFAULT '0', create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS sys_role (role_id BIGINT AUTO_INCREMENT PRIMARY KEY, role_name VARCHAR(30), role_key VARCHAR(100) UNIQUE, role_sort INT, status CHAR(1) DEFAULT '0', create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS sys_user_role (user_id BIGINT, role_id BIGINT, PRIMARY KEY(user_id, role_id));
CREATE TABLE IF NOT EXISTS sys_user_tenant (user_id BIGINT PRIMARY KEY, tenant_id BIGINT);

INSERT INTO tenants (id, name) VALUES (1, '测试租户');
INSERT INTO sys_role (role_id, role_name, role_key) VALUES (1, '管理员', 'admin'), (2, '租户', 'tenant');
INSERT INTO sys_user (user_id, user_name, password, status) VALUES (1, 'admin', '$2a$10$kGC8yoXcnNnp/hwfVX.UuO2hRpyhp6g3ZEGUhFjWEYMT1ehnBIzle', '0');
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
