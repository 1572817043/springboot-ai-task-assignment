# 当前进度快照

## 已完成阶段

### 第一阶段：项目骨架与基础页面

完成时间：2026-06-29

已完成内容：

- 创建后端 Spring Boot 项目骨架，使用 Maven Wrapper。
- 新增后端统一响应结构 `ApiResponse`。
- 新增后端健康检查接口 `GET /api/system/health`。
- 创建前端 Vue3 + Vite + TypeScript 项目骨架。
- 配置 Tailwind CSS、Vue Router、Pinia、Axios、ECharts、`@lucide/vue`。
- 完成简洁工具型主布局。
- 完成登录页、工作台、项目管理、任务列表、AI 分配、成员画像、设置页基础界面。
- 清理 Vite 和 Spring Initializr 生成的无关示例文件。

### 第二阶段：登录与权限基础

完成时间：2026-06-29

已完成内容：

- 新增后端登录接口 `POST /api/auth/login`。
- 新增后端当前用户接口 `GET /api/auth/me`。
- 新增内存演示用户：管理员、项目经理、成员。
- 新增 HMAC token 生成和解析逻辑。
- 新增业务异常 `BusinessException`，统一处理登录失败、未登录和请求方法不支持。
- 前端登录页接入真实登录接口。
- 前端新增 token 和用户信息持久化。
- 前端新增路由守卫，未登录访问工作台会跳转登录页。
- 新增数据库初始化草稿 `docs/sql/init.sql`，覆盖用户、角色、技能、项目、任务、成果、AI 推荐和知识库基础表。

演示账号：

```text
admin / admin123
manager / manager123
member / member123
```

## 验证结果

后端：

```text
./mvnw test
结果：7 个测试通过，0 个失败
```

前端：

```text
npm test
结果：7 个测试通过，0 个失败
```

```text
npm run build
结果：构建通过
```

接口联调：

```text
POST /api/auth/login
结果：200，返回 token 和项目经理用户信息

GET /api/auth/me
结果：200，返回当前用户信息

GET /api/auth/me 不带 token
结果：401，提示未登录或登录已过期
```

浏览器检查：

```text
登录页提交 manager / manager123 后可跳转到工作台
token 和用户信息可正常写入 localStorage
工作台界面无明显错位
未发现浏览器控制台错误
```

## 当前运行地址

```text
后端：http://127.0.0.1:8080
前端：http://127.0.0.1:5173
```

## 下一阶段

第三阶段：数据库持久化与成员基础管理。

建议下一步实现：

- 引入 MyBatis Plus 和 MySQL 连接。
- 将当前内存用户切换为数据库用户。
- 密码改为加密存储。
- 实现用户列表、新增用户、编辑用户和启停用户。
- 实现技能标签列表和用户技能维护。
- 实现成员画像基础信息维护。
