package com.example.triada.domain.model

data class LearnerProfile(val id: String, val displayName: String, val avatar: String)
data class CountQuestion(
    val id: String,
    val prompt: String,
    val objectCount: Int,
    val options: List<Int>,
    val hints: List<String>,
)
data class CountLesson(
    val id: String,
    val version: Int,
    val title: String,
    val objective: String,
    val questions: List<CountQuestion>,
)

/** Presentation data contains choices, never a correctness flag or answer key. */
data class QuestionPresentation(
    val id: String,
    val prompt: String,
    val objectCount: Int,
    val options: List<Int>,
    val type: ActivityType = ActivityType.COUNT_OBJECTS,
    val instructions: String = "Señala cada círculo y cuenta despacio.",
    val choices: List<ExerciseItem> = emptyList(),
    val sequence: List<ExerciseItem> = emptyList(),
    val left: List<ExerciseItem> = emptyList(),
    val right: List<ExerciseItem> = emptyList(),
    val tiles: List<ExerciseItem> = emptyList(),
    val cueAudioKey: String? = null,
    val illustrationKey: String? = null,
    val feedbackCorrect: String = "¡Bien! Contaste todos los círculos.",
    val feedbackIncorrect: String = "Contemos otra vez, despacio. Puedes pedir una pista.",
)
data class LessonSession(
    val attemptId: String,
    val profileId: String,
    val lessonId: String,
    val contentVersion: Int,
    val title: String,
    val objective: String,
    val currentIndex: Int,
    val totalQuestions: Int,
    val solvedQuestions: Int,
    val question: QuestionPresentation,
    val currentSolved: Boolean,
    val visibleHints: List<String>,
    val hintsExhausted: Boolean,
    val completed: Boolean,
    val bestSolvedQuestions: Int,
    val everCompleted: Boolean,
    val earnedSticker: EarnedSticker? = null,
    val currentErrors: Int = 0,
)

sealed interface Evaluation {
    data object Correct : Evaluation
    data object Incorrect : Evaluation
    data object Invalid : Evaluation
}

object CountEvaluator {
    fun evaluate(question: CountQuestion, selected: Int): Evaluation = when {
        selected !in question.options -> Evaluation.Invalid
        selected == question.objectCount -> Evaluation.Correct
        else -> Evaluation.Incorrect
    }
}

object ProgressiveHints {
    fun visible(hints: List<String>, used: Int): List<String> = hints.take(used.coerceIn(0, hints.size))
    fun nextCount(total: Int, used: Int): Int {
        require(total >= 0 && used in 0..total)
        return (used + 1).coerceAtMost(total)
    }
}
