@file:Suppress("OPT_IN_USAGE")

package com.huanmeng.plugin.utils

import cxhttp.converter.CxHttpConverter
import cxhttp.response.CxHttpResult
import cxhttp.response.Response
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.serializer
import java.lang.reflect.Type

/**
 * 基于 Kotlin Serialization JSON 的 CxHttp 转换器
 * 幻梦轻小说 API 使用标准 JSON 格式，不需要 CBOR
 */
@Suppress("UNCHECKED_CAST")
class KotlinSerializationJsonConverter(
    json: Json = Json,
    builderAction: JsonBuilder.() -> Unit = {}
) : CxHttpConverter {
    override val contentType: String = "application/json"

    private val json = Json(json) {
        ignoreUnknownKeys = true
        coerceInputValues = true
        builderAction()
    }

    override fun <T> convert(body: Response.Body, tType: Class<T>): T {
        return json.decodeFromString(json.serializersModule.serializer(tType), body.string()) as T
    }

    override fun <T, RESULT : CxHttpResult<T>> convertResult(
        body: Response.Body,
        resultType: Class<RESULT>,
        tType: Type
    ): RESULT {
        return json.decodeFromString(
            json.serializersModule.serializer(resultType),
            body.string()
        ) as RESULT
    }

    override fun <T, RESULT : CxHttpResult<List<T>>> convertResultList(
        body: Response.Body,
        resultType: Class<RESULT>,
        tType: Type
    ): RESULT {
        return json.decodeFromString(
            json.serializersModule.serializer(resultType),
            body.string()
        ) as RESULT
    }

    override fun <T> convert(value: T, tType: Class<out T>): ByteArray {
        return json.encodeToString(
            json.serializersModule.serializer(tType),
            value as Any
        ).toByteArray(Charsets.UTF_8)
    }
}
