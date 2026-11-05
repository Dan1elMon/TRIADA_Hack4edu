package com.example.triada.feature.learning

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triada.domain.model.Evaluation
import com.example.triada.domain.model.LearnerProfile
import com.example.triada.domain.model.LessonSession
import com.example.triada.domain.repository.LearningRepository
import com.example.triada.domain.repository.ProfilePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LearningUiState(
    val profiles: List<LearnerProfile> = emptyList(),
    val lastProfileId: String? = null,
    val loadingProfiles: Boolean = true,
    val activeProfileId: String? = null,
    val session: LessonSession? = null,
    val busy: Boolean = false,
    val feedback: String? = null,
    val error: String? = null,
)

@HiltViewModel
class LearningViewModel @Inject constructor(
    private val repository: LearningRepository,
    private val preferences: ProfilePreferences,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LearningUiState())
    val state = mutableState.asStateFlow()
    private var pendingAnswer: PendingAnswer? = null

    init { observeProfiles() }

    private fun observeProfiles() {
        viewModelScope.launch {
            try {
                combine(repository.profiles(), preferences.lastProfileId) { profiles, last -> profiles to last }
                    .collect { (profiles, last) ->
                        mutableState.update { it.copy(profiles = profiles, lastProfileId = last, loadingProfiles = false) }
                    }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("AulaNexo", "Could not read profiles", error)
                mutableState.update { it.copy(loadingProfiles = false, error = "No pudimos leer los perfiles. Vuelve a abrir la aplicación para reintentar.") }
            }
        }
    }

    fun createProfile(name: String, avatar: String) = action {
        repository.createProfile(name, avatar)
        mutableState.update { it.copy(feedback = "¡Perfil creado! Selecciona tu tarjeta para comenzar.") }
    }

    fun loadProfile(profileId: String) = action {
        mutableState.update { it.copy(activeProfileId = profileId, session = null, feedback = null) }
        pendingAnswer = null
        val session = repository.openLesson(profileId)
        preferences.select(profileId)
        mutableState.update { it.copy(session = session) }
    }

    fun answer(choice: Int) {
        val session = state.value.session ?: return
        if (session.currentSolved || session.completed) return
        val previous = pendingAnswer
        val event = if (previous != null && previous.attemptId == session.attemptId && previous.questionId == session.question.id && previous.choice == choice) previous else {
            PendingAnswer(UUID.randomUUID().toString(), session.attemptId, session.question.id, choice)
        }
        action {
            pendingAnswer = event
            val evaluation = repository.answer(session.attemptId, session.profileId, session.question.id, choice, event.id)
            val refreshed = repository.session(session.attemptId, session.profileId)
            pendingAnswer = null
            mutableState.update {
                it.copy(session = refreshed, feedback = when (evaluation) {
                    Evaluation.Correct -> "¡Bien! Contaste todos los círculos."
                    Evaluation.Incorrect -> "Contemos otra vez, despacio. Puedes pedir una pista."
                    Evaluation.Invalid -> "Elige uno de los números disponibles."
                })
            }
        }
    }

    fun hint() {
        val session = state.value.session ?: return
        action {
            val refreshed = repository.requestHint(session.attemptId, session.profileId, session.question.id)
            mutableState.update { it.copy(session = refreshed, feedback = null) }
        }
    }

    fun next() {
        val session = state.value.session ?: return
        action {
            val refreshed = repository.advance(session.attemptId, session.profileId)
            mutableState.update { it.copy(session = refreshed, feedback = null) }
        }
    }

    fun practiceAgain() {
        val session = state.value.session ?: return
        action {
            val refreshed = repository.openLesson(session.profileId, practiceAgain = true)
            mutableState.update { it.copy(session = refreshed, feedback = null) }
        }
    }

    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { block() } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("AulaNexo", "Learning operation failed", error)
                mutableState.update { it.copy(error = "No pudimos guardar o cargar el avance. Inténtalo de nuevo.") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private data class PendingAnswer(val id: String, val attemptId: String, val questionId: String, val choice: Int)
}
