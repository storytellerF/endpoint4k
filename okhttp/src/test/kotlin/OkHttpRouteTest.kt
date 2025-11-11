import com.storyteller_f.endpoint4k.common.MutationMethodType
import com.storyteller_f.endpoint4k.common.mutationEndpoint
import com.storyteller_f.endpoint4k.common.mutationEndpointWithQueryAndPath
import com.storyteller_f.endpoint4k.common.safeEndpoint
import com.storyteller_f.endpoint4k.common.safeEndpointWithQuery
import com.storyteller_f.endpoint4k.common.safeEndpointWithQueryAndPath
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import com.storyteller_f.endpoint4k.okhttp.client.invoke as invoke2

class OkHttpRouteTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var baseUrl: String

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        client = OkHttpClient()
        server.start()
        baseUrl = server.url("/").toString().removeSuffix("/")
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Serializable
    data class CommonQuery(val name: String)

    @Serializable
    data class CommonPath(val id: Int)

    @Serializable
    data class CommonObject(val name: String)

    private fun installDispatcher() {
        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: "/"
                val query = request.requestUrl?.queryParameter("name")
                return when {
                    // GET /user -> returns {"name":"ok"}
                    request.method == "GET" && path == "/user" && query == null ->
                        MockResponse().setResponseCode(200).setBody(Json.encodeToString(CommonObject("ok")))

                    // GET /user?id -> returns {"name":"{id} {queryName}"}
                    request.method == "GET" && path.startsWith("/user/") && query != null -> {
                        val id = path.substringAfterLast('/')
                        MockResponse().setResponseCode(200).setBody(Json.encodeToString(CommonObject("$id $query")))
                    }

                    // GET /user?name=xxx -> returns {"name":"{queryName}"}
                    request.method == "GET" && path == "/user" && query != null ->
                        MockResponse().setResponseCode(200).setBody(Json.encodeToString(CommonObject(query)))

                    // POST /user with body -> echo body
                    request.method == "POST" && path == "/user" -> {
                        val bodyStr = request.body.readUtf8()
                        val obj = if (bodyStr.isBlank()) {
                            CommonObject(
                                ""
                            )
                        } else {
                            Json.decodeFromString<CommonObject>(bodyStr)
                        }
                        MockResponse().setResponseCode(200).setBody(Json.encodeToString(obj))
                    }

                    // DELETE /user -> returns {"name":"delete"}
                    request.method == "DELETE" && path == "/user" ->
                        MockResponse().setResponseCode(200).setBody(Json.encodeToString(CommonObject("delete")))

                    // POST /user/{id}?name=xxx with body -> returns {"name":"{name} {body.name} {id}"}
                    request.method == "POST" && path.startsWith("/user/") && query != null -> {
                        val id = path.substringAfterLast('/')
                        val bodyStr = request.body.readUtf8()
                        val obj = if (bodyStr.isBlank()) {
                            CommonObject(
                                ""
                            )
                        } else {
                            Json.decodeFromString<CommonObject>(bodyStr)
                        }
                        MockResponse().setResponseCode(200)
                            .setBody(Json.encodeToString(CommonObject("$query ${obj.name} $id")))
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.dispatcher = dispatcher
    }

    @Test
    fun `test get route`() = runBlocking {
        installDispatcher()
        val api = safeEndpoint<CommonObject>("$baseUrl/user")
        val result = with(client) { api.invoke2() }
        assertEquals("ok", result.name)
    }

    @Test
    fun `test get with path and query route`() = runBlocking {
        installDispatcher()
        val api = safeEndpointWithQueryAndPath<CommonObject, CommonQuery, CommonPath>("$baseUrl/user/{id}")
        val result = with(client) { api.invoke2(CommonQuery("name"), CommonPath(1)) }
        assertEquals("1 name", result.name)
    }

    @Test
    fun `test get with query route`() = runBlocking {
        installDispatcher()
        val api = safeEndpointWithQuery<CommonObject, CommonQuery>("$baseUrl/user")
        val result = with(client) { api.invoke2(CommonQuery("name")) }
        assertEquals("name", result.name)
    }

    @Test
    fun `test mut routes`() = runBlocking {
        installDispatcher()
        val add = mutationEndpoint<CommonObject, CommonObject>("$baseUrl/user")
        val delete = mutationEndpoint<CommonObject, Unit>("$baseUrl/user", MutationMethodType.DELETE)

        val added = with(client) { add.invoke2<CommonObject, CommonObject>(CommonObject("add")) { /* no headers */ } }
        assertEquals("add", added.name)

        val deleted = with(client) { delete.invoke2<CommonObject, Unit>(Unit) { /* no headers */ } }
        assertEquals("delete", deleted.name)
    }

    @Test
    fun `test mut with query and path`() = runBlocking {
        installDispatcher()
        val api = mutationEndpointWithQueryAndPath<CommonObject, CommonObject, CommonQuery, CommonPath>("$baseUrl/user/{id}")
        val result = with(client) {
            api.invoke2(CommonQuery("name"), CommonPath(7), CommonObject("body")) { }
        }
        assertEquals("name body 7", result.name)
    }
}
