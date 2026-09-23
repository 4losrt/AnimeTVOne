package com.one.animetvone.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import com.one.animetvone.data.model.HistoryItem
import com.one.animetvone.data.source.HistoryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HistoryUiState(
    val historyItems: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = false
)

class HistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadHistory(context: Context) {
        val list = HistoryManager.getHistoryList(context)
        _uiState.update { it.copy(historyItems = list) }
    }

    fun removeHistoryItem(context: Context, episodeId: String) {
        HistoryManager.removeHistoryItem(context, episodeId)
        loadHistory(context)
    }

    fun clearHistory(context: Context) {
        HistoryManager.clearHistory(context)
        _uiState.update { it.copy(historyItems = emptyList()) }
    }
}
