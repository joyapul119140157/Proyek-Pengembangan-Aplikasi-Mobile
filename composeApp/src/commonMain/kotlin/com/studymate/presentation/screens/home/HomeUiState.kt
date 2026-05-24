package com.studymate.presentation.screens.home

import com.studymate.domain.model.Note
import com.studymate.domain.model.NoteCategory
import com.studymate.domain.usecase.NoteSortBy

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val category: NoteCategory? = null,
    val sortBy: NoteSortBy = NoteSortBy.UPDATED_DESC,
    val isLoading: Boolean = false,
    val dailyMantra: String = "Lulus PAM dengan nilai A!",
    val streakCount: Int = 0,
    val error: String? = null
)
