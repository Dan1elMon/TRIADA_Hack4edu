package com.example.triada

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.packageio.CatalogPackageService
import com.example.triada.data.repository.RoomLearningRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class CatalogPackageTest {
    @Test fun corruptOrIncompatiblePackagesLeaveTheDatabaseUntouched() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
        try {
            val service = CatalogPackageService(db, AssetCatalogSource(context, false))
            val output = ByteArrayOutputStream()
            service.exportBundledCatalog(output)
            val original = com.example.triada.data.packageio.SafePackageReader().read(output.toByteArray().inputStream())
            fun packageBytes(files: Map<String, ByteArray>, rehash: Boolean): ByteArray {
                val mutable = files.toMutableMap()
                if (rehash) {
                    val checksums = org.json.JSONObject()
                    listOf("manifest.json", "catalog.json").forEach { name -> checksums.put(name, java.security.MessageDigest.getInstance("SHA-256").digest(mutable.getValue(name)).joinToString("") { "%02x".format(it) }) }
                    mutable["checksums.json"] = checksums.toString().toByteArray()
                }
                val bytes = ByteArrayOutputStream()
                java.util.zip.ZipOutputStream(bytes).use { zip -> mutable.forEach { (name, data) -> zip.putNextEntry(java.util.zip.ZipEntry(name)); zip.write(data); zip.closeEntry() } }
                return bytes.toByteArray()
            }
            val incompatible = original.toMutableMap().apply {
                this["manifest.json"] = org.json.JSONObject(getValue("manifest.json").toString(Charsets.UTF_8)).put("formatVersion", 99).toString().toByteArray()
            }
            val tampered = original.toMutableMap().apply { this["catalog.json"] = "{}".toByteArray() }
            val extra = original + ("resources/unknown" to byteArrayOf(1))
            listOf(packageBytes(incompatible, true), packageBytes(tampered, false), packageBytes(extra, true)).forEach { bytes ->
                var rejected = false
                try { service.importCatalog(bytes.inputStream()) } catch (_: Exception) { rejected = true }
                assertTrue(rejected)
                assertNull(db.catalogDao().metadata())
                assertTrue(db.catalogDao().topics().isEmpty())
            }
        } finally { db.close() }
    }
    @Test fun roundTripIsIdempotentAndPreservesExistingAnswers() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
        try {
            val repo = RoomLearningRepository(db)
            val profile = repo.createProfile("Paquetes", "sun")
            val session = repo.openLesson(profile.id)
            repo.answer(session.attemptId, profile.id, session.question.id, 4, "saved")
            val service = CatalogPackageService(db, AssetCatalogSource(context, false))
            val output = ByteArrayOutputStream()
            service.exportBundledCatalog(output)
            repeat(2) { service.importCatalog(output.toByteArray().inputStream()) }
            assertEquals(214, db.catalogDao().topics().size)
            assertEquals(1, repo.session(session.attemptId, profile.id).solvedQuestions)
            assertEquals(1, db.learningDao().answers(session.attemptId).size)
        } finally { db.close() }
    }
}
