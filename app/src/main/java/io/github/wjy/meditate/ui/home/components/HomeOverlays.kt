package io.github.wjy.meditate.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.wjy.meditate.data.JournalEntry

@Composable
fun AddEntryOverlay(
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
                .clip(RoundedCornerShape(28.dp))
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
                            Text(tag, style = MaterialTheme.typography.labelLarge)
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
                                Text("+", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            BasicTextField(
                                value = newTag,
                                onValueChange = { newTag = it },
                                modifier = Modifier
                                    .widthIn(min = 64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (newTag.isEmpty()) {
                                        Text(
                                            "新标签",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
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
fun TrashOverlay(
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
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("回收站", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(entries, key = { it.id }) { entry ->
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
