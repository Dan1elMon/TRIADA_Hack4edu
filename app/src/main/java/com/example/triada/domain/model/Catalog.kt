package com.example.triada.domain.model

enum class ContentAvailability { CATALOG_ONLY, AVAILABLE }
enum class LearningStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

data class Grade(val id: String, val number: Int, val label: String, val sortOrder: Int)
data class Subject(val id: String, val gradeId: String, val code: String, val title: String, val iconKey: String, val sortOrder: Int, val referenceCount: Int)
data class LearningUnit(val id: String, val subjectId: String, val title: String, val description: String, val sortOrder: Int)
data class Topic(val id: String, val subjectId: String, val unitId: String?, val title: String, val sourceOrder: Int, val contentStatus: ContentAvailability)
data class CatalogLessonLink(val lessonId: String, val contentVersion: Int, val topicId: String)
data class CatalogBundle(
    val schemaVersion: Int, val catalogVersion: Int, val grades: List<Grade>, val subjects: List<Subject>,
    val units: List<LearningUnit>, val topics: List<Topic>, val lessons: List<CatalogLessonLink>,
)
data class CatalogLesson(
    val id: String, val contentVersion: Int, val topicId: String, val title: String, val objective: String,
    val solvedQuestions: Int, val totalQuestions: Int, val status: LearningStatus,
)
data class LearningCatalog(
    val profileId: String, val version: Int, val grades: List<Grade>, val subjects: List<Subject>,
    val units: List<LearningUnit>, val topics: List<Topic>, val lessons: List<CatalogLesson>,
)

/** Validate a complete catalog before any persistent writes; titles are never identifiers. */
object CatalogValidator {
    fun validate(bundle: CatalogBundle) {
        require(bundle.schemaVersion == 1 && bundle.catalogVersion > 0) { "Unsupported catalog version" }
        fun unique(values: List<String>) { require(values.all { it.isNotBlank() } && values.size == values.toSet().size) { "Duplicate or empty identifier" } }
        unique(bundle.grades.map { it.id })
        unique(bundle.subjects.map { it.id })
        unique(bundle.units.map { it.id })
        unique(bundle.topics.map { it.id })
        require(bundle.grades.isNotEmpty() && bundle.subjects.isNotEmpty())
        require(bundle.grades.all { it.number > 0 && it.label.isNotBlank() && it.sortOrder >= 0 })
        require(bundle.grades.map { it.number }.distinct().size == bundle.grades.size)
        val grades = bundle.grades.associateBy { it.id }
        val subjects = bundle.subjects.associateBy { it.id }
        val units = bundle.units.associateBy { it.id }
        val topics = bundle.topics.associateBy { it.id }
        bundle.subjects.forEach { subject ->
            require(subject.gradeId in grades && subject.code.isNotBlank() && subject.title.isNotBlank() && subject.referenceCount > 0 && subject.sortOrder >= 0)
            val sourceTopics = bundle.topics.filter { it.subjectId == subject.id }
            require(sourceTopics.size == subject.referenceCount)
            require(sourceTopics.map { it.sourceOrder }.sorted() == (1..subject.referenceCount).toList())
        }
        bundle.subjects.groupBy { it.gradeId }.values.forEach { rows ->
            unique(rows.map { it.code }); require(rows.map { it.sortOrder }.distinct().size == rows.size)
        }
        bundle.units.forEach { require(it.subjectId in subjects && it.title.isNotBlank() && it.description.isNotBlank() && it.sortOrder >= 0) }
        bundle.units.groupBy { it.subjectId }.values.forEach { rows -> require(rows.map { it.sortOrder }.distinct().size == rows.size) }
        bundle.topics.forEach { topic ->
            require(topic.subjectId in subjects && topic.title.isNotBlank())
            if (topic.unitId != null) require(units[topic.unitId]?.subjectId == topic.subjectId)
        }
        require(bundle.lessons.map { it.lessonId to it.contentVersion }.distinct().size == bundle.lessons.size)
        bundle.lessons.forEach { link ->
            require(link.lessonId.isNotBlank() && link.contentVersion > 0)
            require(topics[link.topicId]?.contentStatus == ContentAvailability.AVAILABLE)
            require(topics[link.topicId]?.unitId != null)
        }
        val linkedTopics = bundle.lessons.map { it.topicId }.toSet()
        require(bundle.topics.filter { it.contentStatus == ContentAvailability.AVAILABLE }.all { it.id in linkedTopics })
    }
}
