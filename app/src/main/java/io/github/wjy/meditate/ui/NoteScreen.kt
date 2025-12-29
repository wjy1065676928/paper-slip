package io.github.wjy.meditate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wjy.meditate.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    noteViewModel: NoteViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("碎碎念") }

    val notes by noteViewModel.notes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 输入区
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        noteViewModel.addNote(text, tag)
                        text = ""
                    }
                }
            ) {
                Text("添加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tag 选择
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("碎碎念", "感悟", "随笔").forEach { t ->
                FilterChip(
                    selected = t == tag,
                    onClick = { tag = t },
                    label = { Text(t) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 列表
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { note ->

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart ||
                            value == SwipeToDismissBoxValue.StartToEnd
                        ) {
                            noteViewModel.removeNote(note)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        // 这里可以以后加红色删除背景
                    }
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(note.content)
                            Text(
                                text = "Tag: ${note.tag}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
