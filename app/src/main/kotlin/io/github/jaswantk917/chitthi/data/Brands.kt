package io.github.jaswantk917.chitthi.data

import org.json.JSONArray

data class Brand(val name: String, val domain: String)

/** DLT header → brand, from the bundled assets/brands.json. "ICICI*" entries match by prefix. */
class Brands(json: String) {
    private val exact = HashMap<String, Brand>()
    private val prefixes = ArrayList<Pair<String, Brand>>()
    private val domains = HashMap<String, Brand>()

    init {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            val brand = Brand(entry.getString("n"), entry.getString("d"))
            domains.putIfAbsent(brand.domain, brand)
            entry.optJSONArray("a")?.let { aliases ->
                for (j in 0 until aliases.length()) domains.putIfAbsent(aliases.getString(j), brand)
            }
            val headers = entry.getJSONArray("h")
            for (j in 0 until headers.length()) {
                val header = headers.getString(j).uppercase()
                if (header.endsWith('*')) prefixes += header.dropLast(1) to brand
                else exact.putIfAbsent(header, brand)
            }
        }
        prefixes.sortByDescending { it.first.length }
    }

    fun find(header: String): Brand? {
        val upper = header.uppercase()
        return exact[upper] ?: prefixes.firstOrNull { upper.startsWith(it.first) }?.second
    }

    /** The brand behind a linked domain, including its short-link domains like "fkrt.it". */
    fun byDomain(domain: String): Brand? = domains[domain]

    companion object {
        /** A shop name from its domain when nothing better is known: "bigbasket.com" → "Bigbasket". */
        fun nameFromDomain(domain: String): String =
            domain.substringBefore('.').replaceFirstChar { it.uppercase() }
    }
}
