package com.boxing.analysis.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxing.analysis.domain.model.AnalysisState
import com.boxing.analysis.domain.model.SessionResults
import com.boxing.analysis.domain.usecase.GetSessionResultsUseCase
import com.boxing.analysis.domain.usecase.PollAnalysisStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionDetailState {
    data object Loading : SessionDetailState
    data class  Polling(val status: String) : SessionDetailState
    data class  Ready(val results: SessionResults) : SessionDetailState
    data class  Error(val message: String) : SessionDetailState
}

class SessionDetailViewModel(
    private val sessionId: String,
    private val getResults: GetSessionResultsUseCase = TODO("inject"),
    private val pollStatus: PollAnalysisStatusUseCase = TODO("inject"),
) : ViewModel() {

    private val _state = MutableStateFlow<SessionDetailState>(SessionDetailState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            runCatching {
                // Try results first (session may already be COMPLETE)
                getResults(sessionId)
            }.onSuccess { results ->
                _state.value = SessionDetailState.Ready(results)
            }.onFailure {
                // Results not ready — start polling
                _state.value = SessionDetailState.Polling("PROCESSING")
                val finalState = pollStatus(sessionId) { status ->
                    _state.value = SessionDetailState.Polling(status)
                }
                if (finalState == AnalysisState.COMPLETE) {
                    runCatching { getResults(sessionId) }
                        .onSuccess { _state.value = SessionDetailState.Ready(it) }
                        .onFailure { e -> _state.value = SessionDetailState.Error(e.message ?: "Failed to load") }
                } else {
                    _state.value = SessionDetailState.Error("Analysis failed. Please try again.")
                }
            }
        }
    }
}
