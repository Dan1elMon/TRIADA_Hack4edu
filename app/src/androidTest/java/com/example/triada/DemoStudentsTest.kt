package com.example.triada

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.SchoolRole
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class DemoStudentsTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun demoRosterAuthenticatesAndReinitializationPreservesCredentialsProgressAndUnlinking()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
        try {
            val auth=LocalAuthRepository(db);auth.initialize(includeDemoStudents=true)
            val school=SchoolRepository(db)
            assertEquals(10,school.linkedStudents("demo-teacher").size)
            DemoStudents.teachers.forEach{teacher->
                assertEquals(teacher.accountId,auth.login(teacher.username,"admin")?.id)
                assertEquals(DemoStudents.all.filter{it.teacherId==teacher.accountId}.map{it.accountId}.toSet(),school.linkedStudents(teacher.accountId).map{it.id}.toSet())
            }
            assertEquals(3,school.accounts().count{it.role==SchoolRole.TEACHER})
            assertTrue(runCatching{school.grades("demo-teacher",DemoStudents.all[10].accountId)}.isFailure)
            DemoStudents.all.forEach{demo->assertEquals(demo.accountId,auth.login(demo.username,"admin")?.id)}
            val first=DemoStudents.all.first();val learning=RoomLearningRepository(db)
            val session=learning.openLesson(first.profileId)
            learning.answer(session.attemptId,first.profileId,session.question.id,4,"kept-demo-answer")
            auth.change(first.accountId,"admin","00112233","nuevaClave")
            school.link("demo-teacher",first.accountId,false)
            repeat(2){auth.initialize(includeDemoStudents=true)}
            assertEquals(30,school.accounts().count{it.id in DemoStudents.all.map{d->d.accountId}})
            assertNull(auth.login(first.username,"admin"))
            assertEquals(first.accountId,auth.login("00112233","nuevaClave")?.id)
            assertEquals(1,learning.session(session.attemptId,first.profileId).solvedQuestions)
            assertFalse(school.linkedStudents("demo-teacher").any{it.id==first.accountId})
        }finally{db.close()}
    }
    @Test fun occupiedDemoUsernameKeepsExistingAccountAndGetsAFallbackWithoutStealingTheNextStudentsUsername()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
        try {
            val auth=LocalAuthRepository(db);auth.initialize()
            val school=SchoolRepository(db);school.create("Cuenta anterior",SchoolRole.STUDENT);auth.initialize()
            val existing=school.accounts().first{it.role==SchoolRole.STUDENT}
            auth.change(existing.id,"admin","20000001","conservarClave")
            auth.initialize(includeDemoStudents=true)
            assertEquals(existing.id,auth.login("20000001","conservarClave")?.id)
            assertEquals(DemoStudents.all.first().accountId,auth.login("29000001","admin")?.id)
            assertEquals(DemoStudents.all[1].accountId,auth.login("20000002","admin")?.id)
            assertEquals(34,db.extensionDao().usernames().distinct().size)
        }finally{db.close()}
    }
    @Test fun occupiedTeacherCodeDoesNotOverwriteExistingAccountOrReuseAnotherTeachersCode()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
        try {
            val auth=LocalAuthRepository(db);auth.initialize()
            val school=SchoolRepository(db);school.create("Usuario anterior",SchoolRole.STUDENT);auth.initialize()
            val previous=school.accounts().first{it.role==SchoolRole.STUDENT}
            auth.change(previous.id,"admin","11000002","claveAnterior")
            auth.initialize(includeDemoStudents=true)
            assertEquals(previous.id,auth.login("11000002","claveAnterior")?.id)
            assertEquals("demo-teacher-02",auth.login("19000001","admin")?.id)
            assertEquals("demo-teacher-03",auth.login("11000003","admin")?.id)
        } finally{db.close()}
    }
}
