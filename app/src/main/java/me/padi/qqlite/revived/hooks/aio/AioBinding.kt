package me.padi.qqlite.revived.hooks.aio

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.isVisible
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.padi.qqlite.revived.ModuleMainKt
import me.padi.qqlite.revived.compose.screens.aio.AioUiController
import me.padi.qqlite.revived.shared.model.aio.AioEmotionCategory
import me.padi.qqlite.revived.shared.model.aio.AioEmotionKind
import me.padi.qqlite.revived.shared.model.aio.AioEmotionItem
import me.padi.qqlite.revived.shared.model.aio.AioMessage
import me.padi.qqlite.revived.shared.model.aio.AioMessageKind
import me.padi.qqlite.revived.shared.model.aio.AioPeer
import me.padi.qqlite.revived.shared.model.aio.AioLongPressMenuItem
import me.padi.qqlite.revived.shared.model.aio.AioLongPressMenuState
import me.padi.qqlite.revived.shared.model.aio.AioUiState
import me.padi.qqlite.revived.shared.model.aio.sortedOldestFirst
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import java.io.File
import java.lang.ref.WeakReference

internal class AioBinding(
    private val module: ModuleMainKt,
    private val hookState: AioHookState,
    root: ViewGroup,
    viewPager: View?,
    hostFragment: Any?,
    peer: AioPeer,
    initialState: AioUiState
) : AioUiController {
    private val rootRef = WeakReference(root)
    private var viewPagerRef = WeakReference(viewPager)
    private var hostFragmentRef = WeakReference(hostFragment)
    private var hostMsgRepoRef: WeakReference<Any>? = null
    private var hostAioListVbRef: WeakReference<Any>? = null
    private var inputBarControllerRef: WeakReference<Any>? = null
    private var hostListViewRef: WeakReference<View>? = null
    private val pendingMessages = LinkedHashMap<String, AioMessage>()
    private val messageViewRefs = LinkedHashMap<String, WeakReference<View>>()
    private val pendingVoiceAutoPlayKeys = LinkedHashSet<String>()
    private val pendingEmotionPreviewKeys = LinkedHashSet<String>()
    private var flushScheduled = false
    private var renderRefreshScheduled = false
    private var mediaRefreshSequence = 0
    private var hostListSyncSequence = 0
    private var loadOlderRequestId = 0
    private var emotionLoadStarted = false
    private val pendingMemberInfoUids = LinkedHashSet<String>()
    private var activeLongPressActions: List<() -> Unit> = emptyList()
    private var pendingLongPressAnchor: IntOffset? = null
    private val _pagingMessages = MutableStateFlow(PagingData.from(initialState.messages))
    private val _uiState = MutableStateFlow(
        initialState.copy(
            peer = peer,
            peerAvatar = hookState.toPeerAvatar(peer, hostFragment)
        )
    )

    override val uiState: StateFlow<AioUiState>
        get() = _uiState
    override val pagingMessages: Flow<PagingData<AioMessage>>
        get() = _pagingMessages

    val currentState: AioUiState
        get() = _uiState.value

    fun updateHost(hostFragment: Any?, viewPager: View?, peer: AioPeer) {
        hostFragmentRef = WeakReference(hostFragment)
        if (viewPager != null) {
            viewPagerRef = WeakReference(viewPager)
        }
        updateState { state ->
            val peerAvatar = hookState.toPeerAvatar(peer, hostFragment)
            val avatarFragment = state.peerAvatar?.fragmentRef?.get()
            if (state.peer == peer &&
                state.peerAvatar?.stableKey() == peerAvatar?.stableKey() &&
                avatarFragment === hostFragment
            ) {
                state
            } else {
                state.copy(peer = peer, peerAvatar = peerAvatar)
            }
        }
    }

    fun enqueueMessage(message: AioMessage) {
        rememberMessageView(message)
        pendingMessages[message.key] = message.copy(itemViewRef = null)
        if (flushScheduled) return

        flushScheduled = true
        AioRuntimeStore.mainHandler.postDelayed({
            flushScheduled = false
            flushMessages()
        }, MESSAGE_FLUSH_DELAY_MS)
    }

    fun markLoaded() {
        updateState { state -> state.copy(loading = false) }
    }

    fun flushMessagesNow() {
        flushScheduled = false
        flushMessages()
    }

    fun hostFragment(): Any? = hostFragmentRef.get()

    fun snapshot() {
        AioRuntimeStore.snapshotsByPeer[currentState.peer.stableKey()] = currentState
    }

    fun attachHostMsgRepo(repo: Any?) {
        if (repo != null) {
            hostMsgRepoRef = WeakReference(repo)
        }
    }

    fun attachHostAioListVb(listVb: Any?) {
        if (listVb != null) {
            hostAioListVbRef = WeakReference(listVb)
        }
    }

    fun attachInputBarController(inputBarController: Any?) {
        if (inputBarController != null) {
            inputBarControllerRef = WeakReference(inputBarController)
        }
    }

    fun attachHostListView(listView: View?) {
        if (listView != null) {
            hostListViewRef = WeakReference(listView)
        }
    }

    fun replaceMessagesFromHost(hostRows: Iterable<*>?) {
        val rawRows = hostRows?.toList() ?: return
        runOnMain {
            val rows = hookState.toMessages(rawRows, hostFragment())
            applyMessages(rows, replace = true)
        }
    }

    fun upsertMessagesFromHost(hostRows: Iterable<*>?) {
        val rawRows = hostRows?.toList() ?: return
        if (rawRows.isEmpty()) return
        runOnMain {
            val rows = hookState.toMessages(rawRows, hostFragment())
            if (rows.isNotEmpty()) {
                applyMessages(rows, replace = false)
            }
        }
    }

    fun upsertMessageFromHost(hostRow: Any?) {
        if (hostRow == null) return
        upsertMessagesFromHost(listOf(hostRow))
    }

    fun removeMessagesByIds(hostIds: Iterable<*>?) {
        val ids = hostIds
            ?.mapNotNull { (it as? Number)?.toLong() }
            ?.toHashSet()
            ?: return
        if (ids.isEmpty()) return
        runOnMain {
            pendingMessages.entries.removeAll { it.value.msgId in ids }
            messageViewRefs.entries.removeAll { it.key in currentState.messages
                .filter { message -> message.msgId in ids }
                .map { message -> message.key }
                .toHashSet()
            }
            val nextRows = currentState.messages.filterNot { it.msgId in ids }
            updateState { state ->
                state.copy(messages = nextRows, loading = false, loadingOlder = false)
            }
            publishMessages(nextRows)
        }
    }

    fun clearMessages() {
        runOnMain {
            pendingMessages.clear()
            messageViewRefs.clear()
            updateState { state ->
                state.copy(messages = emptyList(), loading = false, loadingOlder = false)
            }
            publishMessages(emptyList())
        }
    }

    override fun navigateBack() {
        runCatching {
            if (viewPagerRef.get().moveToPreviousAIOFrame()) {
                return
            }
            val popped = hostFragmentRef.get()?.popWatchFragment() == true
            if (popped) {
                clearScrollSnapshotForExit()
                AioRuntimeStore.mainHandler.postDelayed({
                    AioRuntimeStore.releaseAioSurfaceForHome()
                }, RELEASE_AIO_AFTER_BACK_DELAY_MS)
            }
        }.onFailure {
            clearScrollSnapshotForExit()
            AioRuntimeStore.releaseAioSurfaceForHome()
            module.logHook(Log.WARN, "AIO compose back failed", it)
        }
    }

    override fun openPeerPanel() {
        viewPagerRef.get()?.setViewPagerCurrentItem(MENU_PAGE_INDEX, true)
    }

    override fun updateDraft(value: String) {
        updateState { state -> state.copy(draft = value) }
    }

    override fun sendDraft() {
        val text = currentState.draft.trim()
        val listVb = hostAioListVbRef?.get() ?: AioRuntimeStore.latestAioListVb?.get()
        val inputBar = inputBarControllerRef?.get()
            ?: AioRuntimeStore.latestInputBarController?.get()
        if (text.isNotBlank()) {
            updateDraft("")
        }
        val sent = if (text.isBlank()) {
            false
        } else {
            runCatching {
                hookState.trySendText(rootRef.get(), listVb, inputBar, text)
            }.onFailure {
                module.logHook(Log.WARN, "AIO compose send failed", it)
            }.getOrDefault(false)
        }
        if (sent) {
            updateState { state ->
                state.copy(scrollToBottomRequest = state.scrollToBottomRequest + 1L)
            }
        } else if (text.isNotBlank()) {
            updateDraft(text)
        }
    }

    override fun openEmojiPanel() {
        val inputBar = inputBarControllerRef?.get()
            ?: AioRuntimeStore.latestInputBarController?.get()
        runCatching {
            hookState.tryOpenEmoji(rootRef.get(), hostAioListVbRef?.get(), inputBar)
        }.onFailure {
            module.logHook(Log.WARN, "AIO compose emoji click failed", it)
        }
    }

    override fun requestEmotionCategories() {
        if (emotionLoadStarted && currentState.emotionCategories.isNotEmpty()) {
            return
        }
        emotionLoadStarted = true
        runCatching {
            val categories = hookState.loadInitialEmotionCategories()
            updateState { state -> state.copy(emotionCategories = categories) }
            hookState.loadFavoriteEmotionItems { items ->
                replaceEmotionCategory(
                    AioEmotionCategory(
                        key = EMOTION_CATEGORY_FAVORITE,
                        title = "收藏",
                        items = items,
                        loading = false
                    )
                )
            }
            hookState.loadHotEmotionItems { items ->
                replaceEmotionCategory(
                    AioEmotionCategory(
                        key = EMOTION_CATEGORY_HOT,
                        title = "热门",
                        items = items,
                        loading = false
                    )
                )
            }
        }.onFailure {
            emotionLoadStarted = false
            module.logHook(Log.WARN, "AIO compose emotion load failed", it)
            updateState { state ->
                state.copy(
                    emotionCategories = listOf(
                        AioEmotionCategory(EMOTION_CATEGORY_FAVORITE, "收藏"),
                        AioEmotionCategory(EMOTION_CATEGORY_SYSTEM, "表情"),
                        AioEmotionCategory(EMOTION_CATEGORY_BIG_SYSTEM, "大表情"),
                        AioEmotionCategory(EMOTION_CATEGORY_HOT, "热门")
                    )
                )
            }
        }
    }

    override fun clickEmotion(item: AioEmotionItem): Boolean {
        val sent = runCatching {
            val listVb = hostAioListVbRef?.get() ?: AioRuntimeStore.latestAioListVb?.get()
            hookState.sendEmotion(listVb, hostFragment(), item)
        }.onFailure {
            module.logHook(Log.WARN, "AIO compose emotion send failed", it)
        }.getOrDefault(false)
        return sent
    }

    override fun ensureEmotionPreview(item: AioEmotionItem) {
        if (item.kind == AioEmotionKind.System || item.hasUsablePreviewPath()) return
        if (!pendingEmotionPreviewKeys.add(item.key)) return
        hookState.ensureEmotionPreview(item) { updatedItem ->
            pendingEmotionPreviewKeys.remove(item.key)
            updateEmotionItem(updatedItem)
        }
    }

    override fun getEmotionPreviewDrawable(item: AioEmotionItem): Drawable? {
        return hookState.getEmotionPreviewDrawable(item)
    }

    override fun createEmotionPreviewView(context: Context, item: AioEmotionItem): View? {
        return hookState.createEmotionPreviewView(context, item)
    }

    override fun reloadEmotionPreview(view: View, item: AioEmotionItem) {
        hookState.reloadEmotionPreview(view, item)
    }

    override fun loadOlderMessages() {
        val repo = hostMsgRepoRef?.get() ?: return
        if (currentState.loadingOlder) return

        val requestId = ++loadOlderRequestId
        updateState { state -> state.copy(loadingOlder = true) }
        runCatching {
            hookState.requestLoadOlderMessages(repo)
        }.onFailure {
            module.logHook(Log.WARN, "AIO compose load older failed", it)
            updateState { state -> state.copy(loadingOlder = false) }
            return
        }
        AioRuntimeStore.mainHandler.postDelayed({
            if (loadOlderRequestId == requestId && currentState.loadingOlder) {
                updateState { state -> state.copy(loadingOlder = false) }
            }
        }, LOAD_OLDER_TIMEOUT_MS)
    }

    override fun syncHostListPosition(messageKey: String?) {
        val key = messageKey?.takeIf { it.isNotBlank() } ?: return
        val target = currentState.messages.firstOrNull { it.key == key } ?: return
        if (target.needsMediaRefreshPoll()) {
            schedulePendingMediaRefresh(currentState.messages)
        }
        if (messageViewRefs[key]?.get() != null) return
        if (!target.needsHostBinding()) return
        val targetIndex = currentState.messages.indexOfFirst { it.key == key }
        if (targetIndex < 0) return
        val sequence = ++hostListSyncSequence
        AioRuntimeStore.mainHandler.postDelayed({
            if (hostListSyncSequence != sequence) return@postDelayed
            hostListViewRef?.get()?.scrollHostListToPosition(targetIndex)
        }, 96L)
    }

    override fun updateScrollSnapshot(firstVisibleMessageKey: String?, firstVisibleMessageOffset: Int) {
        if (currentState.firstVisibleMessageKey == firstVisibleMessageKey &&
            currentState.firstVisibleMessageOffset == firstVisibleMessageOffset
        ) {
            return
        }
        updateState { state ->
            state.copy(
                firstVisibleMessageKey = firstVisibleMessageKey,
                firstVisibleMessageOffset = firstVisibleMessageOffset
            )
        }
    }

    override fun copyMessageText(message: AioMessage, clipboardManager: ClipboardManager): Boolean {
        val value = message.text.trim()
        if (value.isBlank()) return false
        clipboardManager.setText(AnnotatedString(value))
        return true
    }

    override fun clickMessage(message: AioMessage) {
        val itemView = messageViewRefs[message.key]?.get()
        when (message.renderKind) {
            AioMessageKind.Voice -> {
                if (!message.isVoicePlayable()) {
                    pendingVoiceAutoPlayKeys += message.key
                }
                itemView.performBoundVoiceClick()
            }

            AioMessageKind.Image,
            AioMessageKind.Video,
            AioMessageKind.FaceBubble,
            AioMessageKind.Giphy -> {
                itemView.performBoundMediaClick()
            }

            else -> Unit
        }
    }

    override fun clickAvatar(message: AioMessage) {
        val itemView = messageViewRefs[message.key]?.get()
        itemView.performBestAvatarClick()
    }

    override fun longClickMessage(message: AioMessage) {
        val itemView = messageViewRefs[message.key]?.get()
        itemView.performBestLongClick()
    }

    override fun rememberLongPressAnchor(anchor: IntOffset) {
        pendingLongPressAnchor = anchor
    }

    override fun showLongPressMenu(anchor: IntOffset, items: List<AioLongPressMenuItem>) {
        if (items.isEmpty()) {
            dismissLongPressMenu()
            return
        }
        updateState { state ->
            state.copy(
                longPressMenu = AioLongPressMenuState(
                    anchorX = anchor.x,
                    anchorY = anchor.y,
                    items = items
                )
            )
        }
    }

    fun showLongPressMenu(
        anchor: IntOffset,
        items: List<AioLongPressMenuItem>,
        actions: List<() -> Unit>
    ) {
        activeLongPressActions = actions
        showLongPressMenu(anchor, items)
    }

    override fun dismissLongPressMenu() {
        activeLongPressActions = emptyList()
        updateState { state -> state.copy(longPressMenu = null) }
    }

    override fun selectLongPressMenu(index: Int) {
        val action = activeLongPressActions.getOrNull(index)
        dismissLongPressMenu()
        action?.invoke()
    }

    fun consumePendingLongPressAnchor(): IntOffset? {
        val anchor = pendingLongPressAnchor
        pendingLongPressAnchor = null
        return anchor
    }

    override fun createAvatarView(context: Context, spec: AvatarSpec?): View {
        return hookState.createAvatarView(context, spec)
    }

    override fun reloadAvatar(view: View, spec: AvatarSpec?) {
        hookState.loadAvatar(view, spec)
    }

    override fun createHostMessagePreviewView(context: Context, message: AioMessage): View? {
        val itemView = messageViewRefs[message.key]?.get() ?: return null
        return itemView.findHostMediaSource()
    }

    private fun updateState(transform: (AioUiState) -> AioUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next
        AioRuntimeStore.snapshotsByPeer[next.peer.stableKey()] = next
    }

    private fun clearScrollSnapshotForExit() {
        updateState { state ->
            state.copy(
                firstVisibleMessageKey = null,
                firstVisibleMessageOffset = 0
            )
        }
    }

    private fun replaceEmotionCategory(category: AioEmotionCategory) {
        runOnMain {
            updateState { state ->
                val next = state.emotionCategories.map { old ->
                    if (old.key == category.key) category else old
                }
                state.copy(emotionCategories = next)
            }
        }
    }

    private fun updateEmotionItem(item: AioEmotionItem) {
        runOnMain {
            updateState { state ->
                val nextCategories = state.emotionCategories.map { category ->
                    var changed = false
                    val nextItems = category.items.map { current ->
                        if (current.key == item.key) {
                            changed = true
                            item
                        } else {
                            current
                        }
                    }
                    if (changed) category.copy(items = nextItems) else category
                }
                state.copy(emotionCategories = nextCategories)
            }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            AioRuntimeStore.mainHandler.post(block)
        }
    }

    private fun applyMessages(rows: List<AioMessage>, replace: Boolean) {
        val nextByKey = LinkedHashMap<String, AioMessage>()
        if (!replace) {
            currentState.messages.forEach { nextByKey[it.key] = it }
        }
        rows.forEach { message ->
            rememberMessageView(message)
            nextByKey[message.key] = message.copy(itemViewRef = null)
        }

        val nextRows = nextByKey.values
            .toList()
            .dedupeSemanticMessages()
            .sortedOldestFirst()
            .takeLast(MAX_RENDERED_MESSAGES)
        val visibleKeys = nextRows.mapTo(HashSet()) { it.key }
        messageViewRefs.keys.retainAll(visibleKeys)
        updateState { state ->
            if (!state.loading && !state.loadingOlder && state.messages.sameMessageUi(nextRows)) {
                state
            } else {
                state.copy(messages = nextRows, loading = false, loadingOlder = false)
            }
        }
        publishMessages(nextRows)
        triggerPendingVoiceAutoPlay(nextRows)
        schedulePendingMediaRefresh(nextRows)
        requestMissingMemberInfo(nextRows)
    }

    private fun flushMessages() {
        if (pendingMessages.isEmpty()) return
        val nextByKey = LinkedHashMap<String, AioMessage>()
        currentState.messages.forEach { nextByKey[it.key] = it }
        pendingMessages.forEach { (key, message) -> nextByKey[key] = message }
        pendingMessages.clear()

        val nextRows = nextByKey.values
            .toList()
            .dedupeSemanticMessages()
            .sortedOldestFirst()
            .takeLast(MAX_RENDERED_MESSAGES)
        val visibleKeys = nextRows.mapTo(HashSet()) { it.key }
        messageViewRefs.keys.retainAll(visibleKeys)
        updateState { state ->
            if (!state.loading && !state.loadingOlder && state.messages.sameMessageUi(nextRows)) {
                state
            } else {
                state.copy(messages = nextRows, loading = false, loadingOlder = false)
            }
        }
        publishMessages(nextRows)
        triggerPendingVoiceAutoPlay(nextRows)
        schedulePendingMediaRefresh(nextRows)
        requestMissingMemberInfo(nextRows)
    }

    private fun requestMissingMemberInfo(rows: List<AioMessage>) {
        val peer = currentState.peer
        if (peer.chatType != GROUP_CHAT_TYPE) return
        val groupCode = peer.chatUin.takeIf { it > 0L } ?: peer.peerId.toLongOrNull() ?: return
        val missingUids = rows.asSequence()
            .filter { it.badges.isEmpty() }
            .map { it.senderUid }
            .filter { it.isNotBlank() && pendingMemberInfoUids.add(it) }
            .toList()
        if (missingUids.isEmpty()) return
        val requested = hookState.requestGroupMemberInfo(groupCode, missingUids) { infos ->
            pendingMemberInfoUids.removeAll(infos.keys)
            AioRuntimeStore.mergeMemberInfoCache(infos)
            val hostRows = AioRuntimeStore.latestListUiRows
            if (!hostRows.isNullOrEmpty()) {
                upsertMessagesFromHost(hostRows)
            }
        }
        if (!requested) {
            pendingMemberInfoUids.removeAll(missingUids.toSet())
        }
    }

    private fun publishMessages(rows: List<AioMessage>) {
        _pagingMessages.value = PagingData.from(rows)
    }

    private fun schedulePendingMediaRefresh(rows: List<AioMessage>) {
        if (rows.none { it.needsMediaRefreshPoll() }) return
        val sequence = ++mediaRefreshSequence
        repeat(20) { index ->
            AioRuntimeStore.mainHandler.postDelayed({
                if (mediaRefreshSequence != sequence) return@postDelayed
                updateState { state ->
                    state.copy(renderRevision = state.renderRevision + 1L)
                }
            }, 500L * (index + 1))
        }
    }

    private fun triggerPendingVoiceAutoPlay(rows: List<AioMessage>) {
        if (pendingVoiceAutoPlayKeys.isEmpty()) return
        val readyMessages = rows.filter { message ->
            message.key in pendingVoiceAutoPlayKeys && message.renderKind == AioMessageKind.Voice &&
                message.isVoicePlayable()
        }
        if (readyMessages.isEmpty()) return
        readyMessages.forEach { message ->
            pendingVoiceAutoPlayKeys.remove(message.key)
            val itemView = messageViewRefs[message.key]?.get() ?: return@forEach
            AioRuntimeStore.mainHandler.postDelayed({
                itemView.performBoundVoiceClick()
            }, 120L)
        }
    }

    private fun rememberMessageView(message: AioMessage) {
        val view = message.itemViewRef?.get() ?: return
        if (messageViewRefs[message.key]?.get() === view) return
        messageViewRefs[message.key] = WeakReference(view)
        scheduleRenderRefresh()
    }

    private fun scheduleRenderRefresh() {
        if (renderRefreshScheduled) return
        renderRefreshScheduled = true
        AioRuntimeStore.mainHandler.postDelayed({
            renderRefreshScheduled = false
            updateState { state ->
                state.copy(renderRevision = state.renderRevision + 1L)
            }
        }, RENDER_REFRESH_DELAY_MS)
    }

    private fun View?.moveToPreviousAIOFrame(): Boolean {
        if (this == null) return false
        val currentItem = readCurrentViewPagerItem() ?: return false
        if (currentItem <= 0) return false
        setViewPagerCurrentItem(currentItem - 1, true)
        return true
    }

    private fun Any.popWatchFragment(): Boolean {
        val popMethod = javaClass.methods.firstOrNull {
            it.name == "pop" && it.parameterTypes.isEmpty()
        } ?: return false
        popMethod.invoke(this)
        return true
    }

    private companion object {
        const val MAX_RENDERED_MESSAGES = 300
        const val MESSAGE_FLUSH_DELAY_MS = 48L
        const val RENDER_REFRESH_DELAY_MS = 80L
        const val LOAD_OLDER_TIMEOUT_MS = 3000L
        const val RELEASE_AIO_AFTER_BACK_DELAY_MS = 120L
        const val GROUP_CHAT_TYPE = 2
        const val EMOTION_CATEGORY_FAVORITE = "favorite"
        const val EMOTION_CATEGORY_SYSTEM = "system"
        const val EMOTION_CATEGORY_BIG_SYSTEM = "big-system"
        const val EMOTION_CATEGORY_HOT = "hot"
    }
}

