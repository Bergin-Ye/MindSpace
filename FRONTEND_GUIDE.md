# MindSpace 前端开发说明

这份文档是给前端开发看的，重点说明“后端能提供什么、前端可以怎么组织页面和交互”，不展开具体接口字段。具体请求和响应以 [API.md](API.md) 为准。

## 一、后端能提供什么

| 模块 | 前端可以做的功能 | 用户能看到什么 |
|---|---|---|
| 账号 | 注册、登录、查看当前用户、修改资料、修改密码 | 登录态、个人资料 |
| 知识库 | 新建、列表、详情、重命名、删除 | 自己的知识库列表 |
| 文件夹 | 平铺列表、树结构、新建、重命名、移动、删除 | 知识库目录树 |
| 文件 | 单文件/批量上传、分页列表、详情、下载、重命名、移动、删除 | 文件管理页面 |
| 文档解析 | 同步解析、异步解析、状态轮询、失败重试 | 解析状态、失败原因 |
| AI 分析 | 摘要、关键词、分类、置信度 | 文档的 AI 信息 |
| 全文检索 | 关键词搜索、组合筛选、分页 | 匹配文档和正文片段 |
| 语义检索 | 文档向量化、自然语言搜索 | 语义相关片段和相似度 |
| RAG 问答 | 基于知识库提问，返回答案和引用 | 回答、来源文档、来源片段 |
| 会话历史 | 会话列表、消息列表、重命名、删除 | 历史对话 |

## 二、推荐页面

| 路由 | 页面 | 主要内容 |
|---|---|---|
| `/login` | 登录 | 邮箱、密码，成功后保存 token |
| `/register` | 注册 | 用户名、邮箱、密码 |
| `/knowledge-bases` | 知识库列表 | 卡片/列表、新建、改名、删除 |
| `/knowledge-bases/:id/files` | 文件管理 | 左侧文件夹树，右侧文件列表，上传/移动/删除 |
| `/documents/:id` | 文档详情 | 文件信息、解析按钮、解析状态、AI 分析、正文预览、向量化 |
| `/search` | 搜索 | 关键词/语义切换、筛选条件、结果列表、正文片段 |
| `/chat` | RAG 问答 | 左侧会话列表，右侧消息区、输入框、引用片段 |

## 三、最小可用闭环

```text
注册/登录
  → 新建知识库
  → 上传一个 TXT/PDF/DOCX
  → 解析文档
  → 搜索文档内容
  → 向量化文档
  → 发起一次 RAG 问答
  → 查看会话历史
```

建议先把这个闭环跑通，再做文件夹树、批量上传、AI 分析等增强功能。

## 四、联调基础约定

- Base URL：`http://localhost:8080`
- 认证：请求头 `Authorization: Bearer <token>`
- 所有接口 HTTP 状态码统一为 200，业务结果看响应体里的 `code`
- 成功：`code = 200`
- 失败：`code` 为业务错误码，`message` 是原因
- 时间格式：ISO-8601，例如 `2026-11-22T20:00:00`
- 分页结构：`PageResult { total, page, size, pages, records }`

登录成功后建议：

- token 存 `localStorage` 或内存状态；
- 请求拦截器统一加 `Authorization`；
- 收到 `code=401` 时清除 token 并跳转登录页。

## 五、异步解析

文档解析建议走异步：

1. `POST /api/documents/{id}/parse-async` 提交；
2. 每 1–2 秒轮询 `GET /api/documents/{id}/parse-status`；
3. 直到 `processStatus` 为 `SUCCESS` 或 `FAILED`；
4. `FAILED` 时展示 `errorMessage`，提供“重新解析”。

前端状态建议：

```text
UPLOADED → 待解析
PROCESSING → 解析中（禁用重复提交）
SUCCESS → 解析成功
FAILED → 解析失败（展示原因，可重试）
```

## 六、AI 接口耗时

- `POST /api/documents/{id}/analyze` 和 `POST /api/chat/ask` 是同步调用大模型；
- 通常几秒到几十秒；
- 请求超时建议不低于 60 秒；
- 提交后加 loading，避免重复点击。

没有配置 AI key 时，这两个接口会返回 `3004`，前端可以提示“AI 服务未配置”，不影响其他功能。

## 七、文件上传

`multipart/form-data`：

| 字段 | 说明 |
|---|---|
| knowledgeBaseId | 必填 |
| folderId | 可选，不传使用默认文件夹 |
| file | 单文件上传 |
| files | 批量上传 |

限制：

- 单文件最大 20MB；
- 单请求最大 100MB；
- 支持 PDF、DOCX、TXT、MD。

上传完成后记得刷新文件列表。

## 八、两种搜索

