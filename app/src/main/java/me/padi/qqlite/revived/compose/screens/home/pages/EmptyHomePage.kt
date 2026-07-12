package me.padi.qqlite.revived.compose.screens.home.pages

import androidx.compose.runtime.Composable
import me.padi.qqlite.revived.compose.component.state.MiuixEmptyState
import me.padi.qqlite.revived.compose.component.state.MiuixLoadingState

@Composable
internal fun EmptyHomePage(text: String) {
    MiuixEmptyState(text = text)
}

@Composable
internal fun HomeLoadingPage(text: String) {
    MiuixLoadingState(text = text)
}

