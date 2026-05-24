package com.studymate

import androidx.compose.runtime.Composable
import com.studymate.presentation.navigation.AppNavHost
import com.studymate.presentation.theme.StudyMateTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        StudyMateTheme {
            AppNavHost()
        }
    }
}
