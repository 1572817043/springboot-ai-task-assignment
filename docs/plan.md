# 基于 Spring Boot 与 AI 的任务分配平台项目规划

## 1. 项目定位

本项目面向中小型软件开发团队，提供项目任务管理、成员能力画像、AI 辅助任务推荐分配、任务进度跟踪和统计分析能力。

系统不做“AI 自动强制分配”，而是做“AI 辅助推荐分配”。项目经理创建任务后，系统根据任务内容、成员简历、技能标签、历史任务成果和当前工作负载，推荐合适的负责人，并给出推荐理由，最终由项目经理确认。

## 2. 核心业务闭环

```text
管理员创建用户和技能标签
  ↓
成员完善简历、技能和个人经历
  ↓
项目经理创建项目并添加成员
  ↓
项目经理创建任务
  ↓
系统分析任务所需能力
  ↓
系统检索成员简历和历史成果
  ↓
系统计算候选成员推荐分数
  ↓
AI 生成推荐理由
  ↓
项目经理确认分配
  ↓
成员执行任务并提交成果
  ↓
系统沉淀成果，后续继续用于推荐
```

## 3. 角色设计

### 管理员

负责系统基础数据和用户权限管理。

- 用户管理
- 角色分配
- 技能标签管理
- 任务类型管理
- 查看系统操作日志

### 项目经理

负责项目和任务分配，是平台的核心使用者。

- 创建和维护项目
- 添加项目成员
- 创建任务
- 查看 AI 推荐分配结果
- 确认或调整任务负责人
- 跟踪项目进度
- 查看任务统计

### 成员

负责维护个人能力信息和执行任务。

- 维护个人简历
- 维护技能标签
- 查看分配给自己的任务
- 更新任务状态
- 提交任务成果
- 查看个人任务统计

## 4. 功能模块

### 用户认证与权限模块

- 登录
- 退出登录
- 当前用户信息查询
- 基于角色控制菜单和接口权限

### 用户与成员管理模块

- 用户列表
- 新增用户
- 编辑用户基础信息
- 分配角色
- 启用或禁用用户

### 项目管理模块

- 项目列表
- 新建项目
- 编辑项目信息
- 项目详情
- 项目成员管理
- 项目状态管理：未开始、进行中、已完成、已暂停

### 任务管理模块

- 任务列表
- 新建任务
- 编辑任务
- 任务详情
- 修改任务状态
- 手动分配负责人
- 查看任务成果

任务状态：

```text
待分配
待开始
进行中
待验收
已完成
已逾期
```

任务优先级：

```text
低
中
高
紧急
```

### 成员能力画像模块

- 成员简历信息
- 技能标签
- 当前任务负载
- 历史任务完成情况
- 历史任务成果
- 完成率和逾期率

### AI 辅助任务分配模块

第一阶段先做规则推荐，保证系统能完整运行。

推荐评分维度：

```text
技能匹配度：35%
历史相似任务成果：25%
当前工作量：20%
历史完成率：10%
截止时间风险：10%
```

第二阶段接入 RAG。

- 将成员简历向量化
- 将历史任务成果向量化
- 根据新任务描述检索相似经历和成果
- 将检索结果作为推荐依据

第三阶段接入大模型。

- 分析任务所需技能
- 生成推荐理由
- 解释候选成员优缺点
- 生成任务拆解建议

### 任务成果沉淀模块

- 成员提交成果说明
- 上传或填写成果链接
- 项目经理验收
- 成果进入成员历史成果库
- 后续作为 RAG 检索数据来源

### 工作台与统计模块

- 项目数量
- 任务数量
- 待分配任务数
- 逾期任务数
- AI 推荐次数
- AI 推荐采纳率
- 成员任务负载分布
- 项目完成进度

## 5. 技术栈

### 后端

```text
Spring Boot
Spring Security
MyBatis Plus
MySQL
Redis
Spring AI
PostgreSQL + PGVector
Maven
```

说明：

- MySQL 存储业务数据。
- Redis 可用于登录 token、验证码或缓存。
- PostgreSQL + PGVector 用于后期 RAG 向量检索。
- Spring AI 后期用于接入大模型和向量数据库。

### 前端

```text
Vue 3
Vite
TypeScript
Tailwind CSS
shadcn-vue 风格组件
Pinia
Vue Router
Axios
ECharts
lucide-vue-next
```

说明：

- 不使用 Element Plus。
- 视觉风格参考 Codex、Linear、Notion。
- 整体保持简洁、克制、工具型产品感。

## 6. 后端目录结构

```text
backend/
  pom.xml
  src/main/java/com/example/taskai/
    TaskAiApplication.java
    common/
      result/
      exception/
      page/
      constants/
    config/
      security/
      mybatis/
      ai/
    auth/
      controller/
      service/
      dto/
      vo/
    user/
      controller/
      service/
      mapper/
      entity/
      dto/
      vo/
    project/
      controller/
      service/
      mapper/
      entity/
      dto/
      vo/
    task/
      controller/
      service/
      mapper/
      entity/
      dto/
      vo/
    member/
      controller/
      service/
      mapper/
      entity/
      dto/
      vo/
    result/
      controller/
      service/
      mapper/
      entity/
      dto/
      vo/
    ai/
      controller/
      service/
      dto/
      vo/
      scorer/
      rag/
    dashboard/
      controller/
      service/
      vo/
  src/main/resources/
    application.yml
    mapper/
```

