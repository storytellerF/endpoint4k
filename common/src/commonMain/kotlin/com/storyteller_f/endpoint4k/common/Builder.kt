@file:Suppress("UnusedReceiverParameter", "unused")

package com.storyteller_f.endpoint4k.common

import kotlin.reflect.KClass

fun <Resp : Any> SafeEndpoint<Resp>.resp(resp: KClass<Resp>) = Unit

inline fun <Resp : Any> safeEndpointBuilder(
    path: String,
    methodType: SafeMethodType = SafeMethodType.GET,
    build: SafeEndpoint<Resp>.() -> Unit = {}
): SafeEndpoint<Resp> {
    return SafeEndpoint<Resp>(
        path,
        methodType
    ).apply {
        build()
    }
}

inline fun <Resp : Any, reified Query : Any> SafeEndpointWithQuery<Resp, Query>.resp(resp: KClass<Resp>) = Unit

inline fun <Resp : Any, reified Query : Any> SafeEndpointWithQuery<Resp, Query>.query(query: KClass<Query>) = Unit

inline fun <Resp : Any, reified Query : Any> safeEndpointWithQueryBuilder(
    path: String,
    methodType: SafeMethodType = SafeMethodType.GET,
    build: SafeEndpointWithQuery<Resp, Query>.() -> Unit = {}
): SafeEndpointWithQuery<Resp, Query> {
    return SafeEndpointWithQuery<Resp, Query>(
        path,
        Query::class,
        methodType
    ).apply {
        build()
    }
}

inline fun <Resp : Any, reified PathQuery : Any> SafeEndpointWithPath<Resp, PathQuery>.resp(resp: KClass<Resp>) = Unit

inline fun <Resp : Any, reified PathQuery : Any> SafeEndpointWithPath<Resp, PathQuery>.path(path: KClass<PathQuery>) =
    Unit

inline fun <Resp : Any, reified PathQuery : Any> safeEndpointWithPathBuilder(
    path: String,
    methodType: SafeMethodType = SafeMethodType.GET,
    build: SafeEndpointWithPath<Resp, PathQuery>.() -> Unit = {}
): SafeEndpointWithPath<Resp, PathQuery> {
    return SafeEndpointWithPath<Resp, PathQuery>(
        path,
        PathQuery::class,
        methodType
    ).apply {
        build()
    }
}

inline fun <Resp : Any, reified Query : Any, reified PathQuery : Any>
    SafeEndpointWithQueryAndPath<Resp, Query, PathQuery>.resp(resp: KClass<Resp>) = Unit

inline fun <Resp : Any, reified Query : Any, reified PathQuery : Any>
    SafeEndpointWithQueryAndPath<Resp, Query, PathQuery>.query(query: KClass<Query>) = Unit

inline fun <Resp : Any, reified Query : Any, reified PathQuery : Any>
    SafeEndpointWithQueryAndPath<Resp, Query, PathQuery>.path(path: KClass<PathQuery>) = Unit

inline fun <Resp : Any, reified Query : Any, reified PathQuery : Any> safeEndpointWithQueryAndPathBuilder(
    path: String,
    methodType: SafeMethodType = SafeMethodType.GET,
    build: SafeEndpointWithQueryAndPath<Resp, Query, PathQuery>.() -> Unit = {}
): SafeEndpointWithQueryAndPath<Resp, Query, PathQuery> {
    return SafeEndpointWithQueryAndPath<Resp, Query, PathQuery>(
        path,
        Query::class,
        PathQuery::class,
        methodType
    ).apply {
        build()
    }
}

fun <Resp : Any, Body : Any> MutationEndpoint<Resp, Body>.resp(resp: KClass<Resp>) = Unit

fun <Resp : Any, Body : Any> MutationEndpoint<Resp, Body>.body(body: KClass<Body>) = Unit

