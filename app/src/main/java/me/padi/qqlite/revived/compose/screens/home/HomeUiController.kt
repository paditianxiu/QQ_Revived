package me.padi.qqlite.revived.compose.screens.home

import android.content.Context
import android.view.View
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import me.padi.qqlite.revived.shared.model.home.ContactRow
import me.padi.qqlite.revived.shared.model.home.QZoneFeedRow
import me.padi.qqlite.revived.shared.model.home.RecentRow
import me.padi.qqlite.revived.shared.model.home.ScrollSnapshot
import me.padi.qqlite.revived.shared.model.home.SelfActionRow
import kotlinx.coroutines.flow.StateFlow
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import java.lang.ref.WeakReference

interface HomeUiController {
    val uiState: StateFlow<HomeUiState>
    val hostFragmentRef: WeakReference<Any?>

    fun selectPage(index: Int)
    fun clickRecent(row: RecentRow)
    fun longClickRecent(row: RecentRow)
    fun clickContact(row: ContactRow)
    fun clickContactExt(row: ContactRow)
    fun clickQZone(row: QZoneFeedRow, fakeView: View?)
    fun clickSelfAction(row: SelfActionRow, fakeView: View?)
    fun updateRecentScroll(snapshot: ScrollSnapshot)
    fun updateContactScroll(snapshot: ScrollSnapshot)
    fun updateQZoneScroll(snapshot: ScrollSnapshot)
    fun updateSelfScroll(snapshot: ScrollSnapshot)
    fun requestQZoneLoadMore()
    fun createAvatarView(context: Context, spec: AvatarSpec?): View
    fun reloadAvatar(view: View, spec: AvatarSpec?)
}
