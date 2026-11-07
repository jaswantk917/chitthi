package io.github.jaswantk917.chitthi.classify

object Otp {
    // words that put a nearby number in "this is a code" territory
    private val strong = Regex(
        "(?i)(otp|o\\.t\\.p|one[ -]?time|verification|verify|passcode|login code|security code|" +
            "auth(?:entication)? code|confirmation code|code is|code:|ओटीपी|सत्यापन)"
    )
    private val weak = Regex("(?i)\\b(code|pin|password|token)\\b")
    private val candidate = Regex("(?<![\\dXx*₹.,/:-])(?:[A-Z]-)?(\\d{4,8})(?![\\d,]|\\.\\d|%|/|:\\d)")
    private val split = Regex("(?<![\\d])(\\d{3})[ -](\\d{3})(?![\\d])")
    private val money = Regex("(?i)(rs\\.?|inr|₹|amt|amount)\\s*$")
    private const val REACH = 48

    fun extract(body: String): String? {
        val keywords = (strong.findAll(body) + weak.findAll(body)).map { it.range }.toList()
        if (keywords.isEmpty()) return null
        val isStrong = strong.containsMatchIn(body)
        data class Hit(val code: String, val at: IntRange)
        val hits = candidate.findAll(body)
            .filterNot { money.containsMatchIn(body.substring(maxOf(0, it.range.first - 6), it.range.first)) }
            .map { Hit(it.groupValues[1], it.range) }
            .toMutableList()
        if (hits.isEmpty()) {
            split.findAll(body).forEach { hits += Hit(it.groupValues[1] + it.groupValues[2], it.range) }
        }
        return hits
            .map { hit -> hit to keywords.minOf { distance(it, hit.at) } }
            .filter { (_, d) -> d <= if (isStrong) REACH * 2 else REACH }
            .minByOrNull { it.second }
            ?.first?.code
    }

    private fun distance(a: IntRange, b: IntRange): Int = when {
        a.last < b.first -> b.first - a.last
        b.last < a.first -> a.first - b.last
        else -> 0
    }
}

object Money {
    data class Amount(val value: String, val debit: Boolean)

    private val amount = Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.\\d{1,2})?)")
    private val debitWords = Regex(
        "(?i)\\b(debited|spent|paid|withdrawn|deducted|charged|purchase|sent|transferred|txn of|" +
            "used (?:for|at)|transaction of|dr\\b)"
    )
    private val creditWords = Regex("(?i)\\b(credited|received|deposited|refund(?:ed)?|cr\\b|added to)")
    // reminders about money that hasn't moved yet are bills, not transactions
    private val future = Regex("(?i)(will be (?:debited|deducted|charged)|is due|due on|due date|pay by|requested)")

    fun parse(body: String): Amount? {
        if (future.containsMatchIn(body)) return null
        val value = amount.find(body)?.groupValues?.get(1)?.trimEnd('.') ?: return null
        val debitAt = debitWords.find(body)?.range?.first
        val creditAt = creditWords.find(body)?.range?.first
        val debit = when {
            debitAt != null && creditAt != null -> debitAt < creditAt
            debitAt != null -> true
            creditAt != null -> false
            else -> return null
        }
        return Amount(value, debit)
    }
}

object Links {
    private val url = Regex("(?i)\\b(?:https?://)?((?:[a-z0-9-]+\\.)+[a-z]{2,})(?:/[^\\s]*)?")
    private val generic = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "rb.gy", "cutt.ly", "is.gd", "shorturl.at", "s.id",
        "wa.me", "whatsapp.com", "play.google.com", "apps.apple.com", "youtube.com", "youtu.be",
        "facebook.com", "instagram.com", "twitter.com", "x.com", "forms.gle", "google.com", "g.co",
        "linktr.ee", "1kx.in", "msg.ly", "i.ivr.in", "sms.ly", "web.app",
    )
    private val secondLevel = setOf("co", "com", "net", "org", "gov", "ac", "edu", "nic", "res", "gen", "firm", "ind")

    /** The brand-ish domains a message links to, e.g. "bigbasket.com" for a bigbasket.com/... link. */
    fun domains(body: String): List<String> = url.findAll(body)
        .map { registrable(it.groupValues[1].lowercase()) }
        .filter { it !in generic && it.contains('.') && !it.first().isDigit() }
        .toList()

    fun registrable(host: String): String {
        val labels = host.removePrefix("www.").split('.')
        if (labels.size <= 2) return labels.joinToString(".")
        val keep = if (labels[labels.size - 2] in secondLevel && labels.last().length == 2) 3 else 2
        return labels.takeLast(keep).joinToString(".")
    }
}
