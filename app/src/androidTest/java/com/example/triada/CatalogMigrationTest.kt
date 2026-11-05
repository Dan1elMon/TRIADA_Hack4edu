package com.example.triada

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.RoomCatalogRepository
import com.example.triada.data.repository.RoomLearningRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class CatalogMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @get:Rule val helper = MigrationTestHelper(instrumentation, LearningDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory())

    @Test fun versionOneDataSurvivesMigrationAndCatalogInstallation() = migrate(1)
    @Test fun versionTwoDataSurvivesGenericActivityMigration() = migrate(2)
    private fun migrate(startVersion: Int) = runBlocking {
        val name = "catalog-migration-${UUID.randomUUID()}"
        try {
            helper.createDatabase(name, startVersion).use { old ->
                old.execSQL("INSERT INTO profiles VALUES ('legacy-profile', 'Luna', 'star', 100)")
                old.execSQL("INSERT INTO lessons VALUES ('g1-mat-count-1-9', 1, 'Cuento del 1 al 9', 'Contar objetos')")
                old.execSQL("INSERT INTO questions VALUES ('g1-mat-count-q01', 1, 'g1-mat-count-1-9', 0, '¿Cuántos círculos hay?', 4)")
                old.execSQL("INSERT INTO options VALUES ('g1-mat-count-q01', 1, 4, 1)")
                old.execSQL("INSERT INTO hints VALUES ('g1-mat-count-q01', 1, 0, 'Señala cada círculo mientras cuentas.')")
                old.execSQL("INSERT INTO attempts VALUES ('legacy-attempt', 'legacy-profile', 'g1-mat-count-1-9', 1, 0, 101, NULL)")
                old.execSQL("INSERT INTO question_state VALUES ('legacy-attempt', 'g1-mat-count-q01', 1, 1, 1)")
                old.execSQL("INSERT INTO answers VALUES ('wrong', 'legacy-attempt', 'g1-mat-count-q01', 1, 3, 0, 1, 102)")
                old.execSQL("INSERT INTO answers VALUES ('right', 'legacy-attempt', 'g1-mat-count-q01', 1, 4, 1, 1, 103)")
            }
            helper.runMigrationsAndValidate(name, 4, true, CatalogMigration.FROM_1_TO_2, CatalogMigration.FROM_2_TO_3, CatalogMigration.FROM_3_TO_4).close()
            val database = Room.databaseBuilder(instrumentation.targetContext, LearningDatabase::class.java, name)
                .addMigrations(CatalogMigration.FROM_1_TO_2, CatalogMigration.FROM_2_TO_3, CatalogMigration.FROM_3_TO_4, com.example.triada.data.local.SchoolMigration.FROM_4_TO_5, com.example.triada.data.local.ExtensionMigration.FROM_5_TO_6).build()
            try {
                val catalog = RoomCatalogRepository(database, AssetCatalogSource(instrumentation.targetContext)).load("legacy-profile")
                val session = RoomLearningRepository(database).session("legacy-attempt", "legacy-profile")
                assertEquals(214, catalog.topics.size)
                assertEquals(1, catalog.lessons.single { it.id == "g1-mat-count-1-9" }.solvedQuestions)
                assertEquals(0, session.currentIndex)
                assertEquals(1, session.visibleHints.size)
                assertTrue(session.currentSolved)
                assertEquals(listOf(false, true), database.learningDao().answers("legacy-attempt").map { it.correct })
                assertEquals("Luna", database.learningDao().profile("legacy-profile")!!.displayName)
            } finally { database.close() }
        } finally { instrumentation.targetContext.deleteDatabase(name) }
    }
}

