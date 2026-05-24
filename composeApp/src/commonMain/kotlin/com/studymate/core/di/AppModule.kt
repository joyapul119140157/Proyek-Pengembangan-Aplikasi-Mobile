package com.studymate.core.di

import com.studymate.core.network.HttpClientFactory
import com.studymate.core.util.DatabaseDriverFactory
import com.studymate.data.local.NoteDatabase
import com.studymate.data.local.datastore.DataStoreFactory
import com.studymate.data.local.datastore.UserPreferences
import com.studymate.data.local.datastore.create
import com.studymate.data.remote.api.GeminiService
import com.studymate.data.repository.AIRepositoryImpl
import com.studymate.data.repository.NoteRepositoryImpl
import com.studymate.domain.repository.AIRepository
import com.studymate.domain.repository.NoteRepository
import com.studymate.domain.usecase.DeleteNoteUseCase
import com.studymate.domain.usecase.GenerateIdeasUseCase
import com.studymate.domain.usecase.GenerateQuizUseCase
import com.studymate.domain.usecase.GetAllNotesUseCase
import com.studymate.domain.usecase.ImproveWritingUseCase
import com.studymate.domain.usecase.SaveNoteUseCase
import com.studymate.domain.usecase.SearchNotesUseCase
import com.studymate.domain.usecase.SummarizeNoteUseCase
import com.studymate.presentation.screens.add.AddEditViewModel
import com.studymate.presentation.screens.ai.AIAssistantViewModel
import com.studymate.presentation.screens.detail.NoteDetailViewModel
import com.studymate.presentation.screens.home.HomeViewModel
import com.studymate.presentation.screens.notes.NotesViewModel
import com.studymate.presentation.screens.quiz.QuizViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

// ==================== NETWORK MODULE ====================

val networkModule = module {
    single { HttpClientFactory.create(enableLogging = true) }
    singleOf(::GeminiService)
}

// ==================== DATABASE MODULE ====================

val databaseModule = module {
    single {
        val driverFactory: DatabaseDriverFactory = get()
        NoteDatabase(driverFactory.createDriver())
    }
}

// ==================== PREFERENCES MODULE ====================

val preferencesModule = module {
    single { get<DataStoreFactory>().create() }
    single { UserPreferences(get()) }
}

// ==================== REPOSITORY MODULE ====================

val repositoryModule = module {
    singleOf(::NoteRepositoryImpl) bind NoteRepository::class
    singleOf(::AIRepositoryImpl) bind AIRepository::class
}

// ==================== USE CASE MODULE ====================

val useCaseModule = module {
    singleOf(::GetAllNotesUseCase)
    singleOf(::SearchNotesUseCase)
    singleOf(::SaveNoteUseCase)
    singleOf(::DeleteNoteUseCase)
    singleOf(::SummarizeNoteUseCase)
    singleOf(::ImproveWritingUseCase)
    singleOf(::GenerateIdeasUseCase)
    singleOf(::GenerateQuizUseCase)
}

// ==================== VIEWMODEL MODULE ====================

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::NotesViewModel)
    viewModelOf(::AddEditViewModel)
    viewModelOf(::NoteDetailViewModel)
    viewModelOf(::AIAssistantViewModel)
    viewModelOf(::QuizViewModel)
}

// ==================== SHARED MODULES ====================

val sharedModules = listOf(
    networkModule,
    databaseModule,
    preferencesModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)

// ==================== INIT FUNCTION ====================

fun initKoin(
    platformModules: List<Module> = emptyList(),
    config: KoinAppDeclaration? = null
) {
    startKoin {
        config?.invoke(this)
        modules(platformModules + sharedModules)
    }
}
