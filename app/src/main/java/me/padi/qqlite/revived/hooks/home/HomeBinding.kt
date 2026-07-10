package me.padi.qqlite.revived.hooks.home

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import me.padi.qqlite.revived.compose.screens.home.HomeUiController
import me.padi.qqlite.revived.di.RevivedKoin
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import me.padi.qqlite.revived.shared.model.home.ContactRow
import me.padi.qqlite.revived.shared.model.home.HOME_QZONE_LOAD_MORE_TIMEOUT_MS
import me.padi.qqlite.revived.shared.model.home.HomePage
import me.padi.qqlite.revived.shared.model.home.HomeProfile
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import me.padi.qqlite.revived.shared.model.home.QZoneFeedRow
import me.padi.qqlite.revived.shared.model.home.RecentRow
import me.padi.qqlite.revived.shared.model.home.ScrollSnapshot
import me.padi.qqlite.revived.shared.model.home.SelfActionRow
import me.padi.qqlite.revived.shared.viewmodel.home.HomeStateStore
import me.padi.qqlite.revived.shared.viewmodel.home.HomeViewModel
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

internal class HomeBinding(
    private val state: HomeHookState, viewPager: View, eventView: View, hostFragment: Any?
) : HomeUiController {
    val viewPagerRef = WeakReference(viewPager)
    override var hostFragmentRef = WeakReference(hostFragment)
    private val eventViewRef = WeakReference(eventView)
    private var viewModel = RevivedKoin.createHomeViewModel(HomeStateStore.latestSnapshot)
    override val uiState: StateFlow<HomeUiState>
        get() = viewModel.uiState
    val currentState: HomeUiState
        get() = viewModel.currentState

    fun attachViewModel(owner: ViewModelStoreOwner) {
        RevivedKoin.ensureStarted(eventViewRef.get()?.context)
        val nextViewModel = ViewModelProvider(
            owner,
            RevivedKoin.homeViewModelFactory(HomeStateStore.latestSnapshot)
        ).get(HomeViewModel::class.java)
        if (viewModel === nextViewModel) return
        viewModel = nextViewModel
        HomeStateStore.latestSnapshot = nextViewModel.currentState
    }

    fun detachViewModel() {
        HomeStateStore.latestSnapshot = viewModel.currentState
        viewModel = RevivedKoin.createHomeViewModel(HomeStateStore.latestSnapshot)
    }

    override fun selectPage(index: Int) {
        val target = viewModel.selectPage(index)
        viewPagerRef.get()?.setViewPagerCurrentItem(target, false)
    }

    fun upsertRecent(row: RecentRow) {
        viewModel.upsertRecent(row)
    }

    fun updateRecentRows(rows: List<RecentRow>) {
        viewModel.updateRecentRows(rows)
    }

    fun upsertContact(row: ContactRow) {
        viewModel.upsertContact(row)
    }

    fun updateQZoneFeeds(rows: List<QZoneFeedRow>) {
        viewModel.updateQZoneFeeds(rows)
    }

    fun updatePages(pages: List<HomePage>) {
        viewModel.updatePages(pages)
    }

    fun updateCurrentIndex(index: Int) {
        viewModel.updateCurrentIndex(index)
    }

    fun updateProfile(profile: HomeProfile) {
        viewModel.updateProfile(profile)
    }

    fun updateContacts(rows: List<ContactRow>) {
        viewModel.updateContacts(rows)
    }

    fun updateSelfActions(rows: List<SelfActionRow>) {
        viewModel.updateSelfActions(rows)
    }

    override fun clickRecent(row: RecentRow) {
        val clicked = runCatching {
            state.recentClickMethod?.invoke(state.recentClickListener, row.rawItem)
            true
        }.onFailure {
            Log.w(
                HOME_HOOK_TAG,
                "recent host click failed, fallback title=${row.title} type=${row.chatType} peer=${row.peerId}",
                it
            )
        }.getOrDefault(false)

        if (!clicked) {
            val context = eventViewRef.get()?.context ?: viewPagerRef.get()?.context
            state.openRecentFromMainActivity(context, row)
        }
    }

    override fun longClickRecent(row: RecentRow) {
        state.recentLongClickMethod?.invoke(state.recentClickListener, row.rawItem)
    }

    override fun clickContact(row: ContactRow) {
        state.sendContactIntent(
            row.fragmentRef.get(), state.contactUseClickConstructor, row.rawItem
        )
    }

    override fun clickContactExt(row: ContactRow) {
        state.sendContactIntent(
            row.fragmentRef.get(), state.contactExtClickConstructor, row.rawItem
        )
    }

    override fun clickQZone(row: QZoneFeedRow, fakeView: View?) {
        val fragment = row.fragmentRef.get() ?: return
        state.qZoneElementClickMethod?.invoke(
            fragment,
            fakeView ?: eventViewRef.get() ?: viewPagerRef.get(),
            1,
            row.rawFeed,
            row.index,
            null
        )
    }

    override fun clickSelfAction(row: SelfActionRow, fakeView: View?) {
        state.viewOnClickMethod?.invoke(
            row.rawAction, fakeView ?: eventViewRef.get() ?: viewPagerRef.get()
        )
    }

    override fun updateRecentScroll(snapshot: ScrollSnapshot) {
        viewModel.updateRecentScroll(snapshot)
    }

    override fun updateContactScroll(snapshot: ScrollSnapshot) {
        viewModel.updateContactScroll(snapshot)
    }

    override fun updateQZoneScroll(snapshot: ScrollSnapshot) {
        viewModel.updateQZoneScroll(snapshot)
    }

    override fun updateSelfScroll(snapshot: ScrollSnapshot) {
        viewModel.updateSelfScroll(snapshot)
    }

    override fun requestQZoneLoadMore() {
        val fragment =
            currentState.qzoneFeeds.lastOrNull()?.fragmentRef?.get() ?: hostFragmentRef.get()
            ?: return
        val now = SystemClock.uptimeMillis()
        if (!viewModel.markQZoneLoadMoreStarted(now)) return
        if (!state.requestQZoneLoadMore(fragment)) {
            viewModel.finishQZoneLoadMore()
            return
        }
        HomeRuntimeStore.mainHandler.postDelayed({
            viewModel.resetQZoneLoadMoreIfTimedOut(SystemClock.uptimeMillis())
        }, HOME_QZONE_LOAD_MORE_TIMEOUT_MS)
    }

    fun finishQZoneLoadMore() {
        viewModel.finishQZoneLoadMore()
    }

    override fun createAvatarView(context: Context, spec: AvatarSpec?): View {
        return state.createAvatarView(context, spec)
    }

    override fun reloadAvatar(view: View, spec: AvatarSpec?) {
        state.loadAvatar(view, spec)
    }
}

private const val HOME_HOOK_TAG = "QQRevived.Home"

