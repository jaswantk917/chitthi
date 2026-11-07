package io.github.jaswantk917.chitthi.mms

import java.nio.charset.Charset

/**
 * Just enough of the MMS encapsulation (OMA-MMS-ENC, WSP-encoded) to read an m-notification-ind
 * and an m-retrieve-conf. Android keeps its own parser hidden, so the default SMS app brings one.
 */
object Pdu {
    const val TYPE_NOTIFICATION_IND = 0x82
    const val TYPE_RETRIEVE_CONF = 0x84

    data class Part(val contentType: String, val name: String?, val contentId: String?, val contentLocation: String?, val charset: Int?, val data: ByteArray) {
        val text: String? get() = if (contentType.startsWith("text/")) String(data, charsetOf(charset)) else null
    }

    data class Message(
        val type: Int = 0,
        val transactionId: String? = null,
        val contentLocation: String? = null,
        val from: String? = null,
        val subject: String? = null,
        /** seconds */
        val date: Long? = null,
        val messageId: String? = null,
        val contentType: String? = null,
        val parts: List<Part> = emptyList(),
    )

    fun parse(bytes: ByteArray): Message {
        val r = Reader(bytes)
        var m = Message()
        while (r.hasMore()) {
            val field = r.byte()
            if (field < 0x80) {
                // an application header: token name, then a text value
                r.back(); r.text(); r.text()
                continue
            }
            when (field) {
                0x8C -> m = m.copy(type = r.byte())
                0x98 -> m = m.copy(transactionId = r.text())
                0x83 -> m = m.copy(contentLocation = r.text())
                0x8B -> m = m.copy(messageId = r.text())
                0x96 -> m = m.copy(subject = r.encodedString())
                0x85 -> m = m.copy(date = r.longInt())
                0x89 -> {
                    val end = r.valueLength() + r.pos
                    if (r.byte() == 0x80) m = m.copy(from = r.encodedString().removeSuffix("/TYPE=PLMN"))
                    r.pos = end
                }
                0x84 -> {
                    m = m.copy(contentType = r.contentType().type)
                    if (m.contentType!!.contains("multipart")) m = m.copy(parts = r.multipart())
                    return m
                }
                else -> r.skipValue()
            }
        }
        return m
    }

    private data class ContentType(val type: String, val charset: Int?, val name: String?)

    private class Reader(private val data: ByteArray) {
        var pos = 0
        fun hasMore() = pos < data.size
        fun byte(): Int = data[pos++].toInt() and 0xFF
        fun peek(): Int = data[pos].toInt() and 0xFF
        fun back() { pos-- }

        fun uintvar(): Long {
            var value = 0L
            while (true) {
                val b = byte()
                value = (value shl 7) or (b and 0x7F).toLong()
                if (b and 0x80 == 0) return value
            }
        }

        fun valueLength(): Int {
            val b = byte()
            return if (b < 31) b else uintvar().toInt()
        }

        fun text(): String {
            if (peek() == 127 || peek() == 0x22) pos++
            val start = pos
            while (pos < data.size && data[pos] != 0.toByte()) pos++
            val s = String(data, start, pos - start, Charsets.UTF_8)
            pos++
            return s
        }

        fun longInt(): Long {
            val length = byte()
            var value = 0L
            repeat(length) { value = (value shl 8) or byte().toLong() }
            return value
        }

        fun integer(): Long = if (peek() >= 0x80) (byte() and 0x7F).toLong() else longInt()

        fun skipValue() {
            val b = peek()
            when {
                b < 31 -> pos += 1 + b
                b == 31 -> { pos++; val length = uintvar(); pos += length.toInt() }
                b < 128 -> text()
                else -> pos++
            }
        }

        fun encodedString(): String {
            if (peek() >= 32) return text()
            val end = valueLength() + pos
            val charset = integer().toInt()
            if (peek() == 127) pos++
            val start = pos
            while (pos < end && data[pos] != 0.toByte()) pos++
            val s = String(data, start, pos - start, charsetOf(charset))
            pos = end
            return s
        }

