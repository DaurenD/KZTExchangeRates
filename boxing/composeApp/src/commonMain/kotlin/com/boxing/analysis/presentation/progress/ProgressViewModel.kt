package com.boxing.analysis.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxing.analysis.domain.model.ProgressPoint
import com.boxing.analysis.domain.usecase.GetProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgressViewModel(private val getProgress: GetProgressUseCase) : ViewModel() {
    private val _points    = MutableStateFlow<List<ProgressPoint>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val points    = _points.asStateFlow()
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { getProgress() }.onSuccess { _points.value = it }
            _isLoading.value = false
        }
    }
}
