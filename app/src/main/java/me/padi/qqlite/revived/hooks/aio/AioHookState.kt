package me.padi.qqlite.revived.hooks.aio

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import me.padi.qqlite.revived.hooks.common.findTargetClass
import me.padi.qqlite.revived.shared.model.aio.AioEmotionCategory
import me.padi.qqlite.revived.shared.model.aio.AioEmotionItem
import me.padi.qqlite.revived.shared.model.aio.AioEmotionKind
import me.padi.qqlite.revived.shared.model.aio.AioForwardPreview
import me.padi.qqlite.revived.shared.model.aio.AioMediaSpec
import me.padi.qqlite.revived.shared.model.aio.AioMessage
import me.padi.qqlite.revived.shared.model.aio.AioMessageBadge
import me.padi.qqlite.revived.shared.model.aio.AioMessageKind
import me.padi.qqlite.revived.shared.model.aio.AioPeer
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.text.DateFormat
import java.util.Date

internal data class AioHookState(
    val classLoader: ClassLoader,
    val watchAIOFragmentClass: Class<*>,
    val fragmentClass: Class<*>,
    val requireArgumentsMethod: Method,
    val viewPagerField: Field?,
    val absMsgListVBClass: Class<*>,
    val listUIOperationApiImplClass: Class<*>?,
    val listSubmitMethod: Method?,
    val listReadOnlyListMethod: Method?,
    val listRecyclerViewMethod: Method?,
    val aioMsgViewHolderClass: Class<*>,
    val iMsgItemClass: Class<*>,
    val watchMsgItemClass: Class<*>,
    val nickNameAbilityClass: Class<*>?,
    val nickNameAbilityInjectMethod: Method?,
    val nickNameAbilityMemberInfoCacheField: Field?,
    val watchMsgListRepoClass: Class<*>?,
    val watchAioListVbClass: Class<*>?,
    val watchAioListCreateViewMethod: Method?,
    val inputBarControllerField: Field?,
    val inputBarIconFaceField: Field?,
    val inputBarImeContentField: Field?,
    val inputBarEmotionClickConstructor: Constructor<*>?,
    val inputBarIntentDispatchMethod: Method?,
    val watchAioListVmClass: Class<*>?,
    val watchAioListVmSendElementsMethod: Method?,
    val kernelGroupServiceUtilCompanion: Any?,
    val kernelGroupServiceGetterMethod: Method?,
    val groupMemberListCallbackClass: Class<*>?,
    val groupMemberListResultInfosField: Field?,
    val imeTextUtilCompanion: Any?,
    val imeTextToElementsMethod: Method?,
    val repoConstructor: Constructor<*>?,
    val repoLoadOlderMethod: Method?,
    val repoAddLocalSendMethod: Method?,
    val repoClearMethod: Method?,
    val repoReceiveMethod: Method?,
    val repoDeleteMethod: Method?,
    val repoUpdateMethod: Method?,
    val topGestureLayoutClass: Class<*>?,
    val topGestureInterceptMethod: Method?,
    val topGestureSetInterceptTouchFlagMethod: Method?,
    val watchAioSwipeEnableMethod: Method?,
    val itemViewField: Field,
    val msgRecordField: Field,
    val isSelfMethod: Method,
    val msgIdGetter: Method,
    val msgSeqGetter: Method,
    val msgRandomGetter: Method,
    val msgTimeGetter: Method,
    val timeStampGetter: Method,
    val msgTypeGetter: Method,
    val senderUidGetter: Method,
    val senderUinGetter: Method,
    val sendNickNameGetter: Method?,
    val sendMemberNameGetter: Method?,
    val fromGuildRoleInfoGetter: Method?,
    val fromChannelRoleInfoGetter: Method?,
    val levelRoleInfoGetter: Method?,
    val elementsGetter: Method,
    val watchMsgShowNickNameField: Field?,
    val watchMsgShowTimeStampFlagField: Field?,
    val watchMsgMemberInfoField: Field?,
    val memberInfoCardNameGetter: Method?,
    val memberInfoRemarkGetter: Method?,
    val memberInfoNickGetter: Method?,
    val memberInfoSpecialTitleGetter: Method?,
    val memberInfoRoleGetter: Method?,
    val memberInfoLevelGetter: Method?,
    val fromRoleInfoNameGetter: Method?,
    val fromRoleInfoColorGetter: Method?,
    val elementTypeGetter: Method,
    val textElementGetter: Method,
    val picElementGetter: Method,
    val multiForwardElementGetter: Method,
    val avRecordElementGetter: Method,
    val videoElementGetter: Method,
    val pttElementGetter: Method,
    val fileElementGetter: Method,
    val grayTipElementGetter: Method,
    val textContentGetter: Method,
    val multiForwardXmlContentGetter: Method,
    val multiForwardFileNameGetter: Method,
    val avRecordTextGetter: Method,
    val avRecordTypeGetter: Method,
    val picSourcePathGetter: Method,
    val picThumbPathGetter: Method,
    val picOriginUrlGetter: Method,
    val picWidthGetter: Method,
    val picHeightGetter: Method,
    val picFileNameGetter: Method,
    val watchPicThumbPathMethod: Method?,
    val watchPicOriginPathMethod: Method?,
    val videoFileNameGetter: Method,
    val videoFilePathGetter: Method,
    val videoFileSizeGetter: Method,
    val videoFileTimeGetter: Method,
    val videoThumbPathGetter: Method,
    val videoThumbWidthGetter: Method,
    val videoThumbHeightGetter: Method,
    val pttDurationGetter: Method,
    val pttTextGetter: Method?,
    val pttFilePathGetter: Method?,
    val fileNameGetter: Method,
    val filePathGetter: Method?,
    val fileSizeGetter: Method,
    val fileThumbGetter: Method?,
    val fileVideoDurationGetter: Method?,
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
    val lifecycleScopeMethod: Method?
) {
    fun readPeerInfo(fragment: Any?): AioPeer? {
        if (fragment == null) return null
        return runCatching {
            val arguments = requireArgumentsMethod.invoke(fragment) as? Bundle ?: return null
            val peerId = arguments.getString(KEY_PEER_ID)?.takeIf { it.isNotBlank() }
                ?: return null
            val chatType = arguments.getInt(KEY_CHAT_TYPE)
            val chatNick = arguments.getString(KEY_CHAT_NICK)?.takeIf { it.isNotBlank() }
            val chatUin =
                arguments.getString(KEY_CHAT_UIN)?.toLongOrNull()
                    ?: peerId.toLongOrNull()
                    ?: 0L
            AioPeer(
                peerId = peerId,
                chatType = chatType,
                chatNick = chatNick,
                chatUin = chatUin
            )
        }.getOrNull()
    }

    fun toPeerAvatar(peer: AioPeer, fragment: Any?): AvatarSpec? {
        if (peer.peerId.isBlank() && peer.chatUin <= 0L) return null
        return AvatarSpec(
            chatType = peer.chatType,
            uid = peer.peerId,
            uin = peer.chatUin,
            fragmentRef = WeakReference(fragment)
        )
    }

    fun toMessage(
        data: Any?,
        holder: Any?,
        hostFragment: Any?
    ): AioMessage? {
        if (data == null || !watchMsgItemClass.isInstance(data)) return null
        return runCatching {
            val itemView = holder?.let { itemViewField.get(it) as? View }
            val msgRecord = msgRecordField.get(data) ?: return null
            val msgId = msgIdGetter.invokeLong(msgRecord)
            val msgSeq = msgSeqGetter.invokeLong(msgRecord)
            val msgRandom = msgRandomGetter.invokeLong(msgRecord)
            val msgTime = timeStampGetter.invokeLong(msgRecord).takeIf { it > 0L }
                ?: msgTimeGetter.invokeLong(msgRecord)
            val msgType = msgTypeGetter.invokeInt(msgRecord)
            val senderUid = senderUidGetter.invokeString(msgRecord).orEmpty()
            val senderUin = senderUinGetter.invokeLong(msgRecord)
            val isSelf = isSelfMethod.invoke(data) as? Boolean == true
            val senderName = readSenderName(data, msgRecord, senderUid)
            val badges = readSenderBadges(data, msgRecord)
            val showTimeDivider = readShowTimeDivider(data)
            val parsed = parseElements(msgRecord)
            val rawKind = parsed.rawKind.takeIf { it != AioMessageKind.Unsupported }
                ?: AioMessageKind.fromMsgType(msgType)
            val hostTipText = if (parsed.renderKind == AioMessageKind.Tip) {
                readHostGrayTipText(data)
            } else {
                null
            }
            val key = buildMessageKey(msgId, msgSeq, msgRandom, msgTime, senderUid, senderUin)

            AioMessage(
                key = key,
                msgId = msgId,
                msgSeq = msgSeq,
                msgRandom = msgRandom,
                msgTime = msgTime,
                senderUid = senderUid,
                senderUin = senderUin,
                isSelf = isSelf,
                renderKind = parsed.renderKind,
                rawKind = rawKind,
                text = (hostTipText ?: parsed.text).ifBlank {
                    defaultMessageText(parsed.renderKind, rawKind, msgType)
                },
                senderName = senderName,
                badges = badges,
                showTimeDivider = showTimeDivider,
                timeDividerText = formatTimeDividerText(msgTime),
                forwardPreview = parsed.forwardPreview,
                media = parsed.media,
                avatar = senderAvatar(senderUid, senderUin, hostFragment),
                itemViewRef = itemView?.let { WeakReference(it) }
            )
        }.getOrNull()
    }

    fun toMessages(rows: Iterable<*>, hostFragment: Any?): List<AioMessage> {
        return rows.mapNotNull { row ->
            toMessage(data = row, holder = null, hostFragment = hostFragment)
        }
    }

    fun isWatchMsgListRepo(value: Any?): Boolean {
        return value != null && watchMsgListRepoClass?.isInstance(value) == true
    }

    fun readListUiMessages(operation: Any?): List<*>? {
        if (operation == null || listUIOperationApiImplClass?.isInstance(operation) != true) {
            return null
        }
        return listReadOnlyListMethod?.invoke(operation) as? List<*>
    }

    fun readListUiView(operation: Any?): View? {
        if (operation == null || listUIOperationApiImplClass?.isInstance(operation) != true) {
            return null
        }
        return listRecyclerViewMethod?.invoke(operation) as? View
    }

    fun isWatchAioListVb(value: Any?): Boolean {
        return value != null && watchAioListVbClass?.isInstance(value) == true
    }

    fun readInputBarController(listVb: Any?): Any? {
        if (!isWatchAioListVb(listVb)) return null
        return inputBarControllerField?.get(listVb)
    }

    fun requestLoadOlderMessages(repo: Any) {
        if (!isWatchMsgListRepo(repo)) return
        repoLoadOlderMethod?.invoke(repo)
    }

    fun requestGroupMemberInfo(
        groupCode: Long,
        uids: Collection<String>,
        onLoaded: (Map<String, Any>) -> Unit
    ): Boolean {
        if (groupCode <= 0L || uids.isEmpty()) return false
        val groupService = kernelGroupServiceGetterMethod?.invoke(kernelGroupServiceUtilCompanion)
            ?: return false
        val callbackClass = groupMemberListCallbackClass ?: return false
        val infosField = groupMemberListResultInfosField ?: return false
        val callback = Proxy.newProxyInstance(
            classLoader,
            arrayOf(callbackClass)
        ) { _, method, args ->
            if (method.name == "onResult") {
                val resultCode = (args?.getOrNull(0) as? Number)?.toInt() ?: -1
                val result = args?.getOrNull(2)
                if (resultCode == 0 && result != null) {
                    @Suppress("UNCHECKED_CAST")
                    val infos = (infosField.get(result) as? Map<String, Any>).orEmpty()
                    if (infos.isNotEmpty()) {
                        AioRuntimeStore.mainHandler.post { onLoaded(infos) }
                    }
                }
            }
            defaultInvocationReturn(method.returnType)
        }
        val uidList = ArrayList(uids.filter { it.isNotBlank() }.distinct())
        if (uidList.isEmpty()) return false
        val fetchMethod = groupService.javaClass.methods.firstOrNull { method ->
            method.name == "n" && method.parameterTypes.size == 5
        } ?: return false
        return runCatching {
            fetchMethod.invoke(groupService, groupCode, uidList, false, "", callback)
            true
        }.getOrDefault(false)
    }

    fun createAvatarView(context: Context, spec: AvatarSpec?): View {
        val avatar = runCatching {
            watchAvatarConstructor?.newInstance(context, null, 0, 0) as? View
        }.getOrNull() ?: ImageView(context)

        (avatar as? ImageView)?.scaleType = ImageView.ScaleType.CENTER_CROP
        avatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.rgb(226, 232, 240))
        }
        loadAvatar(avatar, spec)
        return avatar
    }

    fun loadAvatar(view: View, spec: AvatarSpec?) {
        if (spec == null) return
        val fragment = spec.fragmentRef?.get()
        val bindingKey =
            "${spec.stableKey()}:${fragment?.let { System.identityHashCode(it) } ?: 0}"
        if (view.getTag(AIO_AVATAR_BINDING_KEY) == bindingKey) return
        view.setTag(AIO_AVATAR_BINDING_KEY, bindingKey)

        runCatching {
            val companion = avatarFacadeCompanion ?: return
            val bindMethod = avatarTargetBindMethod ?: return
            val optionMethod = avatarRequestOptionMethod ?: return
            val builderClass = avatarOptionBuilderClass ?: return
            val optionBuilder = builderClass.getDeclaredConstructor().newInstance()
            avatarOptionBuilderSizeMethod?.invoke(optionBuilder, avatarSizeType)
            val option = avatarOptionBuilderBuildMethod?.invoke(optionBuilder)
            val target = if (fragment != null && avatarFacadeFragmentMethod != null) {
                avatarFacadeFragmentMethod.invoke(companion, fragment)
            } else {
                avatarFacadeContextMethod?.invoke(companion, view.context)
            } ?: return
            val request = bindMethod.invoke(target, view) ?: return
            val requestWithOption = if (option != null) {
                optionMethod.invoke(request, option) ?: request
            } else {
                request
            }
            val scope = fragment?.let { lifecycleScopeMethod?.invoke(null, it) } ?: return
            if (spec.chatType == GROUP_CHAT_TYPE) {
                avatarRequestLoadUinMethod?.invoke(requestWithOption, spec.uin, scope)
            } else {
                val uid = spec.uid.takeIf { it.isNotBlank() }
                if (!uid.isNullOrBlank() && avatarRequestLoadUidMethod != null) {
                    avatarRequestLoadUidMethod.invoke(requestWithOption, uid, spec.uin, scope)
                } else if (spec.uin > 0L) {
                    avatarRequestLoadUinMethod?.invoke(requestWithOption, spec.uin, scope)
                }
            }
        }
    }

    fun tryOpenEmoji(root: View?, listVb: Any?, inputBarController: Any?): Boolean {
        if (dispatchEmotionClick(listVb)) {
            return true
        }
        if (clickInputBarView(inputBarController, inputBarIconFaceField, "b")) {
            return true
        }
        return root?.findHostActionView(
            "表情",
            "emoji",
            "emoticon",
            "face"
        )?.performClick() == true
    }

    fun loadInitialEmotionCategories(): List<AioEmotionCategory> {
        return listOf(
            AioEmotionCategory(
                key = EMOTION_CATEGORY_FAVORITE,
                title = "收藏",
                loading = true
            ),
            AioEmotionCategory(
                key = EMOTION_CATEGORY_SYSTEM,
                title = "表情",
                items = loadSystemEmotionItems(bigSticker = false)
            ),
            AioEmotionCategory(
                key = EMOTION_CATEGORY_BIG_SYSTEM,
                title = "大表情",
                items = loadSystemEmotionItems(bigSticker = true)
            ),
            AioEmotionCategory(
                key = EMOTION_CATEGORY_HOT,
                title = "热门",
                loading = true
            )
        )
    }

    fun loadFavoriteEmotionItems(onLoaded: (List<AioEmotionItem>) -> Unit) {
        val callbackClass = classLoader.findOptionalClass(FETCH_FAV_EMOJI_CALLBACK_CLASS)
            ?: run {
                onLoaded(emptyList())
                return
            }
        val callback = Proxy.newProxyInstance(
            classLoader,
            arrayOf(callbackClass)
        ) { _, method, args ->
            if (method.name == "onFetchFavEmojiListCallback") {
                val result = (args?.getOrNull(0) as? Number)?.toInt() ?: -1
                val rows = args?.getOrNull(2) as? Iterable<*>
                val items = if (result == 0) {
                    rows?.mapNotNull(::toFavoriteEmotionItem).orEmpty()
                } else {
                    emptyList()
                }
                AioRuntimeStore.mainHandler.post { onLoaded(items) }
            }
            null
        }

        Thread {
            val msgService = findMsgService(waitRuntime = true)
                ?: run {
                    AioRuntimeStore.mainHandler.post { onLoaded(emptyList()) }
                    return@Thread
                }
            runCatching {
                msgService.javaClass.methods.firstOrNull { method ->
                    method.name == "fetchFavEmojiList" && method.parameterTypes.size == 5
                }?.invoke(msgService, "", 1000, true, false, callback)
                    ?: AioRuntimeStore.mainHandler.post { onLoaded(emptyList()) }
            }.onFailure {
                AioRuntimeStore.mainHandler.post { onLoaded(emptyList()) }
            }
        }.start()
    }

    fun loadHotEmotionItems(onLoaded: (List<AioEmotionItem>) -> Unit) {
        val callbackClass = classLoader.findOptionalClass(GET_HOT_PIC_CALLBACK_CLASS)
            ?: run {
                onLoaded(emptyList())
                return
            }
        val msgService = findMsgService(waitRuntime = false)
            ?: run {
                onLoaded(emptyList())
                return
            }
        val callback = Proxy.newProxyInstance(
            classLoader,
            arrayOf(callbackClass)
        ) { _, method, args ->
            if (method.name == "onGetHotPicInfoList") {
                val result = (args?.getOrNull(0) as? Number)?.toInt() ?: -1
                val rows = args?.getOrNull(2) as? Iterable<*>
                val items = if (result == 0) {
                    rows?.mapNotNull(::toHotEmotionItem).orEmpty()
                } else {
                    emptyList()
                }
                AioRuntimeStore.mainHandler.post { onLoaded(items) }
            }
            null
        }

        runCatching {
            msgService.javaClass.methods.firstOrNull { method ->
                method.name == "getHotPicInfoListSearchString" &&
                    method.parameterTypes.size == 6
            }?.invoke(msgService, "", "", 10, 1, false, callback) ?: onLoaded(emptyList())
        }.onFailure {
            onLoaded(emptyList())
        }
    }

    fun createEmotionPreviewView(context: Context, item: AioEmotionItem): View? {
        if (item.kind != AioEmotionKind.System) return null
        return ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true
            reloadEmotionPreview(this, item)
        }
    }

    fun reloadEmotionPreview(view: View, item: AioEmotionItem) {
        if (view !is ImageView || item.kind != AioEmotionKind.System) return
        view.setImageDrawable(loadSystemEmotionDrawable(item))
    }

    fun getEmotionPreviewDrawable(item: AioEmotionItem): Drawable? {
        if (item.kind != AioEmotionKind.System) return null
        return loadSystemEmotionDrawable(item)
    }

    fun ensureEmotionPreview(item: AioEmotionItem, onLoaded: (AioEmotionItem) -> Unit) {
        if (item.kind != AioEmotionKind.Favorite && item.kind != AioEmotionKind.Hot) return
        val localPath = item.resultPath?.takeIf { it.isNotBlank() && File(it).isFile }
            ?: item.localPath?.takeIf { it.isNotBlank() && File(it).isFile }
        if (localPath != null) {
            onLoaded(item.copy(localPath = localPath, resultPath = localPath))
            return
        }
        val url = item.resultUrl?.takeIf { it.isNotBlank() } ?: item.remoteUrl?.takeIf { it.isNotBlank() }
        val resId = item.resultResId?.takeIf { it.isNotBlank() }
        val md5 = item.resultMd5?.takeIf { it.isNotBlank() }
        if (url == null || resId == null || md5 == null) return
        downloadRemoteEmotionPath(url, resId, md5) { path ->
            if (path.isBlank()) return@downloadRemoteEmotionPath
            AioRuntimeStore.mainHandler.post {
                onLoaded(item.copy(localPath = path, resultPath = path))
            }
        }
    }

    fun sendEmotion(listVb: Any?, fragment: Any?, item: AioEmotionItem): Boolean {
        if (sendEmotionWithHostVm(listVb, item)) {
            return true
        }
        return sendEmotionByFragmentResult(fragment, item)
    }

    private fun sendEmotionWithHostVm(listVb: Any?, item: AioEmotionItem): Boolean {
        if (!isWatchAioListVb(listVb)) {
            return false
        }
        val hostVm = listVb?.findHostWatchAioListVm()
        if (hostVm == null) {
            return false
        }
        return when (item.kind) {
            AioEmotionKind.System -> {
                val element = createSystemEmotionElement(item)
                sendSingleElement(hostVm, element)
            }

            AioEmotionKind.Favorite,
            AioEmotionKind.Hot -> {
                sendPictureEmotion(hostVm, item)
            }

            AioEmotionKind.MarketFavorite -> {
                sendMarketEmotion(hostVm, item)
            }
        }
    }

    private fun sendEmotionByFragmentResult(fragment: Any?, item: AioEmotionItem): Boolean {
        if (fragment == null || !fragmentClass.isInstance(fragment)) return false
        val requestKey = (requireArgumentsMethod.invoke(fragment) as? Bundle)
            ?.getString(REQUEST_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: return false
        val extClass = classLoader.findOptionalClass(WATCH_PIC_ELEMENT_EXT_CLASS) ?: return false
        val parentFragment = extClass.getDeclaredMethod("A0", fragmentClass)
            .apply { isAccessible = true }
            .invoke(null, fragment)
            ?: return false
        val result = Bundle()
        when (item.kind) {
            AioEmotionKind.System -> {
                result.putInt("request_result_type", 0)
                result.putInt("request_result_emotion_code", item.systemCode)
                result.putInt("request_result_emotion_type", item.systemType)
            }

            AioEmotionKind.Favorite -> {
                result.putInt("request_result_type", 1)
                result.putString("request_result", item.resultPath.orEmpty())
                result.putString("request_result_fav_emotion_url", item.resultUrl.orEmpty())
                result.putString("request_result_fav_emotion_res_id", item.resultResId.orEmpty())
                result.putString("request_result_fav_emotion_md5", item.resultMd5.orEmpty())
            }

            AioEmotionKind.MarketFavorite -> {
                result.putInt("request_result_type", 3)
                result.putString("request_result_market_epid", item.marketEpId.orEmpty())
                result.putString("request_result_market_eid", item.marketEId.orEmpty())
            }

            AioEmotionKind.Hot -> {
                result.putInt("request_result_type", 2)
                result.putString("request_result", item.resultPath.orEmpty())
                result.putString("request_result_fav_emotion_url", item.resultUrl.orEmpty())
                result.putString("request_result_fav_emotion_res_id", item.resultResId.orEmpty())
                result.putString("request_result_fav_emotion_md5", item.resultMd5.orEmpty())
            }
        }
        extClass.getDeclaredMethod("s2", fragmentClass, String::class.java, Bundle::class.java)
            .apply { isAccessible = true }
            .invoke(null, parentFragment, requestKey, result)
        return true
    }

    private fun createSystemEmotionElement(item: AioEmotionItem): Any? {
        return runCatching {
            val emoMsgUtilsClass = classLoader.findOptionalClass(EMO_MSG_UTILS_CLASS)
                ?: return@runCatching null
            val emoMsgUtils = emoMsgUtilsClass.getDeclaredField("a")
                .apply { isAccessible = true }
                .get(null)
                ?: return@runCatching null
            if (item.systemType == SYSTEM_EMOTION_TYPE_EMOJI) {
                emoMsgUtilsClass.getDeclaredMethod("b", Integer.TYPE, Integer.TYPE)
                    .apply { isAccessible = true }
                    .invoke(emoMsgUtils, item.systemCode, item.systemType)
            } else {
                markRecentSystemEmotion(item.systemCode)
                val serverCode = classLoader.findOptionalClass(QQ_SYS_FACE_UTIL_CLASS)
                    ?.getDeclaredMethod("convertToServer", Integer.TYPE)
                    ?.apply { isAccessible = true }
                    ?.invoke(null, item.systemCode) as? Number
                emoMsgUtilsClass.getDeclaredMethod("a", Integer.TYPE)
                    .apply { isAccessible = true }
                    .invoke(emoMsgUtils, serverCode?.toInt() ?: item.systemCode)
            }
        }.getOrNull()
    }

    private fun markRecentSystemEmotion(code: Int) {
        runCatching {
            val recentClass = classLoader.findOptionalClass(RECENT_USE_DATA_SOURCE_CLASS)
                ?: return
            val recent = recentClass.getDeclaredField("b")
                .apply { isAccessible = true }
                .get(null)
                ?: return
            recentClass.getDeclaredMethod("b", Integer.TYPE)
                .apply { isAccessible = true }
                .invoke(recent, code)
        }
    }

    private fun sendPictureEmotion(hostVm: Any, item: AioEmotionItem): Boolean {
        val localPath = item.resultPath?.takeIf { it.isNotBlank() }
        if (localPath != null && File(localPath).isFile) {
            return sendPictureEmotionPath(hostVm, localPath)
        }

        val url = item.resultUrl?.takeIf { it.isNotBlank() } ?: item.remoteUrl?.takeIf { it.isNotBlank() }
        val resId = item.resultResId?.takeIf { it.isNotBlank() }
        val md5 = item.resultMd5?.takeIf { it.isNotBlank() }
        if (url == null || resId == null || md5 == null) {
            return false
        }
        return scheduleRemoteEmotionDownload(hostVm, url, resId, md5)
    }

    private fun sendPictureEmotionPath(hostVm: Any, path: String): Boolean {
        val element = runCatching {
            val msgUtilClass = classLoader.findOptionalClass(MSG_UTIL_CLASS) ?: return@runCatching null
            val msgUtil = msgUtilClass.getDeclaredField("a")
                .apply { isAccessible = true }
                .get(null)
                ?: return@runCatching null
            msgUtilClass.getDeclaredMethod(
                "a",
                String::class.java,
                Integer.TYPE,
                String::class.java
            ).apply { isAccessible = true }
                .invoke(msgUtil, path, 1, null)
        }.getOrNull()
        return sendSingleElement(hostVm, element)
    }

    private fun scheduleRemoteEmotionDownload(
        hostVm: Any,
        url: String,
        resId: String,
        md5: String
    ): Boolean {
        return runCatching {
            downloadRemoteEmotionPath(url, resId, md5) { path ->
                if (path.isNotBlank()) {
                    AioRuntimeStore.mainHandler.post {
                        sendPictureEmotionPath(hostVm, path)
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun downloadRemoteEmotionPath(
        url: String,
        resId: String,
        md5: String,
        onLoaded: (String) -> Unit
    ) {
        val listenerClass = classLoader.findOptionalClass(I_KERNEL_MSG_LISTENER_CLASS) ?: return
        val paramsClass = classLoader.findOptionalClass(GPRO_EMOJI_DOWNLOAD_PARAMS_CLASS) ?: return
        val params = paramsClass.getDeclaredConstructor(
            String::class.java,
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }
            .newInstance(url, resId, md5)
        Thread({
            runCatching {
                val msgService = findMsgService(waitRuntime = true) ?: return@runCatching
                var listener: Any? = null
                listener = Proxy.newProxyInstance(
                    classLoader,
                    arrayOf(listenerClass)
                ) { _, method, args ->
                    if (method.name == "onEmojiDownloadComplete") {
                        unregisterKernelMsgListener(msgService, listenerClass, listener)
                        val path = args?.firstOrNull()
                            ?.findFieldValue("path")
                            ?.toString()
                            .orEmpty()
                        if (path.isNotBlank()) {
                            onLoaded(path)
                        }
                    }
                    defaultInvocationReturn(method.returnType)
                }
                if (!registerKernelMsgListener(msgService, listenerClass, listener)) {
                    return@runCatching
                }
                val paramsList = java.util.ArrayList<Any>(1).apply { add(params) }
                val downloadMethod = msgService.javaClass.methods.firstOrNull { method ->
                    method.name == "downloadEmojiPic" &&
                        method.parameterTypes.size == 4
                }
                if (downloadMethod == null) {
                    unregisterKernelMsgListener(msgService, listenerClass, listener)
                    return@runCatching
                }
                runCatching {
                    downloadMethod.invoke(msgService, 1, paramsList, 0, HashMap<Any, Any>())
                }.onFailure {
                    unregisterKernelMsgListener(msgService, listenerClass, listener)
                    throw it
                }
            }
        }, "QQR-EmotionDownload").start()
    }

    private fun sendMarketEmotion(hostVm: Any, item: AioEmotionItem): Boolean {
        return runCatching {
            val epId = item.marketEpId?.toIntOrNull() ?: return@runCatching false
            val eId = item.marketEId?.takeIf { it.isNotBlank() } ?: return@runCatching false
            val vmClass = watchAioListVmClass ?: hostVm.javaClass
            val runnableClass = classLoader.findOptionalClass(MARKET_EMOTION_SEND_RUNNABLE_CLASS)
                ?: return@runCatching false
            val runnable = runnableClass.getDeclaredConstructor(
                vmClass,
                Int::class.javaObjectType,
                String::class.java
            ).apply { isAccessible = true }
                .newInstance(hostVm, epId, eId) as? Runnable
                ?: return@runCatching false
            Thread(runnable, "QQR-MarketEmotion").start()
            true
        }.getOrDefault(false)
    }

    private fun registerKernelMsgListener(
        msgService: Any,
        listenerClass: Class<*>,
        listener: Any?
    ): Boolean {
        if (listener == null) return false
        return runCatching {
            val registerMethod = msgService.javaClass.methods.firstOrNull { method ->
                method.name == "u" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0].isAssignableFrom(listenerClass)
            } ?: return@runCatching false
            registerMethod.invoke(msgService, listener)
            true
        }.getOrDefault(false)
    }

    private fun unregisterKernelMsgListener(
        msgService: Any,
        listenerClass: Class<*>,
        listener: Any?
    ) {
        if (listener == null) return
        runCatching {
            msgService.javaClass.methods.firstOrNull { method ->
                method.name == "e" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0].isAssignableFrom(listenerClass)
            }?.invoke(msgService, listener)
        }
    }

    private fun sendSingleElement(hostVm: Any, element: Any?): Boolean {
        if (element == null) return false
        val elements = java.util.ArrayList<Any>(1).apply { add(element) }
        watchAioListVmSendElementsMethod?.invoke(hostVm, elements) ?: return false
        return true
    }

    private fun defaultInvocationReturn(type: Class<*>): Any? {
        return when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            Character.TYPE -> 0.toChar()
            else -> null
        }
    }

    private fun dispatchEmotionClick(listVb: Any?): Boolean {
        if (!isWatchAioListVb(listVb)) return false
        val constructor = inputBarEmotionClickConstructor ?: return false
        val dispatch = inputBarIntentDispatchMethod ?: return false
        val intent = constructor.newInstance(true)
        dispatch.invoke(listVb, intent)
        return true
    }

    private fun loadSystemEmotionItems(bigSticker: Boolean): List<AioEmotionItem> {
        return runCatching {
            val sysFaceClass = classLoader.findOptionalClass(QQ_SYS_FACE_UTIL_CLASS)
                ?: return@runCatching emptyList()
            val qqEmojiClass = classLoader.findOptionalClass(QQ_EMOJI_UTIL_CLASS)
                ?: return@runCatching emptyList()
            val managerClass = classLoader.findOptionalClass(SYSTEM_EMOJI_DATA_MANAGER_CLASS)
                ?: return@runCatching emptyList()
            val manager = managerClass.getDeclaredField("a").apply { isAccessible = true }
                .get(null)
            val systemMapper = manager.javaClass.methods.firstOrNull { method ->
                method.name == "b" && method.parameterTypes.size == 1
            } ?: return@runCatching emptyList()
            val emojiMapper = manager.javaClass.methods.firstOrNull { method ->
                method.name == "a" && method.parameterTypes.size == 1
            } ?: return@runCatching emptyList()
            val isBigSticker = sysFaceClass.methods.firstOrNull { method ->
                method.name == "isBigStickerById" && method.parameterTypes.size == 1
            } ?: return@runCatching emptyList()

            val rows = ArrayList<Any?>()
            rows.addAll(
                (systemMapper.invoke(
                    manager,
                    sysFaceClass.getDeclaredMethod("getOrderList").invoke(null)
                ) as? Iterable<*>)?.toList().orEmpty()
            )
            rows.addAll(
                (emojiMapper.invoke(
                    manager,
                    qqEmojiClass.getDeclaredMethod("getOrderList").invoke(null)
                ) as? Iterable<*>)?.toList().orEmpty()
            )
            rows.addAll(
                (systemMapper.invoke(
                    manager,
                    sysFaceClass.getDeclaredMethod("getAllAniStickerOrderList").invoke(null)
                ) as? Iterable<*>)?.toList().orEmpty()
            )

            rows.mapNotNull { row ->
                val type = (row?.findFieldValue("j") as? Number)?.toInt() ?: return@mapNotNull null
                val code = (row.findFieldValue("k") as? Number)?.toInt() ?: return@mapNotNull null
                val visible = if (type != SYSTEM_EMOTION_TYPE_FACE) {
                    !bigSticker
                } else {
                    val isBig = isBigSticker.invoke(null, code) as? Boolean == true
                    isBig == bigSticker && code !in HIDDEN_BIG_STICKER_CODES
                }
                if (!visible) return@mapNotNull null
                AioEmotionItem(
                    key = "system:$type:$code",
                    title = code.toString(),
                    kind = AioEmotionKind.System,
                    systemCode = code,
                    systemType = type
                )
            }.distinctBy { item -> item.key }
        }.getOrDefault(emptyList())
    }

    private fun toFavoriteEmotionItem(row: Any?): AioEmotionItem? {
        if (row == null) return null
        val isMarket = row.findFieldValue("isMarkFace") as? Boolean == true
        val emoId = (row.findFieldValue("emoId") as? Number)?.toInt() ?: 0
        if (isMarket) {
            val epId = row.findFieldValue("epId")?.toString().orEmpty()
            val eId = row.findFieldValue("eId")?.toString().orEmpty()
            return AioEmotionItem(
                key = "fav-market:$epId:$eId:$emoId",
                title = emoId.takeIf { it > 0 }?.toString() ?: "收藏",
                kind = AioEmotionKind.MarketFavorite,
                marketEpId = epId,
                marketEId = eId
            )
        }
        val path = row.findFieldValue("emoPath")?.toString().orEmpty()
        val url = row.findFieldValue("url")?.toString().orEmpty()
        val resId = row.findFieldValue("resId")?.toString().orEmpty()
        val md5 = row.findFieldValue("md5")?.toString().orEmpty()
        return AioEmotionItem(
            key = "fav:$md5:$resId:$path:$url",
            title = emoId.takeIf { it > 0 }?.toString() ?: "收藏",
            kind = AioEmotionKind.Favorite,
            localPath = path,
            remoteUrl = url,
            resultPath = path,
            resultUrl = url,
            resultResId = resId,
            resultMd5 = md5
        )
    }

    private fun toHotEmotionItem(row: Any?): AioEmotionItem? {
        if (row == null) return null
        val path = row.findFieldValue("path")?.toString().orEmpty()
        val url = row.findFieldValue("downloadUrl")?.toString().orEmpty()
        val picId = row.findFieldValue("picId")?.toString().orEmpty()
        val md5 = row.findFieldValue("fileMd5")?.toString().orEmpty()
        return AioEmotionItem(
            key = "hot:$picId:$md5:$path:$url",
            title = picId.ifBlank { "热门" },
            kind = AioEmotionKind.Hot,
            localPath = path,
            remoteUrl = url,
            resultPath = path,
            resultUrl = url,
            resultResId = picId,
            resultMd5 = md5
        )
    }

    private fun loadSystemEmotionDrawable(item: AioEmotionItem): Drawable? {
        return runCatching {
            if (item.systemType == SYSTEM_EMOTION_TYPE_FACE) {
                classLoader.findOptionalClass(QQ_SYS_FACE_UTIL_CLASS)
                    ?.getDeclaredMethod("getFaceDrawable", Integer.TYPE)
                    ?.invoke(null, item.systemCode) as? Drawable
            } else {
                classLoader.findOptionalClass(QQ_EMOJI_UTIL_CLASS)
                    ?.getDeclaredMethod("getEmojiDrawable", Integer.TYPE)
                    ?.invoke(null, item.systemCode) as? Drawable
            }
        }.getOrNull()
    }

    private fun findMsgService(waitRuntime: Boolean): Any? {
        return runCatching {
            val mobileQQClass = classLoader.findOptionalClass(MOBILE_QQ_CLASS) ?: return null
            val mobileQQ = mobileQQClass.getDeclaredField("sMobileQQ")
                .apply { isAccessible = true }
                .get(null) ?: return null
            val runtime = if (waitRuntime) {
                mobileQQ.javaClass.methods.firstOrNull { method ->
                    method.name == "waitAppRuntime" && method.parameterTypes.isEmpty()
                }?.invoke(mobileQQ)
            } else {
                mobileQQ.javaClass.methods.firstOrNull { method ->
                    method.name == "peekAppRuntime" && method.parameterTypes.isEmpty()
                }?.invoke(mobileQQ)
            } ?: return null
            val kernelServiceClass = classLoader.findOptionalClass(I_KERNEL_SERVICE_CLASS)
                ?: return null
            val getRuntimeService = runtime.javaClass.methods.firstOrNull { method ->
                method.name == "getRuntimeService" &&
                    method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == Class::class.java
            } ?: return null
            val kernelService = getRuntimeService.invoke(runtime, kernelServiceClass, "all")
                ?: getRuntimeService.invoke(runtime, kernelServiceClass, "")
                ?: return null
            kernelService.invokeNoArg("getMsgService")
        }.getOrNull()
    }

    fun sendImage(peer: AioPeer, path: String): Boolean {
        val normalizedPath = path.trim()
            .removePrefix("file://")
            .takeIf { it.isNotBlank() }
            ?: return false
        if (!File(normalizedPath).isFile) return false
        return runCatching {
            val element = createHostImageElement(normalizedPath) ?: return@runCatching false
            sendHostElements(peer, java.util.ArrayList<Any>(1).apply { add(element) })
        }.getOrDefault(false)
    }

    fun startAvCall(fragment: Any?, peer: AioPeer, isVideo: Boolean): Boolean {
        if (fragment == null || !fragmentClass.isInstance(fragment)) return false
        if (peer.chatType != SINGLE_CHAT_TYPE) return false
        return runCatching {
            val qavFacadeClass = classLoader.findOptionalClass(I_WATCH_QAV_FACADE_CLASS)
                ?: return@runCatching false
            val qavFacade = findQRouteApi(qavFacadeClass) ?: return@runCatching false
            val context = fragment.invokeNoArg("requireContext") ?: return@runCatching false
            val parentFragment = fragment.invokeNoArg("requireParentFragment")
                ?: return@runCatching false
            val method = qavFacade.javaClass.methods.firstOrNull { candidate ->
                candidate.name == "goToAVScene" && candidate.parameterTypes.size == 6
            } ?: return@runCatching false
            method.invoke(
                qavFacade,
                context,
                parentFragment,
                peer.chatUin.takeIf { it > 0L }?.toString().orEmpty(),
                peer.peerId,
                peer.chatNick.orEmpty(),
                isVideo
            )
            true
        }.getOrDefault(false)
    }

    fun tryOpenIme(root: View?, inputBarController: Any?): Boolean {
        if (clickInputBarView(inputBarController, inputBarImeContentField, "f")) {
            return true
        }
        return root?.findHostActionView("输入", "键盘", "keyboard", "ime")?.performClick() == true
    }

    fun trySendText(root: View?, listVb: Any?, inputBarController: Any?, text: String): Boolean {
        if (sendTextWithHostVm(listVb, text)) {
            root?.clearHostDraftText()
            return true
        }
        val editText = root?.findFirstEditText()
        if (editText != null) {
            editText.requestFocus()
            editText.setText(text)
            editText.setSelection(editText.text?.length ?: 0)
        }
        val sendView = root?.findHostActionView("发送", "send")
        if (sendView?.performClick() == true) {
            root.clearHostDraftText()
            return true
        }
        return false
    }

    private fun clickInputBarView(
        inputBarController: Any?,
        viewField: Field?,
        functionFieldName: String
    ): Boolean {
        if (inputBarController == null) return false
        val actionView = viewField?.get(inputBarController) as? View
        if (actionView?.performClick() == true) {
            return true
        }
        return inputBarController.findFieldValue(functionFieldName).invokeFunction0()
    }

    private fun sendTextWithHostVm(listVb: Any?, text: String): Boolean {
        if (!isWatchAioListVb(listVb) || text.isBlank()) return false
        val hostVm = listVb?.findHostWatchAioListVm()
            ?: return false
        val elements = imeTextToElementsMethod
            ?.invoke(imeTextUtilCompanion, text) as? java.util.ArrayList<*>
            ?: return false
        if (elements.isEmpty()) return false
        watchAioListVmSendElementsMethod?.invoke(hostVm, elements) ?: return false
        return true
    }

    private fun createHostImageElement(path: String): Any? {
        return runCatching {
            val msgUtilClass = classLoader.findOptionalClass(MSG_UTIL_CLASS) ?: return@runCatching null
            val msgUtil = msgUtilClass.getDeclaredField("a")
                .apply { isAccessible = true }
                .get(null)
                ?: return@runCatching null
            msgUtilClass.getDeclaredMethod(
                "a",
                String::class.java,
                Integer.TYPE,
                String::class.java
            ).apply { isAccessible = true }
                .invoke(msgUtil, path, HOST_IMAGE_SUB_TYPE, null)
        }.getOrNull()
    }

    private fun sendHostElements(peer: AioPeer, elements: java.util.ArrayList<Any>): Boolean {
        if (elements.isEmpty()) return false
        val msgServiceApiClass = classLoader.findOptionalClass(HOST_MSG_SERVICE_API_CLASS) ?: return false
        val msgService = findQRouteApi(msgServiceApiClass) ?: return false
        val contactClass = classLoader.findOptionalClass(HOST_CONTACT_CLASS) ?: return false
        val contact = contactClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        contactClass.setIntFieldIfPresent(contact, "chatType", peer.chatType)
        contactClass.setObjectFieldIfPresent(contact, "peerUid", peer.peerId)
        val sendMsgMethod = msgService.javaClass.methods.firstOrNull { method ->
            method.name == "sendMsg" && method.parameterTypes.size == 4
        } ?: return false
        sendMsgMethod.invoke(msgService, contact, 0L, elements, null)
        return true
    }

    private fun findQRouteApi(apiClass: Class<*>): Any? {
        val qRouteClass = classLoader.findOptionalClass(Q_ROUTE_CLASS) ?: return null
        val apiMethod = qRouteClass.methods.firstOrNull { method ->
            method.name == "api" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == Class::class.java
        } ?: return null
        return apiMethod.invoke(null, apiClass)
    }

    private fun Any.findHostWatchAioListVm(): Any? {
        val vmClass = watchAioListVmClass ?: return null
        findFieldValue("b")?.takeIf { vmClass.isInstance(it) }?.let { return it }
        findFieldValue("E")?.findFieldValue("b")?.takeIf { vmClass.isInstance(it) }?.let {
            return it
        }
        return null
    }

    private fun parseElements(msgRecord: Any): ParsedMessage {
        val elements = elementsGetter.invoke(msgRecord) as? Iterable<*> ?: return ParsedMessage()
        val textParts = mutableListOf<String>()
        var media: AioMediaSpec? = null
        var renderKind = AioMessageKind.Unsupported
        var rawKind = AioMessageKind.Unsupported
        var forwardPreview: AioForwardPreview? = null
        var hasTip = false

        elements.filterNotNull().forEach { element ->
            if (rawKind == AioMessageKind.Unsupported) {
                rawKind = AioMessageKind.fromElementType(elementTypeGetter.invokeInt(element))
            }
            textElementGetter.invokeAny(element)?.let { textElement ->
                textContentGetter.invokeString(textElement)?.takeIf { it.isNotBlank() }
                    ?.let(textParts::add)
            }
            multiForwardElementGetter.invokeAny(element)?.let { multiForward ->
                val preview = parseMultiForwardPreview(multiForward)
                forwardPreview = preview
                preview.items.takeIf { it.isNotEmpty() }?.let(textParts::addAll)
                renderKind = AioMessageKind.MultiMsgForward
            }
            avRecordElementGetter.invokeAny(element)?.let { avRecord ->
                parseAvRecordText(avRecord).takeIf { it.isNotBlank() }?.let(textParts::add)
                renderKind = AioMessageKind.Call
            }
            if (media == null) {
                videoElementGetter.invokeAny(element)?.let { video ->
                    media = parseVideo(video)
                    renderKind = AioMessageKind.Video
                }
            }
            if (media == null) {
                picElementGetter.invokeAny(element)?.let { pic ->
                    media = parsePic(pic)
                    renderKind = AioMessageKind.Image
                }
            }
            if (media == null) {
                pttElementGetter.invokeAny(element)?.let { ptt ->
                    media = parsePtt(ptt)
                    pttTextGetter?.invokeString(ptt)?.takeIf { it.isNotBlank() }
                        ?.let(textParts::add)
                    renderKind = AioMessageKind.Voice
                }
            }
            if (media == null) {
                fileElementGetter.invokeAny(element)?.let { file ->
                    media = parseFile(file)
                    renderKind = AioMessageKind.File
                }
            }
            if (grayTipElementGetter.invokeAny(element) != null) {
                hasTip = true
            }
        }

        if (renderKind == AioMessageKind.Unsupported) {
            renderKind = when {
                textParts.isNotEmpty() -> AioMessageKind.Text
                hasTip -> AioMessageKind.Tip
                else -> AioMessageKind.Unsupported
            }
        }
        return ParsedMessage(renderKind, rawKind, textParts.joinToString(""), media, forwardPreview)
    }

    private fun parseMultiForwardPreview(multiForward: Any): AioForwardPreview {
        val xmlContent = multiForwardXmlContentGetter.invokeString(multiForward).orEmpty()
        if (xmlContent.isNotBlank()) {
            Log.d("QQRevived.MultiForwardXml", xmlContent)
        }
        val fileName = multiForwardFileNameGetter.invokeString(multiForward)?.trim().orEmpty()
        val titles = MULTI_FORWARD_TITLE_REGEX.findAll(xmlContent)
            .mapNotNull { sanitizeXmlText(it.groupValues[1]).takeIf(String::isNotBlank) }
            .toList()
        val summary = MULTI_FORWARD_SUMMARY_REGEX.find(xmlContent)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::sanitizeXmlText)
            .orEmpty()
        val header = titles.firstOrNull()
            ?: fileName.takeIf { it.isNotBlank() }
            ?: "合并转发"
        val items = titles.drop(1)
        val count = MULTI_FORWARD_TSUM_REGEX.find(xmlContent)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: summary.takeIf { it.isNotBlank() }?.let(::extractForwardCount)
            ?: 0
        val footerText = summary.ifBlank {
            count.takeIf { it > 0 }?.let { "查看${it}条转发消息" } ?: "合并转发"
        }
        return AioForwardPreview(
            header = header,
            items = items,
            count = count,
            footer = footerText,
            rawXml = xmlContent
        )
    }

    private fun sanitizeXmlText(value: String): String {
        return value
            .replace("<![CDATA[", "")
            .replace("]]>", "")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun extractForwardCount(text: String): Int {
        return FORWARD_COUNT_REGEX.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun parsePic(pic: Any): AioMediaSpec {
        val sourcePath = watchPicOriginPathMethod?.invokeStringOrNull(null, pic)
            ?: picSourcePathGetter.invokeString(pic)?.takeIf { it.isNotBlank() }
        val thumbPath = firstCandidatePath(
            watchPicThumbPathMethod?.invokeStringOrNull(null, pic, PIC_THUMB_SIZE_SMALL),
            watchPicThumbPathMethod?.invokeStringOrNull(null, pic, PIC_THUMB_SIZE_ORIGIN),
            watchPicThumbPathMethod?.invokeStringOrNull(null, pic, PIC_THUMB_SIZE_LARGE),
            picThumbPathGetter.invokeMapPath(pic)
        )
        val originUrl = picOriginUrlGetter.invokeString(pic)?.takeIf { it.isNotBlank() }
        return AioMediaSpec(
            localPath = firstCandidatePath(sourcePath, thumbPath),
            remoteUrl = normalizeRemoteUrl(originUrl),
            previewPath = firstCandidatePath(thumbPath, sourcePath),
            playbackPath = firstCandidatePath(sourcePath, thumbPath),
            width = picWidthGetter.invokeInt(pic),
            height = picHeightGetter.invokeInt(pic),
            fileName = picFileNameGetter.invokeString(pic)
        )
    }

    private fun parseAvRecordText(avRecord: Any): String {
        val detail = avRecordTextGetter.invokeString(avRecord)?.trim().orEmpty()
        val type = avRecordTypeGetter.invokeInt(avRecord)
        val kindLabel = if (type in VIDEO_AV_RECORD_TYPES) "视频通话" else "语音通话"
        if (detail.isBlank()) return kindLabel
        return if (detail.contains(kindLabel)) detail else "$kindLabel $detail"
    }

    private fun parseVideo(video: Any): AioMediaSpec {
        val thumbPath = firstCandidatePath(videoThumbPathGetter.invokeMapPath(video))
        val filePath = videoFilePathGetter.invokeString(video)?.takeIf { it.isNotBlank() }
        return AioMediaSpec(
            localPath = firstCandidatePath(filePath, thumbPath),
            previewPath = firstCandidatePath(thumbPath, filePath),
            playbackPath = firstCandidatePath(filePath, thumbPath),
            width = videoThumbWidthGetter.invokeInt(video),
            height = videoThumbHeightGetter.invokeInt(video),
            durationSeconds = videoFileTimeGetter.invokeInt(video),
            fileName = videoFileNameGetter.invokeString(video),
            fileSize = videoFileSizeGetter.invokeLong(video)
        )
    }

    private fun parsePtt(ptt: Any): AioMediaSpec {
        return AioMediaSpec(
            localPath = pttFilePathGetter?.invokeString(ptt),
            durationSeconds = pttDurationGetter.invokeInt(ptt)
        )
    }

    private fun parseFile(file: Any): AioMediaSpec {
        return AioMediaSpec(
            localPath = fileThumbGetter?.invokeString(file) ?: filePathGetter?.invokeString(file),
            durationSeconds = fileVideoDurationGetter?.invokeInt(file) ?: 0,
            fileName = fileNameGetter.invokeString(file),
            fileSize = fileSizeGetter.invokeLong(file)
        )
    }

    private fun senderAvatar(senderUid: String, senderUin: Long, fragment: Any?): AvatarSpec? {
        if (senderUid.isBlank() && senderUin <= 0L) return null
        return AvatarSpec(
            chatType = 1,
            uid = senderUid,
            uin = senderUin,
            fragmentRef = WeakReference(fragment)
        )
    }

    private fun preferredLocalPath(vararg values: String?): String? {
        return values
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull { path ->
                path.startsWith("content://") ||
                    path.startsWith("file://") ||
                    File(path).exists()
            }
    }

    private fun firstCandidatePath(vararg values: String?): String? {
        return values
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull()
    }

    private fun normalizeRemoteUrl(value: String?): String? {
        val url = value?.trim()?.takeIf(String::isNotBlank) ?: return null
        return when {
            url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true) -> url
            url.startsWith("//") -> "https:$url"
            url.contains('.') && !url.startsWith("/") -> "https://$url"
            else -> url
        }
    }

    private fun readHostGrayTipText(data: Any): String? {
        return listOf("r", "grayTipText", "tipText")
            .firstNotNullOfOrNull { fieldName ->
                (data.findFieldValue(fieldName) as? CharSequence)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
            }
    }

    private fun readSenderName(data: Any, msgRecord: Any, senderUid: String): String {
        val hostName = (watchMsgShowNickNameField?.get(data) as? CharSequence)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
        if (hostName != null) return hostName
        readMemberInfoDisplayName(resolveMemberInfo(data, senderUid))?.let { return it }
        val memberName = sendMemberNameGetter?.invokeString(msgRecord)?.takeIf { it.isNotBlank() }
        if (memberName != null) return memberName
        val nickName = sendNickNameGetter?.invokeString(msgRecord)?.takeIf { it.isNotBlank() }
        if (nickName != null) return nickName
        return senderUid
    }

    private fun readSenderBadges(data: Any, msgRecord: Any): List<AioMessageBadge> {
        val senderUid = senderUidGetter.invokeString(msgRecord).orEmpty()
        val memberInfo = resolveMemberInfo(data, senderUid)
        buildMemberInfoBadge(memberInfo)?.let { return listOf(it) }

        val badges = linkedSetOf<AioMessageBadge>()
        addRoleBadge(badges, fromGuildRoleInfoGetter?.invokeAny(msgRecord))
        addRoleBadge(badges, fromChannelRoleInfoGetter?.invokeAny(msgRecord))
        addRoleBadge(badges, levelRoleInfoGetter?.invokeAny(msgRecord))
        if (badges.isNotEmpty()) return badges.toList()

        addFallbackRoleBadge(badges, memberInfo)
        return badges.toList()
    }

    private fun resolveMemberInfo(data: Any, senderUid: String): Any? {
        return watchMsgMemberInfoField?.get(data)
            ?: AioRuntimeStore.findMemberInfo(senderUid)
    }

    private fun readMemberInfoDisplayName(memberInfo: Any?): String? {
        if (memberInfo == null) return null
        val cardName = memberInfoCardNameGetter?.invokeString(memberInfo)?.trim().orEmpty()
        if (cardName.isNotEmpty()) return cardName
        val remark = memberInfoRemarkGetter?.invokeString(memberInfo)?.trim().orEmpty()
        if (remark.isNotEmpty()) return remark
        return memberInfoNickGetter?.invokeString(memberInfo)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun buildMemberInfoBadge(memberInfo: Any?): AioMessageBadge? {
        if (memberInfo == null) return null
        val roleValue = memberInfoRoleGetter?.invokeAny(memberInfo)
        val specialTitle = memberInfoSpecialTitleGetter?.invokeString(memberInfo)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val memberLevel = memberInfoLevelGetter?.invokeInt(memberInfo) ?: 0
        val type = when {
            roleValue.isOwnerRole() -> BadgeType.Owner
            roleValue.isAdminRole() -> BadgeType.Admin
            specialTitle != null -> BadgeType.Special
            else -> BadgeType.Normal
        }
        val text = buildString {
            append("LV")
            append(memberLevel.coerceAtLeast(0))
            when {
                specialTitle != null -> {
                    append(" ")
                    append(specialTitle)
                }

                type == BadgeType.Owner -> append(" 群主")
                type == BadgeType.Admin -> append(" 管理员")
            }
        }
        val colors = type.colors()
        return AioMessageBadge(
            text = text,
            colorArgb = colors.textColor,
            backgroundColorArgb = colors.backgroundColor
        )
    }

    private fun addRoleBadge(target: MutableSet<AioMessageBadge>, roleInfo: Any?) {
        if (roleInfo == null) return
        val text = fromRoleInfoNameGetter?.invokeString(roleInfo)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return
        val color = (fromRoleInfoColorGetter?.invokeAny(roleInfo) as? Number)?.toInt()
        target += AioMessageBadge(text = text, colorArgb = color)
    }

    private fun addFallbackRoleBadge(target: MutableSet<AioMessageBadge>, memberInfo: Any?) {
        if (memberInfo == null) return
        if (target.any { it.text == "群主" || it.text == "管理员" }) return
        val roleValue = memberInfoRoleGetter?.invokeAny(memberInfo)
        when {
            roleValue.isOwnerRole() -> {
                target += AioMessageBadge(
                    text = "群主",
                    colorArgb = COLOR_OWNER_TEXT,
                    backgroundColorArgb = COLOR_OWNER_BG
                )
            }

            roleValue.isAdminRole() -> {
                target += AioMessageBadge(
                    text = "管理员",
                    colorArgb = COLOR_ADMIN_TEXT,
                    backgroundColorArgb = COLOR_ADMIN_BG
                )
            }
        }
    }

    private fun readShowTimeDivider(data: Any): Boolean {
        return (watchMsgShowTimeStampFlagField?.get(data) as? Boolean) == true
    }

    private fun formatTimeDividerText(msgTime: Long): String {
        return formatMessageClock(msgTime)
    }

    private fun buildMessageKey(
        msgId: Long,
        msgSeq: Long,
        msgRandom: Long,
        msgTime: Long,
        senderUid: String,
        senderUin: Long
    ): String {
        return "$msgId:$msgSeq:$msgRandom:$msgTime:$senderUid:$senderUin"
    }

    private fun defaultMessageText(
        renderKind: AioMessageKind,
        rawKind: AioMessageKind,
        msgType: Int
    ): String {
        return when (renderKind) {
            AioMessageKind.Image -> "图片"
            AioMessageKind.Call -> "通话"
            AioMessageKind.Video -> "视频"
            AioMessageKind.Voice -> "语音"
            AioMessageKind.File -> "文件"
            AioMessageKind.Tip -> "系统提示"
            AioMessageKind.Text -> ""
            else -> rawKind.takeIf { it != AioMessageKind.Unsupported }
                ?.let { "[${it.displayName}]" }
                ?: "暂不支持的消息类型 $msgType"
        }
    }

    private fun View.findFirstEditText(): EditText? {
        var target: EditText? = null
        forEachDescendant { view ->
            if (view is EditText) {
                target = view
                true
            } else {
                false
            }
        }
        return target
    }

    private fun formatMessageClock(value: Long): String {
        if (value <= 0L) return ""
        val millis = if (value > 100_000_000_000L) value else value * 1000L
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
    }

    private fun View.clearHostDraftText() {
        findFirstEditText()?.let { editText ->
            editText.text?.clear()
            editText.setText("")
        }
    }

    private fun View.findHostActionView(vararg tokens: String): View? {
        val normalizedTokens = tokens.map { it.lowercase() }
        var target: View? = null
        forEachDescendant { view ->
            val signature = view.hostActionSignature()
            if ((view.hasOnClickListeners() || view.isClickable) &&
                normalizedTokens.any { signature.contains(it) }
            ) {
                target = view
                true
            } else {
                false
            }
        }
        return target
    }

    private fun View.hostActionSignature(): String {
        val parts = mutableListOf(javaClass.name)
        contentDescription?.toString()?.let(parts::add)
        if (this is TextView) {
            text?.toString()?.let(parts::add)
            hint?.toString()?.let(parts::add)
        }
        if (id != View.NO_ID) {
            runCatching { resources.getResourceEntryName(id) }.getOrNull()?.let(parts::add)
        }
        return parts.joinToString(" ").lowercase()
    }

    private data class ParsedMessage(
        val renderKind: AioMessageKind = AioMessageKind.Unsupported,
        val rawKind: AioMessageKind = AioMessageKind.Unsupported,
        val text: String = "",
        val media: AioMediaSpec? = null,
        val forwardPreview: AioForwardPreview? = null
    )

    companion object {
        private const val PIC_THUMB_SIZE_SMALL = 198
        private const val PIC_THUMB_SIZE_ORIGIN = 0
        private const val PIC_THUMB_SIZE_LARGE = 720
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val FORWARD_COUNT_REGEX = Regex("(\\d+)\\s*条(?:聊天记录|转发消息)")
        private val MULTI_FORWARD_TITLE_REGEX = Regex(
            "<title[^>]*>(.*?)</title>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val MULTI_FORWARD_SUMMARY_REGEX = Regex(
            "<summary[^>]*>(.*?)</summary>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val MULTI_FORWARD_TSUM_REGEX = Regex(
            "\\btSum=[\"'](\\d+)[\"']",
            RegexOption.IGNORE_CASE
        )
        private val VIDEO_AV_RECORD_TYPES = setOf(2, 3, 4, 5, 6, 12, 26, 28)
        private const val REQUEST_KEY = "request_key"
        private const val EMOTION_CATEGORY_FAVORITE = "favorite"
        private const val EMOTION_CATEGORY_SYSTEM = "system"
        private const val EMOTION_CATEGORY_BIG_SYSTEM = "big-system"
        private const val EMOTION_CATEGORY_HOT = "hot"
        private const val SYSTEM_EMOTION_TYPE_FACE = 1
        private const val SYSTEM_EMOTION_TYPE_EMOJI = 2
        private val HIDDEN_BIG_STICKER_CODES = setOf(392, 393, 394)

        private const val MOBILE_QQ_CLASS = "mqq.app.MobileQQ"
        private const val Q_ROUTE_CLASS = "com.tencent.mobileqq.qroute.QRoute"
        private const val I_KERNEL_SERVICE_CLASS = "com.tencent.qqnt.kernel.api.IKernelService"
        private const val HOST_CONTACT_CLASS = "com.tencent.qqnt.kernel.nativeinterface.Contact"
        private const val HOST_MSG_SERVICE_API_CLASS = "com.tencent.qqnt.msg.api.IMsgService"
        private const val I_WATCH_QAV_FACADE_CLASS = "com.tencent.qqnt.watch.IWatchQavFacade"
        private const val I_KERNEL_MSG_LISTENER_CLASS =
            "com.tencent.qqnt.kernel.nativeinterface.IKernelMsgListener"
        private const val GPRO_EMOJI_DOWNLOAD_PARAMS_CLASS =
            "com.tencent.qqnt.kernel.nativeinterface.GproEmojiDownloadParams"
        private const val FETCH_FAV_EMOJI_CALLBACK_CLASS =
            "com.tencent.qqnt.kernel.nativeinterface.IFetchFavEmojiListCallback"
        private const val GET_HOT_PIC_CALLBACK_CLASS =
            "com.tencent.qqnt.kernel.nativeinterface.IGetHotPicInfoListCallback"
        private const val QQ_SYS_FACE_UTIL_CLASS = "com.tencent.mobileqq.emoticon.QQSysFaceUtil"
        private const val QQ_EMOJI_UTIL_CLASS = "com.tencent.mobileqq.emoticon.QQEmojiUtil"
        private const val SYSTEM_EMOJI_DATA_MANAGER_CLASS =
            "com.tencent.qqnt.emotion.data.SystemAndEmojiDataManager"
        private const val EMO_MSG_UTILS_CLASS = "com.tencent.qqnt.watch.emotion.popemo.EmoMsgUtils"
        private const val RECENT_USE_DATA_SOURCE_CLASS =
            "com.tencent.qqnt.watch.emotion.recentuse.RecentUseDataSource"
        private const val MSG_UTIL_CLASS = "com.tencent.watch.aio_impl.ext.MsgUtil"
        private const val MARKET_EMOTION_SEND_RUNNABLE_CLASS = "d.c.q.a.a.e.f"
        private const val MEMBER_INFO_CLASS = "com.tencent.qqnt.kernel.nativeinterface.MemberInfo"
        private const val FROM_ROLE_INFO_CLASS = "com.tencent.qqnt.kernel.nativeinterface.FromRoleInfo"
        private const val NICK_NAME_ABILITY_CLASS =
            "com.tencent.watch.aio_impl.coreImpl.repo.NickNameAbility"
        private const val KERNEL_SERVICE_UTIL_CLASS = "com.tencent.qqnt.msg.KernelServiceUtil"
        private const val GROUP_MEMBER_LIST_CALLBACK_CLASS =
            "com.tencent.qqnt.kernel.nativeinterface.IGroupMemberListCallback"
        private const val GROUP_MEMBER_LIST_RESULT_CLASS =
            "com.tencent.qqnt.kernel.nativeinterface.GroupMemberListResult"
        private const val COLOR_SPECIAL_BG = 0xFF30263F.toInt()
        private const val COLOR_SPECIAL_TEXT = 0xFFBB87F7.toInt()
        private const val COLOR_NORMAL_BG = 0xFF2E2E2E.toInt()
        private const val COLOR_NORMAL_TEXT = 0xFF9E9E9E.toInt()
        private const val COLOR_ADMIN_BG = 0xFF0E2D41.toInt()
        private const val COLOR_ADMIN_TEXT = 0xFF0088EE.toInt()
        private const val COLOR_OWNER_BG = 0xFF412917.toInt()
        private const val COLOR_OWNER_TEXT = 0xFFFF8D40.toInt()
        private const val HOST_IMAGE_SUB_TYPE = 0
        private const val SINGLE_CHAT_TYPE = 1

        fun create(classLoader: ClassLoader): AioHookState {
            val watchAIOFragmentClass = classLoader.findTargetClass(WATCH_AIO_FRAGMENT_CLASS)
            val fragmentClass = classLoader.findTargetClass(FRAGMENT_CLASS)
            val absMsgListVBClass = classLoader.findTargetClass(ABS_MSG_LIST_VB_CLASS)
            val listUIOperationApiImplClass =
                classLoader.findOptionalClass(LIST_UI_OPERATION_API_IMPL_CLASS)
            val dataSubmitActionClass = classLoader.findOptionalClass(DATA_SUBMIT_ACTION_CLASS)
            val aioMsgViewHolderClass = classLoader.findTargetClass(AIO_MSG_VIEW_HOLDER_CLASS)
            val iMsgItemClass = classLoader.findTargetClass(I_MSG_ITEM_CLASS)
            val watchMsgItemClass = classLoader.findTargetClass(WATCH_AIO_MSG_ITEM_CLASS)
            val nickNameAbilityClass = classLoader.findOptionalClass(NICK_NAME_ABILITY_CLASS)
            val nickNameAbilityMemberInfoCacheField = nickNameAbilityClass?.let {
                findOptionalField(it, "memberInfoCache")
            }
            val kernelServiceUtilClass = classLoader.findOptionalClass(KERNEL_SERVICE_UTIL_CLASS)
            val groupMemberListCallbackClass =
                classLoader.findOptionalClass(GROUP_MEMBER_LIST_CALLBACK_CLASS)
            val groupMemberListResultClass =
                classLoader.findOptionalClass(GROUP_MEMBER_LIST_RESULT_CLASS)
            val watchMsgListRepoClass = classLoader.findOptionalClass(WATCH_MSG_LIST_REPO_CLASS)
            val watchAioListVbClass = classLoader.findOptionalClass(WATCH_AIO_LIST_VB_CLASS)
            val watchAioListVmClass = classLoader.findOptionalClass(WATCH_AIO_LIST_VM_CLASS)
            val inputBarControllerClass = classLoader.findOptionalClass(INPUT_BAR_CONTROLLER_CLASS)
            val inputBarEmotionClickClass =
                classLoader.findOptionalClass(INPUT_BAR_EMOTION_CLICK_CLASS)
            val imeTextUtilClass = classLoader.findOptionalClass(IME_TEXT_UTIL_CLASS)
            val imeTextUtilCompanion = imeTextUtilClass?.getDeclaredField("a")
                ?.apply { isAccessible = true }
                ?.get(null)
            val topGestureLayoutClass = classLoader.findOptionalClass(TOP_GESTURE_LAYOUT_CLASS)
            val msgRecordClass = classLoader.findTargetClass(MSG_RECORD_CLASS)
            val msgElementClass = classLoader.findTargetClass(MSG_ELEMENT_CLASS)
            val textElementClass = classLoader.findTargetClass(TEXT_ELEMENT_CLASS)
            val picElementClass = classLoader.findTargetClass(PIC_ELEMENT_CLASS)
            val watchPicElementExtClass = classLoader.findOptionalClass(WATCH_PIC_ELEMENT_EXT_CLASS)
            val multiForwardElementClass = classLoader.findTargetClass(MULTI_FORWARD_MSG_ELEMENT_CLASS)
            val avRecordElementClass = classLoader.findTargetClass(AV_RECORD_ELEMENT_CLASS)
            val videoElementClass = classLoader.findTargetClass(VIDEO_ELEMENT_CLASS)
            val pttElementClass = classLoader.findTargetClass(PTT_ELEMENT_CLASS)
            val fileElementClass = classLoader.findTargetClass(FILE_ELEMENT_CLASS)
            val memberInfoClass = classLoader.findOptionalClass(MEMBER_INFO_CLASS)
            val fromRoleInfoClass = classLoader.findOptionalClass(FROM_ROLE_INFO_CLASS)
            val watchAvatarViewClass = classLoader.findOptionalClass(WATCH_AVATAR_VIEW_CLASS)
            val avatarFacadeClass = classLoader.findOptionalClass(AVATAR_FACADE_CLASS)
            val avatarTargetClass = classLoader.findOptionalClass(I_AVATAR_TARGET_CLASS)
            val avatarRequestLoadClass = classLoader.findOptionalClass(I_AVATAR_REQUEST_LOAD_CLASS)
            val avatarOptionClass = classLoader.findOptionalClass(AVATAR_OPTION_CLASS)
            val avatarOptionBuilderClass = classLoader.findOptionalClass(AVATAR_OPTION_BUILDER_CLASS)
            val avatarSizeTypeClass = classLoader.findOptionalClass(AVATAR_SIZE_TYPE_CLASS)
            val coroutineScopeClass = classLoader.findOptionalClass(COROUTINE_SCOPE_CLASS)
            val lifecycleOwnerClass = classLoader.findOptionalClass(LIFECYCLE_OWNER_CLASS)
            val lifecycleOwnerKtClass = classLoader.findOptionalClass(LIFECYCLE_OWNER_KT_CLASS)
            val avatarFacadeCompanion =
                avatarFacadeClass?.getDeclaredField("a")?.apply { isAccessible = true }?.get(null)

            return AioHookState(
                classLoader = classLoader,
                watchAIOFragmentClass = watchAIOFragmentClass,
                fragmentClass = fragmentClass,
                requireArgumentsMethod = fragmentClass.requiredMethod("requireArguments"),
                viewPagerField = findOptionalField(watchAIOFragmentClass, "f"),
                absMsgListVBClass = absMsgListVBClass,
                listUIOperationApiImplClass = listUIOperationApiImplClass,
                listSubmitMethod = listUIOperationApiImplClass?.let { apiClass ->
                    dataSubmitActionClass?.let { actionClass ->
                        apiClass.optionalMethod("A", actionClass)
                    }
                },
                listReadOnlyListMethod = listUIOperationApiImplClass?.optionalMethod("m"),
                listRecyclerViewMethod = listUIOperationApiImplClass?.optionalMethod("j"),
                aioMsgViewHolderClass = aioMsgViewHolderClass,
                iMsgItemClass = iMsgItemClass,
                watchMsgItemClass = watchMsgItemClass,
                nickNameAbilityClass = nickNameAbilityClass,
                nickNameAbilityInjectMethod = nickNameAbilityClass?.optionalMethod("a", List::class.java),
                nickNameAbilityMemberInfoCacheField = nickNameAbilityMemberInfoCacheField,
                watchMsgListRepoClass = watchMsgListRepoClass,
                watchAioListVbClass = watchAioListVbClass,
                watchAioListCreateViewMethod = watchAioListVbClass?.declaredMethods
                    ?.firstOrNull { method ->
                        method.name == "i" &&
                            method.parameterTypes.size == 3 &&
                            View::class.java.isAssignableFrom(method.returnType)
                    }
                    ?.apply { isAccessible = true },
                inputBarControllerField = watchAioListVbClass?.let {
                    findOptionalField(it, "J")
                },
                inputBarIconFaceField = inputBarControllerClass?.let {
                    findOptionalField(it, "n")
                },
                inputBarImeContentField = inputBarControllerClass?.let {
                    findOptionalField(it, "o")
                },
                inputBarEmotionClickConstructor = inputBarEmotionClickClass
                    ?.getDeclaredConstructor(java.lang.Boolean.TYPE)
                    ?.apply { isAccessible = true },
                inputBarIntentDispatchMethod = watchAioListVbClass?.methods?.firstOrNull { method ->
                    method.name == "O" &&
                        method.parameterTypes.size == 1 &&
                        inputBarEmotionClickClass?.let { method.parameterTypes[0].isAssignableFrom(it) } == true
                }?.apply { isAccessible = true },
                watchAioListVmClass = watchAioListVmClass,
                watchAioListVmSendElementsMethod = watchAioListVmClass
                    ?.optionalMethod("J", java.util.ArrayList::class.java),
                kernelGroupServiceUtilCompanion = kernelServiceUtilClass,
                kernelGroupServiceGetterMethod = kernelServiceUtilClass?.methods?.firstOrNull {
                    it.name == "c" && it.parameterTypes.isEmpty()
                }?.apply { isAccessible = true },
                groupMemberListCallbackClass = groupMemberListCallbackClass,
                groupMemberListResultInfosField = groupMemberListResultClass?.let {
                    findOptionalField(it, "infos")
                },
                imeTextUtilCompanion = imeTextUtilCompanion,
                imeTextToElementsMethod = imeTextUtilCompanion?.javaClass?.declaredMethods
                    ?.firstOrNull { method ->
                        method.name == "b" &&
                            method.parameterTypes.contentEquals(arrayOf(String::class.java))
                    }
                    ?.apply { isAccessible = true },
                repoConstructor = watchMsgListRepoClass?.declaredConstructors
                    ?.let { constructors ->
                        constructors.firstOrNull { it.parameterTypes.size == 2 }
                            ?: constructors.firstOrNull()
                    }
                    ?.apply { isAccessible = true },
                repoLoadOlderMethod = watchMsgListRepoClass?.optionalMethod("D"),
                repoAddLocalSendMethod = watchMsgListRepoClass?.optionalMethod(
                    "a",
                    watchMsgItemClass
                ),
                repoClearMethod = watchMsgListRepoClass?.optionalMethod("b"),
                repoReceiveMethod = watchMsgListRepoClass?.optionalMethod("c", List::class.java),
                repoDeleteMethod = watchMsgListRepoClass?.optionalMethod("d", List::class.java),
                repoUpdateMethod = watchMsgListRepoClass?.optionalMethod("g", List::class.java),
                topGestureLayoutClass = topGestureLayoutClass,
                topGestureInterceptMethod = topGestureLayoutClass?.optionalMethod(
                    "onInterceptTouchEvent",
                    android.view.MotionEvent::class.java
                ),
                topGestureSetInterceptTouchFlagMethod = topGestureLayoutClass?.optionalMethod(
                    "setInterceptTouchFlag",
                    java.lang.Boolean.TYPE
                ),
                watchAioSwipeEnableMethod = watchAIOFragmentClass.optionalMethod("Z"),
                itemViewField = findRequiredField(aioMsgViewHolderClass, "itemView"),
                msgRecordField = findRequiredField(watchMsgItemClass, "d"),
                isSelfMethod = watchMsgItemClass.requiredMethod("h"),
                msgIdGetter = msgRecordClass.requiredMethod("getMsgId"),
                msgSeqGetter = msgRecordClass.requiredMethod("getMsgSeq"),
                msgRandomGetter = msgRecordClass.requiredMethod("getMsgRandom"),
                msgTimeGetter = msgRecordClass.requiredMethod("getMsgTime"),
                timeStampGetter = msgRecordClass.requiredMethod("getTimeStamp"),
                msgTypeGetter = msgRecordClass.requiredMethod("getMsgType"),
                senderUidGetter = msgRecordClass.requiredMethod("getSenderUid"),
                senderUinGetter = msgRecordClass.requiredMethod("getSenderUin"),
                sendNickNameGetter = msgRecordClass.optionalMethod("getSendNickName"),
                sendMemberNameGetter = msgRecordClass.optionalMethod("getSendMemberName"),
                fromGuildRoleInfoGetter = msgRecordClass.optionalMethod("getFromGuildRoleInfo"),
                fromChannelRoleInfoGetter = msgRecordClass.optionalMethod("getFromChannelRoleInfo"),
                levelRoleInfoGetter = msgRecordClass.optionalMethod("getLevelRoleInfo"),
                elementsGetter = msgRecordClass.requiredMethod("getElements"),
                watchMsgShowNickNameField = findOptionalField(watchMsgItemClass, "showNickName"),
                watchMsgShowTimeStampFlagField = findOptionalField(watchMsgItemClass, "showTimeStampFlag"),
                watchMsgMemberInfoField = findOptionalField(watchMsgItemClass, "memberInfo"),
                memberInfoCardNameGetter = memberInfoClass?.optionalMethod("getCardName"),
                memberInfoRemarkGetter = memberInfoClass?.optionalMethod("getRemark"),
                memberInfoNickGetter = memberInfoClass?.optionalMethod("getNick"),
                memberInfoSpecialTitleGetter = memberInfoClass?.optionalMethod("getMemberSpecialTitle"),
                memberInfoRoleGetter = memberInfoClass?.optionalMethod("getRole"),
                memberInfoLevelGetter = memberInfoClass?.optionalMethod("getMemberLevel"),
                fromRoleInfoNameGetter = fromRoleInfoClass?.optionalMethod("getName"),
                fromRoleInfoColorGetter = fromRoleInfoClass?.optionalMethod("getColor"),
                elementTypeGetter = msgElementClass.requiredMethod("getElementType"),
                textElementGetter = msgElementClass.requiredMethod("getTextElement"),
                picElementGetter = msgElementClass.requiredMethod("getPicElement"),
                multiForwardElementGetter = msgElementClass.requiredMethod("getMultiForwardMsgElement"),
                avRecordElementGetter = msgElementClass.requiredMethod("getAvRecordElement"),
                videoElementGetter = msgElementClass.requiredMethod("getVideoElement"),
                pttElementGetter = msgElementClass.requiredMethod("getPttElement"),
                fileElementGetter = msgElementClass.requiredMethod("getFileElement"),
                grayTipElementGetter = msgElementClass.requiredMethod("getGrayTipElement"),
                textContentGetter = textElementClass.requiredMethod("getContent"),
                multiForwardXmlContentGetter = multiForwardElementClass.requiredMethod("getXmlContent"),
                multiForwardFileNameGetter = multiForwardElementClass.requiredMethod("getFileName"),
                avRecordTextGetter = avRecordElementClass.requiredMethod("getText"),
                avRecordTypeGetter = avRecordElementClass.requiredMethod("getType"),
                picSourcePathGetter = picElementClass.requiredMethod("getSourcePath"),
                picThumbPathGetter = picElementClass.requiredMethod("getThumbPath"),
                picOriginUrlGetter = picElementClass.requiredMethod("getOriginImageUrl"),
                picWidthGetter = picElementClass.requiredMethod("getPicWidth"),
                picHeightGetter = picElementClass.requiredMethod("getPicHeight"),
                picFileNameGetter = picElementClass.requiredMethod("getFileName"),
                watchPicThumbPathMethod = watchPicElementExtClass?.optionalMethod(
                    "G0",
                    picElementClass,
                    Integer.TYPE
                ),
                watchPicOriginPathMethod = watchPicElementExtClass?.optionalMethod(
                    "D0",
                    picElementClass
                ),
                videoFileNameGetter = videoElementClass.requiredMethod("getFileName"),
                videoFilePathGetter = videoElementClass.requiredMethod("getFilePath"),
                videoFileSizeGetter = videoElementClass.requiredMethod("getFileSize"),
                videoFileTimeGetter = videoElementClass.requiredMethod("getFileTime"),
                videoThumbPathGetter = videoElementClass.requiredMethod("getThumbPath"),
                videoThumbWidthGetter = videoElementClass.requiredMethod("getThumbWidth"),
                videoThumbHeightGetter = videoElementClass.requiredMethod("getThumbHeight"),
                pttDurationGetter = pttElementClass.requiredMethod("getDuration"),
                pttTextGetter = pttElementClass.optionalMethod("getText"),
                pttFilePathGetter = pttElementClass.optionalMethod("getFilePath"),
                fileNameGetter = fileElementClass.requiredMethod("getFileName"),
                filePathGetter = fileElementClass.optionalMethod("getFilePath"),
                fileSizeGetter = fileElementClass.requiredMethod("getFileSize"),
                fileThumbGetter = fileElementClass.optionalMethod("getPicThumbPath"),
                fileVideoDurationGetter = fileElementClass.optionalMethod("getVideoDuration"),
                watchAvatarConstructor = watchAvatarViewClass?.getDeclaredConstructor(
                    Context::class.java,
                    AttributeSet::class.java,
                    Integer.TYPE,
                    Integer.TYPE
                )?.apply { isAccessible = true },
                avatarFacadeCompanion = avatarFacadeCompanion,
                avatarFacadeContextMethod = avatarFacadeCompanion?.javaClass?.methods?.firstOrNull {
                    it.name == "b" && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0].isAssignableFrom(Context::class.java)
                }?.apply { isAccessible = true },
                avatarFacadeFragmentMethod = avatarFacadeCompanion?.javaClass?.methods?.firstOrNull {
                    it.name == "c" && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0].name == FRAGMENT_CLASS
                }?.apply { isAccessible = true },
                avatarTargetBindMethod = avatarTargetClass?.optionalMethod("b", View::class.java),
                avatarRequestOptionMethod = avatarRequestLoadClass?.let { requestClass ->
                    avatarOptionClass?.let { requestClass.optionalMethod("b", it) }
                },
                avatarOptionBuilderClass = avatarOptionBuilderClass,
                avatarOptionBuilderSizeMethod = avatarOptionBuilderClass?.methods?.firstOrNull {
                    it.name == "b" && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0].name == AVATAR_SIZE_TYPE_CLASS
                }?.apply { isAccessible = true },
                avatarOptionBuilderBuildMethod = avatarOptionBuilderClass?.optionalMethod("a"),
                avatarSizeType = avatarSizeTypeClass?.enumConstants
                    ?.firstOrNull { (it as? Enum<*>)?.name == "SMALL" }
                    ?: avatarSizeTypeClass?.getDeclaredField("b")?.apply { isAccessible = true }
                        ?.get(null),
                avatarRequestLoadUidMethod = avatarRequestLoadClass?.let {
                    coroutineScopeClass?.let { scopeClass ->
                        it.optionalMethod(
                            "d",
                            String::class.java,
                            java.lang.Long.TYPE,
                            scopeClass
                        )
                    }
                },
                avatarRequestLoadUinMethod = avatarRequestLoadClass?.let {
                    coroutineScopeClass?.let { scopeClass ->
                        it.optionalMethod("e", java.lang.Long.TYPE, scopeClass)
                    }
                },
                lifecycleScopeMethod = lifecycleOwnerClass?.let { ownerClass ->
                    lifecycleOwnerKtClass?.optionalMethod("a", ownerClass)
                }
            )
        }

        private fun Class<*>.requiredMethod(name: String, vararg parameterTypes: Class<*>): Method {
            return getDeclaredMethod(name, *parameterTypes).apply { isAccessible = true }
        }

        private fun Class<*>.optionalMethod(name: String, vararg parameterTypes: Class<*>): Method? {
            return runCatching {
                getDeclaredMethod(name, *parameterTypes).apply { isAccessible = true }
            }.getOrNull()
        }
    }
}

