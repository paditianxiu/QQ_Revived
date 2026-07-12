package me.padi.qqlite.revived.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import java.util.IdentityHashMap
import me.padi.qqlite.revived.hooks.common.StatusBarHook

internal fun ViewGroup.addHostComposeView(
    tag: Any,
    bindingKey: Int,
    layoutParamsFactory: ViewGroup.() -> ViewGroup.LayoutParams,
    lifecycleAnchor: View = this,
    useWindowLayer: Boolean = false,
    wrapInContainer: Boolean = true,
    configure: ComposeView.() -> Unit = {},
    onHostViewReady: (View) -> Unit = {},
    onHostViewDestroyed: (View) -> Unit = {},
    onOwnerReady: (ViewModelStoreOwner) -> Unit = {},
    onOwnerDestroyed: () -> Unit = {},
    onWindowBackPressed: () -> Unit = {},
    onWindowDismissed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    findViewWithTag<View>(tag)?.let { removeView(it) }

    val oldBinding = getTag(bindingKey) as? HostComposeBinding
    oldBinding?.dispose()

    val binding = HostComposeBinding(
        root = this,
        lifecycleAnchor = lifecycleAnchor,
        viewTag = tag,
        layoutParamsFactory = layoutParamsFactory,
        useWindowLayer = useWindowLayer,
        wrapInContainer = wrapInContainer,
        configure = configure,
        onHostViewReady = onHostViewReady,
        onHostViewDestroyed = onHostViewDestroyed,
        onOwnerReady = onOwnerReady,
        onOwnerDestroyed = onOwnerDestroyed,
        onWindowBackPressed = onWindowBackPressed,
        onWindowDismissed = onWindowDismissed,
        content = content
    )
    setTag(bindingKey, binding)
    binding.attachWhenReady()
}

internal fun ViewGroup.removeHostComposeView(
    tag: Any,
    bindingKey: Int
) {
    val oldBinding = getTag(bindingKey) as? HostComposeBinding
    oldBinding?.dispose()
    setTag(bindingKey, null)

    repeat(MAX_DUPLICATE_HOST_REMOVAL) {
        val duplicate = findViewWithTag<View>(tag) ?: return
        duplicate.disposeComposeChildren()
        (duplicate.parent as? ViewGroup)?.removeView(duplicate)
    }
}

