@file:Suppress("detekt.formatting")

package com.storyteller_f.route4k.ktor.client

import com.storyteller_f.route4k.common.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.InternalSerializationApi
import kotlin.reflect.KClass

context(route: HttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any> SafeApi<R>.invoke(): R {
    return with(route) {
        commonSafeRequest(urlString).body<R>()
    }
}

context(route: HttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, Q : Any> SafeApiWithQuery<R, Q>.invoke(query: Q): R {
    return with(route) {
        commonSafeRequest(urlString) {
            appendQueryParameters(query)
        }.body<R>()
    }
}

context(route: HttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, Q : Any, P : Any> SafeApiWithQueryAndPath<R, Q, P>.invoke(
    query: Q,
    path: P,
): R {
    val newUrlString = getUrlString(path, pathClass, urlString)
    return with(route) {
        commonSafeRequest(newUrlString) {
            appendQueryParameters(query)
        }.body<R>()
    }
}

context(route: HttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, P : Any> SafeApiWithPath<R, P>.invoke(path: P): R {
    val newUrlString = getUrlString(path, pathClass, urlString)
    return with(route) {
        commonSafeRequest(newUrlString).body<R>()
    }
}

context(route: HttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any> MutationApi<R, B>.invoke(
    body: B,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): R {
    return with(route) {
        customMutationRequest(urlString) {
            if (body !is Unit) {
                setBody(body)
            }
            block()
        }.body<R>()
    }
}

context(route: HttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, Q : Any> MutationApiWithQuery<R, B, Q>.invoke(
    query: Q,
    body: B,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): R {
    return with(route) {
        customMutationRequest(urlString) {
            appendQueryParameters(query)
            if (body !is Unit) {
                setBody(body)
            }
            block()
        }.body<R>()
    }
}

context(route: HttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, Q : Any, P : Any>
        MutationApiWithQueryAndPath<R, B, Q, P>.invoke(
    query: Q,
    path: P,
    body: B,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): R {
    val newUrlString = getUrlString(path, pathClass, urlString)
    return with(route) {
        customMutationRequest(newUrlString) {
            appendQueryParameters(query)
            if (body !is Unit) {
                setBody(body)
            }
            block()
        }.body<R>()
    }
}

context(route: HttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, P : Any> MutationApiWithPath<R, B, P>.invoke(
    path: P,
    body: B,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): R {
    val newUrlString = getUrlString(path, pathClass, urlString)
    return with(route) {
        customMutationRequest(newUrlString) {
            if (body !is Unit) {
                setBody(body)
            }
            block()
        }.body<R>()
    }
}

context(route: HttpClient)
suspend fun <Resp, Body> AbstractMutationApi<Resp, Body>.customMutationRequest(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse {
    val builder = HttpRequestBuilder()
    builder.method = when (methodType) {
        MutationMethodType.POST -> HttpMethod.Post
        MutationMethodType.PUT -> HttpMethod.Put
        MutationMethodType.PATCH -> HttpMethod.Patch
        MutationMethodType.DELETE -> HttpMethod.Delete
    }
    return route.request(builder.apply {
        url(urlString)
        block()
    })
}

context(route: HttpClient)
suspend fun <Resp> AbstractSafeApi<Resp>.commonSafeRequest(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse {
    val builder = HttpRequestBuilder()
    builder.method = when (methodType) {
        SafeMethodType.GET -> HttpMethod.Get
        SafeMethodType.OPTIONS -> HttpMethod.Options
    }
    return route.request(builder.apply {
        url(urlString)
        block()
    })
}

@OptIn(InternalSerializationApi::class)
fun <P : Any> getUrlString(path: P, pathClass: KClass<P>, urlString: String): String {
    val pathParams = encodeQueryParams(path, pathClass)
    val newUrlString = pathParams.toList().fold(urlString) { acc, (key, value) ->
        acc.replace("{$key}", value.first())
    }
    return newUrlString
}

context(builder: HttpRequestBuilder)
fun <Q : Any> WithQueryApi<Q>.appendQueryParameters(
    query: Q,
) {
    val clazz = queryClass
    val params = encodeQueryParams(query, clazz)
    builder.url {
        params.forEach { (key, value) ->
            parameters.appendAll(key, value)
        }
    }
}

inline fun json(
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): HttpRequestBuilder.() -> Unit {
    return {
        contentType(ContentType.Application.Json)
        block()
    }
}

inline fun xml(
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): HttpRequestBuilder.() -> Unit {
    return {
        contentType(ContentType.Application.Xml)
        block()
    }
}

inline fun cbor(
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): HttpRequestBuilder.() -> Unit {
    return {
        contentType(ContentType.Application.Cbor)
        block()
    }
}