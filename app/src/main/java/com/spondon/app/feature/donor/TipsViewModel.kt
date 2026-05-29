package com.spondon.app.feature.donor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.data.repository.TipsRepository
import com.spondon.app.core.domain.model.DonationTip
import com.spondon.app.core.domain.model.TipCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TipsState(
    val allTips: List<DonationTip> = emptyList(),
    val tipsByCategory: Map<TipCategory, List<DonationTip>> = emptyMap(),
    val tipOfTheDay: DonationTip? = null,
    val searchQuery: String = "",
    val filteredTips: Map<TipCategory, List<DonationTip>> = emptyMap(),
    val isLoading: Boolean = true,
    val tipDismissed: Boolean = false,
)

@HiltViewModel
class TipsViewModel @Inject constructor(
    private val tipsRepository: TipsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TipsState())
    val state: StateFlow<TipsState> = _state.asStateFlow()

    init {
        loadTips()
    }

    private fun loadTips() {
        viewModelScope.launch {
            val tips = tipsRepository.getAllTips()
            val grouped = tips.groupBy { it.category }
            val tipOfDay = tipsRepository.getTipOfTheDay()
            _state.update {
                it.copy(
                    allTips = tips,
                    tipsByCategory = grouped,
                    filteredTips = grouped,
                    tipOfTheDay = tipOfDay,
                    isLoading = false,
                )
            }
        }
    }

    fun searchTips(query: String) {
        _state.update { current ->
            val filtered = if (query.isBlank()) {
                current.tipsByCategory
            } else {
                current.tipsByCategory.mapValues { (_, tips) ->
                    tips.filter { tip ->
                        tip.title.contains(query, ignoreCase = true) ||
                                tip.titleBn.contains(query, ignoreCase = true) ||
                                tip.body.contains(query, ignoreCase = true) ||
                                tip.bodyBn.contains(query, ignoreCase = true)
                    }
                }.filterValues { it.isNotEmpty() }
            }
            current.copy(searchQuery = query, filteredTips = filtered)
        }
    }

    fun dismissTipOfTheDay() {
        _state.update { it.copy(tipDismissed = true) }
    }
}