## 7. 前端目录结构

```text
frontend/
  package.json
  index.html
  vite.config.ts
  src/
    main.ts
    App.vue
    api/
      request.ts
      auth.ts
      user.ts
      project.ts
      task.ts
      member.ts
      ai.ts
      dashboard.ts
    assets/
    components/
      common/
      layout/
      task/
      member/
      ai/
    layouts/
      AuthLayout.vue
      MainLayout.vue
    router/
      index.ts
    stores/
      auth.ts
      user.ts
    styles/
      index.css
    views/
      login/
        LoginView.vue
      dashboard/
        DashboardView.vue
      projects/
        ProjectListView.vue
        ProjectDetailView.vue
      tasks/
        TaskListView.vue
        TaskDetailView.vue
      ai-assignment/
        AiAssignmentView.vue
      members/
        MemberListView.vue
        MemberProfileView.vue
      settings/
        UserSettingsView.vue
        SkillSettingsView.vue
```

## 8. 页面设计

### 整体布局

采用简洁工具型布局：

```text
左侧导航栏 + 顶部标题区 + 主内容区
```

视觉原则：

- 白色和浅灰色背景
- 黑白灰为主色
- 少量蓝色或绿色作为强调色
- 使用细边框
- 少阴影
- 不做渐变大屏
- 不做发光、玻璃拟态、装饰性背景

### 登录页

- 居中登录面板
- 项目名称
- 账号输入
- 密码输入
- 登录按钮

### 工作台

展示当前团队和项目的核心状态。

- 项目总数
- 任务总数
- 待分配任务
- 逾期任务
- 最近任务
- 最近 AI 推荐记录
- 简洁趋势图

### 项目管理页

- 项目列表
- 项目状态
- 项目负责人
- 项目成员数
- 项目进度
- 新建项目按钮

### 任务列表页

默认使用列表视图。

```text
任务标题 | 所属项目 | 优先级 | 状态 | 负责人 | 截止时间 | 操作
```

支持按项目、状态、优先级、负责人筛选。

### AI 分配页

作为系统重点展示页面。

```text
┌───────────────┬────────────────┬────────────────┐
│ 任务信息       │ 候选成员列表     │ AI 推荐结果      │
│ 标题           │ 张三 82 分       │ 推荐：张三       │
│ 描述           │ 李四 75 分       │ 推荐理由         │
│ 优先级         │ 王五 68 分       │ 匹配依据         │
│ 截止时间       │                 │ 确认分配按钮     │
└───────────────┴────────────────┴────────────────┘
```

### 成员画像页

- 基础信息
- 简历摘要
- 技能标签
- 当前任务负载
- 历史任务成果
- 完成率
- 逾期率

## 9. 数据库表设计

### sys_user

```text
id
username
password
real_name
email
phone
avatar
status
created_at
updated_at
```

### sys_role

```text
id
role_code
role_name
created_at
updated_at
```

### sys_user_role

```text
id
user_id
role_id
```

### skill

```text
id
name
category
description
created_at
updated_at
```

### user_skill

```text
id
user_id
skill_id
level
created_at
updated_at
```

### member_profile

```text
id
user_id
resume_text
experience_summary
current_workload
completion_rate
overdue_rate
created_at
updated_at
```

### project

```text
id
name
description
manager_id
status
start_date
end_date
created_at
updated_at
```

### project_member

```text
id
project_id
user_id
project_role
joined_at
```

### task

```text
id
project_id
title
description
priority
status
creator_id
assignee_id
deadline
created_at
updated_at
```

### task_assignment

```text
id
task_id
assignee_id
assigner_id
assign_type
recommendation_id
assigned_at
```

### task_result

```text
id
task_id
user_id
summary
result_url
quality_score
review_comment
submitted_at
reviewed_at
created_at
updated_at
```

### ai_recommendation

```text
id
task_id
recommended_user_id
score
skill_score
history_score
workload_score
completion_score
risk_score
reason
evidence
accepted
created_at
updated_at
```

### ai_knowledge_document

```text
id
user_id
source_type
source_id
title
content
embedding_id
created_at
updated_at
```

## 10. 主要接口规划

### 认证接口

