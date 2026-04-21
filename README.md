# Spring Authorization Server 局域网联调配置说明

本文档用于指导在同一局域网下，通过手机访问当前 Spring Authorization Server + Vue 演示项目，并测试 Authorization Code + PKCE、Device Code 等 OAuth2 场景。

当前电脑局域网 IP：`192.168.0.108`

下面所有配置和命令都已经替换为这个 IP，可直接使用。

---

## 1. 先确认你电脑的局域网 IP

在电脑上执行：

```bash
ipconfig
```

找到当前 Wi‑Fi 或以太网网卡的 IPv4，比如：

```text
192.168.0.108
```

下面都使用这个地址。

---

## 2. 后端配置

文件：[`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml)

把这段改成下面这样：

```yml
server:
  port: 30000

app:
  base-url: http://192.168.0.108:30000
  allowed-origins: http://localhost:5173,http://192.168.0.108:5173
  frontend-base-url: http://192.168.0.108:5173
```

### 这 3 个值分别是干什么的

- `app.base-url`
  - OAuth2 服务器自己的对外地址
  - 会影响 issuer、discovery、device code 返回的 `verification_uri`

- `app.allowed-origins`
  - 允许哪些前端来源跨域访问后端
  - 本机浏览器调试要保留 `http://localhost:5173`
  - 手机访问要加 `http://192.168.0.108:5173`

- `app.frontend-base-url`
  - 前端对外访问地址
  - 启动时会用它自动同步数据库里的 redirect URI

---

## 3. 前端配置

前端不需要再改源码文件，只要在启动时传环境变量。

### PowerShell 启动方式

```powershell
$env:VITE_BACKEND_ORIGIN="http://192.168.0.108:30000"
npm --prefix frontend run dev
```

### CMD 启动方式

```cmd
set VITE_BACKEND_ORIGIN=http://192.168.0.108:30000
npm --prefix frontend run dev
```

### Git Bash 启动方式

```bash
VITE_BACKEND_ORIGIN=http://192.168.0.108:30000 npm --prefix frontend run dev
```

这个变量会被 [`frontend/vite.config.js`](frontend/vite.config.js) 和 [`frontend/src/views/FlowsDemo.vue`](frontend/src/views/FlowsDemo.vue) 使用：

- Vite 代理转发到 `http://192.168.0.108:30000`
- 前端“发起授权登录”时跳到 `http://192.168.0.108:30000/oauth2/authorize`

---

## 4. 后端启动方式

如果你直接改了 [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml)，正常启动即可：

```bash
mvn -f backend/pom.xml spring-boot:run
```

如果你更想用环境变量覆盖，也可以。

### PowerShell

```powershell
$env:APP_BASE_URL="http://192.168.0.108:30000"
$env:APP_ALLOWED_ORIGINS="http://localhost:5173,http://192.168.0.108:5173"
$env:APP_FRONTEND_BASE_URL="http://192.168.0.108:5173"
mvn -f backend/pom.xml spring-boot:run
```

### Git Bash

```bash
APP_BASE_URL=http://192.168.0.108:30000 \
APP_ALLOWED_ORIGINS=http://localhost:5173,http://192.168.0.108:5173 \
APP_FRONTEND_BASE_URL=http://192.168.0.108:5173 \
mvn -f backend/pom.xml spring-boot:run
```

---

## 5. 推荐的完整启动顺序

### 先启动后端

```bash
APP_BASE_URL=http://192.168.0.108:30000 \
APP_ALLOWED_ORIGINS=http://localhost:5173,http://192.168.0.108:5173 \
APP_FRONTEND_BASE_URL=http://192.168.0.108:5173 \
mvn -f backend/pom.xml spring-boot:run
```

### 再启动前端

```bash
VITE_BACKEND_ORIGIN=http://192.168.0.108:30000 npm --prefix frontend run dev
```

---

## 6. 启动成功后怎么访问

### 电脑本机访问

```text
http://localhost:5173
```

### 手机访问

```text
http://192.168.0.108:5173
```

注意：手机和电脑必须在同一个 Wi‑Fi / 同一局域网。

---

## 7. 手机联调时重点验证什么

### 场景 1：Authorization Code + PKCE

在手机上打开：

```text
http://192.168.0.108:5173
```

然后：

1. 进入 OAuth2 演示页
2. 点击“发起授权登录”
3. 浏览器应跳到：

```text
http://192.168.0.108:30000/oauth2/authorize?...
```

4. 登录授权后，应跳回：

```text
http://192.168.0.108:5173/callback?code=...
```

5. 页面显示 access_token / refresh_token

---

### 场景 2：Device Code

在电脑或手机点“申请 Device Code”后，返回的：

- `verification_uri`
- `verification_uri_complete`

应该是：

```text
http://192.168.0.108:30000/...
```

而不是：

```text
http://localhost:30000/...
```

如果还是 `localhost`，说明后端 `app.base-url` 没生效。

---

## 8. 常见问题排查

### 问题 1：手机打不开前端页面

先检查：

- 前端是否监听了 `0.0.0.0`
  - 现在 [`frontend/vite.config.js`](frontend/vite.config.js) 已经支持
- Windows 防火墙是否拦截了 5173 端口
- 手机和电脑是否在同一网络

你可以先在电脑浏览器访问：

```text
http://192.168.0.108:5173
```

如果电脑自己都打不开 LAN 地址，手机肯定也不行。

---

### 问题 2：授权跳转到了 localhost

说明前端或后端仍有旧配置：

- 检查前端启动时有没有带：
  - `VITE_BACKEND_ORIGIN=http://192.168.0.108:30000`
- 检查后端：
  - `app.base-url=http://192.168.0.108:30000`

---

### 问题 3：跳转后 400 redirect_uri 不匹配

现在 [`backend/src/main/java/com/demo/authserver/config/DataInitializer.java`](backend/src/main/java/com/demo/authserver/config/DataInitializer.java) 会在启动时自动同步：

- `spa-public-client`
- `all-in-one-client`

的 redirect URI。

所以遇到这个问题时先做两件事：

1. 确认后端已经重启
2. 确认 `app.frontend-base-url` 是：
   - `http://192.168.0.108:5173`

---

### 问题 4：手机请求接口报 CORS

检查 [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) 里的：

```yml
app:
  allowed-origins: http://localhost:5173,http://192.168.0.108:5173
```

如果你的手机访问地址不是这个 IP，就要改成实际 IP。

---

## 9. 一份可直接用的最终示例

假设你的 IP 就是 `192.168.0.108`。

### `application.yml`

```yml
server:
  port: 30000

app:
  base-url: http://192.168.0.108:30000
  allowed-origins: http://localhost:5173,http://192.168.0.108:5173
  frontend-base-url: http://192.168.0.108:5173

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ssdemo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: fairy-vip
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

logging:
  level:
    org.springframework.security: DEBUG
    com.demo.authserver: DEBUG
```

### 后端启动

```bash
mvn -f backend/pom.xml spring-boot:run
```

### 前端启动

```bash
VITE_BACKEND_ORIGIN=http://192.168.0.108:30000 npm --prefix frontend run dev
```

### 手机访问

```text
http://192.168.0.108:5173
```

---

## 10. 可直接复制执行的命令清单

### 方案 A：直接改 `application.yml`

后端：

```bash
mvn -f backend/pom.xml spring-boot:run
```

前端：

```bash
VITE_BACKEND_ORIGIN=http://192.168.0.108:30000 npm --prefix frontend run dev
```

### 方案 B：全部用环境变量启动

后端：

```bash
APP_BASE_URL=http://192.168.0.108:30000 \
APP_ALLOWED_ORIGINS=http://localhost:5173,http://192.168.0.108:5173 \
APP_FRONTEND_BASE_URL=http://192.168.0.108:5173 \
mvn -f backend/pom.xml spring-boot:run
```

前端：

```bash
VITE_BACKEND_ORIGIN=http://192.168.0.108:30000 npm --prefix frontend run dev
```

---

## 11. 建议的实际操作顺序

1. 先把 [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) 改成上面的实际 IP 配置
2. 重启后端
3. 用带 `VITE_BACKEND_ORIGIN` 的命令启动前端
4. 用手机访问 `http://192.168.0.108:5173`
5. 先测试 PKCE 登录
6. 再测试 Device Code 的 `verification_uri` 是否已变成 LAN 地址
