package me.padi.qqlite.revived.shared.model.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.padi.qqlite.revived.shared.model.ui.RevivedAdaptiveInfo
import me.padi.qqlite.revived.shared.model.ui.RevivedWidthSizeClass
import java.lang.ref.WeakReference

data class HomeProfile(
    val uid: String? = null, val nickName: String? = null, val avatarPath: String? = null
)

data class HomeUiState(
    val pages: List<HomePage> = emptyList(),
    val currentIndex: Int = 0,
    val profile: HomeProfile = HomeProfile(),
    val recentRows: List<RecentRow> = emptyList(),
    val contacts: List<ContactRow> = emptyList(),
    val qzoneFeeds: List<QZoneFeedRow> = emptyList(),
    val selfActions: List<SelfActionRow> = emptyList(),
    val recentScroll: ScrollSnapshot = ScrollSnapshot(),
    val contactScroll: ScrollSnapshot = ScrollSnapshot(),
    val qzoneScroll: ScrollSnapshot = ScrollSnapshot(),
    val selfScroll: ScrollSnapshot = ScrollSnapshot()
) {
    val visibleSelfActions: List<SelfActionRow>
        get() = selfActions.filter { it.title in VISIBLE_SELF_ACTION_TITLES }

    private companion object {
        val VISIBLE_SELF_ACTION_TITLES = setOf("我", "修改资料", "退出账号")
    }
}

data class ScrollSnapshot(
    val index: Int = 0, val offset: Int = 0
)

data class HomePage(
    val index: Int, val title: String, val className: String
)

data class HomeWindowInfo(
    val useNavigationRail: Boolean,
    val headerHeight: Dp,
    val horizontalPadding: Dp,
    val contentPadding: Dp,
    val maxContentWidth: Dp,
    val profileGridColumns: Int,
    val listItemSpacing: Dp,
    val pictureHeight: Dp,
    val pictureItemWidth: Dp
) {
    companion object {
        fun create(width: Dp, height: Dp): HomeWindowInfo {
            val adaptive = RevivedAdaptiveInfo.create(width, height)
            val isTabletOrLarge = adaptive.isTablet
            val isLandscape = adaptive.isLandscape
            return HomeWindowInfo(
                useNavigationRail = isTabletOrLarge,
                headerHeight = when {
                    isTabletOrLarge -> 68.dp
                    isLandscape -> 76.dp
                    else -> HOME_HEADER_HEIGHT_DP.dp
                },
                horizontalPadding = if (isTabletOrLarge) 22.dp else 14.dp,
                contentPadding = if (isTabletOrLarge) 16.dp else 10.dp,
                maxContentWidth = when (adaptive.widthClass) {
                    RevivedWidthSizeClass.Expanded -> 980.dp
                    RevivedWidthSizeClass.Medium -> 820.dp
                    RevivedWidthSizeClass.Compact -> 640.dp
                },
                profileGridColumns = when (adaptive.widthClass) {
                    RevivedWidthSizeClass.Expanded -> 4
                    RevivedWidthSizeClass.Medium -> 3
                    RevivedWidthSizeClass.Compact -> 2
                },
                listItemSpacing = if (isTabletOrLarge) 10.dp else 8.dp,
                pictureHeight = when {
                    adaptive.widthClass == RevivedWidthSizeClass.Expanded -> 240.dp
                    isTabletOrLarge -> 220.dp
                    isLandscape -> 150.dp
                    else -> 176.dp
                },
                pictureItemWidth = when {
                    adaptive.widthClass == RevivedWidthSizeClass.Expanded -> 240.dp
                    isTabletOrLarge -> 220.dp
                    isLandscape -> 176.dp
                    else -> 156.dp
                }
            )
        }
    }
}

data class RecentRow(
    val key: Long,
    val title: String,
    val summary: String,
    val time: String,
    val sortTime: Long,
    val orderHint: Int = Int.MAX_VALUE,
    val unread: Long,
    val pinned: Boolean,
    val avatar: AvatarSpec?,
    val rawItem: Any,
    val chatType: Int = avatar?.chatType ?: 0,
    val peerId: String = avatar?.uid.orEmpty(),
    val peerUin: Long = avatar?.uin ?: 0L
)

data class ContactRow(
    val key: String,
    val title: String,
    val type: String,
    val unread: Int,
    val hasExtIcon: Boolean,
    val avatar: AvatarSpec?,
    val rawItem: Any,
    val fragmentRef: WeakReference<Any>
)

data class QZoneFeedRow(
    val key: String,
    val title: String,
    val time: String,
    val summary: String,
    val avatar: AvatarSpec?,
    val pictures: List<PictureSpec>,
    val totalPictureCount: Int,
    val rawFeed: Any,
    val fragmentRef: WeakReference<Any>,
    val index: Int
)

data class AvatarSpec(
    val chatType: Int,
    val uid: String,
    val uin: Long,
    val fragmentRef: WeakReference<Any?>?,
    val imageUrl: String? = null
) {
    fun stableKey(): String {
        return imageUrl?.takeIf { it.isNotBlank() } ?: "$chatType:$uid:$uin"
    }
}

data class PictureSpec(
    val value: String, val isLocalFile: Boolean, val width: Int = 0, val height: Int = 0
)

data class PictureUrlSpec(
    val value: String, val width: Int, val height: Int
)

data class SelfActionRow(
    val title: String, val rawAction: Any
)

fun RecentRow.sameUi(other: RecentRow): Boolean {
    return key == other.key &&
            title == other.title &&
            summary == other.summary &&
            time == other.time &&
            sortTime == other.sortTime &&
            orderHint == other.orderHint &&
            unread == other.unread &&
            pinned == other.pinned &&
            avatar?.stableKey() == other.avatar?.stableKey()
}

fun ContactRow.sameUi(other: ContactRow): Boolean {
    return key == other.key && title == other.title && type == other.type && unread == other.unread && hasExtIcon == other.hasExtIcon && avatar?.stableKey() == other.avatar?.stableKey()
}

fun List<ContactRow>.sameContactUi(other: List<ContactRow>): Boolean {
    if (size != other.size) return false
    return indices.all { index -> this[index].sameUi(other[index]) }
}

fun List<RecentRow>.sortedForMessageHome(): List<RecentRow> {
    return sortedByDescending { it.sortTime }
}
