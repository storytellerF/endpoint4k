import com.storyteller_f.endpoint4k.common.MutationMethodType
import com.storyteller_f.endpoint4k.common.mutationEndpoint
import com.storyteller_f.endpoint4k.common.mutationEndpointWithQueryAndPath
import com.storyteller_f.endpoint4k.common.safeEndpoint
import com.storyteller_f.endpoint4k.common.safeEndpointWithQuery
import com.storyteller_f.endpoint4k.common.safeEndpointWithQueryAndPath
import com.storyteller_f.endpoint4k.http4k.server.receiveBody
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.routes
import kotlin.test.Test
import kotlin.test.assertEquals
import com.storyteller_f.endpoint4k.http4k.client.invoke as invoke3
import com.storyteller_f.endpoint4k.http4k.server.invoke as serverInvoke

class Http4kRouteTest {

    @Serializable
    data class CommonQuery(val name: String)

    @Serializable
    data class CommonPath(val id: Int)

    @Serializable
    data class CommonObject(val name: String)

    @Test
    fun `test get route`() = runBlocking {
        // server side
        val getUser = safeEndpoint<CommonObject>("/user")
        val handler: HttpHandler = routes(
            getUser.serverInvoke(handleResult = { _, result ->
                result.fold(
                    onSuccess = { Response(Status.OK).body(Json.encodeToString(it)) },
                    onFailure = { Response(Status.INTERNAL_SERVER_ERROR) }
                )
            }) { req ->
                val n = req.query("name")
                if (n != null) Result.success(CommonObject(n)) else Result.success(CommonObject("ok"))
            }
        )
        // client side
        val api = safeEndpoint<CommonObject>("http://localhost/user")
        val result = with(handler) { api.invoke3() }
        assertEquals("ok", result.name)
    }

    @Test
    fun `test get with path and query route`() = runBlocking {
        // server side
        val getUserPathQuery = safeEndpointWithQueryAndPath<CommonObject, CommonQuery, CommonPath>("/user/{id}")
        val handler: HttpHandler = routes(
            getUserPathQuery.serverInvoke(handleResult = { _, result ->
                result.fold(
                    onSuccess = { Response(Status.OK).body(Json.encodeToString(it)) },
                    onFailure = { Response(Status.INTERNAL_SERVER_ERROR) }
                )
            }) { _, q, p ->
                Result.success(CommonObject("${p.id} ${q.name}"))
            }
        )
        // client side
        val api = safeEndpointWithQueryAndPath<CommonObject, CommonQuery, CommonPath>("http://localhost/user/{id}")
        val result = with(handler) { api.invoke3(CommonQuery("name"), CommonPath(1)) }
        assertEquals("1 name", result.name)
    }

    @Test
    fun `test get with query route`() = runBlocking {
        // server side
        val getUser = safeEndpoint<CommonObject>("/user")
        val handler: HttpHandler = routes(
            getUser.serverInvoke(handleResult = { _, result ->
                result.fold(
                    onSuccess = { Response(Status.OK).body(Json.encodeToString(it)) },
                    onFailure = { Response(Status.INTERNAL_SERVER_ERROR) }
                )
            }) { req ->
                val n = req.query("name")
                if (n != null) Result.success(CommonObject(n)) else null
            }
        )
        // client side
        val api = safeEndpointWithQuery<CommonObject, CommonQuery>("http://localhost/user")
        val result = with(handler) { api.invoke3(CommonQuery("name")) }
        assertEquals("name", result.name)
    }

    @Test
    fun `test mut routes`() = runBlocking {
        // server side
        val addUser = mutationEndpoint<CommonObject, CommonObject>("/user")
        val deleteUser = mutationEndpoint<CommonObject, Unit>("/user", MutationMethodType.DELETE)
        val handler: HttpHandler = routes(
            addUser.serverInvoke(handleResult = { _, result ->
                result.fold(
                    onSuccess = { Response(Status.OK).body(Json.encodeToString(it)) },
                    onFailure = { Response(Status.INTERNAL_SERVER_ERROR) }
                )
            }) { req, api ->
                val body = runBlocking { api.receiveBody<CommonObject, CommonObject>(req) }
                Result.success(body)
            },
            deleteUser.serverInvoke(handleResult = { _, result ->
                result.fold(
                    onSuccess = { Response(Status.OK).body(Json.encodeToString(it)) },
                    onFailure = { Response(Status.INTERNAL_SERVER_ERROR) }
                )
            }) { _, _ ->
                Result.success(CommonObject("delete"))
            }
        )
        // client side
        val add = mutationEndpoint<CommonObject, CommonObject>("http://localhost/user")
        val delete = mutationEndpoint<CommonObject, Unit>("http://localhost/user", MutationMethodType.DELETE)

        val added = with(handler) { add.invoke3<CommonObject, CommonObject>(CommonObject("add")) { it } }
        assertEquals("add", added.name)

        val deleted = with(handler) { delete.invoke3<CommonObject, Unit>(Unit) { it } }
        assertEquals("delete", deleted.name)
    }

    @Test
    fun `test mut with query and path`() = runBlocking {
        // server side
        val addUserWithQP = mutationEndpointWithQueryAndPath<CommonObject, CommonObject, CommonQuery, CommonPath>(
            "/user/{id}"
        )
        val handler: HttpHandler = routes(
            addUserWithQP.serverInvoke(handleResult = { _, result ->
                result.fold(
                    onSuccess = { Response(Status.OK).body(Json.encodeToString(it)) },
                    onFailure = { Response(Status.INTERNAL_SERVER_ERROR) }
                )
            }) { req, q, p, api ->
                val body = runBlocking { api.receiveBody<CommonObject, CommonObject>(req) }
                Result.success(CommonObject("${q.name} ${body.name} ${p.id}"))
            }
        )
        // client side
        val api = mutationEndpointWithQueryAndPath<CommonObject, CommonObject, CommonQuery, CommonPath>(
            "http://localhost/user/{id}"
        )
        val result = with(handler) {
            api.invoke3(CommonQuery("name"), CommonPath(7), CommonObject("body")) { it }
        }
        assertEquals("name body 7", result.name)
    }
}
