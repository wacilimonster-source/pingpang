package com.pingpang.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingpang.app.data.db.TrainingSessionDao
import com.pingpang.app.data.model.TrainingSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(dao: TrainingSessionDao) : ViewModel() {
    val sessions: StateFlow<List<TrainingSession>> =
        dao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
