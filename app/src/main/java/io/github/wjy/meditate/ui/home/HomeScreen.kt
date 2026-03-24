package io.github.wjy.meditate.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wjy.meditate.data.JournalEntry
import io.github.wjy.meditate.ui.components.PaperCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val tags by viewModel.tags.collectAsState()
    var isAdding by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("全部") }
    var isDeleteMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    val filteredEntries = remember(entries, selectedFilter) {
        if (selectedFilter == "全部") entries else entries.filter { it.moodTag == selectedFilter }
    }

    // 解决“分类删完后不自动回到全部”的问题
    LaunchedEffect(tags) {
        if (selectedFilter != "全部" && !tags.contains(selectedFilter)) {
            selectedFilter = "全部"
        }
    }

    var lastEntryCount by remember { mutableIntStateOf(entries.size) }

    LaunchedEffect(entries.size) {
        if (entries.size > lastEntryCount) {
            listState.scrollToItem(0)
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
                        onClick = { isAdding = true },
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
                        detectTapGestures(onTap = { isDeleteMode = false })
                    }
            ) {
                CategoryRow(
                    categories = listOf("全部") + tags,
                    selectedCategory = selectedFilter,
                    isDeleteMode = isDeleteMode,
                    onCategorySelected = { 
                        selectedFilter = it
                        isDeleteMode = false
                    },
                    onCategoryLongClick = { isDeleteMode = true },
                    onDeleteCategory = { tag ->
                        viewModel.deleteEntriesByTag(tag)
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
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteEntry(entry)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            modifier = Modifier.animateItem()
                        ) {
                            JournalItem(entry = entry)
                        }
                    }
                }
            }
        }

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
                existingTags = tags,
                onDismiss = { isAdding = false },
                onSave = { content, tag, advice ->
                    viewModel.addEntry(content, tag, advice)
                    isAdding = false
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
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        items(categories) { category ->
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedCategory == category) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    contentColor = if (selectedCategory == category) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = { onCategorySelected(category) },
                        onLongClick = { if (category != "全部") onCategoryLongClick() }
                    )
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                if (isDeleteMode && category != "全部") {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = 6.dp, y = (-6).dp)
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
    onSave: (String, String, String?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var advice by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("") }
    var showNewTagInput by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                focusManager.clearFocus()
                onDismiss() 
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
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
                    placeholder = { Text("自言自语...") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("分类", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(existingTags) { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedTag == tag) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.combinedClickable(
                                onClick = { 
                                    selectedTag = tag
                                    showNewTagInput = false
                                },
                                onDoubleClick = {
                                    newTagText = tag
                                    selectedTag = tag 
                                    showNewTagInput = true
                                }
                            )
                        ) {
                            Text(tag, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    item {
                        if (showNewTagInput) {
                            TextField(
                                value = newTagText,
                                onValueChange = { newTagText = it },
                                placeholder = { Text("新分类...") },
                                singleLine = true,
                                modifier = Modifier
                                    .width(120.dp)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { 
                                        if (!it.isFocused && newTagText.isNotBlank()) {
                                            selectedTag = newTagText
                                            showNewTagInput = false
                                        }
                                    },
                                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp)
                            )
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        } else {
                            AssistChip(
                                onClick = { 
                                    newTagText = ""
                                    showNewTagInput = true 
                                }, 
                                label = { Text("添加") }, 
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }, 
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("建议", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                TextField(
                    value = advice,
                    onValueChange = { advice = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("理性的理清...") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        val finalTag = if (newTagText.isNotBlank()) newTagText else if (selectedTag.isNotBlank()) selectedTag else "未分类"
                        if (content.isNotBlank()) onSave(content, finalTag, advice.ifBlank { null }) 
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("存入墙壁")
                }
            }
        }
    }
}

private val journalSdf = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())

@Composable
fun JournalItem(entry: JournalEntry, modifier: Modifier = Modifier) {
    val dateText = remember(entry.timestamp) { journalSdf.format(Date(entry.timestamp)) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(text = entry.moodTag, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Text(text = dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = entry.content, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp, letterSpacing = 0.5.sp), color = MaterialTheme.colorScheme.onSurface)
            if (!entry.selfAdvice.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "理性的回响", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = entry.selfAdvice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
