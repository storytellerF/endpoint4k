package com.storyteller_f.endpoint4k.common

import kotlin.reflect.KClass

enum class MutationMethodType {
    POST, PUT, PATCH, DELETE
}

enum class SafeMethodType {
    GET, OPTIONS
}

sealed interface AbstractEndpoint<Resp> {
    val urlString: String
}

/**
 * 有时Body 和框架相关，这时最好把Body 设置为Unit，然后手动设置Body
 * ```
 * API.Files.upload(
 *     /* */,
 *     /* */,
 *     Unit
 * ) {
 *     setBody(ByteReadChannel(input))
 * }
 * ```
 */
interface AbstractMutationEndpoint<Resp, Body> : AbstractEndpoint<Resp> {
    val methodType: MutationMethodType
}

interface AbstractSafeEndpoint<Resp> : AbstractEndpoint<Resp> {
    val methodType: SafeMethodType
}

interface WithQueryEndpoint<T : Any> {
    val queryClass: KClass<T>
}

class SafeEndpoint<Resp : Any>(
    override val urlString: String,
    override val methodType: SafeMethodType,
) : AbstractSafeEndpoint<Resp>

class SafeEndpointWithQuery<Resp : Any, Query : Any>(
    override val urlString: String,
    override val queryClass: KClass<Query>,
    override val methodType: SafeMethodType,
) : AbstractSafeEndpoint<Resp>, WithQueryEndpoint<Query>

class SafeEndpointWithPath<Resp : Any, PathQuery : Any>(
    override val urlString: String,
    val pathClass: KClass<PathQuery>,
    override val methodType: SafeMethodType
) : AbstractSafeEndpoint<Resp>

class SafeEndpointWithQueryAndPath<Resp : Any, Query : Any, PathQuery : Any>(
    override val urlString: String,
    override val queryClass: KClass<Query>,
    val pathClass: KClass<PathQuery>,
    override val methodType: SafeMethodType
) : AbstractSafeEndpoint<Resp>, WithQueryEndpoint<Query>

class MutationEndpoint<Resp : Any, Body : Any>(
    override val urlString: String,
    override val methodType: MutationMethodType,
) : AbstractMutationEndpoint<Resp, Body>

class MutationEndpointWithQuery<Resp : Any, Body : Any, Query : Any>(
    override val urlString: String,
    override val queryClass: KClass<Query>,
    override val methodType: MutationMethodType,
) : AbstractMutationEndpoint<Resp, Body>, WithQueryEndpoint<Query>

class MutationEndpointWithPath<Resp : Any, Body : Any, PathQuery : Any>(
    override val urlString: String,
    val pathClass: KClass<PathQuery>,
    override val methodType: MutationMethodType
) : AbstractMutationEndpoint<Resp, Body>

class MutationEndpointWithQueryAndPath<Resp : Any, Body : Any, Query : Any, PathQuery : Any>(
    override val urlString: String,
    override val queryClass: KClass<Query>,
    val pathClass: KClass<PathQuery>,
    override val methodType: MutationMethodType
) : AbstractMutationEndpoint<Resp, Body>, WithQueryEndpoint<Query>

inline fun <Resp : Any, reified Query : Any, reified PathQuery : Any> safeEndpointWithQueryAndPath(
    path: String,
    methodType: SafeMethodType = SafeMethodType.GET
): SafeEndpointWithQueryAndPath<Resp, Query, PathQuery> {
    return SafeEndpointWithQueryAndPath(
        path,
        Query::class,
        PathQuery::class,
        methodType
    )
}

inline fun <Resp : Any, reified Query : Any> safeEndpointWithQuery(
    path: String,
    methodType: SafeMethodType = SafeMethodType.GET
): SafeEndpointWithQuery<Resp, Query> {
    return SafeEndpointWithQuery(
        path,
        Query::class,
        methodType
    )
}

inline fun <Resp : Any, reified Path : Any> safeEndpointWithPath(
    path: String,
    methodType: SafeMethodType = SafeMethodType.GET
): SafeEndpointWithPath<Resp, Path> = SafeEndpointWithPath(
    path,
    Path::class,
    methodType
)

fun <Resp : Any> safeEndpoint(
    path: String,
    methodType: SafeMethodType = SafeMethodType.GET
): SafeEndpoint<Resp> {
    return SafeEndpoint(
        path,
        methodType
    )
}

inline fun <Resp : Any, Body : Any, reified Query : Any, reified PathQuery : Any> mutationEndpointWithQueryAndPath(
    path: String,
    methodType: MutationMethodType = MutationMethodType.POST
): MutationEndpointWithQueryAndPath<Resp, Body, Query, PathQuery> {
    return MutationEndpointWithQueryAndPath(
        path,
        Query::class,
        PathQuery::class,
        methodType
    )
}

inline fun <Resp : Any, Body : Any, reified Query : Any> mutationEndpointWithQuery(
    path: String,
    methodType: MutationMethodType = MutationMethodType.POST
): MutationEndpointWithQuery<Resp, Body, Query> {
    return MutationEndpointWithQuery(
        path,
        Query::class,
        methodType
    )
}

inline fun <Resp : Any, Body : Any, reified Path : Any> mutationEndpointWithPath(
    path: String,
    methodType: MutationMethodType = MutationMethodType.POST
): MutationEndpointWithPath<Resp, Body, Path> = MutationEndpointWithPath(
    path,
    Path::class,
    methodType
)

fun <Resp : Any, Body : Any> mutationEndpoint(
    path: String,
    methodType: MutationMethodType = MutationMethodType.POST
): MutationEndpoint<Resp, Body> {
    return MutationEndpoint(
        path,
        methodType
    )
}