private fun AioMessage.isVoicePlayable(): Boolean {
    if (renderKind != AioMessageKind.Voice) return false
    val path = media?.localPath?.trim().orEmpty()
    if (path.isBlank()) return false
    return path.startsWith("content://") ||
        path.startsWith("file://") ||
        File(path).exists()
}

private fun AioEmotionItem.hasUsablePreviewPath(): Boolean {
    val path = localPath?.trim()?.takeIf { it.isNotBlank() }
        ?: resultPath?.trim()?.takeIf { it.isNotBlank() }
        ?: return false
    return path.startsWith("content://") ||
        path.startsWith("file://") ||
        File(path).isFile
}

private fun AioMessage.needsMediaRefreshPoll(): Boolean {
    if (renderKind != AioMessageKind.Image &&
        renderKind != AioMessageKind.Video &&
        renderKind != AioMessageKind.Giphy
    ) {
        return false
    }
    val media = media ?: return false
    val previewPath = media.previewPath?.trim().orEmpty()
    val playbackPath = media.playbackPath?.trim().orEmpty()
    val localPath = media.localPath?.trim().orEmpty()
    if (previewPath.isBlank() && playbackPath.isBlank() && localPath.isBlank()) {
        return false
    }
    return !media.hasUsablePreviewPath()
}

