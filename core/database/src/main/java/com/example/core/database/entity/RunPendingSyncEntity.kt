package com.example.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.bson.types.ObjectId

@Entity
data class RunPendingSyncEntity(
    @Embedded val runEntity: RunEntity,
    @PrimaryKey(autoGenerate = false) val runId: String = runEntity.id,
    val mapPictureByteArray: ByteArray,
    val userId: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RunPendingSyncEntity

        if (runEntity != other.runEntity) return false
        if (runId != other.runId) return false
        if (!mapPictureByteArray.contentEquals(other.mapPictureByteArray)) return false
        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = runEntity.hashCode()
        result = 31 * result + runId.hashCode()
        result = 31 * result + mapPictureByteArray.contentHashCode()
        result = 31 * result + userId.hashCode()
        return result
    }
}
