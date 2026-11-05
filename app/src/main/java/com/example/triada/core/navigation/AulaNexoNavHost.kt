package com.example.triada.core.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.triada.feature.learning.*

@Composable
private fun LoadCatalog(viewModel: LearningViewModel, profileId: String) {
    LaunchedEffect(profileId) {
        if (viewModel.state.value.catalog?.profileId == profileId) viewModel.refreshCatalog(profileId)
        else viewModel.loadProfile(profileId)
    }
}

@Composable
fun AulaNexoNavHost(viewModel: LearningViewModel) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val back: () -> Unit = { navController.popBackStack() }
    fun arguments(vararg names: String) = names.map { name -> navArgument(name) { type = NavType.StringType } }
    fun segment(id: String) = Uri.encode(id)
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        NavHost(navController, startDestination = "profiles", modifier = Modifier.padding(padding)) {
            composable("profiles") {
                ProfilesScreen(state, viewModel::createProfile) { id ->
                    navController.navigate("map/${segment(id)}") { launchSingleTop = true }
                }
            }
            composable("map/{profileId}", arguments = arguments("profileId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                LoadCatalog(viewModel, id)
                GradesScreen(state, id, back, { grade -> navController.navigate("subjects/${segment(id)}/${segment(grade)}") }, { viewModel.loadProfile(id) })
            }
            composable("subjects/{profileId}/{gradeId}", arguments = arguments("profileId", "gradeId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                val grade = requireNotNull(entry.arguments?.getString("gradeId"))
                LoadCatalog(viewModel, id)
                SubjectsScreen(state, id, grade, back, { subject -> navController.navigate("units/${segment(id)}/${segment(subject)}") }, { viewModel.loadProfile(id) })
            }
            composable("units/{profileId}/{subjectId}", arguments = arguments("profileId", "subjectId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                val subject = requireNotNull(entry.arguments?.getString("subjectId"))
                LoadCatalog(viewModel, id)
                UnitsScreen(state, id, subject, back, { unit -> navController.navigate("lessons/${segment(id)}/${segment(unit)}") }, {
                    navController.navigate("reference/${segment(id)}/${segment(subject)}")
                }, { viewModel.loadProfile(id) })
            }
            composable("lessons/{profileId}/{unitId}", arguments = arguments("profileId", "unitId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                val unit = requireNotNull(entry.arguments?.getString("unitId"))
                LoadCatalog(viewModel, id)
                UnitLessonsScreen(state, id, unit, back, { lesson -> navController.navigate("overview/${segment(id)}/${segment(lesson)}") }, { viewModel.loadProfile(id) })
            }
            composable("reference/{profileId}/{subjectId}", arguments = arguments("profileId", "subjectId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                val subject = requireNotNull(entry.arguments?.getString("subjectId"))
                LoadCatalog(viewModel, id)
                ReferenceTopicsScreen(state, id, subject, back, { lesson -> navController.navigate("overview/${segment(id)}/${segment(lesson)}") }, { viewModel.loadProfile(id) })
            }
            composable("overview/{profileId}/{lessonId}", arguments = arguments("profileId", "lessonId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                val lesson = requireNotNull(entry.arguments?.getString("lessonId"))
                LaunchedEffect(id, lesson) { viewModel.loadLesson(id, lesson) }
                LessonOverviewScreen(state, id, lesson, back, {
                    navController.navigate("activity/${segment(id)}/${segment(lesson)}") { launchSingleTop = true }
                }, { viewModel.loadLesson(id, lesson) })
            }
            composable("activity/{profileId}/{lessonId}", arguments = arguments("profileId", "lessonId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                val lesson = requireNotNull(entry.arguments?.getString("lessonId"))
                LaunchedEffect(id, lesson) {
                    if (state.session?.profileId != id || state.session?.lessonId != lesson) viewModel.loadLesson(id, lesson)
                }
                ActivityScreen(state, id, lesson, back, viewModel::answer, viewModel::hint, viewModel::next,
                    viewModel::practiceAgain, { viewModel.loadLesson(id, lesson) })
            }
            // Restore navigation saved by the previous debug build without creating another graph.
            composable("lesson/{profileId}", arguments = arguments("profileId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                LaunchedEffect(id) {
                    navController.navigate("activity/${segment(id)}/g1-mat-count-1-9") {
                        popUpTo("lesson/{profileId}") { inclusive = true }
                    }
                }
            }
        }
    }
    BackHandler(enabled = state.busy) { /* Complete the confirmed local write before navigating. */ }
}
