package com.studymate.presentation.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studymate.domain.model.Question
import com.studymate.domain.usecase.GenerateQuizUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuizViewModel(
    private val generateQuizUseCase: GenerateQuizUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun generateQuiz(content: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            generateQuizUseCase(content)
                .onSuccess { questions ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            questions = questions,
                            currentQuestionIndex = 0
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Gagal membuat kuis"
                        )
                    }
                }
        }
    }

    fun selectOption(index: Int) {
        if (_uiState.value.isAnswered) return

        _uiState.update {
            it.copy(
                selectedOptionIndex = index,
                isAnswered = true
            )
        }
    }

    fun nextQuestion() {
        val nextIndex = _uiState.value.currentQuestionIndex + 1
        if (nextIndex < _uiState.value.questions.size) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    selectedOptionIndex = null,
                    isAnswered = false
                )
            }
        } else {
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun resetQuiz() {
        _uiState.update {
            it.copy(
                currentQuestionIndex = 0,
                selectedOptionIndex = null,
                isAnswered = false,
                isFinished = false
            )
        }
    }
}

data class QuizUiState(
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswered: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFinished: Boolean = false
) {
    val currentQuestion: Question?
        get() = questions.getOrNull(currentQuestionIndex)
    
    val score: Int
        get() = 0 // Implementasi score jika dibutuhkan
}
