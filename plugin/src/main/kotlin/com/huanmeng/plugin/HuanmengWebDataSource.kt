package com.huanmeng.plugin

import android.net.Uri
import com.huanmeng.plugin.model.HuanmengBookDetail
import com.huanmeng.plugin.model.HuanmengBookList
import com.huanmeng.plugin.model.HuanmengChapterList
import com.huanmeng.plugin.model.HuanmengContentData
import com.huanmeng.plugin.model.HuanmengBookItem
import com.huanmeng.plugin.model.HuanmengResponse
import com.huanmeng.plugin.utils.KotlinSerializationJsonConverter
import cxhttp.CxHttpHelper
import cxhttp.request.get
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.book.ChapterInformation
import io.nightfish.lightnovelreader.api.book.MutableBookInformation
import io.nightfish.lightnovelreader.api.book.Volume
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.content.builder.ContentBuilder
import io.nightfish.lightnovelreader.api.content.builder.image
import io.nightfish.lightnovelreader.api.content.builder.simpleText
import io.nightfish.lightnovelreader.api.explore.ExploreDisplayBook
import io.nightfish.lightnovelreader.api.explore.ExploreBooksRow
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExploreExpandedPageDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.explore.ExploreTapPageDataSource
import io.nightfish.lightnovelreader.api.web.explore.filter.Filter
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.lightnovelreader.api.web.search.SearchResult
import io.nightfish.lightnovelreader.api.web.search.SearchType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val BASE_URL = "https://www.huanmengacg.com/index.php/bookapi"
private const val PASSWORD = "huanmengapi"
private const val UA =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

/**
 * 幻梦轻小说 LNR Web 数据源
 *
 * 搜索：  GET /search?password=&key=&page=&size=
 * 详情：  GET /detail?password=&id=
 * 目录：  GET /chapters?password=&id=&size=5000
 * 正文：  GET /content?password=&bid=&cid=
 * 探索：  GET /search?password=&state=|tags=&page=&size=
 */
