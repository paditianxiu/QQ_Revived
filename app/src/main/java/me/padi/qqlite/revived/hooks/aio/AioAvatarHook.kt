package me.padi.qqlite.revived.hooks.aio

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import me.padi.qqlite.revived.ModuleMainKt
import me.padi.qqlite.revived.hooks.common.BaseHook
import me.padi.qqlite.revived.hooks.common.findTargetClass
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.math.roundToInt

internal object AioAvatarHook : BaseHook() {
    private const val ABS_MSG_LIST_VB_CLASS =
        "com.tencent.aio.part.root.panel.content.firstLevel.msglist.mvx.vb.core.AbsMsgListVB"
    private const val AIO_MSG_VIEW_HOLDER_CLASS =
        "com.tencent.aio.part.root.panel.content.firstLevel.msglist.mvx.vb.ui.adapter.holder.AIOMsgViewHolder"
    private const val I_MSG_ITEM_CLASS = "com.tencent.aio.data.msglist.IMsgItem"
    private const val WATCH_AIO_MSG_ITEM_CLASS = "com.tencent.watch.aio_impl.data.WatchAIOMsgItem"
    private const val MSG_RECORD_CLASS = "com.tencent.qqnt.kernel.nativeinterface.MsgRecord"
    private const val AIO_CELL_GROUP_WIDGET_CLASS =
        "com.tencent.watch.aio_impl.ui.widget.AIOCellGroupWidget"
    private const val WATCH_AVATAR_VIEW_CLASS = "com.tencent.qqnt.avatar.WatchAvatarView"
    private const val AVATAR_FACADE_CLASS = "com.tencent.qqnt.avatar.AvatarFacade"
    private const val I_AVATAR_TARGET_CLASS = "com.tencent.qqnt.avatar.IAvatarTarget"
    private const val I_AVATAR_REQUEST_LOAD_CLASS = "com.tencent.qqnt.avatar.IAvatarRequestLoad"
    private const val AVATAR_OPTION_CLASS = "com.tencent.qqnt.avatar.AvatarOption"
    private const val AVATAR_OPTION_BUILDER_CLASS = "com.tencent.qqnt.avatar.AvatarOption\$Builder"
    private const val AVATAR_SIZE_TYPE_CLASS = "com.tencent.qqnt.avatar.AvatarSizeType"
    private const val COROUTINE_SCOPE_CLASS = "kotlinx.coroutines.CoroutineScope"
    private const val AVATAR_TAG = "QQRevived.AioAvatarHook.Avatar"
    private const val BINDING_PREFIX = "QQRevived.AioAvatarHook.Binding:"
    private const val AVATAR_SIZE_DP = 28
    private const val AVATAR_GAP_DP = 4

    private var installed = false

    override fun reset() {
        installed = false
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        if (installed) return
        val legacyHookEnabled = false
        if (!legacyHookEnabled) {
            installed = true
            module.logHook(Log.INFO, "AIO legacy avatar hook disabled")
            return
        }

        val targetClassLoader = requireClassLoader(classLoader)
        runCatching {
            val hookState = HookState.create(targetClassLoader)

            module.intercept(
                hookState.absMsgListVBClass.getDeclaredMethod(
                    "j",
                    hookState.aioMsgViewHolderClass,
                    Integer.TYPE,
                    hookState.iMsgItemClass,
                    List::class.java
                )
            ) {
                val result = proceed()
                runCatching {
                    bindAvatarAfterMessageBind(
                        module = module,
                        hookState = hookState,
                        msgListVB = thisObject,
                        holder = args.getOrNull(0),
                        data = args.getOrNull(2)
                    )
                }.onFailure {
                    module.logHook(Log.WARN, "AIO avatar bind skipped", it)
                }
                result
            }

            installed = true
            module.logHook(Log.INFO, "AIO avatar hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "AIO avatar hook skipped", it)
        }
    }

    private fun bindAvatarAfterMessageBind(
        module: ModuleMainKt,
        hookState: HookState,
        msgListVB: Any?,
        holder: Any?,
        data: Any?
    ) {
        if (msgListVB == null || holder == null || data == null) return
        if (!hookState.watchMsgItemClass.isInstance(data)) return

        val itemView = hookState.itemViewField.get(holder) as? ViewGroup ?: return
        if (!hookState.cellGroupWidgetClass.isInstance(itemView)) return

        val wrapper = hookState.longClickWrapperMethod.invoke(itemView) as? FrameLayout ?: return
        val msgRecord = hookState.msgRecordField.get(data) ?: return
        val senderUin = hookState.senderUinGetter.invoke(msgRecord) as? Long ?: return
        val senderUid = hookState.senderUidGetter.invoke(msgRecord) as? String
        val isSelf = hookState.isSelfMethod.invoke(data) as? Boolean ?: false

        if (senderUin <= 0L && senderUid.isNullOrBlank()) {
            wrapper.findAvatarView()?.visibility = View.GONE
            return
        }

        val avatar = wrapper.findAvatarView() ?: createAvatarView(hookState, wrapper.context).also {
            wrapper.addView(it)
        }

        val size = AVATAR_SIZE_DP.toPx(wrapper)
        val gap = AVATAR_GAP_DP.toPx(wrapper)
        if (isSelf) {
            wrapper.setPadding(0, 0, size + gap, 0)
        } else {
            wrapper.setPadding(size + gap, 0, 0, 0)
        }


        layoutAvatar(itemView, wrapper, avatar, isSelf)

        val bindingKey = "$BINDING_PREFIX${senderUid.orEmpty()}:$senderUin"
        if (avatar.contentDescription?.toString() == bindingKey) return

        avatar.contentDescription = bindingKey
        avatar.visibility = View.VISIBLE
        val scope = hookState.scopeGetter.invoke(msgListVB) ?: return
        loadAvatar(module, hookState, avatar, senderUid, senderUin, scope)
    }

    private fun createAvatarView(hookState: HookState, context: Context): View {
        val avatar = runCatching {
            hookState.avatarViewConstructor?.newInstance(context, null) as? View
        }.getOrNull() ?: ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP

        }
        return avatar.apply {
            tag = AVATAR_TAG
        }
    }

