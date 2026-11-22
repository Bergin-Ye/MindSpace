# MindSpace 接口文档

| 项目 | 内容 |
|---|---|
| 文档版本 | v0.2 |
| 更新时间 | 2026-11-17 |
| Base URL | `http://localhost:8080` |
| 字符集 | UTF-8 |
| 请求/响应格式 | `application/json` |
| 认证方式 | JWT Bearer Token |

---

## 1. 通用约定

### 1.1 请求头

| Header | 是否必填 | 说明 |
|---|---|---|
| Content-Type | POST/PUT 必填 | 固定 `application/json` |
| Authorization | 需要登录的接口必填 | 格式：`Bearer <token>` |

### 1.2 统一响应结构

所有接口使用同一层包装：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| code | Integer | 是 | 业务状态码，200 表示成功 |
| message | String | 是 | 面向用户的提示信息 |
| data | Object/Array/null | 是 | 业务数据，失败时通常为 null |

### 1.3 HTTP 状态码约定

- 第一版所有业务响应统一返回 **HTTP 200**。
- 业务成功与否以响应体中的 `code` 为准。
- 只有网关、网络、服务器崩溃等基础设施问题才会出现非 200 的 HTTP 状态。

### 1.4 字段与格式约定

| 类型 | 约定 |
|---|---|
| 字段命名 | camelCase，如 `createTime`、`expiresIn` |
| 时间 | ISO-8601 字符串，如 `2026-11-17T16:50:14` |
| 时长 | 毫秒，如 `expiresIn = 86400000` 表示 24 小时 |
| 密码 | 永远不在响应中出现，数据库只存 BCrypt 密文 |

### 1.5 认证约定

- 登录成功后返回 JWT。
- 后续请求携带：`Authorization: Bearer <token>`。
- 默认有效期 24 小时。
- token 缺失、格式错误、签名错误、已过期，统一返回：

```json
{"code": 401, "message": "未登录或登录已过期", "data": null}
```

---

## 2. 接口总览

| 编号 | 方法 | 路径 | 功能 | 认证 |
|---|---|---|---|---|
| AUTH-01 | POST | `/api/auth/register` | 用户注册 | 否 |
| AUTH-02 | POST | `/api/auth/login` | 用户登录 | 否 |
| AUTH-03 | GET | `/api/auth/me` | 获取当前用户信息 | 是 |
| USER-01 | PUT | `/api/user/profile` | 修改个人资料 | 是 |
| USER-02 | PUT | `/api/user/password` | 修改密码 | 是 |
| KB-01 | POST | `/api/knowledge-bases` | 创建知识库 | 是 |
| KB-02 | GET | `/api/knowledge-bases` | 查询知识库列表 | 是 |
| KB-03 | GET | `/api/knowledge-bases/{id}` | 查询知识库详情 | 是 |
| KB-04 | PUT | `/api/knowledge-bases/{id}` | 修改知识库 | 是 |
| KB-05 | DELETE | `/api/knowledge-bases/{id}` | 删除知识库 | 是 |
| FO-01 | POST | `/api/folders` | 创建文件夹 | 是 |
| FO-02 | GET | `/api/folders?knowledgeBaseId={id}` | 文件夹列表 | 是 |
| FO-03 | GET | `/api/folders/tree?knowledgeBaseId={id}` | 文件夹树 | 是 |
| FO-04 | PUT | `/api/folders/{id}` | 重命名文件夹 | 是 |
| FO-05 | DELETE | `/api/folders/{id}` | 删除文件夹 | 是 |
| FO-06 | PUT | `/api/folders/{id}/move` | 移动文件夹 | 是 |
| DOC-01 | POST | `/api/documents/upload` | 单文件上传 | 是 |
| DOC-02 | POST | `/api/documents/batch-upload` | 批量上传 | 是 |
| DOC-03 | GET | `/api/documents` | 文件分页列表 | 是 |
| DOC-04 | GET | `/api/documents/{id}` | 文件详情 | 是 |
| DOC-05 | PUT | `/api/documents/{id}` | 重命名文件 | 是 |
| DOC-06 | PUT | `/api/documents/{id}/move` | 移动文件 | 是 |
| DOC-07 | DELETE | `/api/documents/{id}` | 删除文件 | 是 |
| DOC-08 | GET | `/api/documents/{id}/download` | 下载原文件 | 是 |
| DOC-09 | POST | `/api/documents/{id}/parse` | 解析文档正文 | 是 |
| DOC-10 | POST | `/api/documents/{id}/parse-async` | 提交异步解析 | 是 |
| DOC-11 | GET | `/api/documents/{id}/parse-status` | 查询解析状态 | 是 |
| DOC-12 | GET | `/api/documents/search` | 全文检索 / 高级搜索 | 是 |
| DOC-13 | POST | `/api/documents/{id}/embed` | 文档分块并向量化入库 | 是 |
| DOC-14 | GET | `/api/documents/vector-search` | 向量语义检索 | 是 |
| AI-01 | POST | `/api/documents/{id}/analyze` | AI 摘要、关键词、分类 | 是 |
| CHAT-01 | POST | `/api/chat/ask` | RAG 智能问答 | 是 |
| CHAT-02 | GET | `/api/chat/sessions` | 会话列表 | 是 |
| CHAT-03 | GET | `/api/chat/sessions/{id}/messages` | 会话消息列表 | 是 |
| CHAT-04 | PUT | `/api/chat/sessions/{id}` | 重命名会话 | 是 |
| CHAT-05 | DELETE | `/api/chat/sessions/{id}` | 删除会话 | 是 |

---

## 3. AUTH-01 用户注册

### 3.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/auth/register` |
| 认证 | 不需要 |
| Content-Type | `application/json` |

