package me.padi.qqlite.revived.compose.screens.home.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.padi.qqlite.revived.compose.screens.home.HomeListRow
import me.padi.qqlite.revived.compose.screens.home.HomeUiController
import me.padi.qqlite.revived.compose.screens.home.PersistListScroll
import me.padi.qqlite.revived.compose.screens.home.rememberHomeListState
import me.padi.qqlite.revived.shared.model.home.ContactRow
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun ContactsPage(
    controller: HomeUiController, uiState: HomeUiState
) {
    val rows = uiState.contacts
    if (rows.isEmpty()) {
        EmptyHomePage("等待联系人数据")
        return
    }
    val shortcutRows = rows.filter { it.isContactShortcut() }.sortedBy { it.shortcutOrder() }
    val contactRows = rows.filterNot { it.isContactShortcut() }
    var searchText by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    val query = searchText.trim()
    val filteredContactRows = remember(contactRows, query) {
        if (query.isBlank()) {
            contactRows
        } else {
            contactRows.filter { row ->
                row.title.contains(query, ignoreCase = true) || row.type.contains(
                    query, ignoreCase = true
                )
            }
        }
    }
    val listState = rememberHomeListState(uiState.contactScroll)
    PersistListScroll(listState, controller::updateContactScroll)
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        ContactSearchBar(
            query = searchText,
            onQueryChange = { searchText = it },
            expanded = searchExpanded,
            onExpandedChange = { searchExpanded = it },
            resultCount = filteredContactRows.size
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical(),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "contacts-top-spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (query.isBlank() && shortcutRows.isNotEmpty()) {
                item(key = "contact-function-title") {
                    SmallTitle(text = "功能")
                }
                item(key = "contact-shortcuts") {
                    ContactShortcutRow(controller, shortcutRows)
                }
            }
            if (query.isNotBlank() && filteredContactRows.isEmpty()) {
                item(key = "contacts-empty-search") {
                    EmptyContactSearch(query)
                }
            }

            if (filteredContactRows.isNotEmpty()) {
                item(key = "contact-list-title") {
                    ContactListHeader(
                        title = if (query.isBlank()) "好友列表" else "搜索结果",
                        count = filteredContactRows.size
                    )
                }
            }

            items(filteredContactRows, key = { it.key }) { row ->
                HomeListRow(
                    controller = controller,
                    title = row.title,
                    subtitle = row.type,
                    meta = "",
                    badge = row.unread.takeIf { it > 0 }?.toString(),
                    leading = row.title.take(1),
                    avatar = row.avatar,
                    icon = when (row.type) {
                        "群聊" -> Icons.Filled.Groups
                        "好友" -> Icons.Filled.Person
                        "通知" -> Icons.Filled.Campaign
                        else -> Icons.Filled.Badge
                    },
                    onClick = { controller.clickContact(row) },
                    onLongClick = { controller.clickContactExt(row) })
            }
            item(key = "contacts-bottom-spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ContactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    resultCount: Int
) {
    val inputModifier = if (expanded) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            detectTapGestures {
                onExpandedChange(true)
            }
        }
    }
    SearchBar(
        inputField = {
            Column{
                InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { onExpandedChange(false) },
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    modifier = inputModifier,
                    label = "搜索联系人",
                    enabled = expanded
                )
                Spacer(Modifier.height(4.dp))
            }
        }, expanded = expanded, onExpandedChange = onExpandedChange
    ) {

    }
}

private fun ContactRow.isContactShortcut(): Boolean {
    return title == "加好友/群聊" || title == "我的通知" || type == "添加" || type == "通知"
}

private fun ContactRow.shortcutOrder(): Int {
    return when {
        title == "加好友/群聊" || type == "添加" -> 0
        title == "我的通知" || type == "通知" -> 1
        else -> 2
    }
}

@Composable
private fun ContactListHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SmallTitle(text = title)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = count.toString(),
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyContactSearch(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MiuixTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = "没有找到相关联系人",
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = "未匹配到“$query”",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ContactShortcutRow(
    controller: HomeUiController, rows: List<ContactRow>
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.take(2).forEach { row ->
            val visual = contactShortcutVisual(row)
            ContactShortcutCard(
                title = row.title,
                subtitle = visual.subtitle,
                icon = visual.icon,
                iconColor = visual.iconColor,
                modifier = Modifier.weight(1f),
                onClick = { controller.clickContact(row) },
                onLongClick = { controller.clickContactExt(row) })
        }
    }
}

@Composable
private fun ContactShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = modifier.height(116.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
        onLongPress = onLongClick,
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
            contentColor = MiuixTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

private data class ContactShortcutVisual(
    val icon: ImageVector, val iconColor: Color, val subtitle: String
)

@Composable
private fun contactShortcutVisual(row: ContactRow): ContactShortcutVisual {
    val colors = MiuixTheme.colorScheme
    return when {
        row.title == "加好友/群聊" || row.type == "添加" -> ContactShortcutVisual(
            Icons.Filled.PersonAdd, colors.primary, "添加好友或群聊"
        )

        row.title == "我的通知" || row.type == "通知" -> ContactShortcutVisual(
            Icons.Filled.Campaign, colors.onSurfaceVariantSummary, "查看联系人通知"
        )

        else -> ContactShortcutVisual(Icons.Filled.Badge, colors.outline, row.type)
    }
}
