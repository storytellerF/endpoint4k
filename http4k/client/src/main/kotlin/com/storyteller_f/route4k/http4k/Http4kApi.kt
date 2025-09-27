@file:Suppress("detekt.formatting")

package com.storyteller_f.route4k.http4k

import com.storyteller_f.route4k.common.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.core.*
import kotlin.reflect.KClass

@PublishedApi
internal val JsonCodec: Json = Json { ignoreUnknownKeys = true }

// -------------------- Safe APIs --------------------

context(route: HttpHandler)
@OptIn(InternalSerializationApi::class)
@Suppress("NOTHING_TO_INLINE")
suspend inline operator fun <reified R : Any> SafeApi<R>.invoke(): R {
    return with(route) {
        val request = Request(methodType.toHttp4kMethod(), Uri.of(urlString))
        requestAndDecode<R>(request)
    }
}

context(route: HttpHandler)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, Q : Any> SafeApiWithQuery<R, Q>.invoke(query: Q): R {
    return with(route) {
        val finalUrl = appendQueryParameters(urlString, this@invoke, query)
        val request = Request(methodType.toHttp4kMethod(), Uri.of(finalUrl))
        requestAndDecode<R>(request)
    }
}

context(route: HttpHandler)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, Q : Any, P : Any> SafeApiWithQueryAndPath<R, Q, P>.invoke(
    query: Q,
    path: P,
): R {
    val newUrlString = buildPathUrlString(path, pathClass, urlString)
    return with(route) {
        val finalUrl = appendQueryParameters(newUrlString, this@invoke, query)
        val request = Request(methodType.toHttp4kMethod(), Uri.of(finalUrl))
        requestAndDecode<R>(request)
    }
}

context(route: HttpHandler)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, P : Any> SafeApiWithPath<R, P>.invoke(path: P): R {
    val newUrlString = buildPathUrlString(path, pathClass, urlString)
    return with(route) {
        val request = Request(methodType.toHttp4kMethod(), Uri.of(newUrlString))
        requestAndDecode<R>(request)
    }
}

// -------------------- Mutation APIs --------------------

context(route: HttpHandler)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any> MutationApi<R, B>.invoke(
    body: B,
    crossinline block: (Request) -> Request,
): R {
    return with(route) {
        val request = buildMutationRequest(urlString, methodType, body, block)
        requestAndDecode<R>(request)
    }
}

context(route: HttpHandler)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, Q : Any> MutationApiWithQuery<R, B, Q>.invoke(
    query: Q,
    body: B,
    crossinline block: (Request) -> Request,
): R {
    return with(route) {
        val finalUrl = appendQueryParameters(urlString, this@invoke, query)
        val request = buildMutationRequest(finalUrl, methodType, body, block)
        requestAndDecode<R>(request)
    }
}

context(route: HttpHandler)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, Q : Any, P : Any> MutationApiWithQueryAndPath<R, B, Q, P>.invoke(
    query: Q,
    path: P,
    body: B,
    crossinline block: (Request) -> Request,
): R {
    val newUrlString = buildPathUrlString(path, pathClass, urlString)
    return with(route) {
        val finalUrl = appendQueryParameters(newUrlString, this@invoke, query)
        val request = buildMutationRequest(finalUrl, methodType, body, block)
        requestAndDecode<R>(request)
    }
}

context(route: HttpHandler)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, P : Any> MutationApiWithPath<R, B, P>.invoke(
    path: P,
    body: B,
    crossinline block: (Request) -> Request,
): R {
    val newUrlString = buildPathUrlString(path, pathClass, urlString)
    return with(route) {
        val request = buildMutationRequest(newUrlString, methodType, body, block)
        requestAndDecode<R>(request)
    }
}

@PublishedApi
internal fun SafeMethodType.toHttp4kMethod(): Method = when (this) {
    SafeMethodType.GET -> Method.GET
    SafeMethodType.OPTIONS -> Method.OPTIONS
}

