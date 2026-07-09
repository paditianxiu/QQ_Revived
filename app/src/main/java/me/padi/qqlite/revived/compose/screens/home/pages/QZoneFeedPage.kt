package me.padi.qqlite.revived.compose.screens.home.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import me.padi.qqlite.revived.compose.screens.home.*
import me.padi.qqlite.revived.shared.model.home.HomeUiState
import me.padi.qqlite.revived.shared.model.home.PictureSpec
import me.padi.qqlite.revived.shared.model.home.QZoneFeedRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private const val QZONE_SHEET_IMAGE_PAGE_SIZE = 18

@Composable
internal fun QZoneFeedPage(
    controller: HomeUiController,
    uiState: HomeUiState
) {
    val rows = uiState.qzoneFeeds
    if (rows.isEmpty()) {
        EmptyHomePage("等待动态数据")
        return
    }
    val listState = rememberHomeListState(uiState.qzoneScroll)
    PersistListScroll(listState, controller::updateQZoneScroll)
    LaunchedEffect(listState, rows.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }.distinctUntilChanged().collect { (lastVisible, totalItems) ->
            if (totalItems > 0 && lastVisible >= totalItems - 4) {
                controller.requestQZoneLoadMore()
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "qzone-top-spacer") {
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(rows, key = { it.key }) { row ->
            QZoneFeedCard(controller, row)
        }
        item(key = "qzone-bottom-spacer") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QZoneFeedCard(
    controller: HomeUiController,
    row: QZoneFeedRow
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = { controller.clickQZone(row, null) },
        onLongPress = { controller.clickQZone(row, null) },
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
            contentColor = MiuixTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HostAvatar(controller, row.avatar, row.title.take(1), 40)
                Column(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f)
                ) {
                    Text(
                        row.title,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        row.time.ifBlank { "动态" },
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    Icons.Filled.DynamicFeed,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary
                )
            }
            if (row.summary.isNotBlank()) {
                Text(
                    text = row.summary,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (row.pictures.isNotEmpty()) {
                QZonePictures(row.pictures, row.totalPictureCount)
            }
        }
    }
}

@Composable
private fun QZonePictures(
    pictures: List<PictureSpec>,
    totalPictureCount: Int
) {
    val visiblePictures = remember(pictures) { pictures.distinctBy { it.value } }
    var showBottomSheet by remember { mutableStateOf(false) }
    val gridPictures = remember(visiblePictures) { visiblePictures.take(9) }
    val declaredPictureCount = maxOf(totalPictureCount, visiblePictures.size)
    val remainingCount = (declaredPictureCount - gridPictures.size).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        gridPictures.chunked(3).forEachIndexed { rowIndex, rowPictures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowPictures.forEachIndexed { columnIndex, picture ->
                    val index = rowIndex * 3 + columnIndex
                    QZonePictureThumb(
                        picture = picture,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        remainingCount = if (remainingCount > 0 && index == gridPictures.lastIndex) {
                            remainingCount
                        } else {
                            0
                        },
                        onClick = { showBottomSheet = true }
                    )
                }
                repeat(3 - rowPictures.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }

    WindowBottomSheet(
        show = showBottomSheet,
        title = "全部图片",
        endAction = {
            IconButton(
                onClick = { showBottomSheet = false },
                minWidth = 36.dp,
                minHeight = 36.dp,
                cornerRadius = 18.dp,
                backgroundColor = MiuixTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        onDismissRequest = { showBottomSheet = false }
    ) {
        QZonePictureSheetContent(
            pictures = visiblePictures,
            show = showBottomSheet
        )
    }
}

@Composable
private fun QZonePictureSheetContent(
    pictures: List<PictureSpec>,
    show: Boolean
) {
    val gridState = rememberLazyGridState()
    var loadedCount by remember(pictures, show) {
        mutableIntStateOf(minOf(QZONE_SHEET_IMAGE_PAGE_SIZE, pictures.size))
    }
    val loadedPictures = remember(pictures, loadedCount) {
        pictures.take(loadedCount.coerceAtMost(pictures.size))
    }
    val hasMoreLocalPictures = loadedCount < pictures.size

    LaunchedEffect(gridState, pictures.size, loadedCount, show) {
        if (!show || !hasMoreLocalPictures) return@LaunchedEffect
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.distinctUntilChanged().collect { lastVisibleIndex ->
            if (lastVisibleIndex >= loadedPictures.lastIndex - 6) {
                loadedCount = minOf(loadedCount + QZONE_SHEET_IMAGE_PAGE_SIZE, pictures.size)
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        gridItems(loadedPictures, key = { it.value }) { picture ->
            QZonePictureThumb(
                picture = picture,
                modifier = Modifier.aspectRatio(1f),
                remainingCount = 0,
                onClick = null
            )
        }
        if (hasMoreLocalPictures) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                QZonePictureSheetFooter()
            }
        }
    }
}

@Composable
private fun QZonePictureSheetFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageLoadingIndicator()
        Text(
            text = "加载更多图片",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun QZonePictureThumb(
    picture: PictureSpec,
    modifier: Modifier,
    remainingCount: Int,
    onClick: (() -> Unit)?
) {
    val content: @Composable () -> Unit = {
        var retryVersion by remember(picture.value) { mutableIntStateOf(0) }
        val request = rememberImageRequest(
            value = picture.value,
            cacheKeyPrefix = "qzone-picture",
            requestVersion = retryVersion
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                val imageState by painter.state.collectAsState()
                when (imageState) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    is AsyncImagePainter.State.Loading -> Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        ImageLoadingIndicator()
                    }

                    else -> {
                        LaunchedEffect(picture.value, retryVersion) {
                            if (retryVersion < 2) {
                                delay(450L)
                                retryVersion += 1
                            }
                        }
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
            if (remainingCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.46f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$remainingCount",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (onClick != null) {
        Card(
            modifier = modifier,
            pressFeedbackType = PressFeedbackType.Sink,
            showIndication = true,
            onClick = onClick,
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.surfaceVariant,
                contentColor = MiuixTheme.colorScheme.onSurface
            )
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.surfaceVariant,
                contentColor = MiuixTheme.colorScheme.onSurface
            )
        ) {
            content()
        }
    }
}


