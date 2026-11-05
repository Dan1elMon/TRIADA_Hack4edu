package com.example.triada.domain.repository

import com.example.triada.domain.model.Evaluation
import com.example.triada.domain.model.LearnerProfile
import com.example.triada.domain.model.LessonSession
import com.example.triada.domain.model.LearningAnswer
import com.example.triada.domain.model.EarnedSticker
import kotlinx.coroutines.flow.Flow

interface LearningRepository {
    suspend fun dailyGoal(profileId:String):Pair<Int,Int> = 0 to 1
    suspend fun setDailyGoal(profileId:String,target:Int) { }
    suspend fun updateProfile(id: String, name: String, avatar: String)
    suspend fun rewards(profileId: String): List<EarnedSticker>
    fun profiles(): Flow<List<LearnerProfile>>
    suspend fun createProfile(name: String, avatar: String): LearnerProfile
    suspend fun openLesson(profileId: String, practiceAgain: Boolean = false, lessonId: String = "g1-mat-count-1-9"): LessonSession
    suspend fun session(attemptId: String, profileId: String): LessonSession
    suspend fun answer(attemptId: String, profileId: String, questionId: String, choice: Int, eventId: String): Evaluation
    suspend fun submitAnswer(attemptId: String, profileId: String, questionId: String, answer: LearningAnswer, eventId: String): Evaluation
    suspend fun requestHint(attemptId: String, profileId: String, questionId: String): LessonSession
    suspend fun advance(attemptId: String, profileId: String): LessonSession
}

interface ProfilePreferences {
    val lastProfileId: Flow<String?>
    suspend fun select(profileId: String)
}
