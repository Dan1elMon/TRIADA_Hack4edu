package com.example.triada.data.local

import android.content.Context
import com.example.triada.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

fun interface CatalogSource {
    fun read(): String
    fun hasAudio(key: String): Boolean = false
    fun expandedLessons():String? = null
}

class AssetCatalogSource(private val context: Context, private val fullCurriculum:Boolean=true) : CatalogSource {
    override fun read(): String = context.assets.open(if(fullCurriculum)"content/catalog-v3.json" else "content/catalog-v2.json").bufferedReader(Charsets.UTF_8).use { it.readText().removePrefix("\uFEFF") }
    override fun expandedLessons():String? = if(fullCurriculum)context.assets.open("content/expanded-lessons-v1.json").bufferedReader(Charsets.UTF_8).use{it.readText()} else null
    override fun hasAudio(key: String): Boolean {
        if (!key.matches(Regex("[a-z0-9-]{1,80}"))) return false
        return try { context.assets.open("audio/$key.wav").use { it.read() >= 0 } }
        catch (_: java.io.IOException) { java.io.File(context.filesDir, "content/audio/$key.wav").isFile }
    }
}

object CatalogParser {
    fun parse(raw: String): CatalogBundle {
        val root = JSONObject(raw)
        fun <T> JSONArray.mapRows(transform: (JSONObject) -> T): List<T> = (0 until length()).map { transform(getJSONObject(it)) }
        return CatalogBundle(
            root.getInt("schemaVersion"), root.getInt("catalogVersion"),
            root.getJSONArray("grades").mapRows { Grade(it.getString("id"), it.getInt("number"), it.getString("label"), it.getInt("sortOrder")) },
            root.getJSONArray("subjects").mapRows { Subject(it.getString("id"), it.getString("gradeId"), it.getString("code"), it.getString("title"), it.getString("iconKey"), it.getInt("sortOrder"), it.getInt("referenceCount")) },
            root.getJSONArray("units").mapRows { LearningUnit(it.getString("id"), it.getString("subjectId"), it.getString("title"), it.getString("description"), it.getInt("sortOrder")) },
            root.getJSONArray("topics").mapRows { Topic(it.getString("id"), it.getString("subjectId"), if (it.isNull("unitId")) null else it.getString("unitId"), it.getString("title"), it.getInt("sourceOrder"), ContentAvailability.valueOf(it.getString("contentStatus"))) },
            root.getJSONArray("lessons").mapRows { CatalogLessonLink(it.getString("lessonId"), it.getInt("contentVersion"), it.getString("topicId")) },
        ).also(CatalogValidator::validate)
    }
}
