package io.github.wjy.meditate.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wjy.meditate.data.JournalEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 【首页屏幕 (HomeScreen)】
 * 包含符合 MD3 规范的滑动删除列表和位置可调的悬浮按钮。
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
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    
    val filteredEntries = remember(entries, selectedFilter) {
        if (selectedFilter == "全部") entries else entries.filter { it.moodTag == selectedFilter }
    }

    // 标签同步逻辑
    LaunchedEffect(allTags) {
        if (selectedFilter != "全部" && !allTags.contains(selectedFilter)) {
            selectedFilter = "全部"
        }
    }

    // 新增自动滚动
    var lastEntryCount by remember { mutableIntStateOf(entries.size) }
    LaunchedEffect(entries.size) {
        if (entries.size > lastEntryCount) {
            listState.animateScrollToItem(0)
        }
        lastEntryCount = entries.size
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            floatingActionButton = {
                if (!isAdding) {
                    FloatingActionButton(
                        onClick = {
                            isDeleteMode = false
                            isAdding = true
                        },
                        /*
                         * 【界面调整：右下角“+”按钮位置】
                         * 修改 padding 的 bottom 和 end 数值：
                         * bottom: 越大越靠上移。
                         * end: 越大越靠左移。
                         */
                        modifier = Modifier.padding(bottom = 28.dp, end = 28.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "添加", modifier = Modifier.size(24.dp))
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
                        // 1. 使用 SwipeToDismissBox (Material3 API)
                        val dismissState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { distance ->
                                distance * 0.6f   // 触发阈值
                            },
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteEntry(entry)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                                // 【滑动进度计算：解决闪烁与即时变红问题】
                                // 5. 滑动过程中背景透明度随滑动进度动态变化 (alpha 动画)
                                backgroundContent = {
                                    BoxWithConstraints {
                                    val offset = dismissState.requireOffset()
                                    val width = constraints.maxWidth.toFloat()
                                    val progress = (kotlin.math.abs(offset) / width).coerceIn(0f, 1f)
                                    val eased = FastOutSlowInEasing.transform(progress)
                                    val scale = 0.8f + 0.4f * eased

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = eased))
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.onError.copy(alpha = eased),
                                            modifier = Modifier.scale(scale)
                                        )
                                    }
                                }
                            },
                            // 2. 支持从右向左滑动 (EndToStart)
                            enableDismissFromStartToEnd = false,
                            modifier = Modifier.animateItem()
                        ) {
                            // 8. 前景内容使用 Card 包裹，符合 Material3 风格
                            JournalItem(entry = entry)
                        }
                    }
                }
            }
        }

        // 添加面板动画
        AnimatedVisibility(
            visible = isAdding,
            enter = scaleIn(
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                initialScale = 0f,
                transformOrigin = TransformOrigin(0.9f, 0.9f)
            ) + fadeIn(tween(300)),
            exit = scaleOut(
                animationSpec = tween(350, easing = FastOutSlowInEasing),
                targetScale = 0f,
                transformOrigin = TransformOrigin(0.9f, 0.9f)
            ) + fadeOut(tween(300))
        ) {
            AddEntryOverlay(
                existingTags = allTags,
                onDismiss = { isAdding = false },
                onSave = { content, tag, advice ->
                    viewModel.addEntry(content, tag, advice)
                    isAdding = false
                },
                onDeleteTag = { tag ->
                    viewModel.deleteEntriesByTag(tag)
                    sessionTags = sessionTags - tag
                },
                onTagSync = { tag ->
                    sessionTags = sessionTags + tag
                }
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
    var editingTag by remember { mutableStateOf<String?>(null) }
    var newTagText by remember { mutableStateOf("") }
    var tagDeleteMode by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val syncNewTagAndClose = {
        if (newTagText.isNotBlank()) {
            selectedTag = newTagText
            onTagSync(newTagText)
        }
        editingTag = null
    }

    val handleFinalSave = {
        if (editingTag != null && newTagText.isNotBlank()) {
            selectedTag = newTagText
            onTagSync(newTagText)
        }
        val finalTag = selectedTag.ifBlank { "未分类" }
        if (content.isNotBlank()) {
            onSave(content, finalTag, advice.ifBlank { null })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                if (editingTag != null) syncNewTagAndClose()
                else onDismiss() 
                tagDeleteMode = false
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (editingTag != null) syncNewTagAndClose()
                    else focusManager.clearFocus()
                    tagDeleteMode = false
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
                    placeholder = { Text("此刻的心情...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleFinalSave() }),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("分类", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(existingTags) { tag ->
                        Box(contentAlignment = Alignment.TopEnd) {
                            if (editingTag == tag) {
                                TextField(
                                    value = newTagText,
                                    onValueChange = { newTagText = it },
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(100.dp)
                                        .focusRequester(focusRequester),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { 
                                        syncNewTagAndClose()
                                    })
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp, end = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedTag == tag) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .combinedClickable(
                                            onClick = { selectedTag = tag },
                                            onLongClick = { tagDeleteMode = true }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(tag, color = if (selectedTag == tag) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                }
                                if (tagDeleteMode) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .zIndex(2f)
                                            .clickable { onDeleteTag(tag) }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        if (editingTag == null) {
                            IconButton(onClick = { 
                                editingTag = "new"
                                newTagText = ""
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "新增标签")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("建议 (可选)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                TextField(
                    value = advice,
                    onValueChange = { advice = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("给自己一点建议...") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { handleFinalSave() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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
        /*
         * 【界面定制：卡片不透明度】
         * alpha 数值越小越透明。
         */
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = entry.moodTag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )

            entry.selfAdvice?.let { advice ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = advice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
