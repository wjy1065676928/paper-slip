package io.github.wjy.meditate.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wjy.meditate.data.NoteDatabase
import io.github.wjy.meditate.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NoteDatabase.getDatabase(application).noteDao()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> get() = _notes

    init {
        viewModelScope.launch {
            dao.getAllNotes().collectLatest { list ->
                _notes.value = list
            }
        }
    }

    fun addNote(content: String, tag: String) {
        viewModelScope.launch {
            dao.insert(Note(content = content, tag = tag))
        }
    }

    fun removeNote(note: Note) {
        viewModelScope.launch {
            dao.delete(note)
        }
    }
}
