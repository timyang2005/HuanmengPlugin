package com.huanmeng.plugin

import android.net.Uri
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.WordCount
import java.time.LocalDateTime

/**
 * 自定义 BookInformation 实现
 *
 * 不使用 MutableBookInformation，因为其构造函数参数包含 LocalDateTime，
 * 在外部插件中 d8 desugaring 不会改写外部类方法引用中的 java.time → j$.time，
 * 导致运行时 NoSuchMethodError。
 *
 * 参考 LNR 官方 JS 插件的 JsBookInformation 模式，直接实现 BookInformation 接口。
 */
data class HuanmengBookInformation(
    override val id: String,
    override val title: String,
    override val subtitle: String,
    override val coverUri: Uri,
    override val author: String,
    override val description: String,
    override val tags: List<String>,
    override val publishingHouse: String,
    override val wordCount: WordCount,
    override val lastUpdated: LocalDateTime,
    override val isComplete: Boolean
) : BookInformation
