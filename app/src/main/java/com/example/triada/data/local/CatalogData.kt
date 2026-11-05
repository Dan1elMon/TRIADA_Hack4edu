package com.example.triada.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(@PrimaryKey val id: String = "main", val schemaVersion: Int, val catalogVersion: Int, val checksum: String)

@Entity(tableName = "grades", indices = [Index(value = ["number"], unique = true)])
data class GradeEntity(@PrimaryKey val id: String, val number: Int, val label: String, val sortOrder: Int)

@Entity(
    tableName = "subjects",
    foreignKeys = [ForeignKey(entity = GradeEntity::class, parentColumns = ["id"], childColumns = ["gradeId"])],
    indices = [Index(value = ["gradeId", "code"], unique = true), Index(value = ["gradeId", "sortOrder"], unique = true)],
)
data class SubjectEntity(@PrimaryKey val id: String, val gradeId: String, val code: String, val title: String, val iconKey: String, val sortOrder: Int, val referenceCount: Int)

@Entity(
    tableName = "learning_units",
    foreignKeys = [ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"])],
    indices = [Index(value = ["subjectId", "sortOrder"], unique = true)],
)
data class LearningUnitEntity(@PrimaryKey val id: String, val subjectId: String, val title: String, val description: String, val sortOrder: Int)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"]),
        ForeignKey(entity = LearningUnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"]),
    ],
    indices = [Index(value = ["subjectId", "sourceOrder"], unique = true), Index("unitId")],
)
data class TopicEntity(@PrimaryKey val id: String, val subjectId: String, val unitId: String?, val title: String, val sourceOrder: Int, val contentStatus: String)

@Entity(
    tableName = "catalog_lessons", primaryKeys = ["lessonId", "contentVersion"],
    foreignKeys = [
        ForeignKey(entity = LessonEntity::class, parentColumns = ["id", "version"], childColumns = ["lessonId", "contentVersion"]),
        ForeignKey(entity = TopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"]),
    ], indices = [Index("topicId")],
)
data class CatalogLessonEntity(val lessonId: String, val contentVersion: Int, val topicId: String)

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_metadata WHERE id = 'main'") suspend fun metadata(): CatalogMetadataEntity?
    @Query("SELECT * FROM grades ORDER BY sortOrder, id") suspend fun grades(): List<GradeEntity>
    @Query("SELECT * FROM subjects ORDER BY sortOrder, id") suspend fun subjects(): List<SubjectEntity>
    @Query("SELECT * FROM learning_units ORDER BY sortOrder, id") suspend fun units(): List<LearningUnitEntity>
    @Query("SELECT * FROM topics ORDER BY subjectId, sourceOrder") suspend fun topics(): List<TopicEntity>
    @Query("SELECT * FROM topics WHERE id = :id") suspend fun topic(id: String): TopicEntity?
    @Query("SELECT * FROM catalog_lessons ORDER BY lessonId, contentVersion DESC") suspend fun lessons(): List<CatalogLessonEntity>
    @Query("SELECT * FROM catalog_lessons WHERE lessonId = :lessonId ORDER BY contentVersion DESC LIMIT 1") suspend fun lesson(lessonId: String): CatalogLessonEntity?
    @Upsert suspend fun saveMetadata(row: CatalogMetadataEntity)
    @Upsert suspend fun saveGrades(rows: List<GradeEntity>)
    @Upsert suspend fun saveSubjects(rows: List<SubjectEntity>)
    @Upsert suspend fun saveUnits(rows: List<LearningUnitEntity>)
    @Upsert suspend fun saveTopics(rows: List<TopicEntity>)
    @Upsert suspend fun saveLessons(rows: List<CatalogLessonEntity>)
}
