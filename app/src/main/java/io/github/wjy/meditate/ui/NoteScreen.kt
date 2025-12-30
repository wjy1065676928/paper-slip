package io.github.wjy.meditate.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wjy.meditate.model.Note

/**
 * 笔记主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    modifier: Modifier = Modifier,
    noteViewModel: NoteViewModel = viewModel()
) {
    // --- 状态管理 ---
    var text by remember { mutableStateOf("") } // 笔记输入内容
    var tag by remember { mutableStateOf("碎碎念") } // 选中标签
    var isInputActive by remember { mutableStateOf(false) } // 输入模式激活状态
    var expandedNoteId by remember { mutableStateOf<Int?>(null) } // 当前展开（长按）的笔记ID

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() } // 用于主动控制输入框聚焦
    val notes by noteViewModel.notes.collectAsState()
    
    // --- 布局工具 ---
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp
    
    // --- 核心动画进度 (0.0: 问号状态 -> 1.0: 展开输入状态) ---
    val progress by animateFloatAsState(
        targetValue = if (isInputActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy, 
            stiffness = Spring.StiffnessMediumLow       
        ),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // 全局背景点击监听：点击空白处时，恢复问号状态并收起所有展开的笔记卡片
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isInputActive = false
                focusManager.clearFocus()
                expandedNoteId = null // 点击背景时重置展开状态
            }
    ) {
        // --- 第一层：笔记列表层 (zIndex 1) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 输入时列表淡出
                .graphicsLayer { alpha = 1f - (progress * 0.7f) }
                .zIndex(1f)
        ) {
            NoteList(
                notes = notes, 
                viewModel = noteViewModel, 
                topPadding = 80.dp, // 问号上移了，顶部边距也相应调小，让布局更紧凑
                expandedNoteId = expandedNoteId,
                onExpandNote = { expandedNoteId = it }
            )
        }

        // --- 第二层：全屏模糊遮罩层 (zIndex 2) ---
        // 关键：输入模式下拦截所有手势，点击它同样会触发收起笔记
        if (progress > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f) 
                    .graphicsLayer { alpha = progress }
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .blur(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isInputActive = false
                        focusManager.clearFocus()
                        expandedNoteId = null // 遮罩点击也触发收回
                    }
            )
        }

        // --- 第三层：悬浮交互层 (zIndex 3) ---
        // 位移计算：将问号的初始位置上移至 10.dp，展开后升至屏幕 1/3.5 的位置
        val translationY = with(density) { 
            lerp(10.dp, screenHeight / 3.5f, progress).toPx() 
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(3f) 
                .graphicsLayer {
                    this.translationY = translationY
                },
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = isInputActive,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.9f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.9f))
                },
                label = "inputSwitch"
            ) { active ->
                if (!active) {
                    // --- 初始态：精致小巧的问号 ---
                    Text(
                        text = "?",
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isInputActive = true }
                            .padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.W300,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                } else {
                    // --- 展开态：输入框区域 ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text("记录此刻的想法...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)) },
                            // 统一圆角：使用 medium，与下方的笔记卡片保持一致
                            shape = MaterialTheme.shapes.medium, 
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (text.isNotBlank()) {
                                    TextButton(onClick = {
                                        noteViewModel.addNote(text, tag)
                                        text = ""
                                        isInputActive = false
                                        focusManager.clearFocus()
                                    }) {
                                        Text("发布", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        )
                        
                        // 底部标签选择器
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            listOf("碎碎念", "感悟", "随笔").forEach { t ->
                                FilterChip(
                                    selected = t == tag,
                                    onClick = { tag = t },
                                    label = { Text(t) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // 展开时自动弹出键盘
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    }
}

/**
 * 笔记列表封装
 */
@Composable
fun NoteList(
    notes: List<Note>, 
    viewModel: NoteViewModel, 
    topPadding: androidx.compose.ui.unit.Dp,
    expandedNoteId: Int?,
    onExpandNote: (Int?) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        // 顶部留出间距，避免问号遮挡第一条信息
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 60.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteItem(
                note = note, 
                viewModel = viewModel,
                isExpanded = expandedNoteId == note.id,
                onToggleExpand = {
                    // 长按切换：点击已展开的则关闭，否则展开新的
                    onExpandNote(if (expandedNoteId == note.id) null else note.id)
                },
                onCollapse = { onExpandNote(null) }
            )
        }
    }
}

/**
 * 单个笔记卡片：支持长按放大展现详情
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note, 
    viewModel: NoteViewModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCollapse: () -> Unit
) {
    // 展开时的轻微放大动画
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "itemScale"
    )

    // 侧滑删除控制
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                viewModel.removeNote(note) // 侧滑到底执行删除
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) 
                Color.Red.copy(alpha = 0.5f) else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, "删除", tint = Color.White)
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .combinedClickable(
                        onClick = onCollapse, // 单击收起
                        onLongClick = onToggleExpand, // 长按切换展开状态
                        indication = null, 
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isExpanded) 4.dp else 0.5.dp // 展开时阴影深一点
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                // 使用默认圆润圆角
                shape = MaterialTheme.shapes.medium 
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(note.content, style = MaterialTheme.typography.bodyLarge)
                    
                    // Tag 显示：带有平滑的展开效果
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = "#${note.tag}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    )
}