| 类型 | 用途 | 前端表现 |
|---|---|---|
| 全文检索 | 找包含关键词的文档 | 关键词输入框 + 筛选条件 |
| 语义检索 | 找意思相关的文档 | 需要先向量化，展示相似度和片段 |

可以做成一个搜索框 + “关键词/语义”切换。

搜索结果建议展示：

- 文件名；
- 文件类型、大小、状态；
- 正文片段；
- 上传时间；
- 语义搜索时展示相似度。

## 九、会话

- 第一次提问不传 `sessionId`，后端自动新建会话；
- 后续追问把返回的 `sessionId` 传回来；
- 会话列表按更新时间倒序；
- 消息列表按时间正序；
- assistant 消息带引用片段；
- 删除会话会同时删除消息。

聊天页面建议：

- 左侧会话列表；
- 右侧消息气泡；
- assistant 消息下方展示引用卡片；
- 输入框支持 Enter 发送、Shift+Enter 换行；
- AI 回答中展示 loading。

## 十、错误处理

建议按 `code` 做统一提示：

| code 范围 | 含义 |
|---|---|
| 400 | 参数错误 |
| 401 | 未登录或登录过期 |
| 2001–2016 | 知识库、文件夹、文件、解析相关 |
| 3001–3008 | AI、向量化、RAG 相关 |

前端至少处理：

- 401 跳登录；
- 400 展示 `message`；
- 文件不存在/会话不存在展示空状态；
- AI 服务未配置展示配置提示；
- 网络错误和超时单独提示。

## 十一、CORS

后端没有配置 CORS。`frontend/` 已经用 Vite 的 devServer 代理把 `/api` 转发到 `http://localhost:8080`，本地开发不需要额外配置。

如果要部署到独立域名，再让后端在 `SecurityConfig` / `WebMvcConfigurer` 中加 CORS，或者在网关层处理。

## 十二、本地后端

前端联调时，后端需要先启动：

```powershell
cd E:\code_for_java\MindSpace
$env:JAVA_HOME = 'E:\jdk21'
$env:PATH = "E:\jdk21\bin;$env:PATH"
mvn spring-boot:run
```

默认端口 `8080`，默认 dev 配置，不需要环境变量。

语义检索需要本地 Ollama：

```powershell
ollama pull bge-m3
```

AI 分析、RAG 问答需要真实 AI key；没有 key 时只影响这两个功能。

## 十三、当前限制

- 前端已经实现，代码在 `frontend/`，覆盖接口文档里的全部页面；
- 未做 OCR，扫描版 PDF 可能解析失败；
- AI 分析和 RAG 问答是同步接口，没有流式输出；
- 异步解析使用单机内存线程池，应用重启会把处理中的文档标记为失败；
- 语义检索单次最多加载 5000 个 chunk；
- 未实现文件分享、多人协作、收藏、标签等功能。

## 十四、接口文档

具体路径、请求参数、响应字段和错误码见 [API.md](API.md)。

项目整体说明见 [README.md](README.md)。

## 十五、前端工程

前端已经实现并与后端放在同一个仓库，代码在 `frontend/`。

技术栈：

- Vue 3 + JavaScript（不是 TypeScript）
- Vite 8
- Vue Router（history 模式）
- Pinia
- axios

视觉与实现约定：

- 苹果极简 + 岛屿式卡片布局 + 毛玻璃（`backdrop-filter`），主色为橙色；
- 使用系统字体栈（SF Pro / Segoe UI / PingFang SC），不加载外部字体；
- 不引入 UI 组件库和图标库，所有图标都是内联 SVG；
- 顶栏、侧栏、页面标题、内容面板都是独立圆角岛屿，间距和圆角统一由 `styles/tokens.css` 控制；
- 响应式覆盖桌面、平板、390px 手机，移动端侧栏改为抽屉。

本地启动：

```powershell
cd E:\code_for_java\MindSpace\frontend
npm install
npm run dev
```

打开 `http://localhost:5173`。`vite.config.js` 已把 `/api` 代理到 `http://localhost:8080`，后端不需要配置 CORS。

构建：

```powershell
npm run build
```

产物在 `frontend/dist/`。部署时把静态文件交给 Nginx，并把 `/api` 反向代理到后端。

前端已覆盖的页面：

| 路由 | 页面 |
|---|---|
| `/login`、`/register` | 登录、注册 |
| `/knowledge-bases` | 知识库列表、新建、重命名、删除 |
| `/knowledge-bases/:id/files` | 文件夹树、上传、解析、移动、重命名、下载、删除 |
| `/documents/:id` | 解析进度轮询、AI 分析、向量化、正文预览 |
| `/search` | 关键词检索与语义检索、筛选、分页 |
| `/chat` | RAG 问答、引用片段、会话历史 |
| `/settings` | 修改资料、修改密码 |
