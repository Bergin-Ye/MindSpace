# MindSpace 个人知识库

MindSpace 是一个面向个人学习的知识库项目，后端使用 Spring Boot，前端使用 Vue 3。它把文件管理、文档解析、AI 理解、全文检索、语义检索、RAG 问答和会话历史串成了一条完整链路。

前后端在同一个仓库：`src/` 是后端，`frontend/` 是前端。接口契约见 [API.md](API.md)，前端开发说明见 [FRONTEND_GUIDE.md](FRONTEND_GUIDE.md)。

## 一、项目功能

| 模块 | 功能 | 状态 |
|---|---|---|
| 账号 | 注册、登录、JWT 鉴权、当前用户 | 已完成 |
| 用户 | 修改资料、修改密码 | 已完成 |
| 知识库 | 新建、列表、详情、修改、删除 | 已完成 |
| 文件夹 | 平铺列表、树结构、新建、重命名、移动、删除 | 已完成 |
| 文件上传 | 单文件、批量上传、SHA-256 去重、本地存储 | 已完成 |
| 文件管理 | 分页、详情、重命名、移动、删除、下载 | 已完成 |
| 文档解析 | Tika 解析 PDF/DOCX/TXT/MD | 已完成 |
| 异步解析 | 线程池、状态轮询、启动恢复 | 已完成 |
| AI 分析 | 摘要、关键词、分类、置信度 | 已完成 |
| 全文检索 | MySQL FULLTEXT + ngram，支持组合筛选 | 已完成 |
| 向量化 | 文本分块、Ollama bge-m3、向量入库 | 已完成 |
| 语义检索 | 余弦相似度、topK 相关片段 | 已完成 |
| RAG 问答 | 基于知识库检索回答，返回引用片段 | 已完成 |
| 会话历史 | 会话列表、消息分页、重命名、删除 | 已完成 |

## 二、核心链路

```text
注册/登录
  → 建知识库
  → 建文件夹
  → 上传文档
  → Tika 解析正文
  → AI 摘要 / 关键词 / 分类
  → 全文检索
  → 文档分块向量化
  → 语义检索
  → RAG 智能问答
  → 会话历史
```

## 三、技术栈

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
| 前端 | Vue 3 + Vite + Vue Router + Pinia（JavaScript） |

## 四、项目结构

```text
src/main/java/com/mindspace
├── ai                  # AI / embedding 客户端
├── common              # 统一响应、异常、配置
├── controller          # REST 接口
├── document            # 文件下载、文本切块等
├── dto                 # 请求参数
├── entity              # 数据库实体
├── mapper              # MyBatis-Plus Mapper
├── security            # JWT 与安全配置
├── service             # 业务接口与实现
├── task                # 启动恢复任务
└── vo                  # 返回给前端的视图对象

sql/
├── 01_schema.sql       # 建表
├── 02_data.sql         # 基础数据
├── 03_fulltext.sql     # 已有库补全文索引
└── 04_vector.sql       # 已有库补向量表

frontend/
├── src/
│   ├── api             # 接口封装
│   ├── components      # 通用组件（图标全部为内联 SVG）
│   ├── layouts         # 应用壳层与登录壳层
│   ├── router          # 路由与登录守卫
│   ├── stores          # Pinia 状态
│   ├── styles          # 设计令牌与全局样式
│   └── views           # 页面
└── vite.config.js      # /api 代理到 8080

API.md                  # 接口文档
FRONTEND_GUIDE.md       # 前端开发说明
LEARNING_NOTES.md       # 学习笔记
```

## 五、设计要点

- 统一返回 `Result { code, message, data }`，HTTP 状态码统一 200；
- 业务失败抛 `BusinessException`，由全局异常处理器转换；
- 错误码按模块分段：1xxx 用户、2xxx 知识库/文件夹/文件、3xxx AI/向量/RAG；
- 所有查询都带 `userId`，保证用户数据隔离；
- 实体数值字段使用包装类型，避免部分更新写 0；
- 逻辑删除使用 `@TableLogic`；
- 对外返回 VO，不直接返回 Entity；
- 分页统一使用 `PageResult { total, page, size, pages, records }`；
- 异步解析使用线程池，状态通过轮询接口查询；
- 全文检索使用 MySQL FULLTEXT；
- 向量以 JSON 字符串存 MySQL，Java 内计算余弦相似度；
- 解析、AI、embedding 等外部调用方法不包事务，避免失败状态被回滚。

