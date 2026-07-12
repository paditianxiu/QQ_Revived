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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.padi.qqlite.revived.compose.component.card.MiuixFeatureCard
import me.padi.qqlite.revived.compose.screens.home.*
import me.padi.qqlite.revived.compose.screens.settings.ModuleThemeSettingsSheet
import me.padi.qqlite.revived.compose.theme.RevivedThemeState
import me.padi.qqlite.revived.shared.model.home.HomeWindowInfo
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun ProfileActionsPage(
    controller: HomeUiController,
    uiState: HomeUiState,
    windowInfo: HomeWindowInfo,
) {
    val context = LocalContext.current
    val actions = uiState.visibleSelfActions
    var showModuleSettings by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(windowInfo.profileGridColumns),
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(if (windowInfo.useNavigationRail) 20.dp else 16.dp)
    ) {
        item(key = MODULE_SETTINGS_TITLE) {
            GridItem(
                title = MODULE_SETTINGS_TITLE,
                subtitle = "打开主题与模块配置",
                icon = Icons.Filled.Settings,
                iconColor = MiuixTheme.colorScheme.primary,
                onClick = {
                    showModuleSettings = true
                },
                onLongClick = {
                    showModuleSettings = true
                }
            )
        }

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

    ModuleThemeSettingsSheet(
        show = showModuleSettings,
        themePreference = RevivedThemeState.preference,
        onDismissRequest = { showModuleSettings = false },
        onUiModeChange = { uiMode ->
            RevivedThemeState.updateUiMode(context, uiMode)
        },
        onThemeModeChange = { mode ->
            RevivedThemeState.updateMode(context, mode)
        },
        onThemePresetChange = { preset ->
            RevivedThemeState.updatePreset(context, preset)
        },
        onLiquidGlassChange = { enabled ->
            RevivedThemeState.updateLiquidGlassEnabled(context, enabled)
        }
    )
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
    MiuixFeatureCard(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconColor = iconColor,
        modifier = Modifier.fillMaxWidth(),
        minHeight = 150,
        onClick = onClick,
        onLongClick = onLongClick
    )
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
            "退出账号" -> ProfileActionVisual(
            Icons.AutoMirrored.Filled.Logout,
            colors.onSurfaceVariantSummary,
            "退出当前登录"
        )

        else -> ProfileActionVisual(Icons.Filled.Settings, colors.outline, "")
    }
}

private const val MODULE_SETTINGS_TITLE = "模块设置"


