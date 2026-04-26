package io.github.wjy.meditate.ui.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import io.github.wjy.meditate.data.JournalEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyItemScope.JournalEntryItem(
    entry: JournalEntry,
    isScrolling: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onDelete: () -> Unit,
    onToggleSelection: () -> Unit
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
        enableDismissFromEndToStart = !isScrolling && !isSelectionMode,
        modifier = Modifier.animateItem(),
        backgroundContent = {
            DismissBackground(dismissState)
        }
    ) {
        JournalItem(
            entry = entry, 
            isSelected = isSelected, 
            isSelectionMode = isSelectionMode,
            onToggleSelection = onToggleSelection
        )
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
fun JournalItem(
    entry: JournalEntry,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelection: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { 
                    if (isSelectionMode) onToggleSelection()
                },
                onLongClick = {
                    if (!isSelectionMode) onToggleSelection()
                }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
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
}