## 六、本地运行

### 1. 环境要求

- JDK 21
- Maven
- MySQL 8.4
- 可选：Ollama（语义检索需要的本地向量模型）
- 可选：DeepSeek / OpenAI 等大模型 key（AI 分析、RAG 问答）

### 2. 数据库

数据库名：`mindspace`

新库执行：

```text
sql/01_schema.sql
sql/02_data.sql
```

已有库升级（已经执行过就跳过）：

```text
sql/03_fulltext.sql
sql/04_vector.sql
```

默认数据库账号在 `application-dev.yaml` 中，当前为 `root / 123456`。

### 3. 启动

```powershell
cd E:\code_for_java\MindSpace
$env:JAVA_HOME = 'E:\jdk21'
$env:PATH = "E:\jdk21\bin;$env:PATH"
mvn spring-boot:run
```

默认使用 `dev` profile，端口 `8080`，本地不需要配置环境变量。

打包运行：

```powershell
mvn clean package
java -jar target\MindSpace-0.0.1-SNAPSHOT.jar
```

### 4. 向量模型

语义检索默认使用本地 Ollama 的 `bge-m3`：

```powershell
ollama pull bge-m3
```

默认地址：`http://localhost:11434/v1`，默认模型：`bge-m3`。

### 5. AI key

AI 分析、RAG 问答需要真实大模型 key。没有 key 时：

- 项目仍能正常启动；
- AI 相关接口返回 `3004 AI 服务未配置`；
- 其他功能不受影响。

可以设置 `AI_API_KEY` 环境变量，或放到被 Git 忽略的 `application-local.yaml`。

### 6. 测试账号

```text
邮箱：test@mindspace.com
密码：123456
```

### 7. 前端

前端在 `frontend/`，使用 Vue 3 + JavaScript、Vue Router、Pinia、axios，不依赖 UI 组件库和图标库，图标全部是内联 SVG。

```powershell
cd E:\code_for_java\MindSpace\frontend
npm install
npm run dev
```

打开 `http://localhost:5173`。开发服务器把 `/api` 代理到 `http://localhost:8080`，后端不需要额外配置 CORS。

生产构建：

```powershell
npm run build
```

产物在 `frontend/dist/`，可交给 Nginx 等静态服务器托管，并把 `/api` 反向代理到后端。

## 七、配置与日志

- `application.yaml`：公共配置；
- `application-dev.yaml`：本地数据库、JWT、上传目录、DEBUG 日志；
- `application-prod.yaml`：生产配置，敏感值通过环境变量注入；
- `logback-spring.xml`：控制台 + 滚动文件日志。

本地默认 dev，不需要环境变量。生产使用：

```powershell
java -jar target\MindSpace-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 八、文档

- 接口文档：[API.md](API.md)
- 前端开发说明：[FRONTEND_GUIDE.md](FRONTEND_GUIDE.md)
- 学习笔记：[LEARNING_NOTES.md](LEARNING_NOTES.md)

## 九、当前限制

- 前端已完成，代码在 `frontend/`；
- Docker 未做；
- 未做 OCR，扫描版 PDF 可能解析失败；
- AI 分析和 RAG 问答是同步接口，没有流式输出；
- 异步解析使用单机内存线程池，应用重启会把 `PROCESSING` 文档标记为 `FAILED`；
- 语义检索在 Java 内存中计算余弦相似度，单次最多加载 5000 个 chunk；
- 全文检索基于 MySQL ngram，个别英文词可能不命中，代码中用 `LOCATE` 做了兜底；
- 未实现文件分享、多人协作、收藏、标签等功能。
