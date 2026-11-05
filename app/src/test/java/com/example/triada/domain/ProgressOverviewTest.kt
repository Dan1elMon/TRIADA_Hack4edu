package com.example.triada.domain

import com.example.triada.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ProgressOverviewTest {
    private fun catalog(lessons: List<CatalogLesson>) = LearningCatalog("a", 2, emptyList(), emptyList(), emptyList(), emptyList(), lessons)
    @Test fun emptyCatalogHasNoInvalidPercentage() {
        assertEquals(0f, ProgressOverview.from(catalog(emptyList())).fraction)
    }
    @Test fun summaryUsesBestProgressOfAvailableLessons() {
        val lessons = listOf(CatalogLesson("one",1,"t1","One","Learn",3,3,LearningStatus.COMPLETED),
            CatalogLesson("two",1,"t2","Two","Learn",1,3,LearningStatus.IN_PROGRESS))
        val result = ProgressOverview.from(catalog(lessons))
        assertEquals(1, result.completedLessons)
        assertEquals(4, result.solvedExercises)
        assertEquals(6, result.totalExercises)
    }
    @Test fun uniqueStableGoalsCoverTheSixLessonsAndThreeCategories() {
        assertEquals(6, StickerCatalog.all.map { it.id }.distinct().size)
        assertEquals(6, StickerCatalog.all.map { it.lessonId }.distinct().size)
        assertEquals(3, StickerCatalog.all.map { it.category }.distinct().size)
        assertNull(StickerCatalog.forLesson("not-published"))
    }
}
