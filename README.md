# MindSpace 个人知识库

MindSpace 是一个面向个人学习的知识库后端项目。它把“文件管理、文档解析、AI 理解、全文检索、语义检索、RAG 问答、会话历史”串成了一条完整链路。

当前版本**只提供后端 API**，前端尚未实现。接口契约以 [API.md](API.md) 为准。

## 一、前端可以做什么

| 模块 | 前端可以做的页面/功能 | 用户能看到什么 |
|---|---|---|
| 账号 | 注册、登录、查看当前用户、修改资料、修改密码 | 登录态、个人资料、修改结果 |
| 知识库 | 新建、列表、详情、重命名、删除 | 自己的知识库列表 |
| 文件夹 | 平铺列表、树状结构、新建、重命名、移动、删除 | 知识库内的目录树 |
| 文件 | 单文件上传、批量上传、分页列表、详情、下载、重命名、移动、删除 | 文件列表和文件操作 |
| 文档解析 | 同步解析、异步解析、查询解析状态、失败重试 | 正文是否解析成功、失败原因 |
| AI 分析 | 生成摘要、关键词、分类、置信度 | 文档的 AI 标签和摘要 |
| 全文检索 | 关键词搜索、按类型/状态/分类/时间筛选、分页 | 匹配的文档和正文片段 |
| 语义检索 | 文档向量化、自然语言搜索 | 语义相关的文档片段和相似度 |
| RAG 问答 | 基于知识库提问，返回答案和引用片段 | 回答、来源文档、来源片段 |
| 会话历史 | 会话列表、消息列表、重命名、删除 | 历史对话和多轮上下文 |

## 二、推荐的前端页面

不用一次全做，可以先做最小闭环。

| 页面 | 主要内容和交互 |
|---|---|
| `/login`、`/register` | 登录/注册表单，成功后保存 token |
| `/knowledge-bases` | 知识库卡片或列表，新建/改名/删除 |
| `/knowledge-bases/:id/files` | 左侧文件夹树，右侧文件列表，上传、下载、移动、删除 |
| `/documents/:id` | 文件信息、解析按钮、解析状态、AI 分析结果、正文预览、向量化按钮 |
| `/search` | 关键词/语义搜索切换，筛选条件，结果列表，正文片段 |
| `/chat` | 左侧会话列表，右侧消息区，输入问题，展示回答和引用 |

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

## 四、前端联调须知

### 1. 基础约定

- Base URL：`http://localhost:8080`
- 认证：请求头 `Authorization: Bearer <token>`
- 所有接口 HTTP 状态码统一为 200，业务结果看响应体里的 `code`
- 成功：`code = 200`
- 失败：`code` 为 4xx/5xx 业务码，`message` 是原因
- 时间格式：ISO-8601，例如 `2026-11-22T20:00:00`
- 分页结构：`PageResult { total, page, size, pages, records }`

### 2. 异步解析

文档解析推荐走异步：

1. `POST /api/documents/{id}/parse-async` 提交；
2. 前端每 1–2 秒轮询 `GET /api/documents/{id}/parse-status`；
3. 直到 `processStatus` 为 `SUCCESS` 或 `FAILED`；
4. `FAILED` 时展示 `errorMessage`，可以重新提交。

### 3. AI 接口耗时

- `POST /api/documents/{id}/analyze` 和 `POST /api/chat/ask` 是同步调用大模型；
- 通常几秒到几十秒；
- 前端请求超时建议不低于 60 秒；
- 点击后加 loading，避免重复提交。

### 4. 文件上传

`multipart/form-data`：

| 字段 | 说明 |
|---|---|
| knowledgeBaseId | 必填 |
| folderId | 可选，不传使用默认文件夹 |
| file | 单文件上传 |
| files | 批量上传 |

- 单文件最大 20MB；
- 单请求最大 100MB；
- 支持 PDF、DOCX、TXT、MD。

### 5. 两种搜索

| 类型 | 用途 | 特点 |
|---|---|---|
| 全文检索 | 找“包含某个词”的文档 | 关键词匹配，快，有正文片段 |
| 语义检索 | 找“意思相关”的文档 | 需要先向量化，按相似度排序 |

