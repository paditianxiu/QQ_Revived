package me.padi.qqlite.revived.compose.screens.qav

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import me.padi.qqlite.revived.compose.theme.RevivedTheme
import me.padi.qqlite.revived.shared.model.ui.QavWindowInfo
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun QavCallScreen(controller: QavUiController) {
    val uiState by controller.uiState.collectAsState()
    val hostAvatarView = controller.obtainAvatarHostView()
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val windowInfo = remember(containerSize, density) {
        with(density) {
            QavWindowInfo.create(
                width = containerSize.width.toDp(),
                height = containerSize.height.toDp()
            )
        }
    }

    RevivedTheme {
        if (uiState.isVideoCall) {
            VideoCallScreen(
                uiState = uiState,
                avatarView = hostAvatarView,
                videoView = controller.obtainVideoHostView(),
                windowInfo = windowInfo,
                onToggleMic = controller::toggleMic,
                onToggleCamera = controller::toggleCamera,
                onHangUp = controller::hangUp
            )
        } else {
            VoiceCallScreen(
                uiState = uiState,
                avatarView = hostAvatarView,
                windowInfo = windowInfo,
                onToggleMic = controller::toggleMic,
                onHangUp = controller::hangUp
            )
        }
    }
}

@Composable
private fun VideoCallScreen(
    uiState: QavUiState,
    avatarView: View?,
    videoView: View?,
    windowInfo: QavWindowInfo,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onHangUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF081018))
    ) {
        if (videoView != null) {
            HostView(
                view = videoView,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66000000),
                            Color.Transparent,
                            Color(0xCC05080D)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    horizontal = windowInfo.horizontalPadding,
                    vertical = windowInfo.verticalPadding
                )
        ) {
            Spacer(modifier = Modifier.weight(1f))
            VideoIdentityBlock(
                uiState = uiState,
                avatarView = avatarView,
                windowInfo = windowInfo
            )
            Text(
                text = uiState.statusText,
                color = Color(0xE6FFFFFF),
                fontSize = windowInfo.statusTextSize.value.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 18.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            VideoControlBar(
                uiState = uiState,
                windowInfo = windowInfo,
                onToggleMic = onToggleMic,
                onToggleCamera = onToggleCamera,
                onHangUp = onHangUp
            )
        }
    }
}

@Composable
private fun VoiceCallScreen(
    uiState: QavUiState,
    avatarView: View?,
    windowInfo: QavWindowInfo,
    onToggleMic: () -> Unit,
    onHangUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF17314B),
                        Color(0xFF0E1C2A),
                        Color(0xFF05080D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    horizontal = windowInfo.horizontalPadding,
                    vertical = windowInfo.verticalPadding
                )
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HostAvatar(
                    avatarView = avatarView,
                    fallback = uiState.peerName,
                    modifier = Modifier.size(windowInfo.voiceAvatarSize)
                )
                Text(
                    text = uiState.peerName,
                    color = Color.White,
                    fontSize = windowInfo.peerNameSize.value.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 18.dp)
                )
                if (uiState.peerId.isNotBlank()) {
                    Text(
                        text = uiState.peerId,
                        color = Color(0xB3FFFFFF),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Text(
                    text = uiState.statusText,
                    color = Color(0xE6FFFFFF),
                    fontSize = (windowInfo.statusTextSize.value + 1f).sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            VoiceControlBar(
                micEnabled = uiState.micEnabled,
                windowInfo = windowInfo,
                onToggleMic = onToggleMic,
                onHangUp = onHangUp
            )
        }
    }
}

@Composable
private fun VideoIdentityBlock(
    uiState: QavUiState,
    avatarView: View?,
    windowInfo: QavWindowInfo,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HostAvatar(
            avatarView = avatarView,
            fallback = uiState.peerName,
            modifier = Modifier.size(windowInfo.videoIdentityAvatarSize)
        )
        Column(
            modifier = Modifier.padding(start = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = uiState.peerName,
                color = Color.White,
                fontSize = (windowInfo.peerNameSize.value - 6f).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (uiState.peerId.isNotBlank()) {
                Text(
                    text = uiState.peerId,
                    color = Color(0xD9FFFFFF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VideoControlBar(
    uiState: QavUiState,
    windowInfo: QavWindowInfo,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onHangUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = if (uiState.micEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
            label = if (uiState.micEnabled) "麦克风已开" else "麦克风已关",
            background = if (uiState.micEnabled) Color(0x26FFFFFF) else Color(0xCC334155),
            windowInfo = windowInfo,
            onClick = onToggleMic
        )
        ControlButton(
            icon = if (uiState.cameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
            label = if (uiState.cameraEnabled) "摄像头已开" else "摄像头已关",
            background = if (uiState.cameraEnabled) Color(0x26FFFFFF) else Color(0xCC334155),
            windowInfo = windowInfo,
            onClick = onToggleCamera
        )
        ControlButton(
            icon = Icons.Filled.CallEnd,
            label = "挂断",
            background = Color(0xFFE5484D),
            windowInfo = windowInfo,
            onClick = onHangUp
        )
    }
}

@Composable
private fun VoiceControlBar(
    micEnabled: Boolean,
    windowInfo: QavWindowInfo,
    onToggleMic: () -> Unit,
    onHangUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            icon = if (micEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
            label = if (micEnabled) "麦克风已开" else "麦克风已关",
            background = if (micEnabled) Color(0x26FFFFFF) else Color(0xCC334155),
            windowInfo = windowInfo,
            onClick = onToggleMic
        )
        ControlButton(
            icon = Icons.Filled.CallEnd,
            label = "挂断",
            background = Color(0xFFE5484D),
            windowInfo = windowInfo,
            onClick = onHangUp
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    background: Color,
    windowInfo: QavWindowInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = windowInfo.controlSpacing / 2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            minWidth = windowInfo.controlButtonSize,
            minHeight = windowInfo.controlButtonSize,
            cornerRadius = windowInfo.controlButtonSize / 2,
            backgroundColor = background
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(windowInfo.controlIconSize)
            )
        }
        Text(
            text = label,
            color = Color(0xCCFFFFFF),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = windowInfo.controlLabelTopPadding)
        )
    }
}

@Composable
private fun HostAvatar(
    avatarView: View?,
    fallback: String,
    modifier: Modifier = Modifier
) {
    if (avatarView != null) {
        HostView(
            view = avatarView,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFF1F2937)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallback.ifBlank { "Q" }.take(1),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HostView(
    view: View,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                clipChildren = false
                clipToPadding = false
            }
        },
        modifier = modifier,
        update = { container ->
            view.detachFromParent()
            if (container.childCount > 0) {
                container.removeAllViews()
            }
            container.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    )
}

private fun View.detachFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}
