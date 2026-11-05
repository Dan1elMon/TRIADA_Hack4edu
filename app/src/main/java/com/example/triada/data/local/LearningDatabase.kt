package com.example.triada.data.local

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class ProfileEntity(@PrimaryKey val id: String, val displayName: String, val avatar: String, val createdAt: Long)

@Entity(tableName = "lessons", primaryKeys = ["id", "version"])
data class LessonEntity(val id: String, val version: Int, val title: String, val objective: String)

@Entity(
    tableName = "questions", primaryKeys = ["id", "version"],
    foreignKeys = [ForeignKey(entity = LessonEntity::class, parentColumns = ["id", "version"], childColumns = ["lessonId", "version"])],
    indices = [Index(value = ["lessonId", "version", "sortOrder"], unique = true)],
)
data class QuestionEntity(
    val id: String, val version: Int, val lessonId: String, val sortOrder: Int, val prompt: String, val objectCount: Int,
    @ColumnInfo(defaultValue = "'COUNT_OBJECTS'") val activityType: String = "COUNT_OBJECTS",
    @ColumnInfo(defaultValue = "'{}'") val payloadJson: String = "{}",
    @ColumnInfo(defaultValue = "'{}'") val answerKeyJson: String = "{}",
)

@Entity(
    tableName = "options", primaryKeys = ["questionId", "version", "value"],
    foreignKeys = [ForeignKey(entity = QuestionEntity::class, parentColumns = ["id", "version"], childColumns = ["questionId", "version"])],
)
data class OptionEntity(val questionId: String, val version: Int, val value: Int, val sortOrder: Int)

@Entity(
    tableName = "hints", primaryKeys = ["questionId", "version", "sortOrder"],
    foreignKeys = [ForeignKey(entity = QuestionEntity::class, parentColumns = ["id", "version"], childColumns = ["questionId", "version"])],
)
data class HintEntity(val questionId: String, val version: Int, val sortOrder: Int, val text: String)

@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"]),
        ForeignKey(entity = LessonEntity::class, parentColumns = ["id", "version"], childColumns = ["lessonId", "version"]),
    ],
    indices = [Index("profileId"), Index(value = ["lessonId", "version"])],
)
data class AttemptEntity(
    @PrimaryKey val id: String, val profileId: String, val lessonId: String, val version: Int,
    val currentIndex: Int, val startedAt: Long, val completedAt: Long?,
)

@Entity(
    tableName = "question_state", primaryKeys = ["attemptId", "questionId", "version"],
    foreignKeys = [
        ForeignKey(entity = AttemptEntity::class, parentColumns = ["id"], childColumns = ["attemptId"]),
        ForeignKey(entity = QuestionEntity::class, parentColumns = ["id", "version"], childColumns = ["questionId", "version"]),
    ], indices = [Index(value = ["questionId", "version"])],
)
data class QuestionStateEntity(val attemptId: String, val questionId: String, val version: Int, val solved: Boolean, val hintsUsed: Int)

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(entity = AttemptEntity::class, parentColumns = ["id"], childColumns = ["attemptId"]),
        ForeignKey(entity = QuestionEntity::class, parentColumns = ["id", "version"], childColumns = ["questionId", "version"]),
    ], indices = [Index("attemptId"), Index(value = ["questionId", "version"])],
)
data class AnswerEntity(
    @PrimaryKey val eventId: String, val attemptId: String, val questionId: String, val version: Int,
    val choice: Int, val correct: Boolean, val hintsUsed: Int, val answeredAt: Long,
    @ColumnInfo(defaultValue = "''") val answerPayloadJson: String = "",
)

data class ProgressSummary(val solvedQuestions: Int, val completed: Boolean, val started: Boolean)

