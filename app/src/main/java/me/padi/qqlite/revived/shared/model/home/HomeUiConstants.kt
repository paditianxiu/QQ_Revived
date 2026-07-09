package me.padi.qqlite.revived.shared.model.home

const val HOME_HEADER_HEIGHT_DP = 112
const val HOME_RAIL_WIDTH_DP = 108
const val HOME_QZONE_LOAD_MORE_INTERVAL_MS = 1200L
const val HOME_QZONE_LOAD_MORE_TIMEOUT_MS = 10000L

fun Int.coerceInHome(pages: List<HomePage>): Int {
    if (pages.isEmpty()) return 0
    return coerceIn(0, pages.lastIndex)
}
