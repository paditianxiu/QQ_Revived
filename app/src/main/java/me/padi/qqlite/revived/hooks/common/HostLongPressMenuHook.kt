package me.padi.qqlite.revived.hooks.common

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import me.padi.qqlite.revived.ModuleMainKt
import me.padi.qqlite.revived.hooks.aio.AioBinding
import me.padi.qqlite.revived.hooks.aio.AioRuntimeStore
import me.padi.qqlite.revived.shared.model.aio.AioLongPressMenuItem
import me.padi.qqlite.revived.utils.addHostComposeView
import me.padi.qqlite.revived.utils.removeHostComposeView
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.window.WindowListPopup
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

internal object HostLongPressMenuHook : BaseHook() {
    private var touchHookInstalled = false
    private var cellLongClickHookInstalled = false
    private var fragmentHookInstalled = false

    override fun reset() {
        touchHookInstalled = false
        cellLongClickHookInstalled = false
        fragmentHookInstalled = false
        HostLongPressMenuRuntime.reset()
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        val targetClassLoader = requireClassLoader(classLoader)
        hookTouchTracking(module)
        hookWatchAioCellLongClick(module, targetClassLoader)
        hookHostLongPressFragment(module, targetClassLoader)
    }

    private fun hookTouchTracking(module: ModuleMainKt) {
        if (touchHookInstalled) return
        runCatching {
            module.intercept(
                Activity::class.java.getMethod("dispatchTouchEvent", MotionEvent::class.java)
            ) {
                val activity = thisObject as? Activity
                val event = args.firstOrNull() as? MotionEvent
                if (activity != null && event != null) {
                    HostLongPressMenuRuntime.rememberTouch(activity, event)
                }
                proceed()
            }
            touchHookInstalled = true
        }.onFailure {
            module.logHook(android.util.Log.WARN, "Host long press touch hook skipped", it)
        }
    }

    private fun hookWatchAioCellLongClick(module: ModuleMainKt, classLoader: ClassLoader) {
        if (cellLongClickHookInstalled) return
        runCatching {
            val cellClass =
                classLoader.loadClass("com.tencent.watch.aio_impl.ui.cell.base.WatchAIOGroupWidgetItemCell")
            module.intercept(
                cellClass.getDeclaredMethod("onLongClick", View::class.java)
            ) {
                val cell = thisObject ?: return@intercept proceed()
                val view = args.firstOrNull() as? View ?: return@intercept proceed()
                val fragment = resolveHostFragment(view) ?: return@intercept proceed()
                val activity = fragment.requireActivity()
                val contentRoot =
                    activity.findViewById<ViewGroup>(android.R.id.content) ?: return@intercept proceed()
                val itemEnums = runCatching {
                    @Suppress("UNCHECKED_CAST")
                    cell.javaClass.getMethod("m").invoke(cell) as? List<Any?>
                }.getOrNull()
                    ?.filterNotNull()
                    ?.distinctBy { hostMenuEnumName(it) }
                    ?: return@intercept proceed()
                if (itemEnums.isEmpty()) {
                    return@intercept true
                }
                val items = itemEnums.mapNotNull { enumValue ->
                    val label = resolveHostMenuLabel(activity, enumValue) ?: return@mapNotNull null
                    HostLongPressMenuItem(label) {
                        runCatching {
                            cell.javaClass.getMethod(
                                "l",
                                enumValue.javaClass,
                                Fragment::class.java
                            ).invoke(cell, enumValue, fragment)
                        }
                    }
                }
                if (items.isEmpty()) {
                    return@intercept true
                }
                val aioBinding = resolveAioBinding(fragment, view)
                val anchor = aioBinding?.consumePendingLongPressAnchor()
                    ?: HostLongPressMenuRuntime.resolveAnchor(activity, view)
                if (aioBinding != null && AioRuntimeStore.hasActiveAioSurface()) {
                    aioBinding.showLongPressMenu(
                        anchor = anchor,
                        items = items.map { AioLongPressMenuItem(label = it.label) },
                        actions = items.map { it.invoke }
                    )
                } else {
                    contentRoot.showHostLongPressPopup(
                        items = items,
                        anchor = anchor
                    )
                }
                HostLongPressMenuRuntime.markPopupShown()
                true
            }
            cellLongClickHookInstalled = true
        }.onFailure {
            module.logHook(android.util.Log.WARN, "WatchAIO long click hook skipped", it)
        }
    }

