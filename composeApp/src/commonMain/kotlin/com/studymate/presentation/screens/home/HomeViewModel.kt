package com.studymate.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.data.local.datastore.UserPreferences
import com.studymate.domain.model.Note
import com.studymate.domain.model.NoteCategory
import com.studymate.domain.repository.NoteRepository
import com.studymate.domain.usecase.DeleteNoteUseCase
import com.studymate.domain.usecase.GetAllNotesUseCase
import com.studymate.domain.usecase.NoteSortBy
import com.studymate.domain.usecase.SearchNotesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val repository: NoteRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<NoteCategory?>(null)
    private val _sortBy = MutableStateFlow(NoteSortBy.UPDATED_DESC)
    private val _isLoading = MutableStateFlow(false)
    
    private val debouncedSearchQuery = _searchQuery.debounce(300)
    
    val sortBy: StateFlow<NoteSortBy> = _sortBy
    
    val uiState: StateFlow<HomeUiState> = combine(
        debouncedSearchQuery,
        _selectedCategory,
        _sortBy,
        userPreferences.dailyMantra,
        userPreferences.streakCount
    ) { query, category, sortBy, mantra, streak ->
        DataParams(query, category, sortBy, mantra, streak)
    }.flatMapLatest { params ->
        val notesFlow = if (params.query.isBlank() && params.category == null) {
            getAllNotesUseCase(params.sortBy)
        } else {
            searchNotesUseCase(params.query, params.category)
        }
        
        notesFlow.combine(_isLoading) { notes, isLoading ->
            HomeUiState(
                notes = notes,
                query = _searchQuery.value,
                category = _selectedCategory.value,
                sortBy = _sortBy.value,
                isLoading = isLoading,
                dailyMantra = params.mantra,
                streakCount = params.streak
            )
        }
    }.catch { e ->
        emit(HomeUiState(error = e.message ?: "Terjadi kesalahan"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )
    
    private data class DataParams(
        val query: String,
        val category: NoteCategory?,
        val sortBy: NoteSortBy,
        val mantra: String,
        val streak: Int
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    fun onCategorySelected(category: NoteCategory?) {
        _selectedCategory.value = category
    }
    
    fun onSortByChanged(sortBy: NoteSortBy) {
        _sortBy.value = sortBy
    }
    
    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            repository.togglePinNote(noteId)
        }
    }
    
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            deleteNoteUseCase(noteId)
        }
    }

    fun updateMantra(mantra: String) {
        viewModelScope.launch {
            userPreferences.setDailyMantra(mantra)
        }
    }
}
