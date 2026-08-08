package com.pingpang.app

import android.app.Application
import com.pingpang.app.data.db.AppDatabase

class PingPangApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
    }
}
