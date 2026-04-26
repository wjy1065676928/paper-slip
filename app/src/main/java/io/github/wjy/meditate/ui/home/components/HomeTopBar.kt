package io.github.wjy.meditate.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalVisibleCount: Int,
    onSettingsClick: () -> Unit,
    onCancelSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onShareSelected: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { 
            if (isSelectionMode) {
                Text("已选 $selectedCount", style = MaterialTheme.typography.titleMedium)
            }
        },
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onCancelSelection) {
                    Icon(Icons.Default.Close, contentDescription = "取消")
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onToggleSelectAll) {
                    Icon(
                        imageVector = if (selectedCount == totalVisibleCount) Icons.Default.RemoveDone else Icons.Default.DoneAll,
                        contentDescription = if (selectedCount == totalVisibleCount) "取消全选" else "全选"
                    )
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Default.Delete, contentDescription = "删除选中")
                }
                IconButton(onClick = onShareSelected) {
                    Icon(Icons.Default.Share, contentDescription = "分享选中")
                }
            } else {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