    private fun hookHostLongPressFragment(module: ModuleMainKt, classLoader: ClassLoader) {
        if (fragmentHookInstalled) return
        runCatching {
            val fragmentClass =
                classLoader.loadClass("com.tencent.watch.aio_impl.ui.menu.AIOLongClickMenuFragment")
            module.intercept(
                fragmentClass.getDeclaredMethod(
                    "onCreateView",
                    LayoutInflater::class.java,
                    ViewGroup::class.java,
                    Bundle::class.java
                )
            ) {
                val result = proceed()
                val root = result as? View ?: return@intercept result
                runCatching {
                    val fragment = thisObject ?: return@runCatching
                    val activity = root.context.findActivity() ?: return@runCatching
                    val contentRoot =
                        activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runCatching
                    if (HostLongPressMenuRuntime.consumePopupShownFlag()) {
                        prepareHostFragmentForIntercept(fragment, root)
                        dismissFragmentAllowingStateLoss(fragment)
                        return@runCatching
                    }
                    val items = root.extractHostMenuItems()
                    if (items.isNotEmpty()) {
                        prepareHostFragmentForIntercept(fragment, root)
                        val aioBinding = resolveAioBinding(fragment as? Fragment, root)
                        val anchor = aioBinding?.consumePendingLongPressAnchor()
                            ?: HostLongPressMenuRuntime.resolveAnchor(activity, root)
                        if (aioBinding != null && AioRuntimeStore.hasActiveAioSurface()) {
                            aioBinding.showLongPressMenu(
                                anchor = anchor,
                                items = items.map { AioLongPressMenuItem(label = it.label) },
                                actions = items.map { it.invoke }
                            )
                        } else {
                            contentRoot.showHostLongPressPopup(
                                items = items,
                                anchor = anchor
                            )
                        }
                        HostLongPressMenuRuntime.markPopupShown()
                        dismissFragmentAllowingStateLoss(fragment)
                    }
                }
                result
            }
            fragmentHookInstalled = true
        }.onFailure {
            module.logHook(android.util.Log.WARN, "Host long press fragment hook skipped", it)
        }
    }
}

private fun ViewGroup.showHostLongPressPopup(
    items: List<HostLongPressMenuItem>,
    anchor: IntOffset
) {
    removeHostComposeView(HOST_LONG_PRESS_POPUP_TAG, HOST_LONG_PRESS_POPUP_BINDING_KEY)
    addHostComposeView(
        tag = HOST_LONG_PRESS_POPUP_TAG,
        bindingKey = HOST_LONG_PRESS_POPUP_BINDING_KEY,
        layoutParamsFactory = {
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        },
        lifecycleAnchor = this,
        useWindowLayer = false,
        wrapInContainer = false,
        configure = {
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            alpha = 1f
        }
    ) {
        HostLongPressPopup(
            anchor = anchor.toWindowOffset(this),
            items = items,
            onDismissRequest = {
                removeHostComposeView(HOST_LONG_PRESS_POPUP_TAG, HOST_LONG_PRESS_POPUP_BINDING_KEY)
            }
        )
    }
}

