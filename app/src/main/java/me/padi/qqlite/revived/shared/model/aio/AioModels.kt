package me.padi.qqlite.revived.shared.model.aio

import android.view.View
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import java.lang.ref.WeakReference

data class AioPeer(
    val peerId: String = "",
    val chatType: Int = 0,
    val chatNick: String? = null,
    val chatUin: Long = 0L
) {
    val displayName: String
        get() = chatNick?.takeIf { it.isNotBlank() } ?: peerId.ifBlank { "聊天" }

    fun stableKey(): String = "$chatType:$peerId:$chatUin"
}

data class AioUiState(
    val peer: AioPeer = AioPeer(),
    val peerAvatar: AvatarSpec? = null,
    val messages: List<AioMessage> = emptyList(),
    val draft: String = "",
    val loading: Boolean = true,
    val loadingOlder: Boolean = false,
    val emotionCategories: List<AioEmotionCategory> = emptyList(),
    val renderRevision: Long = 0L,
    val scrollToBottomRequest: Long = 0L
)

data class AioMessage(
    val key: String,
    val msgId: Long,
    val msgSeq: Long,
    val msgRandom: Long,
    val msgTime: Long,
    val senderUid: String,
    val senderUin: Long,
    val isSelf: Boolean,
    val kind: AioMessageKind,
    val text: String,
    val media: AioMediaSpec? = null,
    val avatar: AvatarSpec? = null,
    val itemViewRef: WeakReference<View>? = null
) {
    val sortTime: Long
        get() = when {
            msgTime > 0L -> msgTime
            msgSeq > 0L -> msgSeq
            else -> msgId
        }
}

enum class AioMessageKind {
    Text,
    Image,
    Video,
    Voice,
    File,
    Tip,
    Unsupported
}

data class AioMediaSpec(
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationSeconds: Int = 0,
    val fileName: String? = null,
    val fileSize: Long = 0L
) {
    val displayPath: String?
        get() = localPath?.takeIf { it.isNotBlank() } ?: remoteUrl?.takeIf { it.isNotBlank() }
}

data class AioEmotionCategory(
    val key: String,
    val title: String,
    val items: List<AioEmotionItem> = emptyList(),
    val loading: Boolean = false
)

data class AioEmotionItem(
    val key: String,
    val title: String,
    val kind: AioEmotionKind,
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val systemCode: Int = 0,
    val systemType: Int = 0,
    val resultPath: String? = null,
    val resultUrl: String? = null,
    val resultResId: String? = null,
    val resultMd5: String? = null,
    val marketEpId: String? = null,
    val marketEId: String? = null
) {
    val displayPath: String?
        get() = localPath?.takeIf { it.isNotBlank() } ?: remoteUrl?.takeIf { it.isNotBlank() }
}

enum class AioEmotionKind {
    System,
    Favorite,
    MarketFavorite,
    Hot
}

fun List<AioMessage>.sortedNewestFirst(): List<AioMessage> {
    return sortedWith(
        compareByDescending<AioMessage> { it.sortTime }
            .thenByDescending { it.msgSeq }
            .thenByDescending { it.msgId }
            .thenByDescending { it.msgRandom }
    )
}

fun List<AioMessage>.sortedOldestFirst(): List<AioMessage> {
    return sortedWith(
        compareBy<AioMessage> { it.sortTime }
            .thenBy { it.msgSeq }
            .thenBy { it.msgId }
            .thenBy { it.msgRandom }
    )
}
