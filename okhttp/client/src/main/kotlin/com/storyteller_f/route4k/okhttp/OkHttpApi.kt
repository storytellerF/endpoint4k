package com.storyteller_f.route4k.okhttp

import com.storyteller_f.route4k.common.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.reflect.KClass

/**
 * OkHttp-based client implementation mirroring the Ktor client helpers.
 *
 * These helpers use kotlinx-serialization for JSON encoding/decoding.
 * Use them with context receivers:
 *
 * context(okHttpClient) { api.invoke(...) }
 */

@PublishedApi
internal val JsonCodec: Json = Json { ignoreUnknownKeys = true }

// -------------------- Safe APIs --------------------

context(route: OkHttpClient)
@OptIn(InternalSerializationApi::class)
@Suppress("NOTHING_TO_INLINE")
suspend inline operator fun <reified R : Any> SafeApi<R>.invoke(): R {
    return with(route) {
        val request = Request.Builder()
            .url(urlString)
            .method(methodType.toOkHttpMethod(), null)
            .build()
        requestAndDecode<R>(request)
    }
}

context(route: OkHttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, Q : Any> SafeApiWithQuery<R, Q>.invoke(query: Q): R {
    return with(route) {
        val finalUrl = appendQueryParametersOkHttp(urlString, this@invoke, query)
        val request = Request.Builder()
            .url(finalUrl)
            .method(methodType.toOkHttpMethod(), null)
            .build()
        requestAndDecode<R>(request)
    }
}

context(route: OkHttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, Q : Any, P : Any> SafeApiWithQueryAndPath<R, Q, P>.invoke(
    query: Q,
    path: P
): R {
    val newUrlString = buildPathUrlString(path, pathClass, urlString)
    return with(route) {
        val finalUrl = appendQueryParametersOkHttp(newUrlString, this@invoke, query)
        val request = Request.Builder()
            .url(finalUrl)
            .method(methodType.toOkHttpMethod(), null)
            .build()
        requestAndDecode<R>(request)
    }
}

context(route: OkHttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, P : Any> SafeApiWithPath<R, P>.invoke(path: P): R {
    val newUrlString = buildPathUrlString(path, pathClass, urlString)
    return with(route) {
        val request = Request.Builder()
            .url(newUrlString)
            .method(methodType.toOkHttpMethod(), null)
            .build()
        requestAndDecode<R>(request)
    }
}

// -------------------- Mutation APIs --------------------

context(route: OkHttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any> MutationApi<R, B>.invoke(
    body: B,
    crossinline block: Request.Builder.() -> Unit
): R {
    return with(route) {
        val request = buildMutationRequest(urlString, methodType, body, block)
        requestAndDecode<R>(request)
    }
}

context(route: OkHttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, Q : Any> MutationApiWithQuery<R, B, Q>.invoke(
    query: Q,
    body: B,
    crossinline block: Request.Builder.() -> Unit
): R {
    return with(route) {
        val finalUrl = appendQueryParametersOkHttp(urlString, this@invoke, query)
        val request = buildMutationRequest(finalUrl, methodType, body, block)
        requestAndDecode<R>(request)
    }
}

context(route: OkHttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, Q : Any, P : Any>
        MutationApiWithQueryAndPath<R, B, Q, P>.invoke(
    query: Q,
    path: P,
    body: B,
    crossinline block: Request.Builder.() -> Unit
): R {
    val newUrlString = buildPathUrlString(path, pathClass, urlString)
    return with(route) {
        val finalUrl = appendQueryParametersOkHttp(newUrlString, this@invoke, query)
        val request = buildMutationRequest(finalUrl, methodType, body, block)
        requestAndDecode<R>(request)
    }
}

context(route: OkHttpClient)
@OptIn(InternalSerializationApi::class)
suspend inline operator fun <reified R : Any, reified B : Any, P : Any> MutationApiWithPath<R, B, P>.invoke(
    path: P,
    body: B,
    crossinline block: Request.Builder.() -> Unit
): R {
    val newUrlString = buildPathUrlString(path, pathClass, urlString)
    return with(route) {
        val request = buildMutationRequest(newUrlString, methodType, body, block)
        requestAndDecode<R>(request)
    }
}

// -------------------- Internal helpers --------------------

@PublishedApi
internal fun SafeMethodType.toOkHttpMethod(): String = when (this) {
    SafeMethodType.GET -> "GET"
    SafeMethodType.OPTIONS -> "OPTIONS"
}

@PublishedApi
internal fun MutationMethodType.toOkHttpMethod(hasBody: Boolean): String = when (this) {
    MutationMethodType.POST -> "POST"
    MutationMethodType.PUT -> "PUT"
    MutationMethodType.PATCH -> "PATCH"
    MutationMethodType.DELETE -> if (hasBody) "DELETE" else "DELETE"
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal inline fun <reified R : Any> OkHttpClient.requestAndDecode(request: Request): R {
    val response: Response = newCall(request).execute()
    response.use {
        if (!it.isSuccessful) throw IllegalStateException("HTTP ${'$'}{it.code}: ${'$'}{it.message}")
        val body = it.body?.string() ?: ""
        @Suppress("UNCHECKED_CAST")
        return when (R::class) {
            String::class -> body as R
            Unit::class -> Unit as R
            else -> {
                val deserializer = R::class.serializer()
                JsonCodec.decodeFromString(deserializer, body)
            }
        }
    }
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal inline fun <reified B : Any> buildRequestBody(body: B): RequestBody? {
    if (body is Unit) return null
    val serializer = B::class.serializer()
    val json = JsonCodec.encodeToString(serializer, body)
    return json.toRequestBody("application/json".toMediaType())
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal inline fun <reified B : Any> buildMutationRequest(
    urlString: String,
    methodType: MutationMethodType,
    body: B,
    crossinline block: Request.Builder.() -> Unit
): Request {
    val hasBody = body !is Unit
    val requestBody = buildRequestBody(body)
    val builder = Request.Builder()
        .url(urlString)
        .apply(block)

    val method = methodType.toOkHttpMethod(hasBody)

    return when (method) {
        "POST" -> builder.post(requestBody ?: "".toRequestBody(null)).build()
        "PUT" -> builder.put(requestBody ?: "".toRequestBody(null)).build()
        "PATCH" -> builder.patch(requestBody ?: "".toRequestBody(null)).build()
        "DELETE" -> if (requestBody != null) builder.delete(requestBody).build() else builder.delete().build()
        else -> builder.method(method, requestBody).build()
    }
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal fun <Q : Any> appendQueryParametersOkHttp(
    urlString: String,
    api: WithQueryApi<Q>,
    query: Q
): String {
    val params = encodeQueryParams(query, api.queryClass)
    val base = urlString.toHttpUrlOrNull()
        ?: throw IllegalArgumentException("Invalid absolute URL for OkHttp: ${'$'}urlString")
    val b = base.newBuilder()
    params.forEach { (key, values) ->
        values.forEach { v -> b.addQueryParameter(key, v) }
    }
    return b.build().toString()
}

@PublishedApi
@OptIn(InternalSerializationApi::class)
internal fun <P : Any> buildPathUrlString(path: P, pathClass: KClass<P>, urlString: String): String {
    val pathParams = encodeQueryParams(path, pathClass)
    return pathParams.toList().fold(urlString) { acc, (key, value) ->
        acc.replace("{$key}", value.first())
    }
}

// Local copy of query encoder to avoid depending on :ktor:client
@OptIn(InternalSerializationApi::class)
internal fun <T : Any> encodeQueryParams(value: T, clazz: KClass<T>): Map<String, List<String>> {
    val serializer = clazz.serializer()
    val encoder = CustomParameterEncoder(clazz, serializer)
    encoder.encodeSerializableValue(serializer, value)
    return encoder.map
}

@OptIn(InternalSerializationApi::class)
internal class CustomParameterEncoder<T : Any>(
    clazz: KClass<T>,
    serializer: KSerializer<T>
) : NamedValueEncoder() {
    val map = mutableMapOf<String, MutableList<String>>()

    override val serializersModule: SerializersModule =
        serializersModuleOf(clazz, serializer)

    override fun encodeTaggedValue(tag: String, value: Any) {
        val newTag = tag.substringBeforeLast(".")
        when (value) {
            is Iterable<*> -> value.forEach { item ->
                if (item != null) map.getOrPut(newTag) { mutableListOf() }.add(item.toString())
            }
            is Array<*> -> value.forEach { item ->
                if (item != null) map.getOrPut(newTag) { mutableListOf() }.add(item.toString())
            }
            else -> map.getOrPut(newTag) { mutableListOf() }.add(value.toString())
        }
    }

    override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
        map.getOrPut(tag) { mutableListOf() }.add(enumDescriptor.getElementName(ordinal))
    }

    override fun encodeTaggedNull(tag: String) {
        // skip nulls
    }
}
