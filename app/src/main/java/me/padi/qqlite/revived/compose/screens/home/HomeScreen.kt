package me.padi.qqlite.revived.compose.screens.home

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import me.padi.qqlite.revived.compose.screens.home.pages.ContactsPage
import me.padi.qqlite.revived.compose.screens.home.pages.EmptyHomePage
import me.padi.qqlite.revived.compose.screens.home.pages.HomeLoadingPage
import me.padi.qqlite.revived.compose.screens.home.pages.ProfileActionsPage
import me.padi.qqlite.revived.compose.screens.home.pages.QZoneFeedPage
import me.padi.qqlite.revived.compose.screens.home.pages.RecentMessagesPage
import me.padi.qqlite.revived.shared.model.home.AvatarSpec
import me.padi.qqlite.revived.shared.model.home.HOME_RAIL_WIDTH_DP
import me.padi.qqlite.revived.shared.model.home.HomePage
import me.padi.qqlite.revived.shared.model.home.HomeProfile
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import me.padi.qqlite.revived.shared.model.home.HomeWindowInfo
import me.padi.qqlite.revived.shared.model.home.ScrollSnapshot
import me.padi.qqlite.revived.shared.model.home.coerceInHome
import me.padi.qqlite.revived.shared.model.home.normalizedImageValue
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.io.File
import android.graphics.Color as AndroidColor

