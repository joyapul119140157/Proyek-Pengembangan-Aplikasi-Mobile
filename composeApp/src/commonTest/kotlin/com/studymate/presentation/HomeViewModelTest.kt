package com.studymate.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.studymate.data.local.datastore.UserPreferences
import com.studymate.data.repository.FakeNoteRepository
import com.studymate.domain.model.Note
import com.studymate.domain.model.NoteCategory
import com.studymate.domain.model.NoteColor
import com.studymate.domain.repository.NoteRepository
import com.studymate.domain.usecase.DeleteNoteUseCase
import com.studymate.domain.usecase.GetAllNotesUseCase
import com.studymate.domain.usecase.NoteSortBy
import com.studymate.domain.usecase.SearchNotesUseCase
import com.studymate.presentation.screens.home.HomeUiState
import com.studymate.presentation.screens.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit Tests untuk HomeViewModel
 * 
 * Testing Guidelines:
 * 1. Setup test dispatcher untuk control coroutines
 * 2. Gunakan Turbine untuk test StateFlow
 * 3. Test UI state transformations
 * 4. Test user actions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var repository: FakeNoteRepository
    private lateinit var getAllNotesUseCase: GetAllNotesUseCase
    private lateinit var searchNotesUseCase: SearchNotesUseCase
    private lateinit var deleteNoteUseCase: DeleteNoteUseCase
    private lateinit var userPreferences: UserPreferences
    private lateinit var viewModel: HomeViewModel
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        repository = FakeNoteRepository()
        getAllNotesUseCase = GetAllNotesUseCase(repository)
        searchNotesUseCase = SearchNotesUseCase(repository)
        deleteNoteUseCase = DeleteNoteUseCase(repository)
        
        // Mock DataStore untuk UserPreferences
        val fakeDataStore = object : DataStore<Preferences> {
            private val _data = MutableStateFlow(emptyPreferences())
            override val data: Flow<Preferences> = _data
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                val current = _data.value
                val new = transform(current)
                _data.value = new
                return new
            }
        }
        userPreferences = UserPreferences(fakeDataStore)
        
        viewModel = HomeViewModel(
            getAllNotesUseCase = getAllNotesUseCase,
            searchNotesUseCase = searchNotesUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            repository = repository,
            userPreferences = userPreferences
        )
    }
    
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ==================== UI STATE TESTS ====================
    
    @Test
    fun `initial state should be Loading then Empty`() = runTest {
        viewModel.uiState.test {
            // Initial loading state (dari initialValue di stateIn)
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            
            // Advance time untuk melewati debounce(300) di HomeViewModel
            testScheduler.advanceTimeBy(400)
            advanceUntilIdle()
            
            // Setelah loading dan combine, state harus tidak loading dan kosong
            val state = awaitItem()
            assertTrue(!state.isLoading)
            assertTrue(state.notes.isEmpty())
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `state should show notes when they exist`() = runTest {
        // Arrange
        repository.insertNote(createTestNote("Note 1"))
        repository.insertNote(createTestNote("Note 2"))
        
        // Create new viewmodel after inserting notes
        val vm = HomeViewModel(
            getAllNotesUseCase = getAllNotesUseCase,
            searchNotesUseCase = searchNotesUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            repository = repository,
            userPreferences = userPreferences
        )
        
        // Act & Assert
        vm.uiState.test {
            skipItems(1) // Skip initial loading
            
            testScheduler.advanceTimeBy(400)
            advanceUntilIdle()
            
            val state = awaitItem()
            assertTrue(!state.isLoading)
            assertEquals(2, state.notes.size)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== SEARCH TESTS ====================
    
    @Test
    fun `search should filter notes by query`() = runTest {
        // Arrange
        repository.insertNote(createTestNote("Kotlin Guide"))
        repository.insertNote(createTestNote("Java Tutorial"))
        
        val vm = HomeViewModel(
            getAllNotesUseCase = getAllNotesUseCase,
            searchNotesUseCase = searchNotesUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            repository = repository,
            userPreferences = userPreferences
        )
        
        vm.uiState.test {
            skipItems(1) // Skip loading
            testScheduler.advanceTimeBy(400)
            advanceUntilIdle()
            skipItems(1) // Skip initial empty results
            
            // Act
            vm.onSearchQueryChange("Kotlin")
            
            // Assert - wait for debounce
            testScheduler.advanceTimeBy(400)
            advanceUntilIdle()
            
            val state = expectMostRecentItem()
            assertEquals(1, state.notes.size)
            assertEquals("Kotlin Guide", state.notes.first().title)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `clearSearch should reset query`() = runTest {
        // Act
        viewModel.onSearchQueryChange("test query")
        viewModel.clearSearch()
        
        // Assert
        viewModel.uiState.test {
            testScheduler.advanceTimeBy(400)
            advanceUntilIdle()
            
            val state = expectMostRecentItem()
            assertEquals("", state.query)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== CATEGORY FILTER TESTS ====================
    
    @Test
    fun `category filter should filter notes`() = runTest {
        // Arrange
        repository.insertNote(createTestNote("Work Note", category = NoteCategory.WORK))
        repository.insertNote(createTestNote("Personal Note", category = NoteCategory.PERSONAL))
        
        val vm = HomeViewModel(
            getAllNotesUseCase = getAllNotesUseCase,
            searchNotesUseCase = searchNotesUseCase,
            deleteNoteUseCase = deleteNoteUseCase,
            repository = repository,
            userPreferences = userPreferences
        )
        
        vm.uiState.test {
            skipItems(1) // Loading
            testScheduler.advanceTimeBy(400)
            advanceUntilIdle()
            skipItems(1) // Initial success
            
            // Act
            vm.onCategorySelected(NoteCategory.WORK)
            
            testScheduler.advanceTimeBy(400)
            advanceUntilIdle()
            
            // Assert
            val state = expectMostRecentItem()
            assertEquals(1, state.notes.size)
            assertEquals(NoteCategory.WORK, state.notes.first().category)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== ACTION TESTS ====================
    
    @Test
    fun `togglePin should toggle note pin status`() = runTest {
        // Arrange
        val noteId = repository.insertNote(createTestNote("Pin Me"))
        
        // Act
        viewModel.togglePin(noteId)
        advanceUntilIdle()
        
        // Assert
        repository.getNoteById(noteId).test {
            val note = awaitItem()
            assertTrue(note?.isPinned == true)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `deleteNote should remove note`() = runTest {
        // Arrange
        val noteId = repository.insertNote(createTestNote("Delete Me"))
        
        // Act
        viewModel.deleteNote(noteId)
        advanceUntilIdle()
        
        // Assert
        repository.getAllNotes().test {
            val notes = awaitItem()
            assertTrue(notes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== HELPER FUNCTIONS ====================
    
    private fun createTestNote(
        title: String,
        category: NoteCategory = NoteCategory.GENERAL
    ): Note {
        return Note(
            id = 0,
            title = title,
            content = "Test content",
            category = category,
            color = NoteColor.DEFAULT,
            isPinned = false,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
}
