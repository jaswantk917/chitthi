package io.github.jaswantk917.chitthi.classify

/** An SMS "address" as Indian carriers deliver it: a phone number, a short code or a DLT header. */
sealed interface SenderId {
    val raw: String
    val key: String

    data class Phone(override val raw: String, val digits: String) : SenderId {
        /** the last 10 digits, so +91 98765 43210 and 09876543210 are one person */
        val national: String get() = digits.takeLast(10)
        override val key: String get() = "p:$national"
    }

    data class ShortCode(override val raw: String, val digits: String) : SenderId {
        override val key: String get() = "c:$digits"
    }

    /**
     * A DLT header like "AX-ICICIB-S": a 2-letter carrier/circle prefix, the 6-letter header and,
     * since TRAI's 2025 rule, a route suffix: S service, T transactional, P promotional, G government.
     */
    data class Business(override val raw: String, val header: String, val route: Char?) : SenderId {
        override val key: String get() = "h:$header"
    }

    /** A Google RCS business chat, e.g. "mvvwc…@bot.rcs.google.com"; its name isn't in the address. */
    data class RcsBot(override val raw: String) : SenderId {
        override val key: String get() = "r:" + raw.substringBefore('@').lowercase()
    }

    companion object {
        private val ROUTES = setOf('S', 'T', 'P', 'G')

        fun parse(address: String): SenderId {
            if (address.contains('@')) return RcsBot(address)
            val compact = address.filterNot { it.isWhitespace() || it == '(' || it == ')' }
            val digitsOnly = compact.removePrefix("+").filter { it != '-' }
            if (digitsOnly.isNotEmpty() && digitsOnly.all { it.isDigit() }) {
                return if (digitsOnly.length >= 10) Phone(address, digitsOnly) else ShortCode(address, digitsOnly)
            }
            val parts = compact.uppercase().split('-').filter { it.isNotEmpty() }
            return when {
                parts.size >= 2 && parts[0].length == 2 -> {
                    val route = parts.getOrNull(2)?.singleOrNull()?.takeIf { it in ROUTES }
                    Business(address, parts[1], route)
                }
                parts.size == 2 && parts[1].length == 1 && parts[1][0] in ROUTES ->
                    Business(address, parts[0], parts[1][0])
                else -> Business(address, parts.joinToString("-").ifEmpty { address }, null)
            }
        }
    }
}