### 3.2 请求参数

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| username | String | 是 | 3—50 字符，全局唯一 | 用户名 |
| email | String | 是 | 邮箱格式，最长 100，全局唯一 | 登录账号 |
| password | String | 是 | 6—50 字符 | 明文传输，服务端 BCrypt 加密后存储 |

### 3.3 请求示例

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "123456"
}
```

### 3.4 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 3.5 失败场景

| 触发条件 | code | message |
|---|---|---|
| 用户名为空或长度不合法 | 400 | 具体校验提示 |
| 邮箱为空、格式错误或超长 | 400 | 具体校验提示 |
| 密码为空或长度不合法 | 400 | 具体校验提示 |
| 用户名已存在 | 1001 | 用户名已存在 |
| 邮箱已被注册 | 1002 | 邮箱已被注册 |

---

## 4. AUTH-02 用户登录

### 4.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/auth/login` |
| 认证 | 不需要 |
| Content-Type | `application/json` |

### 4.2 请求参数

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| email | String | 是 | 邮箱格式 | 注册时使用的邮箱 |
| password | String | 是 | 非空 | 注册时使用的密码 |

### 4.3 请求示例

```json
{
  "email": "test@mindspace.com",
  "password": "123456"
}
```

### 4.4 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzM4NCJ9.xxx.yyy",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "test",
      "email": "test@mindspace.com",
      "avatar": null,
      "createTime": "2026-11-17T16:50:14"
    }
  }
}
```

### 4.5 data 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| token | String | JWT 字符串，前端必须保存 |
| tokenType | String | 固定 `Bearer`，用于拼 Authorization |
| expiresIn | Long | 有效期，单位毫秒 |
| user | Object | 当前用户公开信息，结构见 4.6 |

### 4.6 user 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 用户主键 |
| username | String | 用户名 |
| email | String | 邮箱 |
| avatar | String/null | 头像地址 |
| createTime | String | 注册时间 |

### 4.7 失败场景

| 触发条件 | code | message |
|---|---|---|
| 邮箱或密码为空 | 400 | 具体校验提示 |
| 邮箱格式错误 | 400 | 邮箱格式不正确 |
| 邮箱不存在或密码错误 | 1003 | 邮箱或密码错误 |

安全说明：不区分“用户不存在”和“密码错误”，避免被用来枚举已注册邮箱。

---

## 5. AUTH-03 获取当前用户

### 5.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/auth/me` |
| 认证 | 必须 |
| 请求头 | `Authorization: Bearer <token>` |

### 5.2 请求参数

无。

### 5.3 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "test",
    "email": "test@mindspace.com",
    "avatar": null,
    "createTime": "2026-11-17T16:50:14"
  }
}
```

### 5.4 失败场景

| 触发条件 | code | message |
|---|---|---|
| 未携带 Authorization | 401 | 未登录或登录已过期 |
| Bearer 格式错误 | 401 | 未登录或登录已过期 |
| token 签名错误 | 401 | 未登录或登录已过期 |
| token 已过期 | 401 | 未登录或登录已过期 |
| token 中的用户已被删除 | 1004 | 用户不存在 |

---

## 6. USER-01 修改个人资料

### 6.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | PUT |
| 路径 | `/api/user/profile` |
| 认证 | 必须 |
| 请求头 | `Authorization: Bearer <token>`、`Content-Type: application/json` |

### 6.2 请求参数

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| username | String | 是 | 3—50 字符，全局唯一 | 新的用户名 |
| avatar | String | 否 | 最长 500 | 头像地址；不传表示不修改，传空串表示清空 |

说明：

- 第一版不支持修改邮箱，邮箱是登录账号，修改需要额外的验证流程。
- 头像先接收 URL 字符串，等文件上传功能完成后再接上传接口返回的地址。

### 6.3 请求示例

```json
{
  "username": "test_new",
  "avatar": "https://example.com/avatar/test.png"
}
```

### 6.4 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "test_new",
    "email": "test@mindspace.com",
    "avatar": "https://example.com/avatar/test.png",
    "createTime": "2026-11-17T16:50:14"
  }
}
```

### 6.5 失败场景

| 触发条件 | code | message |
|---|---|---|
| 用户名为空或长度不合法 | 400 | 具体校验提示 |
| 用户名已被其他用户占用 | 1001 | 用户名已存在 |
| token 中用户已被删除 | 1004 | 用户不存在 |
| 未登录 | 401 | 未登录或登录已过期 |

---

## 7. USER-02 修改密码

### 7.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | PUT |
| 路径 | `/api/user/password` |
| 认证 | 必须 |
| 请求头 | `Authorization: Bearer <token>`、`Content-Type: application/json` |

### 7.2 请求参数

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| oldPassword | String | 是 | 非空 | 当前使用的密码 |
| newPassword | String | 是 | 6—50 字符 | 新密码，不能与原密码相同 |

### 7.3 请求示例

```json
{
  "oldPassword": "123456",
  "newPassword": "abcdef"
}
```

### 7.4 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 7.5 失败场景

| 触发条件 | code | message |
|---|---|---|
| 原密码为空 | 400 | 原密码不能为空 |
| 新密码长度不合法 | 400 | 新密码长度必须在6到50之间 |
| 原密码错误 | 1005 | 原密码错误 |
| 新密码与原密码相同 | 1006 | 新密码不能与原密码相同 |
| 未登录 | 401 | 未登录或登录已过期 |

说明：第一版修改密码后旧 token 不会立即失效，会继续有效到过期时间。后续需要时再引入 token 黑名单或版本号机制。

---

## 8. KB-01 创建知识库

### 8.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/knowledge-bases` |
| 认证 | 必须 |
| 请求头 | `Authorization: Bearer <token>`、`Content-Type: application/json` |

### 8.2 请求参数

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| name | String | 是 | 1—100 字符 | 知识库名称 |
| description | String | 否 | 最长 500 | 描述 |

### 8.3 请求示例

