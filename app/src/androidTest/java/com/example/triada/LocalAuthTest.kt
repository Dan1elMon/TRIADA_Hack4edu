package com.example.triada

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.SchoolRole
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class LocalAuthTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),LearningDatabase::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
    @Test fun defaultCredentialsCanChangeWithoutChangingIdentityOrAllowingDuplicates()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
        try {
            val auth=LocalAuthRepository(db);auth.initialize()
            assertNull(auth.login("10000001","incorrecta"));assertNull(auth.login("1234567","admin"))
            val teacher=requireNotNull(auth.login("10000001","admin"));assertEquals(SchoolRole.TEACHER,teacher.role)
            val stored=requireNotNull(db.extensionDao().credential(teacher.id));assertNotEquals("admin",stored.passwordHash)
            SchoolRepository(db).create("Alumno",SchoolRole.STUDENT);auth.initialize()
            val pupil=SchoolRepository(db).accounts().first{it.role==SchoolRole.STUDENT}
            assertTrue(runCatching{auth.change(teacher.id,"admin",pupil.username,"nuevaClave")}.isFailure)
            assertTrue(runCatching{auth.change(teacher.id,"error","00123456","nuevaClave")}.isFailure)
            auth.change(teacher.id,"admin","00123456","nuevaClave");auth.initialize()
            assertNull(auth.login("10000001","admin"));assertNull(auth.login("00123456","admin"))
            assertEquals(teacher.id,auth.login("00123456","nuevaClave")?.id)
            assertEquals(pupil.id,auth.login(pupil.username,"admin")?.id)
            assertNotEquals(stored.salt,db.extensionDao().credential(teacher.id)!!.salt)
        }finally{db.close()}
    }
    @Test fun schemaFiveUpgradesWithoutRemovingAccountsOrProgress()=runBlocking {
        val name="auth-${UUID.randomUUID()}"
        try {
            helper.createDatabase(name,5).use{old ->
                old.execSQL("INSERT INTO profiles VALUES ('saved','Luna','sun',1)")
                old.execSQL("INSERT INTO school_accounts VALUES ('student-saved','Luna','STUDENT','saved')")
            }
            helper.runMigrationsAndValidate(name,6,true,ExtensionMigration.FROM_5_TO_6).close()
            val db=Room.databaseBuilder(context,LearningDatabase::class.java,name).build()
            try{val auth=LocalAuthRepository(db);auth.initialize();val student=SchoolRepository(db).accounts().single{it.profileId=="saved"};assertEquals(student.id,auth.login(student.username,"admin")?.id);assertEquals("sun",db.learningDao().profile("saved")!!.avatar)}finally{db.close()}
        }finally{context.deleteDatabase(name)}
    }
}
