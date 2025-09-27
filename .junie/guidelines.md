# Project Guidelines (route4k)

This document captures project-specific knowledge to speed up builds, testing, and development for contributors.

## Modules and Layout
- Root project (Kotlin/JVM) with submodules:
  - `common`: Core API definitions and utilities shared by client/server.
  - `ktor:client`: Ktor HTTP client-side helpers. Requires Kotlin context receivers.
  - `ktor:server`: Ktor server-side route helpers.
  - `ktor`: Test-only module integrating both client and server for end-to-end style tests.
- Source sets:
  - Production: `src/main/kotlin`
  - Tests: `src/test/kotlin` and test resources in `src/test/resources` (e.g., `ktor/src/test/resources/logback.xml`).

## Toolchain and Build
- Kotlin: 2.1.21 in all modules (server test module additionally applies `kotlinx-serialization` plugin 2.2.0).
- JDK: Toolchain set to Java 21 in all modules:
  - `kotlin { jvmToolchain(21) }`
- Build system: Gradle Kotlin DSL with Gradle Wrapper checked in.
- JUnit Platform is enabled in all test tasks: `tasks.test { useJUnitPlatform() }`.
- Compiler flags:
  - Context receivers are used in `ktor:client` and enabled via `freeCompilerArgs.add("-Xcontext-parameters")`.
- Serialization:
  - Ktor 3.1.3 artifacts are used in Ktor modules.
  - kotlinx-serialization-json 1.9.0.

### Commands (Windows PowerShell/CMD)
- Build all:
  - `./gradlew.bat build`
- Run all tests for the whole repo:
  - `./gradlew.bat test`
- Run tests for a specific module:
  - Common: `./gradlew.bat :common:test`
  - Ktor client: `./gradlew.bat :ktor:client:test`
  - Ktor server: `./gradlew.bat :ktor:server:test`
  - Ktor tests module: `./gradlew.bat :ktor:test`
- Run a specific test class or method (use JUnit Platform `--tests` filter):
  - Class: `./gradlew.bat :ktor:test --tests "KtorRouteTest"`
  - Single method: `./gradlew.bat :ktor:test --tests "KtorRouteTest.test get route"`

Notes:
- On Unix shells, replace `gradlew.bat` with `./gradlew`.
- Running tests in IDE (IntelliJ IDEA) works out of the box. Ensure Project SDK is set to JDK 21.

## Testing Guidance
- The `ktor` module contains comprehensive integration-style tests. See `ktor/src/test/kotlin/KtorRouteTest.kt` for patterns:
  - Uses `io.ktor.server.testing.testApplication { ... }` to spin up an in-memory Ktor server.
  - Client calls are made via the helpers in `ktor:client` using context receivers (import `com.storyteller_f.route4k.ktor.client.invoke as invoke2` for disambiguation in tests).
  - Content negotiation is set up via `kotlinx.serialization` (JSON) for both client and server in tests.
- Logging in tests: `ktor/src/test/resources/logback.xml` configures Logback for test runs.

### Adding a New Test
1. Pick the module whose behavior you’re verifying. For end-to-end route testing, prefer the `ktor` module.
2. Create a test under `src/test/kotlin`. Example minimal test:
   
   ```kotlin
   import kotlin.test.Test
   import kotlin.test.assertTrue
   
   class SimpleGuidelinesTest {
       @Test
       fun `smoke test`() {
           assertTrue(true)
       }
   }
   ```
3. Run only that test:
   - `./gradlew.bat :ktor:test --tests "SimpleGuidelinesTest"`
4. For Ktor route tests, mirror the structure in `KtorRouteTest.kt`:
   - Define route APIs via `safeApi`, `mutationApi`, and their `WithPath/WithQuery` variants from `common`.
   - Use server-side helpers from `ktor:server` (`invoke`, `receiveBody`).
   - Use client-side helpers from `ktor:client` (context receiver `HttpClient`), and set content type or body as needed.

## Development Notes
- Context receivers: Several client/server APIs are designed with context receivers to keep call sites concise. This requires the `-Xcontext-parameters` compiler flag (already configured in `ktor:client` and `ktor` modules). If you add new modules that use context receivers, remember to add the same flag in their `kotlin { compilerOptions { ... } }` block.
- Version alignment:
  - Ktor artifacts are pinned to `3.1.3`. Keep client/server/test dependencies aligned to avoid runtime mismatches.
  - Kotlinx Serialization JSON is `1.9.0` and must match the Kotlin/serialization plugin sufficiently; update versions in tandem.
- Publishing: `common` and `ktor:client` apply a custom `common-publish` plugin (`common-publish/src/main/kotlin/common-publish.gradle.kts`). There’s also a `jitpack-publish.sh` helper script. If publishing or changing coordinates, review those scripts and plugin logic.
- API URL composition: Path parameters are encoded via `encodeQueryParams` and substituted into templates like `/user/{id}`. Ensure any new placeholders match the data class property names of the `path` parameter.
- Query encoding: Query classes are encoded into multi-valued parameters. When adding new types, ensure they are serializable or encodable by `CustomParameterEncoder` on the client side.
- JSON payloads: For mutation APIs, set the request body with `setBody(body)` and specify content type when necessary (`ContentType.Application.Json`).

## Verified Steps This Session
- Executed the full `ktor` test suite: all 8 tests passed.
- Added a temporary smoke test under `ktor/src/test/kotlin`, executed it successfully, and removed it afterward to keep the repo clean.

If anything becomes outdated (Kotlin, Ktor, or serialization versions), update the version in the affected `build.gradle.kts` files and re-run `./gradlew.bat build` to validate.