@Dao
interface LearningDao {
    @Query("SELECT * FROM attempts WHERE profileId = :profileId ORDER BY startedAt, id LIMIT 10001")
    suspend fun profileAttempts(profileId: String): List<AttemptEntity>
    @Query("SELECT COUNT(*) FROM answers JOIN attempts ON answers.attemptId = attempts.id WHERE attempts.profileId = :profileId")
    suspend fun profileAnswerCount(profileId: String): Long
    @Query("SELECT * FROM profiles ORDER BY createdAt, id") fun profiles(): Flow<List<ProfileEntity>>
    @Query("SELECT * FROM profiles WHERE id = :id") suspend fun profile(id: String): ProfileEntity?
    @Insert suspend fun insertProfile(profile: ProfileEntity)
    @Query("UPDATE profiles SET displayName = :name, avatar = :avatar WHERE id = :id")
    suspend fun updateProfile(id: String, name: String, avatar: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertLesson(lesson: LessonEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertQuestions(questions: List<QuestionEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertOptions(options: List<OptionEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertHints(hints: List<HintEntity>)
    @Query("SELECT * FROM lessons WHERE id = :id AND version = :version") suspend fun lesson(id: String, version: Int): LessonEntity?
    @Query("SELECT * FROM questions WHERE lessonId = :lessonId AND version = :version ORDER BY sortOrder")
    suspend fun questions(lessonId: String, version: Int): List<QuestionEntity>
    @Query("SELECT * FROM questions WHERE id = :id AND version = :version")
    suspend fun question(id: String, version: Int): QuestionEntity?
    @Query("SELECT * FROM options WHERE questionId = :questionId AND version = :version ORDER BY sortOrder")
    suspend fun options(questionId: String, version: Int): List<OptionEntity>
    @Query("SELECT * FROM hints WHERE questionId = :questionId AND version = :version ORDER BY sortOrder")
    suspend fun hints(questionId: String, version: Int): List<HintEntity>
    @Insert suspend fun insertAttempt(attempt: AttemptEntity)
    @Query("UPDATE attempts SET currentIndex = :index, completedAt = :completedAt WHERE id = :id")
    suspend fun restorePosition(id: String, index: Int, completedAt: Long?)
    @Query("SELECT * FROM attempts WHERE id = :id") suspend fun attempt(id: String): AttemptEntity?
    @Query("SELECT * FROM attempts WHERE profileId = :profileId AND lessonId = :lessonId AND version = :version ORDER BY startedAt DESC, rowid DESC LIMIT 1")
    suspend fun latestAttempt(profileId: String, lessonId: String, version: Int): AttemptEntity?
    @Query("UPDATE attempts SET currentIndex = :index WHERE id = :id") suspend fun setIndex(id: String, index: Int)
    @Query("UPDATE attempts SET completedAt = :time WHERE id = :id") suspend fun complete(id: String, time: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveState(state: QuestionStateEntity)
    @Query("SELECT * FROM question_state WHERE attemptId = :attemptId") suspend fun states(attemptId: String): List<QuestionStateEntity>
    @Insert suspend fun insertAnswer(answer: AnswerEntity)
    @Query("SELECT * FROM answers WHERE eventId = :id") suspend fun answer(id: String): AnswerEntity?
    @Query("SELECT * FROM answers WHERE attemptId = :attemptId ORDER BY answeredAt, rowid") suspend fun answers(attemptId: String): List<AnswerEntity>
    @Query("""
        SELECT COALESCE(MAX((SELECT COUNT(*) FROM question_state s WHERE s.attemptId = a.id AND s.solved = 1)), 0) AS solvedQuestions,
            COUNT(a.completedAt) > 0 AS completed, COUNT(a.id) > 0 AS started
        FROM attempts a WHERE a.profileId = :profileId AND a.lessonId = :lessonId AND a.version = :version
    """)
    suspend fun progress(profileId: String, lessonId: String, version: Int): ProgressSummary
}

@Database(
    entities = [ProfileEntity::class, LessonEntity::class, QuestionEntity::class, OptionEntity::class,
        HintEntity::class, AttemptEntity::class, QuestionStateEntity::class, AnswerEntity::class,
        CatalogMetadataEntity::class, GradeEntity::class, SubjectEntity::class, LearningUnitEntity::class,
        TopicEntity::class, CatalogLessonEntity::class, EarnedStickerEntity::class, SchoolAccountEntity::class, TeacherStudentEntity::class, CredentialEntity::class, ReadingProgressEntity::class, DailyGoalEntity::class, LessonDraftEntity::class],
    version = 6, exportSchema = true,
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao
    abstract fun catalogDao(): CatalogDao
    abstract fun rewardsDao(): RewardsDao
    abstract fun schoolDao(): SchoolDao
    abstract fun extensionDao(): ExtensionDao
}

