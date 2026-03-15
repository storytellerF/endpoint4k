import com.storyteller_f.endpoint4k.common.MutationMethodType
import com.storyteller_f.endpoint4k.common.body
import com.storyteller_f.endpoint4k.common.mutationEndpointBuilder
import com.storyteller_f.endpoint4k.common.mutationEndpointWithQueryAndPathBuilder
import com.storyteller_f.endpoint4k.common.path
import com.storyteller_f.endpoint4k.common.query
import com.storyteller_f.endpoint4k.common.resp
import com.storyteller_f.endpoint4k.common.safeEndpointBuilder
import com.storyteller_f.endpoint4k.common.safeEndpointWithQueryAndPathBuilder
import com.storyteller_f.endpoint4k.common.safeEndpointWithQueryBuilder
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
        val getUser = safeEndpointBuilder("/user") {
            resp(CommonObject::class)
        }
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
        val api = safeEndpointBuilder<CommonObject>("http://localhost/user") {
            resp(CommonObject::class)
        }
        val result = with(handler) { api.invoke3() }
        assertEquals("ok", result.name)
    }

    @Test
    fun `test get with path and query route`() = runBlocking {
        val getUserPathQuery = safeEndpointWithQueryAndPathBuilder("/user/{id}") {
            resp(CommonObject::class)
            query(CommonQuery::class)
            path(CommonPath::class)
        }
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
        val api = safeEndpointWithQueryAndPathBuilder("http://localhost/user/{id}") {
            resp(CommonObject::class)
            query(CommonQuery::class)
            path(CommonPath::class)
        }
        val result = with(handler) { api.invoke3(CommonQuery("name"), CommonPath(1)) }
        assertEquals("1 name", result.name)
    }

    @Test
    fun `test get with query route`() = runBlocking {
        val getUser = safeEndpointBuilder("/user") {
            resp(CommonObject::class)
        }
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
        val api = safeEndpointWithQueryBuilder("http://localhost/user") {
            resp(CommonObject::class)
            query(CommonQuery::class)
        }
        val result = with(handler) { api.invoke3(CommonQuery("name")) }
        assertEquals("name", result.name)
    }

    @Test
    fun `test mut routes`() = runBlocking {
        val addUser = mutationEndpointBuilder("/user") {
            resp(CommonObject::class)
            body(CommonObject::class)
        }
        val deleteUser = mutationEndpointBuilder("/user", MutationMethodType.DELETE) {
            resp(CommonObject::class)
            body(Unit::class)
        }
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
        val add = mutationEndpointBuilder("http://localhost/user") {
            resp(CommonObject::class)
            body(CommonObject::class)
        }
        val delete = mutationEndpointBuilder("http://localhost/user", MutationMethodType.DELETE) {
            resp(CommonObject::class)
            body(Unit::class)
        }

        val added = with(handler) { add.invoke3<CommonObject, CommonObject>(CommonObject("add")) { it } }
        assertEquals("add", added.name)

        val deleted = with(handler) { delete.invoke3<CommonObject, Unit>(Unit) { it } }
        assertEquals("delete", deleted.name)
    }

    @Test
    fun `test mut with query and path`() = runBlocking {
        val addUserWithQP = mutationEndpointWithQueryAndPathBuilder("/user/{id}") {
            resp(CommonObject::class)
            body(CommonObject::class)
            query(CommonQuery::class)
            path(CommonPath::class)
        }
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
        val api = mutationEndpointWithQueryAndPathBuilder("http://localhost/user/{id}") {
            resp(CommonObject::class)
            body(CommonObject::class)
            query(CommonQuery::class)
            path(CommonPath::class)
        }
        val result = with(handler) {
            api.invoke3(CommonQuery("name"), CommonPath(7), CommonObject("body")) { it }
        }
        assertEquals("name body 7", result.name)
    }
}