@PublishedApi
internal fun MutationMethodType.toHttp4kMethod(hasBody: Boolean): Method = when (this) {
    MutationMethodType.POST -> Method.POST
    MutationMethodType.PUT -> Method.PUT
    MutationMethodType.PATCH -> Method.PATCH
    MutationMethodType.DELETE -> if (hasBody) Method.DELETE else Method.DELETE
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal suspend inline fun <reified R : Any> HttpHandler.requestAndDecode(request: Request): R {
    return withContext(Dispatchers.IO) {
        val response: Response = this@requestAndDecode(request)
        if (response.status.successful) {
            val body = response.bodyString()
            @Suppress("UNCHECKED_CAST")
            when (R::class) {
                String::class -> body as R
                Unit::class -> Unit as R
                else -> {
                    val deserializer = R::class.serializer()
                    JsonCodec.decodeFromString(deserializer, body)
                }
            }
        } else {
            error("HTTP error ${'$'}{response.status.code}: ${'$'}{response.bodyString()}")
        }
    }
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal inline fun <reified B : Any> buildRequestBody(body: B): Pair<List<Pair<String, String>>, String?> {
    if (body is Unit) return emptyList<Pair<String, String>>() to null
    val serializer = B::class.serializer()
    val json = JsonCodec.encodeToString(serializer, body)
    val headers = listOf("Content-Type" to "application/json")
    return headers to json
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal inline fun <reified B : Any> buildMutationRequest(
    urlString: String,
    methodType: MutationMethodType,
    body: B,
    crossinline block: (Request) -> Request,
): Request {
    val (headers, bodyContent) = buildRequestBody(body)
    val method = methodType.toHttp4kMethod(body !is Unit)
    var req = Request(method, Uri.of(urlString))
    headers.forEach { (k, v) -> req = req.header(k, v) }
    if (bodyContent != null) req = req.body(bodyContent)
    req = block(req)
    return req
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal fun <Q : Any> appendQueryParameters(urlString: String, api: WithQueryApi<Q>, query: Q): String {
    val params = encodeQueryParams(query, api.queryClass)
    val hasQuery = urlString.contains("?")
    val sb = StringBuilder(urlString)
    var first = !hasQuery
    params.forEach { (key, values) ->
        values.forEach { v ->
            sb.append(if (first) "?" else "&")
            first = false
            sb.append(java.net.URLEncoder.encode(key, "UTF-8"))
                .append("=")
                .append(java.net.URLEncoder.encode(v, "UTF-8"))
        }
    }
    return sb.toString()
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal fun <P : Any> buildPathUrlString(path: P, pathClass: KClass<P>, urlString: String): String {
    val params = encodeQueryParams(path, pathClass)
    return params.entries.fold(urlString) { acc, (key, value) ->
        acc.replace("{" + key + "}", value.firstOrNull() ?: "")
    }
}

// Local copy of query encoder
@PublishedApi
@OptIn(InternalSerializationApi::class)
internal fun <T : Any> encodeQueryParams(value: T, clazz: KClass<T>): Map<String, List<String>> {
    val serializer = clazz.serializer()
    val encoder = CustomParameterEncoder(clazz, serializer)
    encoder.encodeSerializableValue(serializer, value)
    return encoder.map
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal class CustomParameterEncoder<T : Any>(
    clazz: KClass<T>,
    serializer: KSerializer<T>,
) : NamedValueEncoder() {
    val map = mutableMapOf<String, MutableList<String>>()

    override val serializersModule: SerializersModule = serializersModuleOf(clazz, serializer)

    override fun encodeTaggedValue(tag: String, value: Any) {
        val newTag = tag.substringBeforeLast(".")
        when (value) {
            is Iterable<*> -> value.forEach { item ->
                if (item != null) map.getOrPut(
                    newTag
                ) { mutableListOf() }.add(item.toString())
            }

            is Array<*> -> value.forEach { item ->
                if (item != null) map.getOrPut(
                    newTag
                ) { mutableListOf() }.add(item.toString())
            }

            else -> map.getOrPut(newTag) { mutableListOf() }.add(value.toString())
        }
    }

    override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
        val newTag = tag.substringBeforeLast(".")
        map.getOrPut(newTag) { mutableListOf() }.add(enumDescriptor.getElementName(ordinal))
    }

    override fun encodeTaggedNull(tag: String) {
        // ignore nulls
    }
}
