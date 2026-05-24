package com.studymate.presentation.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.domain.model.Note
import com.studymate.domain.model.NoteCategory
import com.studymate.domain.model.NoteColor
import com.studymate.domain.repository.NoteRepository
import com.studymate.domain.usecase.SaveNoteUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class AddEditViewModel(
    private val repository: NoteRepository,
    private val saveNoteUseCase: SaveNoteUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<AddEditEvent>()
    val events: SharedFlow<AddEditEvent> = _events.asSharedFlow()
    
    private var currentNoteId: Long? = null
    
    fun loadNote(noteId: Long) {
        currentNoteId = noteId
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            repository.getNoteById(noteId).collect { note ->
                note?.let {
                    _uiState.update { state ->
                        state.copy(
                            title = note.title,
                            content = note.content,
                            category = note.category,
                            color = note.color,
                            isLoading = false,
                            isEditMode = true,
                            createdAt = note.createdAt
                        )
                    }
                }
            }
        }
    }
    
    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title, titleError = null) }
    }
    
    fun onContentChange(content: String) {
        _uiState.update { it.copy(content = content) }
    }
    
    fun onCategoryChange(category: NoteCategory) {
        _uiState.update { it.copy(category = category) }
    }
    
    fun onColorChange(color: NoteColor) {
        _uiState.update { it.copy(color = color) }
    }
    
    fun saveNote() {
        val state = _uiState.value
        
        if (state.title.isBlank() && state.content.isBlank()) {
            _uiState.update { it.copy(titleError = "Judul atau konten harus diisi") }
            return
        }
        
        _uiState.update { it.copy(isSaving = true) }
        
        viewModelScope.launch {
            val note = Note(
                id = currentNoteId ?: 0,
                title = state.title.trim(),
                content = state.content.trim(),
                category = state.category,
                color = state.color,
                createdAt = if (currentNoteId == null) Clock.System.now() else state.createdAt,
                updatedAt = Clock.System.now()
            )
            
            saveNoteUseCase(note)
                .onSuccess {
                    _events.emit(AddEditEvent.NoteSaved)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(AddEditEvent.Error(error.message ?: "Gagal menyimpan"))
                }
        }
    }
    
    fun applyAISuggestion(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }
}
