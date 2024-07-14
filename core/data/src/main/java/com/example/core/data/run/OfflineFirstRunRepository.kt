package com.example.core.data.run

import com.example.core.data.networking.get
import com.example.core.database.dao.RunPendingSyncDao
import com.example.core.database.mapper.toRun
import com.example.core.domain.SessionStorage
import com.example.core.domain.run.LocalRunDataSource
import com.example.core.domain.run.RemoteRunDataSource
import com.example.core.domain.run.Run
import com.example.core.domain.run.RunId
import com.example.core.domain.run.RunRepository
import com.example.core.domain.run.SyncRunScheduler
import com.example.core.domain.util.DataError
import com.example.core.domain.util.EmptyResult
import com.example.core.domain.util.Result
import com.example.core.domain.util.asEmptyDataResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OfflineFirstRunRepository(
    private val localRunDataSource: LocalRunDataSource,
    private val remoteRunDataSource: RemoteRunDataSource,
    private val applicationScope: CoroutineScope,
    private val runPendingSyncDao: RunPendingSyncDao,
    private val sessionStorage: SessionStorage,
    private val syncRunScheduler: SyncRunScheduler,
    private val httpClient: HttpClient
) : RunRepository {

    override fun getRuns(): Flow<List<Run>> {
        return localRunDataSource.getRuns() // Always get runs from local database as the single source of truth
    }

    override suspend fun fetchRuns(): EmptyResult<DataError> {
        return when (val result = remoteRunDataSource.getRuns()) {
            is Result.Error -> result.asEmptyDataResult()
            is Result.Success -> {
                applicationScope.async { // Assure that a CancellationCoroutineException will not produce an inconsistency between remote and local
                    localRunDataSource.upsertRuns(result.data).asEmptyDataResult() // The local upsertRuns will trigger the local getRuns flow and will update data to view from local database
                }.await()
            }
        }
    }

    override suspend fun upsertRun(run: Run, mapPictureByteArray: ByteArray): EmptyResult<DataError> {
        val localResult = localRunDataSource.upsertRun(run)
        if (localResult !is Result.Success) {
            return localResult.asEmptyDataResult() // Disk is full, we cannot store run to local database
        }

        val runWithId = run.copy(id = localResult.data)
        val remoteResult = remoteRunDataSource.postRun(run = runWithId, mapPicture = mapPictureByteArray)
        return when (remoteResult) {
            is Result.Error -> {
                applicationScope.launch {
                    syncRunScheduler.scheduleSync(SyncRunScheduler.SyncType.CreateRun(run = runWithId, mapPictureByteArray = mapPictureByteArray))
                }.join()
                Result.Success(Unit)
            }
            is Result.Success -> {
                applicationScope.async { // Assure that a CancellationCoroutineException will not produce an inconsistency between remote and local
                    localRunDataSource.upsertRun(remoteResult.data).asEmptyDataResult()
                }.await()
            }
        }
    }

    override suspend fun deleteRun(id: RunId) {
        localRunDataSource.deleteRun(id)

        // Edge case where the run is created in offline mode and then deleted in offline mode without being sync in between
        val isPendingSync = runPendingSyncDao.getRunPendingSyncEntity(id) != null
        if (isPendingSync) {
            runPendingSyncDao.deleteRunPendingSyncEntity(id)
            return
        }

        val remoteResult = applicationScope.async { // Assure that a CancellationCoroutineException will not produce an inconsistency between remote and local
            remoteRunDataSource.deleteRun(id)
        }.await()

        if (remoteResult is Result.Error) {
            applicationScope.launch {
                syncRunScheduler.scheduleSync(SyncRunScheduler.SyncType.DeleteRun(runId = id))
            }.join()
        }
    }

    override suspend fun deleteAllRuns() {
        localRunDataSource.deleteAllRuns()
    }

    override suspend fun syncPendingRuns() {
        withContext(Dispatchers.IO) {
            val userId = sessionStorage.get()?.userId ?: return@withContext

            val createdRuns = async { runPendingSyncDao.getAllRunPendingSyncEntities(userId) }
            val deletedRuns = async { runPendingSyncDao.getAllDeletedRunSyncEntities(userId) }

            val createJobs = createdRuns.await().map {
                launch {
                    val run = it.runEntity.toRun()
                    when (remoteRunDataSource.postRun(run, it.mapPictureByteArray)) {
                        is Result.Error -> Unit
                        is Result.Success -> {
                            applicationScope.launch {
                                runPendingSyncDao.deleteRunPendingSyncEntity(it.runId)
                            }.join()
                        }
                    }
                }
            }
            val deletedJobs = deletedRuns.await().map {
                launch {
                    when (remoteRunDataSource.deleteRun(it.runId)) {
                        is Result.Error -> Unit
                        is Result.Success -> {
                            applicationScope.launch {
                                runPendingSyncDao.deleteDeletedRunSyncEntity(it.runId)
                            }.join()
                        }
                    }
                }
            }

            createJobs.forEach { it.join() }
            deletedJobs.forEach { it.join() }
        }
    }

    override suspend fun logout(): EmptyResult<DataError.Network> {
        val result = httpClient.get<Unit>(
            route = "/logout"
        ).asEmptyDataResult()

        httpClient.plugin(Auth).providers.filterIsInstance<BearerAuthProvider>()
            .firstOrNull()
            ?.clearToken() // When logging out, to delete the token stored locally in ktor, we need to clear it

        return result
    }

}