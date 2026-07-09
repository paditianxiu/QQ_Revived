package me.padi.qqlite.revived.shared.viewmodel.home

import me.padi.qqlite.revived.shared.model.home.HomeProfile
import me.padi.qqlite.revived.shared.model.home.HomeUiState

object HomeStateStore {
    var latestProfile = HomeProfile()
    var latestSnapshot = HomeUiState()

    fun reset() {
        latestProfile = HomeProfile()
        latestSnapshot = HomeUiState()
    }
}