private fun AioMessage.needsHostBinding(): Boolean {
    return renderKind == AioMessageKind.Image ||
        renderKind == AioMessageKind.Video ||
        renderKind == AioMessageKind.Giphy ||
        rawKind == AioMessageKind.FaceBubble
}

private fun me.padi.qqlite.revived.shared.model.aio.AioMediaSpec.hasUsablePreviewPath(): Boolean {
    return previewPath.isUsableMediaPath() ||
        playbackPath.isUsableMediaPath() ||
        localPath.isUsableMediaPath()
}

private fun String?.isUsableMediaPath(): Boolean {
    val path = this?.trim()?.takeIf { it.isNotBlank() } ?: return false
    return path.startsWith("content://") ||
        path.startsWith("file://") ||
        File(path).exists()
}

private fun View.scrollHostListToPosition(position: Int) {
    if (position < 0) return
    (this as? RecyclerView)?.scrollToPosition(position)
        ?: runCatching {
            javaClass.methods.firstOrNull { method ->
                method.name == "scrollToPosition" &&
                    method.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
            }?.invoke(this, position)
        }
}

private fun View.findHostMediaSource(): View? {
    findFirstDescendantByClassName(PIC_GROUP_WIDGET_CLASS)
        ?.invokeHostMediaGetter(PIC_VIEW_GETTER)
        ?.let { return it }
    findFirstDescendantByClassName(VIDEO_GROUP_WIDGET_CLASS)
        ?.invokeHostMediaGetter(VIDEO_COVER_GETTER)
        ?.let { return it }
    findFirstDescendantByClassName(FACE_BUBBLE_GROUP_WIDGET_CLASS)
        ?.invokeHostMediaGetter(FACE_EMO_GETTER)
        ?.let { return it }
    findFirstDescendantByClassName(MARKET_FACE_GROUP_WIDGET_CLASS)
        ?.invokeHostMediaGetter(CONTENT_WIDGET_GETTER)
        ?.findLargestHostMediaCandidate()
        ?.let { return it }

    return findLargestHostMediaCandidate()
}

