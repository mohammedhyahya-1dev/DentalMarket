package com.dentalmarket.app.model

import java.util.Locale

// Single definition of how money reaches the screen. Every price, fee and
// total in the app formats through here, so a future currency or formatting
// change stays a one-file edit.
//
// Locale.US is deliberate rather than the device locale: on an Arabic-locale
// device the default would render Eastern Arabic numerals and a different
// group separator, so the same order would read differently depending on
// whose phone it's on.
//
// IQD has no practical subunit, so amounts are shown as whole numbers. This
// is display only — stored values, calculatePayout() and every fee/commission
// calculation still work in full precision Doubles.
fun formatPrice(amount: Double): String =
    String.format(Locale.US, "%,.0f IQD", amount)

// The whole-number value a given amount will *display* as. Totals that appear
// next to their own parts must be built by summing this over the parts, not by
// rounding the precise total — otherwise two lines showing "45 IQD" can sit
// above a total showing "89 IQD".
fun roundPrice(amount: Double): Double = Math.round(amount).toDouble()

// Fees and shipping charges are legitimately zero (free shipping, no safety
// fee), and "0 IQD" reads like a missing value. Item prices never use this —
// a zero-price listing isn't a real case.
fun formatPriceOrFree(amount: Double): String =
    if (roundPrice(amount) == 0.0) "Free" else formatPrice(amount)
