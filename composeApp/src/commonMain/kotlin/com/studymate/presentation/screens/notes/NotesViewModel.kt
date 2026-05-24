package com.studymate.presentation.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.domain.model.Note
import com.studymate.domain.usecase.DeleteNoteUseCase
import com.studymate.domain.usecase.GetAllNotesUseCase
import com.studymate.domain.usecase.SaveNoteUseCase
import com.studymate.domain.usecase.SearchNotesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    
    val state: StateFlow<NotesUiState> = combine(
        _state,
        _state.flatMapLatest { state ->
            if (state.searchQuery.isBlank() && state.selectedCategory == null) {
                getAllNotesUseCase(state.sortBy)
            } else {
                searchNotesUseCase(state.searchQuery, state.selectedCategory)
            }
        }
    ) { state, notes ->
        state.copy(notes = notes, isLoading = false)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NotesUiState(isLoading = true)
    )

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
            is NotesEvent.OnCategorySelect -> {
                _state.update { it.copy(selectedCategory = event.category) }
            }
            is NotesEvent.OnSortByChange -> {
                _state.update { it.copy(sortBy = event.sortBy) }
            }
            is NotesEvent.OnTogglePin -> {
                togglePin(event.note)
            }
            is NotesEvent.OnDeleteNote -> {
                deleteNote(event.note)
            }
            NotesEvent.OnToggleSearch -> {
                _state.update { 
                    it.copy(
                        isSearchActive = !it.isSearchActive,
                        searchQuery = if (it.isSearchActive) "" else it.searchQuery
                    )
                }
            }
        }
    }

    private fun togglePin(note: Note) {
        viewModelScope.launch {
            saveNoteUseCase(note.copy(isPinned = !note.isPinned))
        }
    }

    private fun deleteNote(note: Note) {
        viewModelScope.launch {
            deleteNoteUseCase(note.id)
        }
    }
}
