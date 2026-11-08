# 项目指引（endpoint4k）

本文记录项目专属知识，帮助贡献者更快完成构建、测试和开发。

## 模块与布局

- 根项目为 Kotlin/JVM，包含以下子模块：
  - `common`：客户端与服务器共享的核心 API 定义和工具。
  - `ktor:client`：Ktor HTTP 客户端辅助方法，要求 Kotlin 上下文接收者。
  - `ktor:server`：Ktor 服务器路由辅助方法。
  - `ktor`：仅用于测试，集成客户端与服务器执行端到端风格测试。
  - `okhttp:client`：基于 OkHttp、对应 Ktor 客户端 API 的 HTTP 客户端辅助方法，要求 Kotlin 上下文接收者。
  - `okhttp`：仅用于 OkHttp 辅助方法测试，通过 MockWebServer 模拟服务器。
  - `http4k:client`：对应 Ktor/OkHttp 客户端 API 的 http4k HTTP 客户端辅助方法。
  - `http4k:server`：http4k 服务器路由辅助方法。
  - `http4k`：仅用于 http4k 辅助方法和路由测试，使用进程内 http4k 服务器。
- 源集：
  - 生产代码：`src/main/kotlin`
  - 测试：`src/test/kotlin`；测试资源位于 `src/test/resources`，例如 `ktor/src/test/resources/logback.xml`。

## 工具链与构建

- Kotlin：所有模块使用 2.1.21。需要 JSON 序列化功能的测试专用模块（`ktor` 和 `okhttp`）应用 2.2.0 版 `kotlinx-serialization` 插件。
- JDK：所有模块的工具链设置为 Java 21：
  - `kotlin { jvmToolchain(21) }`
- 构建系统：Gradle Kotlin DSL，仓库包含 Gradle Wrapper。
- 所有测试任务均启用 JUnit Platform：`tasks.test { useJUnitPlatform() }`。
- 编译器参数：
  - `ktor:client`、`okhttp:client` 及调用这些辅助方法的测试使用上下文接收者；各相应模块通过 `freeCompilerArgs.add("-Xcontext-parameters")` 启用。
- 序列化与依赖：
  - Ktor 模块使用 Ktor 3.1.3 产物。
  - OkHttp 客户端与测试使用 OkHttp 4.12.0。
  - http4k 客户端、服务器与测试使用 http4k 5.30.0.0。
  - 各模块统一使用 kotlinx-serialization-json 1.9.0。

### 命令（Windows PowerShell/CMD）

- 构建全部模块：
  - `./gradlew.bat build`
- 运行全仓库测试：
  - `./gradlew.bat test`
- 运行指定模块测试：
  - Common：`./gradlew.bat :common:test`
  - Ktor 客户端：`./gradlew.bat :ktor:ktor-client:test`
  - Ktor 服务器：`./gradlew.bat :ktor:ktor-server:test`
  - Ktor 测试模块：`./gradlew.bat :ktor:test`
  - OkHttp 客户端：`./gradlew.bat :okhttp:okhttp-client:test`
  - OkHttp 测试模块：`./gradlew.bat :okhttp:test`
  - http4k 客户端：`./gradlew.bat :http4k:http4k-client:test`
  - http4k 服务器：`./gradlew.bat :http4k:http4k-server:test`
  - http4k 测试模块：`./gradlew.bat :http4k:test`
- 运行指定测试类或方法，使用 JUnit Platform 的 `--tests` 过滤：
  - Ktor 类：`./gradlew.bat :ktor:test --tests "KtorRouteTest"`
  - OkHttp 类：`./gradlew.bat :okhttp:test --tests "OkHttpRouteTest"`
  - http4k 类：`./gradlew.bat :http4k:test --tests "Http4kRouteTest"`
  - 单个方法示例：`./gradlew.bat :ktor:test --tests "KtorRouteTest.test get route"`

说明：

- Unix shell 中将 `gradlew.bat` 替换为 `./gradlew`。
- 可直接在 IDE（IntelliJ IDEA）中运行测试，确保 Project SDK 为 JDK 21。

## 测试指引

- Ktor 路径：
  - `ktor` 模块包含完整的集成风格测试，可参考 `ktor/src/test/kotlin/KtorRouteTest.kt`：
    - 使用 `io.ktor.server.testing.testApplication { ... }` 启动内存内 Ktor 服务器。
    - 通过 `ktor:client` 的辅助方法和上下文接收者调用客户端；测试中导入 `com.storyteller_f.endpoint4k.ktor.client.invoke as invoke2` 消除歧义。
    - 测试的客户端与服务器都通过 `kotlinx.serialization`（JSON）配置内容协商。
  - 测试日志：`ktor/src/test/resources/logback.xml` 配置测试运行的 Logback。
- OkHttp 路径：
  - `okhttp` 模块包含 OkHttp 客户端集成风格测试，见 `okhttp/src/test/kotlin/OkHttpRouteTest.kt`：
    - 使用 `MockWebServer` 和自定义 `Dispatcher` 模拟 GET/POST/DELETE，以及路径与查询变体的端点。
    - 通过 `okhttp:client` 的辅助方法和上下文接收者调用客户端：`context(OkHttpClient) { api.invoke(...) }`。
    - OkHttp 辅助方法通过 kotlinx-serialization 编解码 JSON；修改型 API 的请求体自动编码为 `application/json`。
- http4k 路径：
  - `http4k` 模块包含 http4k 客户端与服务器的集成风格测试，见 `http4k/src/test/kotlin/Http4kRouteTest.kt`：
    - 使用进程内 http4k 路由模拟 GET/POST/DELETE，以及路径与查询变体的端点。
    - 通过 `http4k:client` 辅助方法调用客户端，不使用上下文接收者；辅助方法构造 http4k Request 并解码 Response。
    - http4k 辅助方法通过 kotlinx-serialization 编解码 JSON；修改型 API 的请求体自动编码为 `application/json`。