```json
{
  "name": "Java 学习",
  "description": "Java 相关文档"
}
```

### 8.4 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "name": "Java 学习",
    "description": "Java 相关文档",
    "createTime": "2026-11-18T14:30:00",
    "updateTime": "2026-11-18T14:30:00"
  }
}
```

### 8.5 失败场景

| 触发条件 | code | message |
|---|---|---|
| 名称为空或超长 | 400 | 具体校验提示 |
| 未登录 | 401 | 未登录或登录已过期 |

---

## 9. KB-02 查询知识库列表

### 9.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/knowledge-bases` |
| 认证 | 必须 |
| 请求头 | `Authorization: Bearer <token>` |

### 9.2 请求参数

无。只返回当前登录用户自己的知识库。

### 9.3 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "我的知识库",
      "description": "注册时自动创建的默认知识库",
      "createTime": "2026-11-17T16:50:14",
      "updateTime": "2026-11-17T16:50:14"
    },
    {
      "id": 2,
      "name": "Java 学习",
      "description": "Java 相关文档",
      "createTime": "2026-11-18T14:30:00",
      "updateTime": "2026-11-18T14:30:00"
    }
  ]
}
```

说明：文件数量 `documentCount` 等文件功能完成后补充，当前版本不返回。

---

## 10. KB-03 查询知识库详情

### 10.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/knowledge-bases/{id}` |
| 认证 | 必须 |
| 请求头 | `Authorization: Bearer <token>` |

### 10.2 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "name": "Java 学习",
    "description": "Java 相关文档",
    "createTime": "2026-11-18T14:30:00",
    "updateTime": "2026-11-18T14:30:00"
  }
}
```

### 10.3 失败场景

| 触发条件 | code | message |
|---|---|---|
| id 不存在 | 2001 | 知识库不存在 |
| id 属于其他用户 | 2001 | 知识库不存在 |
| 未登录 | 401 | 未登录或登录已过期 |

说明：别人的知识库也返回“知识库不存在”，不暴露它是否存在。

---

## 11. KB-04 修改知识库

### 11.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | PUT |
| 路径 | `/api/knowledge-bases/{id}` |
| 认证 | 必须 |
| 请求头 | `Authorization: Bearer <token>`、`Content-Type: application/json` |

### 11.2 请求参数

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| name | String | 是 | 1—100 字符 | 新名称 |
| description | String | 否 | 最长 500 | 新描述 |

### 11.3 请求示例

```json
{
  "name": "Java 进阶",
  "description": "Spring Boot 与 MySQL"
}
```

### 11.4 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "name": "Java 进阶",
    "description": "Spring Boot 与 MySQL",
    "createTime": "2026-11-18T14:30:00",
    "updateTime": "2026-11-18T14:35:00"
  }
}
```

### 11.5 失败场景

| 触发条件 | code | message |
|---|---|---|
| 名称为空或超长 | 400 | 具体校验提示 |
| id 不存在或不属于当前用户 | 2001 | 知识库不存在 |
| 未登录 | 401 | 未登录或登录已过期 |

---

## 12. KB-05 删除知识库

### 12.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | DELETE |
| 路径 | `/api/knowledge-bases/{id}` |
| 认证 | 必须 |
| 请求头 | `Authorization: Bearer <token>` |

### 12.2 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 12.3 失败场景

| 触发条件 | code | message |
|---|---|---|
| id 不存在或不属于当前用户 | 2001 | 知识库不存在 |
| 删除后一个知识库都不剩 | 2002 | 至少保留一个知识库 |
| 未登录 | 401 | 未登录或登录已过期 |

说明：

- 采用逻辑删除，数据库记录保留，`deleted` 置 1。
- 知识库下的文件、向量数据的级联清理在文件与 RAG 功能完成后补充。

---

## 13. 错误码总表

| code | 含义 | 典型来源 |
|---|---|---|
| 200 | 成功 | 正常返回 |
| 400 | 参数校验失败 | `@Valid` + GlobalExceptionHandler |
| 401 | 未登录或 token 失效 | Spring Security EntryPoint |
| 403 | 无权访问 | 业务权限判断 |
| 404 | 资源不存在 | 业务查询 |
| 500 | 系统错误 | 未捕获异常兜底 |
| 1001 | 用户名已存在 | 注册 |
| 1002 | 邮箱已被注册 | 注册 |
| 1003 | 邮箱或密码错误 | 登录 |
| 1004 | 用户不存在 | 当前用户查询、用户操作 |
| 1005 | 原密码错误 | 修改密码 |
| 1006 | 新密码与原密码相同 | 修改密码 |
| 2001 | 知识库不存在 | 知识库查询、修改、删除 |
| 2002 | 至少保留一个知识库 | 删除知识库 |
| 2003 | 文件夹不存在 | 文件夹查询、修改、删除、移动 |
| 2004 | 默认文件夹不能删除 | 删除文件夹 |
| 2005 | 请先删除或移动子文件夹 | 删除文件夹 |
| 2006 | 父文件夹不存在或不属于当前知识库 | 创建、移动文件夹 |
| 2007 | 不能移动到自身或其子文件夹 | 移动文件夹 |
| 2008 | 不支持的文件类型 | 文件上传 |
| 2009 | 文件大小超过限制 | 文件上传 |
| 2010 | 文件内容为空 | 文件上传 |
| 2011 | 文件已存在 | 文件上传 |
| 2012 | 文件保存失败 | 文件上传 |
| 2013 | 文件不存在 | 文件查询、删除、下载 |
| 2014 | 文件解析失败 | 文档解析 |
| 2015 | 当前状态不允许解析 | 文档解析 |
| 2016 | 解析任务提交失败 | 异步解析 |
| 3001 | AI 服务调用失败 | AI 分析 |
| 3002 | AI 返回内容格式错误 | AI 分析 |
| 3003 | 文档尚未解析 | AI 分析 |
| 3004 | AI 服务未配置 | AI 分析 |
| 3005 | 向量化服务调用失败 | 向量化 |
| 3006 | 向量化返回格式错误 | 向量化 |
| 3007 | 向量化服务未配置 | 向量化 |
| 3008 | 会话不存在 | RAG 问答 |

