package com.iurie.etfwatch.ui.common

import java.util.Locale

/**
 * Number formatting for the UI.
 *
 * Everything is formatted with [Locale.US] on purpose: these are market figures, and on a
 * comma-decimal device the default locale renders "12,34" while the alert dialog's parser only
 * accepts dots — so a value the app printed could not be typed back in. [parseDecimal] accepts
 * either separator so manual entry still works on any keyboard.
 */
object Format {

    const val EM_DASH = "—"

    fun price(value: Double?): String =
        value?.let { String.format(Locale.US, "%.2f", it) } ?: EM_DASH

    fun money(value: Double?): String =
        value?.let { String.format(Locale.US, "$%.2f", it) } ?: EM_DASH

    /** Percentage with an explicit sign, e.g. "+1.20%" / "-0.35%". */
    fun signedPct(value: Double?): String =
        value?.let { String.format(Locale.US, "%+.2f%%", it) } ?: EM_DASH

    /** Percentage without a forced sign, e.g. "9.84%". */
    fun pct(value: Double?): String =
        value?.let { String.format(Locale.US, "%.2f%%", it) } ?: EM_DASH

    fun parseDecimal(input: String): Double? =
        input.trim().replace(',', '.').toDoubleOrNull()
}
