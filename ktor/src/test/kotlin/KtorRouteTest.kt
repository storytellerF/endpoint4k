import com.storyteller_f.endpoint4k.common.*
import com.storyteller_f.endpoint4k.ktor.client.json
import com.storyteller_f.endpoint4k.ktor.server.invoke
import com.storyteller_f.endpoint4k.ktor.server.receiveBody
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import com.storyteller_f.endpoint4k.ktor.client.invoke as invoke2

class KtorRouteTest {
    @Serializable
    data class CommonObject(val name: String)

    @Serializable
    data class CommonPath(val id: Long)

    @Serializable
    data class CommonQuery(val name: String)

    @Test
    fun `test get route`() {
        val customApi = object {
            val get = safeApi<CommonObject>("/")
        }

        test({
            customApi.get.invoke(RoutingContext::handleResult) {
                runCatching {
                    CommonObject("Hello, world!")
                }
            }
        }, {
            val text = customApi.get.invoke2<CommonObject>()
            assertEquals("Hello, world!", text.name)
        })
    }

    @Test
    fun `test get with path route`() {
        val customApi = object {
            val get = safeApiWithPath<CommonObject, CommonPath>("/user/{id}")
        }

        test({
            customApi.get.invoke(RoutingContext::handleResult) {
                runCatching {
                    CommonObject("${it.id}")
                }
            }
        }, {
            val text = customApi.get.invoke2(CommonPath(1))
            assertEquals("1", text.name)
        })
    }

    @Test
    fun `test get with query and path route`() {
        val customApi = object {
            val get = safeApiWithQueryAndPath<CommonObject, CommonQuery, CommonPath>("/user/{id}")
        }

        test({
            customApi.get.invoke(RoutingContext::handleResult) { q, p ->
                runCatching {
                    CommonObject("${p.id} ${q.name}")
                }
            }
        }, {
            val text = customApi.get.invoke2(CommonQuery("name"), CommonPath(1))
            assertEquals("1 name", text.name)
        })
    }

    @Test
    fun `test get with query route`() {
        val customApi = object {
            val get = safeApiWithQuery<CommonObject, CommonQuery>("/user")
        }

        test({
            customApi.get.invoke(RoutingContext::handleResult) {
                runCatching {
                    CommonObject(it.name)
                }
            }
        }, {
            val text = customApi.get.invoke2(CommonQuery("name"))
            assertEquals("name", text.name)
        })
    }

    @Test
    fun `test mut route`() {
        val customApi = object {
            val add = mutationApi<CommonObject, CommonObject>("/user")
            val delete = mutationApi<CommonObject, Unit>("/user", MutationMethodType.DELETE)
        }

        test({
            customApi.add.invoke(RoutingContext::handleResult) { api ->
                runCatching {
                    with(api) { receiveBody() }
                }
            }
            customApi.delete.invoke(RoutingContext::handleResult) { api ->
                runCatching {
                    CommonObject("delete")
                }
            }
        }, {
            assertEquals(
                "add",
                customApi.add.invoke2<CommonObject, CommonObject>(
                    CommonObject("add"),
                    json()
                ).name
            )
            assertEquals(
                "delete",
                customApi.delete.invoke2<CommonObject, Unit>(Unit, json()).name
            )
        })
    }

    @Test
    fun `test mut with query route`() {
        val customApi = object {
            val add = mutationApiWithQuery<CommonObject, CommonObject, CommonQuery>("/user")
            val delete = mutationApiWithQuery<CommonObject, Unit, CommonQuery>("/user", MutationMethodType.DELETE)
        }

        test({
            customApi.add.invoke(RoutingContext::handleResult) { q, api ->
                runCatching {
                    val commonObject = with(api) { receiveBody() }
                    commonObject.copy(name = "${q.name} ${commonObject.name}")
                }
            }
            customApi.delete.invoke(RoutingContext::handleResult) { q, api ->
                runCatching {
                    CommonObject(q.name)
                }
            }
        }, {
            assertEquals(
                "name old",
                customApi.add.invoke2<CommonObject, CommonObject, CommonQuery>(
                    CommonQuery("name"),
                    CommonObject("old"),
                    json()
                ).name
            )
            assertEquals(
                "name",
                customApi.delete.invoke2<CommonObject, Unit, CommonQuery>(CommonQuery("name"), Unit, json()).name
            )
        })
    }