private fun View.findLargestHostMediaCandidate(): View? {
    var bestView: View? = null
    var bestArea = 0
    forEachDescendant { view ->
        val width = view.width.takeIf { it > 0 } ?: view.measuredWidth
        val height = view.height.takeIf { it > 0 } ?: view.measuredHeight
        val area = width * height
        if (area > bestArea && view.isLikelyHostMediaView()) {
            bestView = view
            bestArea = area
        }
        false
    }
    return bestView
}

private fun View.invokeHostMediaGetter(name: String): View? {
    return runCatching { invokeNoArg(name) as? View }.getOrNull()
}

private fun View.isLikelyHostMediaView(): Boolean {
    if (this is ImageView) return width > 12 && height > 12
    val name = javaClass.name.lowercase()
    return width > 12 && height > 12 &&
        (name.contains("image") ||
            name.contains("pic") ||
            name.contains("gif") ||
            name.contains("video") ||
            name.contains("texture") ||
            name.contains("player"))
}

private fun View?.performBestClick(): HostClickResult {
    if (this == null) return HostClickResult(false, "null")
    if (performClick()) return HostClickResult(true, clickTargetName())
    val target = findLargestClickableDescendant()
    return HostClickResult(target?.performClick() == true, target.clickTargetName())
}

private fun View?.performBoundVoiceClick(): HostClickResult {
    if (this == null) return HostClickResult(false, "null")
    val pttGroup = findFirstDescendantByClassName(PTT_GROUP_WIDGET_CLASS) ?: return HostClickResult(
        false,
        "null"
    )
    pttGroup.findFirstDescendantByClassName(PTT_WAVE_VIEW_CLASS)?.let { target ->
        val result = target.invokeBoundClickListener(fallbackToPerformClick = false)
        if (result.clicked) return result
    }
    pttGroup.findFirstClickableDescendantBySimpleName("LinearLayout")?.let { target ->
        val result = target.invokeBoundClickListener(fallbackToPerformClick = false)
        if (result.clicked) return result
    }
    return HostClickResult(false, pttGroup.clickTargetName())
}

