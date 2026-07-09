package me.padi.qqlite.revived.hooks.common

import kotlin.math.min

internal object PhoneProfile {
    const val SHORT_WIDTH_DP = 450
    const val LONG_HEIGHT_DP = 800
    const val BASE_DENSITY_DPI = 160
}


internal fun minPositive(first: Int, second: Int): Int {
    return when {
        first > 0 && second > 0 -> min(first, second)
        first > 0 -> first
        second > 0 -> second
        else -> 0
    }
}
