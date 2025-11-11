# endpoint4k

[![](https://jitpack.io/v/storytellerF/endpoint4k.svg)](https://jitpack.io/#storytellerF/endpoint4k)

`endpoint4k` is a type-safe routing library for [Ktor](https://ktor.io/). It allows you to share API definitions between your server and client, simplifying the development of web applications and reducing runtime errors caused by API mismatches.

## Features

- **Type-Safe**: Catch routing errors at compile time, not at runtime.
- **Code Sharing**: Share API definitions between your Ktor server and client.
- **Concise Syntax**: Utilizes Kotlin's features, like extension functions and the `invoke` operator, to provide a clean and expressive API.
- **Comprehensive Support**: Supports path parameters, query parameters, request bodies, and common HTTP methods (GET, POST, PUT, PATCH, DELETE).

## Installation

First, add the JitPack repository to your root `build.gradle.kts` or `settings.gradle.kts` file:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https.jitpack.io") }
    }
}
```

Then, add the following dependencies to your module's `build.gradle.kts` file:

```kotlin
val endpoint4kVersion = "1.0-SNAPSHOT"
// common
implementation("com.github.storytellerF.endpoint4k:common:$endpoint4kVersion")

// ktor server
implementation("com.github.storytellerF.endpoint4k:ktor-server:$endpoint4kVersion")

// ktor client
implementation("com.github.storytellerF.endpoint4k:ktor-client:$endpoint4kVersion")
```

## Usage Example

Here is a complete example demonstrating how to define an API, implement it on the server, and call it from the client.

### 1. Define the Shared API

First, define your API endpoints in a shared module (e.g., `commonMain`). This includes data classes and the API routes themselves.

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

### 2. Server-Side Implementation

On your Ktor server, use the `invoke` operator to implement your defined API.

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

### 3. Client-Side Call

In your Ktor client, also use the `invoke` operator to safely call the API.

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

In this way, `endpoint4k` ensures that the API calls between the server and client are consistent and type-safe in terms of parameters, paths, and return types.
