package io.github.wjy.meditate.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wjy.meditate.data.JournalEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 【首页屏幕 (HomeScreen)】
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    // 数据订阅
    val entries by viewModel.entries.collectAsState()
    val dbTags by viewModel.tags.collectAsState()
    var sessionTags by remember { mutableStateOf(setOf<String>()) }
    val allTags = remember(dbTags, sessionTags) {
        (dbTags + sessionTags).distinct().filter { it.isNotBlank() }
    }

    // 交互状态
    var isAdding by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("全部") }
    var isDeleteMode by remember { mutableStateOf(false) }
    var isShowingTrash by remember { mutableStateOf(false) }
    var showTrashFab by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    
    val filteredEntries = remember(entries, selectedFilter) {
        if (selectedFilter == "全部") entries else entries.filter { it.moodTag == selectedFilter }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            floatingActionButton = {
                if (!isAdding) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 28.dp, end = 28.dp)
                    ) {
                        // 1. 回收站按钮 (通过长按弹出)
                        AnimatedVisibility(
                            visible = showTrashFab,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            FloatingActionButton(
                                onClick = { 
                                    isShowingTrash = true
                                    showTrashFab = false
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "回收站")
                            }
                        }

                        // 2. 主添加按钮 (支持长按手势)
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
                                        onClick = { 
                                            isAdding = true
                                            showTrashFab = false 
                                        },
                                        onLongClick = { showTrashFab = !showTrashFab }
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
                    categories = listOf("全部") + allTags,
                    selectedCategory = selectedFilter,
                    isDeleteMode = isDeleteMode,
                    onCategorySelected = { 
                        selectedFilter = it
                        isDeleteMode = false
                    },
                    onCategoryLongClick = { isDeleteMode = true },
                    onDeleteCategory = { tag ->
                        viewModel.deleteEntriesByTag(tag)
                        sessionTags = sessionTags - tag
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
                        // 互斥优化：滚动时不开启左右滑动
                        val isScrolling = listState.isScrollInProgress
                        val dismissState = rememberSwipeToDismissBoxState()

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = !isScrolling,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp))
                                        .graphicsLayer {
                                            val offset = try { dismissState.requireOffset() } catch (e: Exception) { 0f }
                                            val progress = if (size.width > 0) (kotlin.math.abs(offset) / size.width).coerceIn(0f, 1f) else 0f
                                            alpha = FastOutSlowInEasing.transform(progress)
                                        }
                                        .background(MaterialTheme.colorScheme.error)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.graphicsLayer {
                                            val offset = try { dismissState.requireOffset() } catch (e: Exception) { 0f }
                                            val progress = if (size.width > 0) (kotlin.math.abs(offset) / size.width).coerceIn(0f, 1f) else 0f
                                            val scale = 0.8f + 0.4f * FastOutSlowInEasing.transform(progress)
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.animateItem()
                        ) {
                            JournalItem(entry = entry)
                        }
                        var hasDeleted by remember { mutableStateOf(false) }

                        LaunchedEffect(dismissState.currentValue) {
                            if (
                                dismissState.currentValue == SwipeToDismissBoxValue.EndToStart &&
                                !hasDeleted
                            ) {
                                hasDeleted = true
                                viewModel.softDeleteEntry(entry)
                            }
                        }
                    }
                }
            }
        }

        // 回收站面板
        val deletedEntries by viewModel.deletedEntries.collectAsState(initial = emptyList())
        AnimatedVisibility(
            visible = isShowingTrash,
            modifier = Modifier.zIndex(100f),
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            TrashScreen(
                deletedEntries = deletedEntries,
                onClose = { isShowingTrash = false },
                onRestore = { entry -> viewModel.restoreEntry(entry); isShowingTrash = false },
                onPermanentlyDelete = { entry -> viewModel.permanentlyDeleteEntry(entry) },
                onEmptyTrash = { viewModel.emptyTrash(); isShowingTrash = false }
            )
        }
        
        // 添加面板
        AnimatedVisibility(
            visible = isAdding,
            enter = fadeIn() + scaleIn(initialScale = 0.9f, transformOrigin = TransformOrigin(0.9f, 0.9f)),
            exit = fadeOut() + scaleOut(targetScale = 0.9f, transformOrigin = TransformOrigin(0.9f, 0.9f))
        ) {
            AddEntryOverlay(
                existingTags = allTags,
                onDismiss = { isAdding = false },
                onSave = { content, tag, advice ->
                    viewModel.addEntry(content, tag, advice)
                    isAdding = false
                },
                onDeleteTag = { tag -> viewModel.deleteEntriesByTag(tag); sessionTags = sessionTags - tag },
                onTagSync = { tag -> sessionTags = sessionTags + tag }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryRow(
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
        items(categories) { category ->
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedCategory == category) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = { onCategorySelected(category) },
                            onLongClick = { if (category != "全部") onCategoryLongClick() }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
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
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddEntryOverlay(
    existingTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    onDeleteTag: (String) -> Unit,
    onTagSync: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var advice by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(existingTags) }
    
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
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() },
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
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { if (content.isNotBlank()) onSave(content, selectedTag.ifBlank { "未分类" }, advice.ifBlank { null }) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
fun TrashScreen(deletedEntries: List<JournalEntry>, onClose: () -> Unit, onRestore: (JournalEntry) -> Unit, onPermanentlyDelete: (JournalEntry) -> Unit, onEmptyTrash: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("回收站", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(deletedEntries) { entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.content.take(20) + "...", modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRestore(entry) }) { Text("恢复") }
                            IconButton(onClick = { onPermanentlyDelete(entry) }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                Button(onClick = onEmptyTrash, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("清空") }
            }
        }
    }
}
