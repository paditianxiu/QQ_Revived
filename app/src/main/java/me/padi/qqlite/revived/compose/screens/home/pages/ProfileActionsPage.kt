package me.padi.qqlite.revived.compose.screens.home.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.padi.qqlite.revived.compose.screens.home.*
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun ProfileActionsPage(
    controller: HomeUiController,
    uiState: HomeUiState
) {
    val actions = uiState.visibleSelfActions
    if (actions.isEmpty()) {
        EmptyHomePage("等待个人数据")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(actions, key = { it.title }) { row ->
            val visual = profileActionVisual(row.title)

            GridItem(
                title = row.title,
                subtitle = visual.subtitle,
                icon = visual.icon,
                iconColor = visual.iconColor,
                onClick = {
                    controller.clickSelfAction(row, null)
                },
                onLongClick = {
                    controller.clickSelfAction(row, null)
                }
            )
        }
    }
}

@Composable
private fun GridItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
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
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.1f), shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

private data class ProfileActionVisual(
    val icon: ImageVector,
    val iconColor: Color,
    val subtitle: String
)

@Composable
private fun profileActionVisual(title: String): ProfileActionVisual {
    val colors = MiuixTheme.colorScheme
    return when (title) {
        "我" -> ProfileActionVisual(Icons.Filled.AccountCircle, colors.primary, "查看个人信息")
        "修改资料" -> ProfileActionVisual(Icons.Filled.Edit, colors.error, "修改账号资料")
        "绑定监护人" -> ProfileActionVisual(Icons.Filled.PersonAdd, colors.primary, "管理监护关系")
        "注销账号" -> ProfileActionVisual(Icons.Filled.Delete, colors.error, "永久删除账号")
        "退出账号" -> ProfileActionVisual(
            Icons.AutoMirrored.Filled.Logout,
            colors.onSurfaceVariantSummary,
            "退出当前登录"
        )

        else -> ProfileActionVisual(Icons.Filled.Settings, colors.outline, "")
    }
}


