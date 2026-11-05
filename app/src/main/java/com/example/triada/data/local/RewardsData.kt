package com.example.triada.data.local

import androidx.room.*

@Entity(tableName = "earned_stickers", primaryKeys = ["profileId", "lessonId"],
    foreignKeys = [ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"])],
    indices = [Index(value = ["profileId", "stickerId"], unique = true)])
data class EarnedStickerEntity(val profileId: String, val lessonId: String, val stickerId: String, val obtainedAt: Long)

@Dao
interface RewardsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun award(row: EarnedStickerEntity)
    @Query("SELECT * FROM earned_stickers WHERE profileId = :profileId ORDER BY obtainedAt, stickerId")
    suspend fun earned(profileId: String): List<EarnedStickerEntity>
}
