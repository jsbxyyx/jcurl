# JCurl 使用手册

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 一个简洁、强大的 Java HTTP 客户端库，类似于 curl 的使用体验，零依赖。

## 📖 目录

- [特性](#特性)
- [快速开始](#快速开始)
- [基本用法](#基本用法)
- [高级功能](#高级功能)
- [文件上传](#文件上传)
- [认证](#认证)
- [配置选项](#配置选项)
- [自定义执行器](#自定义执行器)
- [API 参考](#api-参考)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

## ✨ 特性

- ✅ **零依赖** - 仅使用 JDK 标准库
- ✅ **链式调用** - 流畅的 API 设计
- ✅ **Curl 兼容** - 支持从 curl 命令创建请求
- ✅ **文件上传** - 完整的 multipart/form-data 支持
- ✅ **多值支持** - Headers 和 Query 参数支持多个相同的 key
- ✅ **大小写不敏感** - 响应头大小写不敏感（符合 HTTP 规范）
- ✅ **认证支持** - Basic、Bearer Token
- ✅ **代理支持** - HTTP/SOCKS 代理
- ✅ **重试机制** - 可配置的重试策略
- ✅ **SSL 配置** - 支持不安全连接
- ✅ **压缩支持** - 自动处理 gzip

## 🚀 快速开始

### 安装

maven

```xml
<dependency>
    <groupId>io.github.jsbxyyx</groupId>
    <artifactId>jcurl</artifactId>
    <version>1.0.0</version>
</dependency>
```

或者

将 `JCurl.java` 复制到你的项目中：

```bash
# 克隆仓库
git clone https://github.com/jsbxyyx/jcurl.git

# 复制到你的项目
cp jcurl/src/main/java/io/github/jsbxyyx/jcurl/JCurl.java your-project/src/
```

### 第一个请求

```java
import io.github.jsbxyyx.jcurl.JCurl;

// 简单的 GET 请求
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.github.com/users/octocat")
    .get()
    .exec();

System.out.println("状态码:  " + response.getStatusCode());
System.out.println("响应体: " + response.getBody());
```

## 📚 基本用法

### GET 请求

```java
// 基本 GET
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://httpbin.org/get")
    .get()
    .exec();

// 带查询参数
response = JCurl.create()
    .url("https://httpbin.org/get")
    .queryParam("key1", "value1")
    .queryParam("key2", "value2")
    .get()
    .exec();

// 带自定义 Headers
response = JCurl.create()
    .url("https://httpbin.org/get")
    .header("Accept", "application/json")
    .header("User-Agent", "JCurl/1.0")
    .get()
    .exec();
```

### POST 请求

```java
// POST JSON
String json = "{\"name\": \"John\", \"age\": 30}";
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://httpbin.org/post")
    .post()
    .jsonBody(json)
    .exec();

// POST 表单数据
response = JCurl.create()
    .url("https://httpbin.org/post")
    .post()
    .formField("username", "john")
    .formField("password", "secret")
    .exec();

// POST 原始数据
response = JCurl.create()
    .url("https://httpbin.org/post")
    .post()
    .body("raw text data")
    .header("Content-Type", "text/plain")
    .exec();
```

### 其他 HTTP 方法

```java
// PUT
JCurl.create()
    .url("https://api.example.com/users/1")
    .put()
    .jsonBody("{\"name\": \"Updated\"}")
    .exec();

// DELETE
JCurl.create()
    .url("https://api.example.com/users/1")
    .delete()
    .exec();

// PATCH
JCurl.create()
    .url("https://api.example.com/users/1")
    .patch()
    .jsonBody("{\"email\": \"new@example.com\"}")
    .exec();

// HEAD
JCurl.create()
    .url("https://api.example.com/resource")
    .head()
    .exec();
```

## 🎯 高级功能

### 从 Curl 命令创建

```java
// 从 curl 命令字符串
String curlCommand = "curl -X POST https://httpbin.org/post " +
    "-H 'Content-Type: application/json' " +
    "-d '{\"key\": \"value\"}'";

JCurl.HttpResponseModel response = JCurl.fromCurl(curlCommand).exec();

// 使用 opt() 方法（curl 风格）
response = JCurl.create()
    .url("https://httpbin.org/post")
    .opt("-X", "POST")
    .opt("-H", "Content-Type:  application/json")
    .opt("-d", "{\"key\": \"value\"}")
    .exec();
```

### 多值 Headers 和 Query 参数

```java
// 多个相同名称的查询参数
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/search")
    .queryParam("tag", "java")
    .queryParam("tag", "http")
    .queryParam("tag", "curl")
    .get()
    .exec();

// 多个相同名称的 Headers
response = JCurl.create()
    .url("https://api.example.com/data")
    .header("Accept", "application/json")
    .header("Accept", "application/xml")
    .get()
    .exec();

// 访问响应的多值 Headers
List<String> cookies = response.getHeaderValues("Set-Cookie");
for (String cookie : cookies) {
    System.out.println("Cookie: " + cookie);
}
```

### 响应头处理（大小写不敏感）

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://httpbin.org/get")
    .get()
    .exec();

// 以下三种方式都能获取到相同的值
String ct1 = response.getHeader("Content-Type");
String ct2 = response.getHeader("content-type");
String ct3 = response.getHeader("CONTENT-TYPE");

// 检查是否存在某个 Header
boolean hasContentType = response.hasHeader("content-type");

// 获取所有 Set-Cookie
List<String> cookies = response.getCookies();
```

## 📤 文件上传

### 单文件上传

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://httpbin.org/post")
    .post()
    .formFile("file", "/path/to/file.txt")
    .exec();
```

### 多文件上传

```java
// 不同字段名（推荐）
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/upload")
    .post()
    .formFile("avatar", "photo.jpg")
    .formFile("document", "resume.pdf")
    .formFile("certificate", "cert.png")
    .exec();

// 相同字段名（后端支持时）
response = JCurl.create()
    .url("https://api.example.com/upload")
    .post()
    .formFile("files", "file1.txt")
    .formFile("files", "file2.txt")
    .formFile("files", "file3.txt")
    .exec();
```

### 文件 + 文本字段混合

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/profile")
    .post()
    .formField("username", "john")
    .formField("email", "john@example.com")
    .formField("bio", "Software developer")
    .formFile("avatar", "avatar.jpg")
    .formFile("resume", "resume.pdf")
    .exec();
```

### 自定义文件名和 Content-Type

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/upload")
    .post()
    .formFile("file", "/local/path/image.png", "custom-name.png", "image/png")
    .exec();
```

## 🔐 认证

### Basic 认证

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://httpbin.org/basic-auth/user/passwd")
    .auth("user", "passwd")
    .get()
    .exec();
```

### Bearer Token

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/data")
    .bearerToken("your-jwt-token-here")
    .get()
    .exec();
```

### Cookie

```java
// 设置单个 Cookie
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/data")
    .cookie("session", "abc123")
    .get()
    .exec();

// 设置多个 Cookies
Map<String, String> cookies = new HashMap<>();
cookies.put("session", "abc123");
cookies.put("token", "xyz789");

response = JCurl.create()
    .url("https://api.example.com/data")
    .cookies(cookies)
    .get()
    .exec();
```

## ⚙️ 配置选项

### 超时设置

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/slow")
    .connectTimeout(5000)   // 连接超时 5 秒
    .readTimeout(10000)     // 读取超时 10 秒
    .get()
    .exec();
```

### 重定向

```java
// 自动跟随重定向
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://httpbin.org/redirect/3")
    .followRedirects(true)
    .get()
    .exec();

// 不跟随重定向
response = JCurl.create()
    .url("https://httpbin.org/redirect/3")
    .followRedirects(false)
    .get()
    .exec();

String location = response.getHeader("Location");
```

### SSL 配置

```java
// 允许不安全的 HTTPS 连接（跳过证书验证）
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://self-signed.badssl.com/")
    .insecure()
    .get()
    .exec();

// 或者
response = JCurl.create()
    .url("https://self-signed.badssl.com/")
    .verifySSL(false)
    .get()
    .exec();
```

### 代理

```java
// HTTP 代理
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/data")
    .proxy("http://proxy.example.com:8080")
    .get()
    .exec();

// SOCKS 代理
response = JCurl.create()
    .url("https://api.example.com/data")
    .proxy("socks5://proxy.example.com:1080")
    .get()
    .exec();

// 代理认证
response = JCurl.create()
    .url("https://api.example.com/data")
    .proxy("http://proxy.example.com:8080")
    .proxyAuth("username", "password")
    .get()
    .exec();
```

### 重试机制

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/unstable")
    .retry(3)           // 最多重试 3 次
    .retryDelay(1000)   // 重试间隔 1 秒
    .get()
    .exec();
```

### 压缩

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://httpbin.org/gzip")
    .compressed()  // 自动处理 gzip 压缩
    .get()
    .exec();
```

### 其他设置

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/data")
    .userAgent("MyApp/1.0")
    .referer("https://example.com")
    .maxDownloadSize(1024 * 1024 * 10)  // 最大下载 10MB
    .get()
    .exec();
```

## 🔌 自定义执行器

JCurl 支持自定义执行器，你可以选择不同的 HTTP 客户端实现。

### 使用内置的 HttpURLConnection（默认）

```java
// 默认使用 HttpURLConnection
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/data")
    .get()
    .exec();

// 或显式指定
response = JCurl.create()
    .url("https://api.example.com/data")
    .get()
    .exec(JCurl.HttpUrlConnectionExecutor.create());
```

### 使用 OkHttp 执行器

首先添加 OkHttp 依赖：

```xml
<!-- Maven -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

```gradle
// Gradle
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

然后使用 OkHttp 执行器：

```java
import io.github.jsbxyyx.jcurl.OkHttpExecutor;

// 使用 OkHttp（支持 HTTP/2）
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/data")
    .get()
    .exec(OkHttpExecutor.create());

```

### 实现自定义执行器

```java
import io.github.jsbxyyx.jcurl.JCurl;

public class MyCustomExecutor implements JCurl.HttpExecutor {
    
    @Override
    public JCurl.HttpResponseModel execute(JCurl.HttpRequestModel request) throws IOException {
        // 使用你喜欢的 HTTP 客户端实现
        // 例如:  Apache HttpClient, java.net.http.HttpClient 等
        
        JCurl.HttpResponseModel response = new JCurl.HttpResponseModel();
        
        // ...  实现请求逻辑 ...
        
        return response;
    }
}

// 使用自定义执行器
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/data")
    .get()
    .exec(new MyCustomExecutor());
```

### 执行器对比

| 执行器 | HTTP/2 | 依赖 | 性能 | 推荐场景 |
|--------|--------|------|------|---------|
| **HttpURLConnection** | ❌ (Java 8) / ✅ (Java 11+) | ✅ 零依赖 | 中等 | 简单项目、零依赖需求 |
| **OkHttp** | ✅ | ❌ 需要引入 | 优秀 | 生产环境、需要 HTTP/2 |
| **自定义** | 取决于实现 | 取决于实现 | 取决于实现 | 特殊需求 |

## 📖 API 参考

### JCurl 主要方法

| 方法 | 描述 |
|------|------|
| `create()` | 创建新的 JCurl 实例 |
| `fromCurl(String)` | 从 curl 命令创建 |
| `url(String)` | 设置 URL |
| `method(String)` | 设置 HTTP 方法 |
| `get()` | GET 请求 |
| `post()` | POST 请求 |
| `put()` | PUT 请求 |
| `delete()` | DELETE 请求 |
| `patch()` | PATCH 请求 |
| `head()` | HEAD 请求 |
| `header(String, String)` | 添加请求头 |
| `queryParam(String, String)` | 添加查询参数 |
| `body(String)` | 设置请求体 |
| `jsonBody(String)` | 设置 JSON 请求体 |
| `formField(String, String)` | 添加表单字段 |
| `formFile(String, String)` | 添加文件字段 |
| `auth(String, String)` | Basic 认证 |
| `bearerToken(String)` | Bearer Token 认证 |
| `cookie(String, String)` | 添加 Cookie |
| `proxy(String)` | 设置代理 |
| `connectTimeout(int)` | 连接超时（毫秒） |
| `readTimeout(int)` | 读取超时（毫秒） |
| `followRedirects(boolean)` | 是否跟随重定向 |
| `insecure()` | 允许不安全的 SSL |
| `retry(int)` | 重试次数 |
| `exec()` | 执行请求 |

### HttpResponseModel 方法

| 方法 | 描述 |
|------|------|
| `getStatusCode()` | 获取状态码 |
| `getStatusMessage()` | 获取状态消息 |
| `getHeader(String)` | 获取单个响应头（大小写不敏感） |
| `getHeaderValues(String)` | 获取多值响应头 |
| `getCookies()` | 获取所有 Set-Cookie |
| `hasHeader(String)` | 检查是否存在某个响应头 |
| `getBody()` | 获取响应体（字符串） |
| `getBodyBytes()` | 获取响应体（字节数组） |
| `isSuccess()` | 是否成功（2xx） |

## 💡 最佳实践

### 1. 错误处理

```java
try {
    JCurl.HttpResponseModel response = JCurl.create()
        .url("https://api.example.com/data")
        .get()
        .exec();

    if (response.isSuccess()) {
        // 处理成功响应
        System.out.println(response.getBody());
    } else {
        // 处理错误响应
        System.err.println("Error: " + response.getStatusCode());
        System.err.println(response.getBody());
    }
} catch (IOException e) {
    // 处理网络错误
    e.printStackTrace();
}
```

### 2. JSON 处理

```java
// 发送 JSON
String requestJson = "{\"name\": \"John\", \"age\": 30}";
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/users")
    .post()
    .jsonBody(requestJson)
    .exec();

// 解析响应 JSON（使用你喜欢的 JSON 库）
if (response.isSuccess()) {
    String responseJson = response.getBody();
    // 使用 Gson/Jackson/etc 解析
}
```

### 3. 文件上传最佳实践

```java
// 推荐：使用不同的字段名
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://api.example.com/upload")
    .post()
    .formField("title", "My Upload")
    .formField("description", "Description here")
    .formFile("avatar", "photo.jpg")
    .formFile("document", "doc.pdf")
    .exec();
```

### 4. 复用配置

```java
// 构建基础请求
JCurl baseRequest = JCurl.create()
    .url("https://api.example.com")
    .header("Authorization", "Bearer " + token)
    .header("Accept", "application/json")
    .connectTimeout(5000)
    .readTimeout(10000);

// 从模型创建新请求
HttpRequestModel baseModel = baseRequest.build();

// 请求 1
JCurl.HttpResponseModel response1 = JCurl.fromModel(baseModel)
    .url("https://api.example.com/users")
    .get()
    .exec();

// 请求 2
JCurl.HttpResponseModel response2 = JCurl.fromModel(baseModel)
    .url("https://api.example.com/posts")
    .get()
    .exec();
```

## ❓ 常见问题

### Q: JCurl 需要什么 Java 版本？
**A:** Java 8 或更高版本。

### Q: 是否支持异步请求？
**A:** 当前版本是同步的。你可以使用 `ExecutorService` 或 `CompletableFuture` 包装实现异步。

```java
CompletableFuture<HttpResponseModel> future = CompletableFuture.supplyAsync(() -> {
    try {
        return JCurl.create()
            .url("https://api.example.com/data")
            .get()
            .exec();
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
});
```

### Q: 如何调试请求？
**A:** 使用 `peek()` 方法查看请求模型：

```java
JCurl jcurl = JCurl.create()
    .url("https://api.example.com/data")
    .header("X-Custom", "value")
    .queryParam("key", "value");

HttpRequestModel model = jcurl.peek();
System.out.println("URL: " + model.getFullUrl());
System.out.println("Method: " + model.getMethod());
System.out.println("Headers: " + model.getHeaders());
```

### Q: 是否支持 HTTP/2？
**A:** 当前使用 `HttpURLConnection`，支持的协议取决于 JDK 版本。Java 11+ 可考虑使用新的 `HttpClient`。

### Q: 文件上传时 httpbin 只显示一个文件？
**A:** 这是 httpbin.org 的限制，实际应用中的后端（Spring Boot、Express 等）都能正确接收多个同名文件。

### Q: 如何处理大文件下载？
**A:** 使用 `maxDownloadSize()` 限制大小：

```java
JCurl.HttpResponseModel response = JCurl.create()
    .url("https://example.com/large-file.zip")
    .maxDownloadSize(1024 * 1024 * 100)  // 限制 100MB
    .get()
    .exec();

byte[] data = response.getBodyBytes();
Files.write(Paths.get("downloaded.zip"), data);
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

MIT License

---

**Made with ❤️ by [@jsbxyyx](https://github.com/jsbxyyx)**