        fun contentType(): ContentType {
            val b = peek()
            if (b >= 0x80) { pos++; return ContentType(media(b and 0x7F), null, null) }
            if (b >= 32) return ContentType(text(), null, null)
            val end = valueLength() + pos
            val type = when {
                peek() >= 0x80 -> media(byte() and 0x7F)
                peek() < 31 -> media(longInt().toInt())
                else -> text()
            }
            var charset: Int? = null
            var name: String? = null
            while (pos < end) {
                when (val p = byte()) {
                    0x81 -> charset = integer().toInt()
                    0x85, 0x97, 0x86, 0x98 -> name = text()
                    else -> if (p >= 0x80) skipValue() else { back(); text(); skipValue() }
                }
            }
            pos = end
            return ContentType(type, charset, name)
        }

        fun multipart(): List<Part> {
            val count = uintvar().toInt()
            return List(count) {
                val headersLength = uintvar().toInt()
                val dataLength = uintvar().toInt()
                val headersEnd = pos + headersLength
                val type = contentType()
                var contentId: String? = null
                var location: String? = null
                while (pos < headersEnd) {
                    when (val h = byte()) {
                        0x8E -> location = text()
                        0xC0 -> contentId = text()
                        else -> if (h >= 0x80) skipValue() else { back(); text(); text() }
                    }
                }
                pos = headersEnd
                val body = data.copyOfRange(pos, pos + dataLength)
                pos += dataLength
                Part(type.type, type.name ?: location, contentId, location, type.charset, body)
            }
        }
    }

    fun charsetOf(mib: Int?): Charset = when (mib) {
        3 -> Charsets.US_ASCII
        4 -> Charsets.ISO_8859_1
        1000, 1015 -> Charsets.UTF_16
        else -> Charsets.UTF_8
    }

    private fun media(code: Int): String = MEDIA.getOrElse(code) { "application/octet-stream" }

    private val MEDIA = listOf(
        "*/*", "text/*", "text/html", "text/plain", "text/x-hdml", "text/x-ttml", "text/x-vCalendar",
        "text/x-vCard", "text/vnd.wap.wml", "text/vnd.wap.wmlscript", "text/vnd.wap.wta-event",
        "multipart/*", "multipart/mixed", "multipart/form-data", "multipart/byterantes",
        "multipart/alternative", "application/*", "application/java-vm",
        "application/x-www-form-urlencoded", "application/x-hdmlc", "application/vnd.wap.wmlc",
        "application/vnd.wap.wmlscriptc", "application/vnd.wap.wta-eventc",
        "application/vnd.wap.uaprof", "application/vnd.wap.wtls-ca-certificate",
        "application/vnd.wap.wtls-user-certificate", "application/x-x509-ca-cert",
        "application/x-x509-user-cert", "image/*", "image/gif", "image/jpeg", "image/tiff",
        "image/png", "image/vnd.wap.wbmp", "application/vnd.wap.multipart.*",
        "application/vnd.wap.multipart.mixed", "application/vnd.wap.multipart.form-data",
        "application/vnd.wap.multipart.byteranges", "application/vnd.wap.multipart.alternative",
        "application/xml", "text/xml", "application/vnd.wap.wbxml", "application/x-x968-cross-cert",
        "application/x-x968-ca-cert", "application/x-x968-user-cert", "text/vnd.wap.si",
        "application/vnd.wap.sic", "text/vnd.wap.sl", "application/vnd.wap.slc", "text/vnd.wap.co",
        "application/vnd.wap.coc", "application/vnd.wap.multipart.related",
        "application/vnd.wap.sia", "text/vnd.wap.connectivity-xml",
        "application/vnd.wap.connectivity-wbxml", "application/pkcs7-mime",
        "application/vnd.wap.hashed-certificate", "application/vnd.wap.signed-certificate",
        "application/vnd.wap.cert-response", "application/xhtml+xml", "application/wml+xml",
        "text/css", "application/vnd.wap.mms-message",
    )
}
