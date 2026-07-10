package me.padi.qqlite.revived.hooks.aio

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import me.padi.qqlite.revived.utils.removeHostComposeView
import me.padi.qqlite.revived.shared.model.aio.AioMessage
import me.padi.qqlite.revived.shared.model.aio.AioUiState
import java.lang.ref.WeakReference
import java.util.WeakHashMap

internal object AioRuntimeStore {
    val mainHandler = Handler(Looper.getMainLooper())
    var latestBinding: WeakReference<AioBinding>? = null
    var latestComposeView: WeakReference<View>? = null
    var latestMsgRepo: WeakReference<Any>? = null
    var latestAioListVb: WeakReference<Any>? = null
    var latestInputBarController: WeakReference<Any>? = null
    var latestNickNameAbility: WeakReference<Any>? = null
    var disabledHomeComposeView: WeakReference<View>? = null
    var disabledHomeComposeState: ViewState? = null
    var creatingFragment: WeakReference<Any>? = null
    var latestListUiRows: List<*>? = null
    var aioSurfaceActive: Boolean = false
    val bindingsByRoot = WeakHashMap<View, WeakReference<AioBinding>>()
    val bindingsByFragment = WeakHashMap<Any, WeakReference<AioBinding>>()
    val snapshotsByPeer = LinkedHashMap<String, AioUiState>()
    private val pendingMessages = WeakHashMap<Any, MutableList<AioMessage>>()
    private val orphanPendingMessages = ArrayList<AioMessage>()
    private var latestMemberInfoCache: Map<String, Any>? = null

    fun reset() {
        latestBinding = null
        latestComposeView = null
        latestMsgRepo = null
        latestAioListVb = null
        latestInputBarController = null
        latestNickNameAbility = null
        disabledHomeComposeView = null
        disabledHomeComposeState = null
        creatingFragment = null
        latestListUiRows = null
        aioSurfaceActive = false
        bindingsByRoot.clear()
        bindingsByFragment.clear()
        snapshotsByPeer.clear()
        orphanPendingMessages.clear()
        pendingMessages.clear()
        latestMemberInfoCache = null
    }

    fun registerBinding(root: View, hostFragment: Any?, binding: AioBinding) {
        latestBinding = WeakReference(binding)
        bindingsByRoot[root] = WeakReference(binding)
        if (hostFragment != null) {
            bindingsByFragment[hostFragment] = WeakReference(binding)
        }
    }

    fun rememberMsgRepo(repo: Any?) {
        if (repo != null) {
            latestMsgRepo = WeakReference(repo)
        }
    }

    fun rememberAioListVb(listVb: Any?) {
        if (listVb != null) {
            latestAioListVb = WeakReference(listVb)
        }
    }

    fun rememberInputBarController(inputBarController: Any?) {
        if (inputBarController != null) {
            latestInputBarController = WeakReference(inputBarController)
        }
    }

    fun rememberNickNameAbility(ability: Any?, cacheField: java.lang.reflect.Field?) {
        if (ability != null) {
            latestNickNameAbility = WeakReference(ability)
        }
        latestMemberInfoCache = runCatching {
            @Suppress("UNCHECKED_CAST")
            cacheField?.get(ability) as? Map<String, Any>
        }.getOrNull() ?: latestMemberInfoCache
    }

    fun findMemberInfo(uid: String): Any? {
        if (uid.isBlank()) return null
        val cache = latestMemberInfoCache ?: return null
        return cache[uid]
    }

    fun mergeMemberInfoCache(infos: Map<String, Any>) {
        if (infos.isEmpty()) return
        val merged = LinkedHashMap<String, Any>()
        latestMemberInfoCache?.let(merged::putAll)
        merged.putAll(infos)
        latestMemberInfoCache = merged
    }

    fun markAioSurfaceActive() {
        aioSurfaceActive = true
    }

    fun markAioSurfaceInactive() {
        aioSurfaceActive = false
    }

    fun rememberComposeView(view: View?) {
        if (view != null) {
            latestComposeView = WeakReference(view)
        }
    }

    fun forgetComposeView(view: View?) {
        if (view != null && latestComposeView?.get() === view) {
            latestComposeView = null
        }
    }

    fun activeComposeView(): View? {
        return latestComposeView?.get()?.takeIf {
            it.isAttachedToWindow && it.isVisible && it.width > 0 && it.height > 0
        }
    }

    fun hasActiveComposeView(): Boolean {
        return activeComposeView() != null
    }

    fun hasActiveAioSurface(): Boolean {
        return aioSurfaceActive || hasActiveComposeView()
    }

