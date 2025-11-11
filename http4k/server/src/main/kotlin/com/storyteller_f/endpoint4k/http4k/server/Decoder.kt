package com.storyteller_f.endpoint4k.http4k.server

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import org.http4k.core.Request
import org.http4k.routing.path

/**
 * Minimal parameters abstraction over http4k Request for use in our decoder.
 */
internal interface SimpleParameters {
    fun get(name: String): String?
    fun getAll(name: String): List<String>?
    fun contains(name: String): Boolean = get(name) != null
}

internal class QueryParameters(private val request: Request) : SimpleParameters {
    override fun get(name: String): String? = request.query(name)
    override fun getAll(name: String): List<String>? = request.queries(name)?.filterNotNull()
}

internal class PathParameters(private val request: Request) : SimpleParameters {
    override fun get(name: String): String? = request.path(name)
    override fun getAll(name: String): List<String>? = request.path(name)?.let { listOf(it) }
}

@OptIn(ExperimentalSerializationApi::class)
internal class ParametersDecoder(
    override val serializersModule: SerializersModule,
    private val parameters: SimpleParameters,
    elementNames: Iterable<String>
) : AbstractDecoder() {

    private val parameterNames = elementNames.iterator()
    private lateinit var currentName: String

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (!parameterNames.hasNext()) return CompositeDecoder.DECODE_DONE
        while (parameterNames.hasNext()) {
            currentName = parameterNames.next()
            val elementIndex = descriptor.getElementIndex(currentName)
            val elementDescriptorKind = descriptor.getElementDescriptor(elementIndex).kind
            val isPrimitive = elementDescriptorKind is PrimitiveKind
            val isEnum = elementDescriptorKind is SerialKind.ENUM
            if (!(isPrimitive || isEnum) || parameters.contains(currentName)) {
                return elementIndex
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (descriptor.kind == StructureKind.LIST) {
            return ListLikeDecoder(serializersModule, parameters, currentName)
        }
        return ParametersDecoder(serializersModule, parameters, descriptor.elementNames)
    }

    override fun decodeBoolean(): Boolean = decodeString().toBoolean()
    override fun decodeByte(): Byte = decodeString().toByte()
    override fun decodeChar(): Char = decodeString()[0]
    override fun decodeDouble(): Double = decodeString().toDouble()
    override fun decodeFloat(): Float = decodeString().toFloat()
    override fun decodeInt(): Int = decodeString().toInt()
    override fun decodeLong(): Long = decodeString().toLong()
    override fun decodeShort(): Short = decodeString().toShort()

    override fun decodeString(): String = parameters.get(currentName)!!

    override fun decodeNotNullMark(): Boolean = parameters.contains(currentName)
    override fun decodeNull(): Nothing? = null

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val enumName = decodeString()
        val index = enumDescriptor.getElementIndex(enumName)
        if (index == CompositeDecoder.UNKNOWN_NAME) {
            throw Exception("${enumDescriptor.serialName} does not contain element with name '$enumName'")
        }
        return index
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class ListLikeDecoder(
    override val serializersModule: SerializersModule,
    private val parameters: SimpleParameters,
    private val parameterName: String
) : AbstractDecoder() {

    private var currentIndex = -1
    private val elementsCount = parameters.getAll(parameterName)?.size ?: 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (++currentIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return currentIndex
    }

    override fun decodeBoolean(): Boolean = decodeString().toBoolean()
    override fun decodeByte(): Byte = decodeString().toByte()
    override fun decodeChar(): Char = decodeString()[0]
    override fun decodeDouble(): Double = decodeString().toDouble()
    override fun decodeFloat(): Float = decodeString().toFloat()
    override fun decodeInt(): Int = decodeString().toInt()
    override fun decodeLong(): Long = decodeString().toLong()
    override fun decodeShort(): Short = decodeString().toShort()

    override fun decodeString(): String = parameters.getAll(parameterName)!![currentIndex]

    override fun decodeNotNullMark(): Boolean = parameters.contains(parameterName)
    override fun decodeNull(): Nothing? = null

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val enumName = decodeString()
        val index = enumDescriptor.getElementIndex(enumName)
        if (index == CompositeDecoder.UNKNOWN_NAME) {
            throw Exception("${enumDescriptor.serialName} does not contain element with name '$enumName'")
        }
        return index
    }
}

// Public helpers to extract parameter decoders for a given request
internal fun queryParametersOf(request: Request): SimpleParameters = QueryParameters(request)
internal fun pathParametersOf(request: Request): SimpleParameters = PathParameters(request)
