
<div align="center">
<h1>HookSMS</h1>

<a href="https://github.com/IamRuaRua/HookSMS/releases"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/IamRuaRua/HookSMS/total?label=Downloads"></a>
<a href="https://github.com/IamRuaRua/HookSMS/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/IamRuaRua/HookSMS"></a>
<a href="https://github.com/IamRuaRua/HookSMS/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/IamRuaRua/HookSMS"></a>
<a href="https://t.me/HookSMSChannel"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-频道-blue.svg?logo=telegram"></a>

<p>在设备接收到短信时自动将其转发到外部消息服务(QQ NapCat、飞书机器人或自定义 HTTP 端点)的Xposed模块</p>
</div>  


## 功能特性

- **实时拦截短信**:Hook 系统 `com.android.mms` 应用的短信接收流程,自动获取发件人、内容和时间戳
- **多渠道转发**:支持以下三种发送方式,并可自由扩展
  - **QQ NapCat** —— 通过 OneBot HTTP API 发送群消息
  - **飞书机器人** —— 通过 Webhook 推送文本消息
  - **自定义 HTTP** —— 完全自定义 URL、请求方法、请求头和请求体模板
- **内置测试连接**:在配置界面一键测试发送渠道连通性,带详细日志
- **日志查看**:集成日志面板,支持查看历史日志并一键清除

## 环境要求

| 项目           | 要求                           |
|--------------|------------------------------|
| Root 框架      | 已安装 LSPosed(或兼容的 Xposed 框架)  |
| 目标应用         | 系统 `com.android.mms`(原生短信应用) |
| minXposedAPI | API 93                       |

## 安装

1 在 LSPosed 管理器中启用本模块
2. 在模块作用域中勾选 **信息**(`com.android.mms`)应用
3. 重启系统短信应用使 Hook 生效

## 配置说明

启用模块后,可在 LSPosed 管理器中打开 **模块配置**,或直接启动 HookSMS 应用进入配置界面。

### 1. 选择发送渠道

在「配置」标签页顶部的下拉框中选择发送方式。

### 2. QQ NapCat 配置

| 字段 | 说明 | 示例 |
|------|------|------|
| 服务器 | NapCat HTTP 服务地址(IP:端口) | `127.0.0.1:3000` |
| Token | NapCat 鉴权 Token | `your-token-here` |
| 群号 | 目标 QQ 群号 | `123456789` |

API 端点:`POST http://{server}/send_group_msg`
请求头:`Authorization: Bearer {token}`

### 3. 飞书机器人配置

| 字段 | 说明 | 示例 |
|------|------|------|
| Webhook URL | 飞书自定义机器人 Webhook 地址 | `https://open.feishu.cn/open-apis/bot/v2/hook/xxxxx` |

### 4. 自定义 HTTP 配置

| 字段 | 说明 | 示例 |
|------|------|------|
| 请求 URL | 目标接口地址 | `https://example.com/api/sms` |
| 请求方法 | GET 或 POST | `POST` |
| 请求头 | 格式:`Key:Value;Key2:Value2` | `Content-Type:application/json` |
| 数据模板 | 使用 `$text` 占位符表示消息内容 | `{"msg":"$text"}` |

> GET 请求会将 `$text` URL 编码后拼接到 URL 中;POST 请求会将 `$text` 替换到模板中作为请求体发送。

### 5. 测试连接

点击「测试连接」按钮可向当前配置的发送渠道发送一条测试消息,结果会显示「日志」标签页中。

## 编译构建

```bash
# 克隆仓库
git clone https://github.com/IamRuaRua/HookSMS.git
cd HookSMS

# 使用 Gradle 构建
./gradlew assembleRelease
```


## 开源协议

本项目基于 [GPL-3.0](LICENSE) 协议开源。

- 你可以自由使用、修改和分发本项目
- 任何基于本项目的衍生作品必须同样以 GPL-3.0 协议开源
- 使用 Xposed API 部分遵循其各自的开源协议

## 免责声明

本模块仅用于合法的通知转发和自动化场景。使用者需确保遵守所在地区的法律法规,不得用于侵犯他人隐私或非法监控用途。作者不对任何滥用行为承担责任。
