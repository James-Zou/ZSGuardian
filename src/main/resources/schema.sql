-- 创建代码审查记录表
CREATE TABLE code_review_records (
    id BIGSERIAL PRIMARY KEY,
    repository VARCHAR(255) NOT NULL,
    branch VARCHAR(255) NOT NULL,
    total_commits INTEGER NOT NULL,
    review_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

);

-- 创建索引
CREATE INDEX idx_code_review_records_repository ON code_review_records(repository);
CREATE INDEX idx_code_review_records_branch ON code_review_records(branch);
CREATE INDEX idx_code_review_records_created_at ON code_review_records(created_at DESC);

-- 添加注释
COMMENT ON TABLE code_review_records IS '代码审查记录表';
COMMENT ON COLUMN code_review_records.id IS '主键ID';
COMMENT ON COLUMN code_review_records.repository IS '代码仓库地址';
COMMENT ON COLUMN code_review_records.branch IS '分支名称';
COMMENT ON COLUMN code_review_records.total_commits IS '提交总数';
COMMENT ON COLUMN code_review_records.review_data IS '审查详细数据（JSON格式）';
COMMENT ON COLUMN code_review_records.created_at IS '创建时间';



-- 创建用户表
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 添加唯一约束
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT uk_sys_user_email UNIQUE (email),
    CONSTRAINT uk_sys_user_phone UNIQUE (phone),
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN last_login_time TIMESTAMP,
    ADD COLUMN last_login_ip VARCHAR(50)
);

-- 添加索引
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_email ON sys_user(email);
CREATE INDEX idx_sys_user_phone ON sys_user(phone);
CREATE INDEX idx_sys_user_enabled ON sys_user(enabled);

-- 添加注释
COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.enabled IS '用户状态：true-启用 false-禁用';
COMMENT ON COLUMN sys_user.last_login_time IS '最后登录时间';
COMMENT ON COLUMN sys_user.last_login_ip IS '最后登录IP';

-- 需求表
CREATE TABLE requirement (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(1000) NOT NULL,
    description TEXT,
    document_path VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creator_id BIGINT REFERENCES sys_user(id),
    code VARCHAR(200)
);

COMMENT ON TABLE requirement IS '需求表';
COMMENT ON COLUMN requirement.id IS '需求ID';
COMMENT ON COLUMN requirement.title IS '需求标题';
COMMENT ON COLUMN requirement.description IS '需求描述';
COMMENT ON COLUMN requirement.document_path IS '需求文档路径';
COMMENT ON COLUMN requirement.create_time IS '创建时间';
COMMENT ON COLUMN requirement.update_time IS '更新时间';
COMMENT ON COLUMN requirement.creator_id IS '创建人ID';
COMMENT ON COLUMN requirement.code IS '需求代码';

CREATE INDEX idx_requirement_create_time ON requirement(create_time);

ALTER TABLE requirement ADD CONSTRAINT uk_requirement_code UNIQUE (code);










CREATE TABLE bug (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(1000) NOT NULL,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tester_id BIGINT,
    assignee_id BIGINT,
    root_cause TEXT,
    status VARCHAR(20),
    priority VARCHAR(20),
    requirement_id BIGINT REFERENCES requirement(id),
    creator_id BIGINT REFERENCES sys_user(id)
);






-- 重新创建测试用例表
CREATE TABLE test_case (
    id BIGSERIAL PRIMARY KEY,
    requirement_id BIGINT REFERENCES requirement(id),
    parent_id BIGINT,
    title VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'folder' 或 'case'
    level INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER,
    priority VARCHAR(20), -- HIGH/MEDIUM/LOW
    precondition TEXT,
    steps TEXT,
    expected_result TEXT,
    description TEXT,
    tags TEXT[],
    configuration TEXT, -- 存储API测试配置等
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator_id BIGINT REFERENCES sys_user(id)
);

-- 添加索引
CREATE INDEX idx_test_case_requirement ON test_case(requirement_id);
CREATE INDEX idx_test_case_parent ON test_case(parent_id);
CREATE INDEX idx_test_case_type ON test_case(type);
CREATE INDEX idx_test_case_level ON test_case(level);
CREATE INDEX idx_test_case_create_time ON test_case(create_time);

-- 添加注释
COMMENT ON TABLE test_case IS '测试用例表';
COMMENT ON COLUMN test_case.id IS '用例ID';
COMMENT ON COLUMN test_case.requirement_id IS '关联需求ID';
COMMENT ON COLUMN test_case.parent_id IS '父节点ID';
COMMENT ON COLUMN test_case.title IS '用例标题';
COMMENT ON COLUMN test_case.type IS '节点类型(folder/case)';
COMMENT ON COLUMN test_case.level IS '树形层级';
COMMENT ON COLUMN test_case.sort_order IS '同级排序';
COMMENT ON COLUMN test_case.priority IS '优先级(HIGH/MEDIUM/LOW)';
COMMENT ON COLUMN test_case.precondition IS '前置条件';
COMMENT ON COLUMN test_case.steps IS '测试步骤';
COMMENT ON COLUMN test_case.expected_result IS '预期结果';
COMMENT ON COLUMN test_case.description IS '用例描述';
COMMENT ON COLUMN test_case.tags IS '标签数组';
COMMENT ON COLUMN test_case.configuration IS '测试配置';
COMMENT ON COLUMN test_case.create_time IS '创建时间';
COMMENT ON COLUMN test_case.update_time IS '更新时间';
COMMENT ON COLUMN test_case.creator_id IS '创建人ID';

