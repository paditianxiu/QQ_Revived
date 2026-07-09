package me.padi.qqlite.revived.hooks.aio

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.google.android.material.navigationrail.NavigationRailView
import me.padi.qqlite.revived.ModuleMainKt
import me.padi.qqlite.revived.hooks.common.BaseHook
import me.padi.qqlite.revived.hooks.common.findTargetClass
import me.padi.qqlite.revived.utils.MaterialInflaterContext
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.math.roundToInt

internal object AioTopBarHook : BaseHook() {
    private const val WATCH_AIO_FRAGMENT_CLASS = "com.tencent.watch.aio_impl.ui.WatchAIOFragment"
    private const val VIEW_PAGER_CLASS = "androidx.viewpager2.widget.ViewPager2"
    private const val WATCH_AIO_LIST_CONTAINER_CLASS =
        "com.tencent.watch.aio_impl.coreImpl.vb.WatchAIOListVB\$onCreateView\$8"
    private const val THIRD_LEVEL_CONTAINER_CLASS =
        "com.tencent.aio.part.root.panel.content.thirdLevel.mvx.vb.ThirdLevelVB\$onCreateView\$container\$1"
    private const val THIRD_LEVEL_VB_CLASS =
        "com.tencent.aio.part.root.panel.content.thirdLevel.mvx.vb.ThirdLevelVB"
    private const val CREATE_VIEW_PARAMS_CLASS = "com.tencent.mvi.api.help.CreateViewParams"
    private const val CIRCLE_INDICATOR_CLASS =
        "com.tencent.qqnt.watch.ui.componet.tablayout.CircleIndicator"
    private const val WATCH_AVATAR_VIEW_CLASS = "com.tencent.qqnt.avatar.WatchAvatarView"
    private const val SINGLE_LINE_TEXT_VIEW_CLASS = "com.tencent.widget.SingleLineTextView"
    private const val AVATAR_UTILS_CLASS = "com.tencent.qqnt.watch.gallery.preview.AvatarUtils"
    private const val AVATAR_SIZE_TYPE_CLASS = "com.tencent.qqnt.avatar.AvatarSizeType"
    private const val FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
    private const val LIFECYCLE_OWNER_CLASS = "androidx.lifecycle.LifecycleOwner"
    private const val LIFECYCLE_OWNER_KT_CLASS = "androidx.lifecycle.LifecycleOwnerKt"
    private const val COROUTINE_SCOPE_CLASS = "kotlinx.coroutines.CoroutineScope"
    private const val GROUP_AIO_HELPER_CLASS =
        "com.tencent.watch.aio_impl.coreImpl.helper.GroupAIOHelper"
    private const val GROUP_DETAIL_COLLECTOR_CLASS =
        "com.tencent.watch.aio_impl.coreImpl.helper.GroupAIOHelper\$onMoveToState\$1\$invokeSuspend\$\$inlined\$collect\$1"
    private const val GROUP_DETAIL_INFO_CLASS =
        "com.tencent.qqnt.kernel.nativeinterface.GroupDetailInfo"
    private const val CONTINUATION_CLASS = "kotlin.coroutines.Continuation"
    private const val TOP_BAR_TAG = "QQRevived.AioTopBarHook.TopBar"
    private const val BACK_TAG = "QQRevived.AioTopBarHook.Back"
    private const val AVATAR_TAG = "QQRevived.AioTopBarHook.Avatar"
    private const val TITLE_TAG = "QQRevived.AioTopBarHook.Title"
    private const val MENU_TAG = "QQRevived.AioTopBarHook.Menu"
    private const val AVATAR_BINDING_PREFIX = "QQRevived.AioTopBarHook.AvatarBinding:"
    private const val KEY_PEER_ID = "key_bundle_peer_id"
    private const val KEY_CHAT_TYPE = "key_bundle_chat_type"
    private const val KEY_CHAT_NICK = "key_bundle_chat_nick"
    private const val KEY_CHAT_UIN = "key_bundle_chat_uin"
    private const val GROUP_CHAT_TYPE = 2
    private const val MENU_PAGE_INDEX = 1
    private const val TOP_BAR_HEIGHT_DP = 100
    private const val ICON_WIDTH_DP = 50
    private const val AVATAR_SIZE_DP = 60
    private const val HORIZONTAL_PADDING_DP = 4
    private const val TITLE_GAP_DP = 8
    private const val THIRD_LEVEL_TOP_PADDING_DP = TOP_BAR_HEIGHT_DP
    private val GROUP_INFO_REFRESH_DELAYS = longArrayOf(500L, 1500L, 3000L)

