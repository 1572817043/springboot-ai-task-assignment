# 基于 Spring Boot 与 AI 的任务分配平台

基于 Spring Boot 与 Vue 的智能任务分配平台，面向项目团队中的任务创建、成员画像维护、AI 推荐分配、成果提交与知识沉淀等流程。项目以毕业设计和软件开发实践为目标，重点展示“员工能力画像 + 历史任务成果 + RAG 知识库 + 推荐评分”的任务分配思路。

## 项目定位

传统任务分配通常依赖项目经理的经验判断，容易出现成员能力掌握不完整、任务负载不均衡、历史成果难以复用等问题。本平台希望通过结构化数据和 AI 辅助推荐，让任务分配过程更可解释、更可追踪。

平台主要面向项目型团队，核心使用者包括：

- 管理员：维护用户、角色、技能等基础数据。
- 项目经理：创建项目和任务，查看 AI 推荐结果，并确认最终任务分配。
- 成员：维护个人能力画像，接收任务，提交任务成果。

## 技术栈

后端：

- Spring Boot 4
- Java 21
- MyBatis Plus
- MySQL
- Maven

前端：

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Tailwind CSS
- lucide-vue

测试：

- 后端：JUnit、Spring Boot Test、H2
- 前端：Vitest、Vue Test Utils、jsdom

## 功能模块

- 用户与权限：登录认证、Token 校验、角色管理、用户管理。
- 成员画像：维护简历、经验总结、技能等级、工作负载、完成率等信息。
- 技能管理：维护平台内可用于任务匹配的技能标签。
- 项目管理：创建项目、维护项目成员、管理项目状态。
- 任务管理：创建任务、维护优先级、截止时间、所需技能、任务状态。
- 任务分配：支持人工分配和 AI 推荐后的确认分配。
- 成果提交：成员提交任务成果，负责人审核成果并沉淀为历史数据。
- AI 推荐：根据技能匹配、历史表现、当前负载、完成率、截止风险等维度生成候选人排序。
- AI 知识库：沉淀成员简历和任务成果，为后续 RAG 检索与推荐解释提供数据基础。

## AI 分配与 RAG 思路

本项目的 AI 分配不是只让大模型直接“拍脑袋”选人，而是先把平台内可验证的数据沉淀下来，再让 AI 在可解释的数据基础上辅助决策。

核心思路如下：

1. 建立成员画像：将成员简历、技能标签、技能等级、项目经历、历史任务成果、审核评价等信息持续沉淀。
2. 建立任务画像：记录任务描述、任务所需技能、任务优先级、预计工时、截止时间、所属项目等信息。
3. 建立 RAG 知识库：把成员简历和长期提交任务后的成果转为知识文档，并在后续接入向量化索引。
4. 检索相关上下文：当项目经理创建任务并请求 AI 分配时，根据任务描述和技能要求检索相关成员经历、成果片段和历史表现。
5. 结合规则评分：在 RAG 检索结果之外，再结合技能匹配、历史完成质量、当前工作负载、完成率、延期风险等指标进行综合评分。
6. 输出推荐解释：给出推荐成员、排序、各项得分和推荐理由，项目经理保留最终确认权。

当前工程已经完成规则评分式 AI 推荐和 `ai_knowledge_document` 知识文档沉淀，适合作为毕业设计中的第一阶段实现。后续可以接入 Spring AI 的 EmbeddingModel、VectorStore 和 ChatModel，把 `indexed` 状态的知识文档真正向量化，实现“任务描述 -> 检索成员相关经历 -> 生成推荐理由”的完整 RAG 流程。

## 工程结构

- `backend/`：Spring Boot 后端服务。
- `frontend/`：Vue 前端应用。
- `docs/sql/init.sql`：MySQL 初始化脚本，包含表结构和演示数据。
- `docs/`：项目规划、进度和辅助文档。

## 本地运行

准备 MySQL 数据库并执行初始化脚本：

```bash
mysql -u root -p < docs/sql/init.sql
```

后端开发环境默认读取 `application-dev.yml`，数据库用户名、密码和 JWT 密钥建议通过环境变量配置：

```bash
export DB_USERNAME=root
export DB_PASSWORD=123456
export JWT_SECRET=dev-secret-key-change-in-production
```

启动后端：

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

启动前端：

```bash
cd frontend
npm install
npm run dev
```

默认演示账号来自初始化脚本，仅用于本地开发：

- `admin / admin123`
- `manager / manager123`
- `member / member123`

## 测试与构建

后端测试：

```bash
cd backend
./mvnw test
```

前端测试与构建：

```bash
cd frontend
npm run test
npm run build
```

## 当前实现状态

当前版本已完成平台基础框架、主要业务表、后端接口、前端页面、AI 推荐评分、知识文档沉淀和基础测试。后续重点可以继续完善 Spring AI 接入、向量数据库、RAG 检索链路、接口权限细化和论文配套材料。

## 许可证

本项目暂未声明开源许可证，当前仅作为毕业设计和学习实践项目使用。
