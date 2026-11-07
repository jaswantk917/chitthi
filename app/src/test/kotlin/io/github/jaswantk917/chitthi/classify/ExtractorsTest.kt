package io.github.jaswantk917.chitthi.classify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpTest {
    @Test fun `hash prefixed app verification`() =
        assertEquals("5070", Otp.extract("<#> 5070 is your JioHotstar verification code. Do not share it. abc123XYZ"))

    @Test fun `login code before the number`() =
        assertEquals("814946", Otp.extract("Bigbasket login code: 814946. Valid for 10 minutes. Do not share."))

    @Test fun `Hinglish otp`() =
        assertEquals("482913", Otp.extract("Aapka Seekho app ka login OTP 482913 hai. Kisi ke saath share na karein."))

    @Test fun `google style prefix keeps digits only`() =
        assertEquals("482913", Otp.extract("G-482913 is your Google verification code."))

    @Test fun `bank otp ignores amount and masked card`() =
        assertEquals("731904", Otp.extract("731904 is the OTP for txn of INR 2,499.00 at AMAZON on ICICI Bank card XX4321. Valid till 12:41. Do not share."))

    @Test fun `split code`() =
        assertEquals("123456", Otp.extract("Your WhatsApp code: 123-456. Don't share this code with others"))

    @Test fun `hindi otp`() =
        assertEquals("9921", Otp.extract("आपका ओटीपी 9921 है।"))

    @Test fun `transaction without code has none`() =
        assertNull(Otp.extract("Rs.500.00 debited from A/c XX1234 on 06-Nov-26 to VPA abc@ybl. Not you? Call 18002586161"))

    @Test fun `data pack expiry has none`() =
        assertNull(Otp.extract("डेटा पैक 06-Nov-26 01:04 Hrs को समाप्त हो जाएगा। रिचार्ज करें jio.com/r"))

    @Test fun `promo with code word but no number near`() =
        assertNull(Otp.extract("Use code FLAT50 to get 50% off on your first order! Shop now at myntra.com"))
}

class MoneyTest {
    @Test fun debit() {
        val money = Money.parse("Rs.500.00 debited from A/c XX1234 on 06-Nov-26 to VPA abc@ybl")!!
        assertEquals("500.00", money.value)
        assertEquals(true, money.debit)
    }

    @Test fun credit() {
        val money = Money.parse("INR 12,000 credited to your A/c XX1234 by NEFT from ACME CORP")!!
        assertEquals("12,000", money.value)
        assertEquals(false, money.debit)
    }

    @Test fun `debit then credited to payee is a debit`() =
        assertEquals(true, Money.parse("₹250 debited from a/c XX12 and credited to merchant@okaxis")!!.debit)

    @Test fun `autopay notice is not money moved`() =
        assertNull(Money.parse("Dear Customer, your AutoPay mandate of Rs 199 will be debited on 10-Nov"))
}

class LinksTest {
    @Test fun `brand domain from link`() =
        assertEquals(listOf("bigbasket.com"), Links.domains("Track it at https://www.bigbasket.com/orders/123"))

    @Test fun `second level india domains keep three labels`() =
        assertEquals("dominos.co.in", Links.registrable("m.dominos.co.in"))

    @Test fun `shorteners are not brands`() =
        assertEquals(emptyList<String>(), Links.domains("Click bit.ly/abc or wa.me/919876543210"))
}

class SenderIdTest {
    @Test fun `dlt header with route`() {
        val id = SenderId.parse("AX-ICICIB-S") as SenderId.Business
        assertEquals("ICICIB", id.header)
        assertEquals('S', id.route)
    }

    @Test fun `old style header`() {
        val id = SenderId.parse("VM-HDFCBK") as SenderId.Business
        assertEquals("HDFCBK", id.header)
        assertNull(id.route)
    }

    @Test fun `bare header`() = assertEquals("JIOINF", (SenderId.parse("JIOINF") as SenderId.Business).header)

    @Test fun `phone numbers merge on last ten digits`() {
        assertEquals(SenderId.parse("+91 98765 43210").key, SenderId.parse("09876543210").key)
    }

    @Test fun `short code`() = assertEquals("c:57575022", SenderId.parse("57575022").key)
}
