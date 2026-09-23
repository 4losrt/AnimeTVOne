package com.one.animetvone.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.one.animetvone.data.model.ScheduleDay
import com.one.animetvone.data.repository.AnimeRepository
import com.one.animetvone.data.repository.AnimeRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val scheduleDays: List<ScheduleDay> = emptyList(),
    val selectedDayName: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ScheduleViewModel(
    private val repository: AnimeRepository = AnimeRepositoryProvider.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            repository.getSchedule()
                .onStart {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Failed to load schedule"
                        )
                    }
                }
                .collect { days ->
                    _uiState.update {
                        it.copy(
                            scheduleDays = days,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun selectDay(dayName: String?) {
        _uiState.update { it.copy(selectedDayName = dayName) }
    }
}