@Suppress("unused")
@WebDataSource(
    name = "幻梦轻小说",
    provider = "huanmengacg.com"
)
class HuanmengWebDataSource : WebBookDataSource {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        @Suppress("OPT_IN_USAGE")
        CxHttpHelper.init(
            scope = MainScope(),
            debugLog = false,
            converter = KotlinSerializationJsonConverter()
        )
    }

    override val id: Int = "huanmengacg.com".hashCode()
    override suspend fun isOffLine(): Boolean = false
    override val offLine: Boolean = false
    override val isOffLineFlow: StateFlow<Boolean> = MutableStateFlow(false)

    // ================================================================
    // 探索页面
    // ================================================================

    override val explorePageProvider: ExplorePageProvider =
        object : ExplorePageProvider.DefaultExplorePageProvider {

            // 定义所有标签页 ID
            override val explorePageIdList: List<String> = listOf(
                "latest",
                "ongoing",
                "completed",
                "tag_1", "tag_2", "tag_3", "tag_4", "tag_16",
                "tag_17", "tag_26", "tag_34", "tag_46"
            )

            override val exploreTapPageDataSourceMap: Map<String, ExploreTapPageDataSource> = mapOf(
                "latest"    to buildTapPage("🍑 最新更新", null, null),
                "ongoing"   to buildTapPage("连载中", "state", "1"),
                "completed" to buildTapPage("已完结", "state", "2"),
                "tag_1"     to buildTapPage("校园", "tags", "1"),
                "tag_2"     to buildTapPage("青春", "tags", "2"),
                "tag_3"     to buildTapPage("恋爱", "tags", "3"),
                "tag_4"     to buildTapPage("治愈", "tags", "4"),
                "tag_16"    to buildTapPage("穿越", "tags", "16"),
                "tag_17"    to buildTapPage("奇幻", "tags", "17"),
                "tag_26"    to buildTapPage("悬疑", "tags", "26"),
                "tag_34"    to buildTapPage("游戏", "tags", "34"),
                "tag_46"    to buildTapPage("百合", "tags", "46")
            )

            override val exploreExpandedPageDataSourceMap: Map<String, ExploreExpandedPageDataSource> = mapOf(
                "latest_exp"    to buildExpandedPage("最新更新", null, null),
                "ongoing_exp"   to buildExpandedPage("连载中", "state", "1"),
                "completed_exp" to buildExpandedPage("已完结", "state", "2")
            )
        }

    // ================================================================
    // 搜索
    // ================================================================

    override val searchProvider: SearchProvider = object : SearchProvider {

        override val searchTypes: List<SearchType> = listOf(
            SearchType("keyword", "关键词")
        )

        override fun search(searchType: SearchType, keyword: String): Flow<SearchResult> = flow {
            var page = 1
            while (true) {
                try {
                    val books = fetchBookList(key = keyword, page = page, size = 20)
                    val list = books?.list ?: emptyList()
                    if (list.isEmpty()) {
                        emit(SearchResult.End())
                        break
                    }
                    list.forEach { emit(SearchResult.MultipleBook(it.toBookInformation())) }
                    if (list.size < 20) {
                        emit(SearchResult.End())
                        break
                    }
                    page++
                } catch (e: Exception) {
                    emit(SearchResult.Error(e))
                    break
                }
            }
        }
    }

    // ================================================================
    // 书籍详情
    // ================================================================

    override suspend fun getBookInformation(id: String): BookInformation {
        return try {
            val resp = CxHttpHelper.get<HuanmengResponse<HuanmengBookDetail>>(
                "$BASE_URL/detail"
            ) {
                param("password", PASSWORD)
                param("id", id)
                header("User-Agent", UA)
            }.await()
            resp.data?.toBookInformation() ?: BookInformation.empty(id)
        } catch (e: Exception) {
            BookInformation.empty(id)
        }
    }

    // ================================================================
    // 章节目录（按卷分组）
    // ================================================================

    override suspend fun getBookVolumes(id: String): BookVolumes {
        return try {
            val resp = CxHttpHelper.get<HuanmengResponse<HuanmengChapterList>>(
                "$BASE_URL/chapters"
            ) {
                param("password", PASSWORD)
                param("id", id)
                param("size", "5000")
                header("User-Agent", UA)
            }.await()

            val chapters = resp.data?.list ?: return BookVolumes.empty(id)

            // 按 volumeId 分组
            val groupMap = linkedMapOf<Int, Pair<String, MutableList<ChapterInformation>>>()
            for (ch in chapters) {
                val vId = ch.volumeId
                val vName = ch.volumeName.ifBlank { "正文" }
                groupMap.getOrPut(vId) { Pair(vName, mutableListOf()) }
                    .second.add(
                        ChapterInformation(
                            id = "${ch.bid}_${ch.id}",
                            title = ch.name
                        )
                    )
            }

            val volumes = groupMap.map { (vId, pair) ->
                Volume(
                    volumeId = vId.toString(),
                    volumeTitle = pair.first,
                    chapters = pair.second
                )
            }

            BookVolumes(bookId = id, volumes = volumes)
        } catch (e: Exception) {
            BookVolumes.empty(id)
        }
    }

    // ================================================================
    // 章节正文
    // ================================================================

    override suspend fun getChapterContent(chapterId: String, bookId: String): ChapterContent {
        return try {
            val parts = chapterId.split("_")
            if (parts.size != 2) return ChapterContent.empty(chapterId)
            val (bid, cid) = parts

            val resp = CxHttpHelper.get<HuanmengResponse<HuanmengContentData>>(
                "$BASE_URL/content"
            ) {
                param("password", PASSWORD)
                param("bid", bid)
                param("cid", cid)
                header("User-Agent", UA)
            }.await()

            val raw = resp.data?.content ?: return ChapterContent.empty(chapterId)

            val builder = ContentBuilder()
            parseHtmlContent(raw, builder)

            val mutable = ChapterContent.empty(chapterId).toMutable()
            mutable.id = chapterId
            mutable.content = builder.build()
            mutable
        } catch (e: Exception) {
            ChapterContent.empty(chapterId)
        }
    }

    // ================================================================
    // 私有工具方法
    // ================================================================

    /**
     * 解析 HTML 正文，将文本段和 <img> 交替拆分后加入 builder
     */
    private fun parseHtmlContent(html: String, builder: ContentBuilder) {
        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        var lastEnd = 0

        for (match in imgRegex.findAll(html)) {
            val textBefore = html.substring(lastEnd, match.range.first).cleanHtml()
            if (textBefore.isNotBlank()) builder.simpleText(textBefore.trim())

            val imgUrl = match.groupValues[1]
            if (imgUrl.isNotBlank()) builder.image(Uri.parse(imgUrl))

            lastEnd = match.range.last + 1
        }

        val remaining = html.substring(lastEnd).cleanHtml()
        if (remaining.isNotBlank()) builder.simpleText(remaining.trim())

        // 如果没有任何内容，放原始文本
        if (builder.components.isEmpty()) {
            builder.simpleText(html.cleanHtml().trim())
        }
    }

    /** 构造标签页数据源（加载首页20本） */
    private fun buildTapPage(
        pageTitle: String,
        filterKey: String?,
        filterValue: String?
    ): ExploreTapPageDataSource = object : ExploreTapPageDataSource {

        override val title: String = pageTitle

        override fun getRowsFlow(): Flow<List<ExploreBooksRow>> = flow {
            val books = fetchBookList(
                paramKey = filterKey,
                paramValue = filterValue,
                page = 1,
                size = 20
            )
            val displayBooks = (books?.list ?: emptyList()).map { it.toExploreDisplayBook() }
            val expandedId = when {
                filterKey == null -> "latest_exp"
                filterKey == "state" && filterValue == "1" -> "ongoing_exp"
                filterKey == "state" && filterValue == "2" -> "completed_exp"
                else -> null
            }
            emit(
                listOf(
                    ExploreBooksRow(
                        title = pageTitle,
                        bookList = displayBooks,
                        expandable = expandedId != null,
                        expandedPageDataSourceId = expandedId
                    )
                )
            )
        }
    }

    /** 构造可翻页扩展数据源 */
    private fun buildExpandedPage(
        pageTitle: String,
        filterKey: String?,
        filterValue: String?
    ): ExploreExpandedPageDataSource = object : ExploreExpandedPageDataSource {

        override val title: String = pageTitle
        override val filters: List<Filter<*>> = emptyList()

        private val _resultFlow = MutableStateFlow<SearchResult>(SearchResult.Empty())
        private var currentPage = 1
        private var loading = false

        override fun loadMore() {
            if (loading) return
            loading = true
            ioScope.launch {
                try {
                    val books = fetchBookList(
                        paramKey = filterKey,
                        paramValue = filterValue,
                        page = currentPage,
                        size = 20
                    )
                    val list = books?.list ?: emptyList()
                    if (list.isEmpty()) {
                        _resultFlow.emit(SearchResult.End())
                    } else {
                        list.forEach {
                            _resultFlow.emit(SearchResult.MultipleBook(it.toBookInformation()))
                        }
                        currentPage++
                        if (list.size < 20) _resultFlow.emit(SearchResult.End())
                    }
                } catch (e: Exception) {
                    _resultFlow.emit(SearchResult.Error(e))
                } finally {
                    loading = false
                }
            }
        }

        override fun getResultFlow(): Flow<SearchResult> = _resultFlow
    }

    /** 发起书单请求（搜索 / 探索复用） */
    private suspend fun fetchBookList(
        key: String? = null,
        paramKey: String? = null,
        paramValue: String? = null,
        page: Int = 1,
        size: Int = 20
    ): HuanmengBookList? {
        val resp = CxHttpHelper.get<HuanmengResponse<HuanmengBookList>>(
            "$BASE_URL/search"
        ) {
            param("password", PASSWORD)
            param("page", page.toString())
            param("size", size.toString())
            if (!key.isNullOrBlank()) param("key", key)
            if (paramKey != null && paramValue != null) param(paramKey, paramValue)
            header("User-Agent", UA)
        }.await()
        return resp.data
    }
}