    private fun layoutAvatar(
        itemView: ViewGroup,
        wrapper: FrameLayout,
        avatar: View,
        isSelf: Boolean
    ) {
        itemView.clipChildren = false
        itemView.clipToPadding = false
        wrapper.clipChildren = false
        wrapper.clipToPadding = false

        val size = AVATAR_SIZE_DP.toPx(itemView)
        val gap = AVATAR_GAP_DP.toPx(itemView)
        val params = (avatar.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(size, size)

        params.width = size
        params.height = size
        params.gravity = Gravity.TOP or if (isSelf) Gravity.END else Gravity.START
        avatar.layoutParams = params
        avatar.translationX = if (isSelf) (size + gap).toFloat() else -(size + gap).toFloat()
        avatar.translationY = 0f
        avatar.isClickable = false
        avatar.isFocusable = false
        avatar.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        if (avatar.background == null) {
            avatar.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor("#FFE5E7EB".toColorInt())
            }
        }
    }

    private fun loadAvatar(
        module: ModuleMainKt,
        hookState: HookState,
        avatar: View,
        senderUid: String?,
        senderUin: Long,
        scope: Any
    ) {
        runCatching {
            val target = hookState.avatarTargetBuildContextMethod.invoke(
                hookState.avatarFacadeCompanion,
                avatar.context
            )
            val request = hookState.avatarTargetRequestMethod.invoke(target, avatar) ?: return
            val option = buildAvatarOption(hookState)
            val requestWithOption =
                hookState.avatarRequestOptionMethod.invoke(request, option) ?: request

            if (!senderUid.isNullOrBlank()) {
                hookState.avatarRequestLoadUidMethod.invoke(
                    requestWithOption,
                    senderUid,
                    senderUin,
                    scope
                )
            } else {
                hookState.avatarRequestLoadUinMethod.invoke(requestWithOption, senderUin, scope)
            }
        }.onFailure {
            module.logHook(Log.WARN, "AIO avatar load failed", it)
        }
    }

    private fun buildAvatarOption(hookState: HookState): Any {
        val builder = hookState.avatarOptionBuilderConstructor.newInstance()
        hookState.avatarSizeSmall?.let {
            hookState.avatarOptionSetSizeMethod?.invoke(builder, it)
        }
        return hookState.avatarOptionBuildMethod.invoke(builder)
            ?: error("AvatarOption.Builder.a() returned null")
    }

