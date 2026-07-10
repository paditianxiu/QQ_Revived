package me.padi.qqlite.revived.compose.screens.aio

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.withClip
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import me.padi.qqlite.revived.shared.model.aio.AioEmotionCategory
import me.padi.qqlite.revived.shared.model.aio.AioEmotionItem
import me.padi.qqlite.revived.shared.model.aio.AioEmotionKind
import me.padi.qqlite.revived.shared.model.aio.AioMediaSpec
import me.padi.qqlite.revived.shared.model.aio.AioMessage
import me.padi.qqlite.revived.shared.model.aio.AioMessageKind
import me.padi.qqlite.revived.shared.model.aio.AioPeer
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.io.File
import java.text.DateFormat
import java.util.Date
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
internal fun AioChatScreen(controller: AioUiController) {
    val uiState by controller.uiState.collectAsState()

    MiuixTheme(
        controller = remember { ThemeController(colorSchemeMode = ColorSchemeMode.System) }
    ) {
        var showEmojiSheet by remember { mutableStateOf(false) }
        ApplyAioEdgeToEdge()
        Scaffold(
            topBar = {
                AioTopBar(
                    peer = uiState.peer,
                    avatar = uiState.peerAvatar,
                    controller = controller
                )
            },
            bottomBar = {
                AioInputBar(
                    draft = uiState.draft,
                    onDraftChanged = controller::updateDraft,
                    onEmojiClick = { showEmojiSheet = true },
                    onSendClick = controller::sendDraft
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.secondaryVariant)
                    .padding(padding)
            ) {
                if (uiState.loading && uiState.messages.isEmpty()) {
                    AioLoading()
                } else {
                    AioMessageList(
                        messages = uiState.messages,
                        loadingOlder = uiState.loadingOlder,
                        renderRevision = uiState.renderRevision,
                        scrollToBottomRequest = uiState.scrollToBottomRequest,
                        controller = controller,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        LaunchedEffect(showEmojiSheet) {
            if (showEmojiSheet) {
                controller.requestEmotionCategories()
            }
        }
        AioEmojiSheet(
            show = showEmojiSheet,
            categories = uiState.emotionCategories,
            controller = controller,
            onDismissRequest = { showEmojiSheet = false },
            onEmotionClick = { item ->
                controller.clickEmotion(item)
                showEmojiSheet = false
            }
        )
    }
}

@Suppress("DEPRECATION")
@Composable
private fun ApplyAioEdgeToEdge() {
    val view = LocalView.current
    val backgroundColor = MiuixTheme.colorScheme.secondaryVariant
    val useLightSystemBars = backgroundColor.luminance() > 0.5f
    DisposableEffect(view, backgroundColor, useLightSystemBars) {
        val activity = view.context.findActivity()
        val window = activity?.window
        val oldStatusBarColor = window?.statusBarColor
        val oldNavigationBarColor = window?.navigationBarColor
        val oldSystemUiVisibility = window?.decorView?.systemUiVisibility
        val oldNavigationContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isNavigationBarContrastEnforced
        } else {
            null
        }
        val oldStatusContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isStatusBarContrastEnforced
        } else {
            null
        }

        if (window != null) {
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.setSystemBarsAppearance(
                    if (useLightSystemBars) {
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    } else {
                        0
                    },
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
            val lightFlags = if (useLightSystemBars) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                0
            }
            window.decorView.systemUiVisibility =
                (window.decorView.systemUiVisibility and
                        (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR).inv()) or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        lightFlags
        }

        onDispose {
            if (window != null) {
                if (oldStatusBarColor != null) window.statusBarColor = oldStatusBarColor
                if (oldNavigationBarColor != null) window.navigationBarColor = oldNavigationBarColor
                if (oldSystemUiVisibility != null) {
                    window.decorView.systemUiVisibility = oldSystemUiVisibility
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (oldNavigationContrast != null) {
                        window.isNavigationBarContrastEnforced = oldNavigationContrast
                    }
                    if (oldStatusContrast != null) {
                        window.isStatusBarContrastEnforced = oldStatusContrast
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun AioTopBar(
    peer: AioPeer,
    avatar: AvatarSpec?,
    controller: AioUiController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 10.dp),
        ) {
            IconButton(
                onClick = controller::navigateBack,
                minWidth = 42.dp,
                minHeight = 42.dp,
                cornerRadius = 21.dp,
                backgroundColor = MiuixTheme.colorScheme.surfaceVariant,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 54.dp)
                    .clickable(onClick = controller::openPeerPanel),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AioAvatar(
                    controller = controller,
                    spec = avatar,
                    fallback = peer.displayName.take(1),
                    size = 38
                )
                Spacer(modifier = Modifier.width(9.dp))
                AnimatedContent(
                    targetState = peer.displayName,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)) togetherWith
                                fadeOut(animationSpec = tween(90))
                    },
                    label = "aioTitle"
                ) { title ->
                    Column(
                        modifier = Modifier.widthIn(max = 172.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = title,
                            style = MiuixTheme.textStyles.title4,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = peer.peerId,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(
                onClick = controller::openPeerPanel,
                minWidth = 40.dp,
                minHeight = 40.dp,
                cornerRadius = 20.dp,
                backgroundColor = Color.Transparent,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "聊天详情",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun AioMessageList(
    messages: List<AioMessage>,
    loadingOlder: Boolean,
    renderRevision: Long,
    scrollToBottomRequest: Long,
    controller: AioUiController,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        AioEmptyMessages(modifier)
        return
    }

    val listState = rememberLazyListState()
    var didInitialScroll by remember { mutableStateOf(false) }
    var loadOlderArmed by remember { mutableStateOf(false) }
    var anchoredFirstMessageKey by remember { mutableStateOf<String?>(null) }
    var anchoredFirstVisibleOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(messages.size) {
        if (!didInitialScroll && messages.isNotEmpty()) {
            listState.scrollToItem(messages.size)
            didInitialScroll = true
        }
    }

    LaunchedEffect(messages.firstOrNull()?.key, messages.size, loadingOlder) {
        val anchorKey = anchoredFirstMessageKey ?: return@LaunchedEffect
        if (loadingOlder) return@LaunchedEffect
        val newIndex = messages.indexOfFirst { it.key == anchorKey }
        if (newIndex >= 0) {
            val targetIndex = (newIndex + 1).coerceAtMost(messages.size)
            listState.scrollToItem(targetIndex, anchoredFirstVisibleOffset)
        }
        anchoredFirstMessageKey = null
    }

    LaunchedEffect(messages.lastOrNull()?.key, loadingOlder) {
        if (!didInitialScroll || messages.isEmpty() || loadingOlder) return@LaunchedEffect
        val lastMessage = messages.last()
        val totalItems = listState.layoutInfo.totalItemsCount
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastMessage.isSelf || lastVisibleIndex >= totalItems - 3) {
            listState.animateScrollToItem(messages.size)
        }
    }

    LaunchedEffect(scrollToBottomRequest, messages.size, didInitialScroll) {
        if (scrollToBottomRequest <= 0L || !didInitialScroll || messages.isEmpty()) {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(messages.size)
    }

    LaunchedEffect(listState, messages.size, didInitialScroll) {
        if (!didInitialScroll || messages.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                item.key != "aio-load-older" && item.key != "aio-bottom-spacer"
            }?.key as? String
        }
            .distinctUntilChanged()
            .collect(controller::syncHostListPosition)
    }

    LaunchedEffect(listState, didInitialScroll) {
        if (!didInitialScroll || messages.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            listState.isScrollInProgress to listState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .collect { (scrolling, firstIndex) ->
                if (scrolling && firstIndex > 2) {
                    loadOlderArmed = true
                }
            }
    }

    LaunchedEffect(listState, messages.size, loadingOlder, didInitialScroll, loadOlderArmed) {
        if (!didInitialScroll || messages.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            listState.isScrollInProgress &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0 &&
                    listState.layoutInfo.totalItemsCount > listState.layoutInfo.visibleItemsInfo.size
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (loadOlderArmed && !loadingOlder) {
                    val anchorItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                        item.key != "aio-load-older" && item.key != "aio-bottom-spacer"
                    }
                    anchoredFirstMessageKey = anchorItem?.key as? String
                        ?: messages.firstOrNull()?.key
                    anchoredFirstVisibleOffset = anchorItem?.offset ?: 0
                    loadOlderArmed = false
                    controller.loadOlderMessages()
                }
            }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "aio-load-older") {
            LoadOlderIndicator(loadingOlder)
        }
        items(messages, key = { it.key }) { message ->
            AioMessageRow(
                message = message,
                renderRevision = renderRevision,
                controller = controller
            )
        }
        item(key = "aio-bottom-spacer") {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun LoadOlderIndicator(loading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (loading) 34.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfiniteProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MiuixTheme.colorScheme.primary
                )
                Text(
                    text = "加载更早消息",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AioMessageRow(
    message: AioMessage,
    renderRevision: Long,
    controller: AioUiController
) {
    if (message.renderKind == AioMessageKind.Tip) {
        TipMessage(message.text)
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isSelf) {
            AioAvatar(
                controller = controller,
                message = message,
                spec = message.avatar,
                fallback = message.senderUid.take(1),
                size = 32
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        MessageBubble(
            message = message,
            renderRevision = renderRevision,
            controller = controller,
            modifier = Modifier.widthIn(max = 292.dp)
        )
        if (message.isSelf) {
            Spacer(modifier = Modifier.width(8.dp))
            AioAvatar(
                controller = controller,
                message = message,
                spec = message.avatar,
                fallback = "我",
                size = 32
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: AioMessage,
    renderRevision: Long,
    controller: AioUiController,
    modifier: Modifier = Modifier
) {
    val color = if (message.isSelf) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surface
    }
    val contentColor = if (message.isSelf) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .combinedClickable(
                onClick = { controller.clickMessage(message) },
                onLongClick = { controller.longClickMessage(message) }
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            when (message.renderKind) {
                AioMessageKind.Text -> TextMessageContent(message, contentColor)
                AioMessageKind.Image -> ImageMessageContent(
                    message,
                    renderRevision,
                    controller,
                    contentColor
                )

                AioMessageKind.Video -> VideoMessageContent(
                    message,
                    renderRevision,
                    controller,
                    contentColor
                )

                AioMessageKind.Voice -> VoiceMessageContent(message, contentColor)
                AioMessageKind.File -> FileMessageContent(message, contentColor)
                AioMessageKind.Unsupported,
                AioMessageKind.Unknown,
                AioMessageKind.Null,
                AioMessageKind.Mix,
                AioMessageKind.Struct,
                AioMessageKind.MultiMsgForward,
                AioMessageKind.Reply,
                AioMessageKind.Wallet,
                AioMessageKind.ArkStruct,
                AioMessageKind.StructLongMsg,
                AioMessageKind.Giphy,
                AioMessageKind.Gift,
                AioMessageKind.TextGift,
                AioMessageKind.OnlineFile,
                AioMessageKind.FaceBubble,
                AioMessageKind.ShareLocation,
                AioMessageKind.OnlineFolder,
                AioMessageKind.Prologue -> UnsupportedMessageContent(
                    message = message,
                    renderRevision = renderRevision,
                    controller = controller,
                    contentColor = contentColor
                )

                AioMessageKind.Tip -> TextMessageContent(message, contentColor)
            }
            Text(
                text = formatMessageTime(message.msgTime),
                color = contentColor.copy(alpha = 0.64f),
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier
                    .align(if (message.isSelf) Alignment.End else Alignment.Start)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TextMessageContent(message: AioMessage, contentColor: Color) {
    Text(
        text = message.text,
        color = contentColor,
        style = MiuixTheme.textStyles.main,
        fontSize = 15.sp
    )
}

@Composable
private fun ImageMessageContent(
    message: AioMessage,
    renderRevision: Long,
    controller: AioUiController,
    contentColor: Color
) {
    val media = message.media
    HostMessagePreview(
        message = message,
        renderRevision = renderRevision,
        controller = controller,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(resolveAspectRatio(media, 4f / 3f))
    ) {
        MediaPreviewFrame(
            media = media,
            icon = Icons.Filled.Image,
            contentDescription = "图片",
            modifier = Modifier.fillMaxSize()
        )
    }
    if (message.text.isNotBlank() && message.text != "图片") {
        Text(
            text = message.text,
            color = contentColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}

@Composable
private fun VideoMessageContent(
    message: AioMessage,
    renderRevision: Long,
    controller: AioUiController,
    contentColor: Color
) {
    val media = message.media
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(resolveAspectRatio(media, 16f / 9f))
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
    ) {
        HostMessagePreview(
            message = message,
            renderRevision = renderRevision,
            controller = controller,
            modifier = Modifier.fillMaxSize()
        ) {
            MediaPreviewFrame(
                media = media,
                icon = Icons.Filled.Videocam,
                contentDescription = "视频",
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.48f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        media?.durationSeconds?.takeIf { it > 0 }?.let { seconds ->
            Text(
                text = formatDuration(seconds),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.46f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
    media?.fileName?.takeIf { it.isNotBlank() }?.let { fileName ->
        Text(
            text = fileName,
            color = contentColor.copy(alpha = 0.78f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}

@Composable
private fun HostMessagePreview(
    message: AioMessage,
    renderRevision: Long,
    controller: AioUiController,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current
    val hostView = rememberHostMessagePreviewView(message, renderRevision, controller, context)
    if (hostView != null) {
        HostMessagePreviewCanvas(
            source = hostView,
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.secondaryContainer)
        )
    } else {
        Box(modifier = modifier) {
            fallback()
        }
    }
}

@Composable
private fun HostMessagePreviewCanvas(
    source: View,
    modifier: Modifier = Modifier
) {
    var frameTick by remember(source) { mutableIntStateOf(0) }
    LaunchedEffect(source) {
        repeat(HOST_PREVIEW_FRAME_REFRESH_COUNT) {
            delay(HOST_PREVIEW_FRAME_DELAY_MS)
            frameTick++
        }
    }
    Canvas(modifier = modifier) {
        frameTick
        val sourceWidth = source.width.takeIf { it > 0 } ?: source.measuredWidth
        val sourceHeight = source.height.takeIf { it > 0 } ?: source.measuredHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return@Canvas

        val scale = maxOf(
            size.width / sourceWidth.toFloat(),
            size.height / sourceHeight.toFloat()
        )
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.withClip(0f, 0f, size.width, size.height) {
                translate(
                    (size.width - sourceWidth * scale) / 2f,
                    (size.height - sourceHeight * scale) / 2f
                )
                scale(scale, scale)
                source.draw(this)
            }
        }
    }
}

@Composable
private fun rememberHostMessagePreviewView(
    message: AioMessage,
    renderRevision: Long,
    controller: AioUiController,
    context: Context
): View? {
    return remember(message.key, message.renderKind, message.media, renderRevision) {
        controller.createHostMessagePreviewView(context, message)
    }
}

@Composable
private fun UnsupportedMessageContent(
    message: AioMessage,
    renderRevision: Long,
    controller: AioUiController,
    contentColor: Color
) {
    Column {
        Text(
            text = message.text,
            color = contentColor.copy(alpha = 0.82f),
            fontSize = 14.sp
        )
        Text(
            text = buildString {
                append(message.rawKind.name)
                message.rawKind.value.takeIf { it >= 0 }?.let {
                    append(" (")
                    append(it)
                    append(')')
                }
            },
            color = contentColor.copy(alpha = 0.56f),
            style = MiuixTheme.textStyles.body2,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun VoiceMessageContent(message: AioMessage, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = message.media?.durationSeconds?.takeIf { it > 0 }?.let {
                "${it}s"
            } ?: message.text,
            color = contentColor,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun FileMessageContent(message: AioMessage, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.padding(start = 9.dp)) {
            Text(
                text = message.media?.fileName?.takeIf { it.isNotBlank() } ?: message.text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            message.media?.fileSize?.takeIf { it > 0L }?.let {
                Text(
                    text = formatFileSize(it),
                    color = contentColor.copy(alpha = 0.68f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun MediaPreviewFrame(
    media: AioMediaSpec?,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val source = media.displayPathForPreview()
        if (!source.isNullOrBlank()) {
            AsyncImage(
                model = rememberAioImageRequest(source, contentDescription),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun TipMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.72f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AioInputBar(
    draft: String,
    onDraftChanged: (String) -> Unit,
    onEmojiClick: () -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(
            onClick = onEmojiClick,
            minWidth = 44.dp,
            minHeight = 44.dp,
            cornerRadius = 22.dp,
            backgroundColor = MiuixTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEmotions,
                contentDescription = "表情",
                tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        TextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .heightIn(min = 44.dp, max = 112.dp),
            insideMargin = DpSize(13.dp, 10.dp),
            cornerRadius = 22.dp,
            label = "消息",
            useLabelAsPlaceholder = true,
            singleLine = false,
            minLines = 1,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            textStyle = MiuixTheme.textStyles.main.copy(fontSize = 14.sp)
        )
        Button(
            onClick = onSendClick,
            enabled = draft.isNotBlank(),
            minWidth = 44.dp,
            minHeight = 44.dp,
            cornerRadius = 22.dp,
            insideMargin = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColorsPrimary()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送",
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AioEmojiSheet(
    show: Boolean,
    categories: List<AioEmotionCategory>,
    controller: AioUiController,
    onDismissRequest: () -> Unit,
    onEmotionClick: (AioEmotionItem) -> Unit
) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidth = with(density) { containerSize.width.toDp() }
    val screenHeight = with(density) { containerSize.height.toDp() }
    val horizontalInset = with(density) { 100f.toDp() }
    val sheetWidth = (screenWidth - horizontalInset * 2)
        .coerceAtLeast(240.dp)
        .coerceAtMost(screenWidth)

    WindowBottomSheet(
        show = show,
        modifier = Modifier
            .width(sheetWidth)
            .height(screenHeight),
        title = "表情",
        insideMargin = DpSize(0.dp, 0.dp),
        enableNestedScroll = true,
        endAction = {
            val dismiss = LocalDismissState.current
            IconButton(
                onClick = { dismiss?.invoke() ?: onDismissRequest() },
                minWidth = 36.dp,
                minHeight = 36.dp,
                cornerRadius = 18.dp,
                backgroundColor = MiuixTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        onDismissRequest = onDismissRequest
    ) {
        val visibleCategories = if (categories.isNotEmpty()) {
            categories
        } else {
            listOf(AioEmotionCategory("loading", "表情", loading = true))
        }
        val tabs = visibleCategories.map { it.title }
        var selectedTabIndex by remember(show) { mutableIntStateOf(0) }
        LaunchedEffect(visibleCategories.size, selectedTabIndex) {
            if (selectedTabIndex >= visibleCategories.size) {
                selectedTabIndex = 0
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TabRowWithContour(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(160)) togetherWith
                            fadeOut(animationSpec = tween(90))
                },
                label = "aioEmojiCategory",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp)
            ) { tabIndex ->
                val category = visibleCategories.getOrNull(tabIndex) ?: visibleCategories.first()
                AioEmojiCategoryGrid(
                    category = category,
                    controller = controller,
                    modifier = Modifier.fillMaxSize(),
                    onEmotionClick = onEmotionClick
                )
            }
        }
    }
}

@Composable
private fun AioEmojiCategoryGrid(
    category: AioEmotionCategory,
    controller: AioUiController,
    modifier: Modifier = Modifier,
    onEmotionClick: (AioEmotionItem) -> Unit
) {
    if (category.loading && category.items.isEmpty()) {
        AioEmotionSheetState("正在加载表情", loading = true, modifier = modifier)
        return
    }
    if (category.items.isEmpty()) {
        AioEmotionSheetState("暂无表情", loading = false, modifier = modifier)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (category.key.contains("system")) 5 else 3),
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridItems(category.items, key = { it.key }) { item ->
            AioEmojiCategoryItem(
                item = item,
                controller = controller,
                onClick = { onEmotionClick(item) }
            )
        }
    }
}

@Composable
private fun AioEmojiCategoryItem(
    item: AioEmotionItem,
    controller: AioUiController,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            item.kind == AioEmotionKind.System -> {
                AndroidView(
                    factory = { context ->
                        controller.createEmotionPreviewView(context, item) ?: ImageView(context)
                    },
                    update = { view -> controller.reloadEmotionPreview(view, item) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            item.displayPathForPreview() != null -> {
                AsyncImage(
                    model = rememberAioImageRequest(
                        value = item.displayPathForPreview().orEmpty(),
                        cacheKeyPrefix = "emotion-${item.key}"
                    ),
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Text(
                    text = when (item.kind) {
                        AioEmotionKind.MarketFavorite -> "商"
                        AioEmotionKind.Favorite -> "藏"
                        AioEmotionKind.Hot -> "热"
                        AioEmotionKind.System -> item.title.take(2)
                    },
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AioEmotionSheetState(
    text: String,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            InfiniteProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MiuixTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = if (loading) 10.dp else 0.dp)
        )
    }
}

@Composable
private fun AioAvatar(
    controller: AioUiController,
    message: AioMessage? = null,
    spec: AvatarSpec?,
    fallback: String,
    size: Int,
    modifier: Modifier = Modifier,
    useHostAvatar: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.18f), CircleShape)
            .combinedClickable(
                onClick = { message?.let(controller::clickAvatar) },
                onLongClick = { message?.let(controller::longClickMessage) }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (useHostAvatar && spec != null && (spec.uid.isNotBlank() || spec.uin > 0L)) {
            AndroidView(
                factory = { context -> controller.createAvatarView(context, spec) },
                update = { view ->
                    (view as? ImageView)?.scaleType = ImageView.ScaleType.CENTER_CROP
                    controller.reloadAvatar(view, spec)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = fallback.ifBlank { "Q" }.take(1),
                color = MiuixTheme.colorScheme.primary,
                fontSize = (size * 0.46f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AioLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        InfiniteProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = MiuixTheme.colorScheme.primary
        )
        Text(
            text = "正在加载聊天",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun AioEmptyMessages(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = "暂无聊天内容",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun resolveAspectRatio(media: AioMediaSpec?, fallback: Float): Float {
    val width = media?.width ?: 0
    val height = media?.height ?: 0
    if (width <= 0 || height <= 0) return fallback
    return (width.toFloat() / height.toFloat()).coerceIn(0.72f, 1.78f)
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remain = seconds % 60
    return "%d:%02d".format(minutes, remain)
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024f
    if (kb < 1024f) return "%.1f KB".format(kb)
    val mb = kb / 1024f
    return "%.1f MB".format(mb)
}

private fun formatMessageTime(value: Long): String {
    if (value <= 0L) return ""
    val millis = if (value > 100_000_000_000L) value else value * 1000L
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
}

@Composable
private fun rememberAioImageRequest(value: String, cacheKeyPrefix: String): ImageRequest? {
    val context = LocalContext.current
    val model = remember(value) { imageModel(value) } ?: return null
    val cacheKey = remember(model, cacheKeyPrefix) { "aio:$cacheKeyPrefix:$model" }
    return remember(context, model, cacheKey) {
        ImageRequest.Builder(context)
            .data(model)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .build()
    }
}

private fun imageModel(value: String?): Any? {
    val normalized = value?.takeIf { it.isNotBlank() } ?: return null
    return when {
        normalized.startsWith("/") -> File(normalized).takeIf { it.exists() }
        normalized.startsWith("file://", ignoreCase = true) ||
                normalized.startsWith("content://", ignoreCase = true) -> normalized

        else -> normalizeImageUrl(normalized)
    }
}

private fun AioMediaSpec?.displayPathForPreview(): String? {
    if (this == null) return null
    localPath?.trim()?.takeIf(String::isNotBlank)?.let { path ->
        if (!path.startsWith("/") ||
            path.startsWith("file://", ignoreCase = true) ||
            path.startsWith("content://", ignoreCase = true) ||
            File(path).exists()
        ) {
            return path
        }
    }
    return normalizeImageUrl(remoteUrl)
}

private fun AioEmotionItem.displayPathForPreview(): String? {
    localPath?.trim()?.takeIf(String::isNotBlank)?.let { path ->
        if (!path.startsWith("/") ||
            path.startsWith("file://", ignoreCase = true) ||
            path.startsWith("content://", ignoreCase = true) ||
            File(path).exists()
        ) {
            return path
        }
    }
    return normalizeImageUrl(remoteUrl)
}

private fun normalizeImageUrl(value: String?): String? {
    val url = value?.trim()?.takeIf(String::isNotBlank) ?: return null
    return when {
        url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true) -> url

        url.startsWith("//") -> "https:$url"
        url.contains('.') && !url.startsWith("/") -> "https://$url"
        else -> url
    }
}

private const val HOST_PREVIEW_FRAME_DELAY_MS = 250L
private const val HOST_PREVIEW_FRAME_REFRESH_COUNT = 12