错误码分段规则：

- 1xxx：用户与认证
- 2xxx：知识库、文件夹、文件
- 3xxx：AI 分析、RAG、聊天
- 9xxx：预留

---

## 14. 测试账号

由 `sql/02_data.sql` 初始化：

| 邮箱 | 密码 |
|---|---|
| test@mindspace.com | 123456 |

---

## 15. 文件夹接口（FO-01 ~ FO-06）

所有文件夹接口都必须登录，并且只能操作自己知识库下的文件夹。

### 15.1 FO-01 创建文件夹

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/folders` |
| 认证 | 必须 |

请求参数：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| knowledgeBaseId | Long | 是 | 必须是自己的知识库 | 所属知识库 |
| parentId | Long | 否 | 0 或自己的文件夹 id | 父文件夹，不传或 0 表示根目录 |
| name | String | 是 | 1—100 字符 | 文件夹名称 |

请求示例：

```json
{
  "knowledgeBaseId": 1,
  "parentId": 0,
  "name": "Java 基础"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "knowledgeBaseId": 1,
    "parentId": 0,
    "name": "Java 基础",
    "isDefault": 0,
    "createTime": "2026-11-18T17:00:00",
    "updateTime": "2026-11-18T17:00:00"
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 名称为空或超长 | 400 | 具体校验提示 |
| 知识库不存在或不属于自己 | 2001 | 知识库不存在 |
| 父文件夹不存在或不属于该知识库 | 2006 | 父文件夹不存在或不属于当前知识库 |
| 未登录 | 401 | 未登录或登录已过期 |

### 15.2 FO-02 文件夹列表

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/folders?knowledgeBaseId=1` |
| 认证 | 必须 |

成功响应：data 为 FolderVO 数组，按 id 升序。

### 15.3 FO-03 文件夹树

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/folders/tree?knowledgeBaseId=1` |
| 认证 | 必须 |

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "knowledgeBaseId": 1,
      "parentId": 0,
      "name": "未分类",
      "isDefault": 1,
      "children": []
    },
    {
      "id": 2,
      "knowledgeBaseId": 1,
      "parentId": 0,
      "name": "Java 基础",
      "isDefault": 0,
      "children": [
        {
          "id": 3,
          "knowledgeBaseId": 1,
          "parentId": 2,
          "name": "集合",
          "isDefault": 0,
          "children": []
        }
      ]
    }
  ]
}
```

### 15.4 FO-04 重命名文件夹

| 项 | 内容 |
|---|---|
| 方法 | PUT |
| 路径 | `/api/folders/{id}` |
| 认证 | 必须 |

请求示例：

```json
{
  "name": "Java 基础进阶"
}
```

成功响应：data 为更新后的 FolderVO。

### 15.5 FO-05 删除文件夹

| 项 | 内容 |
|---|---|
| 方法 | DELETE |
| 路径 | `/api/folders/{id}` |
| 认证 | 必须 |

失败场景：

| 条件 | code | message |
|---|---|---|
| 文件夹不存在或不属于自己 | 2003 | 文件夹不存在 |
| 默认“未分类”文件夹 | 2004 | 默认文件夹不能删除 |
| 还有子文件夹 | 2005 | 请先删除或移动子文件夹 |
| 未登录 | 401 | 未登录或登录已过期 |

说明：文件夹下的文件迁移到默认文件夹、向量清理，在文件与 RAG 功能完成后补充。

### 15.6 FO-06 移动文件夹

| 项 | 内容 |
|---|---|
| 方法 | PUT |
| 路径 | `/api/folders/{id}/move` |
| 认证 | 必须 |

请求示例：

```json
{
  "parentId": 2
}
```

`parentId = 0` 表示移动到根目录。

失败场景：

| 条件 | code | message |
|---|---|---|
| 文件夹不存在或不属于自己 | 2003 | 文件夹不存在 |
| 目标父文件夹不存在或不属于该知识库 | 2006 | 父文件夹不存在或不属于当前知识库 |
| 移动到自身 | 2007 | 不能移动到自身或其子文件夹 |
| 移动到自己的后代 | 2007 | 不能移动到自身或其子文件夹 |
| 未登录 | 401 | 未登录或登录已过期 |

---

## 16. 文件上传接口（DOC-01 ~ DOC-02）

### 16.1 DOC-01 单文件上传

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/documents/upload` |
| 认证 | 必须 |
| Content-Type | `multipart/form-data` |

form-data 参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| knowledgeBaseId | Long | 是 | 目标知识库，必须是自己的 |
| folderId | Long | 否 | 目标文件夹；不传则放入该知识库的“未分类” |
| file | File | 是 | 支持 pdf、docx、txt、md，最大 20MB |

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "knowledgeBaseId": 1,
    "folderId": 1,
    "originalName": "Spring Boot 指南.pdf",
    "fileType": "pdf",
    "fileSize": 102400,
    "processStatus": "UPLOADED",
    "createTime": "2026-11-19T10:00:00"
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 文件为空 | 2010 | 文件内容为空 |
| 超过 20MB | 2009 | 文件大小超过限制 |
| 扩展名不支持 | 2008 | 不支持的文件类型 |
| 同一知识库下已有相同文件 | 2011 | 文件已存在 |
| 文件保存失败 | 2012 | 文件保存失败 |
| 知识库不存在或不属于自己 | 2001 | 知识库不存在 |
| 文件夹不存在或不属于该知识库 | 2003 | 文件夹不存在 |
| 未登录 | 401 | 未登录或登录已过期 |

