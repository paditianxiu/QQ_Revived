package me.padi.qqlite.revived.compose.screens.aio

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withClip
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import me.padi.qqlite.revived.compose.theme.ApplyTransparentSystemBars
import me.padi.qqlite.revived.compose.theme.RevivedTheme
import me.padi.qqlite.revived.shared.model.aio.AioEmotionCategory
import me.padi.qqlite.revived.shared.model.aio.AioEmotionItem
import me.padi.qqlite.revived.shared.model.aio.AioEmotionKind
import me.padi.qqlite.revived.shared.model.aio.AioLongPressMenuState
import me.padi.qqlite.revived.shared.model.aio.AioMediaSpec
import me.padi.qqlite.revived.shared.model.aio.AioMessage
import me.padi.qqlite.revived.shared.model.aio.AioMessageBadge
import me.padi.qqlite.revived.shared.model.aio.AioMessageKind
import me.padi.qqlite.revived.shared.model.aio.AioPeer
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import me.padi.qqlite.revived.shared.model.ui.AioWindowInfo
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
internal fun AioChatScreen(controller: AioUiController) {
    val uiState by controller.uiState.collectAsState()
    val pagingMessages = controller.pagingMessages.collectAsLazyPagingItems()
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val windowInfo = remember(containerSize, density) {
        with(density) {
            AioWindowInfo.create(
                width = containerSize.width.toDp(),
                height = containerSize.height.toDp()
            )
        }
    }

    RevivedTheme {
        var showEmojiSheet by remember { mutableStateOf(false) }
        var showPbDialog by remember { mutableStateOf(false) }
        var pbJsonText by remember { mutableStateOf("") }
        var previewMessage by remember { mutableStateOf<AioMessage?>(null) }
        val chatBackgroundColor = MiuixTheme.colorScheme.secondaryVariant
        val topBarBackdrop = rememberLayerBackdrop {
            drawRect(chatBackgroundColor)
            drawContent()
        }
        ApplyTransparentSystemBars(MiuixTheme.colorScheme.surface)
        Scaffold(topBar = {
            AioTopBar(
                peer = uiState.peer,
                avatar = uiState.peerAvatar,
                controller = controller,
                windowInfo = windowInfo,
                backdrop = topBarBackdrop,
            )
        }, bottomBar = {
            AioInputBar(
                draft = uiState.draft,
                onDraftChanged = controller::updateDraft,
                onEmojiClick = { showEmojiSheet = true },
                onPbClick = { showPbDialog = true },
                onSendClick = controller::sendDraft,
                windowInfo = windowInfo
            )
        }) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(chatBackgroundColor)
                    .layerBackdrop(topBarBackdrop)
                    .padding(padding)
            ) {
                if (uiState.loading && pagingMessages.itemCount == 0) {
                    AioLoading()
                } else {
                    AioMessageList(
                        messages = pagingMessages,
                        loadingOlder = uiState.loadingOlder,
                        renderRevision = uiState.renderRevision,
                        scrollToBottomRequest = uiState.scrollToBottomRequest,
                        initialFirstVisibleMessageKey = uiState.firstVisibleMessageKey,
                        initialFirstVisibleMessageOffset = uiState.firstVisibleMessageOffset,
                        controller = controller,
                        onPreviewMessage = { previewMessage = it },
                        onLongPressMessage = { message, anchor ->
                            controller.rememberLongPressAnchor(anchor)
                            controller.longClickMessage(message)
                        },
                        windowInfo = windowInfo,
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
            windowInfo = windowInfo,
            onEmotionClick = { item ->
                controller.clickEmotion(item)
                showEmojiSheet = false
            })
        PbSendDialog(
            show = showPbDialog,
            text = pbJsonText,
            onTextChange = { pbJsonText = it },
            onDismissRequest = { showPbDialog = false },
            onSendClick = {
                val sent = controller.sendCustomPbMessage(pbJsonText)
                pbJsonText = ""
                if (sent) {
                    showPbDialog = false
                }
            })
        AioMediaPreviewSheet(
            message = previewMessage, onDismissRequest = { previewMessage = null })
        AioLongPressMenuPopup(
            state = uiState.longPressMenu,
            onDismissRequest = controller::dismissLongPressMenu,
            onSelectIndex = controller::selectLongPressMenu
        )
    }
}

@Composable
private fun AioTopBar(
    peer: AioPeer,
    avatar: AvatarSpec?,
    controller: AioUiController,
    windowInfo: AioWindowInfo,
    backdrop: Backdrop,
) {
    val topBarSurfaceColor = MiuixTheme.colorScheme.surface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp) },
                effects = {
                    vibrancy()
                    blur(18.dp.toPx())
                    lens(28.dp.toPx(), 18.dp.toPx())
                },
                highlight = {
                    Highlight.Default.copy(alpha = 0.24f)
                },
                onDrawSurface = {
                    drawRect(topBarSurfaceColor.copy(alpha = 0.42f))
                }
            )
            .background(Color.Transparent)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(windowInfo.topBarHeight)
                .padding(horizontal = windowInfo.topBarHorizontalPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = controller::navigateBack,
                    minWidth = 42.dp,
                    minHeight = 42.dp,
                    cornerRadius = 21.dp,
                    backgroundColor = MiuixTheme.colorScheme.surfaceVariant
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
                        .weight(1f)
                        .clickable(onClick = controller::openPeerPanel),
                    verticalAlignment = Alignment.CenterVertically
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
                            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(
                                animationSpec = tween(90)
                            )
                        },
                        label = "aioTitle"
                    ) { title ->
                        Column(
                            modifier = Modifier.widthIn(max = windowInfo.titleMaxWidth),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (peer.chatType == 1) {
                        IconButton(
                            onClick = controller::startVoiceCall,
                            minWidth = 40.dp,
                            minHeight = 40.dp,
                            cornerRadius = 20.dp,
                            backgroundColor = Color.Transparent
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "语音通话",
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = controller::startVideoCall,
                            minWidth = 40.dp,
                            minHeight = 40.dp,
                            cornerRadius = 20.dp,
                            backgroundColor = Color.Transparent
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = "视频通话",
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = controller::openPeerPanel,
                        minWidth = 40.dp,
                        minHeight = 40.dp,
                        cornerRadius = 20.dp,
                        backgroundColor = Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "聊天详情",
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
    messages: LazyPagingItems<AioMessage>,
    loadingOlder: Boolean,
    renderRevision: Long,
    scrollToBottomRequest: Long,
    initialFirstVisibleMessageKey: String?,
    initialFirstVisibleMessageOffset: Int,
    controller: AioUiController,
    onPreviewMessage: (AioMessage) -> Unit,
    onLongPressMessage: (AioMessage, IntOffset) -> Unit,
    windowInfo: AioWindowInfo,
    modifier: Modifier = Modifier
) {
    val snapshotMessages = messages.itemSnapshotList.items
    if (snapshotMessages.isEmpty()) {
        AioEmptyMessages(modifier)
        return
    }

    val listState = rememberLazyListState()
    var didInitialScroll by remember { mutableStateOf(false) }
    val restoreMessageKey = remember { initialFirstVisibleMessageKey }
    val restoreMessageOffset = remember { initialFirstVisibleMessageOffset }
    var loadOlderArmed by remember { mutableStateOf(false) }
    var anchoredFirstMessageKey by remember { mutableStateOf<String?>(null) }
    var anchoredFirstVisibleOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(messages.itemCount) {
        if (!didInitialScroll && messages.itemCount > 0) {
            val restoredIndex =
                restoreMessageKey?.let { key -> snapshotMessages.indexOfFirst { it.key == key } }
                    ?.takeIf { it >= 0 }
            if (restoredIndex != null) {
                listState.scrollToItem(restoredIndex, restoreMessageOffset)
            } else {
                listState.scrollToItem(messages.itemCount)
            }
            didInitialScroll = true
        }
    }

    LaunchedEffect(snapshotMessages.firstOrNull()?.key, messages.itemCount, loadingOlder) {
        val anchorKey = anchoredFirstMessageKey ?: return@LaunchedEffect
        if (loadingOlder) return@LaunchedEffect
        val newIndex = snapshotMessages.indexOfFirst { it.key == anchorKey }
        if (newIndex >= 0) {
            val targetIndex = (newIndex + 1).coerceAtMost(messages.itemCount)
            listState.scrollToItem(targetIndex, anchoredFirstVisibleOffset)
        }
        anchoredFirstMessageKey = null
    }

    LaunchedEffect(snapshotMessages.lastOrNull()?.key, loadingOlder) {
        if (!didInitialScroll || snapshotMessages.isEmpty() || loadingOlder) return@LaunchedEffect
        val lastMessage = snapshotMessages.last()
        val totalItems = listState.layoutInfo.totalItemsCount
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastMessage.isSelf || lastVisibleIndex >= totalItems - 3) {
            listState.animateScrollToItem(messages.itemCount)
        }
    }

    LaunchedEffect(scrollToBottomRequest, messages.itemCount, didInitialScroll) {
        if (scrollToBottomRequest <= 0L || !didInitialScroll || messages.itemCount == 0) {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(messages.itemCount)
    }

    LaunchedEffect(listState, messages.itemCount, didInitialScroll) {
        if (!didInitialScroll || messages.itemCount == 0) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                item.key != "aio-load-older" && item.key != "aio-bottom-spacer"
            }?.key as? String
        }.distinctUntilChanged().collect(controller::syncHostListPosition)
    }

    LaunchedEffect(listState, didInitialScroll, messages.itemCount) {
        if (!didInitialScroll || messages.itemCount == 0) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                item.key != "aio-load-older" && item.key != "aio-bottom-spacer"
            }?.let { item ->
                (item.key as? String) to item.offset
            }
        }.distinctUntilChanged().collect { snapshot ->
            controller.updateScrollSnapshot(snapshot?.first, snapshot?.second ?: 0)
        }
    }

    LaunchedEffect(listState, didInitialScroll, messages.itemCount) {
        if (!didInitialScroll || messages.itemCount == 0) return@LaunchedEffect
        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }.distinctUntilChanged().collect { (scrolling, firstIndex, firstOffset) ->
            if (scrolling && (firstIndex > 0 || firstOffset > 0)) {
                loadOlderArmed = true
            }
        }
    }

    LaunchedEffect(listState, messages.itemCount, loadingOlder, didInitialScroll, loadOlderArmed) {
        if (!didInitialScroll || messages.itemCount == 0) return@LaunchedEffect
        snapshotFlow {
            listState.isScrollInProgress && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 && listState.layoutInfo.totalItemsCount > listState.layoutInfo.visibleItemsInfo.size
        }.distinctUntilChanged().filter { it }.collect {
            if (loadOlderArmed && !loadingOlder) {
                val anchorItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    item.key != "aio-load-older" && item.key != "aio-bottom-spacer"
                }
                anchoredFirstMessageKey =
                    anchorItem?.key as? String ?: snapshotMessages.firstOrNull()?.key
                anchoredFirstVisibleOffset = anchorItem?.offset ?: 0
                loadOlderArmed = false
                controller.loadOlderMessages()
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            horizontal = windowInfo.listHorizontalPadding,
            vertical = windowInfo.listVerticalPadding
        ),
        verticalArrangement = Arrangement.spacedBy(windowInfo.listItemSpacing)
    ) {
        item(key = "aio-load-older") {
            LoadOlderIndicator(loadingOlder)
        }
        items(
            count = messages.itemCount, key = messages.itemKey { it.key }) { index ->
            messages[index]?.let { message ->
                val previousMessage = if (index > 0) messages[index - 1] else null
                AioMessageRow(
                    message = message,
                    previousMessage = previousMessage,
                    renderRevision = renderRevision,
                    controller = controller,
                    onPreviewMessage = onPreviewMessage,
                    onLongPressMessage = onLongPressMessage,
                    windowInfo = windowInfo
                )
            }
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
                    modifier = Modifier.size(18.dp), color = MiuixTheme.colorScheme.primary
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
    previousMessage: AioMessage?,
    renderRevision: Long,
    controller: AioUiController,
    onPreviewMessage: (AioMessage) -> Unit,
    onLongPressMessage: (AioMessage, IntOffset) -> Unit,
    windowInfo: AioWindowInfo,
) {
    if (message.renderKind == AioMessageKind.Tip) {
        TipMessage(message.text)
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (shouldShowTimeDivider(
                message, previousMessage
            ) && message.timeDividerText.isNotBlank()
        ) {
            MessageTimeDivider(message.timeDividerText)
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
                    fallback = message.senderName.ifBlank { message.senderUid }.take(1),
                    size = 32
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(
                horizontalAlignment = if (message.isSelf) Alignment.End else Alignment.Start
            ) {
                MessageMetaLine(
                    name = message.senderName.ifBlank { if (message.isSelf) "我" else message.senderUid },
                    badges = message.badges,
                    alignEnd = message.isSelf,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                MessageBubble(
                    message = message,
                    renderRevision = renderRevision,
                    controller = controller,
                    onPreviewMessage = onPreviewMessage,
                    onLongPressMessage = onLongPressMessage,
                    modifier = Modifier.widthIn(max = windowInfo.bubbleMaxWidth)
                )
                MessageTypeCard(
                    message = message,
                    alignEnd = message.isSelf,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
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
}

private fun shouldShowTimeDivider(message: AioMessage, previousMessage: AioMessage?): Boolean {
    if (message.showTimeDivider) return true
    if (previousMessage == null) return true
    val currentTime = message.msgTime
    val previousTime = previousMessage.msgTime
    if (currentTime <= 0L || previousTime <= 0L) return false
    val currentSeconds = normalizeMessageEpochSeconds(currentTime)
    val previousSeconds = normalizeMessageEpochSeconds(previousTime)
    return currentSeconds - previousSeconds >= 5 * 60
}

private fun normalizeMessageEpochSeconds(value: Long): Long {
    return if (value > 100_000_000_000L) value / 1000L else value
}

private fun formatFullMessageEventTime(message: AioMessage): String {
    val rawTime = when {
        message.msgTime > 0L -> message.msgTime
        message.sortTime > 0L -> message.sortTime
        else -> 0L
    }
    if (rawTime <= 0L) return "时间未知"
    val millis = if (rawTime > 100_000_000_000L) rawTime else rawTime * 1000L
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
}

@Composable
private fun MessageTypeCard(
    message: AioMessage, alignEnd: Boolean, modifier: Modifier = Modifier
) {
    val eventTimeText = formatFullMessageEventTime(message)
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart)
                .widthIn(max = 292.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f))
                .border(
                    width = 0.5.dp,
                    color = MiuixTheme.colorScheme.outline.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${message.renderKind.displayName}  $eventTimeText",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MessageMetaLine(
    name: String, badges: List<AioMessageBadge>, alignEnd: Boolean, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        if (!alignEnd) {
            Text(
                text = name,
                color = Color(0xFF545454),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            badges.forEach { badge ->
                Spacer(modifier = Modifier.width(6.dp))
                MessageBadge(badge)
            }
        } else {
            badges.forEachIndexed { index, badge ->
                MessageBadge(badge)
                if (index != badges.lastIndex) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
            if (badges.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = name,
                color = Color(0xFF545454),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: AioMessage,
    renderRevision: Long,
    controller: AioUiController,
    onPreviewMessage: (AioMessage) -> Unit,
    onLongPressMessage: (AioMessage, IntOffset) -> Unit,
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

    var bubblePositionInWindow by remember(message.key) { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .onGloballyPositioned { coordinates ->
                bubblePositionInWindow = coordinates.positionInWindow()
            }
            .pointerInput(message.key, bubblePositionInWindow) {
                detectTapGestures(onTap = {
                    when (message.renderKind) {
                        AioMessageKind.Image, AioMessageKind.Giphy -> onPreviewMessage(message)

                        else -> controller.clickMessage(message)
                    }
                }, onLongPress = { pressOffset ->
                    onLongPressMessage(
                        message, IntOffset(
                            x = (bubblePositionInWindow.x + pressOffset.x).roundToInt(),
                            y = (bubblePositionInWindow.y + pressOffset.y).roundToInt()
                        )
                    )
                })
            }) {
        Column(modifier = Modifier.padding(10.dp)) {
            when (message.renderKind) {
                AioMessageKind.Text -> TextMessageContent(message, contentColor)
                AioMessageKind.Call -> CallMessageContent(message, contentColor)
                AioMessageKind.Image, AioMessageKind.Giphy -> ImageMessageContent(
                    message, contentColor
                )

                AioMessageKind.Video -> VideoMessageContent(
                    message, renderRevision, controller, contentColor
                )

                AioMessageKind.Voice -> VoiceMessageContent(message, contentColor)
                AioMessageKind.File -> FileMessageContent(message, contentColor)
                AioMessageKind.MultiMsgForward -> MultiForwardMessageContent(message, contentColor)
                AioMessageKind.Wallet -> WalletMessageContent(message, contentColor)
                AioMessageKind.ArkStruct -> ArkMessageContent(message, contentColor)
                AioMessageKind.Unsupported, AioMessageKind.Unknown, AioMessageKind.Null, AioMessageKind.Mix, AioMessageKind.Struct, AioMessageKind.Reply, AioMessageKind.StructLongMsg, AioMessageKind.Gift, AioMessageKind.TextGift, AioMessageKind.OnlineFile, AioMessageKind.FaceBubble, AioMessageKind.ShareLocation, AioMessageKind.OnlineFolder, AioMessageKind.Prologue -> UnsupportedMessageContent(
                    message = message,
                    renderRevision = renderRevision,
                    controller = controller,
                    contentColor = contentColor
                )

                AioMessageKind.Tip -> TextMessageContent(message, contentColor)
            }
        }
    }
}

@Composable
private fun MessageTimeDivider(text: String) {
    Text(
        text = text,
        color = Color(0xFF999999),
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun MessageBadge(badge: AioMessageBadge) {
    val accent = badge.colorArgb?.let(::Color) ?: Color(0xFFF08A24)
    val background = badge.backgroundColorArgb?.let(::Color) ?: accent.copy(alpha = 0.12f)
    val border = if (badge.backgroundColorArgb != null) {
        background.copy(alpha = 0.9f)
    } else {
        accent.copy(alpha = 0.32f)
    }
    Text(
        text = badge.text,
        color = accent,
        fontSize = 10.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(0.5.dp, border, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
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
    message: AioMessage, contentColor: Color
) {
    val media = message.media
    ImagePreviewFrame(
        message = message,
        media = media,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(resolveAspectRatio(media, 4f / 3f))
    )
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
    message: AioMessage, renderRevision: Long, controller: AioUiController, contentColor: Color
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
                cacheKeyPrefix = "message-video:${message.key}",
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.48f)), contentAlignment = Alignment.Center
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
    source: View, modifier: Modifier = Modifier
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
            size.width / sourceWidth.toFloat(), size.height / sourceHeight.toFloat()
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
    message: AioMessage, renderRevision: Long, controller: AioUiController, context: Context
): View? {
    return remember(message.key, message.renderKind, message.media, renderRevision) {
        controller.createHostMessagePreviewView(context, message)
    }
}

@Composable
private fun AioMediaPreviewSheet(
    message: AioMessage?, onDismissRequest: () -> Unit
) {
    if (message == null || message.renderKind == AioMessageKind.Video) return
    val media = message.media ?: return
    val title = "图片"
    val dismiss = LocalDismissState.current

    WindowBottomSheet(
        show = true,
        modifier = Modifier.fillMaxSize(),
        title = title,
        insideMargin = DpSize(0.dp, 0.dp),
        endAction = {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AioImagePreviewContent(message = message, media = media)
        }
    }
}

@Composable
private fun AioImagePreviewContent(message: AioMessage, media: AioMediaSpec) {
    val source = media.displayPathForImagePreview() ?: return
    val request = rememberAioImageRequest(source, "preview-image:${message.key}") ?: return
    val preloadPainter = rememberAsyncImagePainter(model = request)
    val imageState by preloadPainter.state.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        when (imageState) {
            is AsyncImagePainter.State.Success -> {
                CoilZoomAsyncImage(
                    model = request,
                    contentDescription = "图片预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            is AsyncImagePainter.State.Error -> {
                Text(
                    text = "图片加载失败", color = Color.White, fontSize = 13.sp
                )
            }

            else -> {
                InfiniteProgressIndicator(
                    modifier = Modifier.size(28.dp), color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AioVideoPreviewContent(media: AioMediaSpec) {
    val source = media.displayPathForPlayback() ?: return
    val uri = remember(source) { source.toPreviewUri() } ?: return
    var isPlaying by remember(source) { mutableStateOf(true) }
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    setVideoURI(uri)
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        if (isPlaying) {
                            start()
                        }
                    }
                    setOnClickListener {
                        if (isPlaying) {
                            pause()
                            isPlaying = false
                        } else {
                            start()
                            isPlaying = true
                        }
                    }
                }
            }, update = { view ->
                if (view.tag != uri.toString()) {
                    view.tag = uri.toString()
                    view.setVideoURI(uri)
                }
                if (isPlaying) {
                    if (!view.isPlaying) {
                        view.start()
                    }
                } else if (view.isPlaying) {
                    view.pause()
                }
            }, modifier = Modifier.fillMaxSize()
        )
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun ArkMessageContent(message: AioMessage, contentColor: Color) {
    val preview = message.arkPreview
    Column(modifier = Modifier.widthIn(min = 180.dp, max = 260.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val headerIcon = preview?.icon.orEmpty()
            if (headerIcon.isNotBlank()) {
                AsyncImage(
                    model = rememberAioImageRequest(headerIcon, "ark-header-icon:${message.key}"),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = preview?.title?.ifBlank { "卡片消息" } ?: "卡片消息",
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        preview?.desc?.takeIf { it.isNotBlank() }?.let { desc ->
            Text(
                text = desc,
                color = contentColor.copy(alpha = 0.78f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        preview?.preview?.takeIf { it.isNotBlank() }?.let { previewImage ->
            AsyncImage(
                model = rememberAioImageRequest(previewImage, "ark-preview:${message.key}"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.08f))
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(contentColor.copy(alpha = 0.14f))
        )
        Row(
            modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Q",
                    color = contentColor.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "QQ小程序",
                color = contentColor.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MultiForwardMessageContent(message: AioMessage, contentColor: Color) {
    val preview = message.forwardPreview
    Column(modifier = Modifier.widthIn(min = 180.dp, max = 260.dp)) {
        Text(text = preview?.header?.ifBlank { "合并转发" } ?: "合并转发",
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis)
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(contentColor.copy(alpha = 0.14f))
        )
        val items = preview?.items.orEmpty()
        if (items.isNotEmpty()) {
            items.take(4).forEach { item ->
                Text(
                    text = item,
                    color = contentColor.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        } else {
            Text(
                text = message.text.ifBlank { "暂无摘要" },
                color = contentColor.copy(alpha = 0.78f),
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 8.dp)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(contentColor.copy(alpha = 0.14f))
        )
        Text(text = preview?.footer?.ifBlank {
            preview.count.takeIf { it > 0 }?.let { "查看${it}条转发消息" } ?: "合并转发"
        } ?: "合并转发",
            color = contentColor.copy(alpha = 0.56f),
            style = MiuixTheme.textStyles.body2,
            fontSize = 11.sp)
    }
}

@Composable
private fun UnsupportedMessageContent(
    message: AioMessage, renderRevision: Long, controller: AioUiController, contentColor: Color
) {
    Column {
        Text(
            text = message.text, color = contentColor.copy(alpha = 0.82f), fontSize = 14.sp
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
private fun WalletMessageContent(message: AioMessage, contentColor: Color) {
    val preview = message.walletPreview
    Column(modifier = Modifier.widthIn(min = 180.dp, max = 240.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFD7A8)), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¥",
                    color = Color(0xFF9A3C00),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = preview?.sourceName?.ifBlank { "QQ红包" } ?: "QQ红包",
                color = contentColor.copy(alpha = 0.92f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Text(text = preview?.title?.ifBlank { "QQ红包" } ?: "QQ红包",
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp))
        preview?.desc?.takeIf { it.isNotBlank() }?.let { desc ->
            Text(
                text = desc,
                color = contentColor.copy(alpha = 0.74f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(contentColor.copy(alpha = 0.14f))
        )
        Text(
            text = "仅支持查看，暂不可领取",
            color = contentColor.copy(alpha = 0.62f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun CallMessageContent(message: AioMessage, contentColor: Color) {
    val isVideoCall = message.text.contains("视频通话")
    val icon = if (isVideoCall) {
        Icons.Filled.Videocam
    } else {
        Icons.Filled.Mic
    }
    val statusText = message.text.removePrefix(if (isVideoCall) "视频通话" else "语音通话").trim()
        .ifBlank { if (isVideoCall) "视频通话" else "语音通话" }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = statusText,
            color = contentColor,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 8.dp)
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
        Text(text = message.media?.durationSeconds?.takeIf { it > 0 }?.let {
            "${it}s"
        } ?: message.text,
            color = contentColor,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 8.dp))
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
            Text(text = message.media?.fileName?.takeIf { it.isNotBlank() } ?: message.text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
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
private fun ImagePreviewFrame(
    message: AioMessage, media: AioMediaSpec?, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val source = media.displayPathForImagePreview()
        if (!source.isNullOrBlank()) {
            AsyncImage(
                model = rememberAioImageRequest(source, "message-image:${message.key}"),
                contentDescription = "图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun MediaPreviewFrame(
    media: AioMediaSpec?,
    icon: ImageVector,
    contentDescription: String,
    cacheKeyPrefix: String,
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
                model = rememberAioImageRequest(source, cacheKeyPrefix),
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
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
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
    onPbClick: () -> Unit,
    onSendClick: () -> Unit,
    windowInfo: AioWindowInfo,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = windowInfo.emojiSheetMaxWidth)
                .align(Alignment.Center)
                .padding(
                    horizontal = windowInfo.inputHorizontalPadding,
                    vertical = windowInfo.inputVerticalPadding
                ),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEmojiClick,
                        onLongClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEmotions,
                    contentDescription = "表情",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onPbClick,
                minWidth = 44.dp,
                minHeight = 44.dp,
                cornerRadius = 22.dp,
                backgroundColor = MiuixTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DataObject,
                    contentDescription = "ProtoBuf",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            TextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 8.dp)
                    .heightIn(min = 44.dp, max = windowInfo.inputMaxHeight),
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
}

@Composable
private fun PbSendDialog(
    show: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onSendClick: () -> Unit
) {
    WindowDialog(
        title = "ProtoBuf 发包",
        summary = "输入 PbSendMsg 的内层 element JSON",
        show = show,
        onDismissRequest = onDismissRequest
    ) {
        val dismiss = LocalDismissState.current
        val isEnabled = text.isNotBlank()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                label = "Pb JSON",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp),
                singleLine = false,
                minLines = 5,
                maxLines = 12
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    text = "取消", onClick = { dismiss?.invoke() }, modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onSendClick,
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        text = "发送 ProtoBuf", color = if (isEnabled) {
                            MiuixTheme.colorScheme.onPrimary
                        } else {
                            MiuixTheme.colorScheme.disabledOnPrimary
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun AioLongPressMenuPopup(
    state: AioLongPressMenuState?, onDismissRequest: () -> Unit, onSelectIndex: (Int) -> Unit
) {
    if (state == null || state.items.isEmpty()) return
    val popupPositionProvider = remember(state.anchorX, state.anchorY) {
        object : top.yukonga.miuix.kmp.basic.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect,
                windowBounds: androidx.compose.ui.unit.IntRect,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize,
                popupMargin: androidx.compose.ui.unit.IntRect,
                alignment: top.yukonga.miuix.kmp.basic.PopupPositionProvider.Align,
            ): IntOffset {
                val rawX = state.anchorX + popupMargin.left
                val rawY = state.anchorY + popupMargin.top
                return IntOffset(
                    x = rawX.coerceIn(
                        windowBounds.left,
                        (windowBounds.right - popupContentSize.width - popupMargin.right).coerceAtLeast(
                            windowBounds.left
                        )
                    ), y = rawY.coerceIn(
                        (windowBounds.top + popupMargin.top).coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                        windowBounds.bottom - popupContentSize.height - popupMargin.bottom
                    )
                )
            }

            override fun getMargins() =
                top.yukonga.miuix.kmp.basic.ListPopupDefaults.ContextMenuPositionProvider.getMargins()
        }
    }
    WindowListPopup(
        show = true,
        popupPositionProvider = popupPositionProvider,
        alignment = top.yukonga.miuix.kmp.basic.PopupPositionProvider.Align.TopStart,
        enableWindowDim = true,
        onDismissRequest = onDismissRequest
    ) {
        ListPopupColumn {
            state.items.forEachIndexed { index, item ->
                DropdownImpl(
                    item = DropdownItem(text = item.label),
                    optionSize = state.items.size,
                    isSelected = false,
                    index = index,
                    onSelectedIndexChange = {
                        onSelectIndex(index)
                    })
            }
        }
    }
}

@Composable
private fun AioEmojiSheet(
    show: Boolean,
    categories: List<AioEmotionCategory>,
    controller: AioUiController,
    onDismissRequest: () -> Unit,
    windowInfo: AioWindowInfo,
    onEmotionClick: (AioEmotionItem) -> Unit
) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenHeight = with(density) { containerSize.height.toDp() }

    val maxSheetHeight = (screenHeight * 0.82f).coerceAtLeast(320.dp)
    val maxGridHeight = (maxSheetHeight - 88.dp).coerceAtLeast(160.dp)

    WindowBottomSheet(
        show = show, title = "表情", enableNestedScroll = true, endAction = {
            val dismiss = LocalDismissState.current
            IconButton(onClick = { dismiss?.invoke() }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Confirm",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        }, onDismissRequest = onDismissRequest
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
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = windowInfo.emojiSheetMaxWidth)
            ) {
                TabRowWithContour(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )
                val category =
                    visibleCategories.getOrNull(selectedTabIndex) ?: visibleCategories.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = maxGridHeight)
                        .padding(top = 12.dp)
                ) {
                    AioEmojiCategoryGrid(
                        category = category,
                        controller = controller,
                        windowInfo = windowInfo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxGridHeight),
                        onEmotionClick = onEmotionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AioEmojiCategoryGrid(
    category: AioEmotionCategory,
    controller: AioUiController,
    windowInfo: AioWindowInfo,
    modifier: Modifier = Modifier,
    onEmotionClick: (AioEmotionItem) -> Unit
) {
    if (category.loading && category.items.isEmpty()) {
        AioEmotionSheetState("正在加载表情", loading = true, modifier = modifier)
        return
    }
    val visibleItems = remember(category.items) { category.items.distinctBy { it.key } }
    if (visibleItems.isEmpty()) {
        AioEmotionSheetState("暂无表情", loading = false, modifier = modifier)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(
            if (category.key.contains("system")) {
                windowInfo.emojiSystemColumns
            } else {
                windowInfo.emojiPackColumns
            }
        ),
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridItems(visibleItems, key = { it.key }) { item ->
            AioEmojiCategoryItem(
                item = item, controller = controller, onClick = { onEmotionClick(item) })
        }
    }
}

@Composable
private fun AioEmojiCategoryItem(
    item: AioEmotionItem, controller: AioUiController, onClick: () -> Unit
) {
    LaunchedEffect(item.key, item.displayPathForPreview()) {
        controller.ensureEmotionPreview(item)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(10.dp), contentAlignment = Alignment.Center
    ) {
        when {
            item.kind == AioEmotionKind.System -> {
                SystemEmotionPreview(
                    item = item, controller = controller, modifier = Modifier.fillMaxSize()
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
    text: String, loading: Boolean, modifier: Modifier = Modifier
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
                modifier = Modifier.size(22.dp), color = MiuixTheme.colorScheme.primary
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
                onLongClick = { message?.let(controller::longClickMessage) }),
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
            modifier = Modifier.size(28.dp), color = MiuixTheme.colorScheme.primary
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
    val model = imageModel(value) ?: return null
    val cacheKey = remember(model, cacheKeyPrefix) { "aio:$cacheKeyPrefix:$model" }
    return remember(context, model, cacheKey) {
        ImageRequest.Builder(context).data(model).memoryCacheKey(cacheKey).diskCacheKey(cacheKey)
            .build()
    }
}

private fun imageModel(value: String?): Any? {
    val normalized = value?.takeIf { it.isNotBlank() } ?: return null
    return when {
        normalized.startsWith("/") -> File(normalized).takeIf { it.exists() }
        normalized.startsWith("file://", ignoreCase = true) || normalized.startsWith(
            "content://",
            ignoreCase = true
        ) -> normalized

        else -> normalizeImageUrl(normalized)
    }
}

@Composable
private fun SystemEmotionPreview(
    item: AioEmotionItem, controller: AioUiController, modifier: Modifier = Modifier
) {
    val drawable = remember(item.key) { controller.getEmotionPreviewDrawable(item) }
    val bitmap = remember(drawable) { drawable?.toPreviewBitmap() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Text(
            text = item.title.take(2),
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun String.toPreviewUri(): Uri? {
    val normalized = trim().takeIf { it.isNotBlank() } ?: return null
    return when {
        normalized.startsWith("content://", ignoreCase = true) || normalized.startsWith(
            "file://",
            ignoreCase = true
        ) || normalized.startsWith(
            "http://",
            ignoreCase = true
        ) || normalized.startsWith("https://", ignoreCase = true) -> Uri.parse(normalized)

        normalized.startsWith("/") -> Uri.fromFile(File(normalized))
        else -> Uri.parse(normalized)
    }
}

private fun AioMediaSpec?.displayPathForPreview(): String? {
    if (this == null) return null
    previewPath.takeUsableMediaPath()?.let { return it }
    localPath.takeUsableMediaPath()?.let { return it }
    playbackPath.takeUsableMediaPath()?.let { return it }
    return normalizeImageUrl(remoteUrl)
}

private fun AioMediaSpec?.displayPathForImagePreview(): String? {
    if (this == null) return null
    playbackPath.takeUsableMediaPath()?.let { return it }
    localPath.takeUsableMediaPath()?.let { return it }
    previewPath.takeUsableMediaPath()?.let { return it }
    remoteUrl.takeUsableRemoteUrl()?.let { return it }
    return null
}

private fun AioMediaSpec?.displayPathForPlayback(): String? {
    if (this == null) return null
    playbackPath.takeUsableMediaPath()?.let { return it }
    localPath.takeUsableMediaPath()?.let { return it }
    previewPath.takeUsableMediaPath()?.let { return it }
    return normalizeImageUrl(remoteUrl)
}

private fun String?.takeUsableMediaPath(): String? {
    this?.trim()?.takeIf(String::isNotBlank)?.let { path ->
        if (!path.startsWith("/") || path.startsWith(
                "file://",
                ignoreCase = true
            ) || path.startsWith("content://", ignoreCase = true) || File(path).exists()
        ) {
            return path
        }
    }
    return null
}

private fun AioEmotionItem.displayPathForPreview(): String? {
    localPath?.trim()?.takeIf(String::isNotBlank)?.let { path ->
        if (!path.startsWith("/") || path.startsWith(
                "file://",
                ignoreCase = true
            ) || path.startsWith("content://", ignoreCase = true) || File(path).exists()
        ) {
            return path
        }
    }
    resultPath?.trim()?.takeIf(String::isNotBlank)?.let { path ->
        if (!path.startsWith("/") || path.startsWith(
                "file://",
                ignoreCase = true
            ) || path.startsWith("content://", ignoreCase = true) || File(path).exists()
        ) {
            return path
        }
    }
    return (resultUrl ?: remoteUrl).takeUsableRemoteUrl()
}

private fun normalizeImageUrl(value: String?): String? {
    val url = value?.trim()?.takeIf(String::isNotBlank) ?: return null
    return when {
        url.startsWith("http://", ignoreCase = true) || url.startsWith(
            "https://",
            ignoreCase = true
        ) -> url

        url.startsWith("//") -> "https:$url"
        url.contains('.') && !url.startsWith("/") -> "https://$url"
        else -> url
    }
}

private fun Drawable.toPreviewBitmap(): android.graphics.Bitmap? {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 128
    val height = intrinsicHeight.takeIf { it > 0 } ?: 128
    return runCatching { toBitmap(width = width, height = height) }.getOrNull()
}

private fun String?.takeUsableRemoteUrl(): String? {
    val url = this?.trim()?.takeIf(String::isNotBlank) ?: return null
    return when {
        url.startsWith("http://", ignoreCase = true) || url.startsWith(
            "https://",
            ignoreCase = true
        ) -> url

        url.startsWith("//") -> "https:$url"
        else -> null
    }
}

private const val HOST_PREVIEW_FRAME_DELAY_MS = 250L
private const val HOST_PREVIEW_FRAME_REFRESH_COUNT = 12