private fun View?.performBoundMediaClick(): HostClickResult {
    if (this == null) return HostClickResult(false, "null")
    val mediaView = findHostMediaSource() ?: return HostClickResult(false, "null")
    val directResult = mediaView.invokeBoundClickListener(fallbackToPerformClick = true)
    if (directResult.clicked) return directResult
    generateSequence(mediaView.parent) { parent -> (parent as? View)?.parent }
        .mapNotNull { it as? View }
        .forEach { parentView ->
            val result = parentView.invokeBoundClickListener(fallbackToPerformClick = false)
            if (result.clicked) {
                return result
            }
        }
    return directResult
}

private fun View?.performBestAvatarClick(): HostClickResult {
    if (this == null) return HostClickResult(false, "null")
    val target = findPreferredClickableDescendant(
        preferredClassNames = listOf(
            HOST_AVATAR_VIEW_CLASS.lowercase(),
            "avatar",
            "head",
            "profile"
        )
    ) ?: findFirstDescendantByClassName(HOST_AVATAR_VIEW_CLASS)
    return target.invokeBoundClickListener()
}

private fun View?.performBestLongClick(): HostClickResult {
    if (this == null) return HostClickResult(false, "null")
    if (performLongClick()) return HostClickResult(true, clickTargetName())
    val target = findLargestLongClickableDescendant()
    return HostClickResult(target?.performLongClick() == true, target.clickTargetName())
}

