package com.studymate.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.studymate.presentation.screens.add.AddEditScreen
import com.studymate.presentation.screens.ai.AIAssistantScreen
import com.studymate.presentation.screens.calendar.CalendarScreen
import com.studymate.presentation.screens.detail.NoteDetailScreen
import com.studymate.presentation.screens.home.HomeScreen
import com.studymate.presentation.screens.notes.NotesScreen
import com.studymate.presentation.screens.profile.ProfileScreen
import com.studymate.presentation.screens.quiz.QuizScreen
import com.studymate.presentation.screens.quiz.QuizViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navigationActions = createNavigationActions(navController)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavRoutes = listOf(
        BottomNavDestination.Home,
        BottomNavDestination.Notes,
        BottomNavDestination.Quiz,
        BottomNavDestination.Calendar,
        BottomNavDestination.Profile
    )

    val showBottomBar = bottomNavRoutes.any { destination ->
        currentDestination?.hierarchy?.any { it.route?.contains(destination.route::class.simpleName ?: "") == true } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavRoutes.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route?.contains(destination.route::class.simpleName ?: "") == true } == true
                        NavigationBarItem(
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = modifier.padding(innerPadding)
        ) {
            composable<Route.Home> {
                HomeScreen(
                    onNavigateToAddNote = { navigationActions.navigateToAddNote() },
                    onNavigateToDetail = { noteId -> navigationActions.navigateToNoteDetail(noteId) },
                    onNavigateToAI = { navigationActions.navigateToAIAssistant() }
                )
            }

            composable<Route.Notes> {
                NotesScreen(
                    onNavigateToAddNote = { navigationActions.navigateToAddNote() },
                    onNavigateToDetail = { noteId -> navigationActions.navigateToNoteDetail(noteId) }
                )
            }

            composable<Route.Quiz> { backStackEntry ->
                val route: Route.Quiz = backStackEntry.toRoute()
                val viewModel: QuizViewModel = koinViewModel()
                
                LaunchedEffect(route.content) {
                    route.content?.let { viewModel.generateQuiz(it) }
                }
                
                QuizScreen(
                    onBackClick = { navigationActions.navigateBack() },
                    viewModel = viewModel
                )
            }

            composable<Route.Calendar> {
                CalendarScreen()
            }

            composable<Route.Profile> {
                ProfileScreen()
            }

            composable<Route.AddNote> { backStackEntry ->
                val route: Route.AddNote = backStackEntry.toRoute()
                AddEditScreen(
                    noteId = route.noteId,
                    onNavigateBack = { navigationActions.navigateBack() },
                    onNavigateToAI = { text ->
                        navigationActions.navigateToAIAssistant(
                            noteId = route.noteId,
                            initialText = text
                        )
                    }
                )
            }

            composable<Route.NoteDetail> { backStackEntry ->
                val route: Route.NoteDetail = backStackEntry.toRoute()
                NoteDetailScreen(
                    noteId = route.noteId,
                    onNavigateBack = { navigationActions.navigateBack() },
                    onNavigateToEdit = { navigationActions.navigateToAddNote(route.noteId) },
                    onNavigateToQuiz = { content -> navigationActions.navigateToQuiz(content) },
                    onShare = { _ -> }
                )
            }

            composable<Route.AIAssistant> { backStackEntry ->
                val route: Route.AIAssistant = backStackEntry.toRoute()
                AIAssistantScreen(
                    noteId = route.noteId,
                    initialText = route.initialText,
                    onNavigateBack = { navigationActions.navigateBack() },
                    onApplyResult = null
                )
            }
        }
    }
}

private sealed class BottomNavDestination(val route: Route, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    data object Home : BottomNavDestination(Route.Home, Icons.Default.Home, "Beranda")
    data object Notes : BottomNavDestination(Route.Notes, Icons.Default.Notes, "Catatan")
    data object Quiz : BottomNavDestination(Route.Quiz(), Icons.Default.Psychology, "Belajar")
    data object Calendar : BottomNavDestination(Route.Calendar, Icons.Default.CalendarMonth, "Kalender")
    data object Profile : BottomNavDestination(Route.Profile, Icons.Default.Person, "Profil")
}

private fun createNavigationActions(navController: NavHostController): NavigationActions {
    return object : NavigationActions {
        override fun navigateToHome() {
            navController.navigate(Route.Home) {
                popUpTo(Route.Home) { inclusive = true }
            }
        }

        override fun navigateToNotes() {
            navController.navigate(Route.Notes)
        }

        override fun navigateToQuiz(content: String?) {
            navController.navigate(Route.Quiz(content))
        }

        override fun navigateToCalendar() {
            navController.navigate(Route.Calendar)
        }

        override fun navigateToProfile() {
            navController.navigate(Route.Profile)
        }

        override fun navigateToAddNote(noteId: Long?) {
            navController.navigate(Route.AddNote(noteId))
        }

        override fun navigateToNoteDetail(noteId: Long) {
            navController.navigate(Route.NoteDetail(noteId))
        }

        override fun navigateToAIAssistant(noteId: Long?, initialText: String?) {
            navController.navigate(Route.AIAssistant(noteId, initialText))
        }

        override fun navigateBack() {
            navController.popBackStack()
        }
    }
}
