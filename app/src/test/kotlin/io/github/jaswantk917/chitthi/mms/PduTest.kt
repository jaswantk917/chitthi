package io.github.jaswantk917.chitthi.mms

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class PduTest {
    private fun ByteArrayOutputStream.text(s: String) { write(s.toByteArray()); write(0) }
    private fun ByteArrayOutputStream.bytes(vararg b: Int) = b.forEach { write(it) }

    @Test fun `notification ind gives the download location`() {
        val pdu = ByteArrayOutputStream().apply {
            bytes(0x8C, 0x82) // m-notification-ind
            bytes(0x98); text("T123")
            bytes(0x8D, 0x92) // version 1.2
            bytes(0x89, 0x0F, 0x80); text("+919876543210")
            bytes(0x8A, 0x80) // personal
            bytes(0x8E, 0x02, 0x10, 0x00) // size, long-integer
            bytes(0x88, 0x05, 0x81, 0x03, 0x01, 0x51, 0x80) // expiry, relative
            bytes(0x83); text("http://mmsc.example/abc")
        }.toByteArray()
        val m = Pdu.parse(pdu)
        assertEquals(Pdu.TYPE_NOTIFICATION_IND, m.type)
        assertEquals("T123", m.transactionId)
        assertEquals("http://mmsc.example/abc", m.contentLocation)
        assertEquals("+919876543210", m.from)
    }

    @Test fun `retrieve conf with a text and an image part`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
        val text = "Picture from home".toByteArray()
        val pdu = ByteArrayOutputStream().apply {
            bytes(0x8C, 0x84) // m-retrieve-conf
            bytes(0x98); text("T9")
            bytes(0x8D, 0x92)
            bytes(0x89, 0x19, 0x80); text("+919876543210/TYPE=PLMN")
            bytes(0x85, 0x04, 0x69, 0x0A, 0x1B, 0x2C) // date
            bytes(0x96, 0x05, 0xEA); text("Hi!") // subject, utf-8
            bytes(0x84, 0xB3) // multipart/related, short form
            // body
            bytes(0x02)
            // part 1: text/plain; charset=utf-8 in the general form
            bytes(0x04, text.size)
            bytes(0x03, 0x83, 0x81, 0xEA)
            write(text)
            // part 2: image/jpeg, headers length 1
            bytes(0x01, jpeg.size)
            bytes(0x9E)
            write(jpeg)
        }.toByteArray()
        val m = Pdu.parse(pdu)
        assertEquals(Pdu.TYPE_RETRIEVE_CONF, m.type)
        assertEquals("+919876543210", m.from)
        assertEquals("Hi!", m.subject)
        assertEquals(2, m.parts.size)
        assertEquals("text/plain", m.parts[0].contentType)
        assertEquals("Picture from home", m.parts[0].text)
        assertEquals("image/jpeg", m.parts[1].contentType)
        assertArrayEquals(jpeg, m.parts[1].data)
    }
}
