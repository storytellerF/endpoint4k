# Project Guidelines (endpoint4k)

This document captures project-specific knowledge to speed up builds, testing, and development for contributors.

## Modules and Layout
- Root project (Kotlin/JVM) with submodules:
  - `common`: Core API definitions and utilities shared by client/server.
  - `ktor:client`: Ktor HTTP client-side helpers. Requires Kotlin context receivers.
  - `ktor:server`: Ktor server-side route helpers.
  - `ktor`: Test-only module integrating both client and server for end-to-end style tests.
  - `okhttp:client`: OkHttp-based HTTP client-side helpers mirroring the Ktor client APIs. Requires Kotlin context receivers.
  - `okhttp`: Test-only module for OkHttp helpers, using MockWebServer to simulate a server.
  - `http4k:client`: http4k HTTP client-side helpers mirroring the Ktor/OkHttp client APIs.
  - `http4k:server`: http4k server-side route helpers.
  - `http4k`: Test-only module for http4k helpers and routing, using in-process http4k server.
- Source sets:
  - Production: `src/main/kotlin`
  - Tests: `src/test/kotlin` and test resources in `src/test/resources` (e.g., `ktor/src/test/resources/logback.xml`).

## Toolchain and Build
- Kotlin: 2.1.21 in all modules. Test-only modules that need JSON serialization features apply the `kotlinx-serialization` plugin 2.2.0 (`ktor` and `okhttp`).
- JDK: Toolchain set to Java 21 in all modules:
  - `kotlin { jvmToolchain(21) }`
- Build system: Gradle Kotlin DSL with Gradle Wrapper checked in.
- JUnit Platform is enabled in all test tasks: `tasks.test { useJUnitPlatform() }`.
- Compiler flags:
  - Context receivers are used in `ktor:client`, `okhttp:client`, and tests that call those helpers; enabled via `freeCompilerArgs.add("-Xcontext-parameters")` in those modules.
- Serialization / Dependencies:
  - Ktor 3.1.3 artifacts are used in Ktor modules.
  - OkHttp 4.12.0 is used in OkHttp client and tests.
  - http4k 5.30.0.0 is used for http4k client/server and tests.
  - kotlinx-serialization-json 1.9.0 across modules.

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
  - OkHttp client: `./gradlew.bat :okhttp:client:test`
  - OkHttp tests module: `./gradlew.bat :okhttp:test`
  - http4k client: `./gradlew.bat :http4k:client:test`
  - http4k server: `./gradlew.bat :http4k:server:test`
  - http4k tests module: `./gradlew.bat :http4k:test`
- Run a specific test class or method (use JUnit Platform `--tests` filter):
  - Ktor class: `./gradlew.bat :ktor:test --tests "KtorRouteTest"`
  - OkHttp class: `./gradlew.bat :okhttp:test --tests "OkHttpRouteTest"`
  - http4k class: `./gradlew.bat :http4k:test --tests "Http4kRouteTest"`
  - Single method (example): `./gradlew.bat :ktor:test --tests "KtorRouteTest.test get route"`

Notes:
- On Unix shells, replace `gradlew.bat` with `./gradlew`.
- Running tests in IDE (IntelliJ IDEA) works out of the box. Ensure Project SDK is set to JDK 21.

## Testing Guidance
- Ktor path:
  - The `ktor` module contains comprehensive integration-style tests. See `ktor/src/test/kotlin/KtorRouteTest.kt` for patterns:
    - Uses `io.ktor.server.testing.testApplication { ... }` to spin up an in-memory Ktor server.
    - Client calls are made via the helpers in `ktor:client` using context receivers (import `com.storyteller_f.endpoint4k.ktor.client.invoke as invoke2` for disambiguation in tests).
    - Content negotiation is set up via `kotlinx.serialization` (JSON) for both client and server in tests.
  - Logging in tests: `ktor/src/test/resources/logback.xml` configures Logback for test runs.
- OkHttp path:
  - The `okhttp` module hosts integration-style tests for the OkHttp client. See `okhttp/src/test/kotlin/OkHttpRouteTest.kt`:
    - Uses `MockWebServer` with a custom `Dispatcher` to simulate endpoints for GET/POST/DELETE and path/query variants.
    - Client calls are made via the helpers in `okhttp:client` using context receivers: `context(OkHttpClient) { api.invoke(...) }`.
    - JSON encoding/decoding is done via kotlinx-serialization in the OkHttp helpers; request bodies are encoded to `application/json` automatically for mutation APIs.
- http4k path:
  - The `http4k` module hosts integration-style tests for the http4k client/server. See `http4k/src/test/kotlin/Http4kRouteTest.kt`:
    - Uses in-process http4k routing to simulate endpoints for GET/POST/DELETE and path/query variants.
    - Client calls are made via the helpers in `http4k:client` without context receivers; helpers build http4k Requests and decode Responses.
    - JSON encoding/decoding is done via kotlinx-serialization in the http4k helpers; request bodies are encoded to `application/json` automatically for mutation APIs.

### Adding a New Test
1. Pick the module whose behavior you’re verifying.
   - For end-to-end route testing with Ktor, prefer the `ktor` module.
   - For OkHttp client behavior against a simulated server, add tests under `okhttp` using MockWebServer.
   - For in-process client/server tests with http4k, add tests under the `http4k` module.