    private var installed = false
    private val topBarBindings = HashMap<String, TopBarBinding>()

    override fun reset() {
        installed = false
        topBarBindings.clear()
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        if (installed) return
        val legacyHookEnabled = false
        if (!legacyHookEnabled) {
            installed = true
            module.logHook(Log.INFO, "AIO legacy top bar hook disabled")
            return
        }

        val targetClassLoader = requireClassLoader(classLoader)
        runCatching {
            val hookState = HookState.create(targetClassLoader)
            module.intercept(
                hookState.watchAIOFragmentClass.getDeclaredMethod(
                    "a0", LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java
                )
            ) {
                val result = proceed()
                runCatching {
                    val root = result as? FrameLayout ?: return@runCatching
                    bindTopBar(module, hookState, thisObject, root)
                }.onFailure {
                    module.logHook(Log.WARN, "AIO top bar bind skipped", it)
                }
                result
            }

            hookGroupInfoCollector(module, hookState)
            hookWatchAIOListContainer(module, hookState)
            hookThirdLevelContainer(module, hookState)

            installed = true
            module.logHook(Log.INFO, "AIO top bar hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "AIO top bar hook skipped", it)
        }
    }

    private fun hookGroupInfoCollector(module: ModuleMainKt, hookState: HookState) {
        val collectorClass = hookState.groupInfoCollectorClass ?: return
        val continuationClass = hookState.continuationClass ?: return

        runCatching {
            module.intercept(
                collectorClass.getDeclaredMethod(
                    "emit", Any::class.java, continuationClass
                )
            ) {
                val result = proceed()
                runCatching {
                    val peerId = hookState.groupInfoCollectorPeerIdField?.get(thisObject) as? String
                        ?: return@runCatching
                    refreshBoundTopBar(hookState, peerId)
                }.onFailure {
                    module.logHook(Log.WARN, "AIO top bar group info refresh skipped", it)
                }
                result
            }
        }.onFailure {
            module.logHook(Log.WARN, "AIO top bar group info hook skipped", it)
        }
    }

    private fun hookWatchAIOListContainer(module: ModuleMainKt, hookState: HookState) {
        val containerClass = hookState.watchAIOListContainerClass ?: return

        runCatching {
            module.hookConstructor(
                containerClass.getDeclaredConstructor(Context::class.java)
            ) {
                val result = proceed()
                runCatching {
                    (thisObject as? ViewGroup)?.applyTopBarContentInset()
                }.onFailure {
                    module.logHook(Log.WARN, "AIO list top padding skipped", it)
                }
                result
            }
        }.onFailure {
            module.logHook(Log.WARN, "AIO list container hook skipped", it)
        }
    }

    private fun hookThirdLevelContainer(module: ModuleMainKt, hookState: HookState) {
        val containerClass = hookState.thirdLevelContainerClass ?: return
        val thirdLevelVBClass = hookState.thirdLevelVBClass ?: return
        val createViewParamsClass = hookState.createViewParamsClass ?: return

        runCatching {
            module.hookConstructor(
                containerClass.getDeclaredConstructor(
                    thirdLevelVBClass, createViewParamsClass, Context::class.java
                )
            ) {
                val result = proceed()
                runCatching {
                    (thisObject as? ViewGroup)?.applyThirdLevelTopPadding()
                }.onFailure {
                    module.logHook(Log.WARN, "AIO third level top padding skipped", it)
                }
                result
            }
        }.onFailure {
            module.logHook(Log.WARN, "AIO third level container hook skipped", it)
        }
    }

    private fun bindTopBar(
        module: ModuleMainKt, hookState: HookState, fragment: Any?, root: FrameLayout
    ) {
        if (fragment == null) return

        root.clipChildren = false
        root.clipToPadding = false

        val indicator = root.findFirstDescendantByClassName(CIRCLE_INDICATOR_CLASS)
        indicator?.hideIndicator()

        val viewPager =
            hookState.viewPagerField?.get(fragment) as? View ?: root.findFirstDescendantByClassName(
                VIEW_PAGER_CLASS
            )
        val height = resolveTopBarHeight(root, indicator, hookState, fragment)
        viewPager?.ensureTopInset(height)

        val peer = readPeerInfo(hookState, fragment) ?: return

        val topBar = (root.findViewWithTag(TOP_BAR_TAG) as? LinearLayout) ?: createTopBar(
            module, hookState, root.context, fragment, viewPager
        ).also {
            root.addView(it)
        }

        topBar.applyFrameLayoutParams(height)
        topBar.updateActions(module, fragment, viewPager)
        topBar.updateTitle(hookState, peer)
        topBar.updateAvatar(module, hookState, fragment, peer)
        topBar.bringToFront()

        rememberTopBar(peer, topBar)
        scheduleGroupInfoRefresh(topBar, hookState, peer)
    }

    private fun rememberTopBar(peer: AIOPeer, topBar: LinearLayout) {
        topBarBindings[peer.peerId] = TopBarBinding(peer, WeakReference(topBar))
    }

    private fun refreshBoundTopBar(hookState: HookState, peerId: String) {
        val binding = topBarBindings[peerId] ?: return
        val topBar = binding.topBarRef.get()
        if (topBar == null) {
            topBarBindings.remove(peerId)
            return
        }
        topBar.post {
            if (topBar.isAttachedToWindow) {
                topBar.updateTitle(hookState, binding.peer)
            }
        }
    }

    private fun createTopBar(
        module: ModuleMainKt,
        hookState: HookState,
        context: Context,
        fragment: Any,
        viewPager: View?
    ): LinearLayout {
        return LinearLayout(context).apply topBar@{
            tag = TOP_BAR_TAG
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor("#2CA1F5".toColorInt())
            setPadding(
                HORIZONTAL_PADDING_DP.toPx(this), 16.toPx(this), HORIZONTAL_PADDING_DP.toPx(this), 0
            )
            isClickable = true
            isFocusable = true
            elevation = 24.toPx(this).toFloat()
            translationZ = 24.toPx(this).toFloat()

            addView(
                createIconButton(
                    module,
                    context,
                    me.padi.qqlite.revived.R.drawable.baseline_arrow_back_ios_new_24
                ).apply {
                    tag = BACK_TAG
                    setOnClickListener {
                        navigateBack(module, fragment, viewPager)
                    }
                })

            addView(createAvatarView(hookState, context).apply {
                val avatarSize = AVATAR_SIZE_DP.toPx(this)
                tag = AVATAR_TAG
                layoutParams = LinearLayout.LayoutParams(
                    avatarSize, avatarSize
                ).apply {
                    marginEnd = TITLE_GAP_DP.toPx(this@topBar)
                }
            })

            addView(createTitleView(hookState, context).apply {
                tag = TITLE_TAG
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            })

            addView(
                createIconButton(
                    module, context, me.padi.qqlite.revived.R.drawable.baseline_menu_24
                ).apply {
                    tag = MENU_TAG
                    setOnClickListener {
                        openMenuPage(viewPager)
                    }
                })
        }
    }

    private fun createIconButton(
        module: ModuleMainKt, context: Context, iconResId: Int
    ): ImageView {
        val moduleContext = module.moduleContext(context.applicationContext)
        val themedContext = MaterialInflaterContext(
            moduleContext,
            com.google.android.material.R.style.Theme_Material3_Light_NoActionBar,
            NavigationRailView::class.java.classLoader
        )

        return ImageView(themedContext).apply {
            setImageResource(iconResId)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setColorFilter(Color.WHITE)
            val view = this
            val width = ICON_WIDTH_DP.toPx(view)
            minimumWidth = width
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10.toPx(view).toFloat()
                setColor(Color.TRANSPARENT)
            }

            layoutParams = LinearLayout.LayoutParams(
                width, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }


    @Suppress("DEPRECATION")
    private fun createTitleView(hookState: HookState, context: Context): View {
        val titleView = runCatching {
            hookState.singleLineTextViewConstructor?.newInstance(context, null) as? View
        }.getOrNull() ?: TextView(context)

        if (titleView is TextView) {
            titleView.setSingleLine(true)
            titleView.ellipsize = TextUtils.TruncateAt.END
            titleView.gravity = Gravity.CENTER_VERTICAL
            titleView.textSize = 15f
            titleView.typeface = Typeface.DEFAULT_BOLD
            titleView.includeFontPadding = false
        } else {
            titleView.invokeIfExists("setTextColor", Color.WHITE)
            titleView.invokeIfExists("setTextSize", 15f)
        }
        return titleView
    }

    private fun createAvatarView(hookState: HookState, context: Context): View {
        val avatar = runCatching {
            hookState.avatarViewConstructor4?.newInstance(context, null, 0, 0) as? View
        }.getOrNull() ?: runCatching {
            hookState.avatarViewConstructor2?.newInstance(context, null) as? View
        }.getOrNull() ?: ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        return avatar.apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor("#FFE5E7EB".toColorInt())
            }
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
    }

    private fun LinearLayout.updateActions(module: ModuleMainKt, fragment: Any, viewPager: View?) {
        findViewWithTag<View>(BACK_TAG)?.setOnClickListener {
            navigateBack(module, fragment, viewPager)
        }
        findViewWithTag<View>(MENU_TAG)?.setOnClickListener {
            openMenuPage(viewPager)
        }
    }

    private fun LinearLayout.updateTitle(hookState: HookState, peer: AIOPeer) {
        val titleView = findViewWithTag<View>(TITLE_TAG) ?: return
        val groupInfo = if (peer.chatType == GROUP_CHAT_TYPE) {
            hookState.readCachedGroupInfo(peer.peerId)
        } else {
            null
        }
        titleView.setTitleText(formatTitle(peer, groupInfo))
    }

    private fun LinearLayout.updateAvatar(
        module: ModuleMainKt, hookState: HookState, fragment: Any, peer: AIOPeer
    ) {
        val avatar = findViewWithTag<View>(AVATAR_TAG) ?: return
        val bindingKey = "$AVATAR_BINDING_PREFIX${peer.chatType}:${peer.peerId}:${peer.chatUin}"
        if (avatar.contentDescription?.toString() == bindingKey) return

        avatar.contentDescription = bindingKey
        loadAvatar(module, hookState, fragment, avatar, peer)
    }

    private fun loadAvatar(
        module: ModuleMainKt, hookState: HookState, fragment: Any, avatar: View, peer: AIOPeer
    ) {
        runCatching {
            val loadMethod = hookState.avatarUtilsLoadMethod ?: return
            val avatarUtils = hookState.avatarUtilsSingleton ?: return
            val scope = hookState.lifecycleScopeMethod?.invoke(null, fragment) ?: return
            val target = if (Modifier.isStatic(loadMethod.modifiers)) null else avatarUtils
            loadMethod.invoke(
                target,
                avatarUtils,
                avatar,
                peer.chatType,
                peer.peerId,
                peer.chatUin,
                fragment,
                false,
                false,
                scope,
                null,
                256
            )
        }.onFailure {
            module.logHook(Log.WARN, "AIO top bar avatar load failed", it)
        }
    }

    private fun scheduleGroupInfoRefresh(
        topBar: LinearLayout, hookState: HookState, peer: AIOPeer
    ) {
        if (peer.chatType != GROUP_CHAT_TYPE) return
        if (hookState.readCachedGroupInfo(peer.peerId)?.memberNum != null) return

        GROUP_INFO_REFRESH_DELAYS.forEach { delay ->
            topBar.postDelayed({
                if (topBar.isAttachedToWindow) {
                    topBar.updateTitle(hookState, peer)
                }
            }, delay)
        }
    }

    private fun readPeerInfo(hookState: HookState, fragment: Any): AIOPeer? {
        val arguments = hookState.requireArgumentsMethod.invoke(fragment) as? Bundle ?: return null
        val peerId = arguments.getString(KEY_PEER_ID)?.takeIf { it.isNotBlank() } ?: return null
        val chatType = arguments.getInt(KEY_CHAT_TYPE)
        val chatNick = arguments.getString(KEY_CHAT_NICK)?.takeIf { it.isNotBlank() }
        val chatUin =
            arguments.getString(KEY_CHAT_UIN)?.toLongOrNull() ?: peerId.toLongOrNull() ?: 0L

        return AIOPeer(
            peerId = peerId, chatType = chatType, chatNick = chatNick, chatUin = chatUin
        )
    }

    private fun formatTitle(peer: AIOPeer, groupInfo: GroupInfo?): String {
        val title =
            groupInfo?.name?.takeIf { it.isNotBlank() } ?: peer.chatNick?.takeIf { it.isNotBlank() }
            ?: peer.peerId
        val memberNum = groupInfo?.memberNum
        return if (peer.chatType == GROUP_CHAT_TYPE && memberNum != null && memberNum > 0) {
            "$title($memberNum)"
        } else {
            title
        }
    }

    private fun navigateBack(module: ModuleMainKt, fragment: Any, viewPager: View?) {
        runCatching {
            if (!viewPager.moveToPreviousAIOFrame()) {
                fragment.popWatchFragment()
            }
        }.onFailure {
            module.logHook(Log.WARN, "AIO top bar back failed", it)
        }
    }

    private fun View?.moveToPreviousAIOFrame(): Boolean {
        if (this == null) return false

        val currentItem = runCatching {
            javaClass.methods.firstOrNull {
                it.name == "getCurrentItem" && it.parameterTypes.isEmpty()
            }?.invoke(this) as? Int
        }.getOrNull() ?: return false

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

    private fun openMenuPage(viewPager: View?) {
        if (viewPager == null) return
        runCatching {
            viewPager.setViewPagerCurrentItem(MENU_PAGE_INDEX, true)
        }
    }

    private fun View.setViewPagerCurrentItem(item: Int, smoothScroll: Boolean) {
        runCatching {
            val method = javaClass.methods.firstOrNull {
                it.name == "setCurrentItem" && it.parameterTypes.contentEquals(
                    arrayOf(
                        Integer.TYPE, java.lang.Boolean.TYPE
                    )
                )
            }
            if (method != null) {
                method.invoke(this, item, smoothScroll)
                return
            }
            javaClass.methods.firstOrNull {
                it.name == "setCurrentItem" && it.parameterTypes.contentEquals(arrayOf(Integer.TYPE))
            }?.invoke(this, item)
        }
    }

    private fun resolveTopBarHeight(
        root: View, indicator: View?, hookState: HookState, fragment: Any
    ): Int {
        val minHeight = TOP_BAR_HEIGHT_DP.toPx(root)
        indicator?.layoutParams?.height?.takeIf { it > 0 }?.let {
            return maxOf(it, minHeight)
        }

        val lazyHeight = runCatching {
            val lazy = hookState.tabHeightField?.get(fragment) ?: return@runCatching null
            lazy.javaClass.methods.firstOrNull {
                it.name == "getValue" && it.parameterTypes.isEmpty()
            }?.invoke(lazy) as? Number
        }.getOrNull()?.toInt()

        return maxOf(lazyHeight?.takeIf { it > 0 } ?: 0, minHeight)
    }

    private fun View.ensureTopInset(topInset: Int) {
        if (paddingTop >= topInset) return

        setPadding(paddingLeft, 0, paddingRight, paddingBottom)
        (this as? ViewGroup)?.clipToPadding = false
    }

    private fun ViewGroup.applyThirdLevelTopPadding() {
        applyTopBarContentInset()
    }

    private fun ViewGroup.applyTopBarContentInset() {
        setPadding(
            paddingLeft,
            paddingTop + THIRD_LEVEL_TOP_PADDING_DP.toPx(this),
            paddingRight,
            paddingBottom
        )
    }

    private fun LinearLayout.applyFrameLayoutParams(height: Int) {
        val params = (layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, height
        )
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = height
        params.gravity = Gravity.TOP
        layoutParams = params
        minimumHeight = height
        elevation = 2.toPx(this).toFloat()
        translationZ = 24.toPx(this).toFloat()
    }

    private fun View.hideIndicator() {
        visibility = View.INVISIBLE
        alpha = 0f
        (this as? ViewGroup)?.hideChildren()
    }

    private fun ViewGroup.hideChildren() {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            child.visibility = View.GONE
            (child as? ViewGroup)?.hideChildren()
        }
    }

    private fun ViewGroup.findFirstDescendantByClassName(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) return child
            if (child is ViewGroup) {
                child.findFirstDescendantByClassName(className)?.let { return it }
            }
        }
        return null
    }

    private fun View.setTitleText(value: String) {
        if (this is TextView) {
            text = value
            setTextColor(Color.WHITE)
        } else {
            invokeIfExists("setText", value)
        }
    }

    private fun View.invokeIfExists(methodName: String, argument: Any): Any? {
        return runCatching {
            val method = javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterTypes.size == 1
            } ?: return null
            method.invoke(this, argument)
        }.getOrNull()
    }

    private fun Int.toPx(view: View): Int {
        return (this * view.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    }

    private fun findOptionalField(clazz: Class<*>, name: String): Field? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            runCatching {
                return currentClass.getDeclaredField(name).apply { isAccessible = true }
            }
            currentClass = currentClass.superclass
        }
        return null
    }

    private fun ClassLoader.findOptionalClass(name: String): Class<*>? {
        return runCatching { findTargetClass(name) }.getOrNull()
    }

    private data class AIOPeer(
        val peerId: String, val chatType: Int, val chatNick: String?, val chatUin: Long
    )

    private data class GroupInfo(
        val name: String?, val memberNum: Int?
    )

    private data class TopBarBinding(
        val peer: AIOPeer, val topBarRef: WeakReference<LinearLayout>
    )

    private data class HookState(
        val watchAIOFragmentClass: Class<*>,
        val viewPagerField: Field?,
        val tabHeightField: Field?,
        val requireArgumentsMethod: Method,
        val watchAIOListContainerClass: Class<*>?,
        val thirdLevelContainerClass: Class<*>?,
        val thirdLevelVBClass: Class<*>?,
        val createViewParamsClass: Class<*>?,
        val groupInfoCollectorClass: Class<*>?,
        val continuationClass: Class<*>?,
        val groupInfoCollectorPeerIdField: Field?,
        val avatarViewConstructor4: Constructor<*>?,
        val avatarViewConstructor2: Constructor<*>?,
        val singleLineTextViewConstructor: Constructor<*>?,
        val avatarUtilsSingleton: Any?,
        val avatarUtilsLoadMethod: Method?,
        val lifecycleScopeMethod: Method?,
        val groupAIOHelperCompanion: Any?,
        val getCachedGroupDetailMethod: Method?,
        val groupNameGetter: Method?,
        val memberNumGetter: Method?
    ) {
        fun readCachedGroupInfo(peerId: String): GroupInfo? {
            return runCatching {
                val companion = groupAIOHelperCompanion ?: return null
                val method = getCachedGroupDetailMethod ?: return null
                val detailInfo = method.invoke(companion, peerId) ?: return null
                GroupInfo(
                    name = groupNameGetter?.invoke(detailInfo) as? String,
                    memberNum = memberNumGetter?.invoke(detailInfo) as? Int
                )
            }.getOrNull()
        }

        companion object {
            fun create(classLoader: ClassLoader): HookState {
                val watchAIOFragmentClass = classLoader.findTargetClass(WATCH_AIO_FRAGMENT_CLASS)
                val fragmentClass = classLoader.findTargetClass(FRAGMENT_CLASS)
                val watchAIOListContainerClass =
                    classLoader.findOptionalClass(WATCH_AIO_LIST_CONTAINER_CLASS)
                val thirdLevelContainerClass =
                    classLoader.findOptionalClass(THIRD_LEVEL_CONTAINER_CLASS)
                val thirdLevelVBClass = classLoader.findOptionalClass(THIRD_LEVEL_VB_CLASS)
                val createViewParamsClass = classLoader.findOptionalClass(CREATE_VIEW_PARAMS_CLASS)
                val watchAvatarViewClass = classLoader.findOptionalClass(WATCH_AVATAR_VIEW_CLASS)
                val singleLineTextViewClass =
                    classLoader.findOptionalClass(SINGLE_LINE_TEXT_VIEW_CLASS)
                val avatarUtilsClass = classLoader.findOptionalClass(AVATAR_UTILS_CLASS)
                val avatarSizeTypeClass = classLoader.findOptionalClass(AVATAR_SIZE_TYPE_CLASS)
                val lifecycleOwnerClass = classLoader.findOptionalClass(LIFECYCLE_OWNER_CLASS)
                val lifecycleOwnerKtClass = classLoader.findOptionalClass(LIFECYCLE_OWNER_KT_CLASS)
                val coroutineScopeClass = classLoader.findOptionalClass(COROUTINE_SCOPE_CLASS)
                val groupAIOHelperClass = classLoader.findOptionalClass(GROUP_AIO_HELPER_CLASS)
                val groupInfoCollectorClass =
                    classLoader.findOptionalClass(GROUP_DETAIL_COLLECTOR_CLASS)
                val groupDetailInfoClass = classLoader.findOptionalClass(GROUP_DETAIL_INFO_CLASS)
                val continuationClass = classLoader.findOptionalClass(CONTINUATION_CLASS)

                val avatarUtilsSingleton = runCatching {
                    avatarUtilsClass?.getDeclaredField("a")?.apply { isAccessible = true }
                        ?.get(null)
                }.getOrNull()

                val avatarUtilsLoadMethod = runCatching {
                    val utilsClass = avatarUtilsClass ?: return@runCatching null
                    val sizeTypeClass = avatarSizeTypeClass ?: return@runCatching null
                    val scopeClass = coroutineScopeClass ?: return@runCatching null
                    utilsClass.getDeclaredMethod(
                        "a",
                        utilsClass,
                        View::class.java,
                        Integer.TYPE,
                        String::class.java,
                        java.lang.Long.TYPE,
                        fragmentClass,
                        java.lang.Boolean.TYPE,
                        java.lang.Boolean.TYPE,
                        scopeClass,
                        sizeTypeClass,
                        Integer.TYPE
                    ).apply { isAccessible = true }
                }.getOrNull()

                val lifecycleScopeMethod = runCatching {
                    val ownerClass = lifecycleOwnerClass ?: return@runCatching null
                    lifecycleOwnerKtClass?.getDeclaredMethod("a", ownerClass)?.apply {
                        isAccessible = true
                    }
                }.getOrNull()

                val groupAIOHelperCompanion = runCatching {
                    groupAIOHelperClass?.getDeclaredField("a")?.apply { isAccessible = true }
                        ?.get(null)
                }.getOrNull()

                return HookState(
                    watchAIOFragmentClass = watchAIOFragmentClass,
                    viewPagerField = findOptionalField(watchAIOFragmentClass, "f"),
                    tabHeightField = findOptionalField(watchAIOFragmentClass, "g"),
                    requireArgumentsMethod = fragmentClass.getDeclaredMethod("requireArguments")
                        .apply { isAccessible = true },
                    watchAIOListContainerClass = watchAIOListContainerClass,
                    thirdLevelContainerClass = thirdLevelContainerClass,
                    thirdLevelVBClass = thirdLevelVBClass,
                    createViewParamsClass = createViewParamsClass,
                    groupInfoCollectorClass = groupInfoCollectorClass,
                    continuationClass = continuationClass,
                    groupInfoCollectorPeerIdField = groupInfoCollectorClass?.let {
                        findOptionalField(it, "b")
                    },
                    avatarViewConstructor4 = runCatching {
                        watchAvatarViewClass?.getDeclaredConstructor(
                            Context::class.java,
                            AttributeSet::class.java,
                            Integer.TYPE,
                            Integer.TYPE
                        )?.apply { isAccessible = true }
                    }.getOrNull(),
                    avatarViewConstructor2 = runCatching {
                        watchAvatarViewClass?.getDeclaredConstructor(
                            Context::class.java, AttributeSet::class.java
                        )?.apply { isAccessible = true }
                    }.getOrNull(),
                    singleLineTextViewConstructor = runCatching {
                        singleLineTextViewClass?.getDeclaredConstructor(
                            Context::class.java, AttributeSet::class.java
                        )?.apply { isAccessible = true }
                    }.getOrNull(),
                    avatarUtilsSingleton = avatarUtilsSingleton,
                    avatarUtilsLoadMethod = avatarUtilsLoadMethod,
                    lifecycleScopeMethod = lifecycleScopeMethod,
                    groupAIOHelperCompanion = groupAIOHelperCompanion,
                    getCachedGroupDetailMethod = runCatching {
                        groupAIOHelperCompanion?.javaClass?.getDeclaredMethod(
                            "a", String::class.java
                        )?.apply { isAccessible = true }
                    }.getOrNull(),
                    groupNameGetter = runCatching {
                        groupDetailInfoClass?.getDeclaredMethod("getGroupName")
                            ?.apply { isAccessible = true }
                    }.getOrNull(),
                    memberNumGetter = runCatching {
                        groupDetailInfoClass?.getDeclaredMethod("getMemberNum")
                            ?.apply { isAccessible = true }
                    }.getOrNull()
                )
            }
        }
    }
}
