package com.studymate.presentation.screens.notes

import com.studymate.domain.model.Note
import com.studymate.domain.model.NoteCategory
import com.studymate.domain.usecase.NoteSortBy

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: NoteCategory? = null,
    val sortBy: NoteSortBy = NoteSortBy.UPDATED_DESC,
    val isLoading: Boolean = false,
    val isSearchActive: Boolean = false
)

sealed interface NotesEvent {
    data class OnSearchQueryChange(val query: String) : NotesEvent
    data class OnCategorySelect(val category: NoteCategory?) : NotesEvent
    data class OnSortByChange(val sortBy: NoteSortBy) : NotesEvent
    data class OnTogglePin(val note: Note) : NotesEvent
    data class OnDeleteNote(val note: Note) : NotesEvent
    data object OnToggleSearch : NotesEvent
}
