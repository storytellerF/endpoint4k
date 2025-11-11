# endpoint4k

[![](https://jitpack.io/v/storytellerF/endpoint4k.svg)](https://jitpack.io/#storytellerF/endpoint4k)

`endpoint4k` 是一个为 [Ktor](https://ktor.io/) 设计的类型安全路由库。它允许您在服务器和客户端之间共享 API 定义，从而简化 Web 应用程序的开发，减少因 API 不匹配而导致的运行时错误。

## 特性

- **类型安全**: 在编译时捕捉路由错误，而不是在运行时。
- **代码共享**: 在 Ktor 服务器和客户端之间共享 API 定义。
- **简洁的语法**: 使用 Kotlin 的特性（如扩展函数和`invoke`操作符）来提供清晰、富有表现力的 API。
- **全面的支持**: 支持路径参数（Path）、查询参数（Query）、请求体（Body）以及常见的 HTTP 方法 (GET, POST, PUT, PATCH, DELETE)。

## 安装

首先，将 JitPack 仓库添加到您的根 `build.gradle.kts` 或 `settings.gradle.kts` 文件中：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https.jitpack.io") }
    }
}
```

然后，将以下依赖项添加到您的模块 `build.gradle.kts` 文件中：

```kotlin
val endpoint4kVersion = "1.0-SNAPSHOT"
// common
implementation("com.github.storytellerF.endpoint4k:common:$endpoint4kVersion")

// ktor server
implementation("com.github.storytellerF.endpoint4k:ktor-server:$endpoint4kVersion")

// ktor client
implementation("com.github.storytellerF.endpoint4k:ktor-client:$endpoint4kVersion")
```

## 使用示例

下面是一个完整的示例，演示了如何定义 API、在服务器上实现它以及在客户端上调用它。

### 1. 定义共享 API

首先，在一个共享模块中（例如 `commonMain`）定义您的 API 端点。这包括数据类和 API 路由本身。

```kotlin
import com.storyteller_f.endpoint4k.common.*
import kotlinx.serialization.Serializable

// --- 数据传输对象 (DTOs) ---
@Serializable
data class User(val id: Long, val name: String)

@Serializable
data class UserPath(val id: Long) // 用于路径参数 /users/{id}

@Serializable
data class UserQuery(val name: String) // 用于查询参数 ?name=...

@Serializable
data class CreateUserRequest(val name: String) // 用于请求体

// --- API 定义 ---
object UserApi {
    // GET /users/{id}?name=...
    val getUser = safeApiWithQueryAndPath<User, UserQuery, UserPath>("/users/{id}")

    // POST /users
    val createUser = mutationApi<User, CreateUserRequest>("/users")
}
```

### 2. 服务器端实现

在您的 Ktor 服务器上，使用 `invoke` 操作符来实现您定义的 API。

```kotlin
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.storyteller_f.endpoint4k.ktor.server.invoke // 导入 endpoint4k 的服务器端扩展
import com.storyteller_f.endpoint4k.ktor.server.receiveBody

fun Application.configureRouting() {
    routing {
        // 实现 GET /users/{id}?name=...
        UserApi.getUser.invoke({ result ->
            // 处理结果，成功或失败
            result.onSuccess { user ->
                if (user != null) call.respond(user) else call.respond(HttpStatusCode.NotFound)
            }.onFailure {
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "Error")
            }
        }) { query, path ->
            // 业务逻辑
            // query: UserQuery, path: UserPath
            runCatching {
                // 示例：返回一个用户，其名字包含查询和路径信息
                User(path.id, "Found user: ${query.name} with ID ${path.id}")
            }
        }

        // 实现 POST /users
        UserApi.createUser.invoke({ result ->
            result.onSuccess { user ->
                if (user != null) call.respond(HttpStatusCode.Created, user) else call.respond(HttpStatusCode.BadRequest)
            }.onFailure {
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "Error")
            }
        }) { api ->
            // 业务逻辑
            runCatching {
                val request = with(api) { receiveBody<CreateUserRequest>() }
                // 示例：创建一个新用户
                User(id = System.currentTimeMillis(), name = request.name)
            }
        }
    }
}
```

### 3. 客户端调用

在您的 Ktor 客户端，同样使用 `invoke` 操作符来安全地调用 API。

```kotlin
import io.ktor.client.*
import com.storyteller_f.endpoint4k.ktor.client.invoke

suspend fun main() {
    val client = HttpClient {
        // ... 客户端配置，例如 JSON 序列化
    }

    // 调用 GET /users/123?name=John
    val user = UserApi.getUser.invoke(
        query = UserQuery("John"),
        path = UserPath(123L)
    )
    println("Fetched User: $user")

    // 调用 POST /users
    val newUser = UserApi.createUser.invoke(
        body = CreateUserRequest("Jane Doe")
    ) {
        // 可以进行额外的请求配置
        contentType(ContentType.Application.Json)
    }
    println("Created User: $newUser")
}
```

通过这种方式，`endpoint4k` 确保了服务器和客户端之间的 API 调用在参数、路径和返回类型上都是一致和类型安全的。