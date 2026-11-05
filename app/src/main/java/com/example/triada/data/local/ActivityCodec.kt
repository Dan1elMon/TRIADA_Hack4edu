package com.example.triada.data.local

import com.example.triada.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

object ActivityCodec {
    fun decodeAnswer(row: JSONObject): LearningAnswer = when (row.getString("type")) {
        "choice" -> LearningAnswer.Choice(row.getString("id"))
        "letters" -> row.getJSONArray("ids").let { values -> LearningAnswer.Letters((0 until values.length()).map { values.getString(it) }) }
        "pairs" -> row.getJSONArray("values").let { values -> LearningAnswer.Pairs((0 until values.length()).map { values.getJSONObject(it).let { pair -> PairSelection(pair.getString("left"), pair.getString("right")) } }) }
        else -> error("Unknown answer type")
    }
    private fun encodeItems(items: List<ExerciseItem>) = JSONArray().apply {
        items.forEach { item -> put(JSONObject().put("id", item.id).put("text", item.text)
            .put("symbol", item.symbol).put("color", item.color).put("illustrationKey", item.illustrationKey).put("audioKey", item.audioKey)) }
    }
    private fun items(root: JSONObject, key: String): List<ExerciseItem> {
        val rows = root.optJSONArray(key) ?: return emptyList()
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            fun optional(key: String) = if (row.has(key) && !row.isNull(key)) row.getString(key) else null
            ExerciseItem(row.getString("id"), row.getString("text"), optional("symbol"), optional("color"), optional("illustrationKey"), optional("audioKey"))
        }
    }
    fun payload(q: QuestionPresentation): String = JSONObject()
        .put("instructions", q.instructions).put("choices", encodeItems(q.choices)).put("sequence", encodeItems(q.sequence))
        .put("left", encodeItems(q.left)).put("right", encodeItems(q.right)).put("tiles", encodeItems(q.tiles))
        .put("cueAudioKey", q.cueAudioKey).put("illustrationKey", q.illustrationKey)
        .put("feedbackCorrect", q.feedbackCorrect).put("feedbackIncorrect", q.feedbackIncorrect).toString()

    fun key(definition: ExerciseDefinition): String = JSONObject().put("choice", definition.correctChoiceId)
        .put("pairs", JSONObject(definition.correctPairs)).put("word", definition.correctWord).put("normalization", definition.normalization.name).toString()

    fun decode(row: QuestionEntity, options: List<Int>): ExerciseDefinition {
        val data = JSONObject(row.payloadJson)
        val key = JSONObject(row.answerKeyJson)
        fun optional(root: JSONObject, name: String) = if (root.has(name) && !root.isNull(name)) root.getString(name) else null
        val q = QuestionPresentation(row.id, row.prompt, row.objectCount, options,
            ActivityType.entries.firstOrNull { it.name == row.activityType } ?: ActivityType.UNKNOWN,
            data.optString("instructions", "Señala cada círculo y cuenta despacio."),
            items(data, "choices"), items(data, "sequence"), items(data, "left"), items(data, "right"), items(data, "tiles"),
            optional(data, "cueAudioKey"), optional(data, "illustrationKey"),
            data.optString("feedbackCorrect", "¡Bien! Contaste todos los círculos."),
            data.optString("feedbackIncorrect", "Contemos otra vez, despacio. Puedes pedir una pista."),
        )
        val pairs = key.optJSONObject("pairs") ?: JSONObject()
        return ExerciseDefinition(q, optional(key, "choice"), pairs.keys().asSequence().associateWith { pairs.getString(it) }, optional(key, "word"), WordNormalization.valueOf(key.optString("normalization", "EXACT")))
    }

    fun answer(answer: LearningAnswer): String = when (answer) {
        is LearningAnswer.Choice -> JSONObject().put("type", "choice").put("id", answer.id)
        is LearningAnswer.Pairs -> JSONObject().put("type", "pairs").put("values", JSONArray().apply {
            answer.values.sortedBy { it.leftId }.forEach { put(JSONObject().put("left", it.leftId).put("right", it.rightId)) }
        })
        is LearningAnswer.Letters -> JSONObject().put("type", "letters").put("ids", JSONArray(answer.tileIds))
    }.toString()
}