inline fun <Resp : Any, Body : Any> mutationEndpointBuilder(
    path: String,
    methodType: MutationMethodType = MutationMethodType.POST,
    build: MutationEndpoint<Resp, Body>.() -> Unit = {}
): MutationEndpoint<Resp, Body> {
    return MutationEndpoint<Resp, Body>(
        path,
        methodType
    ).apply {
        build()
    }
}

inline fun <Resp : Any, Body : Any, reified Query : Any>
    MutationEndpointWithQuery<Resp, Body, Query>.resp(resp: KClass<Resp>) = Unit

inline fun <Resp : Any, Body : Any, reified Query : Any>
    MutationEndpointWithQuery<Resp, Body, Query>.body(body: KClass<Body>) = Unit

inline fun <Resp : Any, Body : Any, reified Query : Any>
    MutationEndpointWithQuery<Resp, Body, Query>.query(query: KClass<Query>) = Unit

inline fun <Resp : Any, Body : Any, reified Query : Any> mutationEndpointWithQueryBuilder(
    path: String,
    methodType: MutationMethodType = MutationMethodType.POST,
    build: MutationEndpointWithQuery<Resp, Body, Query>.() -> Unit = {}
): MutationEndpointWithQuery<Resp, Body, Query> {
    return MutationEndpointWithQuery<Resp, Body, Query>(
        path,
        Query::class,
        methodType
    ).apply {
        build()
    }
}

inline fun <Resp : Any, Body : Any, reified Path : Any>
    MutationEndpointWithPath<Resp, Body, Path>.resp(resp: KClass<Resp>) = Unit

inline fun <Resp : Any, Body : Any, reified Path : Any>
    MutationEndpointWithPath<Resp, Body, Path>.body(body: KClass<Body>) = Unit

inline fun <Resp : Any, Body : Any, reified Path : Any>
    MutationEndpointWithPath<Resp, Body, Path>.path(path: KClass<Path>) = Unit

inline fun <Resp : Any, Body : Any, reified Path : Any> mutationEndpointWithPathBuilder(
    path: String,
    methodType: MutationMethodType = MutationMethodType.POST,
    build: MutationEndpointWithPath<Resp, Body, Path>.() -> Unit = {}
): MutationEndpointWithPath<Resp, Body, Path> {
    return MutationEndpointWithPath<Resp, Body, Path>(
        path,
        Path::class,
        methodType
    ).apply {
        build()
    }
}

inline fun <Resp : Any, Body : Any, reified Query : Any, reified PathQuery : Any>
    MutationEndpointWithQueryAndPath<Resp, Body, Query, PathQuery>.resp(resp: KClass<Resp>) = Unit

inline fun <Resp : Any, Body : Any, reified Query : Any, reified PathQuery : Any>
    MutationEndpointWithQueryAndPath<Resp, Body, Query, PathQuery>.body(body: KClass<Body>) = Unit

inline fun <Resp : Any, Body : Any, reified Query : Any, reified PathQuery : Any>
    MutationEndpointWithQueryAndPath<Resp, Body, Query, PathQuery>.query(query: KClass<Query>) = Unit

inline fun <Resp : Any, Body : Any, reified Query : Any, reified PathQuery : Any>
    MutationEndpointWithQueryAndPath<Resp, Body, Query, PathQuery>.path(path: KClass<PathQuery>) = Unit

inline fun <Resp : Any, Body : Any, reified Query : Any, reified PathQuery : Any>
mutationEndpointWithQueryAndPathBuilder(
    path: String,
    methodType: MutationMethodType = MutationMethodType.POST,
    build: MutationEndpointWithQueryAndPath<Resp, Body, Query, PathQuery>.() -> Unit = {}
): MutationEndpointWithQueryAndPath<Resp, Body, Query, PathQuery> {
    return MutationEndpointWithQueryAndPath<Resp, Body, Query, PathQuery>(
        path,
        Query::class,
        PathQuery::class,
        methodType
    ).apply {
        build()
    }
}
