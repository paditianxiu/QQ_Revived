package me.padi.qqlite.revived.hooks.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.padi.qqlite.revived.ModuleMainKt
import me.padi.qqlite.revived.compose.screens.home.HomeScreen
import me.padi.qqlite.revived.hooks.common.BaseHook
import me.padi.qqlite.revived.shared.model.home.coerceInHome
import me.padi.qqlite.revived.shared.viewmodel.home.HomeStateStore
import me.padi.qqlite.revived.utils.addHostComposeView
import java.lang.ref.WeakReference

internal object HomeComposeHook : BaseHook() {
    private var mainFragmentHookInstalled = false
    private var dataHookInstalled = false
    private var selfProfileHookInstalled = false
    private var hookState: HomeHookState? = null

    override fun reset() {
        mainFragmentHookInstalled = false
        dataHookInstalled = false
        selfProfileHookInstalled = false
        HomeRuntimeStore.reset()
        hookState = null
    }

    override fun install(module: ModuleMainKt, classLoader: ClassLoader?) {
        val targetClassLoader = requireClassLoader(classLoader)
        val state = hookState ?: HomeHookState.create(targetClassLoader).also { hookState = it }
        hookMainFragment(module, state)
        hookDataSources(module, state)
        hookSelfProfileCache(module, state)
    }

