package com.example.cursorpad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cursorpad.ui.screens.ActivationStripEditor
import com.example.cursorpad.ui.screens.FAQScreen
import com.example.cursorpad.ui.screens.HomeScreen
import com.example.cursorpad.ui.screens.SettingsScreen
import com.example.cursorpad.ui.screens.TouchpadPositionEditor
import com.example.cursorpad.ui.screens.Tutorial
import com.example.cursorpad.ui.theme.CursorPadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CursorPadTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    composable(
                        "home",
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() },
                        popEnterTransition = { fadeIn() },
                        popExitTransition = { fadeOut() }
                    ) {
                        HomeScreen(
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onNavigateToTutorial = {
                                navController.navigate("tutorial")
                            },
                            onNavigateToFAQ = {
                                navController.navigate("faq")
                            }
                        )
                    }

                    composable(
                        "settings",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                    ) {
                        SettingsScreen(
                            onBackPressed = { navController.popBackStack() },
                            onNavigateToTouchpadPositionEditor = { side ->
                                navController.navigate("touchpad_position_editor/$side")
                            },
                            onNavigateToStripEditor = { side ->
                                navController.navigate("activation_strip_editor/$side")
                            }
                        )
                    }

                    composable(
                        "touchpad_position_editor/{side}",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                    ) { backStackEntry ->
                        val side = backStackEntry.arguments?.getString("side") ?: "shared"
                        TouchpadPositionEditor(
                            side = side,
                            onBackPressed = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "activation_strip_editor/{side}",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                    ) { backStackEntry ->
                        val side = backStackEntry.arguments?.getString("side") ?: "left"
                        ActivationStripEditor(
                            side = side,
                            onBackPressed = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "tutorial",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                        popExitTransition = { slideOutHorizontally( targetOffsetX = { it }) }
                    ) {
                        Tutorial(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }

                    composable(
                        "faq",
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                    ) {
                        FAQScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}