@Composable
internal fun HomeScreen(controller: HomeUiController) {
    val uiState by controller.uiState.collectAsState()
    val pages = uiState.pages
    val currentIndex = uiState.currentIndex.coerceInHome(pages)
    val profile = uiState.profile

    MiuixTheme(
        controller = remember { ThemeController(colorSchemeMode = ColorSchemeMode.System) }) {
        ApplyHomeEdgeToEdge()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.secondaryVariant)
        ) {
            val windowInfo = remember(maxWidth, maxHeight) {
                HomeWindowInfo.create(maxWidth, maxHeight)
            }
            Scaffold(topBar = {
                HomeHeader(
                    controller = controller,
                    profile = profile,
                    currentPage = pages.getOrNull(currentIndex),
                    windowInfo = windowInfo
                )
            }, bottomBar = {
                if (!windowInfo.useNavigationRail) {
                    HomeBottomBar(
                        pages = pages,
                        currentIndex = currentIndex,
                        onSelect = controller::selectPage
                    )
                }
            }) { padding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.secondaryVariant)
                        .padding(padding)
                ) {
                    if (windowInfo.useNavigationRail) {
                        HomeRail(
                            pages = pages,
                            currentIndex = currentIndex,
                            onSelect = controller::selectPage
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = windowInfo.contentPadding)
                    ) {
                        HomeContent(
                            controller, uiState, pages.getOrNull(currentIndex), windowInfo
                        )
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun ApplyHomeEdgeToEdge() {
    val view = LocalView.current
    val backgroundColor = MiuixTheme.colorScheme.secondaryVariant
    val useLightSystemBars = backgroundColor.luminance() > 0.5f
    DisposableEffect(view, backgroundColor, useLightSystemBars) {
        val activity = view.context.findActivity()
        val window = activity?.window
        val oldStatusBarColor = window?.statusBarColor
        val oldNavigationBarColor = window?.navigationBarColor
        val oldSystemUiVisibility = window?.decorView?.systemUiVisibility
        val oldNavigationContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isNavigationBarContrastEnforced
        } else {
            null
        }
        val oldStatusContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isStatusBarContrastEnforced
        } else {
            null
        }

        if (window != null) {
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.setSystemBarsAppearance(
                    if (useLightSystemBars) {
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    } else {
                        0
                    },
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
            val lightFlags = if (useLightSystemBars) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                0
            }
            window.decorView.systemUiVisibility =
                (window.decorView.systemUiVisibility and (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR).inv()) or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or lightFlags
        }

        onDispose {
            if (window != null) {
                if (oldStatusBarColor != null) window.statusBarColor = oldStatusBarColor
                if (oldNavigationBarColor != null) window.navigationBarColor = oldNavigationBarColor
                if (oldSystemUiVisibility != null) {
                    window.decorView.systemUiVisibility = oldSystemUiVisibility
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (oldNavigationContrast != null) {
                        window.isNavigationBarContrastEnforced = oldNavigationContrast
                    }
                    if (oldStatusContrast != null) {
                        window.isStatusBarContrastEnforced = oldStatusContrast
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun HomeContent(
    controller: HomeUiController,
    uiState: HomeUiState,
    currentPage: HomePage?,
    windowInfo: HomeWindowInfo
) {
    if (currentPage == null) {
        HomeLoadingPage("正在加载首页")
        return
    }
    if (uiState.isPageDataLoading(currentPage)) {
        HomeLoadingPage("正在加载${currentPage.title}")
        return
    }
    when (currentPage.title) {
        "消息" -> RecentMessagesPage(controller, uiState)
        "联系人" -> ContactsPage(controller, uiState)
        "动态" -> QZoneFeedPage(controller, uiState)
        "我的" -> ProfileActionsPage(controller, uiState)
        else -> EmptyHomePage("暂无页面")
    }
}

private fun HomeUiState.isPageDataLoading(page: HomePage): Boolean {
    return when (page.title) {
        "消息" -> recentRows.isEmpty()
        "联系人" -> contacts.isEmpty()
        "动态" -> qzoneFeeds.isEmpty()
        "我的" -> visibleSelfActions.isEmpty()
        else -> false
    }
}

@Composable
private fun HomeHeader(
    controller: HomeUiController,
    profile: HomeProfile,
    currentPage: HomePage?,
    windowInfo: HomeWindowInfo
) {
    val pageTitle = currentPage?.title ?: "首页"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = windowInfo.horizontalPadding)
        ) {
            HostAppIcon(
                modifier = Modifier.align(Alignment.CenterStart)
            )
            AnimatedContent(
                targetState = pageTitle,
                transitionSpec = {
                    fadeIn(animationSpec = tween(160)) togetherWith fadeOut(
                        animationSpec = tween(
                            100
                        )
                    )
                },
                label = "homeHeaderTitle",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 58.dp)
            ) { title ->
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HomeProfileAvatar(
                controller = controller,
                profile = profile,
                modifier = Modifier.align(Alignment.CenterEnd),
                sizeDp = 42
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun HostAppIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        context.applicationInfo.loadIcon(context.packageManager)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .border(
                1.5.dp, MiuixTheme.colorScheme.primary.copy(alpha = 0.34f), RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AndroidView(
            factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        }, update = { view ->
            view.setImageDrawable(appIcon)
        }, modifier = Modifier.size(30.dp)
        )

        Text(
            text = "QQ Revived",
            style = MiuixTheme.textStyles.headline2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeProfileAvatar(
    controller: HomeUiController, profile: HomeProfile, modifier: Modifier = Modifier, sizeDp: Int
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.24f))
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val uid = profile.uid.orEmpty()
        if (uid.isNotBlank()) {
            HostAvatar(
                controller = controller, spec = AvatarSpec(
                    chatType = 1,
                    uid = uid,
                    uin = uid.toLongOrNull() ?: 0L,
                    fragmentRef = controller.hostFragmentRef
                ), fallback = profile.nickName?.take(1).orEmpty(), sizeDp = sizeDp
            )
        } else if (!profile.avatarPath.isNullOrBlank()) {
            AsyncImage(
                model = rememberImageRequest(profile.avatarPath, "home-profile"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(text = profile.nickName?.take(1)?.takeIf { it.isNotBlank() } ?: "Q",
                color = MiuixTheme.colorScheme.primary,
                fontSize = (sizeDp * 0.52f).sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun HostAvatar(
    controller: HomeUiController, spec: AvatarSpec?, fallback: String, sizeDp: Int = 44
) {
    val imageUrl = spec?.imageUrl?.normalizedImageValue()?.takeIf {
        it.startsWith("http://") || it.startsWith("https://") || it.startsWith("/") || it.startsWith(
            "file://"
        ) || it.startsWith("content://")
    }
    if (imageUrl != null) {
        UrlAvatar(imageUrl = imageUrl, fallback = fallback, sizeDp = sizeDp)
    } else if (spec != null && (spec.uid.isNotBlank() || spec.uin > 0L)) {
        AndroidView(
            factory = { context ->
            controller.createAvatarView(context, spec)
        }, update = { view ->
            (view as? ImageView)?.scaleType = ImageView.ScaleType.CENTER_CROP
            controller.reloadAvatar(view, spec)
        }, modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallback.ifBlank { "Q" },
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UrlAvatar(imageUrl: String, fallback: String, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = rememberImageRequest(imageUrl, "avatar"),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        ) {
            val imageState by painter.state.collectAsState()
            if (imageState is AsyncImagePainter.State.Success) {
                SubcomposeAsyncImageContent()
            } else if (imageState is AsyncImagePainter.State.Loading) {
                ImageLoadingIndicator()
            } else {
                Text(
                    text = fallback.ifBlank { "Q" },
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun ImageLoadingIndicator() {
    InfiniteProgressIndicator(
        modifier = Modifier.size(20.dp), color = MiuixTheme.colorScheme.primary
    )
}


@Composable
private fun HomeRail(
    pages: List<HomePage>, currentIndex: Int, onSelect: (Int) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .width(HOME_RAIL_WIDTH_DP.dp)
            .fillMaxHeight()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        pages.forEach { page ->
            val selected = page.index == currentIndex
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(page.index) },
                icon = page.icon(),
                label = page.title
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    pages: List<HomePage>, currentIndex: Int, onSelect: (Int) -> Unit
) {
    NavigationBar {
        pages.forEach { page ->
            NavigationBarItem(
                selected = page.index == currentIndex,
                onClick = { onSelect(page.index) },
                icon = page.icon(),
                label = page.title
            )
        }
    }
}

private fun HomePage.icon(): ImageVector {
    return when (title) {
        "消息" -> Icons.AutoMirrored.Filled.Message
        "联系人" -> Icons.Filled.Contacts
        "动态" -> Icons.Filled.DynamicFeed
        "我的" -> Icons.Filled.AccountCircle
        else -> Icons.Filled.Widgets
    }
}

private fun imageModel(value: String?): Any? {
    val normalized = value?.normalizedImageValue() ?: return null
    return if (normalized.startsWith("/") && File(normalized).exists()) {
        File(normalized)
    } else {
        normalized
    }
}

@Composable
internal fun rememberImageRequest(
    value: String?, cacheKeyPrefix: String, requestVersion: Int = 0
): ImageRequest? {
    val context = LocalContext.current
    val model = remember(value) { imageModel(value) } ?: return null
    val cacheKey = remember(model, cacheKeyPrefix) { "$cacheKeyPrefix:$model" }
    return remember(context, model, cacheKey, requestVersion) {
        ImageRequest.Builder(context).data(model).memoryCacheKey(cacheKey).diskCacheKey(cacheKey)
            .build()
    }
}

@Composable
internal fun rememberHomeListState(snapshot: ScrollSnapshot): LazyListState {
    return rememberLazyListState(
        initialFirstVisibleItemIndex = snapshot.index.coerceAtLeast(0),
        initialFirstVisibleItemScrollOffset = snapshot.offset.coerceAtLeast(0)
    )
}

@Composable
internal fun PersistListScroll(
    listState: LazyListState, onChanged: (ScrollSnapshot) -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow {
            ScrollSnapshot(
                listState.firstVisibleItemIndex, (listState.firstVisibleItemScrollOffset / 24) * 24
            )
        }.distinctUntilChanged().collect { snapshot ->
            onChanged(snapshot)
        }
    }
}


@Composable
internal fun HomeListRow(
    controller: HomeUiController,
    title: String,
    subtitle: String,
    meta: String,
    badge: String?,
    leading: String,
    avatar: AvatarSpec?,
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
        onLongPress = onLongClick,
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
            contentColor = MiuixTheme.colorScheme.onSurface
        ),

        ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            HostAvatar(controller, avatar, leading)
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title.ifBlank { "未命名" },
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Text(
                    text = subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = meta,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.sp
                )
                if (badge != null) {
                    MiuixBadgePill(text = badge, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
internal fun MiuixBadgePill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.error.copy(alpha = 0.92f))
            .padding(horizontal = 7.dp, vertical = 2.dp), contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onPrimary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

