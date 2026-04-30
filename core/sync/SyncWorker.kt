package com.laprevia.restobar.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.laprevia.restobar.core.utils.NetworkUtils
import com.laprevia.restobar.data.repository.*
import com.laprevia.restobar.domain.repository.*
import dagger.hilt.android.EntryPointAccessors

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val appContext = applicationContext

        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            SyncEntryPoint::class.java
        )

        val networkUtils = NetworkUtils(appContext)

        // ❌ sin internet no hace nada
        if (!networkUtils.isOnline()) {
            return Result.retry()
        }

        val syncManager = entryPoint.syncManager()

        syncManager.syncAll()

        return Result.success()
    }
}