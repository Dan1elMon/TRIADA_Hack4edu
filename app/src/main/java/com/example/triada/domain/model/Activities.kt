package com.example.triada.domain.model

import java.util.Locale

enum class ActivityType { COUNT_OBJECTS, SINGLE_CHOICE, SEQUENCE, MATCH_PAIRS, WORD_BUILDING, UNKNOWN }
enum class WordNormalization { EXACT, UPPERCASE }
data class ExerciseItem(val id: String, val text: String, val symbol: String? = null, val color: String? = null, val illustrationKey: String? = null, val audioKey: String? = null)
data class PairSelection(val leftId: String, val rightId: String)
sealed interface LearningAnswer {
    data class Choice(val id: String) : LearningAnswer
    data class Pairs(val values: List<PairSelection>) : LearningAnswer
    data class Letters(val tileIds: List<String>) : LearningAnswer
}

/** Answer keys stay in the domain/data layer. Only [content] is projected to UI. */
data class ExerciseDefinition(
    val content: QuestionPresentation,
    val correctChoiceId: String? = null,
    val correctPairs: Map<String, String> = emptyMap(),
    val correctWord: String? = null,
    val normalization: WordNormalization = WordNormalization.EXACT,
)

object ExerciseEvaluator {
    fun evaluate(definition: ExerciseDefinition, answer: LearningAnswer): Evaluation {
        val question = definition.content
        return when (question.type) {
            ActivityType.COUNT_OBJECTS -> {
                val selected = (answer as? LearningAnswer.Choice)?.id?.toIntOrNull() ?: return Evaluation.Invalid
                if (selected !in question.options) Evaluation.Invalid else result(selected == question.objectCount)
            }
            ActivityType.SINGLE_CHOICE, ActivityType.SEQUENCE -> {
                val selected = (answer as? LearningAnswer.Choice)?.id ?: return Evaluation.Invalid
                if (question.choices.none { it.id == selected }) Evaluation.Invalid else result(selected == definition.correctChoiceId)
            }
            ActivityType.MATCH_PAIRS -> {
                val selected = (answer as? LearningAnswer.Pairs)?.values ?: return Evaluation.Invalid
                val left = question.left.map { it.id }.toSet()
                val right = question.right.map { it.id }.toSet()
                if (selected.size != left.size || selected.map { it.leftId }.toSet() != left || selected.map { it.rightId }.toSet() != right) return Evaluation.Invalid
                result(selected.associate { it.leftId to it.rightId } == definition.correctPairs)
            }
            ActivityType.WORD_BUILDING -> {
                val selected = (answer as? LearningAnswer.Letters)?.tileIds ?: return Evaluation.Invalid
                val tiles = question.tiles.associateBy { it.id }
                if (selected.isEmpty() || selected.distinct().size != selected.size || selected.any { it !in tiles }) return Evaluation.Invalid
                fun normalize(word: String) = when (definition.normalization) {
                    WordNormalization.EXACT -> word
                    WordNormalization.UPPERCASE -> word.uppercase(Locale.ROOT)
                }
                result(normalize(selected.joinToString("") { tiles.getValue(it).text }) == normalize(requireNotNull(definition.correctWord)))
            }
            ActivityType.UNKNOWN -> Evaluation.Invalid
        }
    }
    private fun result(correct: Boolean) = if (correct) Evaluation.Correct else Evaluation.Incorrect
}

object ExerciseValidator {
    fun validate(definition: ExerciseDefinition) {
        val q = definition.content
        require(q.id.isNotBlank() && q.prompt.isNotBlank() && q.instructions.isNotBlank())
        require(q.feedbackCorrect.isNotBlank() && q.feedbackIncorrect.isNotBlank())
        require(q.prompt.length <= 2000 && q.instructions.length <= 2000 && q.feedbackCorrect.length <= 2000 && q.feedbackIncorrect.length <= 2000)
        val items = q.choices + q.sequence + q.left + q.right + q.tiles
        require(listOf(q.choices,q.sequence,q.left,q.right,q.tiles).all { it.size <= 24 })
        require(items.all { it.text.length <= 1000 && it.id.length <= 160 })
        val supportedImages = setOf("sun","hand","foot","eye","ear","mouth","nose")
        require((listOfNotNull(q.illustrationKey) + items.mapNotNull { it.illustrationKey }).all { it in supportedImages })
        require((q.left + q.right + q.tiles + q.sequence).all { it.audioKey == null })
        fun valid(items: List<ExerciseItem>) {
            require(items.isNotEmpty() && items.all { it.id.isNotBlank() && it.text.isNotBlank() })
            require(items.map { it.id }.distinct().size == items.size)
        }
        when (q.type) {
            ActivityType.COUNT_OBJECTS -> require(q.objectCount in 1..9 && q.options.distinct().size == q.options.size && q.options.count { it == q.objectCount } == 1)
            ActivityType.SINGLE_CHOICE, ActivityType.SEQUENCE -> {
                valid(q.choices)
                require(q.choices.count { it.id == definition.correctChoiceId } == 1)
                if (q.type == ActivityType.SEQUENCE) require(q.sequence.isNotEmpty() && q.sequence.all { it.symbol != null && it.text.isNotBlank() })
            }
            ActivityType.MATCH_PAIRS -> {
                valid(q.left); valid(q.right)
                require(definition.correctPairs.keys == q.left.map { it.id }.toSet())
                require(definition.correctPairs.values.toSet() == q.right.map { it.id }.toSet() && q.left.size == q.right.size)
            }
            ActivityType.WORD_BUILDING -> {
                valid(q.tiles)
                val word = requireNotNull(definition.correctWord)
                require(word.isNotBlank())
                fun normalized(text: String) = if (definition.normalization == WordNormalization.UPPERCASE) text.uppercase(Locale.ROOT) else text
                val available = normalized(q.tiles.joinToString("") { it.text }).groupingBy { it }.eachCount()
                require(normalized(word).groupingBy { it }.eachCount().all { (letter, count) -> (available[letter] ?: 0) >= count })
            }
            ActivityType.UNKNOWN -> error("Unsupported activity type")
        }
    }
}
