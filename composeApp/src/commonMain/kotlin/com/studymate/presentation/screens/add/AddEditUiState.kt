package com.studymate.presentation.screens.add

import com.studymate.domain.model.NoteCategory
import com.studymate.domain.model.NoteColor
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class AddEditUiState(
    val title: String = "",
    val content: String = "",
    val category: NoteCategory = NoteCategory.GENERAL,
    val color: NoteColor = NoteColor.DEFAULT,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val titleError: String? = null,
    val createdAt: Instant = Clock.System.now()
) {
    val isValid: Boolean
        get() = title.isNotBlank() || content.isNotBlank()
    
    val canSave: Boolean
        get() = isValid && !isSaving
}

sealed interface AddEditEvent {
    data object NoteSaved : AddEditEvent
    data class Error(val message: String) : AddEditEvent
}
