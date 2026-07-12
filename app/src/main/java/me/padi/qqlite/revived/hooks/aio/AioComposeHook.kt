package me.padi.qqlite.revived.hooks.aio

import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.padi.qqlite.revived.ModuleMainKt
import me.padi.qqlite.revived.compose.screens.aio.AioChatScreen
import me.padi.qqlite.revived.hooks.common.BaseHook
import me.padi.qqlite.revived.shared.model.aio.AioUiState
import me.padi.qqlite.revived.utils.addHostComposeView
import java.lang.ref.WeakReference

internal object AioComposeHook : BaseHook() {
    private var fragmentHookInstalled = false
    private var messageHookInstalled = false
    private var listUiHookInstalled = false
    private var repoHookInstalled = false
    private var watchAioListVbHookInstalled = false
    private var nickNameAbilityHookInstalled = false
    private var hookState: AioHookState? = null

    override fun reset() {
        fragmentHookInstalled = false
        messageHookInstalled = false
        listUiHookInstalled = false
        repoHookInstalled = false
        watchAioListVbHookInstalled = false
        nickNameAbilityHookInstalled = false
        hookState = null
        AioRuntimeStore.reset()
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        val targetClassLoader = requireClassLoader(classLoader)
        val state = hookState ?: AioHookState.create(targetClassLoader).also { hookState = it }
        hookFragment(module, state)
        hookMessageBind(module, state)
        hookListUiOperation(module, state)
        hookMsgListRepo(module, state)
        hookWatchAioListVb(module, state)
        hookNickNameAbility(module, state)
    }

