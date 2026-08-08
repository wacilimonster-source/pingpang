package com.pingpang.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingpang.app.data.db.StagePlanDao
import com.pingpang.app.data.model.StagePlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlanViewModel(dao: StagePlanDao) : ViewModel() {
    val plans: StateFlow<List<StagePlan>> =
        dao.observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
