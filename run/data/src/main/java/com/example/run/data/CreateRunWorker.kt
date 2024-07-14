package com.example.run.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.database.dao.RunPendingSyncDao
import com.example.core.database.mapper.toRun
import com.example.core.domain.run.RemoteRunDataSource
import com.example.core.domain.util.DataError
import com.example.core.domain.util.Result

class CreateRunWorker(
    context: Context,
    private val params: WorkerParameters,
    private val remoteRunDataSource: RemoteRunDataSource,
    private val runPendingSyncDao: RunPendingSyncDao
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (runAttemptCount >= 5) return Result.failure()
        val pendingRunId = params.inputData.getString(RUN_ID) ?: return Result.failure()
        val pendingRunEntity = runPendingSyncDao.getRunPendingSyncEntity(pendingRunId) ?: return Result.failure()
        val run = pendingRunEntity.runEntity.toRun()
        return when (val result = remoteRunDataSource.postRun(run, pendingRunEntity.mapPictureByteArray)) {
            is com.example.core.domain.util.Result.Error -> {
                result.error.toWorkerResult()
            }
            is com.example.core.domain.util.Result.Success -> {
                runPendingSyncDao.deleteRunPendingSyncEntity(pendingRunId)
                Result.success()
            }
        }
    }

    companion object {
        const val RUN_ID = "RUN_ID"
    }

}