// ================================================================
// 扩展函数 —— 模型转换
// ================================================================

private fun HuanmengBookItem.toBookInformation(): BookInformation {
    return MutableBookInformation.empty().apply {
        id = this@toBookInformation.id.toString()
        title = name
        author = this@toBookInformation.author
        coverUri = Uri.parse(pic)
        description = intro
        tags.clear()
        if (kind.isNotBlank()) {
            tags.addAll(kind.split(",", "，", " ").filter { it.isNotBlank() })
        }
        wordCount = WordCount(textNum)
        lastUpdated = updateTime.parseDateTime()
    }
}

private fun HuanmengBookDetail.toBookInformation(): BookInformation {
    return MutableBookInformation.empty().apply {
        id = this@toBookInformation.id.toString()
        title = name
        author = this@toBookInformation.author
        coverUri = Uri.parse(pic)
        description = intro
        tags.clear()
        val allTags = buildList {
            if (kind.isNotBlank()) addAll(kind.split(",", "，", " ").filter { it.isNotBlank() })
            if (tags.isNotBlank()) addAll(tags.split(",", "，", " ").filter { it.isNotBlank() })
        }.distinct()
        this.tags.addAll(allTags)
        wordCount = WordCount(textNum)
        lastUpdated = updateTime.parseDateTime()
        isComplete = state == 2
    }
}

private fun HuanmengBookItem.toExploreDisplayBook(): ExploreDisplayBook =
    ExploreDisplayBook(
        id = this.id.toString(),
        title = this.name,
        author = this.author,
        coverUri = Uri.parse(this.pic)
    )

/** 安全解析日期时间字符串 */
private fun String.parseDateTime(): LocalDateTime {
    val formatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ISO_DATE_TIME
    )
    for (fmt in formatters) {
        try { return LocalDateTime.parse(this, fmt) } catch (_: DateTimeParseException) {}
    }
    return LocalDateTime.MIN
}

/** 去除 HTML 标签，将换行标签转换为 \n */
private fun String.cleanHtml(): String = this
    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<[^>]+>"), "")
    .replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()
