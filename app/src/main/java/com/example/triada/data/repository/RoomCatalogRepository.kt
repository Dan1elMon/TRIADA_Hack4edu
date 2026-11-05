package com.example.triada.data.repository

import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.domain.model.*
import com.example.triada.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class RoomCatalogRepository(private val database: LearningDatabase, private val source: CatalogSource) : CatalogRepository {
    private val dao = database.catalogDao()

    /** A version is immutable. Metadata updates require a new version, preserving stable IDs. */
    suspend fun install() = withContext(Dispatchers.IO) {
        val raw = source.read()
        val bundle = CatalogParser.parse(raw)
        val checksum = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        database.withTransaction {
            val existing = dao.metadata()
            if (existing != null && existing.catalogVersion >= bundle.catalogVersion) {
                if (existing.catalogVersion == bundle.catalogVersion) require(existing.checksum == checksum) { "Catalog version was changed in place" }
                return@withTransaction // Never downgrade a newer installed catalog.
            }
            val oldTopics = dao.topics().associateBy { it.id }
            require(bundle.topics.map { it.id }.toSet().containsAll(oldTopics.keys)) { "Updates cannot remove topic IDs" }
            bundle.topics.forEach { topic ->
                oldTopics[topic.id]?.let { require(it.subjectId == topic.subjectId) { "Topic identity cannot move between subjects" } }
            }
            // This release ships only one executable lesson. Link activation is checked against real local questions.
            RoomLearningRepository(database).seed()
            bundle.lessons.forEach { link ->
                val learning = database.learningDao()
                requireNotNull(learning.lesson(link.lessonId, link.contentVersion)) { "Lesson content is missing" }
                val questions = learning.questions(link.lessonId, link.contentVersion)
                require(questions.size in 3..5)
                questions.forEach { q ->
                    require(q.objectCount in 1..9)
                    require(learning.options(q.id, q.version).count { it.value == q.objectCount } == 1)
                    require(learning.hints(q.id, q.version).size >= 2)
                }
            }
            require(bundle.lessons.map { it.lessonId to it.contentVersion }.toSet().containsAll(dao.lessons().map { it.lessonId to it.contentVersion })) { "Updates cannot remove published lesson links" }
            dao.saveGrades(bundle.grades.map { GradeEntity(it.id, it.number, it.label, it.sortOrder) })
            dao.saveSubjects(bundle.subjects.map { SubjectEntity(it.id, it.gradeId, it.code, it.title, it.iconKey, it.sortOrder, it.referenceCount) })
            dao.saveUnits(bundle.units.map { LearningUnitEntity(it.id, it.subjectId, it.title, it.description, it.sortOrder) })
            dao.saveTopics(bundle.topics.map { TopicEntity(it.id, it.subjectId, it.unitId, it.title, it.sourceOrder, it.contentStatus.name) })
            dao.saveLessons(bundle.lessons.map { CatalogLessonEntity(it.lessonId, it.contentVersion, it.topicId) })
            dao.saveMetadata(CatalogMetadataEntity(schemaVersion = bundle.schemaVersion, catalogVersion = bundle.catalogVersion, checksum = checksum))
        }
    }

    override suspend fun load(profileId: String): LearningCatalog {
        install()
        return database.withTransaction {
            val learning = database.learningDao()
            requireNotNull(learning.profile(profileId))
            val links = dao.lessons().groupBy { it.lessonId }.values.map { rows -> rows.maxBy { it.contentVersion } }
            val lessons = links.map { link ->
                val row = requireNotNull(learning.lesson(link.lessonId, link.contentVersion))
                val progress = learning.progress(profileId, link.lessonId, link.contentVersion)
                CatalogLesson(row.id, row.version, link.topicId, row.title, row.objective, progress.solvedQuestions,
                    learning.questions(row.id, row.version).size, when {
                        progress.completed -> LearningStatus.COMPLETED
                        progress.started -> LearningStatus.IN_PROGRESS
                        else -> LearningStatus.NOT_STARTED
                    })
            }
            LearningCatalog(
                profileId, requireNotNull(dao.metadata()).catalogVersion,
                dao.grades().map { Grade(it.id, it.number, it.label, it.sortOrder) },
                dao.subjects().map { Subject(it.id, it.gradeId, it.code, it.title, it.iconKey, it.sortOrder, it.referenceCount) },
                dao.units().map { LearningUnit(it.id, it.subjectId, it.title, it.description, it.sortOrder) },
                dao.topics().map { Topic(it.id, it.subjectId, it.unitId, it.title, it.sourceOrder, ContentAvailability.valueOf(it.contentStatus)) }, lessons,
            )
        }
    }
}
