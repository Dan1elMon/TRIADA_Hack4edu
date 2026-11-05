package com.example.triada.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.data.packageio.*
import com.example.triada.domain.model.*
import org.json.*
import java.io.ByteArrayOutputStream

class LessonEditorRepository(private val db:LearningDatabase,private val context:Context) {
    private suspend fun teacher(id:String){require(db.schoolDao().account(id)?.role=="TEACHER"){"Solo un docente puede editar contenidos."}}
    suspend fun topics(actor:String):List<TopicEntity>{teacher(actor);RoomCatalogRepository(db,AssetCatalogSource(context)).install();return db.catalogDao().topics()}
    suspend fun drafts(actor:String):List<LessonDraftEntity>{teacher(actor);return db.extensionDao().drafts(actor)}
    suspend fun save(actor:String,id:String,raw:String)=db.withTransaction{teacher(actor);require(raw.length<=64_000);require(id.matches(Regex("teacher-[a-f0-9-]{36}")));val previous=db.extensionDao().draft(id);require(previous==null||previous.teacherId==actor);db.extensionDao().saveDraft(LessonDraftEntity(id,actor,raw,System.currentTimeMillis()))}
    fun validate(raw:String):List<ExerciseDefinition> {
        val root=PackageJson.parse(raw.toByteArray(),128_000);val lesson=root.getJSONObject("lesson")
        require(lesson.getString("title").length in 1..160 && lesson.getString("objective").length in 1..1000)
        require(lesson.getString("objective")!="Escribe el objetivo de aprendizaje."){"Completa el objetivo antes de publicar."}
        val rows=lesson.getJSONArray("questions");require(rows.length() in 3..5){"La lección necesita de 3 a 5 ejercicios."}
        return (0 until rows.length()).map { i ->
            val q=rows.getJSONObject(i);val options=q.getJSONArray("options");require(options.length()<=12)
            require(!q.getString("prompt").contains("escribe aquí tu pregunta")){"Completa todas las preguntas."}
            val values=(0 until options.length()).map{options.getInt(it)}
            val definition=ActivityCodec.decode(QuestionEntity("preview-$i",1,"preview",i,q.getString("prompt"),q.getInt("objectCount"),q.getString("type"),q.getJSONObject("payload").toString(),q.getJSONObject("answerKey").toString()),values)
            ExerciseValidator.validate(definition);val hints=q.getJSONArray("hints");require(hints.length() in 2..8)
            require(definition.content.choices.none{it.text in setOf("Respuesta correcta","Otra opción")}){"Completa las opciones del ejercicio."}
            require((0 until hints.length()).all{hints.getString(it).length in 1..2000})
            definition
        }
    }
    suspend fun publish(actor:String,id:String,raw:String):Int {
        teacher(actor);validate(raw);save(actor,id,raw)
        val root=JSONObject(raw);val topic=root.getString("topicId");requireNotNull(db.catalogDao().topic(topic))
        val out=ByteArrayOutputStream();val service=LessonPackageService(db,context);service.export(out)
        val files=SafePackageReader().read(out.toByteArray().inputStream()).toMutableMap();files.remove("checksums.json")
        val catalog=JSONObject(files.getValue("catalog.json").toString(Charsets.UTF_8));val version=catalog.getInt("catalogVersion")+1
        catalog.put("catalogVersion",version)
        val lessons=JSONObject(files.getValue("lessons.json").toString(Charsets.UTF_8)).getJSONArray("lessons")
        val next=(0 until lessons.length()).map{lessons.getJSONObject(it)}.filter{it.getString("id")==id}.maxOfOrNull{it.getInt("version")}?.plus(1) ?: 1
        val lesson=root.getJSONObject("lesson");lesson.put("id",id).put("version",next)
        val questions=lesson.getJSONArray("questions");repeat(questions.length()){questions.getJSONObject(it).put("id","$id-q${it+1}")}
        lessons.put(lesson);catalog.getJSONArray("lessons").put(JSONObject().put("lessonId",id).put("contentVersion",next).put("topicId",topic))
        val topics=catalog.getJSONArray("topics");repeat(topics.length()){val row=topics.getJSONObject(it);if(row.getString("id")==topic)row.put("contentStatus","AVAILABLE")}
        files["manifest.json"]=JSONObject(files.getValue("manifest.json").toString(Charsets.UTF_8)).put("catalogVersion",version).toString().toByteArray()
        files["catalog.json"]=catalog.toString().toByteArray();files["lessons.json"]=JSONObject().put("lessons",lessons).toString().toByteArray()
        val packed=ByteArrayOutputStream();PackageJson.zip(files,packed);service.import(packed.toByteArray().inputStream());return next
    }
    companion object {
        fun template(topic:String):String {
            val questions=JSONArray();repeat(3){i->questions.put(JSONObject().put("prompt","Pregunta ${i+1}: escribe aquí tu pregunta").put("objectCount",0).put("type","SINGLE_CHOICE")
                .put("payload",JSONObject().put("instructions","Lee y compara las opciones.").put("choices",JSONArray().put(JSONObject().put("id","a").put("text","Respuesta correcta")).put(JSONObject().put("id","b").put("text","Otra opción"))))
                .put("answerKey",JSONObject().put("choice","a")).put("options",JSONArray()).put("hints",JSONArray().put("Observa las palabras de la pregunta.").put("Compara cada opción con lo que se pregunta.")))}
            return JSONObject().put("topicId",topic).put("lesson",JSONObject().put("title","Mi nueva lección").put("objective","Escribe el objetivo de aprendizaje.").put("questions",questions)).toString(2)
        }
    }
}
