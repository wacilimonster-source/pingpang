package com.pingpang.app.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingpang.app.data.db.VideoClipDao
import com.pingpang.app.data.model.VideoClip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class VideoViewModel(dao: VideoClipDao) : ViewModel() {
    val clips: StateFlow<List<VideoClip>> =
        dao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
