package io.github.wjy.meditate.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wjy.meditate.data.JournalEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 【首页屏幕 (HomeScreen)】
 *
 * 架构：MVI (Single Source of Truth)
 * 交互：基于最新 Material 3 的行为模式 (SwipeToDismissBox)
 * 性能：基于 snapshotFlow 与 derivedStateOf 的精准响应
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    // 1. 订阅唯一 UI 状态流
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 2. 局部 UI 状态
    var isAdding by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("全部") }
    var isDeleteMode by remember { mutableStateOf(false) }
    var isShowingTrash by remember { mutableStateOf(false) }
    var showTrashFab by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // 3. 派生状态 (Derived States)
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                HomeTopBar(onNavigateToSettings)
            },
            floatingActionButton = {
                if (!isAdding) {
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
                            focusManager.clearFocus()
                        })
                    }
            ) {
                CategoryRow(
                    categories = categories,
                    selectedCategory = selectedFilter,
                    isDeleteMode = isDeleteMode,
                    onCategorySelected = { 
                        selectedFilter = it
                        isDeleteMode = false
                    },
                    onCategoryLongClick = { isDeleteMode = true },
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
                            onDelete = { viewModel.onAction(HomeAction.SoftDeleteEntry(entry)) }
                        )
                    }
                }
            }
        }

        TrashOverlay(
            visible = isShowingTrash,
            entries = uiState.deletedEntries,
            onClose = { isShowingTrash = false },
            onRestore = { viewModel.onAction(HomeAction.RestoreEntry(it)); isShowingTrash = false },
            onPermanentlyDelete = { viewModel.onAction(HomeAction.PermanentlyDeleteEntry(it)) },
            onEmptyTrash = { viewModel.onAction(HomeAction.EmptyTrash); isShowingTrash = false }
        )
        
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "设置")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyItemScope.JournalEntryItem(
    entry: JournalEntry,
    isScrolling: Boolean,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.7f }
    )
    
    val currentOnDelete by rememberUpdatedState(onDelete)

    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.currentValue }
            .filter { it == SwipeToDismissBoxValue.EndToStart }
            .distinctUntilChanged()
            .collect {
                currentOnDelete()
            }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isScrolling,
        modifier = Modifier.animateItem(),
        backgroundContent = {
            DismissBackground(dismissState)
        }
    ) {
        JournalItem(entry = entry)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val progress = dismissState.progress
    
    val animatedProgress = remember(progress) {
        FastOutSlowInEasing.transform(progress)
    }

    val backgroundColor = lerp(
        start = MaterialTheme.colorScheme.surfaceContainerHigh,
        stop = MaterialTheme.colorScheme.error,
        fraction = animatedProgress
    )

    val iconScale by animateFloatAsState(
        targetValue = if (progress > 0.5f) 1.2f else 0.8f,
        label = "IconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "删除",
            tint = if (progress > 0.5f) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
                alpha = animatedProgress.coerceIn(0f, 1f)
            }
        )
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

@Composable
private fun AddEntryOverlay(
    visible: Boolean,
    existingTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    onTagSync: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f, transformOrigin = TransformOrigin(0.9f, 0.9f)),
        exit = fadeOut() + scaleOut(targetScale = 0.9f, transformOrigin = TransformOrigin(0.9f, 0.9f))
    ) {
        AddEntryContent(existingTags, onDismiss, onSave, onTagSync)
    }
}

@Composable
private fun AddEntryContent(
    existingTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    onTagSync: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("") }
    var tags by remember(existingTags) { mutableStateOf(existingTags) }
    var newTag by remember { mutableStateOf("") }
    var isAddingNewTag by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (isAddingNewTag) {
                        if (newTag.isNotBlank() && newTag !in tags) {
                            tags = tags + newTag
                            onTagSync(newTag)
                            selectedTag = newTag
                        }
                        newTag = ""
                        isAddingNewTag = false
                    }
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("你有什么话要说") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, 
                        unfocusedContainerColor = Color.Transparent, 
                        focusedIndicatorColor = Color.Transparent, 
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedTag == tag) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedTag = if (selectedTag == tag) "" else tag }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(tag)
                        }
                    }

                    item {
                        if (!isAddingNewTag) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { isAddingNewTag = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+")
                            }
                        } else {
                            TextField(
                                value = newTag,
                                onValueChange = { newTag = it },
                                modifier = Modifier.widthIn(min = 64.dp),
                                placeholder = { Text("新标签") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { if (content.isNotBlank()) onSave(content, selectedTag.ifBlank { "未分类" }, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = content.isNotBlank()
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
fun JournalItem(entry: JournalEntry) {
    val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelMedium)
                Text(entry.moodTag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(entry.content, style = MaterialTheme.typography.bodyLarge)
            entry.selfAdvice?.let {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrashOverlay(
    visible: Boolean,
    entries: List<JournalEntry>,
    onClose: () -> Unit,
    onRestore: (JournalEntry) -> Unit,
    onPermanentlyDelete: (JournalEntry) -> Unit,
    onEmptyTrash: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.zIndex(100f),
        enter = fadeIn() + scaleIn(initialScale = 0.9f, transformOrigin = TransformOrigin(0.9f, 0.9f)),
        exit = fadeOut() + scaleOut(targetScale = 0.9f, transformOrigin = TransformOrigin(0.9f, 0.9f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("回收站", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(entries) { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.content.take(20) + "...", modifier = Modifier.weight(1f))
                                TextButton(onClick = { onRestore(entry) }) { Text("恢复") }
                                IconButton(onClick = { onPermanentlyDelete(entry) }) { 
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) 
                                }
                            }
                        }
                    }
                    Button(onClick = onEmptyTrash, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("清空") }
                }
            }
        }
    }
}
