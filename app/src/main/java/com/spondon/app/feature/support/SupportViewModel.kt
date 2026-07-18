package com.spondon.app.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.common.Resource
import com.spondon.app.feature.superadmin.data.SARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportScreenState(
    val developerInfo: SARepository.DeveloperInfo = SARepository.DeveloperInfo(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SupportScreenState())
    val state: StateFlow<SupportScreenState> = _state.asStateFlow()

    init {
        loadDeveloperInfo()
    }

    private fun loadDeveloperInfo() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getDeveloperInfo()) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            developerInfo = result.data ?: SARepository.DeveloperInfo(),
                            isLoading = false,
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                        )
                    }
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }
}
