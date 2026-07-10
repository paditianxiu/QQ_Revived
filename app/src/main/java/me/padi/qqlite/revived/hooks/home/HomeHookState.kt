package me.padi.qqlite.revived.hooks.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import me.padi.qqlite.revived.hooks.common.findTargetClass
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import me.padi.qqlite.revived.shared.model.home.ContactRow
import me.padi.qqlite.revived.shared.model.home.PictureSpec
import me.padi.qqlite.revived.shared.model.home.PictureUrlSpec
import me.padi.qqlite.revived.shared.model.home.QZoneFeedRow
import me.padi.qqlite.revived.shared.model.home.RecentRow
import me.padi.qqlite.revived.shared.model.home.SelfActionRow
import me.padi.qqlite.revived.shared.model.home.normalizedImageValue
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import android.graphics.Color as AndroidColor

internal data class HomeHookState(
    val classLoader: ClassLoader,
    val listAdapterClass: Class<*>,
    val recyclerViewHolderClass: Class<*>,
    val mainFragmentClass: Class<*>,
    val mainViewPagerField: Field?,
    val recentItemBuilderClass: Class<*>,
    val recentItemClass: Class<*>,
    val baseChatHolderClass: Class<*>,
    val recentClickListener: Any?,
    val recentClickMethod: Method?,
    val recentLongClickMethod: Method?,
    val contactAdapterClass: Class<*>,
    val contactAdapterFragmentField: Field?,
    val contactUseClickConstructor: Constructor<*>?,
    val contactExtClickConstructor: Constructor<*>?,
    val contactSendIntentMethod: Method?,
    val contactBaseItemClass: Class<*>,
    val selfFragmentClass: Class<*>,
    val selfOperationItemClass: Class<*>,
    val settingItemClassesField: Field?,
    val selfOperationTitleMethod: Method?,
    val viewOnClickMethod: Method?,
    val qZoneMainFrameClass: Class<*>,
    val qZoneElementClickMethod: Method?,
    val qZoneFeedEngineMethod: Method?,
    val qZoneLoadMoreMethod: Method?,
    val businessFeedDataClass: Class<*>,
    val watchAvatarViewClass: Class<*>?,
    val watchAvatarConstructor: Constructor<*>?,
    val avatarFacadeCompanion: Any?,
    val avatarFacadeContextMethod: Method?,
    val avatarFacadeFragmentMethod: Method?,
    val avatarTargetBindMethod: Method?,
    val avatarRequestOptionMethod: Method?,
    val avatarOptionBuilderClass: Class<*>?,
    val avatarOptionBuilderSizeMethod: Method?,
    val avatarOptionBuilderBuildMethod: Method?,
    val avatarSizeType: Any?,
    val avatarRequestLoadUidMethod: Method?,
    val avatarRequestLoadUinMethod: Method?,
    val qZoneUinToUidMethod: Method?,
    val lifecycleOwnerKtClass: Class<*>?,
    val lifecycleScopeMethod: Method?,
    val selfProfileServiceClass: Class<*>
) {
    fun toRecentRow(item: Any, fragment: Any?, orderHint: Int = Int.MAX_VALUE): RecentRow {
        val title = item.findFieldValue("n")?.findFieldValue("d")?.toString().orEmpty()
        val summary = item.findFieldValue("p")?.findFieldValue("e")?.toString().orEmpty()
        val chatType = (item.findFieldValue("k") as? Number)?.toInt() ?: 0
        val chatUid = item.findFieldValue("l")?.toString().orEmpty()
        val avatarUin =
            (item.findFieldValue("A") as? Number)?.toLong() ?: chatUid.toLongOrNull() ?: 0L
        return RecentRow(
            key = (item.findFieldValue("j") as? Number)?.toLong() ?: item.hashCode().toLong(),
            title = title.ifBlank { item.toString() },
            summary = summary,
            time = item.findFieldValue("o")?.toString().orEmpty(),
            sortTime = item.extractRecentSortTime(),
            orderHint = orderHint,
            unread = (item.findFieldValue("q")?.findFieldValue("b") as? Number)?.toLong() ?: 0L,
            pinned = item.findFieldValue("s") as? Boolean == true,
            avatar = AvatarSpec(chatType, chatUid, avatarUin, WeakReference(fragment)),
            rawItem = item,
            chatType = chatType,
            peerId = chatUid,
            peerUin = avatarUin
        )
    }

    fun toContactRow(item: Any, fragment: Any?): ContactRow {
        val title = item.invokeNoArg("getTitle")?.toString().orEmpty()
        val type = when {
            item.javaClass.name.endsWith("GroupItem") -> "群聊"
            item.javaClass.name.endsWith("ContactItem") -> "好友"
            item.javaClass.name.endsWith("ContactNotifyItem") -> "通知"
            item.javaClass.name.endsWith("AddFriendItem") -> "添加"
            else -> "联系人"
        }
        return ContactRow(
            key = item.toString(),
            title = title.ifBlank { type },
            type = type,
            unread = (item.invokeNoArg("c") as? Number)?.toInt() ?: 0,
            hasExtIcon = (item.invokeNoArg("a") as? Number)?.toInt() != -1,
            avatar = item.toContactAvatarSpec(fragment),
            rawItem = item,
            fragmentRef = WeakReference(fragment)
        )
    }

    fun toQZoneFeedRow(feed: Any, fragment: Any?, index: Int): QZoneFeedRow {
        val user = feed.extractQZoneUser()
        val commInfo = feed.invokeNoArg("getFeedCommInfo")
        val content =
            feed.invokeNoArg("getCellSummaryV2")?.findFieldValue("summary")?.toString()
                .orEmpty()
        val uin = (user?.findFieldValue("uin") as? Number)?.toLong() ?: 0L
        val uid = user?.findFieldValue("uid")?.toString().orEmpty()

        return QZoneFeedRow(
            key = commInfo?.findFieldValue("feedskey")?.toString()
                ?.takeIf { it.isNotBlank() } ?: "${feed.hashCode()}:$index",
            title = user?.findFieldValue("nickName")?.toString()?.takeIf { it.isNotBlank() }
                ?: user?.findFieldValue("nameSeperate")?.toString()?.takeIf { it.isNotBlank() }
                ?: "动态",
            time = commInfo?.invokeNoArg("getDisplayTimeString")?.toString().orEmpty(),
            summary = content,
            avatar = AvatarSpec(
                1,
                uid,
                uin,
                WeakReference(fragment),
                user?.qZoneAvatarUrl()?.takeIf { uid.isBlank() && uin <= 0L }),
            pictures = feed.extractPictures(),
            totalPictureCount = feed.extractPictureCount(),
            rawFeed = feed,
            fragmentRef = WeakReference(fragment),
            index = index
        )
    }

    private fun Any.extractQZoneUser(): Any? {
        val cellUserInfo = invokeNoArg("getCellUserInfo") ?: findFieldValue("cellUserInfo")
        return cellUserInfo?.invokeNoArg("getUserV2") ?: cellUserInfo?.findFieldValue("user")
        ?: invokeNoArg("getUser")
    }

    private fun Any.extractRecentSortTime(): Long {
        val originData = invokeNoArg("getOriginData") ?: findFieldValue("i")
        listOf(
            originData?.invokeNoArg("getSortField"),
            originData?.findFieldValue("sortField"),
            originData?.invokeNoArg("getMsgTime"),
            originData?.findFieldValue("msgTime"),
            originData?.invokeNoArg("getDraftTime"),
            originData?.findFieldValue("draftTime"),
            originData?.invokeNoArg("getTopFlagTime"),
            originData?.findFieldValue("topFlagTime")
        ).firstNotNullOfOrNull { it.toEpochMillisOrNull() }?.let { return it }

        val directCandidates = listOf(
            invokeNoArg("getTimeStamp"),
            invokeNoArg("getTimestamp"),
            invokeNoArg("getSortTime"),
            invokeNoArg("getLastMsgTime"),
            invokeNoArg("getLastTime"),
            invokeNoArg("getTime"),
            findFieldValue("timestamp"),
            findFieldValue("timeStamp"),
            findFieldValue("sortTime"),
            findFieldValue("lastMsgTime"),
            findFieldValue("lastTime"),
            findFieldValue("m"),
            findFieldValue("r"),
            findFieldValue("t")
        )
        directCandidates.firstNotNullOfOrNull { it.toEpochMillisOrNull() }?.let { return it }

        val nestedCandidates = listOf(
            findFieldValue("n"),
            findFieldValue("p"),
            findFieldValue("q"),
            findFieldValue("commInfo"),
            invokeNoArg("getCommInfo")
        )
        nestedCandidates.firstNotNullOfOrNull { nested ->
            nested?.let {
                listOf(
                    it.invokeNoArg("getTimeStamp"),
                    it.invokeNoArg("getTimestamp"),
                    it.invokeNoArg("getSortTime"),
                    it.invokeNoArg("getLastMsgTime"),
                    it.invokeNoArg("getTime"),
                    it.findFieldValue("timestamp"),
                    it.findFieldValue("timeStamp"),
                    it.findFieldValue("sortTime"),
                    it.findFieldValue("lastMsgTime"),
                    it.findFieldValue("time"),
                    it.findFieldValue("a"),
                    it.findFieldValue("b")
                ).firstNotNullOfOrNull { value -> value.toEpochMillisOrNull() }
            }
        }?.let { return it }

        return 0L
    }

    private fun Any.qZoneAvatarUrl(): String? {
        return listOf(
            findFieldValue("avatarPath"),
            findFieldValue("logo"),
            findFieldValue("feedAvatarDecorationUrl"),
            findFieldValue("hostCustomIconUrl"),
            findFieldValue("guestCustomIconUrl")
        ).firstNotNullOfOrNull { value ->
            value?.toString()?.takeIf { it.isNotBlank() }
        }
    }
    fun createAvatarView(context: Context, spec: AvatarSpec?): View {
        val avatar = runCatching {
            watchAvatarConstructor?.newInstance(context, null, 0, 0) as? View
        }.getOrNull() ?: ImageView(context)

        (avatar as? ImageView)?.scaleType = ImageView.ScaleType.CENTER_CROP
        avatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(AndroidColor.rgb(226, 232, 240))
        }
        spec?.let { loadAvatar(avatar, it) }
        return avatar
    }

    fun loadAvatar(view: View, spec: AvatarSpec?) {
        if (spec == null) return
        val fragment = spec.fragmentRef?.get()
        val bindingKey =
            "${spec.stableKey()}:${fragment?.let { System.identityHashCode(it) } ?: 0}"
        if (view.getTag(AVATAR_BINDING_KEY) == bindingKey) return
        view.setTag(AVATAR_BINDING_KEY, bindingKey)
        runCatching {
            val companion = avatarFacadeCompanion ?: return
            val contextMethod = avatarFacadeContextMethod
            val fragmentMethod = avatarFacadeFragmentMethod
            val bindMethod = avatarTargetBindMethod ?: return
            val optionMethod = avatarRequestOptionMethod ?: return
            val builderClass = avatarOptionBuilderClass ?: return
            val optionBuilder = builderClass.getDeclaredConstructor().newInstance()
            avatarOptionBuilderSizeMethod?.invoke(optionBuilder, avatarSizeType)
            val option = avatarOptionBuilderBuildMethod?.invoke(optionBuilder)
            val target = if (fragment != null && fragmentMethod != null) {
                fragmentMethod.invoke(companion, fragment)
            } else {
                contextMethod?.invoke(companion, view.context)
            } ?: return
            val request = bindMethod.invoke(target, view)
            val requestWithOption = if (option != null) {
                optionMethod.invoke(request, option) ?: request
            } else {
                request
            }
            val scope = fragment?.let { lifecycleScopeMethod?.invoke(null, it) } ?: return
            if (spec.chatType == 2) {
                avatarRequestLoadUinMethod?.invoke(requestWithOption, spec.uin, scope)
            } else {
                val uid =
                    spec.uid.takeIf { it.isNotBlank() } ?: resolveUidFromUin(spec.uin) ?: ""
                if (uid.isNotBlank() && avatarRequestLoadUidMethod != null) {
                    avatarRequestLoadUidMethod.invoke(requestWithOption, uid, spec.uin, scope)
                } else if (spec.uin > 0L) {
                    avatarRequestLoadUinMethod?.invoke(requestWithOption, spec.uin, scope)
                }
            }
        }
    }

    fun requestQZoneLoadMore(fragment: Any?): Boolean {
        if (fragment == null || !qZoneMainFrameClass.isInstance(fragment)) return false
        return runCatching {
            val engine = qZoneFeedEngineMethod?.invoke(fragment) ?: return false
            val loadMoreMethod = qZoneLoadMoreMethod ?: return false
            loadMoreMethod.invoke(engine)
            true
        }.getOrDefault(false)
    }

    private fun resolveUidFromUin(uin: Long): String? {
        if (uin <= 0L) return null
        return runCatching {
            qZoneUinToUidMethod?.invoke(null, uin)?.toString()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun Any.toContactAvatarSpec(fragment: Any?): AvatarSpec? {
        return when {
            javaClass.name.endsWith("GroupItem") -> {
                val groupId = (findFieldValue("a") as? Number)?.toLong() ?: return null
                AvatarSpec(2, groupId.toString(), groupId, WeakReference(fragment))
            }

            javaClass.name.endsWith("ContactItem") -> {
                val uin = findFieldValue("a")?.toString().orEmpty()
                val uid = findFieldValue("b")?.toString().orEmpty()
                AvatarSpec(1, uid, uin.toLongOrNull() ?: 0L, WeakReference(fragment))
            }

            else -> null
        }
    }

    private fun Any.extractPictures(): List<PictureSpec> {
        val pictureInfo = invokeNoArg("getPictureInfo") ?: return emptyList()
        val pics = pictureInfo.findFieldValue("pics") as? List<*> ?: return emptyList()
        return pics.mapNotNull { item ->
            item ?: return@mapNotNull null
            val local = item.findFieldValue("localFileUrl")?.toString()
                ?.takeIf { it.isNotBlank() && File(it).exists() }.orEmpty()
            val url = item.preferredPictureUrl()
            val resolved = local.takeIf { it.isNotBlank() } ?: url?.value
            resolved?.let {
                PictureSpec(
                    value = it,
                    isLocalFile = local.isNotBlank(),
                    width = url?.width ?: 0,
                    height = url?.height ?: 0
                )
            }
        }
    }

    private fun Any.extractPictureCount(): Int {
        val pictureInfo = invokeNoArg("getPictureInfo") ?: return 0
        return (pictureInfo.findFieldValue("uploadnum") as? Number)?.toInt()
            ?: (pictureInfo.findFieldValue("pics") as? List<*>)?.size ?: 0
    }

    private fun Any.preferredPictureUrl(): PictureUrlSpec? {
        return pictureUrlCandidates().distinctBy { it.value }
            .firstOrNull { it.value.normalizedImageValue() != null }
    }

    private fun Any.pictureUrlCandidates(): List<PictureUrlSpec> {
        return listOfNotNull(
            findFieldValue("currentUrl"),
            invokeNoArg("getCurrentUrl"),
            invokeNoArg("getPicUrlDec"),
            findFieldValue("bigUrl"),
            findFieldValue("originUrl")
        ).mapNotNull { it.toPictureUrlSpec() }
    }

    private fun Any.toPictureUrlSpec(): PictureUrlSpec? {
        val url = findFieldValue("url")?.toString()?.takeIf { it.isNotBlank() } ?: return null
        return PictureUrlSpec(
            value = url,
            width = (findFieldValue("width") as? Number)?.toInt() ?: 0,
            height = (findFieldValue("height") as? Number)?.toInt() ?: 0
        )
    }

    fun createSelfRows(fragment: Any?): List<SelfActionRow> {
        if (fragment == null) return emptyList()
        val classes = settingItemClassesField?.get(null) as? List<*> ?: return emptyList()
        return classes.mapNotNull { clazz ->
            val action =
                (clazz as? Class<*>)?.getConstructor(classLoader.findTargetClass(FRAGMENT_CLASS))
                    ?.newInstance(fragment) ?: return@mapNotNull null
            val title = selfOperationTitleMethod?.invoke(action)?.let { id ->
                runCatching {
                    fragment.javaClass.getMethod("getString", Integer.TYPE).invoke(fragment, id)
                }.getOrNull()
            }?.toString() ?: action.javaClass.simpleName
            SelfActionRow(title, action)
        }
    }

    fun sendContactIntent(fragment: Any?, constructor: Constructor<*>?, item: Any) {
        if (fragment == null || constructor == null || contactSendIntentMethod == null) return
        val intent = constructor.newInstance(item)
        contactSendIntentMethod.invoke(fragment, intent)
    }

    fun openRecentFromMainActivity(context: Context?, row: RecentRow): Boolean {
        val activity = context.findActivity() ?: return false
        val peerId = row.peerId.takeIf { it.isNotBlank() }
            ?: row.avatar?.uid?.takeIf { it.isNotBlank() }
            ?: return false
        val peerUin = row.peerUin.takeIf { it > 0L }
            ?: row.avatar?.uin?.takeIf { it > 0L }
            ?: peerId.toLongOrNull()
            ?: return false
        val chatType = row.chatType.takeIf { it >= 0 } ?: row.avatar?.chatType ?: 1
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("open_chatfragment", true)
            putExtra("key_chat_type", chatType)
            putExtra("key_peerId", peerId)
            putExtra("key_chat_name", row.title)
            putExtra("key_peerUin", peerUin)
        }

        return runCatching {
            val onNewIntent = activity.javaClass.methods.firstOrNull {
                it.name == "onNewIntent" &&
                    it.parameterTypes.contentEquals(arrayOf(Intent::class.java))
            }
            if (onNewIntent != null) {
                onNewIntent.invoke(activity, intent)
            } else {
                activity.startActivity(intent)
            }
            true
        }.onFailure {
            Log.w(
                HOME_HOOK_TAG,
                "openRecentFromMainActivity failed title=${row.title} type=$chatType peer=$peerId uin=$peerUin",
                it
            )
        }.getOrDefault(false)
    }

    companion object {
        fun create(classLoader: ClassLoader): HomeHookState {
            val mainFragmentClass = classLoader.findTargetClass(MAIN_FRAGMENT_CLASS)
            val recentItemBuilderClass = classLoader.findTargetClass(RECENT_ITEM_BUILDER_CLASS)
            val recentItemClass = classLoader.findTargetClass(RECENT_ITEM_CLASS)
            val contactAdapterClass = classLoader.findTargetClass(CONTACT_ADAPTER_CLASS)
            val contactBaseItemClass = classLoader.findTargetClass(CONTACT_BASE_ITEM_CLASS)
            val selfOperationItemClass = classLoader.findTargetClass(SELF_OPERATION_ITEM_CLASS)
            val qZoneMainFrameClass = classLoader.findTargetClass(QZONE_MAIN_FRAME_CLASS)
            val businessFeedDataClass = classLoader.findTargetClass(BUSINESS_FEED_DATA_CLASS)
            val watchAvatarViewClass = classLoader.findOptionalClass(WATCH_AVATAR_VIEW_CLASS)
            val avatarFacadeClass = classLoader.findOptionalClass(AVATAR_FACADE_CLASS)
            val avatarOptionBuilderClass =
                classLoader.findOptionalClass(AVATAR_OPTION_BUILDER_CLASS)
            val avatarSizeTypeClass = classLoader.findOptionalClass(AVATAR_SIZE_TYPE_CLASS)
            val avatarTargetClass =
                classLoader.findOptionalClass("com.tencent.qqnt.avatar.IAvatarTarget")
            val avatarRequestLoadClass =
                classLoader.findOptionalClass("com.tencent.qqnt.avatar.IAvatarRequestLoad")
            val avatarOptionClass =
                classLoader.findOptionalClass("com.tencent.qqnt.avatar.AvatarOption")
            val qZoneUinToUidClass = classLoader.findOptionalClass(QZONE_UIN_TO_UID_CLASS)
            val coroutineScopeClass =
                classLoader.findOptionalClass("kotlinx.coroutines.CoroutineScope")
            val lifecycleOwnerClass =
                classLoader.findOptionalClass("androidx.lifecycle.LifecycleOwner")
            val lifecycleOwnerKtClass =
                classLoader.findOptionalClass("androidx.lifecycle.LifecycleOwnerKt")
            val viewClass = View::class.java
            val avatarFacadeCompanion =
                avatarFacadeClass?.getDeclaredField("a")?.apply { isAccessible = true }
                    ?.get(null)

            val recentSingleton =
                recentItemBuilderClass.getDeclaredField("c").apply { isAccessible = true }
                    .get(null)
            val recentClickListener =
                recentItemBuilderClass.getDeclaredField("e").apply { isAccessible = true }
                    .get(recentSingleton)

            return HomeHookState(
                classLoader = classLoader,
                listAdapterClass = classLoader.findTargetClass(
                    "androidx.recyclerview.widget.ListAdapter"
                ),
                recyclerViewHolderClass = classLoader.findTargetClass(
                    "androidx.recyclerview.widget.RecyclerView\$ViewHolder"
                ),
                mainFragmentClass = mainFragmentClass,
                mainViewPagerField = findOptionalField(mainFragmentClass, "g"),
                recentItemBuilderClass = recentItemBuilderClass,
                recentItemClass = recentItemClass,
                baseChatHolderClass = classLoader.findTargetClass(BASE_CHAT_HOLDER_CLASS),
                recentClickListener = recentClickListener,
                recentClickMethod = recentClickListener.javaClass.getDeclaredMethod(
                    "b", recentItemClass
                ).apply { isAccessible = true },
                recentLongClickMethod = recentClickListener.javaClass.getDeclaredMethod(
                    "a", recentItemClass
                ).apply { isAccessible = true },
                contactAdapterClass = contactAdapterClass,
                contactAdapterFragmentField = findOptionalField(contactAdapterClass, "a"),
                contactUseClickConstructor = classLoader.findTargetClass(
                    CONTACT_INTENT_USE_CLICK_CLASS
                ).getDeclaredConstructor(contactBaseItemClass).apply { isAccessible = true },
                contactExtClickConstructor = classLoader.findTargetClass(
                    CONTACT_INTENT_EXT_CLICK_CLASS
                ).getDeclaredConstructor(contactBaseItemClass).apply { isAccessible = true },
                contactSendIntentMethod = classLoader.findTargetClass(
                    "com.tencent.qqnt.watch.contact.ui.ContactListFragment"
                ).getDeclaredMethod(
                    "g0",
                    classLoader.findTargetClass("com.tencent.qqnt.watch.contact.mvi.ContactListIntent")
                ).apply { isAccessible = true },
                contactBaseItemClass = contactBaseItemClass,
                selfFragmentClass = classLoader.findTargetClass(SELF_FRAGMENT_CLASS),
                selfOperationItemClass = selfOperationItemClass,
                settingItemClassesField = classLoader.findTargetClass(
                    SETTING_ITEM_PROVIDER_CLASS
                ).getDeclaredField("b").apply { isAccessible = true },
                selfOperationTitleMethod = selfOperationItemClass.getDeclaredMethod("b")
                    .apply { isAccessible = true },
                viewOnClickMethod = View.OnClickListener::class.java.getDeclaredMethod(
                    "onClick", viewClass
                ),
                qZoneMainFrameClass = qZoneMainFrameClass,
                qZoneElementClickMethod = qZoneMainFrameClass.getDeclaredMethod(
                    "A",
                    viewClass,
                    Integer.TYPE,
                    businessFeedDataClass,
                    Integer.TYPE,
                    Any::class.java
                ).apply { isAccessible = true },
                qZoneFeedEngineMethod = findMethodInHierarchy(qZoneMainFrameClass, "g0"),
                qZoneLoadMoreMethod = classLoader.findOptionalClass(
                    "com.tencent.watch.qzone_impl.common.activities.QZoneBaseFeedEngine"
                )?.let { findMethodInHierarchy(it, "a") },
                businessFeedDataClass = businessFeedDataClass,
                watchAvatarViewClass = watchAvatarViewClass,
                watchAvatarConstructor = watchAvatarViewClass?.getDeclaredConstructor(
                    Context::class.java,
                    android.util.AttributeSet::class.java,
                    Integer.TYPE,
                    Integer.TYPE
                )?.apply { isAccessible = true },
                avatarFacadeCompanion = avatarFacadeCompanion,
                avatarFacadeContextMethod = avatarFacadeCompanion?.javaClass?.methods?.firstOrNull {
                    it.name == "b" && it.parameterTypes.size == 1 && it.parameterTypes[0].isAssignableFrom(
                        Context::class.java
                    )
                }?.apply { isAccessible = true },
                avatarFacadeFragmentMethod = avatarFacadeCompanion?.javaClass?.methods?.firstOrNull {
                    it.name == "c" && it.parameterTypes.size == 1 && it.parameterTypes[0].name == FRAGMENT_CLASS
                }?.apply { isAccessible = true },
                avatarTargetBindMethod = avatarTargetClass?.getDeclaredMethod("b", viewClass)
                    ?.apply { isAccessible = true },
                avatarRequestOptionMethod = avatarRequestLoadClass?.let { requestClass ->
                    avatarOptionClass?.let {
                        requestClass.getDeclaredMethod("b", it).apply { isAccessible = true }
                    }
                },
                avatarOptionBuilderClass = avatarOptionBuilderClass,
                avatarOptionBuilderSizeMethod = avatarOptionBuilderClass?.methods?.firstOrNull {
                    it.name == "b" && it.parameterTypes.size == 1 && it.parameterTypes[0].name == AVATAR_SIZE_TYPE_CLASS
                }?.apply { isAccessible = true },
                avatarOptionBuilderBuildMethod = avatarOptionBuilderClass?.methods?.firstOrNull {
                    it.name == "a" && it.parameterTypes.isEmpty()
                }?.apply { isAccessible = true },
                avatarSizeType = avatarSizeTypeClass?.enumConstants?.firstOrNull { (it as? Enum<*>)?.name == "SMALL" }
                    ?: avatarSizeTypeClass?.getDeclaredField("b")?.apply { isAccessible = true }
                        ?.get(null),
                avatarRequestLoadUidMethod = avatarRequestLoadClass?.let {
                    coroutineScopeClass?.let { scopeClass ->
                        it.getDeclaredMethod(
                            "d", String::class.java, java.lang.Long.TYPE, scopeClass
                        ).apply { isAccessible = true }
                    }
                },
                avatarRequestLoadUinMethod = avatarRequestLoadClass?.let {
                    coroutineScopeClass?.let { scopeClass ->
                        it.getDeclaredMethod(
                            "e", java.lang.Long.TYPE, scopeClass
                        ).apply { isAccessible = true }
                    }
                },
                qZoneUinToUidMethod = qZoneUinToUidClass?.getDeclaredMethod(
                    "a", java.lang.Long.TYPE
                )?.apply { isAccessible = true },
                lifecycleOwnerKtClass = lifecycleOwnerKtClass,
                lifecycleScopeMethod = lifecycleOwnerClass?.let { ownerClass ->
                    lifecycleOwnerKtClass?.getDeclaredMethod("a", ownerClass)?.apply {
                        isAccessible = true
                    }
                },
                selfProfileServiceClass = classLoader.findTargetClass(SELF_PROFILE_SERVICE_CLASS)
            )
        }
    }

}

private fun Any?.toEpochMillisOrNull(): Long? {
    val raw = when (this) {
        is Number -> toLong()
        is String -> trim().toLongOrNull()
        else -> null
    } ?: return null
    if (raw <= 0L) return null
    return if (raw < 100_000_000_000L) raw * 1000L else raw
}

private tailrec fun Context?.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private const val HOME_HOOK_TAG = "QQRevived.Home"
