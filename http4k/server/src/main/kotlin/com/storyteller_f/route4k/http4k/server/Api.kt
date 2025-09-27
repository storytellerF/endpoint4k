package com.storyteller_f.route4k.http4k.server

import com.storyteller_f.route4k.common.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import kotlin.reflect.KClass

// -------------------- Safe APIs --------------------

@OptIn(InternalSerializationApi::class)
operator fun <R : Any, Q : Any, P : Any> SafeApiWithQueryAndPath<R, Q, P>.invoke(
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: (req: Request, q: Q, p: P) -> Result<R?>?
): RoutingHttpHandler {
    val handler: HttpHandler = { req ->
        handleRequest(req, handleResult) {
            val q = req.getQuery(queryClass)
            val p = req.getPathQuery(pathClass)
            block(req, q, p)
        }
    }
    return routes(urlString bind methodType.toHttp4kMethod() to handler)
}

@OptIn(InternalSerializationApi::class)
operator fun <R : Any, Q : Any> SafeApiWithQuery<R, Q>.invoke(
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: (req: Request, q: Q) -> Result<R?>?
): RoutingHttpHandler {
    val handler: HttpHandler = { req ->
        handleRequest(req, handleResult) {
            val q = req.getQuery(queryClass)
            block(req, q)
        }
    }
    return routes(urlString bind Method.GET to handler)
}

@OptIn(InternalSerializationApi::class)
operator fun <R : Any, P : Any> SafeApiWithPath<R, P>.invoke(
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: (req: Request, p: P) -> Result<R?>?
): RoutingHttpHandler {
    val handler: HttpHandler = { req ->
        handleRequest(req, handleResult) {
            val p = req.getPathQuery(pathClass)
            block(req, p)
        }
    }
    return routes(urlString bind methodType.toHttp4kMethod() to handler)
}

@OptIn(InternalSerializationApi::class)
operator fun <R : Any> SafeApi<R>.invoke(
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: (req: Request) -> Result<R?>?
): RoutingHttpHandler {
    val handler: HttpHandler = { req -> handleRequest(req, handleResult) { block(req) } }
    return routes(urlString bind methodType.toHttp4kMethod() to handler)
}

// -------------------- Mutation APIs --------------------

@OptIn(InternalSerializationApi::class)
operator fun <R : Any, B : Any, Q : Any, P : Any> MutationApiWithQueryAndPath<R, B, Q, P>.invoke(
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: (req: Request, q: Q, p: P, api: MutationApiWithQueryAndPath<R, B, Q, P>) -> Result<R?>?
): RoutingHttpHandler {
    val handler: HttpHandler = { req ->
        handleRequest(req, handleResult) {
            val q = req.getQuery(queryClass)
            val p = req.getPathQuery(pathClass)
            block(req, q, p, this@invoke)
        }
    }
    return routes(urlString bind methodType.toHttp4kMethod() to handler)
}

@OptIn(InternalSerializationApi::class)
operator fun <R : Any, B : Any, Q : Any> MutationApiWithQuery<R, B, Q>.invoke(
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: (req: Request, q: Q, api: MutationApiWithQuery<R, B, Q>) -> Result<R?>?
): RoutingHttpHandler {
    val handler: HttpHandler = { req ->
        handleRequest(req, handleResult) {
            val q = req.getQuery(queryClass)
            block(req, q, this@invoke)
        }
    }
    return routes(urlString bind methodType.toHttp4kMethod() to handler)
}

@OptIn(InternalSerializationApi::class)
operator fun <R : Any, B : Any, P : Any> MutationApiWithPath<R, B, P>.invoke(
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: (req: Request, p: P, api: MutationApiWithPath<R, B, P>) -> Result<R?>?
): RoutingHttpHandler {
    val handler: HttpHandler = { req ->
        handleRequest(req, handleResult) {
            val p = req.getPathQuery(pathClass)
            block(req, p, this@invoke)
        }
    }
    return routes(urlString bind methodType.toHttp4kMethod() to handler)
}

@OptIn(InternalSerializationApi::class)
operator fun <R : Any, B : Any> MutationApi<R, B>.invoke(
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: (req: Request, api: MutationApi<R, B>) -> Result<R?>?
): RoutingHttpHandler {
    val handler: HttpHandler = { req -> handleRequest(req, handleResult) { block(req, this@invoke) } }
    return routes(urlString bind methodType.toHttp4kMethod() to handler)
}

// -------------------- Helpers --------------------

private fun SafeMethodType.toHttp4kMethod(): Method = when (this) {
    SafeMethodType.GET -> Method.GET
    SafeMethodType.OPTIONS -> Method.OPTIONS
}

private fun MutationMethodType.toHttp4kMethod(): Method = when (this) {
    MutationMethodType.POST -> Method.POST
    MutationMethodType.PUT -> Method.PUT
    MutationMethodType.PATCH -> Method.PATCH
    MutationMethodType.DELETE -> Method.DELETE
}

@OptIn(InternalSerializationApi::class)
private fun <Q : Any> Request.getQuery(kClass: KClass<Q>): Q {
    val querySerializer = kClass.serializer()
    return querySerializer.deserialize(
        ParametersDecoder(
            serializersModuleOf(kClass, querySerializer),
            queryParametersOf(this),
            querySerializer.descriptor.elementNames
        )
    )
}

@OptIn(InternalSerializationApi::class)
private fun <P : Any> Request.getPathQuery(kClass: KClass<P>): P {
    val pathSerializer = kClass.serializer()
    return pathSerializer.deserialize(
        ParametersDecoder(
            serializersModuleOf(kClass, pathSerializer),
            pathParametersOf(this),
            pathSerializer.descriptor.elementNames
        )
    )
}

private inline fun <R : Any> handleRequest(
    request: Request,
    handleResult: (req: Request, result: Result<R?>) -> Response,
    block: () -> Result<R?>?
): Response {
    return try {
        val result = block()
        if (result == null) {
            Response(Status.NOT_FOUND)
        } else {
            handleResult(request, result)
        }
    } catch (e: Exception) {
        handleCaughtException(e)
    }
}

fun handleCaughtException(e: Exception): Response {
    e.printStackTrace()
    return Response(Status.INTERNAL_SERVER_ERROR).body("Catch exception")
}

@OptIn(InternalSerializationApi::class)
inline fun <reified Resp, reified Body : Any> AbstractMutationApi<Resp, Body>.receiveBody(
    request: Request
): Body {
    val bodyString = request.bodyString()
    @Suppress("UNCHECKED_CAST")
    return when (Body::class) {
        Unit::class -> Unit as Body
        String::class -> bodyString as Body
        else -> {
            val deserializer = Body::class.serializer()
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString(deserializer, bodyString)
        }
    }
}