2. Create a test under `src/test/kotlin`. Example minimal test:
   
   ```kotlin
   class SimpleGuidelinesTest {
       fun smokeTest() {
           kotlin.test.assertTrue(true)
       }
   }
   ```
3. Run only that test:
   - `./gradlew.bat :ktor:test --tests "SimpleGuidelinesTest"`
4. For Ktor route tests, mirror the structure in `KtorRouteTest.kt`:
   - Define route APIs via `safeEndpoint`, `mutationEndpoint`, and their `WithPath/WithQuery` variants from `common`.
   - Use server-side helpers from `ktor:server` (`invoke`, `receiveBody`).
   - Use client-side helpers from `ktor:client` (context receiver `HttpClient`), and set content type or body as needed.
5. For OkHttp route tests, mirror the structure in `OkHttpRouteTest.kt`:
  - Define route APIs via `safeEndpoint`, `mutationEndpoint`, and their `WithPath/WithQuery` variants from `common`.
  - Use `MockWebServer` to provide endpoints; build URLs from `server.url("/path")`.
  - Use client-side helpers from `okhttp:client` (context receiver `OkHttpClient`). Optional headers can be added in the request builder lambda parameter.
6. For http4k route tests, mirror the structure in `Http4kRouteTest.kt`:
  - Define route APIs via `safeEndpoint`, `mutationEndpoint`, and their `WithPath/WithQuery` variants from `common`.
  - Use in-process http4k routing via the server helpers in `http4k:server` (`invoke`, `receiveBody`).
  - Use client-side helpers from `http4k:client` (no context receiver). Optional headers can be set on the http4k Request in the builder lambda.

## Development Notes
- Context receivers: Client/server APIs use context receivers to keep call sites concise. Ensure `-Xcontext-parameters` is set in modules that define or call these helpers (`ktor:client`, `okhttp:client`, and test modules that use them).
- Version alignment:
  - Ktor artifacts are pinned to `3.1.3`. Keep client/server/test dependencies aligned to avoid runtime mismatches.
  - OkHttp is pinned to `4.12.0`.
  - Kotlinx Serialization JSON is `1.9.0` and should align with the Kotlin/serialization plugin; update versions in tandem.
- Publishing: `common`, `ktor:client`, and `okhttp:client` apply a custom `common-publish` plugin (`common-publish/src/main/kotlin/common-publish.gradle.kts`). If publishing or changing coordinates, review those scripts and plugin logic.
- API URL composition: Path parameters are encoded via an encoder and substituted into templates like `/user/{id}`. Ensure any new placeholders match the data class property names of the `path` parameter.
- Query encoding: Query classes are encoded into multi-valued parameters. Both Ktor and OkHttp clients have compatible encoders (OkHttp client carries a local copy to avoid cross-module dependencies).
- JSON payloads:
  - Ktor: For mutation APIs, set the request body with `setBody(body)` and specify content type when necessary (`ContentType.Application.Json`).
  - OkHttp: Mutation helpers serialize the body to JSON and set `application/json` automatically; you can still add headers via the provided `Request.Builder` lambda.

## Verification Notes
This guideline doesn’t assert live test results. Use the Commands section to run the relevant module or full test suites locally/CI.

If anything becomes outdated (Kotlin, Ktor, OkHttp, http4k, or serialization versions), update the version in the affected `build.gradle.kts` files and re-run `./gradlew.bat build` to validate.

## Static Analysis (Detekt)
Detekt is enabled across all modules and runs as part of the Gradle build.

- Run detekt on the whole project:
  - `./gradlew.bat detekt`
- Run detekt for a single module (example):
  - `./gradlew.bat :ktor:client:detekt`
- Auto-correction: The build is configured with `autoCorrect=true`. Detekt will automatically fix issues it can correct (mostly formatting via the formatting ruleset). These auto-correctable issues are ignored as failures; only remaining findings (non-auto-correctable) will fail the build.
- Config file: Rules are configured via `config/detekt/detekt.yml` at the root. Adjust rule severities or disable rules there if needed for the project.
- Reports: XML, HTML, TXT, SARIF, and MD reports are generated under each module’s `build/reports/detekt/`. A merged SARIF is also produced at `build/reports/detekt/merge.sarif` at the root for CI code scanning.
- Fix remaining issues: If detekt reports non-auto-correctable findings, fix them manually in code and re-run `./gradlew.bat detekt` until clean.


## Implementation Placement Policy
- All production (implementation) code must reside in submodules dedicated to client or server logic:
  - Ktor client: ktor:client (src/main/kotlin)
  - Ktor server: ktor:server (src/main/kotlin)
  - OkHttp client: okhttp:client (src/main/kotlin)
  - http4k client: http4k:client (src/main/kotlin)
  - http4k server: http4k:server (src/main/kotlin)
- Test-only modules:
  - ktor, okhttp, and http4k at the root of each stack are test harness modules only. They must not contain production sources. Use only src/test/kotlin (and src/test/resources) in these modules for integration/e2e tests.
- Do not place feature implementations under the test modules’ src/main. Keep all new functionality in the appropriate client/server submodule and cover it with tests under the corresponding test module’s src/test.
- When adding new helpers:
  - Shared API definitions and types live in common.
  - Client-specific calling helpers belong to the respective *:client* module.
  - Server-side route wiring belongs to ktor:server and http4k:server only.
