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

## 6. 错误码总表

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

错误码分段规则：

- 1xxx：用户与认证
- 2xxx：知识库、文件夹、文件
- 3xxx：AI 分析、RAG、聊天
- 9xxx：预留

---

## 7. 测试账号

由 `sql/02_data.sql` 初始化：

| 邮箱 | 密码 |
|---|---|
| test@mindspace.com | 123456 |

---

## 8. 规划中的接口（尚未实现）

| 模块 | 规划路径 |
|---|---|
| 知识库 | `/api/knowledge-bases` |
| 文件夹 | `/api/folders` |
| 文件 | `/api/documents` |
| AI 分析 | `/api/documents/{id}/analyze` |
| 聊天 | `/api/chat/sessions` |

未实现前调用这些路径会返回 401（未登录）或 404。
