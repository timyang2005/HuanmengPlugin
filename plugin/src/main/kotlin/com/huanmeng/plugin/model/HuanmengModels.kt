package com.huanmeng.plugin.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// ============================================================
// 自定义序列化器：兼容 String 和 Int 类型
// API 返回的 id/state 等字段有时是字符串有时是数字
// ============================================================

object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        return try {
            decoder.decodeInt()
        } catch (_: Exception) {
            try {
                decoder.decodeString().toIntOrNull() ?: 0
            } catch (_: Exception) {
                0
            }
        }
    }
}

object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        return try {
            decoder.decodeString()
        } catch (_: Exception) {
            try {
                decoder.decodeInt().toString()
            } catch (_: Exception) {
                ""
            }
        }
    }
}

// ============================================================
// 通用响应封装
// ============================================================

@Serializable
data class HuanmengResponse<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null
)

// ============================================================
// 搜索 / 探索书单 — 列表项
// API 实际字段: id(String), name, pic, author, state(String),
//   text(intro), text_num(String/Int), tags, addtime, picx, intro, kind
// ============================================================

@Serializable
data class HuanmengBookItem(
    @Serializable(FlexibleIntSerializer::class)
    val id: Int = 0,
    val name: String = "",
    val author: String = "",
    val pic: String = "",
    val intro: String = "",
    val kind: String = "",
    @Serializable(FlexibleStringSerializer::class)
    @SerialName("text_num") val textNum: String = "",
    @SerialName("addtime") val addtime: String = ""
)

@Serializable
data class HuanmengBookList(
    val list: List<HuanmengBookItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20
)

// ============================================================
// 书籍详情
// API 实际字段: id(String), cid, name, pic, author, state(String),
//   text(intro), text_num(String), nums, tags, addtime, picx, intro, kind
// ============================================================

@Serializable
data class HuanmengBookDetail(
    @Serializable(FlexibleIntSerializer::class)
    val id: Int = 0,
    val name: String = "",
    val author: String = "",
    val pic: String = "",
    val intro: String = "",
    val kind: String = "",
    val tags: String = "",
    @Serializable(FlexibleStringSerializer::class)
    @SerialName("text_num") val textNum: String = "",
    @SerialName("addtime") val addtime: String = "",
    val state: String = ""   // API返回 "连载" 或 "完结"
)

// ============================================================
// 目录（章节列表）
// ============================================================

@Serializable
data class HuanmengChapterItem(
    @Serializable(FlexibleIntSerializer::class)
    val id: Int = 0,
    @Serializable(FlexibleIntSerializer::class)
    val bid: Int = 0,
    val name: String = "",
    @Serializable(FlexibleIntSerializer::class)
    @SerialName("volume_id") val volumeId: Int = 0,
    @SerialName("volume_name") val volumeName: String = ""
)

@Serializable
data class HuanmengChapterList(
    val list: List<HuanmengChapterItem> = emptyList()
)

// ============================================================
// 章节正文
// ============================================================

@Serializable
data class HuanmengContentData(
    val content: String = ""
)
