package com.studymate.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    
    @Serializable
    data object Home : Route
    
    @Serializable
    data object Notes : Route
    
    @Serializable
    data class Quiz(val content: String? = null) : Route
    
    @Serializable
    data object Calendar : Route
    
    @Serializable
    data object Profile : Route
    
    @Serializable
    data class AddNote(val noteId: Long? = null) : Route
    
    @Serializable
    data class NoteDetail(val noteId: Long) : Route
    
    @Serializable
    data class AIAssistant(
        val noteId: Long? = null,
        val initialText: String? = null
    ) : Route
}

interface NavigationActions {
    fun navigateToHome()
    fun navigateToNotes()
    fun navigateToQuiz(content: String? = null)
    fun navigateToCalendar()
    fun navigateToProfile()
    fun navigateToAddNote(noteId: Long? = null)
    fun navigateToNoteDetail(noteId: Long)
    fun navigateToAIAssistant(noteId: Long? = null, initialText: String? = null)
    fun navigateBack()
}
