package me.padi.qqlite.revived.compose.screens.aio

import android.content.Context
import androidx.compose.ui.platform.ClipboardManager
import android.graphics.drawable.Drawable
import android.view.View
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import me.padi.qqlite.revived.shared.model.aio.AioEmotionItem
import me.padi.qqlite.revived.shared.model.aio.AioLongPressMenuItem
import me.padi.qqlite.revived.shared.model.aio.AioMessage
import me.padi.qqlite.revived.shared.model.aio.AioUiState
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import androidx.compose.ui.unit.IntOffset

internal interface AioUiController {
    val uiState: StateFlow<AioUiState>
    val pagingMessages: Flow<PagingData<AioMessage>>

    fun navigateBack()
    fun openPeerPanel()
    fun updateDraft(value: String)
    fun sendDraft()
    fun sendImage(path: String): Boolean
    fun pickImageFromAlbum()
    fun captureImage()
    fun startVoiceCall(): Boolean
    fun startVideoCall(): Boolean
    fun openEmojiPanel()
    fun sendCustomPbMessage(json: String): Boolean
    fun requestEmotionCategories()
    fun clickEmotion(item: AioEmotionItem): Boolean
    fun ensureEmotionPreview(item: AioEmotionItem)
    fun getEmotionPreviewDrawable(item: AioEmotionItem): Drawable?
    fun createEmotionPreviewView(context: Context, item: AioEmotionItem): View?
    fun reloadEmotionPreview(view: View, item: AioEmotionItem)
    fun loadOlderMessages()
    fun syncHostVisibleMessages(messageKeys: List<String>)
    fun updateScrollSnapshot(firstVisibleMessageKey: String?, firstVisibleMessageOffset: Int)
    fun copyMessageText(message: AioMessage, clipboardManager: ClipboardManager): Boolean
    fun clickMessage(message: AioMessage)
    fun doubleTapAvatar(message: AioMessage)
    fun longClickMessage(message: AioMessage)
    fun sendPai(toUin: String, peerUin: String, chatType: Int): Boolean
    fun rememberLongPressAnchor(anchor: IntOffset)
    fun showLongPressMenu(anchor: IntOffset, items: List<AioLongPressMenuItem>)
    fun dismissLongPressMenu()
    fun selectLongPressMenu(index: Int)
    fun createAvatarView(context: Context, spec: AvatarSpec?): View
    fun reloadAvatar(view: View, spec: AvatarSpec?)
    fun createHostMessagePreviewView(context: Context, message: AioMessage): View?
}