说明：上传成功后 `process_status` 为 `UPLOADED`，需要通过解析接口和 AI 分析接口分别处理。

### 16.2 DOC-02 批量上传

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/documents/batch-upload` |
| 认证 | 必须 |
| Content-Type | `multipart/form-data` |

form-data 参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| knowledgeBaseId | Long | 是 | 目标知识库 |
| folderId | Long | 否 | 目标文件夹，不传用“未分类” |
| files | File[] | 是 | 多个文件，字段名统一为 `files` |

成功响应（允许部分成功）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "successCount": 1,
    "failCount": 1,
    "items": [
      {
        "originalName": "a.pdf",
        "success": true,
        "documentId": 1,
        "code": 200,
        "message": "success"
      },
      {
        "originalName": "b.zip",
        "success": false,
        "documentId": null,
        "code": 2008,
        "message": "不支持的文件类型"
      }
    ]
  }
}
```

---

## 17. 文件管理接口（DOC-03 ~ DOC-14）

### 17.1 DOC-03 文件分页列表

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/documents` |
| 认证 | 必须 |

查询参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| knowledgeBaseId | Long | 是 | — | 知识库 id |
| folderId | Long | 否 | — | 按文件夹筛选 |
| keyword | String | 否 | — | 只按原始文件名模糊搜索；全文检索请用 DOC-12 |
| processStatus | String | 否 | — | UPLOADED / PROCESSING / SUCCESS / FAILED |
| page | long | 否 | 1 | 页码，从 1 开始 |
| size | long | 否 | 10 | 每页条数，最大 100 |

请求示例：

```
GET /api/documents?knowledgeBaseId=1&folderId=1&keyword=spring&processStatus=UPLOADED&page=1&size=10
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 25,
    "page": 1,
    "size": 10,
    "pages": 3,
    "records": [
      {
        "id": 1,
        "knowledgeBaseId": 1,
        "folderId": 1,
        "originalName": "Spring Boot 指南.pdf",
        "fileType": "pdf",
        "fileSize": 102400,
        "processStatus": "UPLOADED",
        "createTime": "2026-11-19T16:55:03"
      }
    ]
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 知识库不存在或不属于自己 | 2001 | 知识库不存在 |
| 指定的 folderId 不属于该知识库 | 2003 | 文件夹不存在 |
| 未登录 | 401 | 未登录或登录已过期 |

### 17.2 DOC-04 文件详情

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/documents/{id}` |
| 认证 | 必须 |

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "knowledgeBaseId": 1,
    "folderId": 1,
    "originalName": "Spring Boot 指南.pdf",
    "fileType": "pdf",
    "fileSize": 102400,
    "processStatus": "UPLOADED",
    "summary": null,
    "keywords": null,
    "aiCategory": null,
    "confidence": null,
    "errorMessage": null,
    "createTime": "2026-11-19T16:55:03",
    "updateTime": "2026-11-19T16:55:03"
  }
}
```

失败：文件不存在或不属于当前用户 → 2013。

### 17.3 DOC-05 重命名文件

| 项 | 内容 |
|---|---|
| 方法 | PUT |
| 路径 | `/api/documents/{id}` |
| 认证 | 必须 |
| Content-Type | `application/json` |

请求示例：

```json
{
  "originalName": "Spring Boot 实战.pdf"
}
```

成功：data 为更新后的 DocumentVO。

失败：名称为空/超长 → 400；文件不存在或不属于自己 → 2013。

### 17.4 DOC-06 移动文件

| 项 | 内容 |
|---|---|
| 方法 | PUT |
| 路径 | `/api/documents/{id}/move` |
| 认证 | 必须 |
| Content-Type | `application/json` |

请求示例：

```json
{
  "folderId": 2
}
```

成功：data 为更新后的 DocumentVO。

失败：

| 条件 | code |
|---|---|
| 文件不存在或不属于自己 | 2013 |
| 目标文件夹不存在或不属于该知识库 | 2003 |

### 17.5 DOC-07 删除文件

| 项 | 内容 |
|---|---|
| 方法 | DELETE |
| 路径 | `/api/documents/{id}` |
| 认证 | 必须 |

成功：`data = null`。

失败：文件不存在或不属于自己 → 2013。

说明：数据库做逻辑删除，同时删除磁盘上的原文件；向量数据清理在 RAG 功能完成后补充。

### 17.6 DOC-08 下载原文件

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/documents/{id}/download` |
| 认证 | 必须 |

成功响应不是 JSON，而是二进制文件流：

```
HTTP 200
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="Spring Boot 指南.pdf"
```

失败：

| 条件 | code | message |
|---|---|---|
| 文件不存在或不属于自己 | 2013 | 文件不存在 |
| 磁盘文件丢失 | 2013 | 文件不存在 |

说明：下载接口直接返回文件流，不使用统一 Result 包装。

### 17.7 DOC-09 解析文档正文

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/documents/{id}/parse` |
| 认证 | 必须 |
| 请求体 | 无 |

说明：同步调用 Apache Tika 提取 PDF / DOCX / TXT / MD 的正文，写入 `extracted_text`。状态流转：

