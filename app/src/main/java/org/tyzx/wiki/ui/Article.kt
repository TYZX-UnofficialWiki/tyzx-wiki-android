package org.tyzx.wiki.ui

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** 一段带样式的文字；href 非空即可点击 */
data class Run(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val href: String? = null,
)

sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val runs: List<Run>) : Block
    data class Bullets(val ordered: Boolean, val items: List<List<Run>>) : Block
    data class Figure(val src: String, val caption: String?) : Block
    data class Quote(val runs: List<Run>) : Block
    data class TableView(val html: String) : Block
}

data class Article(
    val title: String,
    val blocks: List<Block>,
)

object Html {
    private val SKIP_TAGS = setOf("style", "script", "sup", "table")

    fun parse(title: String, html: String): Article {
        val doc = Jsoup.parse(html)
        val body = doc.body() ?: return Article(title, emptyList())
        val out = ArrayList<Block>()
        walk(body, out)
        return Article(title, out)
    }

    private fun walk(parent: Element, out: MutableList<Block>) {
        for (el in parent.children()) {
            when (el.tagName()) {
                "section", "div", "center" -> walk(el, out)
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val t = el.text().trim()
                    if (t.isNotEmpty()) out += Block.Heading(el.tagName()[1] - '0', t)
                }
                "p" -> {
                    val runs = inline(el)
                    if (runs.any { it.text.isNotBlank() }) out += Block.Paragraph(runs)
                }
                "ul", "ol" -> {
                    val items = el.children().filter { it.tagName() == "li" }.map { inline(it) }
                    if (items.isNotEmpty()) out += Block.Bullets(el.tagName() == "ol", items)
                }
                "figure" -> {
                    val img = el.selectFirst("img")
                    val cap = el.selectFirst("figcaption")?.text()?.trim()
                    val src = img?.absUrl("src").takeUnless { it.isNullOrBlank() } ?: img?.attr("src")
                    if (!src.isNullOrBlank()) out += Block.Figure(src, cap?.ifBlank { null })
                }
                "blockquote" -> out += Block.Quote(inline(el))
                "table" -> out += Block.TableView(el.outerHtml())
                "pre" -> out += Block.Quote(listOf(Run(el.text(), code = true)))
                "dl" -> walk(el, out)
                else -> if (el.children().isNotEmpty()) walk(el, out) else {
                    val runs = inline(el)
                    if (runs.any { it.text.isNotBlank() }) out += Block.Paragraph(runs)
                }
            }
        }
    }

    private fun inline(el: Element): List<Run> {
        val runs = ArrayList<Run>()
        append(el, runs, false, false, false, null)
        return merge(runs)
    }

    private fun append(
        node: Node,
        out: MutableList<Run>,
        bold: Boolean,
        italic: Boolean,
        code: Boolean,
        href: String?,
    ) {
        when (node) {
            is TextNode -> {
                val t = node.text().replace('\u00a0', ' ')
                if (t.isNotEmpty()) out += Run(t, bold, italic, code, href)
            }
            is Element -> {
                val tag = node.tagName()
                if (tag in SKIP_TAGS) return
                val b = bold || tag == "b" || tag == "strong"
                val i = italic || tag == "i" || tag == "em"
                val c = code || tag == "code" || tag == "tt"
                val h = if (tag == "a") resolveHref(node) ?: href else href
                if (tag == "br") {
                    out += Run("\n", b, i, c, h)
                    return
                }
                for (child in node.childNodes()) append(child, out, b, i, c, h)
            }
        }
    }

    private fun resolveHref(a: Element): String? {
        val href = a.attr("href").trim()
        if (href.isEmpty()) return null
        if (href.startsWith("./")) {
            val raw = href.removePrefix("./")
            if (raw.startsWith("File:") || raw.startsWith("Special:") || raw.startsWith("Category:")) return null
            val t = java.net.URLDecoder.decode(raw, "UTF-8").removePrefix("/wiki/")
            return "wiki:$t"
        }
        if (href.startsWith("#")) return null
        if (href.startsWith("http")) return "url:$href"
        return null
    }

    /** 合并相同样式的相邻文字，减少 span 数量 */
    private fun merge(runs: List<Run>): List<Run> {
        val out = ArrayList<Run>()
        for (r in runs) {
            val last = out.lastOrNull()
            if (last != null && last.bold == r.bold && last.italic == r.italic &&
                last.code == r.code && last.href == r.href
            ) {
                out[out.size - 1] = last.copy(text = last.text + r.text)
            } else out += r
        }
        return out.map { it.copy(text = it.text.replace(Regex("[ \\t]*\\n[ \\t]*"), "\n").trimStart()) }
            .filter { it.text.isNotEmpty() }
    }
}