private fun View.findLargestClickableDescendant(): View? {
    var bestView: View? = null
    var bestArea = 0
    forEachDescendant { view ->
        if (view !== this && view.isVisible && view.isEnabled &&
            (view.hasOnClickListeners() || view.isClickable)
        ) {
            val area = view.touchArea()
            if (area >= bestArea) {
                bestView = view
                bestArea = area
            }
        }
        false
    }
    return bestView
}

private fun View.findPreferredClickableDescendant(preferredClassNames: List<String>): View? {
    val normalized = preferredClassNames.map { it.lowercase() }
    var bestView: View? = null
    var bestScore = Int.MIN_VALUE
    var bestArea = Int.MIN_VALUE
    forEachDescendant { view ->
        if (view !== this && view.isVisible && view.isEnabled &&
            (view.hasOnClickListeners() || view.isClickable)
        ) {
            val className = view.javaClass.name.lowercase()
            val score = normalized.indexOfFirst { token -> className.contains(token) }
                .let { index -> if (index >= 0) normalized.size - index else Int.MIN_VALUE }
            if (score > bestScore || (score == bestScore && view.touchArea() > bestArea)) {
                bestView = view
                bestScore = score
                bestArea = view.touchArea()
            }
        }
        false
    }
    return bestView?.takeIf { bestScore > Int.MIN_VALUE }
}