private class HostComposeBinding(
    private val root: ViewGroup,
    private val lifecycleAnchor: View,
    private val viewTag: Any,
    private val layoutParamsFactory: ViewGroup.() -> ViewGroup.LayoutParams,
    private val useWindowLayer: Boolean,
    private val wrapInContainer: Boolean,
    private val configure: ComposeView.() -> Unit,
    private val onHostViewReady: (View) -> Unit,
    private val onHostViewDestroyed: (View) -> Unit,
    private val onOwnerReady: (ViewModelStoreOwner) -> Unit,
    private val onOwnerDestroyed: () -> Unit,
    private val onWindowBackPressed: () -> Unit,
    private val onWindowDismissed: () -> Unit,
    private val content: @Composable () -> Unit
) : View.OnAttachStateChangeListener {
    private var owner: HostComposeOwner? = null
    private var overlayDialog: Dialog? = null
    private var hostView: View? = null
    private var listening = false

    fun attachWhenReady() {
        if (!listening) {
            lifecycleAnchor.addOnAttachStateChangeListener(this)
            listening = true
        }

        if (lifecycleAnchor.isAttachedToWindow) {
            attachComposeView()
        }
    }

    override fun onViewAttachedToWindow(v: View) {
        attachComposeView()
    }

    override fun onViewDetachedFromWindow(v: View) {
        removeComposeView()
        onOwnerDestroyed()
        owner?.destroy()
        owner = null
    }

    fun dispose() {
        removeComposeView()
        onOwnerDestroyed()
        owner?.destroy()
        owner = null
        if (listening) {
            lifecycleAnchor.removeOnAttachStateChangeListener(this)
            listening = false
        }
    }

    private fun attachComposeView() {
        removeComposeView()

        val nextOwner = HostComposeOwner()
        owner = nextOwner
        nextOwner.install(lifecycleAnchor)
        nextOwner.install(root)
        nextOwner.resume()
        onOwnerReady(nextOwner)

        val composeContext = root.context.asModuleComposeContext()
        val nextHost = createComposeHost(root, nextOwner, composeContext)
        hostView = nextHost
        onHostViewReady(nextHost)
        if (useWindowLayer) {
            overlayDialog = Dialog(root.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCanceledOnTouchOutside(false)
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (event.action == KeyEvent.ACTION_UP) {
                            onWindowBackPressed()
                        }
                        true
                    } else {
                        false
                    }
                }
                setOnDismissListener {
                    onWindowDismissed()
                }
                setContentView(nextHost)
                configureWindow()
                installWindowOwners(this, nextHost, nextOwner)
                show()
                configureWindow()
                installWindowOwners(this, nextHost, nextOwner)
            }
        } else {
            root.addView(nextHost, root.layoutParamsFactory())
            root.findViewWithTag<View>(viewTag)?.bringToFront()
        }
    }

    private fun Dialog.configureWindow() {
        window?.apply {
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            StatusBarHook.applyWindowStatusBarPolicy(this)
            decorView.setPadding(0, 0, 0, 0)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun installWindowOwners(dialog: Dialog, host: View, owner: HostComposeOwner) {
        owner.install(host)
        owner.install(host.rootView)
        dialog.window?.decorView?.let(owner::install)
        (host.parent as? View)?.let(owner::install)
        host.post {
            owner.install(host)
            owner.install(host.rootView)
            dialog.window?.decorView?.let(owner::install)
            (host.parent as? View)?.let(owner::install)
            owner.resume()
        }
    }

    private fun createComposeHost(
        root: View,
        owner: HostComposeOwner,
        composeContext: Context
    ): View {
        val composeView = createComposeView(root, owner, composeContext)
        if (!wrapInContainer) {
            composeView.tag = viewTag
            return composeView
        }

        composeView.tag = "$viewTag.ComposeView"
        return HostComposeContainer(composeContext).apply {
            tag = viewTag
            owner.install(this)
            isClickable = composeView.isClickable
            isEnabled = composeView.isEnabled
            isFocusable = composeView.isFocusable
            isFocusableInTouchMode = composeView.isFocusableInTouchMode
            importantForAccessibility = composeView.importantForAccessibility
            alpha = composeView.alpha
            elevation = composeView.elevation
            translationZ = composeView.translationZ
            clipChildren = false
            clipToPadding = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createComposeView(
        root: View,
        owner: HostComposeOwner,
        composeContext: Context
    ): ComposeView {
        return ComposeView(composeContext).apply {
            owner.install(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.requestFocus()
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    owner.install(v)
                    owner.resume()
                }

                override fun onViewDetachedFromWindow(v: View) {
                    disposeComposition()
                    owner.destroy()
                    removeOnAttachStateChangeListener(this)
                }
            })
            configure()
            setContent(content)
        }
    }

    private fun removeComposeView() {
        val dialogHost = hostView?.takeIf { overlayDialog != null }
        dialogHost?.disposeComposeChildren()
        overlayDialog?.setOnDismissListener(null)
        overlayDialog?.dismiss()
        overlayDialog = null

        val currentHost = hostView ?: root.findViewWithTag<View>(viewTag)
        hostView = null
        if (currentHost != null && currentHost !== dialogHost) {
            currentHost.disposeComposeChildren()
            (currentHost.parent as? ViewGroup)?.removeView(currentHost)
            onHostViewDestroyed(currentHost)
        }
        dialogHost?.let(onHostViewDestroyed)

        repeat(MAX_DUPLICATE_HOST_REMOVAL) {
            val duplicate = root.findViewWithTag<View>(viewTag) ?: return
            duplicate.disposeComposeChildren()
            (duplicate.parent as? ViewGroup)?.removeView(duplicate)
            onHostViewDestroyed(duplicate)
        }
    }

    private companion object {
        const val MAX_DUPLICATE_HOST_REMOVAL = 4
    }
}

private const val MAX_DUPLICATE_HOST_REMOVAL = 4

private class HostComposeContainer(context: Context) : FrameLayout(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val child = getChildAt(0)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                child?.requestFocus()
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        val handled = super.dispatchTouchEvent(event)
        return handled || isClickable
    }
}

