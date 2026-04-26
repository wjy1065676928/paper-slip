@file:Suppress("DEPRECATION")

package io.github.wjy.meditate.ui.home

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.drawToBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wjy.meditate.data.SettingsManager
import io.github.wjy.meditate.network.QrCodeUtils
import io.github.wjy.meditate.ui.home.components.AddEntryOverlay
import io.github.wjy.meditate.ui.home.components.FastTransferOverlay
import io.github.wjy.meditate.ui.home.components.HomeTopBar
import io.github.wjy.meditate.ui.home.components.JournalEntryItem
import io.github.wjy.meditate.ui.home.components.TrashOverlay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ShareDto(
    val c: String, // content
    val t: String, // tag
    val d: Long,   // date
    val a: String? // advice
)

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isAdding by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("全部") }
    var isDeleteMode by remember { mutableStateOf(false) }
    var isShowingTrash by remember { mutableStateOf(false) }
    var showTrashFab by remember { mutableStateOf(false) }
    
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
    
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val categories by remember(uiState.dbTags, uiState.sessionTags, uiState.entries.isEmpty()) {
        derivedStateOf {
            val tags = (uiState.dbTags + uiState.sessionTags).distinct().filter { it.isNotBlank() }
            if (uiState.entries.isEmpty()) tags else listOf("全部") + tags
        }
    }

    val filteredEntries by remember(uiState.entries, selectedFilter) {
        derivedStateOf {
            if (selectedFilter == "全部") uiState.entries else uiState.entries.filter { it.moodTag == selectedFilter }
        }
    }

    LaunchedEffect(categories) {
        if (selectedFilter !in categories && uiState.entries.isNotEmpty()) {
            selectedFilter = "全部"
        }
    }

    val isOverlayVisible = isAdding || isShowingTrash || qrBitmap != null
    val view = LocalView.current
    
    var blurredBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(isOverlayVisible, uiState.blurIntensity) {
        if (isOverlayVisible && uiState.blurEnabled && uiState.blurImplementation == SettingsManager.IMPL_RENDER_SCRIPT) {
            val screenshot = view.drawToBitmap()
            val rs = RenderScript.create(context)
            val input = Allocation.createFromBitmap(rs, screenshot)
            val output = Allocation.createTyped(rs, input.type)
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            script.setRadius(uiState.blurIntensity.coerceIn(1f, 25f))
            script.setInput(input)
            script.forEach(output)
            output.copyTo(screenshot)
            blurredBitmap = screenshot.asImageBitmap()
            rs.destroy()
        } else if (!isOverlayVisible) {
            blurredBitmap = null
        }
    }
    
    val blurRadius by animateFloatAsState(
        targetValue = if (isOverlayVisible && uiState.blurEnabled && 
            (uiState.blurImplementation == SettingsManager.IMPL_HARDWARE || 
             uiState.blurImplementation == SettingsManager.IMPL_RENDER_SCRIPT)) uiState.blurIntensity else 0f,
        label = "BlurAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blurRadius.dp),
            topBar = {
                HomeTopBar(
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedIds.size,
                    totalVisibleCount = filteredEntries.size,
                    onSettingsClick = onNavigateToSettings,
                    onCancelSelection = { selectedIds = emptySet() },
                    onToggleSelectAll = {
                        selectedIds = if (selectedIds.size == filteredEntries.size) {
                            emptySet()
                        } else {
                            filteredEntries.map { it.id }.toSet()
                        }
                    },
                    onDeleteSelected = {
                        val toDelete = uiState.entries.filter { it.id in selectedIds }
                        viewModel.onAction(HomeAction.SoftDeleteEntries(toDelete))
                        selectedIds = emptySet()
                    },
                    onShareSelected = {
                        val toShare = uiState.entries.filter { it.id in selectedIds }
                        try {
                            val dtoList = toShare.map { entry ->
                                ShareDto(
                                    c = entry.content,
                                    t = entry.moodTag,
                                    d = entry.timestamp,
                                    a = entry.selfAdvice
                                )
                            }
                            val json = Json.encodeToString(dtoList)
                            
                            if (json.length > 2000) {
                                Toast.makeText(context, "选中内容过多，超过二维码传输上限", Toast.LENGTH_SHORT).show()
                            } else {
                                qrBitmap = QrCodeUtils.generateQrCode(json, 800)
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "分享失败：内容异常", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!isAdding && !isSelectionMode) {
                    HomeActionButtons(
                        showTrashFab = showTrashFab,
                        onTrashClick = { 
                            isShowingTrash = true
                            showTrashFab = false
                        },
                        onAddClick = { 
                            isAdding = true
                            showTrashFab = false 
                        },
                        onAddLongClick = { showTrashFab = !showTrashFab }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { 
                            isDeleteMode = false 
                            showTrashFab = false
                            selectedIds = emptySet()
                            focusManager.clearFocus()
                        })
                    }
            ) {
                CategoryRow(
                    categories = categories,
                    selectedCategory = selectedFilter,
                    isDeleteMode = isDeleteMode && !isSelectionMode,
                    onCategorySelected = { 
                        if (!isSelectionMode) {
                            selectedFilter = it
                            isDeleteMode = false
                        }
                    },
                    onCategoryLongClick = { if (!isSelectionMode) isDeleteMode = true },
                    onDeleteCategory = { tag ->
                        viewModel.onAction(HomeAction.DeleteEntriesByTag(tag))
                    }
                )
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = filteredEntries,
                        key = { it.id },
                        contentType = { "journal_entry" }
                    ) { entry ->
                        JournalEntryItem(
                            entry = entry,
                            isScrolling = listState.isScrollInProgress,
                            isSelected = entry.id in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onDelete = { viewModel.onAction(HomeAction.SoftDeleteEntry(entry)) },
                            onToggleSelection = {
                                selectedIds = if (entry.id in selectedIds) {
                                    selectedIds - entry.id
                                } else {
                                    selectedIds + entry.id
                                }
                            }
                        )
                    }
                }
            }
        }

        blurredBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().zIndex(85f),
                contentScale = ContentScale.FillBounds
            )
        }

        Box(modifier = Modifier.zIndex(100f)) {
            TrashOverlay(
                visible = isShowingTrash,
                entries = uiState.deletedEntries,
                onClose = { isShowingTrash = false },
                onRestore = { viewModel.onAction(HomeAction.RestoreEntry(it)); isShowingTrash = false },
                onPermanentlyDelete = { viewModel.onAction(HomeAction.PermanentlyDeleteEntry(it)) },
                onEmptyTrash = { viewModel.onAction(HomeAction.EmptyTrash); isShowingTrash = false }
            )
        }
        
        Box(modifier = Modifier.zIndex(110f)) {
            AddEntryOverlay(
                visible = isAdding,
                existingTags = remember(uiState.dbTags, uiState.sessionTags) { 
                    (uiState.dbTags + uiState.sessionTags).distinct().filter { it.isNotBlank() } 
                },
                onDismiss = { isAdding = false },
                onSave = { content, tag, advice ->
                    viewModel.onAction(HomeAction.AddEntry(content, tag, advice))
                    isAdding = false
                },
                onTagSync = { viewModel.onAction(HomeAction.AddSessionTag(it)) }
            )
        }

        FastTransferOverlay(
            visible = qrBitmap != null,
            qrBitmap = qrBitmap,
            canSwitchMode = false, // 主界面仅用于展示码，不提供扫码切换
            onClose = { qrBitmap = null },
            onImport = { viewModel.onAction(HomeAction.ImportEntries(it)) }
        )
    }
}

@Composable
private fun HomeActionButtons(
    showTrashFab: Boolean,
    onTrashClick: () -> Unit,
    onAddClick: () -> Unit,
    onAddLongClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 28.dp, end = 28.dp)
    ) {
        AnimatedVisibility(
            visible = showTrashFab,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            FloatingActionButton(
                onClick = onTrashClick,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "回收站")
            }
        }

        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = onAddClick,
                        onLongClick = onAddLongClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = "添加",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRow(
    categories: List<String>,
    selectedCategory: String,
    isDeleteMode: Boolean,
    onCategorySelected: (String) -> Unit,
    onCategoryLongClick: () -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items = categories, key = { it }) { category ->
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = { onCategorySelected(category) },
                            onLongClick = { if (category != "全部") onCategoryLongClick() }
                        ),
                    color = if (selectedCategory == category) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (selectedCategory == category) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                if (isDeleteMode && category != "全部") {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(22.dp)
                            .zIndex(2f)
                            .clickable { onDeleteCategory(category) },
                        shadowElevation = 4.dp
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}
