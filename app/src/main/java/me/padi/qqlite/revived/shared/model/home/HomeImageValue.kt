package me.padi.qqlite.revived.shared.model.home

fun String.normalizedImageValue(): String? {
    val value = trim().takeIf { it.isNotBlank() } ?: return null
    val lower = value.lowercase()
    return when {
        lower.startsWith("http://") || lower.startsWith("https://") -> value
        lower.startsWith("file://") || lower.startsWith("content://") -> value
        lower.startsWith("//") -> "https:$value"
        value.startsWith("/") -> value
        value.contains(".") && !value.startsWith("data:", ignoreCase = true) -> "https://$value"
        else -> value
    }
}
