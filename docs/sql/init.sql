-- ============================================================
-- 基于 Spring Boot 与 AI 的任务分配平台 - 数据库初始化脚本
-- 可重复执行，使用 IF NOT EXISTS + ON DUPLICATE KEY UPDATE
-- ============================================================

CREATE DATABASE IF NOT EXISTS task_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE task_ai;

-- ------------------------------------------------------------
-- 一、用户权限
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_user (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  username        VARCHAR(50)  NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL COMMENT 'BCrypt 哈希值，明文仅用于开发占位',
  real_name       VARCHAR(50)  NOT NULL,
  email           VARCHAR(100),
  phone           VARCHAR(30),
  avatar_url      VARCHAR(500),
  status          VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  role_code       VARCHAR(50)  NOT NULL UNIQUE,
  role_name       VARCHAR(50)  NOT NULL,
  description     VARCHAR(255),
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
  id              BIGINT   PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT   NOT NULL,
  role_id         BIGINT   NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id),
  CONSTRAINT fk_sur_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_sur_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 二、成员能力画像
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS skill (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  skill_name      VARCHAR(80)  NOT NULL UNIQUE,
  category        VARCHAR(80),
  description     VARCHAR(255),
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_skill (
  id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT        NOT NULL,
  skill_id        BIGINT        NOT NULL,
  level           INT           NOT NULL DEFAULT 1 COMMENT '1-5',
  years           DECIMAL(4,1)  NOT NULL DEFAULT 0.0,
  description     VARCHAR(255),
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_skill (user_id, skill_id),
  CONSTRAINT fk_us_user  FOREIGN KEY (user_id)  REFERENCES sys_user(id),
  CONSTRAINT fk_us_skill FOREIGN KEY (skill_id) REFERENCES skill(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS member_profile (
  id                    BIGINT        PRIMARY KEY AUTO_INCREMENT,
  user_id               BIGINT        NOT NULL UNIQUE,
  resume_text           TEXT,
  experience_summary    TEXT,
  current_workload      INT           NOT NULL DEFAULT 0,
  completed_task_count  INT           NOT NULL DEFAULT 0,
  overdue_task_count    INT           NOT NULL DEFAULT 0,
  task_completion_rate  DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
  overdue_rate          DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
  created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_mp_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 三、项目任务
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS project (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  project_name    VARCHAR(120) NOT NULL,
  description     TEXT,
  manager_id      BIGINT       NOT NULL,
  status          VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
  start_date      DATE,
  end_date        DATE,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_proj_manager FOREIGN KEY (manager_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS project_member (
  id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
  project_id      BIGINT      NOT NULL,
  user_id         BIGINT      NOT NULL,
  project_role    VARCHAR(80),
  joined_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_project_member (project_id, user_id),
  CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_pm_user    FOREIGN KEY (user_id)    REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task (
  id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  project_id      BIGINT        NOT NULL,
  title           VARCHAR(160)  NOT NULL,
  description     TEXT,
  priority        VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
  status          VARCHAR(30)   NOT NULL DEFAULT 'UNASSIGNED',
  creator_id      BIGINT        NOT NULL,
  assignee_id     BIGINT,
  deadline        DATETIME,
  estimated_hours DECIMAL(6,2),
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_task_project  FOREIGN KEY (project_id)  REFERENCES project(id),
  CONSTRAINT fk_task_creator  FOREIGN KEY (creator_id)  REFERENCES sys_user(id),
  CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_required_skill (
  id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
  task_id         BIGINT        NOT NULL,
  skill_id        BIGINT        NOT NULL,
  weight          DECIMAL(4,2)  NOT NULL DEFAULT 1.00,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_trs (task_id, skill_id),
  CONSTRAINT fk_trs_task  FOREIGN KEY (task_id)  REFERENCES task(id),
  CONSTRAINT fk_trs_skill FOREIGN KEY (skill_id) REFERENCES skill(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_assignment (
  id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
  task_id         BIGINT      NOT NULL,
  assignee_id     BIGINT      NOT NULL,
  assigned_by     BIGINT      NOT NULL,
  source          VARCHAR(30) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL / AI',
  reason          TEXT,
  assigned_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ta_task     FOREIGN KEY (task_id)     REFERENCES task(id),
  CONSTRAINT fk_ta_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id),
  CONSTRAINT fk_ta_operator FOREIGN KEY (assigned_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_result (
  id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
  task_id         BIGINT      NOT NULL,
  user_id         BIGINT      NOT NULL,
  result_summary  TEXT        NOT NULL,
  result_url      VARCHAR(500),
  review_status   VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  review_comment  TEXT,
  submitted_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at     DATETIME,
  CONSTRAINT fk_tres_task FOREIGN KEY (task_id) REFERENCES task(id),
  CONSTRAINT fk_tres_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_status_log (
  id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
  task_id         BIGINT      NOT NULL,
  old_status      VARCHAR(30),
  new_status      VARCHAR(30) NOT NULL,
  operator_id     BIGINT      NOT NULL,
  remark          VARCHAR(500),
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tsl_task     FOREIGN KEY (task_id)     REFERENCES task(id),
  CONSTRAINT fk_tsl_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 四、AI 分配
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ai_recommendation_batch (
  id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
  task_id         BIGINT      NOT NULL,
  request_text    TEXT,
  model_name      VARCHAR(100),
  status          VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
  created_by      BIGINT      NOT NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_arb_task   FOREIGN KEY (task_id)    REFERENCES task(id),
  CONSTRAINT fk_arb_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_recommendation_candidate (
  id                  BIGINT        PRIMARY KEY AUTO_INCREMENT,
  batch_id            BIGINT        NOT NULL,
  candidate_user_id   BIGINT        NOT NULL,
  rank_no             INT           NOT NULL,
  total_score         DECIMAL(6,2)  NOT NULL,
  skill_score         DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
  history_score       DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
  workload_score      DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
  completion_score    DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
  deadline_risk_score DECIMAL(6,2)  NOT NULL DEFAULT 0.00,
  reason              TEXT,
  accepted            INT           NOT NULL DEFAULT 0,
  created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_arc_batch   FOREIGN KEY (batch_id)          REFERENCES ai_recommendation_batch(id),
  CONSTRAINT fk_arc_user    FOREIGN KEY (candidate_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 五、RAG 知识沉淀
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT       NOT NULL,
  source_type     VARCHAR(40)  NOT NULL COMMENT 'RESUME / TASK_RESULT',
  source_id       BIGINT,
  title           VARCHAR(160) NOT NULL,
  content         TEXT         NOT NULL,
  indexed         INT          NOT NULL DEFAULT 0,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_akd_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 初始化数据
-- ============================================================

-- 角色
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
  (1, 'ADMIN',  '管理员',   '维护用户、角色、技能等基础数据'),
  (2, 'MANAGER','项目经理', '创建项目、创建任务并确认任务分配'),
  (3, 'MEMBER', '成员',     '维护能力画像、执行任务并提交成果')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

-- 演示用户（password_hash 当前为明文占位，后续接 BCrypt 替换）
INSERT INTO sys_user (id, username, password_hash, real_name, email, status) VALUES
  (1, 'admin',   'admin123',   '管理员',   'admin@example.com',   'ENABLED'),
  (2, 'manager', 'manager123', '项目经理', 'manager@example.com', 'ENABLED'),
  (3, 'member',  'member123',  '成员用户', 'member@example.com',  'ENABLED')
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), email = VALUES(email), status = VALUES(status);

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
  (1, 1),
  (2, 2),
  (3, 3)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 基础技能
INSERT INTO skill (id, skill_name, category, description) VALUES
  (1, 'Spring Boot',   '后端',       'Spring Boot 后端开发'),
  (2, 'Vue',           '前端',       'Vue 前端开发'),
  (3, 'MySQL',         '数据库',     'MySQL 表设计与查询优化'),
  (4, 'Redis',         '数据库',     'Redis 缓存设计与使用'),
  (5, 'Spring AI',     'AI',         'Spring AI 框架集成'),
  (6, '接口设计',       '后端',       'RESTful API 设计与文档'),
  (7, '前端页面开发',    '前端',       '页面布局、组件开发与交互'),
  (8, '测试与联调',     '质量保障',    '单元测试、接口联调与回归验证')
ON DUPLICATE KEY UPDATE category = VALUES(category), description = VALUES(description);
