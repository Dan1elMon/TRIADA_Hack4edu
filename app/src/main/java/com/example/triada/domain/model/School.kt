package com.example.triada.domain.model

enum class SchoolRole { STUDENT, TEACHER }
data class SchoolAccount(val id: String, val name: String, val role: SchoolRole, val profileId: String?, val avatar: String = "star", val username: String = "")
data class PracticeGrade(val attemptId: String, val lesson: String, val version: Int, val completedAt: Long?, val firstCorrect: Int, val total: Int, val hints: Int) {
    val score: Double? get() = if (completedAt == null) null else GradePolicy.score(firstCorrect,total)
}
object GradePolicy {
    fun score(correct: Int, total: Int): Double {
        require(total > 0 && correct in 0..total)
        return kotlin.math.round(20.0 * correct / total * 10) / 10
    }
}