```
UPLOADED / FAILED → PROCESSING → SUCCESS / FAILED
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "knowledgeBaseId": 1,
    "folderId": 1,
    "originalName": "Spring Boot 指南.pdf",
    "fileType": "pdf",
    "fileSize": 102400,
    "processStatus": "SUCCESS",
    "summary": null,
    "keywords": null,
    "aiCategory": null,
    "confidence": null,
    "errorMessage": null,
    "extractedTextLength": 23580,
    "textPreview": "第一章 Spring Boot 简介……",
    "createTime": "2026-11-19T16:55:03",
    "updateTime": "2026-11-19T17:20:11"
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 文件不存在或不属于自己 | 2013 | 文件不存在 |
| 磁盘文件丢失 | 2013 | 文件不存在 |
| 文件类型不支持 | 2008 | 不支持的文件类型 |
| 当前状态不是 UPLOADED/FAILED（解析中或已成功） | 2015 | 当前状态不允许解析 |
| 解析失败 / 未提取到文字 | 2014 | 文件解析失败（message 为具体原因） |

说明：只有 `UPLOADED`、`FAILED` 可以触发解析，`PROCESSING` 和 `SUCCESS` 会返回 2015。解析失败时会把 `processStatus` 置为 `FAILED`，原因写入 `error_message`，可以再次调用本接口重试；正文超过 500 万字符时只保留前 500 万字符。异步解析见 DOC-10 / DOC-11。

---


### 17.8 DOC-10 提交异步解析

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/documents/{id}/parse-async` |
| 认证 | 必须 |
| 请求体 | 无 |

说明：先校验文档归属、类型和磁盘文件，再把状态原子地从 `UPLOADED` / `FAILED` 改成 `PROCESSING`，然后提交线程池，立即返回当前状态。

返回的 `processStatus` 通常是 `PROCESSING`；如果文档很小、解析很快，也可能直接返回 `SUCCESS`。

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 13,
    "processStatus": "PROCESSING",
    "errorMessage": null,
    "extractedTextLength": null,
    "textPreview": null,
    "updateTime": "2026-11-20T19:00:00"
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 文件不存在或不属于自己 | 2013 | 文件不存在 |
| 磁盘文件丢失 | 2013 | 文件不存在 |
| 文件类型不支持 | 2008 | 不支持的文件类型 |
| 当前状态是 PROCESSING / SUCCESS | 2015 | 当前状态不允许解析 |
| 线程池队列已满 | 2016 | 解析任务提交失败 |

说明：队列满时文档会被置为 `FAILED`，可以稍后重新提交。

### 17.9 DOC-11 查询解析状态

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/documents/{id}/parse-status` |
| 认证 | 必须 |
| 请求体 | 无 |

说明：前端在提交异步解析后，每隔 1-2 秒轮询一次，直到 `processStatus` 变成 `SUCCESS` 或 `FAILED`。

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 13,
    "processStatus": "SUCCESS",
    "errorMessage": null,
    "extractedTextLength": 23580,
    "textPreview": "第一章 Spring Boot 简介……",
    "updateTime": "2026-11-20T19:00:03"
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 文件不存在或不属于自己 | 2013 | 文件不存在 |

说明：

- 应用重启时，残留的 `PROCESSING` 文档会被启动恢复任务标记为 `FAILED`，`errorMessage` 为“服务重启，解析任务中断，请重新解析”。
- 同步解析接口 `POST /api/documents/{id}/parse` 仍然保留。
- AI 分析接口仍然是同步的，本次没有改。

### 17.10 DOC-12 全文检索 / 高级搜索

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/documents/search` |
| 认证 | 必须 |

查询参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| knowledgeBaseId | Long | 是 | — | 知识库 id |
| keyword | String | 否 | — | 全文关键词，搜索文件名、正文、AI 摘要；最长 100 字符 |
| folderId | Long | 否 | — | 文件夹筛选，0 或不传表示全部 |
| fileType | String | 否 | — | pdf / docx / txt / md |
| processStatus | String | 否 | — | UPLOADED / PROCESSING / SUCCESS / FAILED |
| aiCategory | String | 否 | — | 技术 / 学习 / 工作 / 生活 / 其他 |
| startTime | String | 否 | — | 上传时间起，ISO 格式：2026-11-01T00:00:00 |
| endTime | String | 否 | — | 上传时间止，ISO 格式 |
| sort | String | 否 | relevance | relevance（相关度）/ createTime（上传时间）；无关键词时强制 createTime |
| page | Long | 否 | 1 | 页码，从 1 开始 |
| size | Long | 否 | 10 | 每页条数，最大 100 |

请求示例：

```
GET /api/documents/search?knowledgeBaseId=1&keyword=Spring&fileType=pdf&startTime=2026-11-01T00:00:00&page=1&size=10
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "page": 1,
    "size": 10,
    "pages": 1,
    "records": [
      {
        "id": 13,
        "knowledgeBaseId": 1,
        "folderId": 1,
        "originalName": "Spring Boot 指南.pdf",
        "fileType": "pdf",
        "fileSize": 102400,
        "processStatus": "SUCCESS",
        "aiCategory": "技术",
        "createTime": "2026-11-20T10:00:00",
        "textSnippet": "……第一章 Spring Boot 简介，自动配置……"
      }
    ]
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 知识库不存在或不属于自己 | 2001 | 知识库不存在 |
| 指定的 folderId 不属于该知识库 | 2003 | 文件夹不存在 |
| fileType 不在允许范围 | 2008 | 不支持的文件类型 |
| processStatus / aiCategory / sort 非法 | 400 | 参数错误 |
| 开始时间晚于结束时间 | 400 | 开始时间不能晚于结束时间 |
| keyword 超过 100 字符 | 400 | 搜索关键词不能超过100个字符 |
| 未登录 | 401 | 未登录或登录已过期 |

说明：

- 使用 MySQL FULLTEXT + ngram 解析器，索引列为 `original_name`、`extracted_text`、`summary`；`keywords` JSON 不参与全文检索。
- 需要先执行 `sql/03_fulltext.sql` 创建全文索引；否则 `MATCH` 会报 1191。
- `ngram_token_size` 默认是 2，单个汉字的关键词可能搜不到；需要的话调整 MySQL 配置。
- 没有 keyword 时就是普通的高级筛选，按 `create_time` 倒序。
- `textSnippet` 是正文中关键词附近的一段，前端可以自行做高亮。
- 只返回当前用户、当前知识库下未删除的文档。

### 17.11 DOC-13 文档分块并向量化

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/documents/{id}/embed` |
| 认证 | 必须 |
| 请求体 | 无 |