```text
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

### 用户接口

```text
GET    /api/users
POST   /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
DELETE /api/users/{id}
```

### 项目接口

```text
GET    /api/projects
POST   /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}
GET    /api/projects/{id}/members
POST   /api/projects/{id}/members
DELETE /api/projects/{id}/members/{userId}
```

### 任务接口

```text
GET  /api/tasks
POST /api/tasks
GET  /api/tasks/{id}
PUT  /api/tasks/{id}
PUT  /api/tasks/{id}/status
PUT  /api/tasks/{id}/assign
```

### 成员画像接口

```text
GET /api/members
GET /api/members/{userId}/profile
PUT /api/members/{userId}/profile
GET /api/members/{userId}/results
```

### 任务成果接口

```text
POST /api/tasks/{taskId}/result
GET  /api/tasks/{taskId}/result
PUT  /api/tasks/{taskId}/result/review
```

### AI 推荐接口

```text
POST /api/ai/tasks/{taskId}/recommend
GET  /api/ai/tasks/{taskId}/recommendations
POST /api/ai/recommendations/{id}/accept
```

### 工作台接口

```text
GET /api/dashboard/summary
GET /api/dashboard/recent-tasks
GET /api/dashboard/recent-recommendations
```

## 11. 开发阶段

### 第一阶段：项目骨架与基础页面

目标：前后端项目能启动，整体布局确定。

- 创建 Spring Boot 后端项目
- 创建 Vue3 前端项目
- 配置前端路由
- 配置主布局
- 完成登录页静态界面
- 完成工作台静态界面

验收标准：

- 后端项目可以启动
- 前端项目可以启动
- 浏览器可以看到登录页和主布局

### 第二阶段：登录与权限

目标：完成基本登录流程。

- 后端实现用户表
- 后端实现登录接口
- 后端返回 token
- 前端保存 token
- 前端路由守卫
- 根据角色显示菜单

验收标准：

- 管理员、项目经理、成员可以登录
- 未登录访问主页面会跳转到登录页

### 第三阶段：项目与成员管理

目标：项目经理可以维护项目和项目成员。

- 项目增删改查
- 项目成员添加和移除
- 项目详情页
- 成员列表页

验收标准：

- 可以创建项目
- 可以给项目添加成员
- 可以查看项目详情和成员列表

### 第四阶段：任务管理与手动分配

目标：先跑通不依赖 AI 的任务分配流程。

- 创建任务
- 编辑任务
- 任务列表筛选
- 任务详情
- 手动分配负责人
- 成员查看自己的任务
- 成员更新任务状态

验收标准：

- 项目经理可以创建任务
- 项目经理可以手动分配任务
- 成员可以看到自己的任务

### 第五阶段：成员画像与成果沉淀

目标：为后续 AI 推荐提供数据基础。

- 成员维护简历
- 成员维护技能标签
- 成员提交任务成果
- 项目经理验收成果
- 统计成员完成率、逾期率和当前负载

验收标准：

- 成员资料中可以看到简历、技能和历史成果
- 完成任务后可以沉淀成果记录

### 第六阶段：规则版 AI 推荐

目标：在不依赖大模型的情况下实现推荐分配。

- 根据技能匹配度计算分数
- 根据历史任务成果计算分数
- 根据当前负载计算分数
- 根据完成率和截止时间计算风险分数
- 生成候选成员排名
- 项目经理确认推荐结果

验收标准：

- 创建任务后可以生成候选成员排名
- 推荐结果有分数和原因
- 项目经理可以采纳推荐

### 第七阶段：Spring AI 与 RAG

目标：增强推荐依据，让系统能利用简历和历史成果文本。

- 接入 Spring AI
- 配置大模型接口
- 接入 PostgreSQL + PGVector
- 将成员简历写入知识库
- 将任务成果写入知识库
- 根据任务描述检索相关经历和成果
- 将检索结果加入推荐理由

验收标准：

- 新任务可以检索到相似历史成果
- 推荐理由中能体现检索到的依据

### 第八阶段：统计分析与系统完善

目标：补齐毕设展示效果。

- 工作台统计
- 成员负载统计
- 项目进度统计
- AI 推荐采纳率
- 操作日志
- 页面细节优化

验收标准：

- 系统具有完整演示流程
- 首页和统计页能展示核心数据

## 12. 当前最先做什么

当前最先做第一阶段，不直接做 AI。

具体顺序：

```text
1. 搭 Spring Boot 后端骨架
2. 搭 Vue3 前端骨架
3. 做前端主布局和登录页
4. 做后端统一返回、异常处理、基础配置
5. 准备用户、角色、项目、任务的基础表结构
6. 跑通登录到工作台的最小流程
```

原因：

- AI 模块依赖成员、任务、成果这些基础数据。
- 先做 AI 会缺少真实业务输入。
- 先跑通任务分配闭环，后面再增强为 RAG 推荐，更稳。

## 13. 毕设论文重点

论文可以围绕以下几个点展开：

- 传统人工任务分配依赖经验，容易忽略成员能力和当前负载。
- 系统通过成员简历、技能标签和历史成果建立能力画像。
- 系统通过规则评分和 RAG 检索辅助项目经理进行任务分配。
- AI 推荐只提供辅助决策，最终由项目经理确认，保证可控性。
- 历史成果会持续沉淀，使后续推荐依据更丰富。

## 14. 最小可演示流程

```text
管理员登录
  ↓
创建项目经理和成员账号
  ↓
成员登录并完善简历和技能
  ↓
项目经理登录
  ↓
创建项目
  ↓
添加项目成员
  ↓
创建任务
  ↓
点击 AI 推荐
  ↓
查看候选成员排名和推荐理由
  ↓
确认分配
  ↓
成员接收任务
  ↓
成员提交成果
  ↓
项目经理验收
  ↓
成果进入成员画像
```