private fun View.findFirstClickableDescendantBySimpleName(simpleName: String): View? {
    var matched: View? = null
    forEachDescendant { view ->
        if (view !== this &&
            view.isVisible &&
            view.isEnabled &&
            (view.hasOnClickListeners() || view.isClickable) &&
            view.javaClass.simpleName == simpleName
        ) {
            matched = view
            true
        } else {
            false
        }
    }
    return matched
}

private fun View.findLargestLongClickableDescendant(): View? {
    var bestView: View? = null
    var bestArea = 0
    forEachDescendant { view ->
        if (view !== this && view.isVisible && view.isEnabled && view.isLongClickable) {
            val area = view.touchArea()
            if (area >= bestArea) {
                bestView = view
                bestArea = area
            }
        }
        false
    }
    return bestView
}

private fun View.touchArea(): Int {
    val resolvedWidth = width.takeIf { it > 0 } ?: measuredWidth
    val resolvedHeight = height.takeIf { it > 0 } ?: measuredHeight
    return resolvedWidth * resolvedHeight
}

private fun View?.invokeBoundClickListener(fallbackToPerformClick: Boolean = true): HostClickResult {
    if (this == null) return HostClickResult(false, "null")
    val listener = readBoundClickListener()
    if (listener != null) {
        runCatching {
            listener.javaClass.getMethod("onClick", View::class.java).invoke(listener, this)
        }.onSuccess {
            return HostClickResult(true, clickTargetName())
        }
    }
    return HostClickResult(if (fallbackToPerformClick) performClick() else false, clickTargetName())
}

private fun View.readBoundClickListener(): Any? {
    val listenerInfo = runCatching {
        View::class.java.getDeclaredMethod("getListenerInfo").apply { isAccessible = true }.invoke(this)
    }.getOrNull() ?: return null
    return runCatching {
        listenerInfo.javaClass.getDeclaredField("mOnClickListener")
            .apply { isAccessible = true }
            .get(listenerInfo)
    }.getOrNull()
}

