package com.example.triada

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class SchoolRepositoryTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),LearningDatabase::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
    @Test fun rolesAndRosterRestrictGradeAccessAndKeepFirstResponseScore() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
        try {
            val repo=SchoolRepository(db)
            repo.create("Ana",SchoolRole.STUDENT); repo.create("Luis",SchoolRole.STUDENT);repo.create("Docente B",SchoolRole.TEACHER)
            val all=repo.accounts();val ana=all.first{it.name=="Ana"};val luis=all.first{it.name=="Luis"}
            val teacher=all.first{it.id=="demo-teacher"};val other=all.first{it.name=="Docente B"}
            assertTrue(runCatching{repo.grades(teacher.id,ana.id)}.isFailure)
            repo.link(teacher.id,ana.id,true)
            assertEquals(listOf(ana.id),repo.linkedStudents(teacher.id).map{it.id})
            assertTrue(runCatching{repo.grades(other.id,ana.id)}.isFailure)
            assertTrue(runCatching{repo.grades(luis.id,ana.id)}.isFailure)
            assertTrue(runCatching{repo.link(ana.id,luis.id,true)}.isFailure)
            val learning=RoomLearningRepository(db); val profile=requireNotNull(ana.profileId)
            var session=learning.openLesson(profile)
            assertNull(repo.grades(ana.id,ana.id).single().score)
            learning.requestHint(session.attemptId,profile,session.question.id)
            learning.answer(session.attemptId,profile,session.question.id,session.question.options.first{it!=session.question.objectCount},"wrong")
            while(!session.completed) {
                learning.answer(session.attemptId,profile,session.question.id,session.question.objectCount,UUID.randomUUID().toString())
                session=learning.advance(session.attemptId,profile)
            }
            val own=repo.grades(ana.id,ana.id).single()
            assertEquals(13.3,requireNotNull(own.score),0.001);assertEquals(1,own.hints)
            assertEquals(own,repo.grades(teacher.id,ana.id).single())
            repo.edit(ana.id,"Ana nueva","flower")
            assertEquals("flower",db.learningDao().profile(profile)!!.avatar)
            assertEquals(own,repo.grades(ana.id,ana.id).single())
            repo.link(teacher.id,ana.id,false)
            assertTrue(runCatching{repo.grades(teacher.id,ana.id)}.isFailure)
            assertEquals(own,repo.grades(ana.id,ana.id).single())
        } finally { db.close() }
    }
    @Test fun migrationPreservesLegacyProgressAndAdoptsProfileWithoutDuplicates() = runBlocking {
        val name="school-${UUID.randomUUID()}"
        try {
            helper.createDatabase(name,4).use { old ->
                old.execSQL("INSERT INTO profiles VALUES ('legacy','Luna','sun',1)")
                old.execSQL("INSERT INTO lessons VALUES ('old',1,'Anterior','Objetivo')")
                old.execSQL("INSERT INTO attempts VALUES ('saved','legacy','old',1,0,1,NULL)")
            }
            helper.runMigrationsAndValidate(name,5,true,SchoolMigration.FROM_4_TO_5).close()
            val db=Room.databaseBuilder(context,LearningDatabase::class.java,name).addMigrations(ExtensionMigration.FROM_5_TO_6).build()
            try {
                val repo=SchoolRepository(db)
                repeat(2){assertEquals(1,repo.accounts().count{it.profileId=="legacy"})}
                assertNotNull(db.learningDao().attempt("saved"))
                assertEquals("Luna",db.learningDao().profile("legacy")!!.displayName)
                assertTrue(repo.linkedStudents("demo-teacher").isEmpty())
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