private fun Method.invokeAny(target: Any): Any? {
    return runCatching { invoke(target) }.getOrNull()
}

private fun Method.invokeString(target: Any): String? {
    return invokeAny(target)?.toString()
}

private fun Method.invokeStringOrNull(target: Any?, vararg args: Any?): String? {
    return runCatching { invoke(target, *args)?.toString()?.takeIf { it.isNotBlank() } }
        .getOrNull()
}

private fun Method.invokeLong(target: Any): Long {
    return (invokeAny(target) as? Number)?.toLong() ?: 0L
}

private fun Method.invokeInt(target: Any): Int {
    return (invokeAny(target) as? Number)?.toInt() ?: 0
}

private fun Method.invokeMapPath(target: Any): String? {
    val map = invokeAny(target) as? Map<*, *> ?: return null
    return map.values.firstNotNullOfOrNull { value ->
        value?.toString()?.takeIf { it.isNotBlank() }
    }
}

private fun Class<*>.setIntFieldIfPresent(target: Any, name: String, value: Int) {
    runCatching {
        findOptionalField(this, name)?.setInt(target, value)
    }
}

private fun Class<*>.setObjectFieldIfPresent(target: Any, name: String, value: Any?) {
    runCatching {
        findOptionalField(this, name)?.set(target, value)
    }
}