    @Test
    fun `test mut with query and path route`() {
        val customApi = object {
            val add = mutationApiWithQueryAndPath<CommonObject, CommonObject, CommonQuery, CommonPath>("/user/{id}")
            val delete = mutationApiWithQueryAndPath<CommonObject, Unit, CommonQuery, CommonPath>(
                "/user/{id}",
                MutationMethodType.DELETE
            )
        }

        test({
            customApi.add.invoke(RoutingContext::handleResult) { q, p, api ->
                runCatching {
                    val commonObject = with(api) { receiveBody() }
                    commonObject.copy(name = "${q.name}${p.id}${commonObject.name}")
                }
            }
            customApi.delete.invoke(RoutingContext::handleResult) { q, p, api ->
                runCatching {
                    CommonObject(q.name + p.id)
                }
            }
        }, {
            assertEquals(
                "name1old",
                customApi.add.invoke2<CommonObject, CommonObject, CommonQuery, CommonPath>(
                    CommonQuery("name"),
                    CommonPath(1),
                    CommonObject("old"),
                    json()
                ).name
            )
            assertEquals(
                "name1",
                customApi.delete.invoke2<CommonObject, Unit, CommonQuery, CommonPath>(
                    CommonQuery("name"),
                    CommonPath(1),
                    Unit,
                    json()
                ).name
            )
        })
    }

    @Test
    fun `test mut with path route`() {
        val customApi = object {
            val add = mutationApiWithPath<CommonObject, CommonObject, CommonPath>("/user/{id}")
            val delete = mutationApiWithPath<CommonObject, Unit, CommonPath>(
                "/user/{id}",
                MutationMethodType.DELETE
            )
        }

        test({
            customApi.add.invoke(RoutingContext::handleResult) { p, api ->
                runCatching {
                    val commonObject = with(api) { receiveBody() }
                    commonObject.copy(name = "${p.id}${commonObject.name}")
                }
            }
            customApi.delete.invoke(RoutingContext::handleResult) { p, api ->
                runCatching {
                    CommonObject("${p.id}")
                }
            }
        }, {
            assertEquals(
                "1old",
                customApi.add.invoke2<CommonObject, CommonObject, CommonPath>(
                    CommonPath(1),
                    CommonObject("old"),
                    json()
                ).name
            )
            assertEquals(
                "1",
                customApi.delete.invoke2<CommonObject, Unit, CommonPath>(
                    CommonPath(1),
                    Unit,
                    json()
                ).name
            )
        })
    }
}

private fun test(
    configuration: Routing.() -> Unit,
    block: suspend HttpClient.() -> Unit,
) {
    val module: Application.() -> Unit = {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        routing(configuration)
    }
    testApplication {
        application {
            module()
        }

        with(createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }) {
            block()
        }
    }
}

suspend inline fun <reified R> RoutingContext.handleResult(it: Result<R>) {
    it.onSuccess {
        when (it) {
            null -> call.respond(HttpStatusCode.NotFound)
            is Unit -> call.respond(HttpStatusCode.OK)
            else -> call.respond(it)
        }
    }.onFailure {
        respondError(it)
        call.application.log.error("Occur server exception", it)
    }
}

suspend fun RoutingContext.respondError(e: Throwable) {
    when (e) {
        is MissingRequestParameterException, is ParameterConversionException, is ContentTransformationException -> {
            call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
        }

        is BadRequestException, is IllegalArgumentException, is IllegalStateException -> {
            call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
        }

        else -> {
            call.respond(
                HttpStatusCode.InternalServerError,
                e.message ?: e.toString()
            )
        }
    }
}
