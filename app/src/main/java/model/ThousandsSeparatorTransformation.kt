package com.dentalmarket.app.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Groups a typed amount into thousands for display only — "10000" reads as
// "10,000" in the field while the state behind it stays "10000", so every
// toDoubleOrNull() and save path is untouched. Matches formatPrice()'s
// Locale.US grouping, so a price looks the same while being typed as it does
// once it's on a listing.
//
// Anything that isn't pure digits (a pasted decimal, a stray sign) falls
// through unformatted rather than guessing at where separators belong.
object ThousandsSeparatorTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty() || !digits.all { it.isDigit() }) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val grouped = buildString {
            digits.forEachIndexed { i, c ->
                if (i > 0 && (digits.length - i) % 3 == 0) append(',')
                append(c)
            }
        }

        // Both directions have to stay in range for every offset Compose can
        // hand us, or selection/cursor moves throw.
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, digits.length)
                // One separator for each group boundary that falls before the
                // cursor; a boundary at the cursor itself belongs after it.
                val separators = (1 until safe).count { (digits.length - it) % 3 == 0 }
                return safe + separators
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, grouped.length)
                return safe - grouped.take(safe).count { it == ',' }
            }
        }

        return TransformedText(AnnotatedString(grouped), offsetMapping)
    }
}