private fun View?.clickTargetName(): String {
    if (this == null) return "null"
    return "${javaClass.simpleName}{clickable=$isClickable longClickable=$isLongClickable size=${width}x$height}"
}

private data class HostClickResult(
    val clicked: Boolean,
    val target: String
)

private fun List<AioMessage>.dedupeSemanticMessages(): List<AioMessage> {
    val deduped = LinkedHashMap<String, AioMessage>()
    for (message in this) {
        val semanticKey = message.semanticIdentityKey()
        val existing = deduped[semanticKey]
        deduped[semanticKey] = if (existing == null) {
            message
        } else {
            existing.preferMoreComplete(message)
        }
    }
    return deduped.values.toList()
}

private fun AioMessage.semanticIdentityKey(): String {
    return when {
        msgId > 0L -> "id:$msgId"
        msgSeq > 0L && msgRandom > 0L -> "seqrand:$senderUin:$msgSeq:$msgRandom"
        msgSeq > 0L -> "seq:$senderUin:$msgSeq"
        else -> buildString {
            append("fallback:")
            append(senderUin)
            append(':')
            append(senderUid)
            append(':')
            append(msgTime)
            append(':')
            append(renderKind.name)
            append(':')
            append(text)
            append(':')
            append(media?.fileName.orEmpty())
            append(':')
            append(media?.playbackPath.orEmpty())
            append(':')
            append(media?.previewPath.orEmpty())
            append(':')
            append(media?.localPath.orEmpty())
        }
    }
}

private fun AioMessage.preferMoreComplete(other: AioMessage): AioMessage {
    val currentScore = completenessScore()
    val otherScore = other.completenessScore()
    return when {
        otherScore > currentScore -> other
        otherScore < currentScore -> this
        other.sortTime > sortTime -> other
        other.msgId > msgId -> other
        else -> this
    }
}

private fun AioMessage.completenessScore(): Int {
    var score = 0
    if (text.isNotBlank()) score += 1
    if (media != null) score += 2
    if (!media?.localPath.isNullOrBlank()) score += 2
    if (!media?.previewPath.isNullOrBlank()) score += 2
    if (!media?.playbackPath.isNullOrBlank()) score += 2
    if (!media?.remoteUrl.isNullOrBlank()) score += 1
    if (avatar != null) score += 1
    if (rawKind != AioMessageKind.Unsupported) score += 1
    if (senderName.isNotBlank()) score += 1
    if (badges.isNotEmpty()) score += 2
    if (showTimeDivider) score += 1
    if (timeDividerText.isNotBlank()) score += 1
    return score
}

private const val PIC_GROUP_WIDGET_CLASS =
    "com.tencent.watch.aio_impl.ui.cell.pic.WatchPicGroupWidget"
private const val VIDEO_GROUP_WIDGET_CLASS =
    "com.tencent.watch.aio_impl.ui.cell.video.WatchVideoGroupWidget"
private const val PTT_GROUP_WIDGET_CLASS =
    "com.tencent.watch.aio_impl.ui.cell.ptt.WatchPttCellGroupWidget"
private const val PTT_WAVE_VIEW_CLASS =
    "com.tencent.watch.aio_impl.ui.cell.ptt.PttAudioWaveView"
private const val HOST_AVATAR_VIEW_CLASS =
    "com.tencent.qqnt.avatar.WatchAvatarView"
private const val FACE_BUBBLE_GROUP_WIDGET_CLASS =
    "com.tencent.watch.aio_impl.ui.cell.facebubble.bubble.WatchFaceBubbleGroupWidget"
private const val MARKET_FACE_GROUP_WIDGET_CLASS =
    "com.tencent.watch.aio_impl.ui.cell.marketface.WatchMarketFaceGroupWidget"
private const val PIC_VIEW_GETTER = "getPicView"
private const val VIDEO_COVER_GETTER = "getCoverImage\$aio_impl_release"
private const val FACE_EMO_GETTER = "getEmoIv\$aio_impl_release"
private const val CONTENT_WIDGET_GETTER = "getContentWidget"

private fun List<AioMessage>.sameMessageUi(other: List<AioMessage>): Boolean {
    if (size != other.size) return false
    return indices.all { index -> this[index].sameUi(other[index]) }
}

private fun AioMessage.sameUi(other: AioMessage): Boolean {
    return key == other.key &&
        msgId == other.msgId &&
        msgSeq == other.msgSeq &&
        msgRandom == other.msgRandom &&
        msgTime == other.msgTime &&
        senderUid == other.senderUid &&
        senderUin == other.senderUin &&
        isSelf == other.isSelf &&
        renderKind == other.renderKind &&
        rawKind == other.rawKind &&
        text == other.text &&
        senderName == other.senderName &&
        badges == other.badges &&
        showTimeDivider == other.showTimeDivider &&
        timeDividerText == other.timeDividerText &&
        media == other.media &&
        avatar?.stableKey() == other.avatar?.stableKey()
}
