-- RuoYi RBAC tables for VR Management Platform
-- Must run AFTER 001_init.sql (original VR domain tables)

CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(30) NOT NULL UNIQUE,
    nick_name VARCHAR(30) DEFAULT '',
    password VARCHAR(100) NOT NULL,
    status CHAR(1) DEFAULT '0',   -- 0=normal, 1=disabled
    del_flag CHAR(1) DEFAULT '0',  -- 0=active, 2=deleted
    create_time TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_role (
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(30) NOT NULL,
    role_key VARCHAR(100) NOT NULL UNIQUE,
    role_sort INTEGER DEFAULT 0,
    status CHAR(1) DEFAULT '0',
    create_time TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id BIGSERIAL PRIMARY KEY,
    menu_name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INTEGER DEFAULT 0,
    path VARCHAR(200) DEFAULT '',
    component VARCHAR(255) DEFAULT '',
    query VARCHAR(255) DEFAULT '',
    is_frame INT DEFAULT 1,
    menu_type CHAR(1) DEFAULT '',  -- M=directory, C=menu, F=button
    visible CHAR(1) DEFAULT '0',
    status CHAR(1) DEFAULT '0',
    perms VARCHAR(100) DEFAULT '',
    icon VARCHAR(100) DEFAULT '#',
    create_time TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS sys_user_tenant (
    user_id BIGINT NOT NULL REFERENCES sys_user(user_id),
    tenant_id BIGINT REFERENCES tenants(id),
    PRIMARY KEY (user_id)
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    oper_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(50) DEFAULT '',
    business_type VARCHAR(50) DEFAULT '',
    method VARCHAR(100) DEFAULT '',
    request_method VARCHAR(10) DEFAULT '',
    operator_type VARCHAR(50) DEFAULT '',
    oper_name VARCHAR(50) DEFAULT '',
    oper_url VARCHAR(255) DEFAULT '',
    oper_ip VARCHAR(128) DEFAULT '',
    oper_param TEXT DEFAULT '',
    json_result TEXT DEFAULT '',
    status INT DEFAULT 0,
    error_msg TEXT DEFAULT '',
    cost_time BIGINT DEFAULT 0,
    oper_time TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_config (
    config_id SERIAL PRIMARY KEY,
    config_name VARCHAR(100) DEFAULT '',
    config_key VARCHAR(100) DEFAULT '',
    config_value VARCHAR(500) DEFAULT '',
    config_type CHAR(1) DEFAULT 'N',
    create_by VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    update_by VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    remark VARCHAR(500) DEFAULT ''
);
