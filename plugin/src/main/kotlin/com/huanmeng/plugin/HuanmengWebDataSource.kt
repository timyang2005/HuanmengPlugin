package com.huanmeng.plugin

import android.net.Uri
import com.huanmeng.plugin.model.HuanmengBookDetail
import com.huanmeng.plugin.model.HuanmengBookList
import com.huanmeng.plugin.model.HuanmengChapterList
import com.huanmeng.plugin.model.HuanmengContentData
import com.huanmeng.plugin.model.HuanmengBookItem
import com.huanmeng.plugin.model.HuanmengResponse
import com.huanmeng.plugin.utils.KotlinSerializationJsonConverter
import cxhttp.CxHttp
import cxhttp.CxHttpHelper
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.book.ChapterInformation
import com.huanmeng.plugin.HuanmengBookInformation
import io.nightfish.lightnovelreader.api.book.Volume
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.content.builder.ContentBuilder
import io.nightfish.lightnovelreader.api.content.builder.image
import io.nightfish.lightnovelreader.api.content.builder.simpleText
import io.nightfish.lightnovelreader.api.explore.ExploreDisplayBook
import io.nightfish.lightnovelreader.api.explore.ExploreBooksRow
import io.nightfish.lightnovelreader.api.util.LocalString
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExploreExpandedPageDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.explore.ExploreTapPageDataSource
import io.nightfish.lightnovelreader.api.web.explore.filter.Filter
import io.nightfish.lightnovelreader.api.web.explore.filter.SingleChoiceFilter
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
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val BASE_URL = "https://www.huanmengacg.com/index.php/bookapi"
private const val PASSWORD = "huanmengapi"
private const val UA =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

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
    private val chapterListCache = mutableMapOf<String, List<String>>()

    override val isOffLineFlow: StateFlow<Boolean> = MutableStateFlow(false)

    // ================================================================
    // 探索页面
    // ================================================================

    override val explorePageProvider: ExplorePageProvider =
        object : ExplorePageProvider.DefaultExplorePageProvider {

            // 一级 tab 只保留 3 个，避免 PrimaryTabRow 显示问题
            override val explorePageIdList: List<String> = listOf(
                "latest",
                "ongoing",
                "completed"
            )

            override val exploreTapPageDataSourceMap: Map<String, ExploreTapPageDataSource> = mapOf(
                "latest"    to buildTapPage("\uD83C\uDFA1 最新", null, null),
                "ongoing"   to buildTapPage("连载", "state", "1"),
                "completed" to buildTapPage("完结", "state", "2")
            )

            // 展开页：分类作为二级筛选
            override val exploreExpandedPageDataSourceMap: Map<String, ExploreExpandedPageDataSource> = mapOf(
                "latest_exp"    to buildExpandedPage("最新更新", null, null),
                "ongoing_exp"   to buildExpandedPage("连载中", "state", "1"),
                "completed_exp" to buildExpandedPage("已完结", "state", "2"),
                // 分类展开页，带标签筛选
                "category_all"  to buildCategoryExpandedPage()
            )
        }

    // ================================================================
    // 搜索
    // ================================================================

    override val searchProvider: SearchProvider = object : SearchProvider {

        override val searchTypes: List<SearchType> = listOf(
            SearchType("keyword", LocalString("关键词"), LocalString("输入关键词搜索"))
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
            val resp = CxHttp.get("$BASE_URL/detail") {
                param("password", PASSWORD)
                param("id", id)
                header("User-Agent", UA)
            }.await()
            val body = resp.body ?: return BookInformation.empty(id)
            val data = json.decodeFromString<HuanmengResponse<HuanmengBookDetail>>(body.string())
            data.data?.toBookInformation() ?: BookInformation.empty(id)
        } catch (e: Exception) {
            BookInformation.empty(id)
        }
    }

    // ================================================================
    // 章节目录（按卷分组）
    // ================================================================

    override suspend fun getBookVolumes(id: String): BookVolumes {
        return try {
            val resp = CxHttp.get("$BASE_URL/chapters") {
                param("password", PASSWORD)
                param("id", id)
                param("size", "5000")
                header("User-Agent", UA)
            }.await()
            val body = resp.body ?: return BookVolumes.empty(id)
            val data = json.decodeFromString<HuanmengResponse<HuanmengChapterList>>(body.string())

            val chapters = data.data?.list ?: return BookVolumes.empty(id)

            // 按 volumeId 分组
            val groupMap: MutableMap<Int, Pair<String, MutableList<ChapterInformation>>> = linkedMapOf()
            for (ch in chapters) {
                val vId = ch.volumeId
                val vName = ch.volumeName.ifBlank { "正文" }
                groupMap.getOrPut(vId) { Pair(vName.decodeHtmlEntities(), mutableListOf()) }
                    .second.add(
                        ChapterInformation(
                            id = "${ch.bid}_${ch.id}",
                            title = ch.name.decodeHtmlEntities()
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

            val resp = CxHttp.get("$BASE_URL/content") {
                param("password", PASSWORD)
                param("bid", bid)
                param("cid", cid)
                header("User-Agent", UA)
            }.await()
            val body = resp.body ?: return ChapterContent.empty(chapterId)
            val data = json.decodeFromString<HuanmengResponse<HuanmengContentData>>(body.string())

            val raw = data.data?.content ?: return ChapterContent.empty(chapterId)

            val builder = ContentBuilder()
            parseHtmlContent(raw, builder)

            // 获取章节列表以设置 lastChapter/nextChapter，启用上一章/下一章导航
            val allChapterIds = getChapterIdList(bookId)
            val currentIndex = allChapterIds.indexOf(chapterId)
            val prevId = if (currentIndex > 0) allChapterIds[currentIndex - 1] else ""
            val nextId = if (currentIndex >= 0 && currentIndex < allChapterIds.size - 1) allChapterIds[currentIndex + 1] else ""

            val mutable = ChapterContent.empty(chapterId).toMutable()
            mutable.id = chapterId
            mutable.content = builder.build()
            mutable.lastChapter = prevId
            mutable.nextChapter = nextId
            mutable
        } catch (e: Exception) {
            ChapterContent.empty(chapterId)
        }
    }

    /**
     * 获取书籍的所有章节 ID 列表（带缓存）
     */
    private suspend fun getChapterIdList(bookId: String): List<String> {
        chapterListCache[bookId]?.let { return it }
        return try {
            val resp = CxHttp.get("$BASE_URL/chapters") {
                param("password", PASSWORD)
                param("id", bookId)
                param("size", "5000")
                header("User-Agent", UA)
            }.await()
            val body = resp.body ?: return emptyList()
            val data = json.decodeFromString<HuanmengResponse<HuanmengChapterList>>(body.string())
            val ids = (data.data?.list ?: emptyList()).map { "${it.bid}_${it.id}" }
            chapterListCache[bookId] = ids
            ids
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ================================================================
    // 私有工具方法
    // ================================================================

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

        if (builder.components.isEmpty()) {
            builder.simpleText(html.cleanHtml().trim())
        }
    }

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

    /**
     * 构建分类展开页（带标签筛选）
     */
    private fun buildCategoryExpandedPage(): ExploreExpandedPageDataSource = object : ExploreExpandedPageDataSource {

        override val title: String = "分类"

        // 分类标签列表
        private val categories = listOf(
            Pair("校园", "1"),
            Pair("青春", "2"),
            Pair("恋爱", "3"),
            Pair("治愈", "4"),
            Pair("穿越", "16"),
            Pair("奇幻", "17"),
            Pair("悬疑", "26"),
            Pair("游戏", "34"),
            Pair("百合", "46")
        )

        private var selectedTag: String? = null
        private val _resultFlow = MutableStateFlow<SearchResult>(SearchResult.Empty())
        private var currentPage = 1
        private var loading = false

        override val filters: List<Filter<*>> = listOf(
            SingleChoiceFilter(
                title = LocalString("标签"),
                dialogTitle = LocalString("选择分类标签"),
                description = LocalString("选择要查看的分类标签"),
                choices = categories.map { it.first },
                defaultChoice = categories.first().first
            ).apply {
                addOnChangeListener { choice ->
                    selectedTag = categories.find { it.first == choice }?.second
                    // 重置并重新加载
                    currentPage = 1
                    _resultFlow.value = SearchResult.Empty()
                    loadMore()
                }
            }
        )

        override fun loadMore() {
            if (loading) return
            loading = true
            ioScope.launch {
                try {
                    val books = fetchBookList(
                        paramKey = if (selectedTag != null) "tags" else null,
                        paramValue = selectedTag,
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

    private suspend fun fetchBookList(
        key: String? = null,
        paramKey: String? = null,
        paramValue: String? = null,
        page: Int = 1,
        size: Int = 20
    ): HuanmengBookList? {
        val resp = CxHttp.get("$BASE_URL/search") {
            param("password", PASSWORD)
            param("page", page.toString())
            param("size", size.toString())
            if (!key.isNullOrBlank()) param("key", key)
            if (paramKey != null && paramValue != null) param(paramKey, paramValue)
            header("User-Agent", UA)
        }.await()
        val body = resp.body ?: return null
        val data = json.decodeFromString<HuanmengResponse<HuanmengBookList>>(body.string())
        return data.data
    }
}

// ================================================================
// 扩展函数 —— 模型转换
// ================================================================

private fun HuanmengBookItem.toBookInformation(): BookInformation {
    val tagList = if (kind.isNotBlank()) {
        kind.split(",", "，", " ").filter { it.isNotBlank() }
    } else emptyList()
    return HuanmengBookInformation(
        id = this@toBookInformation.id.toString(),
        title = name.decodeHtmlEntities(),
        subtitle = "",
        coverUri = Uri.parse(pic),
        author = this@toBookInformation.author.decodeHtmlEntities(),
        description = intro.decodeHtmlEntities(),
        tags = tagList.map { it.decodeHtmlEntities() },
        publishingHouse = "",
        wordCount = WordCount(textNum.parseWordCount()),
        lastUpdated = addtime.parseDateTime(),
        isComplete = false
    )
}

private fun HuanmengBookDetail.toBookInformation(): BookInformation {
    val detailTags = this.tags
    val allTags = buildList {
        if (kind.isNotBlank()) addAll(kind.split(",", "，", " ").filter { it.isNotBlank() })
        if (detailTags.isNotBlank()) addAll(detailTags.split(",", "，", " ").filter { it.isNotBlank() })
    }.distinct()
    return HuanmengBookInformation(
        id = this@toBookInformation.id.toString(),
        title = name.decodeHtmlEntities(),
        subtitle = "",
        coverUri = Uri.parse(pic),
        author = this@toBookInformation.author.decodeHtmlEntities(),
        description = intro.decodeHtmlEntities(),
        tags = allTags.map { it.decodeHtmlEntities() },
        publishingHouse = "",
        wordCount = WordCount(textNum.parseWordCount()),
        lastUpdated = addtime.parseDateTime(),
        isComplete = state.contains("完结")
    )
}

private fun HuanmengBookItem.toExploreDisplayBook(): ExploreDisplayBook =
    ExploreDisplayBook(
        id = this.id.toString(),
        title = this.name,
        author = this.author,
        coverUri = Uri.parse(this.pic)
    )

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun String.parseDateTime(): LocalDateTime {
    if (this.isBlank()) return LocalDateTime.MIN
    try {
        val trimmed = this.trim()
        // 尝试 LocalDateTime.parse（ISO 格式 "2024-01-15T10:30:00"）
        // 这是 LNR 宿主保留的方法，不会因 R8 被移除
        val isoString = trimmed.replace(' ', 'T')
        return try {
            LocalDateTime.parse(isoString)
        } catch (_: Exception) {
            // 只有日期的情况（"2024-01-15"），用 LocalDate.parse + atStartOfDay
            // 这也是 LNR 宿主 wenku8 插件使用的方式
            LocalDate.parse(trimmed.split(" ", "T").first(), dateFormatter).atStartOfDay()
        }
    } catch (_: Exception) {
        return LocalDateTime.MIN
    }
}

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

/**
 * 解码 HTML 实体编码（命名实体 + 数字实体）
 * 处理 &#40; &#41; &#x28; &amp; &lt; 等各种格式
 */
private fun String.decodeHtmlEntities(): String {
    var result = this
    // 解码十六进制数字实体 &#xHH;
    result = Regex("&#x([0-9a-fA-F]+);").replace(result) { match ->
        match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
    }
    // 解码十进制数字实体 &#DDD;
    result = Regex("&#(\\d+);").replace(result) { match ->
        match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
    }
    // 解码常见命名实体
    result = result
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
    return result
}

/**
 * 解析字数字符串（如 "122 万" 或 "5000"）为整数
 */
private fun String.parseWordCount(): Int {
    if (this.isBlank()) return 0
    val trimmed = this.trim()
    // 匹配数字部分
    val numMatch = Regex("""([\d.]+)""").find(trimmed)
    val num = numMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: return 0
    // 判断是否包含"万"字
    return if (trimmed.contains("万")) {
        (num * 10000).toInt()
    } else {
        num.toInt()
    }
}
