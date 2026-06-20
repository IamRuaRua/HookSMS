# HookSMS 发送器架构文档

## 当前架构

本项目采用**策略模式 + 工厂模式**，支持 QQ NapCat 和自定义 HTTP 发送。

### 核心组件

```
sender/
├── SmsSender.kt              # 发送器接口
├── SenderType.kt             # 发送器类型枚举（QQ NapCat、自定义HTTP）
├── AbstractHttpSender.kt     # HTTP发送器抽象基类
├── SenderFactory.kt          # 发送器工厂
├── QQNapCatSender.kt         # QQ NapCat 发送器实现
└── CustomHttpSender.kt       # 自定义HTTP发送器实现

message/
└── MessageFormatter.kt       # 统一消息格式管理

config/
├── SenderConfig.kt           # 配置基类和具体配置类
└── SenderConfigManager.kt    # 配置管理器
```

## 已实现的发送器

### QQ NapCat
- **配置**：服务器地址、Token、群号
- **发送方式**：HTTP POST 到 NapCat API
- **消息格式**：统一通过 MessageFormatter

### 自定义 HTTP
- **配置**：URL、HTTP方法、Headers、Data模板
- **发送方式**：自定义 HTTP POST 或 GET 请求
- **消息格式**：使用 `$text` 占位符表示短信内容，由 MessageFormatter 格式化后替换
- **特点**：
  - 支持自定义请求头（格式：Key:Value;Key2:Value2）
  - 支持自定义请求体模板
  - GET 请求：将 `$text` URL 编码后替换到 URL 中
  - POST 请求：将 `$text` 直接替换到请求体中

## 未来扩展计划

如需添加新的发送方式，只需：

1. **创建发送器类**：继承 `AbstractHttpSender` 并实现 `SmsSender`
2. **添加类型枚举**：在 `SenderType.kt` 中添加
3. **在工厂注册**：在 `SenderFactory.create()` 中添加
4. **创建配置类**：继承 `SenderConfig` 并实现相应方法

## 优势

✅ **易扩展**：添加新发送方式只需实现接口
✅ **易维护**：代码组织清晰，只保留已实现的功能
✅ **格式统一**：所有消息格式通过 MessageFormatter 统一管理
✅ **配置简单**：每个发送器独立配置
✅ **类型安全**：使用 Kotlin 类型系统
✅ **灵活定制**：自定义 HTTP 支持任意 API

## 当前使用方式

### QQ NapCat
```kotlin
// 在 XposedEntry 中
val data = SmsData(
    originatingAddress = phoneNumber,
    messageBody = smsContent,
    timestamp = timestamp
)

val result = sender.send(lpparam, data)
```

### 自定义 HTTP
配置示例：
- **URL**: `http://example.com/api/sms`
- **方法**: POST
- **Headers**: `Content-Type:application/json;Authorization:Bearer xxx`
- **Data**: `{"text":"$text","from":"sms"}`

所有短信都通过统一的 `MessageFormatter` 格式化，修改消息格式只需修改一处。