前端可以做成一个搜索框 + “关键词/语义”切换。

### 6. 会话

- 第一次提问不传 `sessionId`，后端自动新建会话；
- 后续追问把返回的 `sessionId` 传回来，后端会带最近几轮上下文；
- 会话列表按更新时间倒序；
- 消息列表按时间正序；
- 删除会话会同时删除消息。

### 7. CORS

当前后端**没有配置 CORS**。前端本地开发建议：

- 用 Vite/Webpack 的 devServer 代理，把 `/api` 转发到 `http://localhost:8080`；
- 或者后端加 CORS 配置。

## 五、本地运行

### 1. 环境

- JDK 21
- Maven
- MySQL 8.4
- 可选：Ollama（本地向量模型）
- 可选：DeepSeek/OpenAI 等大模型 key（AI 分析、RAG 问答）

### 2. 数据库

数据库名：`mindspace`

新库执行：

```text
sql/01_schema.sql
sql/02_data.sql
```

已有库升级（已经建过就跳过）：

```text
sql/03_fulltext.sql
sql/04_vector.sql
```

默认数据库账号在 `application-dev.yaml` 里，当前是 `root / 123456`。

### 3. 启动

```powershell
cd E:\code_for_java\MindSpace
$env:JAVA_HOME = 'E:\jdk21'
$env:PATH = "E:\jdk21\bin;$env:PATH"
mvn spring-boot:run
```

默认使用 `dev` profile，端口 `8080`，**本地不需要配置环境变量**。

打包：

```powershell
mvn clean package
java -jar target\MindSpace-0.0.1-SNAPSHOT.jar
```

### 4. 向量模型

默认使用本地 Ollama 的 `bge-m3`：

```powershell
ollama pull bge-m3
```

默认地址：`http://localhost:11434/v1`，默认模型：`bge-m3`。

### 5. AI key

AI 分析、RAG 问答需要真实大模型 key。没有 key 时：

- 项目仍能启动；
- 相关接口返回 `3004 AI 服务未配置`；
- 其他功能不受影响。

可以设置 `AI_API_KEY` 环境变量，或者放到被 Git 忽略的 `application-local.yaml`。

### 6. 测试账号

```text
邮箱：test@mindspace.com
密码：123456
```

## 六、技术栈

| 层 | 技术 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5.16 |
| 安全 | Spring Security + JWT + BCrypt |
| 数据库 | MySQL 8.4 |
| ORM | MyBatis-Plus 3.5.18 |
| 文档解析 | Apache Tika 3.3.1 |
| 大模型 | Spring AI 1.1.8，OpenAI 兼容接口 |
| 向量模型 | Ollama + bge-m3 |
| 全文检索 | MySQL FULLTEXT + ngram |
| 构建 | Maven |

## 七、项目亮点

- JWT 登录和统一鉴权；
- 知识库、文件夹、文档的完整增删改查；
- SHA-256 文件去重和本地存储；
- Tika 多格式正文解析；
- 异步解析线程池 + 状态轮询 + 启动恢复；
- AI 摘要、关键词、分类；
- MySQL FULLTEXT 全文检索；
- 本地 embedding + 向量入库 + 余弦相似度语义检索；
- RAG 问答，返回答案和引用片段；
- 多轮会话历史管理。

## 八、当前限制

- 前端未实现；
- Docker 未做；
- 未做 OCR，扫描版 PDF 可能解析失败；
- AI 分析、RAG 问答是同步接口，没有流式输出；
- 异步解析使用单机内存线程池，应用重启会把 `PROCESSING` 文档标记为 `FAILED`；
- 语义检索在 Java 内存里计算余弦相似度，单次最多加载 5000 个 chunk；
- 全文检索基于 MySQL ngram，个别英文词可能不命中，代码里用 `LOCATE` 做了兜底；
- 还没有文件分享、多人协作、收藏、标签等扩展功能。

## 九、文档

- 接口文档：[API.md](API.md)
- 学习笔记：[LEARNING_NOTES.md](LEARNING_NOTES.md)