    private fun hookMainFragment(module: ModuleMainKt, state: HomeHookState) {
        if (mainFragmentHookInstalled) return

        runCatching {
            module.intercept(
                state.mainFragmentClass.getDeclaredMethod(
                    "a0", LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java
                )
            ) {
                val result = proceed()
                runCatching {
                    val root = result as? ViewGroup ?: return@runCatching
                    val viewPager = state.mainViewPagerField?.get(thisObject) as? View
                        ?: root.findFirstDescendantByClassName(VIEW_PAGER_CLASS)
                        ?: return@runCatching
                    root.bindComposeHome(module, state, viewPager, thisObject)
                }.onFailure {
                    module.logHook(Log.WARN, "Home compose bind skipped", it)
                }
                result
            }
            mainFragmentHookInstalled = true
            module.logHook(Log.INFO, "Home compose hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "Home compose hook skipped", it)
        }
    }

    private fun hookDataSources(module: ModuleMainKt, state: HomeHookState) {
        if (dataHookInstalled) return

        hookRecentItems(module, state)
        hookContactItems(module, state)
        hookQZoneFeeds(module, state)
        hookSelfPage(module, state)
        dataHookInstalled = true
    }

    private fun hookRecentItems(module: ModuleMainKt, state: HomeHookState) {
        runCatching {
            module.intercept(
                state.recentItemBuilderClass.getDeclaredMethod(
                    "m", state.baseChatHolderClass, state.recentItemClass, List::class.java
                )
            ) {
                val result = proceed()
                val item = args.getOrNull(1)
                if (item != null) {
                    HomeRuntimeStore.mainHandler.post {
                        HomeRuntimeStore.latestBinding?.get()?.let { binding ->
                            binding.upsertRecent(
                                state.toRecentRow(
                                    item, binding.hostFragmentRef.get()
                                )
                            )
                        }
                    }
                }
                result
            }

            module.intercept(
                state.listAdapterClass.getDeclaredMethod("submitList", List::class.java)
            ) {
                val result = proceed()
                val rows = (args.getOrNull(0) as? List<*>)?.takeIf { items ->
                    items.isNotEmpty() && items.all { candidate ->
                        candidate == null || state.recentItemClass.isInstance(candidate)
                    }
                }?.mapIndexedNotNull { index, item ->
                    item?.let { state.toRecentRow(it, HomeRuntimeStore.latestBinding?.get()?.hostFragmentRef?.get(), index) }
                }.orEmpty()
                if (rows.isNotEmpty()) {
                    HomeRuntimeStore.mainHandler.post {
                        HomeRuntimeStore.latestBinding?.get()?.updateRecentRows(rows)
                    }
                }
                result
            }
            module.logHook(Log.INFO, "Home recent item hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "Home recent item hook skipped", it)
        }
    }

    private fun hookContactItems(module: ModuleMainKt, state: HomeHookState) {
        runCatching {
            module.intercept(
                state.contactAdapterClass.getDeclaredMethod(
                    "onBindViewHolder", state.recyclerViewHolderClass, Integer.TYPE
                )
            ) {
                val result = proceed()
                val adapter = thisObject
                val item = (args.getOrNull(1) as? Int)?.let { adapter?.invokeGetItem(it) }
                if (adapter != null && item != null) {
                    val fragment = state.contactAdapterFragmentField?.get(adapter)
                    HomeRuntimeStore.mainHandler.post {
                        HomeRuntimeStore.latestBinding?.get()
                            ?.upsertContact(state.toContactRow(item, fragment))
                    }
                }
                result
            }

            module.intercept(
                state.listAdapterClass.getDeclaredMethod("submitList", List::class.java)
            ) {
                val result = proceed()
                if (state.contactAdapterClass.isInstance(thisObject)) {
                    val fragment = state.contactAdapterFragmentField?.get(thisObject)
                    val rows = (args.getOrNull(0) as? List<*>).orEmpty()
                        .mapNotNull { item -> item?.let { state.toContactRow(it, fragment) } }
                    HomeRuntimeStore.mainHandler.post {
                        HomeRuntimeStore.latestBinding?.get()?.updateContacts(rows)
                    }
                }
                result
            }
            module.logHook(Log.INFO, "Home contact item hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "Home contact item hook skipped", it)
        }
    }

    private fun hookQZoneFeeds(module: ModuleMainKt, state: HomeHookState) {
        runCatching {
            module.intercept(
                state.qZoneMainFrameClass.getDeclaredMethod(
                    "Q", List::class.java, java.lang.Boolean.TYPE
                )
            ) {
                val result = proceed()
                val fragment = thisObject
                val rows = (args.getOrNull(0) as? List<*>).orEmpty().filterNotNull()
                    .mapIndexed { index, feed -> state.toQZoneFeedRow(feed, fragment, index) }
                HomeRuntimeStore.mainHandler.post {
                    HomeRuntimeStore.latestBinding?.get()?.updateQZoneFeeds(rows)
                }
                result
            }
            module.intercept(
                state.qZoneMainFrameClass.getDeclaredMethod("o", java.lang.Boolean.TYPE)
            ) {
                val result = proceed()
                HomeRuntimeStore.mainHandler.post {
                    HomeRuntimeStore.latestBinding?.get()?.finishQZoneLoadMore()
                }
                result
            }
            module.logHook(Log.INFO, "Home QZone feed hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "Home QZone feed hook skipped", it)
        }
    }

    private fun hookSelfPage(module: ModuleMainKt, state: HomeHookState) {
        runCatching {
            module.intercept(
                state.selfFragmentClass.getDeclaredMethod(
                    "a0", LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java
                )
            ) {
                val result = proceed()
                val fragment = thisObject
                HomeRuntimeStore.mainHandler.post {
                    HomeRuntimeStore.latestBinding?.get()
                        ?.updateSelfActions(state.createSelfRows(fragment))
                }
                result
            }
            module.logHook(Log.INFO, "Home self page hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "Home self page hook skipped", it)
        }
    }

    private fun hookSelfProfileCache(module: ModuleMainKt, state: HomeHookState) {
        if (selfProfileHookInstalled) return

        runCatching {
            module.intercept(
                state.selfProfileServiceClass.getDeclaredMethod(
                    "saveSelfNameAndAvatarPath",
                    String::class.java,
                    String::class.java,
                    String::class.java
                )
            ) {
                val result = proceed()
                updateHomeProfile(
                    uid = args.getOrNull(0) as? String,
                    nickName = args.getOrNull(1) as? String,
                    avatarPath = args.getOrNull(2) as? String
                )
                result
            }
            selfProfileHookInstalled = true
            module.logHook(Log.INFO, "Home profile hook installed")
        }.onFailure {
            module.logHook(Log.WARN, "Home profile hook skipped", it)
        }
    }

    private fun ViewGroup.bindComposeHome(
        module: ModuleMainKt,
        state: HomeHookState,
        viewPager: View,
        hostFragment: Any?
    ) {
        hideOriginalMainViews(viewPager)
        val existingBinding = (getTag(HOME_STATE_BINDING_KEY) as? HomeBinding)?.takeIf {
            it.viewPagerRef.get() === viewPager
        }
        val binding = (existingBinding ?: HomeBinding(state, viewPager, this, hostFragment)).also {
            it.hostFragmentRef = WeakReference(hostFragment)
            setTag(HOME_STATE_BINDING_KEY, it)
        }

        binding.updatePages(readHomePages(viewPager))
        val pages = binding.currentState.pages
        binding.updateCurrentIndex(
            viewPager.readCurrentViewPagerItem()?.coerceInHome(pages)
                ?: binding.currentState.currentIndex.coerceInHome(pages)
        )
        binding.updateProfile(HomeStateStore.latestProfile)
        HomeRuntimeStore.bindingsByViewPager[viewPager] = WeakReference(binding)
        HomeRuntimeStore.latestBinding = WeakReference(binding)
        loadCachedHomeProfile(context)

        val mountRoot = findActivityContentRoot() ?: this
        if (mountRoot !== this) {
            findViewWithTag<View>(HOME_COMPOSE_TAG)?.let(::removeView)
        }

        if (existingBinding != null && mountRoot.findViewWithTag<View>(HOME_COMPOSE_TAG) != null) {
            return
        }

        mountRoot.addHostComposeView(
            tag = HOME_COMPOSE_TAG,
            bindingKey = HOME_COMPOSE_BINDING_KEY,
            layoutParamsFactory = { createFullComposeLayoutParams() },
            lifecycleAnchor = this,
            configure = {
                isClickable = true
                isFocusable = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                elevation = 64f
                translationZ = 64f
                alpha = 1f
            },
            onOwnerReady = { owner ->
                binding.attachViewModel(owner)
            },
            onOwnerDestroyed = {
                binding.detachViewModel()
            }
        ) {
            HomeScreen(binding)
        }
    }

    private fun ViewGroup.hideOriginalMainViews(viewPager: View) {
        clipChildren = false
        clipToPadding = false
        findFirstDescendantByClassName(CIRCLE_INDICATOR_CLASS)?.hideTree()
        viewPager.keepInvisibleDataHost()
    }

    private fun View.keepInvisibleDataHost() {
        visibility = View.INVISIBLE
        alpha = 0f
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        (this as? ViewGroup)?.apply {
            clipChildren = false
            clipToPadding = false
        }
    }

}

private fun View.findActivityContentRoot(): ViewGroup? {
    return rootView?.findViewById(android.R.id.content)
}
