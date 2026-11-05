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
import androidx.navigation.compose.currentBackStackEntryAsState
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
fun AulaNexoNavHost(viewModel: LearningViewModel, packages: PackageViewModel? = null, school: SchoolViewModel? = null) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val back: () -> Unit = { navController.popBackStack() }
    fun arguments(vararg names: String) = names.map { name -> navArgument(name) { type = NavType.StringType } }
    fun segment(id: String) = Uri.encode(id)
    val schoolState = school?.state?.collectAsStateWithLifecycle()?.value
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry, schoolState?.active?.id) {
        if (school != null) {
            val route=currentEntry?.destination?.route
            val profile=currentEntry?.arguments?.getString("profileId")
            val actor=schoolState?.active
            val denied=route=="profiles" || (route!=null && route!="accounts" && actor==null) ||
                (profile!=null && (actor?.role!=com.example.triada.domain.model.SchoolRole.STUDENT || actor.profileId!=profile))
            if(denied) navController.navigate("accounts"){popUpTo(navController.graph.id){inclusive=false};launchSingleTop=true}
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        NavHost(navController, startDestination = if(school==null) "profiles" else "accounts", modifier = Modifier.padding(padding)) {
            composable("accounts") {
                if(school!=null && schoolState!=null) {
                    LaunchedEffect(schoolState.active?.id) { if(schoolState.active!=null) navController.navigate("school-home"){launchSingleTop=true} }
                    AccountsScreen(schoolState,school::login,school::refresh)
                }
            }
            composable("school-home") {
                if(school!=null && schoolState!=null) SchoolHomeScreen(schoolState,{
                    school.logout(); navController.navigate("accounts"){popUpTo("accounts"){inclusive=true}}
                },{id->navController.navigate("map/${segment(id)}")},{id->navController.navigate("grades/${segment(id)}")},school::link,school::edit,{navController.navigate("credentials")},school::create)
            }
            composable("credentials") {
                if(school!=null&&schoolState?.active!=null) CredentialsScreen(schoolState,back,school::changeCredentials)
            }
            composable("grades/{studentId}",arguments=arguments("studentId")) { entry ->
                val student=requireNotNull(entry.arguments?.getString("studentId"))
                if(school!=null && schoolState!=null) {
                    LaunchedEffect(student,schoolState.active?.id){if(schoolState.active!=null)school.loadGrades(student)}
                    GradesScreen(schoolState,student,back,{school.loadGrades(student)})
                }
            }
            composable("profiles") {
                ProfilesScreen(state, viewModel::createProfile, viewModel::editProfile) { id ->
                    navController.navigate("map/${segment(id)}") { launchSingleTop = true }
                }
            }
            composable("map/{profileId}", arguments = arguments("profileId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                LoadCatalog(viewModel, id)
                GradesScreen(state, id, back, { grade -> navController.navigate("subjects/${segment(id)}/${segment(grade)}") }, { viewModel.loadProfile(id) },
                    { navController.navigate("album/${segment(id)}") }, { navController.navigate("progress/${segment(id)}") },
                    if (packages == null) null else { { navController.navigate("packages/${segment(id)}") } })
            }
            composable("packages/{profileId}", arguments = arguments("profileId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                if (packages != null) PackageScreen(packages, id, back)
            }
            // Previous builds saved this route without a profile. Require a fresh selection.
            composable("packages") {
                LaunchedEffect(Unit) {
                    navController.navigate("profiles") { popUpTo("profiles") { inclusive = true } }
                }
            }
            composable("album/{profileId}", arguments = arguments("profileId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                LoadCatalog(viewModel, id)
                StickerAlbumScreen(state, id, back, { viewModel.loadProfile(id) })
            }
            composable("progress/{profileId}", arguments = arguments("profileId")) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                LoadCatalog(viewModel, id)
                ProgressScreen(state, id, back, { viewModel.loadProfile(id) }, { lesson -> navController.navigate("overview/${segment(id)}/${segment(lesson)}") })
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
                ActivityScreen(state, id, lesson, back, viewModel::submitAnswer, viewModel::hint, viewModel::next,
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
