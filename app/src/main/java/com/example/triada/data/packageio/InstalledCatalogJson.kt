package com.example.triada.data.packageio

import com.example.triada.data.local.*
import org.json.JSONArray
import org.json.JSONObject

internal object InstalledCatalogJson {
    private fun row(vararg fields: Pair<String, Any?>) = JSONObject().apply { fields.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) } }
    private fun <T> rows(values: List<T>, block: (T) -> JSONObject) = JSONArray().apply { values.forEach { put(block(it)) } }
    suspend fun encode(db: LearningDatabase): String {
        val dao = db.catalogDao()
        return row("schemaVersion" to 1, "catalogVersion" to requireNotNull(dao.metadata()).catalogVersion,
            "grades" to rows(dao.grades()) { row("id" to it.id, "number" to it.number, "label" to it.label, "sortOrder" to it.sortOrder) },
            "subjects" to rows(dao.subjects()) { row("id" to it.id, "gradeId" to it.gradeId, "code" to it.code, "title" to it.title, "iconKey" to it.iconKey, "sortOrder" to it.sortOrder, "referenceCount" to it.referenceCount) },
            "units" to rows(dao.units()) { row("id" to it.id, "subjectId" to it.subjectId, "title" to it.title, "description" to it.description, "sortOrder" to it.sortOrder) },
            "topics" to rows(dao.topics()) { row("id" to it.id, "subjectId" to it.subjectId, "unitId" to it.unitId, "title" to it.title, "sourceOrder" to it.sourceOrder, "contentStatus" to it.contentStatus) },
            "lessons" to rows(dao.lessons()) { row("lessonId" to it.lessonId, "contentVersion" to it.contentVersion, "topicId" to it.topicId) },
        ).toString()
    }
    fun equivalent(left: String, right: String): Boolean {
        fun canonical(raw: String): com.example.triada.domain.model.CatalogBundle = CatalogParser.parse(raw).let {
            it.copy(grades = it.grades.sortedBy { r -> r.id }, subjects = it.subjects.sortedBy { r -> r.id }, units = it.units.sortedBy { r -> r.id },
                topics = it.topics.sortedBy { r -> r.id }, lessons = it.lessons.sortedWith(compareBy({ r -> r.lessonId }, { r -> r.contentVersion })))
        }
        return canonical(left) == canonical(right)
    }
}