    private fun hookFragment(module: ModuleMainKt, state: AioHookState) {
        if (fragmentHookInstalled) return

        runCatching {
            module.intercept(
                state.watchAIOFragmentClass.getDeclaredMethod(
                    "a0",
                    LayoutInflater::class.java,
                    ViewGroup::class.java,
                    Bundle::class.java
                )
            ) {
                val fragment = thisObject
                AioRuntimeStore.creatingFragment = WeakReference(fragment ?: Any())
                try {
                    val result = proceed()
                    runCatching {
                        val root = result as? ViewGroup ?: return@runCatching
                        val viewPager = state.viewPagerField?.get(fragment) as? View
                            ?: root.findFirstDescendantByClassName(VIEW_PAGER_CLASS)
                        val peer = state.readPeerInfo(fragment) ?: return@runCatching
                        root.bindComposeAio(module, state, fragment, viewPager, peer)
                    }.onFailure {
                        module.logHook(Log.WARN, "AIO compose bind skipped", it)
                    }
                    result
                } finally {
                    AioRuntimeStore.creatingFragment = null
                }
            }
            module.intercept(
                state.watchAIOFragmentClass.getDeclaredMethod("onDestroy")
            ) {
                AioRuntimeStore.findBindingForFragment(thisObject)?.clearTransientStateForExit()
                val result = proceed()
                AioRuntimeStore.releaseAioSurfaceForHome()
                result
            }
            fragmentHookInstalled = true
            module.logHook(Log.INFO, "AIO compose fragment hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "AIO compose fragment hook skipped", it)
        }
    }

    private fun hookMessageBind(module: ModuleMainKt, state: AioHookState) {
        if (messageHookInstalled) return

        runCatching {
            module.intercept(
                state.absMsgListVBClass.getDeclaredMethod(
                    "j",
                    state.aioMsgViewHolderClass,
                    Integer.TYPE,
                    state.iMsgItemClass,
                    List::class.java
                )
            ) {
                val result = proceed()
                runCatching {
                    val creatingFragment = AioRuntimeStore.creatingFragment?.get()
                    val holder = args.getOrNull(0)
                    val itemView = runCatching {
                        state.itemViewField.get(holder) as? View
                    }.getOrNull()
                    val binding = AioRuntimeStore.findBindingForFragment(creatingFragment)
                        ?: AioRuntimeStore.findBindingForView(itemView)
                        ?: AioRuntimeStore.latestBinding?.get()
                            ?.takeIf {
                                creatingFragment == null || it.hostFragment() === creatingFragment
                            }
                    val message = state.toMessage(
                        data = args.getOrNull(2),
                        holder = holder,
                        hostFragment = binding?.hostFragment() ?: creatingFragment
                    ) ?: return@runCatching
                    if (binding == null) {
                        AioRuntimeStore.bufferMessage(creatingFragment, message)
                    } else {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            binding.enqueueMessage(message)
                        } else {
                            AioRuntimeStore.mainHandler.post {
                                binding.enqueueMessage(message)
                            }
                        }
                    }
                }.onFailure {
                    module.logHook(Log.WARN, "AIO compose message capture skipped", it)
                }
                result
            }
            messageHookInstalled = true
            module.logHook(Log.INFO, "AIO compose message hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "AIO compose message hook skipped", it)
        }
    }

    private fun hookListUiOperation(module: ModuleMainKt, state: AioHookState) {
        if (listUiHookInstalled) return
        val submitMethod = state.listSubmitMethod ?: return

        runCatching {
            module.intercept(submitMethod) {
                val operation = thisObject
                val result = proceed()
                scheduleListUiSync(module, state, operation)
                result
            }
            listUiHookInstalled = true
            module.logHook(Log.INFO, "AIO compose list UI hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "AIO compose list UI hook skipped", it)
        }
    }

    private fun hookMsgListRepo(module: ModuleMainKt, state: AioHookState) {
        if (repoHookInstalled) return

        var installedCount = 0
        state.repoConstructor?.let { constructor ->
            runCatching {
                module.hookConstructor(constructor) {
                    val result = proceed()
                    AioRuntimeStore.rememberMsgRepo(thisObject)
                    resolveBindingForRepo(state, thisObject)
                    result
                }
                installedCount++
            }.onFailure {
                module.logHook(Log.WARN, "AIO compose repo constructor hook skipped", it)
            }
        }
        state.repoLoadOlderMethod?.let { method ->
            runCatching {
                module.intercept(method) {
                    resolveBindingForRepo(state, thisObject)?.attachHostMsgRepo(thisObject)
                    proceed()
                }
                installedCount++
            }.onFailure {
                module.logHook(Log.WARN, "AIO compose repo load older hook skipped", it)
            }
        }
        state.repoAddLocalSendMethod?.let { method ->
            runCatching {
                module.intercept(method) {
                    val repo = thisObject
                    val row = args.getOrNull(0)
                    val result = proceed()
                    resolveBindingForRepo(state, repo)?.upsertMessageFromHost(row)
                    result
                }
                installedCount++
            }.onFailure {
                module.logHook(Log.WARN, "AIO compose repo local send hook skipped", it)
            }
        }
        state.repoReceiveMethod?.let { method ->
            runCatching {
                module.intercept(method) {
                    val repo = thisObject
                    val rows = (args.getOrNull(0) as? Iterable<*>)?.toList()
                    val result = proceed()
                    resolveBindingForRepo(state, repo)?.upsertMessagesFromHost(rows)
                    result
                }
                installedCount++
            }.onFailure {
                module.logHook(Log.WARN, "AIO compose repo receive hook skipped", it)
            }
        }
        state.repoUpdateMethod?.let { method ->
            runCatching {
                module.intercept(method) {
                    val repo = thisObject
                    val rows = (args.getOrNull(0) as? Iterable<*>)?.toList()
                    val result = proceed()
                    resolveBindingForRepo(state, repo)?.upsertMessagesFromHost(rows)
                    result
                }
                installedCount++
            }.onFailure {
                module.logHook(Log.WARN, "AIO compose repo update hook skipped", it)
            }
        }
        state.repoDeleteMethod?.let { method ->
            runCatching {
                module.intercept(method) {
                    val repo = thisObject
                    val ids = (args.getOrNull(0) as? Iterable<*>)?.toList()
                    val result = proceed()
                    resolveBindingForRepo(state, repo)?.removeMessagesByIds(ids)
                    result
                }
                installedCount++
            }.onFailure {
                module.logHook(Log.WARN, "AIO compose repo delete hook skipped", it)
            }
        }
        state.repoClearMethod?.let { method ->
            runCatching {
                module.intercept(method) {
                    val repo = thisObject
                    val result = proceed()
                    resolveBindingForRepo(state, repo)?.clearMessages()
                    result
                }
                installedCount++
            }.onFailure {
                module.logHook(Log.WARN, "AIO compose repo clear hook skipped", it)
            }
        }

        if (installedCount > 0) {
            repoHookInstalled = true
            module.logHook(Log.INFO, "AIO compose repo hook installed: $installedCount")
        } else {
            module.logHook(Log.WARN, "AIO compose repo hook skipped: no method resolved")
        }
    }

    private fun hookWatchAioListVb(module: ModuleMainKt, state: AioHookState) {
        if (watchAioListVbHookInstalled) return
        val createViewMethod = state.watchAioListCreateViewMethod ?: return

        runCatching {
            module.intercept(createViewMethod) {
                val result = proceed()
                val listVb = thisObject
                AioRuntimeStore.rememberAioListVb(listVb)
                val inputBar = state.readInputBarController(listVb)
                val listView = state.readListUiView(args.getOrNull(2))
                AioRuntimeStore.rememberInputBarController(inputBar)
                AioRuntimeStore.latestBinding?.get()?.let { binding ->
                    binding.attachHostAioListVb(listVb)
                    binding.attachInputBarController(inputBar)
                    binding.attachHostListView(listView)
                }
                result
            }
            watchAioListVbHookInstalled = true
            module.logHook(Log.INFO, "AIO WatchAIOListVB hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "AIO WatchAIOListVB hook skipped", it)
        }
    }

    private fun hookNickNameAbility(module: ModuleMainKt, state: AioHookState) {
        if (nickNameAbilityHookInstalled) return
        val method = state.nickNameAbilityInjectMethod ?: return

        runCatching {
            module.intercept(method) {
                val rows = (args.getOrNull(0) as? Iterable<*>)?.toList()
                AioRuntimeStore.rememberNickNameAbility(
                    thisObject,
                    state.nickNameAbilityMemberInfoCacheField
                )
                val result = proceed()
                AioRuntimeStore.rememberNickNameAbility(
                    thisObject,
                    state.nickNameAbilityMemberInfoCacheField
                )
                val binding = AioRuntimeStore.latestBinding?.get()
                if (binding != null && !rows.isNullOrEmpty()) {
                    binding.upsertMessagesFromHost(rows)
                }
                result
            }
            nickNameAbilityHookInstalled = true
            module.logHook(Log.INFO, "AIO NickNameAbility hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "AIO NickNameAbility hook skipped", it)
        }
    }

    private fun scheduleListUiSync(
        module: ModuleMainKt,
        state: AioHookState,
        operation: Any?
    ) {
        if (operation == null) return
        AioRuntimeStore.mainHandler.post {
            syncListUiMessages(module, state, operation)
        }
        AioRuntimeStore.mainHandler.postDelayed({
            syncListUiMessages(module, state, operation)
        }, LIST_UI_SYNC_DELAY_MS)
    }

    private fun syncListUiMessages(
        module: ModuleMainKt,
        state: AioHookState,
        operation: Any?
    ) {
        runCatching {
            val rows = state.readListUiMessages(operation)?.toList() ?: return@runCatching
            AioRuntimeStore.latestListUiRows = rows
            val listView = state.readListUiView(operation)
            val binding = listView
                ?.let(AioRuntimeStore::findBindingForView)
                ?: AioRuntimeStore.latestBinding?.get()
                ?: return@runCatching
            binding.attachHostListView(listView)
            if (binding.currentState.messages.isEmpty()) {
                binding.replaceMessagesFromHost(rows)
            } else {
                binding.upsertMessagesFromHost(rows)
            }
        }.onFailure {
            module.logHook(Log.WARN, "AIO compose list UI sync skipped", it)
        }
    }

    private fun resolveBindingForRepo(state: AioHookState, repo: Any?): AioBinding? {
        if (!state.isWatchMsgListRepo(repo)) return null
        AioRuntimeStore.rememberMsgRepo(repo)
        val binding = AioRuntimeStore.latestBinding?.get() ?: return null
        binding.attachHostMsgRepo(repo)
        return binding
    }

    private fun ViewGroup.bindComposeAio(
        module: ModuleMainKt,
        state: AioHookState,
        hostFragment: Any?,
        viewPager: View?,
        peer: me.padi.qqlite.revived.shared.model.aio.AioPeer
    ) {
        clipChildren = false
        clipToPadding = false
        AioRuntimeStore.markAioSurfaceActive()
        viewPager?.keepInvisibleDataHost()
        hideAioHostChrome(viewPager)
        keepAioHostTransparent()

        val initialState = AioRuntimeStore.snapshotsByPeer[peer.stableKey()] ?: AioUiState(
            peer = peer,
            loading = true
        )
        val existingBinding = (getTag(AIO_STATE_BINDING_KEY) as? AioBinding)
            ?.takeIf { it.currentState.peer.stableKey() == peer.stableKey() }
        val binding = (existingBinding ?: AioBinding(
            module = module,
            hookState = state,
            root = this,
            viewPager = viewPager,
            hostFragment = hostFragment,
            peer = peer,
            initialState = initialState
        )).also {
            it.updateHost(hostFragment, viewPager, peer)
            setTag(AIO_STATE_BINDING_KEY, it)
        }
        AioRuntimeStore.registerBinding(this, hostFragment, binding)
        AioRuntimeStore.latestMsgRepo?.get()
            ?.takeIf { state.isWatchMsgListRepo(it) }
            ?.let(binding::attachHostMsgRepo)
        if (binding.currentState.messages.isEmpty()) {
            AioRuntimeStore.latestListUiRows?.let(binding::replaceMessagesFromHost)
        }
        AioRuntimeStore.drainPendingMessages(hostFragment).forEach(binding::enqueueMessage)
        binding.flushMessagesNow()

        AioRuntimeStore.mainHandler.postDelayed({
            binding.markLoaded()
        }, 1200L)

        val mountRoot = this

        if (existingBinding != null && mountRoot.findViewWithTag<View>(AIO_COMPOSE_TAG) != null) {
            mountRoot.keepComposeAioOverlayOnTop()
            return
        }

        mountRoot.addHostComposeView(
            tag = AIO_COMPOSE_TAG,
            bindingKey = AIO_COMPOSE_BINDING_KEY,
            layoutParamsFactory = { createFullComposeLayoutParams() },
            lifecycleAnchor = this,
            useWindowLayer = true,
            wrapInContainer = false,
            configure = {
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                alpha = 1f
                elevation = 128f
                translationZ = 128f
            },
            onHostViewReady = AioRuntimeStore::rememberComposeView,
            onHostViewDestroyed = {
                AioRuntimeStore.forgetComposeView(it)
                AioRuntimeStore.markAioSurfaceInactive()
            },
            onOwnerDestroyed = {
                binding.snapshot()
            },
            onWindowBackPressed = {
                binding.navigateBack()
            },
            onWindowDismissed = {
                binding.navigateBack()
            }
        ) {
            AioChatScreen(binding)
        }
        mountRoot.keepComposeAioOverlayOnTop()
        mountRoot.post { mountRoot.keepComposeAioOverlayOnTop() }
        mountRoot.postDelayed({
            mountRoot.keepComposeAioOverlayOnTop()
        }, 300L)
    }

    private fun ViewGroup.keepComposeAioOverlayOnTop() {
        findViewWithTag<View>(AIO_COMPOSE_TAG)?.let { composeView ->
            AioRuntimeStore.rememberComposeView(composeView)
            AioRuntimeStore.markAioSurfaceActive()
            composeView.visibility = View.VISIBLE
            composeView.isEnabled = true
            composeView.isClickable = false
            composeView.isFocusable = false
            composeView.isFocusableInTouchMode = false
            composeView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            composeView.alpha = 1f
            composeView.elevation = 128f
            composeView.translationZ = 128f
            composeView.bringToFront()
            invalidate()
        }
    }

    private fun ViewGroup.keepAioHostTransparent() {
        setBackgroundColor(Color.TRANSPARENT)
        alpha = 1f
        isClickable = false
        isLongClickable = false
        isFocusable = false
        isFocusableInTouchMode = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        requestDisallowInterceptTouchEvent(false)
    }

    private fun ViewGroup.hideAioHostChrome(viewPager: View?) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child === viewPager) {
                child.keepInvisibleDataHost()
            } else if (child.tag != AIO_COMPOSE_TAG) {
                child.visibility = View.INVISIBLE
                child.alpha = 0f
                child.isClickable = false
                child.isLongClickable = false
                child.isFocusable = false
                child.isFocusableInTouchMode = false
                child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                (child as? ViewGroup)?.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            }
        }
    }

    private const val LIST_UI_SYNC_DELAY_MS = 96L
}

private fun View.findActivityContentRoot(): ViewGroup? {
    return rootView?.findViewById(android.R.id.content)
}