    fun disableHomeComposeForAio(homeCompose: View) {
        if (disabledHomeComposeState == null &&
            homeCompose.isVisible &&
            homeCompose.isEnabled
        ) {
            disabledHomeComposeState = homeCompose.captureState()
            disabledHomeComposeView = WeakReference(homeCompose)
        } else if (disabledHomeComposeView?.get() !== homeCompose) {
            disabledHomeComposeView = WeakReference(homeCompose)
        }
        homeCompose.visibility = View.INVISIBLE
        homeCompose.alpha = 0f
        homeCompose.isEnabled = false
        homeCompose.isClickable = false
        homeCompose.isLongClickable = false
        homeCompose.isFocusable = false
        homeCompose.isFocusableInTouchMode = false
        homeCompose.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    fun restoreHomeComposeAfterAio() {
        val homeCompose = disabledHomeComposeView?.get() ?: return
        val state = disabledHomeComposeState
        if (state != null) {
            state.restoreTo(homeCompose)
        } else {
            homeCompose.visibility = View.VISIBLE
            homeCompose.alpha = 1f
            homeCompose.isEnabled = true
            homeCompose.isClickable = true
            homeCompose.isFocusable = true
            homeCompose.isFocusableInTouchMode = true
            homeCompose.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        }
        homeCompose.bringToFront()
        disabledHomeComposeView = null
        disabledHomeComposeState = null
    }

    fun releaseAioSurfaceForHome(anchor: View? = null) {
        markAioSurfaceInactive()
        val roots = linkedSetOf<ViewGroup>()
        anchor?.collectAioReleaseRoots(roots)
        latestComposeView?.get()?.let { composeView ->
            composeView.collectAioReleaseRoots(roots)
            (composeView.parent as? ViewGroup)?.let(roots::add)
            (composeView.rootView as? ViewGroup)
                ?.findViewWithTag<View>(AIO_COMPOSE_TAG)
                ?.parent
                ?.let { it as? ViewGroup }
                ?.let(roots::add)
        }
        bindingsByRoot.keys.toList().forEach { root ->
            root.collectAioReleaseRoots(roots)
        }
        disabledHomeComposeView?.get()?.rootView
            ?.findViewWithTag<View>(AIO_COMPOSE_TAG)
            ?.parent
            ?.let { it as? ViewGroup }
            ?.let(roots::add)
        roots.forEach { root ->
            root.requestDisallowInterceptTouchEvent(false)
            root.removeHostComposeView(AIO_COMPOSE_TAG, AIO_COMPOSE_BINDING_KEY)
            root.invalidate()
        }
        latestComposeView = null
        restoreHomeComposeAfterAio()
    }

    fun findBindingForFragment(hostFragment: Any?): AioBinding? {
        if (hostFragment == null) return null
        return bindingsByFragment[hostFragment]?.get()
    }

    fun findBindingForView(view: View?): AioBinding? {
        var current: View? = view
        while (current != null) {
            bindingsByRoot[current]?.get()?.let { return it }
            current = current.parent as? View
        }
        return null
    }

    fun bufferMessage(hostFragment: Any?, message: AioMessage) {
        if (hostFragment == null) {
            orphanPendingMessages.add(message)
            return
        }
        pendingMessages.getOrPut(hostFragment) { ArrayList() }.add(message)
    }

    fun drainPendingMessages(hostFragment: Any?): List<AioMessage> {
        if (hostFragment == null && orphanPendingMessages.isEmpty()) return emptyList()
        val messages = ArrayList<AioMessage>()
        hostFragment?.let { fragment ->
            pendingMessages.remove(fragment)?.let(messages::addAll)
        }
        if (orphanPendingMessages.isNotEmpty()) {
            messages.addAll(orphanPendingMessages)
            orphanPendingMessages.clear()
        }
        return messages
    }

    data class ViewState(
        val visibility: Int,
        val alpha: Float,
        val enabled: Boolean,
        val clickable: Boolean,
        val longClickable: Boolean,
        val focusable: Boolean,
        val focusableInTouchMode: Boolean,
        val importantForAccessibility: Int
    ) {
        fun restoreTo(view: View) {
            view.visibility = visibility
            view.alpha = alpha
            view.isEnabled = enabled
            view.isClickable = clickable
            view.isLongClickable = longClickable
            view.isFocusable = focusable
            view.isFocusableInTouchMode = focusableInTouchMode
            view.importantForAccessibility = importantForAccessibility
        }
    }

    private fun View.captureState(): ViewState {
        return ViewState(
            visibility = visibility,
            alpha = alpha,
            enabled = isEnabled,
            clickable = isClickable,
            longClickable = isLongClickable,
            focusable = isFocusable,
            focusableInTouchMode = isFocusableInTouchMode,
            importantForAccessibility = importantForAccessibility
        )
    }

    private fun View.collectAioReleaseRoots(roots: MutableSet<ViewGroup>) {
        (this as? ViewGroup)?.let(roots::add)
        (parent as? ViewGroup)?.let(roots::add)
        rootView?.let { root ->
            (root as? ViewGroup)?.let(roots::add)
            root.findViewById<ViewGroup?>(android.R.id.content)?.let(roots::add)
            root.findViewWithTag<View>(AIO_COMPOSE_TAG)
                ?.parent
                ?.let { it as? ViewGroup }
                ?.let(roots::add)
        }
    }

}
