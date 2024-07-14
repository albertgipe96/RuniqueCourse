package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.core.database.entity.DeletedRunSyncEntity
import com.example.core.database.entity.RunPendingSyncEntity

@Dao
interface RunPendingSyncDao {

    // CREATED RUNS section starts -----------------------------------------------------------------

    @Query("SELECT * FROM runpendingsyncentity WHERE userId=:userId")
    suspend fun getAllRunPendingSyncEntities(userId: String): List<RunPendingSyncEntity>

    @Query("SELECT * FROM runpendingsyncentity WHERE runId=:runId")
    suspend fun getRunPendingSyncEntity(runId: String): RunPendingSyncEntity?

    @Upsert
    suspend fun upsertRunPendingSyncEntity(runPendingSyncEntity: RunPendingSyncEntity)

    @Query("DELETE FROM runpendingsyncentity WHERE runId=:runId")
    suspend fun deleteRunPendingSyncEntity(runId: String)

    // CREATED RUNS section ends -------------------------------------------------------------------

    // DELETED RUNS section starts -----------------------------------------------------------------

    @Query("SELECT * FROM deletedrunsyncentity WHERE userId=:userId")
    suspend fun getAllDeletedRunSyncEntities(userId: String): List<DeletedRunSyncEntity>

    @Upsert
    suspend fun upsertDeletedRunSyncEntity(deletedRunSyncEntity: DeletedRunSyncEntity)

    @Query("DELETE FROM deletedrunsyncentity WHERE runId=:runId")
    suspend fun deleteDeletedRunSyncEntity(runId: String)

    // DELETED RUNS section ends -------------------------------------------------------------------
}