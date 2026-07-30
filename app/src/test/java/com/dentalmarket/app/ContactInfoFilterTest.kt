package com.dentalmarket.app

import com.dentalmarket.app.util.ContactFilterResult
import com.dentalmarket.app.util.ContactInfoFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactInfoFilterTest {

    private fun blocked(text: String) {
        assertEquals("expected BLOCKED for: $text", ContactFilterResult.Blocked, ContactInfoFilter.scan(text))
    }

    private fun clean(text: String) {
        assertEquals("expected CLEAN for: $text", ContactFilterResult.Clean, ContactInfoFilter.scan(text))
    }

    @Test
    fun `blocks example patterns from the plan`() {
        blocked("My number is 07701234567")
        blocked("Call me at +964 770 123 4567")
        blocked("0770-123-4567, text me")
        blocked("0 7 7 0 1 2 3 4 5 6 7")
        blocked("Let's talk on WhatsApp instead")
        blocked("zero seven seven zero one two three four five six seven")
        blocked("email me at seller123@gmail.com")
        blocked("رقمي ٠٧٧٠١٢٣٤٥٦٧")
        blocked("٠٧٧٠ ١٢٣ ٤٥٦٧")
        blocked("اتصل بي على هذا الرقم")
        blocked("تواصل معي بالواتساب")
        blocked("خابرني، رقمي 07901234567")
        blocked("صفر سبعة سبعة صفر واحد اثنين ثلاثة اربعة خمسة ستة سبعة")
    }

    @Test
    fun `blocks spelled-out emails`() {
        blocked("reach me at name at gmail dot com")
        blocked("name[at]yahoo[dot]com")
        blocked("name (at) hotmail (dot) com")
        blocked("name at mail dot yahoo dot com")
    }

    @Test
    fun `does not block ordinary messages`() {
        clean("Is this autoclave still available?")
        clean("What's the condition of the handpiece set?")
        clean("Can you do 250 instead of 300?")
        clean("هل الجهاز شغال زين؟")
        clean("شكرا، وصلني الطلب")
        clean("Let's meet at 5pm tomorrow")
        clean("I have 7 items left")
        clean("عندي سبعة اجهزة للبيع")
    }
}
