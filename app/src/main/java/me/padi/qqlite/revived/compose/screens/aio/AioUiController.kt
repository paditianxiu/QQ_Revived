package me.padi.qqlite.revived.compose.screens.aio

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.StateFlow
import me.padi.qqlite.revived.shared.model.aio.AioEmotionItem
import me.padi.qqlite.revived.shared.model.aio.AioMessage
import me.padi.qqlite.revived.shared.model.aio.AioUiState
import me.padi.qqlite.revived.shared.model.home.AvatarSpec

internal interface AioUiController {
    val uiState: StateFlow<AioUiState>

    fun navigateBack()
    fun openPeerPanel()
    fun updateDraft(value: String)
    fun sendDraft()
    fun openEmojiPanel()
    fun requestEmotionCategories()
    fun clickEmotion(item: AioEmotionItem): Boolean
    fun createEmotionPreviewView(context: Context, item: AioEmotionItem): View?
    fun reloadEmotionPreview(view: View, item: AioEmotionItem)
    fun loadOlderMessages()
    fun syncHostListPosition(messageKey: String?)
    fun clickMessage(message: AioMessage)
    fun clickAvatar(message: AioMessage)
    fun longClickMessage(message: AioMessage)
    fun createAvatarView(context: Context, spec: AvatarSpec?): View
    fun reloadAvatar(view: View, spec: AvatarSpec?)
    fun createHostMessagePreviewView(context: Context, message: AioMessage): View?
}
