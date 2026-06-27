package com.boxing.analysis.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxing.analysis.domain.model.SessionSummary
import com.boxing.analysis.domain.usecase.GetSessionHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getHistory: GetSessionHistoryUseCase,
) : ViewModel() {

    private val _sessions  = MutableStateFlow<List<SessionSummary>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val sessions  = _sessions.asStateFlow()
    val isLoading = _isLoading.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { getHistory() }
                .onSuccess { _sessions.value = it }
            _isLoading.value = false
        }
    }
}
