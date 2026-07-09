package me.padi.qqlite.revived.compose.screens.home.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.padi.qqlite.revived.compose.screens.home.*
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import me.padi.qqlite.revived.shared.model.home.RecentRow
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun RecentMessagesPage(
    controller: HomeUiController,
    uiState: HomeUiState
) {
    val rows = uiState.recentRows
    if (rows.isEmpty()) {
        EmptyHomePage("等待消息数据")
        return
    }
    val pinnedRows = rows.filter { it.pinned }
    val recentRows = rows.filterNot { it.pinned }
    var pinnedExpanded by remember { mutableStateOf(true) }
    val listState = rememberHomeListState(uiState.recentScroll)
    PersistListScroll(listState, controller::updateRecentScroll)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "recent-top-spacer") {
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (pinnedRows.isNotEmpty()) {
            item(key = "pinned-title") {
                PinnedMessagesHeader(
                    expanded = pinnedExpanded,
                    onToggle = { pinnedExpanded = !pinnedExpanded }
                )
            }
            item(key = "pinned-content") {
                AnimatedVisibility(
                    visible = pinnedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pinnedRows.forEach { row ->
                            RecentMessageRow(
                                controller = controller,
                                row = row,
                                icon = Icons.Filled.PushPin
                            )
                        }
                    }
                }
            }
        }
        if (recentRows.isNotEmpty()) {
            item(key = "recent-title") {
                SmallTitle(text = "最近消息")
            }
        }
        items(recentRows, key = { it.key }) { row ->
            RecentMessageRow(
                controller = controller,
                row = row,
                icon = Icons.AutoMirrored.Filled.Chat
            )
        }
    }
}

@Composable
private fun PinnedMessagesHeader(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SmallTitle(text = "置顶消息")
        }
        IconButton(
            onClick = onToggle,
            minWidth = 36.dp,
            minHeight = 36.dp,
            cornerRadius = 18.dp,
            backgroundColor = MiuixTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "折叠置顶消息" else "展开置顶消息",
                tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RecentMessageRow(
    controller: HomeUiController,
    row: RecentRow,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HomeListRow(
        controller = controller,
        title = row.title,
        subtitle = row.summary,
        meta = row.time,
        badge = row.unread.takeIf { it > 0 }?.toString(),
        leading = row.title.take(1),
        avatar = row.avatar,
        icon = icon,
        onClick = { controller.clickRecent(row) },
        onLongClick = { controller.longClickRecent(row) }
    )
}


