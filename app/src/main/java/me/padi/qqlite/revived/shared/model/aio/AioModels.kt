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
    val scrollToBottomRequest: Long = 0L,
    val longPressMenu: AioLongPressMenuState? = null,
    val firstVisibleMessageKey: String? = null,
    val firstVisibleMessageOffset: Int = 0
)

data class AioLongPressMenuState(
    val anchorX: Int,
    val anchorY: Int,
    val items: List<AioLongPressMenuItem>
)

data class AioLongPressMenuItem(
    val label: String
)

data class AioMessageBadge(
    val text: String,
    val colorArgb: Int? = null,
    val backgroundColorArgb: Int? = null
)

data class AioForwardPreview(
    val header: String,
    val items: List<String> = emptyList(),
    val count: Int = 0,
    val footer: String = "",
    val rawXml: String = ""
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
    val renderKind: AioMessageKind,
    val rawKind: AioMessageKind = AioMessageKind.Unsupported,
    val text: String,
    val senderName: String = "",
    val badges: List<AioMessageBadge> = emptyList(),
    val showTimeDivider: Boolean = false,
    val timeDividerText: String = "",
    val forwardPreview: AioForwardPreview? = null,
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

enum class AioMessageKind(val value: Int) {
    Text(-1),
    Image(-1),
    Call(19),
    Video(7),
    Voice(6),
    File(3),
    Tip(5),
    Unsupported(-1),
    Unknown(0),
    Null(1),
    Mix(2),
    Struct(4),
    MultiMsgForward(8),
    Reply(9),
    Wallet(10),
    ArkStruct(11),
    StructLongMsg(12),
    Giphy(13),
    Gift(14),
    TextGift(15),
    OnlineFile(21),
    FaceBubble(24),
    ShareLocation(25),
    OnlineFolder(27),
    Prologue(29);

    val displayName: String
        get() = when (this) {
            Text -> "文本"
            Image -> "图片"
            Call -> "通话"
            Video -> "视频"
            Voice -> "语音"
            File, OnlineFile, OnlineFolder -> "文件"
            Tip -> "系统提示"
            Unsupported -> "暂不支持"
            Unknown -> "未知消息"
            Null -> "空消息"
            Mix -> "混合消息"
            Struct -> "结构化消息"
            MultiMsgForward -> "合并转发"
            Reply -> "回复消息"
            Wallet -> "钱包消息"
            ArkStruct -> "卡片消息"
            StructLongMsg -> "长消息"
            Giphy -> "Giphy"
            Gift -> "礼物消息"
            TextGift -> "文字礼物"
            FaceBubble -> "头像气泡"
            ShareLocation -> "位置分享"
            Prologue -> "开场白"
        }

    companion object {
        fun fromMsgType(value: Int): AioMessageKind {
            return entries.firstOrNull { it.value == value } ?: Unsupported
        }

        fun fromElementType(value: Int): AioMessageKind {
            return when (value) {
                0 -> Unknown
                1 -> Null
                2 -> Mix
                3 -> File
                4 -> Voice
                5 -> Video
                7 -> Reply
                8 -> Tip
                9 -> Wallet
                10 -> ArkStruct
                13 -> StructLongMsg
                15 -> Giphy
                16 -> MultiMsgForward
                21 -> Call
                23 -> OnlineFile
                27 -> FaceBubble
                28 -> ShareLocation
                46 -> Prologue
                else -> Unsupported
            }
        }
    }
}

data class AioMediaSpec(
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val previewPath: String? = null,
    val playbackPath: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationSeconds: Int = 0,
    val fileName: String? = null,
    val fileSize: Long = 0L
) {
    val displayPath: String?
        get() = previewPath?.takeIf { it.isNotBlank() }
            ?: localPath?.takeIf { it.isNotBlank() }
            ?: remoteUrl?.takeIf { it.isNotBlank() }
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
