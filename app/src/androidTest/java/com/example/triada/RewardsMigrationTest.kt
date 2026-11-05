package com.example.triada

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.RoomLearningRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class RewardsMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @get:Rule val helper = MigrationTestHelper(instrumentation, LearningDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory())
    @Test fun completedGoalsAreRecoveredOnceAcrossVersionsAndReopening() = runBlocking {
        val name = "rewards-migration-${UUID.randomUUID()}"
        try {
            helper.createDatabase(name, 3).use { old ->
                old.execSQL("INSERT INTO profiles VALUES ('a','Luna','star',1)")
                old.execSQL("INSERT INTO profiles VALUES ('b','Sol','sun',1)")
                old.execSQL("INSERT INTO lessons VALUES ('g1-mat-count-1-9',1,'Contar','Contar')")
                old.execSQL("INSERT INTO lessons VALUES ('g1-mat-count-1-9',2,'Contar','Contar')")
                old.execSQL("INSERT INTO attempts VALUES ('a1','a','g1-mat-count-1-9',1,2,1,10)")
                old.execSQL("INSERT INTO attempts VALUES ('a2','a','g1-mat-count-1-9',2,2,11,20)")
                old.execSQL("INSERT INTO attempts VALUES ('b1','b','g1-mat-count-1-9',1,0,1,NULL)")
            }
            helper.runMigrationsAndValidate(name, 4, true, CatalogMigration.FROM_3_TO_4).close()
            repeat(2) {
                val db = Room.databaseBuilder(instrumentation.targetContext, LearningDatabase::class.java, name).addMigrations(CatalogMigration.FROM_3_TO_4, com.example.triada.data.local.SchoolMigration.FROM_4_TO_5, com.example.triada.data.local.ExtensionMigration.FROM_5_TO_6).build()
                try {
                    val repo = RoomLearningRepository(db)
                    assertEquals(10L, repo.rewards("a").single().obtainedAt)
                    assertTrue(repo.rewards("b").isEmpty())
                } finally { db.close() }
            }
        } finally { instrumentation.targetContext.deleteDatabase(name) }
    }
}