@Composable
private fun HostLongPressPopup(
    anchor: IntOffset,
    items: List<HostLongPressMenuItem>,
    onDismissRequest: () -> Unit
) {
    MiuixTheme(
        controller = remember { ThemeController(colorSchemeMode = ColorSchemeMode.System) }
    ) {
        val popupPositionProvider = remember(anchor) {
            touchAnchorPositionProvider(anchor)
        }
        Box(modifier = Modifier.fillMaxSize()) {
            WindowListPopup(
                show = true,
                popupPositionProvider = popupPositionProvider,
                alignment = PopupPositionProvider.Align.TopStart,
                enableWindowDim = true,
                onDismissRequest = onDismissRequest
            ) {
                ListPopupColumn {
                    items.forEachIndexed { index, item ->
                        DropdownImpl(
                            item = DropdownItem(text = item.label),
                            optionSize = items.size,
                            isSelected = false,
                            index = index,
                            onSelectedIndexChange = {
                                item.invoke()
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun IntOffset.toWindowOffset(anchorView: View): IntOffset {
    val locationOnScreen = IntArray(2)
    anchorView.getLocationOnScreen(locationOnScreen)
    return IntOffset(
        x = x - locationOnScreen[0],
        y = y - locationOnScreen[1]
    )
}

private fun touchAnchorPositionProvider(anchor: IntOffset): PopupPositionProvider {
    return object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowBounds: IntRect,
            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
            popupContentSize: IntSize,
            popupMargin: IntRect,
            alignment: PopupPositionProvider.Align,
        ): IntOffset {
            val rawX = anchor.x + popupMargin.left
            val rawY = anchor.y + popupMargin.top
            return IntOffset(
                x = rawX.coerceIn(
                    windowBounds.left,
                    (windowBounds.right - popupContentSize.width - popupMargin.right)
                        .coerceAtLeast(windowBounds.left)
                ),
                y = rawY.coerceIn(
                    (windowBounds.top + popupMargin.top)
                        .coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                    windowBounds.bottom - popupContentSize.height - popupMargin.bottom
                )
            )
        }

        override fun getMargins() = ListPopupDefaults.ContextMenuPositionProvider.getMargins()
    }
}

private fun resolveHostFragment(view: View): Fragment? {
    return runCatching {
        val extClass = view.javaClass.classLoader?.loadClass("defpackage.WatchPicElementExtKt")
            ?: return null
        extClass.methods.firstOrNull {
            it.name == "W" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == View::class.java
        }?.invoke(null, view) as? Fragment
    }.getOrNull()
}

private fun resolveAioBinding(fragment: Fragment?, view: View): AioBinding? {
    return fragment?.let(AioRuntimeStore::findBindingForFragment)
        ?: AioRuntimeStore.findBindingForView(view)
        ?: AioRuntimeStore.latestBinding?.get()
}

private fun hostMenuEnumName(value: Any): String {
    return (value as? Enum<*>)?.name ?: value.toString()
}

private fun resolveHostMenuLabel(activity: Activity, enumValue: Any): String? {
    val resourceName = when (hostMenuEnumName(enumValue)) {
        "AddFriend" -> "aio_add_friend"
        "ToChat" -> "aio_go_chat"
        "TranslateText" -> "aio_ptt_translate_text"
        "HideTranslateText" -> "hide_ptt_translate_text"
        "SpeakText" -> "aio_tts_speak"
        "SaveFavEmoji" -> "favorite"
        "Share" -> "share"
        "SavePic" -> "save_pic"
        "DeleteMsg" -> "delete"
        "RevokeMsg" -> "msg_revoke"
        else -> null
    } ?: return null
    val resId = activity.resources.getIdentifier(resourceName, "string", activity.packageName)
    return if (resId != 0) activity.getString(resId) else null
}

private fun prepareHostFragmentForIntercept(fragment: Any, root: View) {
    root.alpha = 0f
    root.visibility = View.INVISIBLE
    root.isClickable = false
    root.isLongClickable = false
    resolveHostDialog(fragment)?.window?.let { window ->
        window.setDimAmount(0f)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.alpha = 0f
        window.decorView.visibility = View.INVISIBLE
    }
}

private fun resolveHostDialog(fragment: Any): Dialog? {
    return runCatching {
        fragment.javaClass.methods.firstOrNull {
            it.name == "getDialog" && it.parameterTypes.isEmpty()
        }?.invoke(fragment) as? Dialog
    }.getOrNull()
}

private fun dismissFragmentAllowingStateLoss(fragment: Any) {
    runCatching {
        fragment.javaClass.methods.firstOrNull {
            it.name == "dismissAllowingStateLoss" && it.parameterTypes.isEmpty()
        }?.invoke(fragment)
    }.recoverCatching {
        fragment.javaClass.methods.firstOrNull {
            it.name == "dismiss" && it.parameterTypes.isEmpty()
        }?.invoke(fragment)
    }
}

private fun View.extractHostMenuItems(): List<HostLongPressMenuItem> {
    val candidates = ArrayList<HostLongPressMenuCandidate>()
    collectHostMenuCandidates(depth = 0, out = candidates)
    return candidates
        .sortedWith(compareBy<HostLongPressMenuCandidate>({ it.top }, { it.left }, { -it.score }))
        .distinctBy { it.label }
        .map { candidate ->
            HostLongPressMenuItem(candidate.label) {
                candidate.target.invokeBoundClickListener()
            }
        }
}

private fun View.collectHostMenuCandidates(
    depth: Int,
    out: MutableList<HostLongPressMenuCandidate>
) {
    val label = readMenuLabel()
    if (!label.isNullOrBlank()) {
        val target = bestMenuClickTarget()
        if (target != null) {
            val location = IntArray(2)
            target.getLocationOnScreen(location)
            out += HostLongPressMenuCandidate(
                label = label,
                target = target,
                top = location[1],
                left = location[0],
                score = target.menuTargetScore(depth)
            )
        }
    }
    if (this !is ViewGroup) return
    for (index in 0 until childCount) {
        getChildAt(index).collectHostMenuCandidates(depth + 1, out)
    }
}

private fun View.readMenuLabel(): String? {
    if (visibility != View.VISIBLE) return null
    if (this is TextView) {
        return text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }
    if (this !is ViewGroup) return null
    var bestLabel: String? = null
    forEachDescendant {
        if (it is TextView) {
            val value = it.text?.toString()?.trim().orEmpty()
            if (value.isNotEmpty()) {
                bestLabel = value
                true
            } else {
                false
            }
        } else {
            false
        }
    }
    return bestLabel
}

private fun View.bestMenuClickTarget(): View? {
    if (isVisibleMenuTarget()) return this
    var best: View? = null
    var bestScore = Int.MIN_VALUE
    forEachDescendant { child ->
        if (child.isVisibleMenuTarget()) {
            val score = child.menuTargetScore(Int.MAX_VALUE)
            if (score > bestScore) {
                best = child
                bestScore = score
            }
        }
        false
    }
    return best
}

private fun View.menuTargetScore(depth: Int): Int {
    val listenerScore = if (readBoundClickListener() != null) 10_000 else 0
    val clickableScore = if (hasOnClickListeners() || isClickable) 1_000 else 0
    val sizeScore =
        (width.takeIf { it > 0 } ?: measuredWidth) * (height.takeIf { it > 0 } ?: measuredHeight)
    return listenerScore + clickableScore + sizeScore - depth
}

private fun View.isVisibleMenuTarget(): Boolean {
    return visibility == View.VISIBLE &&
        isEnabled &&
        (hasOnClickListeners() || isClickable)
}

private fun View.forEachDescendant(visitor: (View) -> Boolean): Boolean {
    if (visitor(this)) return true
    if (this !is ViewGroup) return false
    for (index in 0 until childCount) {
        if (getChildAt(index).forEachDescendant(visitor)) {
            return true
        }
    }
    return false
}

private fun View.invokeBoundClickListener() {
    val listener = readBoundClickListener()
    if (listener != null) {
        runCatching {
            listener.javaClass.getMethod("onClick", View::class.java).invoke(listener, this)
        }.onSuccess {
            return
        }
    }
    performClick()
}

private fun View.readBoundClickListener(): Any? {
    val listenerInfo = runCatching {
        View::class.java.getDeclaredMethod("getListenerInfo")
            .apply { isAccessible = true }
            .invoke(this)
    }.getOrNull() ?: return null
    return runCatching {
        listenerInfo.javaClass.getDeclaredField("mOnClickListener")
            .apply { isAccessible = true }
            .get(listenerInfo)
    }.getOrNull()
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private data class HostLongPressMenuItem(
    val label: String,
    val invoke: () -> Unit
)

private data class HostLongPressMenuCandidate(
    val label: String,
    val target: View,
    val top: Int,
    val left: Int,
    val score: Int
)

private object HostLongPressMenuRuntime {
    private var lastActivityRef: WeakReference<Activity>? = null
    private var lastTouchPoint: IntOffset? = null
    private var lastTouchUptimeMillis: Long = 0L
    private var popupShown: Boolean = false

    fun reset() {
        lastActivityRef = null
        lastTouchPoint = null
        lastTouchUptimeMillis = 0L
        popupShown = false
    }

    fun rememberTouch(activity: Activity, event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) {
            return
        }
        lastActivityRef = WeakReference(activity)
        lastTouchPoint = IntOffset(event.rawX.roundToInt(), event.rawY.roundToInt())
        lastTouchUptimeMillis = event.eventTime
    }

    fun resolveAnchor(activity: Activity, fallbackView: View): IntOffset {
        val point = lastTouchPoint
        if (lastActivityRef?.get() === activity &&
            point != null &&
            android.os.SystemClock.uptimeMillis() - lastTouchUptimeMillis <= TOUCH_POINT_VALID_MS &&
            fallbackView.containsRawPoint(point)
        ) {
            return point
        }
        val location = IntArray(2)
        fallbackView.getLocationOnScreen(location)
        return IntOffset(
            x = location[0] + fallbackView.width / 2,
            y = location[1] + fallbackView.height / 2
        )
    }

    fun markPopupShown() {
        popupShown = true
    }

    fun consumePopupShownFlag(): Boolean {
        val value = popupShown
        popupShown = false
        return value
    }

    private const val TOUCH_POINT_VALID_MS = 3_000L
}

private fun View.containsRawPoint(point: IntOffset): Boolean {
    val location = IntArray(2)
    getLocationOnScreen(location)
    val left = location[0]
    val top = location[1]
    val right = left + width.takeIf { it > 0 }!!.coerceAtLeast(measuredWidth)
    val bottom = top + height.takeIf { it > 0 }!!.coerceAtLeast(measuredHeight)
    return point.x in left..right && point.y in top..bottom
}

private const val HOST_LONG_PRESS_POPUP_TAG = "QQRevived.HostLongPressMenu.Popup"
private const val HOST_LONG_PRESS_POPUP_BINDING_KEY = 0x51524160