private fun Any?.invokeFunction0(): Boolean {
    if (this == null) return false
    return runCatching {
        javaClass.methods.firstOrNull { method ->
            method.name == "invoke" && method.parameterTypes.isEmpty()
        }?.invoke(this) ?: return false
        true
    }.getOrDefault(false)
}

private fun Any?.isOwnerRole(): Boolean {
    return when (this) {
        is Number -> toInt() == 2
        is Enum<*> -> name.contains("OWNER", ignoreCase = true)
        else -> toString().contains("owner", ignoreCase = true) || toString().contains("群主")
    }
}

private fun Any?.isAdminRole(): Boolean {
    return when (this) {
        is Number -> toInt() == 1
        is Enum<*> -> name.contains("ADMIN", ignoreCase = true)
        else -> toString().contains("admin", ignoreCase = true) || toString().contains("管理员")
    }
}

private enum class BadgeType {
    Owner,
    Admin,
    Special,
    Normal
}

private data class BadgeColors(
    val backgroundColor: Int,
    val textColor: Int
)

private fun BadgeType.colors(): BadgeColors {
    return when (this) {
        BadgeType.Owner -> BadgeColors(0xFF412917.toInt(), 0xFFFF8D40.toInt())
        BadgeType.Admin -> BadgeColors(0xFF0E2D41.toInt(), 0xFF0088EE.toInt())
        BadgeType.Special -> BadgeColors(0xFF30263F.toInt(), 0xFFBB87F7.toInt())
        BadgeType.Normal -> BadgeColors(0xFF2E2E2E.toInt(), 0xFF9E9E9E.toInt())
    }
}
