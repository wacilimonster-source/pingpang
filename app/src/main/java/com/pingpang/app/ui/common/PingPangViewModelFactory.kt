package com.pingpang.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.pingpang.app.data.db.StagePlanDao
import com.pingpang.app.data.db.TrainingSessionDao
import com.pingpang.app.data.db.VideoClipDao
import com.pingpang.app.ui.home.HomeViewModel
import com.pingpang.app.ui.plan.PlanViewModel
import com.pingpang.app.ui.video.VideoViewModel

/** 极简 ViewModel 工厂：按需把 Room DAO 注入对应 ViewModel */
class PingPangViewModelFactory(
    private val trainingDao: TrainingSessionDao? = null,
    private val stagePlanDao: StagePlanDao? = null,
    private val videoClipDao: VideoClipDao? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) && trainingDao != null ->
                HomeViewModel(trainingDao) as T
            modelClass.isAssignableFrom(PlanViewModel::class.java) && stagePlanDao != null ->
                PlanViewModel(stagePlanDao) as T
            modelClass.isAssignableFrom(VideoViewModel::class.java) && videoClipDao != null ->
                VideoViewModel(videoClipDao) as T
            else -> error("未知 ViewModel: ${modelClass.name}")
        }
}
