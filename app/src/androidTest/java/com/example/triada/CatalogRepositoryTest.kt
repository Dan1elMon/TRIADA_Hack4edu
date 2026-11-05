package com.example.triada

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.RoomCatalogRepository
import com.example.triada.data.repository.RoomLearningRepository
import com.example.triada.domain.model.ContentAvailability
import com.example.triada.domain.model.LearningStatus
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CatalogRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: LearningDatabase
    private lateinit var learning: RoomLearningRepository
    private lateinit var raw: String

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
        learning = RoomLearningRepository(database)
        raw = AssetCatalogSource(context).read()
    }
    @After fun close() { database.close() }
    private fun repository(text: String = raw) = RoomCatalogRepository(database, object : CatalogSource { override fun read() = text; override fun hasAudio(key: String) = AssetCatalogSource(context).hasAudio(key) })

    @Test fun bundledCatalogContainsExactlyTheReferenceAndSixRealLessons() = runBlocking {
        val profile = learning.createProfile("Catálogo", "sun")
        val catalog = repository().load(profile.id)
        assertEquals(214, catalog.topics.size)
        assertEquals(6, catalog.units.size)
        assertEquals(6, catalog.subjects.size)
        assertEquals(listOf(45, 58, 36, 29, 26, 20), catalog.subjects.map { subject -> catalog.topics.count { it.subjectId == subject.id } })
        assertEquals(6, catalog.lessons.size)
        assertEquals(208, catalog.topics.count { it.contentStatus == ContentAvailability.CATALOG_ONLY })
        assertEquals("Números del 1 al 9", catalog.topics.single { it.id == "g1-mat-t03" }.title)
        assertEquals(LearningStatus.NOT_STARTED, catalog.lessons.single { it.id == "g1-mat-count-1-9" }.status)
        assertNull(database.learningDao().latestAttempt(profile.id, "g1-mat-count-1-9", 1))
    }

    @Test fun repeatedSeedAndMetadataUpgradePreserveAnswersHintsAndProgress() = runBlocking {
        val profile = learning.createProfile("Persistente", "star")
        val repo = repository()
        repo.load(profile.id)
        val session = learning.openLesson(profile.id)
        learning.requestHint(session.attemptId, profile.id, session.question.id)
        learning.answer(session.attemptId, profile.id, session.question.id, 3, "error")
        learning.answer(session.attemptId, profile.id, session.question.id, 4, "correct")
        repeat(2) { repo.load(profile.id) }
        val upgraded = JSONObject(raw).put("catalogVersion", 3)
        upgraded.getJSONArray("topics").getJSONObject(2).put("title", "Números: aprendemos del 1 al 9")
        val catalog = repository(upgraded.toString()).load(profile.id)
        assertEquals(3, catalog.version)
        assertEquals(214, catalog.topics.size)
        assertEquals("Números: aprendemos del 1 al 9", catalog.topics.single { it.id == "g1-mat-t03" }.title)
        assertEquals(1, catalog.lessons.single { it.id == "g1-mat-count-1-9" }.solvedQuestions)
        assertEquals(LearningStatus.IN_PROGRESS, catalog.lessons.single { it.id == "g1-mat-count-1-9" }.status)
        assertEquals(2, database.learningDao().answers(session.attemptId).size)
        assertEquals(1, learning.session(session.attemptId, profile.id).visibleHints.size)
        assertEquals(3, repo.load(profile.id).version) // Bundled v1 cannot downgrade installed v2.
    }

    @Test fun invalidCatalogDoesNotChangeInstalledData() = runBlocking {
        val profile = learning.createProfile("Seguro", "flower")
        repository().load(profile.id)
        val invalid = JSONObject(raw).put("catalogVersion", 3)
        invalid.getJSONArray("topics").getJSONObject(0).put("subjectId", "missing")
        try { repository(invalid.toString()).install(); fail("Invalid references must fail") } catch (_: IllegalArgumentException) { }
        assertEquals(2, database.catalogDao().metadata()!!.catalogVersion)
        assertEquals(214, database.catalogDao().topics().size)
    }

    @Test fun sameVersionCannotBeSilentlyReplaced() = runBlocking {
        repository().install()
        val changed = JSONObject(raw).put("extra", "changed").toString()
        try { repository(changed).install(); fail("A version must be immutable") } catch (_: IllegalArgumentException) { }
        assertEquals(2, database.catalogDao().metadata()!!.catalogVersion)
    }

    @Test fun referenceOnlyTopicCannotStartAnAttempt() = runBlocking {
        val profile = learning.createProfile("Referencia", "sun")
        repository().load(profile.id)
        try {
            learning.openLesson(profile.id, lessonId = "g1-com-t01")
            fail("Reference topics must never be opened as lessons")
        } catch (_: IllegalArgumentException) { }
        assertNull(database.learningDao().latestAttempt(profile.id, "g1-mat-count-1-9", 1))
    }

    @Test fun missingExecutableContentRollsBackTheEntireInstall() = runBlocking {
        val invalid = JSONObject(raw).put("catalogVersion", 3)
        invalid.getJSONArray("lessons").getJSONObject(0).put("lessonId", "missing-content")
        try { repository(invalid.toString()).install(); fail("Missing lessons must not become available") } catch (_: IllegalArgumentException) { }
        assertNull(database.catalogDao().metadata())
        assertTrue(database.catalogDao().topics().isEmpty())
        assertNull(database.learningDao().lesson("g1-mat-count-1-9", 1))
    }
}
