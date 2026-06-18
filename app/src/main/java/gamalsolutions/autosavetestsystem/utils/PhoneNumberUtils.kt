package gamalsolutions.autosavetestsystem.utils

object PhoneNumberUtils {

    /**
     * Normalizes a phone number by leaving only digits.
     * To match different formats (+967777777777, 967777777777, 0777777777, 777777777) etc.,
     * it extracts the last 9 digits if the length is at least 9, representing the significant local part.
     */
    fun normalize(phone: String?): String {
        if (phone == null) return ""
        
        // Remove all non-digit characters
        var digits = phone.filter { it.isDigit() }
        
        // If it starts with a country code, or leading zeros, we extract the local part
        // Many Arab countries have 9-digit mobile numbers (e.g., Yemen 77xxxxxxx, Saudi 5xxxxxxxx).
        // Egypt has 10 digits (e.g., 10xxxxxxxx). Let's take the last 9 digits for standardization.
        if (digits.length >= 9) {
            digits = digits.substring(digits.length - 9)
        }
        
        return digits
    }

    /**
     * Clean phone number for display (keeps '+' prefix and digits only)
     */
    fun cleanForDisplay(phone: String?): String {
        if (phone == null) return ""
        val hasPlus = phone.trim().startsWith("+")
        val digits = phone.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    /**
     * Check if two phone numbers are effectively the same
     */
    fun areEqual(phone1: String?, phone2: String?): Boolean {
        val norm1 = normalize(phone1)
        val norm2 = normalize(phone2)
        return norm1.isNotEmpty() && norm1 == norm2
    }
}