### 添加新测试

1. 选择要验证行为的模块。
   - Ktor 端到端路由测试优先放在 `ktor` 模块。
   - 针对模拟服务器的 OkHttp 客户端行为，在 `okhttp` 下使用 MockWebServer 添加测试。
   - http4k 进程内客户端／服务器测试，添加到 `http4k` 模块。
2. 在 `src/test/kotlin` 下创建测试。最小示例：

   ```kotlin
   class SimpleGuidelinesTest {
       fun smokeTest() {
           kotlin.test.assertTrue(true)
       }
   }
   ```

3. 仅运行该测试：
   - `./gradlew.bat :ktor:test --tests "SimpleGuidelinesTest"`
4. Ktor 路由测试参照 `KtorRouteTest.kt` 结构：
   - 通过 `common` 的 `safeEndpoint`、`mutationEndpoint` 及其 `WithPath/WithQuery` 变体定义路由 API。
   - 使用 `ktor:server` 的服务器辅助方法（`invoke`、`receiveBody`）。
   - 使用 `ktor:client` 的客户端辅助方法（上下文接收者为 `HttpClient`），按需设置内容类型或请求体。
5. OkHttp 路由测试参照 `OkHttpRouteTest.kt` 结构：
   - 通过 `common` 的 `safeEndpoint`、`mutationEndpoint` 及其 `WithPath/WithQuery` 变体定义路由 API。
   - 使用 `MockWebServer` 提供端点，通过 `server.url("/path")` 构造 URL。
   - 使用 `okhttp:client` 的客户端辅助方法（上下文接收者为 `OkHttpClient`），可在请求构建器 lambda 参数中添加可选请求头。
6. http4k 路由测试参照 `Http4kRouteTest.kt` 结构：
   - 通过 `common` 的 `safeEndpoint`、`mutationEndpoint` 及其 `WithPath/WithQuery` 变体定义路由 API。
   - 通过 `http4k:server` 的服务器辅助方法（`invoke`、`receiveBody`）使用进程内 http4k 路由。
   - 使用 `http4k:client` 的客户端辅助方法（没有上下文接收者），可在构建器 lambda 中为 http4k Request 设置可选请求头。

## 开发说明

- 上下文接收者：客户端与服务器 API 用它保持调用简洁。确保定义或调用辅助方法的模块（`ktor:client`、`okhttp:client` 及使用它们的测试模块）设置 `-Xcontext-parameters`。
- 版本对齐：
  - Ktor 产物固定为 `3.1.3`，保持客户端、服务器和测试依赖一致，避免运行时不匹配。
  - OkHttp 固定为 `4.12.0`。
  - Kotlinx Serialization JSON 为 `1.9.0`，应与 Kotlin／序列化插件对齐，一起更新。
- 发布：`common`、`ktor:client` 和 `okhttp:client` 应用自定义 `common-publish` 插件（`common-publish/src/main/kotlin/common-publish.gradle.kts`）。发布或修改坐标时，检查这些脚本和插件逻辑。
- API URL 拼接：路径参数经编码器编码，再替换到 `/user/{id}` 等模板中。新增占位符须匹配 `path` 参数数据类的属性名。
- 查询编码：查询类编码为多值参数。Ktor 与 OkHttp 客户端的编码器兼容；OkHttp 客户端保留本地副本，避免跨模块依赖。
- JSON 请求体：
  - Ktor：修改型 API 通过 `setBody(body)` 设置请求体，必要时指定 `ContentType.Application.Json`。
  - OkHttp：修改辅助方法自动将请求体序列化为 JSON 并设置 `application/json`；仍可通过提供的 `Request.Builder` lambda 添加请求头。

## 验证说明

本指引不声明实时测试结果。请使用“命令”部分在本地或 CI 运行相关模块或完整测试套件。

Kotlin、Ktor、OkHttp、http4k 或序列化版本过时时，更新受影响 `build.gradle.kts` 中的版本，并重新运行 `./gradlew.bat build` 验证。

## 静态分析（Detekt）

遵循 [DEVELOPMENT.md 中的 Detekt 流程](DEVELOPMENT.md#静态分析detekt)。普通检查只读，可与编译同时运行。只有显式 `--auto-correct` 运行必须与同一源码的编译或分析分开；修正源码后重新运行检查。

## 实现代码放置规则

- 所有生产实现必须放在专门负责客户端或服务器逻辑的子模块：
  - Ktor 客户端：ktor:client（src/main/kotlin）
  - Ktor 服务器：ktor:server（src/main/kotlin）
  - OkHttp 客户端：okhttp:client（src/main/kotlin）
  - http4k 客户端：http4k:client（src/main/kotlin）
  - http4k 服务器：http4k:server（src/main/kotlin）
- 测试专用模块：
  - 每套技术栈根部的 ktor、okhttp 和 http4k 仅作为测试框架模块，不得包含生产源码。这些模块的集成和 E2E 测试只使用 src/test/kotlin 和 src/test/resources。
- 不要将功能实现放入测试模块的 src/main。新功能放在合适的客户端或服务器子模块，在对应测试模块的 src/test 下覆盖。
- 新增辅助方法时：
  - 共享 API 定义和类型放在 common。
  - 客户端专用调用辅助方法放在各自的 *:client* 模块。
  - 服务器路由接线只放在 ktor:server 和 http4k:server。

如果有需要，可以随时更改这个文件
