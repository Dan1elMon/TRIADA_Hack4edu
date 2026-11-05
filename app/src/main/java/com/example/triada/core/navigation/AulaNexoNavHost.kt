package com.example.triada.core.navigation

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
import com.example.triada.feature.learning.ActivityScreen
import com.example.triada.feature.learning.LearningViewModel
import com.example.triada.feature.learning.MapScreen
import com.example.triada.feature.learning.ProfilesScreen

@Composable
fun AulaNexoNavHost(viewModel: LearningViewModel) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.busy) { /* Finish the confirmed local write before navigating. */ }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        NavHost(navController, startDestination = "profiles", modifier = Modifier.padding(padding)) {
            composable("profiles") {
                ProfilesScreen(state, viewModel::createProfile) { profileId ->
                    navController.navigate("map/$profileId") { launchSingleTop = true }
                }
            }
            composable("map/{profileId}", arguments = listOf(navArgument("profileId") { type = NavType.StringType })) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                LaunchedEffect(id) { viewModel.loadProfile(id) }
                MapScreen(state, id, { navController.popBackStack() }, {
                    navController.navigate("lesson/$id") { launchSingleTop = true }
                }, { viewModel.loadProfile(id) })
            }
            composable("lesson/{profileId}", arguments = listOf(navArgument("profileId") { type = NavType.StringType })) { entry ->
                val id = requireNotNull(entry.arguments?.getString("profileId"))
                LaunchedEffect(id) {
                    if (state.session?.profileId != id) viewModel.loadProfile(id)
                }
                ActivityScreen(
                    state, id, { navController.popBackStack() }, viewModel::answer, viewModel::hint,
                    viewModel::next, viewModel::practiceAgain, { viewModel.loadProfile(id) },
                )
            }
        }
    }
}
