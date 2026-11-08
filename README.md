# endpoint4k

`endpoint4k` 是面向 [Ktor](https://ktor.io/) 的类型安全路由库。它让服务器与客户端共享 API 定义，简化 Web 应用开发，减少 API 不一致导致的运行时错误。

## 功能

- **类型安全**：在编译期发现路由错误。
- **代码共享**：在 Ktor 服务器与客户端之间共享 API 定义。
- **语法简洁**：利用 Kotlin 扩展函数和 `invoke` 运算符等特性，提供清楚且富有表达力的 API。
- **支持全面**：支持路径参数、查询参数、请求体及常见 HTTP 方法（GET、POST、PUT、PATCH、DELETE）。

## 安装

将以下依赖添加到模块的 `build.gradle.kts`：

```kotlin
val endpoint4kVersion = "1.1.0"
// common
implementation("io.github.storytellerf:endpoint4k-common:$endpoint4kVersion")

// ktor server
implementation("io.github.storytellerf:endpoint4k-ktor-server:$endpoint4kVersion")

// ktor client
implementation("io.github.storytellerf:endpoint4k-ktor-client:$endpoint4kVersion")
```

## 使用示例

以下完整示例展示如何定义 API、在服务器实现并从客户端调用。

### 1. 定义共享 API

首先在共享模块（例如 `commonMain`）中定义 API 端点，包括数据类和 API 路由本身。

```kotlin
import com.storyteller_f.endpoint4k.common.*
import kotlinx.serialization.Serializable

// --- Data Transfer Objects (DTOs) ---
@Serializable
data class User(val id: Long, val name: String)

@Serializable
data class UserPath(val id: Long) // For path parameter /users/{id}

@Serializable
data class UserQuery(val name: String) // For query parameter ?name=...

@Serializable
data class CreateUserRequest(val name: String) // For the request body

// --- API Definition ---
object UserApi {
    // GET /users/{id}?name=...
    val getUser = safeEndpoinkWithQueryAndPath<User, UserQuery, UserPath>("/users/{id}")

    // POST /users
    val createUser = mutationEndpoint<User, CreateUserRequest>("/users")
}
```

### 2. 服务器实现

在 Ktor 服务器中，使用 `invoke` 运算符实现定义好的 API。

```kotlin
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.storyteller_f.endpoint4k.ktor.server.invoke // Import endpoint4k's server-side extensions
import com.storyteller_f.endpoint4k.ktor.server.receiveBody

fun Application.configureRouting() {
    routing {
        // Implement GET /users/{id}?name=...
        UserApi.getUser.invoke({ result ->
            // Handle the result, success or failure
            result.onSuccess { user ->
                if (user != null) call.respond(user) else call.respond(HttpStatusCode.NotFound)
            }.onFailure {
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "Error")
            }
        }) { query, path ->
            // Business logic
            // query: UserQuery, path: UserPath
            runCatching {
                // Example: return a user whose name includes query and path info
                User(path.id, "Found user: ${query.name} with ID ${path.id}")
            }
        }

        // Implement POST /users
        UserApi.createUser.invoke({ result ->
            result.onSuccess { user ->
                if (user != null) call.respond(HttpStatusCode.Created, user) else call.respond(HttpStatusCode.BadRequest)
            }.onFailure {
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "Error")
            }
        }) { api ->
            // Business logic
            runCatching {
                val request = with(api) { receiveBody<CreateUserRequest>() }
                // Example: create a new user
                User(id = System.currentTimeMillis(), name = request.name)
            }
        }
    }
}
```

### 3. 客户端调用

在 Ktor 客户端中，同样使用 `invoke` 运算符安全地调用 API。

```kotlin
import io.ktor.client.*
import com.storyteller_f.endpoint4k.ktor.client.invoke

suspend fun main() {
    val client = HttpClient {
        // ... client configuration, e.g., JSON serialization
    }

    // Call GET /users/123?name=John
    val user = UserApi.getUser.invoke(
        query = UserQuery("John"),
        path = UserPath(123L)
    )
    println("Fetched User: $user")

    // Call POST /users
    val newUser = UserApi.createUser.invoke(
        body = CreateUserRequest("Jane Doe")
    ) {
        // You can add extra request configuration here
        contentType(ContentType.Application.Json)
    }
    println("Created User: $newUser")
}
```

这样，`endpoint4k` 能确保服务器与客户端在参数、路径和返回类型上的 API 调用一致且类型安全。
