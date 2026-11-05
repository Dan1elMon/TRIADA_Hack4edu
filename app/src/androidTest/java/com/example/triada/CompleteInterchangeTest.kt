package com.example.triada

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.packageio.*
import com.example.triada.data.repository.*
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class CompleteInterchangeTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private fun context() = object : ContextWrapper(base) {
        private val folder = File(base.cacheDir,"exchange-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        override fun getFilesDir() = folder
    }
    private fun write(files: Map<String,ByteArray>): ByteArray {
        val hashes = JSONObject(); files.filterKeys { it != "checksums.json" }.forEach { (name,data) -> hashes.put(name,java.security.MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }) }
        val output=ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(output).use { zip -> (files + ("checksums.json" to hashes.toString().toByteArray())).forEach { (name,data) -> zip.putNextEntry(java.util.zip.ZipEntry(name));zip.write(data);zip.closeEntry() } }
        return output.toByteArray()
    }
    @Test fun contentRoundTripAddsANewLessonAndCanBeRepeatedWithoutLosingProgress() = runBlocking {
        val context=context(); val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
        try {
            val repo=RoomLearningRepository(db); val profile=repo.createProfile("Original","star")
            RoomCatalogRepository(db,AssetCatalogSource(context, false)).load(profile.id)
            val original=repo.openLesson(profile.id);repo.answer(original.attemptId,profile.id,original.question.id,4,"original-answer")
            val output=ByteArrayOutputStream(); val service=LessonPackageService(db,context,AssetCatalogSource(context,false)); service.export(output)
            service.import(output.toByteArray().inputStream())
            val files=SafePackageReader().read(output.toByteArray().inputStream()).toMutableMap()
            val catalog=JSONObject(files.getValue("catalog.json").toString(Charsets.UTF_8));catalog.put("catalogVersion",3)
            val topics=catalog.getJSONArray("topics")
            repeat(topics.length()) { n -> val row=topics.getJSONObject(n);if(row.getString("id")=="g1-mat-t04") {row.put("contentStatus","AVAILABLE");row.put("unitId","g1-mat-u1")} }
            catalog.getJSONArray("lessons").put(JSONObject().put("lessonId","g1-mat-extra-count").put("contentVersion",1).put("topicId","g1-mat-t04"))
            val lessons=JSONObject(files.getValue("lessons.json").toString(Charsets.UTF_8))
            val all=lessons.getJSONArray("lessons"); val source=(0 until all.length()).map {all.getJSONObject(it)}.single {it.getString("id")=="g1-mat-count-1-9"}
            val extra=JSONObject(source.toString()).put("id","g1-mat-extra-count").put("title","Más conteo")
            val questions=extra.getJSONArray("questions");repeat(questions.length()) {questions.getJSONObject(it).put("id","g1-extra-q$it")};all.put(extra)
            files["catalog.json"]=catalog.toString().toByteArray();files["lessons.json"]=lessons.toString().toByteArray()
            files["manifest.json"]=JSONObject(files.getValue("manifest.json").toString(Charsets.UTF_8)).put("catalogVersion",3).toString().toByteArray()
            val archive=write(files);repeat(2){service.import(archive.inputStream())}
            assertEquals(7,db.catalogDao().lessons().size)
            val session=repo.openLesson(profile.id,lessonId="g1-mat-extra-count")
            assertEquals(com.example.triada.domain.model.Evaluation.Correct,repo.answer(session.attemptId,profile.id,session.question.id,4,"new-answer"))
            assertEquals(1,repo.session(original.attemptId,profile.id).solvedQuestions)
            val reexport=ByteArrayOutputStream();service.export(reexport);service.import(reexport.toByteArray().inputStream())
            assertEquals(7,db.catalogDao().lessons().size)
        } finally {db.close();context.filesDir.deleteRecursively()}
    }
    @Test fun resultsRecoverIntoSeparateProfileAndRoundTripWithoutDuplicates() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(base,LearningDatabase::class.java).build()
        try {
            val repo=RoomLearningRepository(db);val p=repo.createProfile("Original","sun")
            RoomCatalogRepository(db,AssetCatalogSource(base, false)).load(p.id)
            var session=repo.openLesson(p.id)
            listOf(4,6,9).forEachIndexed {n,value ->repo.answer(session.attemptId,p.id,session.question.id,value,"a$n");if(n<2)session=repo.advance(session.attemptId,p.id)}
            val out=ByteArrayOutputStream();StudentResultsExporter(db).export(p.id,out)
            val importer=StudentResultsImporter(db);val recovered=importer.import(out.toByteArray().inputStream())
            assertNotEquals(p.id,recovered);assertEquals(recovered,importer.import(out.toByteArray().inputStream()))
            assertEquals(1,db.learningDao().profileAttempts(recovered).size)
            assertEquals(1,repo.rewards(recovered).size)
            val again=ByteArrayOutputStream();StudentResultsExporter(db).export(recovered,again)
            assertEquals(recovered,importer.import(again.toByteArray().inputStream()))
            assertEquals(1,db.learningDao().profileAttempts(recovered).size)
            assertEquals(3L,db.learningDao().profileAnswerCount(p.id))
            val corrupt=SafePackageReader().read(out.toByteArray().inputStream()).toMutableMap()
            val answers=JSONObject(corrupt.getValue("answers.json").toString(Charsets.UTF_8));answers.getJSONArray("answers").getJSONObject(0).put("isCorrect",false)
            corrupt["answers.json"]=answers.toString().toByteArray()
            var rejected=false;try{importer.import(write(corrupt).inputStream())}catch(_:IllegalArgumentException){rejected=true}
            assertTrue(rejected);assertEquals(3L,db.learningDao().profileAnswerCount(recovered))
        } finally {db.close()}
    }
}
