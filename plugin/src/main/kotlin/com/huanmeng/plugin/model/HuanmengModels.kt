package com.huanmeng.plugin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
// ============================================================

@Serializable
data class HuanmengBookItem(
    val id: Int = 0,
    val name: String = "",
    val author: String = "",
    val pic: String = "",
    val intro: String = "",
    val kind: String = "",
    @SerialName("text_num") val textNum: Int = 0,
    @SerialName("update_time") val updateTime: String = ""
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
// ============================================================

@Serializable
data class HuanmengBookDetail(
    val id: Int = 0,
    val name: String = "",
    val author: String = "",
    val pic: String = "",
    val intro: String = "",
    val kind: String = "",
    val tags: String = "",
    @SerialName("text_num") val textNum: Int = 0,
    @SerialName("update_time") val updateTime: String = "",
    val state: Int = 0   // 1=连载中 2=已完结
)

// ============================================================
// 目录（章节列表）
// ============================================================

@Serializable
data class HuanmengChapterItem(
    val id: Int = 0,
    val bid: Int = 0,
    val name: String = "",
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
