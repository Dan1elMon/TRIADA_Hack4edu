package com.example.triada.domain.model

data class StickerDefinition(val id: String, val lessonId: String, val title: String, val category: String, val symbol: String)
data class EarnedSticker(val profileId: String, val stickerId: String, val obtainedAt: Long)

object StickerCatalog {
    val all = listOf(
        StickerDefinition("counting-sun", "g1-mat-count-1-9", "Sol de los números", "Sunny Sun", "☀"),
        StickerDefinition("letter-cat", "g1-com-vowels", "Gato de las letras", "Cosmic Cat", "🐱"),
        StickerDefinition("body-bear", "g1-cya-body", "Oso explorador", "Bouncing Bear", "🐻"),
        StickerDefinition("kind-sun", "g1-pso-coexistence", "Sol de la amistad", "Sunny Sun", "☀"),
        StickerDefinition("pattern-cat", "g1-rma-patterns", "Gato de los patrones", "Cosmic Cat", "🐱"),
        StickerDefinition("sound-bear", "g1-rve-initial-sounds", "Oso de los sonidos", "Bouncing Bear", "🐻"),
    )
    fun forLesson(id: String): StickerDefinition? = all.firstOrNull { it.lessonId == id }
}

data class ProgressOverview(val completedLessons: Int, val totalLessons: Int, val solvedExercises: Int, val totalExercises: Int) {
    val fraction: Float get() = if (totalExercises == 0) 0f else solvedExercises.toFloat() / totalExercises
    companion object {
        fun from(catalog: LearningCatalog) = ProgressOverview(
            catalog.lessons.count { it.status == LearningStatus.COMPLETED }, catalog.lessons.size,
            catalog.lessons.sumOf { it.solvedQuestions.coerceIn(0, it.totalQuestions) }, catalog.lessons.sumOf { it.totalQuestions },
        )
    }
}
