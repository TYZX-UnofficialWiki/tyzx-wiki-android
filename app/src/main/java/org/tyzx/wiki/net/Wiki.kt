package org.tyzx.wiki.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 天一维基接口层。全部走匿名接口，登录态另加 cookie 即可复用。
 */
object Wiki {
    const val HOST = "https://zh-wiki.tyzx.org"
    const val API = "$HOST/api.php"
    const val REST = "$HOST/rest.php/v1"

    private const val UA = "TianyiWikiAndroid/0.1 (+https://github.com/TYZX-UnofficialWiki/tyzx-wiki-android)"

    suspend fun raw(url: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/json, text/html;q=0.9")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            if (code !in 200..299) error("HTTP $code: ${text.take(200)}")
            text
        } finally {
            conn.disconnect()
        }
    }

    suspend fun api(vararg params: Pair<String, String>): JSONObject {
        val q = buildString {
            append("?format=json&formatversion=2")
            params.forEach { (k, v) -> append('&').append(k).append('=').append(enc(v)) }
        }
        return JSONObject(raw(API + q))
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    data class Feed(
        val title: String,
        val extract: String?,
        val thumb: String?,
        val categories: List<String>,
    )

    /** 随机条目，带缩略图与导语 */
    suspend fun random(limit: Int = 16): List<Feed> {
        val j = api(
            "action" to "query",
            "generator" to "random",
            "grnnamespace" to "0",
            "grnlimit" to limit.toString(),
            "prop" to "pageimages|extracts|info",
            "piprop" to "thumbnail",
            "pithumbsize" to "600",
            "exintro" to "1",
            "explaintext" to "1",
            "exlimit" to "max",
            "inprop" to "url",
        )
        val pages = j.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        return (0 until pages.length()).mapNotNull { i ->
            val p = pages.optJSONObject(i) ?: return@mapNotNull null
            val title = p.optString("title")
            if (title.isBlank()) return@mapNotNull null
            Feed(
                title = title,
                extract = p.optString("extract").ifBlank { null },
                thumb = p.optJSONObject("thumbnail")?.optString("source"),
                categories = emptyList(),
            )
        }
    }

    /** 分类筛选 + 随机：用 incategory 搜索，拿不到图就退化成标题列表 */
    suspend fun byCategory(category: String, limit: Int = 16): List<Feed> {
        if (category.isBlank()) return random(limit)
        val j = api(
            "action" to "query",
            "generator" to "search",
            "gsrsearch" to "incategory:\"$category\"",
            "gsrnamespace" to "0",
            "gsrlimit" to limit.toString(),
            "prop" to "pageimages|extracts",
            "piprop" to "thumbnail",
            "pithumbsize" to "600",
            "exintro" to "1",
            "explaintext" to "1",
            "exlimit" to "max",
        )
        val pages = j.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        return (0 until pages.length()).mapNotNull { i ->
            val p = pages.optJSONObject(i) ?: return@mapNotNull null
            val title = p.optString("title")
            if (title.isBlank()) return@mapNotNull null
            Feed(
                title = title,
                extract = p.optString("extract").ifBlank { null },
                thumb = p.optJSONObject("thumbnail")?.optString("source"),
                categories = emptyList(),
            )
        }
    }

    data class Recent(val title: String, val timestamp: String, val comment: String?)

    suspend fun recent(limit: Int = 6): List<Recent> {
        val j = api(
            "action" to "query",
            "list" to "recentchanges",
            "rclimit" to limit.toString(),
            "rcnamespace" to "0",
            "rcprop" to "title|timestamp|comment",
            "rctype" to "edit|new",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("recentchanges") ?: return emptyList()
        val seen = HashSet<String>()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val t = o.optString("title")
            if (t.isBlank() || !seen.add(t)) return@mapNotNull null
            Recent(t, o.optString("timestamp"), o.optString("comment").ifBlank { null })
        }
    }

    data class Hit(val title: String, val snippet: String)

    suspend fun search(q: String, limit: Int = 20): List<Hit> {
        if (q.isBlank()) return emptyList()
        val j = api(
            "action" to "query",
            "list" to "search",
            "srsearch" to q,
            "srlimit" to limit.toString(),
            "srnamespace" to "0",
            "srprop" to "snippet",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Hit(o.optString("title"), stripTags(o.optString("snippet")))
        }
    }

    private fun stripTags(s: String): String =
        s.replace(Regex("<[^>]+>"), "").replace("&quot;", "\"").replace("&amp;", "&").trim()

    /** Parsoid 输出的完整 HTML */
    suspend fun pageHtml(title: String): String = raw("$REST/page/${enc(title)}/html")

    data class Cat(val name: String, val size: Int)

    suspend fun categoriesWithSize(): List<Cat> {
        val j = api(
            "action" to "query",
            "list" to "allcategories",
            "aclimit" to "100",
            "acprop" to "size",
            "acmin" to "1",
            "acdir" to "ascending",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("allcategories") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val n = o.optString("category")
            if (n.isBlank()) null else Cat(n, o.optInt("size"))
        }.sortedByDescending { it.size }
    }

    suspend fun categoryMembers(cat: String, limit: Int = 100): List<String> {
        val j = api(
            "action" to "query",
            "list" to "categorymembers",
            "cmtitle" to "Category:$cat",
            "cmlimit" to limit.toString(),
            "cmnamespace" to "0",
            "cmprop" to "title",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("categorymembers") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("title") }
            .filter { it.isNotBlank() }
    }

    suspend fun categories(): List<String> {
        val j = api(
            "action" to "query",
            "list" to "allcategories",
            "aclimit" to "60",
            "acprop" to "size",
            "acmin" to "1",
        )
        val arr = j.optJSONObject("query")?.optJSONArray("allcategories") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("category") }
            .filter { it.isNotBlank() }
    }
}
