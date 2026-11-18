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

## 15. 规划中的接口（尚未实现）

| 模块 | 规划路径 |
|---|---|
| 知识库 | `/api/knowledge-bases` |
| 文件夹 | `/api/folders` |
| 文件 | `/api/documents` |
| AI 分析 | `/api/documents/{id}/analyze` |
| 聊天 | `/api/chat/sessions` |

未实现前调用这些路径会返回 401（未登录）或 404。