private fun Context.asModuleComposeContext(): Context {
    val sourceContext = applicationContext ?: this
    val moduleContext = runCatching {
        sourceContext.createPackageContext(
            MODULE_PACKAGE_NAME,
            Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
        )
    }.getOrNull() ?: return this
    return ModuleResourceContext(this, moduleContext)
}

private class ModuleResourceContext(
    base: Context,
    private val moduleContext: Context
) : ContextWrapper(base) {
    override fun getApplicationContext(): Context {
        return baseContext.applicationContext ?: baseContext
    }

    override fun getResources(): Resources {
        return moduleContext.resources
    }

    override fun getAssets(): AssetManager {
        return moduleContext.assets
    }

    override fun getTheme(): Resources.Theme {
        return moduleContext.theme
    }

    override fun getClassLoader(): ClassLoader {
        return moduleContext.classLoader
    }

    override fun getPackageName(): String {
        return moduleContext.packageName
    }
}

private fun View.disposeComposeChildren() {
    if (this is ComposeView) {
        disposeComposition()
        return
    }
    if (this !is ViewGroup) return
    for (index in 0 until childCount) {
        getChildAt(index).disposeComposeChildren()
    }
}

private const val MODULE_PACKAGE_NAME = "me.padi.qqlite.revived"

private class HostComposeOwner : LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner,
    NavigationEventDispatcherOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val boundViews = IdentityHashMap<View, PreviousOwners>()
    private var created = false
    private var destroyed = false

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val navigationEventDispatcher = NavigationEventDispatcher()

    fun install(view: View) {
        if (destroyed) return
        create()

        var current: View? = view
        while (current != null) {
            installOnView(current)
            current = current.parent as? View
        }
    }

    private fun installOnView(view: View) {
        val currentLifecycleOwner = ViewTreeOwnerBridge.getLifecycleOwner(view)
        val currentSavedStateOwner = ViewTreeOwnerBridge.getSavedStateRegistryOwner(view)
        val currentViewModelOwner = ViewTreeOwnerBridge.getViewModelStoreOwner(view)
        val currentNavigationOwner = ViewTreeOwnerBridge.getNavigationEventDispatcherOwner(view)
        if (currentLifecycleOwner === this &&
            currentSavedStateOwner === this &&
            currentViewModelOwner === this &&
            currentNavigationOwner === this
        ) {
            return
        }
        if (!boundViews.containsKey(view)) {
            boundViews[view] = PreviousOwners(
                lifecycleOwner = currentLifecycleOwner,
                savedStateRegistryOwner = currentSavedStateOwner,
                viewModelStoreOwner = currentViewModelOwner,
                navigationEventDispatcherOwner = currentNavigationOwner
            )
        }
        ViewTreeOwnerBridge.setLifecycleOwner(view, this)
        ViewTreeOwnerBridge.setSavedStateRegistryOwner(view, this)
        ViewTreeOwnerBridge.setViewModelStoreOwner(view, this)
        ViewTreeOwnerBridge.setNavigationEventDispatcherOwner(view, this)
    }

    fun resume() {
        if (destroyed) return
        create()
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    fun destroy() {
        if (destroyed) return

        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        clearViewTreeOwners()
        store.clear()
        destroyed = true
    }

    private fun create() {
        if (created) return
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        created = true
    }

    private fun clearViewTreeOwners() {
        boundViews.forEach { (view, previous) ->
            if (ViewTreeOwnerBridge.getLifecycleOwner(view) === this) {
                ViewTreeOwnerBridge.setLifecycleOwner(view, previous.lifecycleOwner)
            }
            if (ViewTreeOwnerBridge.getSavedStateRegistryOwner(view) === this) {
                ViewTreeOwnerBridge.setSavedStateRegistryOwner(
                    view,
                    previous.savedStateRegistryOwner
                )
            }
            if (ViewTreeOwnerBridge.getViewModelStoreOwner(view) === this) {
                ViewTreeOwnerBridge.setViewModelStoreOwner(view, previous.viewModelStoreOwner)
            }
            if (ViewTreeOwnerBridge.getNavigationEventDispatcherOwner(view) === this) {
                ViewTreeOwnerBridge.setNavigationEventDispatcherOwner(
                    view,
                    previous.navigationEventDispatcherOwner
                )
            }
        }
        boundViews.clear()
        navigationEventDispatcher.dispose()
    }

    private data class PreviousOwners(
        val lifecycleOwner: LifecycleOwner?,
        val savedStateRegistryOwner: SavedStateRegistryOwner?,
        val viewModelStoreOwner: ViewModelStoreOwner?,
        val navigationEventDispatcherOwner: NavigationEventDispatcherOwner?
    )
}

