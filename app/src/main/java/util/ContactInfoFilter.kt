package com.dentalmarket.app.util

sealed class ContactFilterResult {
    object Clean : ContactFilterResult()
    object Blocked : ContactFilterResult()
}

// Blocks buyer questions and seller/admin answers that try to move the deal
// off-platform (phone numbers, emails, or phrases inviting contact outside
// the app). Enforced again in firestore.rules — this client-side pass exists
// so the sender gets instant feedback instead of a round-trip rejection, and
// it's the only place that can chase looser evasions (spaced-out digits,
// spelled-out numbers) since those aren't practical to express in Firestore's
// RE2-based rules.
object ContactInfoFilter {

    const val BLOCKED_MESSAGE =
        "For your safety, contact details can't be shared in chat — please keep communication and payment inside DentalMarket."

    private const val ARABIC_INDIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"
    private const val EXTENDED_ARABIC_INDIC_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    private fun normalizeDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val arabicIndex = ARABIC_INDIC_DIGITS.indexOf(ch)
            val extendedIndex = EXTENDED_ARABIC_INDIC_DIGITS.indexOf(ch)
            when {
                arabicIndex >= 0 -> sb.append('0' + arabicIndex)
                extendedIndex >= 0 -> sb.append('0' + extendedIndex)
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    // Collapses common Arabic letter-spelling variants (alef forms, ta
    // marbuta, alef maksura) so keyword/digit-word matching isn't defeated
    // by ordinary spelling variation.
    private fun normalizeArabicLetters(text: String): String {
        return text
            .replace('أ', 'ا') // أ -> ا
            .replace('إ', 'ا') // إ -> ا
            .replace('آ', 'ا') // آ -> ا
            .replace('ة', 'ه') // ة -> ه
            .replace('ى', 'ي') // ى -> ي
    }

    // Local Iraqi mobile: 07 + 9 digits = 11 digits total (e.g. 07701234567).
    private val localPhoneRegex = Regex("07\\d{9}")

    // International: country code 964 + 7 + 9 digits = 13 digits total,
    // with an optional +/00 already stripped by the caller.
    private val intlPhoneRegex = Regex("964\\d{10}")

    // A run of 10-15 digit characters allowing up to 2 separator characters
    // between each digit, so spaced/dashed/dotted numbers still extract as
    // one contiguous digit string before the phone-number check runs.
    private val looseDigitRunRegex = Regex("[0-9](?:[\\s.,\\-]{0,2}[0-9]){9,14}")

    private val emailRegex = Regex("[\\w.+-]+@[\\w-]+\\.[A-Za-z]{2,}")

    // Spelled-out "name at gmail dot com" style emails, dodging the @ and .
    private val spelledOutEmailRegex = Regex(
        "[a-zA-Z0-9._%+-]+\\s*(?:\\[at\\]|\\(at\\)|\\bat\\b)\\s*[a-zA-Z0-9-]+" +
            "(?:\\s*(?:\\[dot\\]|\\(dot\\)|\\bdot\\b)\\s*[a-zA-Z0-9-]+)+",
        RegexOption.IGNORE_CASE
    )

    private val keywordPhrases = listOf(
        // English
        "call me", "text me", "my number", "phone number", "whatsapp", "whats app",
        "contact me", "reach me", "message me on", "dm me", "viber", "telegram", "imo",
        // Iraqi Arabic (already in normalized-letter form; see normalizeArabicLetters)
        "رقمي", // رقمي
        "رقم هاتفي", // رقم هاتفي
        "اتصل بي", // اتصل بي
        "اتصلي بي", // اتصلي بي
        "كلمني", // كلمني
        "خابرني", // خابرني
        "اطلبني", // اطلبني
        "رنلي", // رنلي
        "رن لي", // رن لي
        "راسلني", // راسلني
        "تواصل معي", // تواصل معي
        "واتساب", // واتساب
        "واتس اب", // واتس اب
        "واتسب", // واتسب
        "تليجرام", // تليجرام
        "تلغرام", // تلغرام
        "فايبر" // فايبر
    )

    private val englishDigitWords = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9"
    )

    // Written with real Arabic spelling for readability; keys get
    // letter-normalized below so they match normalized input at lookup time.
    private val arabicDigitWordsRaw = mapOf(
        "صفر" to "0", // صفر
        "واحد" to "1", // واحد
        "اثنين" to "2", // اثنين
        "ثلاثة" to "3", // ثلاثة
        "اربعة" to "4", // اربعة
        "خمسة" to "5", // خمسة
        "ستة" to "6", // ستة
        "سبعة" to "7", // سبعة
        "ثمانية" to "8", // ثمانية
        "تسعة" to "9" // تسعة
    )

    private val digitWordMap: Map<String, String> by lazy {
        englishDigitWords + arabicDigitWordsRaw.mapKeys { normalizeArabicLetters(it.key) }
    }

    private fun extractSpelledOutDigitRuns(normalizedText: String): List<String> {
        val words = normalizedText.split(Regex("[\\s,]+"))
        val runs = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val digit = digitWordMap[word.lowercase()]
            if (digit != null) {
                current.append(digit)
            } else {
                if (current.length >= 7) runs.add(current.toString())
                current = StringBuilder()
            }
        }
        if (current.length >= 7) runs.add(current.toString())
        return runs
    }

    private fun containsIraqiPhoneNumber(digits: String): Boolean {
        return localPhoneRegex.containsMatchIn(digits) || intlPhoneRegex.containsMatchIn(digits)
    }

    fun scan(text: String): ContactFilterResult {
        if (text.isBlank()) return ContactFilterResult.Clean

        val digitNormalized = normalizeDigits(text)
        val letterNormalized = normalizeArabicLetters(digitNormalized).lowercase()

        if (emailRegex.containsMatchIn(digitNormalized)) return ContactFilterResult.Blocked
        if (spelledOutEmailRegex.containsMatchIn(digitNormalized)) return ContactFilterResult.Blocked

        for (match in looseDigitRunRegex.findAll(digitNormalized)) {
            val stripped = match.value.filter { it.isDigit() }
            if (containsIraqiPhoneNumber(stripped)) return ContactFilterResult.Blocked
        }

        for (run in extractSpelledOutDigitRuns(letterNormalized)) {
            if (containsIraqiPhoneNumber(run)) return ContactFilterResult.Blocked
        }

        for (phrase in keywordPhrases) {
            val normalizedPhrase = normalizeArabicLetters(phrase).lowercase()
            if (letterNormalized.contains(normalizedPhrase)) return ContactFilterResult.Blocked
        }

        return ContactFilterResult.Clean
    }
}
