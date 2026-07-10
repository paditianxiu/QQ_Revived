package me.padi.qqlite.revived.shared.viewmodel.home

import androidx.lifecycle.ViewModel
import me.padi.qqlite.revived.shared.model.home.ContactRow
import me.padi.qqlite.revived.shared.model.home.HOME_QZONE_LOAD_MORE_INTERVAL_MS
import me.padi.qqlite.revived.shared.model.home.HOME_QZONE_LOAD_MORE_TIMEOUT_MS
import me.padi.qqlite.revived.shared.model.home.HomePage
import me.padi.qqlite.revived.shared.model.home.HomeProfile
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import me.padi.qqlite.revived.shared.model.home.QZoneFeedRow
import me.padi.qqlite.revived.shared.model.home.RecentRow
import me.padi.qqlite.revived.shared.model.home.ScrollSnapshot
import me.padi.qqlite.revived.shared.model.home.SelfActionRow
import me.padi.qqlite.revived.shared.model.home.coerceInHome
import me.padi.qqlite.revived.shared.model.home.sameContactUi
import me.padi.qqlite.revived.shared.model.home.sameUi
import me.padi.qqlite.revived.shared.model.home.sortedForMessageHome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(initialState: HomeUiState) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<HomeUiState> = _uiState
    val currentState: HomeUiState
        get() = _uiState.value

    private var qzoneLoadMoreRunning = false
    private var lastQzoneLoadMoreAt = 0L

    fun selectPage(index: Int): Int {
        val target = index.coerceInHome(_uiState.value.pages)
        updateState { state -> state.copy(currentIndex = target) }
        return target
    }

    fun upsertRecent(row: RecentRow) {
        val rows = _uiState.value.recentRows.toMutableList()
        val index = rows.indexOfFirst { it.key == row.key }
        if (index >= 0) {
            if (rows[index].sameUi(row)) return
            val existing = rows[index]
            rows[index] = row.copy(
                orderHint = minOf(row.orderHint, existing.orderHint)
            )
        } else {
            rows.add(row)
        }
        val sortedRows = rows.sortedForMessageHome()
        updateState { state -> state.copy(recentRows = sortedRows) }
    }

    fun updateRecentRows(rows: List<RecentRow>) {
        val sortedRows = rows.sortedForMessageHome()
        updateStateIfChanged(_uiState.value.recentRows != sortedRows) { state ->
            state.copy(recentRows = sortedRows)
        }
    }

    fun upsertContact(row: ContactRow) {
        val rows = _uiState.value.contacts.toMutableList()
        val index = rows.indexOfFirst { it.key == row.key }
        if (index >= 0) {
            if (rows[index].sameUi(row)) return
            rows[index] = row
        } else {
            rows.add(row)
        }
        updateState { state -> state.copy(contacts = rows) }
    }

    fun updateQZoneFeeds(rows: List<QZoneFeedRow>) {
        qzoneLoadMoreRunning = false
        updateState { state -> state.copy(qzoneFeeds = rows) }
    }

    fun updatePages(pages: List<HomePage>) {
        updateStateIfChanged(_uiState.value.pages != pages) { state ->
            state.copy(pages = pages)
        }
    }

    fun updateCurrentIndex(index: Int) {
        val target = index.coerceInHome(_uiState.value.pages)
        updateStateIfChanged(_uiState.value.currentIndex != target) { state ->
            state.copy(currentIndex = target)
        }
    }

    fun updateProfile(profile: HomeProfile) {
        updateStateIfChanged(_uiState.value.profile != profile) { state ->
            state.copy(profile = profile)
        }
    }

    fun updateContacts(rows: List<ContactRow>) {
        updateStateIfChanged(!_uiState.value.contacts.sameContactUi(rows)) { state ->
            state.copy(contacts = rows)
        }
    }

    fun updateSelfActions(rows: List<SelfActionRow>) {
        updateStateIfChanged(
            _uiState.value.selfActions.map { it.title } != rows.map { it.title }
        ) { state ->
            state.copy(selfActions = rows)
        }
    }

    fun updateRecentScroll(snapshot: ScrollSnapshot) {
        updateStateIfChanged(_uiState.value.recentScroll != snapshot) { state ->
            state.copy(recentScroll = snapshot)
        }
    }

    fun updateContactScroll(snapshot: ScrollSnapshot) {
        updateStateIfChanged(_uiState.value.contactScroll != snapshot) { state ->
            state.copy(contactScroll = snapshot)
        }
    }

    fun updateQZoneScroll(snapshot: ScrollSnapshot) {
        updateStateIfChanged(_uiState.value.qzoneScroll != snapshot) { state ->
            state.copy(qzoneScroll = snapshot)
        }
    }

    fun updateSelfScroll(snapshot: ScrollSnapshot) {
        updateStateIfChanged(_uiState.value.selfScroll != snapshot) { state ->
            state.copy(selfScroll = snapshot)
        }
    }

    fun markQZoneLoadMoreStarted(now: Long): Boolean {
        if (qzoneLoadMoreRunning || now - lastQzoneLoadMoreAt < HOME_QZONE_LOAD_MORE_INTERVAL_MS) {
            return false
        }
        qzoneLoadMoreRunning = true
        lastQzoneLoadMoreAt = now
        return true
    }

    fun finishQZoneLoadMore() {
        qzoneLoadMoreRunning = false
    }

    fun resetQZoneLoadMoreIfTimedOut(now: Long) {
        if (now - lastQzoneLoadMoreAt >= HOME_QZONE_LOAD_MORE_TIMEOUT_MS) {
            qzoneLoadMoreRunning = false
        }
    }

    private fun updateStateIfChanged(
        changed: Boolean,
        transform: (HomeUiState) -> HomeUiState
    ) {
        if (!changed) return
        updateState(transform)
    }

    private fun updateState(transform: (HomeUiState) -> HomeUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next
        HomeStateStore.latestSnapshot = next
    }
}