-- 重新创建执行记录表
CREATE TABLE execution_record (
    id BIGSERIAL PRIMARY KEY,
    requirement_id BIGINT REFERENCES requirement(id),
    test_case_id BIGINT REFERENCES test_case(id),
    status VARCHAR(20),
    progress INTEGER DEFAULT 0,
    duration DOUBLE PRECISION,
    logs TEXT,
    screenshot TEXT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    executor VARCHAR(100),
    creator_id BIGINT REFERENCES sys_user(id)
);

-- 重新创建测试用例和bug关联表
CREATE TABLE test_case_bug (
    test_case_id BIGINT REFERENCES test_case(id) ON DELETE CASCADE,
    bug_id BIGINT REFERENCES bug(id) ON DELETE CASCADE,
    PRIMARY KEY (test_case_id, bug_id)
);

-- 更新用户表结构
ALTER TABLE sys_user 
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN last_login_time TIMESTAMP,
    ADD COLUMN last_login_ip VARCHAR(50);

-- 添加索引
CREATE INDEX idx_sys_user_enabled ON sys_user(enabled);

-- 设置管理员账号为启用状态
UPDATE sys_user SET enabled = true WHERE id = 1;

-- 添加注释
COMMENT ON COLUMN sys_user.enabled IS '用户状态：true-启用 false-禁用';
COMMENT ON COLUMN sys_user.last_login_time IS '最后登录时间';
COMMENT ON COLUMN sys_user.last_login_ip IS '最后登录IP';





-- 角色表
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 插入基础角色数据
INSERT INTO roles (id, code, name, description) VALUES
(1, 'ADMIN', '管理员', '系统管理员，具有所有权限'),
(2, 'USER', '普通用户', '普通用户，具有基本操作权限');

-- 插入管理员用户
INSERT INTO user_roles (user_id, role_id) VALUES(1, 1);







-- Remove duplicate test_case_bug table definition and keep only one with proper foreign keys
DROP TABLE IF EXISTS test_case_bug;
CREATE TABLE test_case_bug (
    test_case_id BIGINT ,
    bug_id BIGINT ,
    PRIMARY KEY (test_case_id, bug_id)
);

ALTER TABLE test_case ADD COLUMN tree_data JSON;



-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS execution_cases;




-- 创建执行计划表
CREATE TABLE IF NOT EXISTS execution_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    test_case_id BIGINT,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    completion_time DATETIME,
    execution_score VARCHAR(50),
    creator_id BIGINT,
    executor_id BIGINT,
    total_cases INT,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id),
    FOREIGN KEY (creator_id) REFERENCES users(id),
    FOREIGN KEY (executor_id) REFERENCES users(id)
);

-- 创建执行用例表
CREATE TABLE IF NOT EXISTS execution_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_plan_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    priority VARCHAR(50),
    precondition TEXT,
    steps TEXT,
    expected_result TEXT,
    actual_result TEXT,
    result VARCHAR(50),
    bug_description TEXT,
    status VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    sort_order INT,
    FOREIGN KEY (execution_plan_id) REFERENCES execution_plans(id),
    INDEX idx_execution_plan (execution_plan_id),
    INDEX idx_parent (parent_id)
);



-- 删除已存在的表（如果存在）
DROP TABLE IF EXISTS bug;

CREATE TABLE bug (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(1000) NOT NULL,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tester_id BIGINT REFERENCES sys_user(id),
    assignee_id BIGINT REFERENCES sys_user(id),
    root_cause TEXT,
    status VARCHAR(20),
    priority VARCHAR(20),
    requirement_id BIGINT REFERENCES requirement(id),
    creator_id BIGINT REFERENCES sys_user(id)
);



-- 更新需求表结构
ALTER TABLE requirement
    ADD COLUMN updator_id BIGINT REFERENCES sys_user(id);

-- 变更评审主表
CREATE TABLE change_review (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    change_time TIMESTAMP NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    description TEXT,
    document_url VARCHAR(500),
    executor_id BIGINT NOT NULL REFERENCES sys_user(id),
    auditor_id BIGINT NOT NULL REFERENCES sys_user(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 变更-需求关联表
CREATE TABLE change_review_requirement (
    change_review_id BIGINT REFERENCES change_review(id) ON DELETE CASCADE,
    requirement_id BIGINT REFERENCES requirement(id),
    PRIMARY KEY (change_review_id, requirement_id)
);

-- 变更-Bug关联表
CREATE TABLE change_review_bug (
    change_review_id BIGINT REFERENCES change_review(id) ON DELETE CASCADE,
    bug_id BIGINT REFERENCES bug(id),
    PRIMARY KEY (change_review_id, bug_id)
);

-- 更新bug表结构
ALTER TABLE bug
    ADD COLUMN bug_id BIGINT REFERENCES bug(id);

-- 更新bug表结构
ALTER TABLE bug
    ADD COLUMN code VARCHAR(200);