说明：要求文档已经解析成功（`processStatus=SUCCESS`）且正文不为空。服务会把正文按固定大小切块，逐块调用 embedding 模型，把向量写入 `document_chunk` 表。重复调用会先删除该文档的旧 chunk，再重新写入。

切块规则：

- 每块默认 500 个字符；
- 相邻块重叠 100 个字符；
- 单篇文档默认最多 200 块，避免一次调用太多 embedding。

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "documentId": 13,
    "chunkCount": 4,
    "embeddingDim": 1024,
    "elapsedMs": 1234
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 文件不存在或不属于自己 | 2013 | 文件不存在 |
| 文档尚未解析成功 / 正文为空 | 3003 | 文档尚未解析，请先解析正文 |
| 向量化服务未配置 | 3007 | 向量化服务未配置 |
| 向量化服务调用失败 / 超时 | 3005 | 向量化服务调用失败 |
| 向量化返回格式错误 | 3006 | 向量化返回格式错误 |

说明：

- embedding 服务通过环境变量配置：`EMBEDDING_BASE_URL`、`EMBEDDING_API_KEY`、`EMBEDDING_MODEL`。
- 默认按本地 Ollama 的 OpenAI 兼容接口配置，模型 `bge-m3`；也可以换成 OpenAI `text-embedding-3-small`。
- 向量以 JSON 数组字符串存储在 MySQL 的 `document_chunk.embedding` 字段里，不依赖 Redis 或向量数据库。

### 17.12 DOC-14 向量语义检索

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/documents/vector-search` |
| 认证 | 必须 |

查询参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| knowledgeBaseId | Long | 是 | — | 知识库 id |
| query | String | 是 | — | 自然语言问题或关键词 |
| topK | int | 否 | 5 | 返回最相似的前 K 个 chunk，最大 20 |

请求示例：

```
GET /api/documents/vector-search?knowledgeBaseId=1&query=Spring%20Boot%20自动配置原理&topK=5
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "documentId": 13,
      "originalName": "Spring Boot 指南.pdf",
      "chunkId": 88,
      "chunkIndex": 2,
      "content": "Spring Boot 的自动配置通过 @EnableAutoConfiguration ...",
      "score": 0.8732
    }
  ]
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 知识库不存在或不属于自己 | 2001 | 知识库不存在 |
| query 为空 | 400 | 查询内容不能为空 |
| topK 超出范围 | 400 | topK 必须在 1 到 20 之间 |
| 向量化服务未配置 | 3007 | 向量化服务未配置 |
| 向量化服务调用失败 | 3005 | 向量化服务调用失败 |
| 向量化返回格式错误 | 3006 | 向量化返回格式错误 |

说明：

- 先把 query 向量化，再和该知识库下所有文档 chunk 的向量算余弦相似度，按分数从高到低返回 topK。
- 只检索 `processStatus=SUCCESS`、未删除、属于当前用户当前知识库的文档。
- 当前用 Java 在内存里算余弦相似度，单次最多加载 5000 个 chunk；数据量大时再换向量数据库。
- 使用前需要先对文档调用 DOC-13 做向量化，否则结果为空。



## 18. AI 分析接口（AI-01）

所有 AI 分析接口都必须登录，并且只能分析自己的文档。

### 18.1 AI-01 文档 AI 分析

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/documents/{id}/analyze` |
| 认证 | 必须 |
| 请求体 | 无 |

说明：要求文档已经解析成功（`processStatus=SUCCESS`），否则返回 3003。同步调用大模型，返回摘要、关键词、分类和置信度；重复调用会覆盖上一次结果。

前置条件：

- 文档存在且属于当前用户；
- `process_status = SUCCESS`；
- `extracted_text` 不为空。

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 13,
    "knowledgeBaseId": 1,
    "folderId": 1,
    "originalName": "Spring Boot 指南.pdf",
    "fileType": "pdf",
    "fileSize": 102400,
    "processStatus": "SUCCESS",
    "summary": "本文介绍了 Spring Boot 的核心概念、自动配置和常用组件。",
    "keywords": "[\"Spring Boot\",\"自动配置\",\"Java\"]",
    "aiCategory": "技术",
    "confidence": 0.9500,
    "errorMessage": null,
    "extractedTextLength": 23580,
    "textPreview": "第一章 Spring Boot 简介……",
    "createTime": "2026-11-19T16:55:03",
    "updateTime": "2026-11-20T10:05:00"
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 文件不存在或不属于自己 | 2013 | 文件不存在 |
| 文档还没解析成功 | 3003 | 文档尚未解析，请先解析正文 |
| AI 服务未配置 | 3004 | AI 服务未配置 |
| AI 调用失败 / 超时 | 3001 | AI 服务调用失败（message 为具体原因） |
| AI 返回不是合法 JSON | 3002 | AI 返回内容格式错误（message 为具体原因） |

说明：

- `keywords` 在数据库中是 JSON 字符串，接口原样返回；前端可以 `JSON.parse`。
- `aiCategory` 只会是：技术、学习、工作、生活、其他。
- `confidence` 范围 0–1，保留 4 位小数。
- 当前只把正文前 8000 个字符送给模型，用于控制 token 和费用；长文档分块摘要留到后续功能。
- 分析失败不会修改 `process_status`，文档仍然是 `SUCCESS`，可以重新调用本接口覆盖结果。

---

## 19. RAG 智能问答（CHAT-01）

### 19.1 基本信息

| 项 | 内容 |
|---|---|
| 方法 | POST |
| 路径 | `/api/chat/ask` |
| 认证 | 必须 |
| 请求体 | JSON |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| knowledgeBaseId | Long | 是 | 知识库 id |
| sessionId | Long | 否 | 会话 id；不传则新建会话 |
| question | String | 是 | 用户问题，最长 2000 字符 |

请求示例：

```json
{
  "knowledgeBaseId": 1,
  "sessionId": null,
  "question": "Spring Boot 的自动配置原理是什么？"
}
```

流程：

1. 校验知识库归属；
2. 如果传了 sessionId，校验会话属于当前用户且属于该知识库；
3. 用问题做向量检索，取 topK 个相关 chunk；
4. 把 chunk 作为资料拼进 prompt，让大模型只根据资料回答；
5. 保存 user / assistant 两条 `chat_message`；
6. 返回回答和引用片段。

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": 1,
    "messageId": 2,
    "answer": "Spring Boot 的自动配置通过 @EnableAutoConfiguration 和条件注解实现 [1]。",
    "references": [
      {
        "documentId": 13,
        "originalName": "Spring Boot 指南.pdf",
        "chunkId": 88,
        "chunkIndex": 2,
        "content": "Spring Boot 的自动配置通过 @EnableAutoConfiguration ...",
        "score": 0.8732
      }
    ]
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 知识库不存在或不属于自己 | 2001 | 知识库不存在 |
| 会话不存在或不属于自己 | 3008 | 会话不存在 |
| sessionId 属于其他知识库 | 400 | 会话不属于该知识库 |
| question 为空或超过 2000 字符 | 400 | 参数错误 |
| AI 服务未配置 | 3004 | AI 服务未配置 |
| AI 调用失败 / 超时 | 3001 | AI 服务调用失败 |
| 向量化服务未配置 | 3007 | 向量化服务未配置 |
| 向量化服务调用失败 | 3005 | 向量化服务调用失败 |
| 向量化返回格式错误 | 3006 | 向量化返回格式错误 |

说明：

- 如果知识库还没有任何 chunk，接口会返回固定回答“知识库中还没有可用资料，请先上传文档并完成解析和向量化。”，不会调用大模型，`references` 为空数组。
- 多轮对话时把上一轮返回的 `sessionId` 传回来即可，后端会带最近 6 条消息作为上下文。
- 当前 topK 默认 5，可通过 `chat.top-k` 调整。
- 回答中的 `[1]` 对应 `references` 数组的第 1 项。



## 20. 会话历史管理（CHAT-02 ~ CHAT-05）

### 20.1 CHAT-02 会话列表

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/chat/sessions` |
| 认证 | 必须 |

