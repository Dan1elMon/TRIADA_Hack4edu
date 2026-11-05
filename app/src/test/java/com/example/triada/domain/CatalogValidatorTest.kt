package com.example.triada.domain

import com.example.triada.domain.model.*
import org.junit.Test

class CatalogValidatorTest {
    private fun catalog() = CatalogBundle(1, 1,
        listOf(Grade("g1", 1, "Primer grado", 0)),
        listOf(Subject("mat", "g1", "MAT", "Matemática", "numbers", 0, 1)),
        listOf(LearningUnit("unit", "mat", "Contamos", "Contamos objetos", 0)),
        listOf(Topic("topic", "mat", "unit", "Números", 1, ContentAvailability.AVAILABLE)),
        listOf(CatalogLessonLink("lesson", 1, "topic")),
    )

    @Test fun validCatalogPasses() { CatalogValidator.validate(catalog()) }
    @Test(expected = IllegalArgumentException::class) fun unsupportedSchemaFails() { CatalogValidator.validate(catalog().copy(schemaVersion = 9)) }
    @Test(expected = IllegalArgumentException::class) fun duplicateTopicIdsFail() {
        val data = catalog(); CatalogValidator.validate(data.copy(topics = data.topics + data.topics.first()))
    }
    @Test(expected = IllegalArgumentException::class) fun crossSubjectUnitFails() {
        val data = catalog(); CatalogValidator.validate(data.copy(units = data.units.map { it.copy(subjectId = "other") }))
    }
    @Test(expected = IllegalArgumentException::class) fun availableTopicWithoutLessonFails() { CatalogValidator.validate(catalog().copy(lessons = emptyList())) }
    @Test(expected = IllegalArgumentException::class) fun referenceOnlyTopicCannotHaveLesson() {
        val data = catalog(); CatalogValidator.validate(data.copy(topics = data.topics.map { it.copy(contentStatus = ContentAvailability.CATALOG_ONLY) }))
    }
    @Test(expected = IllegalArgumentException::class) fun wrongReferenceCountFails() {
        val data = catalog(); CatalogValidator.validate(data.copy(subjects = data.subjects.map { it.copy(referenceCount = 2) }))
    }
    @Test(expected = IllegalArgumentException::class) fun missingSourceOrderFails() {
        val data = catalog(); CatalogValidator.validate(data.copy(topics = data.topics.map { it.copy(sourceOrder = 2) }))
    }
    @Test fun referenceTopicsNeedNoUnitOrLesson() {
        val data = catalog(); CatalogValidator.validate(data.copy(topics = data.topics.map { it.copy(unitId = null, contentStatus = ContentAvailability.CATALOG_ONLY) }, lessons = emptyList()))
    }
}