private object ViewTreeOwnerBridge {
    private val lifecycleOwnerClass by lazy { Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner") }
    private val viewModelStoreOwnerClass by lazy {
        Class.forName("androidx.lifecycle.ViewTreeViewModelStoreOwner")
    }
    private val savedStateRegistryOwnerClass by lazy {
        Class.forName("androidx.savedstate.ViewTreeSavedStateRegistryOwner")
    }
    private val navigationEventDispatcherOwnerClass by lazy {
        Class.forName("androidx.navigationevent.ViewTreeNavigationEventDispatcherOwner")
    }

    fun setLifecycleOwner(view: View, owner: LifecycleOwner?) {
        lifecycleOwnerClass.getMethod("set", View::class.java, LifecycleOwner::class.java)
            .invoke(null, view, owner)
    }

    fun getLifecycleOwner(view: View): LifecycleOwner? {
        return lifecycleOwnerClass.getMethod("get", View::class.java)
            .invoke(null, view) as? LifecycleOwner
    }

    fun setViewModelStoreOwner(view: View, owner: ViewModelStoreOwner?) {
        viewModelStoreOwnerClass.getMethod(
            "set", View::class.java, ViewModelStoreOwner::class.java
        ).invoke(null, view, owner)
    }

    fun getViewModelStoreOwner(view: View): ViewModelStoreOwner? {
        return viewModelStoreOwnerClass.getMethod("get", View::class.java)
            .invoke(null, view) as? ViewModelStoreOwner
    }

    fun setSavedStateRegistryOwner(view: View, owner: SavedStateRegistryOwner?) {
        savedStateRegistryOwnerClass.getMethod(
            "set", View::class.java, SavedStateRegistryOwner::class.java
        ).invoke(null, view, owner)
    }

    fun getSavedStateRegistryOwner(view: View): SavedStateRegistryOwner? {
        return savedStateRegistryOwnerClass.getMethod("get", View::class.java)
            .invoke(null, view) as? SavedStateRegistryOwner
    }

    fun setNavigationEventDispatcherOwner(view: View, owner: NavigationEventDispatcherOwner?) {
        navigationEventDispatcherOwnerClass.getMethod(
            "set", View::class.java, NavigationEventDispatcherOwner::class.java
        ).invoke(null, view, owner)
    }

    fun getNavigationEventDispatcherOwner(view: View): NavigationEventDispatcherOwner? {
        return navigationEventDispatcherOwnerClass.getMethod("get", View::class.java)
            .invoke(null, view) as? NavigationEventDispatcherOwner
    }
}
