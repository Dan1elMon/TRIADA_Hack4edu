package com.example.triada.domain.repository

import com.example.triada.domain.model.Evaluation
import com.example.triada.domain.model.LearnerProfile
import com.example.triada.domain.model.LessonSession
import kotlinx.coroutines.flow.Flow

interface LearningRepository {
    fun profiles(): Flow<List<LearnerProfile>>
    suspend fun createProfile(name: String, avatar: String): LearnerProfile
    suspend fun openLesson(profileId: String, practiceAgain: Boolean = false, lessonId: String = "g1-mat-count-1-9"): LessonSession
    suspend fun session(attemptId: String, profileId: String): LessonSession
    suspend fun answer(attemptId: String, profileId: String, questionId: String, choice: Int, eventId: String): Evaluation
    suspend fun requestHint(attemptId: String, profileId: String, questionId: String): LessonSession
    suspend fun advance(attemptId: String, profileId: String): LessonSession
}

interface ProfilePreferences {
    val lastProfileId: Flow<String?>
    suspend fun select(profileId: String)
}
