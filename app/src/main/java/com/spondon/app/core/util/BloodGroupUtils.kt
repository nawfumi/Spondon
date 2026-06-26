package com.spondon.app.core.util

/**
 * Normalizes a blood group string so that different representations of the
 * same group compare equal:
 *   "A−" (U+2212 MINUS SIGN)  ==  "A-" (U+002D HYPHEN-MINUS)
 *   "A+" (ASCII)              ==  "A＋" (fullwidth plus)
 *   leading / trailing spaces, non-breaking spaces, zero-width spaces
 */
object BloodGroupUtils {

    fun normalize(bg: String): String = bg
        .trim()
        .replace("\u00A0", "")   // non-breaking space
        .replace("\u200B", "")   // zero-width space
        .replace(" ", "")
        .replace("\uFF0B", "+")  // fullwidth plus  ＋
        .replace("\u2212", "-")  // minus sign      −  (U+2212)
        .replace("\u2013", "-")  // en dash         –
        .replace("\u2014", "-")  // em dash         —
        .uppercase()

    /**
     * Check if a donor blood group can donate to a recipient blood group.
     * Currently uses exact-match (normalized) — can be extended for
     * cross-type compatibility if needed.
     */
    fun canDonate(donorGroup: String, recipientGroup: String): Boolean {
        if (donorGroup.isBlank() || recipientGroup.isBlank()) return false
        return normalize(donorGroup) == normalize(recipientGroup)
    }
}
