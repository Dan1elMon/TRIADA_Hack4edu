package com.example.triada

import android.content.ContextWrapper
import android.media.MediaMetadataRetriever
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.data.packageio.*
import com.example.triada.domain.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.*
import java.util.UUID

class ExtendedFeaturesTest {
    private val base=InstrumentationRegistry.getInstrumentation().targetContext
    private val folder=File(base.cacheDir,"expanded-${UUID.randomUUID()}").apply{mkdirs()}
    private val context=object:ContextWrapper(base){override fun getFilesDir()=folder}
    private val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
    @After fun close(){db.close();folder.deleteRecursively()}
    @Test fun fullCurriculumHas214ExecutableLessonsAndRoundTripsWithProgressIntact()=runBlocking {
        val learning=RoomLearningRepository(db);val profile=learning.createProfile("Completo","sun")
        val catalog=RoomCatalogRepository(db,AssetCatalogSource(context)).load(profile.id)
        assertEquals(214,catalog.topics.size);assertEquals(214,catalog.lessons.size);assertEquals(642,catalog.lessons.sumOf{it.totalQuestions})
        assertTrue(catalog.topics.all{it.contentStatus==ContentAvailability.AVAILABLE})
        val additions=catalog.lessons.filter{it.id.endsWith("-intro")};assertEquals(208,additions.size)
        additions.forEach{lesson->db.learningDao().questions(lesson.id,lesson.contentVersion).forEach{q->
            val def=ActivityCodec.decode(q,emptyList());ExerciseValidator.validate(def)
            assertEquals(Evaluation.Correct,ExerciseEvaluator.evaluate(def,LearningAnswer.Choice(requireNotNull(def.correctChoiceId))))
            assertEquals(Evaluation.Incorrect,ExerciseEvaluator.evaluate(def,LearningAnswer.Choice(def.content.choices.first{it.id!=def.correctChoiceId}.id)))
        }}
        val lesson=additions.first();var s=learning.openLesson(profile.id,lessonId=lesson.id)
        while(!s.completed){val q=db.learningDao().question(s.question.id,s.contentVersion)!!;val def=ActivityCodec.decode(q,emptyList());learning.submitAnswer(s.attemptId,profile.id,s.question.id,LearningAnswer.Choice(def.correctChoiceId!!),UUID.randomUUID().toString());s=learning.advance(s.attemptId,profile.id)}
        assertEquals(1 to 1,learning.dailyGoal(profile.id));learning.setDailyGoal(profile.id,3);assertEquals(1 to 3,learning.dailyGoal(profile.id))
        val out=ByteArrayOutputStream();val service=LessonPackageService(db,context);service.export(out);service.import(out.toByteArray().inputStream())
        assertEquals(214,db.catalogDao().lessons().size);assertTrue(learning.session(s.attemptId,profile.id).completed)
        val fresh=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
        try {
            LessonPackageService(fresh,context).import(out.toByteArray().inputStream())
            val newcomer=RoomLearningRepository(fresh).createProfile("Nuevo dispositivo","star")
            assertEquals(214,RoomCatalogRepository(fresh,AssetCatalogSource(context)).load(newcomer.id).lessons.size)
        } finally {fresh.close()}
    }
    @Test fun readerPersistsPageAndComprehensionSeparatelyForEachStudentAndAllAudioIsDecodable()=runBlocking {
        val learning=RoomLearningRepository(db);val a=learning.createProfile("Ana","star");val b=learning.createProfile("Luis","sun")
        val reader=ReadingRepository(db,context);assertEquals(6,reader.books.size)
        val book=reader.books.first();reader.move(a.id,book.id,1);reader.move(a.id,book.id,1);reader.finish(a.id,book.id)
        assertFalse(reader.answer(a.id,book.id,1));assertTrue(reader.answer(a.id,book.id,0))
        assertTrue(reader.open(a.id,book.id).comprehensionCorrect);assertEquals(2,reader.open(a.id,book.id).pageIndex)
        assertEquals(0,reader.open(b.id,book.id).pageIndex);assertFalse(reader.open(b.id,book.id).completed)
        val keys=reader.books.flatMap{it.pages}.flatMap{listOf(it.audio)+it.words.map{w->w.audio}}.distinct()
        keys.forEach{key->val media=MediaMetadataRetriever();try{context.assets.openFd("audio/$key.wav").use{media.setDataSource(it.fileDescriptor,it.startOffset,it.length)};assertTrue(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()>0)}finally{media.release()}}
    }
    @Test fun teacherPublishesTwoVersionsAndCannotOverwriteAnotherTeachersDraft()=runBlocking {
        val school=SchoolRepository(db);school.accounts();school.create("Otro docente",SchoolRole.TEACHER);school.create("Alumno",SchoolRole.STUDENT)
        val other=school.accounts().first{it.name=="Otro docente"};val student=school.accounts().first{it.role==SchoolRole.STUDENT}
        val editor=LessonEditorRepository(db,context);editor.topics("demo-teacher")
        val id="teacher-${UUID.randomUUID()}";val root=JSONObject(LessonEditorRepository.template("g1-mat-t01"))
        root.getJSONObject("lesson").put("title","Mi lección publicada").put("objective","Practicar posiciones en el espacio.")
        root.getJSONObject("lesson").put("questions",JSONObject(context.assets.open("content/expanded-lessons-v1.json").bufferedReader().use{it.readText()}).getJSONArray("lessons").getJSONObject(0).getJSONArray("questions"))
        val raw=root.toString()
        assertEquals(3,editor.validate(raw).size);editor.save("demo-teacher",id,raw)
        assertTrue(runCatching{editor.save(other.id,id,raw)}.isFailure)
        assertTrue(runCatching{editor.topics(student.id)}.isFailure)
        assertEquals(1,editor.publish("demo-teacher",id,raw))
        val learning=RoomLearningRepository(db);var session=learning.openLesson(student.profileId!!,lessonId=id)
        learning.submitAnswer(session.attemptId,student.profileId,session.question.id,LearningAnswer.Choice("a"),"saved-version-one")
        assertEquals(2,editor.publish("demo-teacher",id,raw))
        assertEquals(1,learning.session(session.attemptId,student.profileId).solvedQuestions)
        session=learning.openLesson(student.profileId,lessonId=id);assertEquals(2,session.contentVersion);assertEquals(0,session.solvedQuestions)
    }
}
