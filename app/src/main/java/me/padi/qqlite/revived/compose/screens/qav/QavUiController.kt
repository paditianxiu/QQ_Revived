package me.padi.qqlite.revived.compose.screens.qav

import android.view.View
import kotlinx.coroutines.flow.StateFlow

internal data class QavUiState(
    val peerName: String = "通话中",
    val peerId: String = "",
    val statusText: String = "正在连接...",
    val isVideoCall: Boolean = false,
    val isConnected: Boolean = false,
    val micEnabled: Boolean = true,
    val cameraEnabled: Boolean = false
)

internal interface QavUiController {
    val uiState: StateFlow<QavUiState>

    fun toggleMic()
    fun toggleCamera()
    fun hangUp()
    fun obtainVideoHostView(): View?
    fun obtainAvatarHostView(): View?
}