    private fun FrameLayout.findAvatarView(): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.tag == AVATAR_TAG) return child
        }
        return null
    }

    private fun Int.toPx(view: View): Int {
        return (this * view.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    }

    private fun findFieldInClassAndParents(clazz: Class<*>, name: String): Field {
        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            runCatching {
                return currentClass.getDeclaredField(name).apply { isAccessible = true }
            }
            currentClass = currentClass.superclass
        }
        error("Field $name not found in ${clazz.name}")
    }

    private data class HookState(
        val absMsgListVBClass: Class<*>,
        val aioMsgViewHolderClass: Class<*>,
        val iMsgItemClass: Class<*>,
        val watchMsgItemClass: Class<*>,
        val cellGroupWidgetClass: Class<*>,
        val itemViewField: Field,
        val msgRecordField: Field,
        val senderUinGetter: Method,
        val senderUidGetter: Method,
        val isSelfMethod: Method,
        val scopeGetter: Method,
        val longClickWrapperMethod: Method,
        val avatarViewConstructor: Constructor<*>?,
        val avatarFacadeCompanion: Any,
        val avatarTargetBuildContextMethod: Method,
        val avatarTargetRequestMethod: Method,
        val avatarRequestOptionMethod: Method,
        val avatarRequestLoadUidMethod: Method,
        val avatarRequestLoadUinMethod: Method,
        val avatarOptionBuilderConstructor: Constructor<*>,
        val avatarOptionBuildMethod: Method,
        val avatarOptionSetSizeMethod: Method?,
        val avatarSizeSmall: Any?
    ) {
        companion object {
            fun create(classLoader: ClassLoader): HookState {
                val absMsgListVBClass = classLoader.findTargetClass(ABS_MSG_LIST_VB_CLASS)
                val aioMsgViewHolderClass = classLoader.findTargetClass(AIO_MSG_VIEW_HOLDER_CLASS)
                val iMsgItemClass = classLoader.findTargetClass(I_MSG_ITEM_CLASS)
                val watchMsgItemClass = classLoader.findTargetClass(WATCH_AIO_MSG_ITEM_CLASS)
                val msgRecordClass = classLoader.findTargetClass(MSG_RECORD_CLASS)
                val cellGroupWidgetClass = classLoader.findTargetClass(AIO_CELL_GROUP_WIDGET_CLASS)
                val avatarFacadeClass = classLoader.findTargetClass(AVATAR_FACADE_CLASS)
                val avatarTargetClass = classLoader.findTargetClass(I_AVATAR_TARGET_CLASS)
                val avatarRequestLoadClass =
                    classLoader.findTargetClass(I_AVATAR_REQUEST_LOAD_CLASS)
                val avatarOptionClass = classLoader.findTargetClass(AVATAR_OPTION_CLASS)
                val avatarOptionBuilderClass =
                    classLoader.findTargetClass(AVATAR_OPTION_BUILDER_CLASS)
                val coroutineScopeClass = classLoader.findTargetClass(COROUTINE_SCOPE_CLASS)
                val avatarSizeTypeClass =
                    runCatching { classLoader.findTargetClass(AVATAR_SIZE_TYPE_CLASS) }.getOrNull()

                val avatarViewConstructor = runCatching {
                    classLoader.findTargetClass(WATCH_AVATAR_VIEW_CLASS)
                        .getDeclaredConstructor(Context::class.java, AttributeSet::class.java)
                        .apply { isAccessible = true }
                }.getOrNull()

                val avatarFacadeCompanion = requireNotNull(
                    avatarFacadeClass.getDeclaredField("a")
                        .apply { isAccessible = true }
                        .get(null)
                )

                val avatarOptionSetSizeMethod = avatarSizeTypeClass?.let {
                    runCatching {
                        avatarOptionBuilderClass.getDeclaredMethod("b", it).apply {
                            isAccessible = true
                        }
                    }.getOrNull()
                }

                val avatarSizeSmall = avatarSizeTypeClass?.enumConstants
                    ?.firstOrNull { (it as? Enum<*>)?.name == "SMALL" }

                return HookState(
                    absMsgListVBClass = absMsgListVBClass,
                    aioMsgViewHolderClass = aioMsgViewHolderClass,
                    iMsgItemClass = iMsgItemClass,
                    watchMsgItemClass = watchMsgItemClass,
                    cellGroupWidgetClass = cellGroupWidgetClass,
                    itemViewField = findFieldInClassAndParents(aioMsgViewHolderClass, "itemView"),
                    msgRecordField = findFieldInClassAndParents(watchMsgItemClass, "d"),
                    senderUinGetter = msgRecordClass.getDeclaredMethod("getSenderUin"),
                    senderUidGetter = msgRecordClass.getDeclaredMethod("getSenderUid"),
                    isSelfMethod = watchMsgItemClass.getDeclaredMethod("h"),
                    scopeGetter = absMsgListVBClass.getDeclaredMethod("S"),
                    longClickWrapperMethod =
                        cellGroupWidgetClass.getDeclaredMethod("getLongClickWrapper"),
                    avatarViewConstructor = avatarViewConstructor,
                    avatarFacadeCompanion = avatarFacadeCompanion,
                    avatarTargetBuildContextMethod =
                        avatarFacadeCompanion.javaClass.getDeclaredMethod("b", Context::class.java),
                    avatarTargetRequestMethod =
                        avatarTargetClass.getDeclaredMethod("b", View::class.java),
                    avatarRequestOptionMethod =
                        avatarRequestLoadClass.getDeclaredMethod("b", avatarOptionClass),
                    avatarRequestLoadUidMethod =
                        avatarRequestLoadClass.getDeclaredMethod(
                            "d",
                            String::class.java,
                            java.lang.Long.TYPE,
                            coroutineScopeClass
                        ),
                    avatarRequestLoadUinMethod =
                        avatarRequestLoadClass.getDeclaredMethod(
                            "e",
                            java.lang.Long.TYPE,
                            coroutineScopeClass
                        ),
                    avatarOptionBuilderConstructor =
                        avatarOptionBuilderClass.getDeclaredConstructor().apply {
                            isAccessible = true
                        },
                    avatarOptionBuildMethod =
                        avatarOptionBuilderClass.getDeclaredMethod("a").apply {
                            isAccessible = true
                        },
                    avatarOptionSetSizeMethod = avatarOptionSetSizeMethod,
                    avatarSizeSmall = avatarSizeSmall
                )
            }
        }
    }
}