查询参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| knowledgeBaseId | Long | 否 | — | 只看某个知识库的会话 |
| page | long | 否 | 1 | 页码 |
| size | long | 否 | 10 | 每页数量，最大 100 |

说明：只返回当前用户的会话，按 `update_time` 倒序。

成功响应：

```json
{
  "code": 200,
  "data": {
    "total": 2,
    "page": 1,
    "size": 10,
    "pages": 1,
    "records": [
      {
        "id": 1,
        "knowledgeBaseId": 1,
        "title": "Spring Boot 自动配置是什么？",
        "createTime": "2026-11-22T20:00:00",
        "updateTime": "2026-11-22T20:05:00"
      }
    ]
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 知识库不存在或不属于自己 | 2001 | 知识库不存在 |
| 不带 token | 401 | 未登录或登录已过期 |

### 20.2 CHAT-03 会话消息列表

| 项 | 内容 |
|---|---|
| 方法 | GET |
| 路径 | `/api/chat/sessions/{id}/messages` |
| 认证 | 必须 |

查询参数：`page` 默认 1，`size` 默认 20，最大 100。

说明：按 `create_time` 正序返回，`references` 是 assistant 消息保存的引用片段。

成功响应：

```json
{
  "code": 200,
  "data": {
    "total": 4,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 1,
        "sessionId": 1,
        "role": "USER",
        "content": "Spring Boot 自动配置是什么？",
        "references": [],
        "createTime": "2026-11-22T20:00:00"
      },
      {
        "id": 2,
        "sessionId": 1,
        "role": "ASSISTANT",
        "content": "Spring Boot 的自动配置通过 ...... [1]",
        "references": [
          {
            "documentId": 13,
            "originalName": "rag.txt",
            "chunkId": 1,
            "chunkIndex": 0,
            "content": "Spring Boot auto configuration ...",
            "score": 0.6858
          }
        ],
        "createTime": "2026-11-22T20:00:03"
      }
    ]
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 会话不存在或不属于自己 | 3008 | 会话不存在 |

### 20.3 CHAT-04 重命名会话

| 项 | 内容 |
|---|---|
| 方法 | PUT |
| 路径 | `/api/chat/sessions/{id}` |
| 认证 | 必须 |
| 请求体 | JSON |

请求示例：

```json
{
  "title": "Spring Boot 自动配置学习"
}
```

成功响应：

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "knowledgeBaseId": 1,
    "title": "Spring Boot 自动配置学习",
    "createTime": "2026-11-22T20:00:00",
    "updateTime": "2026-11-22T20:10:00"
  }
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 会话不存在或不属于自己 | 3008 | 会话不存在 |
| title 为空或超过 255 字符 | 400 | 参数错误 |

### 20.4 CHAT-05 删除会话

| 项 | 内容 |
|---|---|
| 方法 | DELETE |
| 路径 | `/api/chat/sessions/{id}` |
| 认证 | 必须 |
| 请求体 | 无 |

说明：删除会话时，消息会被物理删除，会话本身逻辑删除。删除后会话列表和消息列表都不再返回它。

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

失败场景：

| 条件 | code | message |
|---|---|---|
| 会话不存在或不属于自己 | 3008 | 会话不存在 |



## 21. 规划中的接口（尚未实现）

| 模块 | 规划路径 |
|---|---|
| 聊天 | `/api/chat/sessions` |

未实现前调用这些路径会返回 401（未登录）或 404。
