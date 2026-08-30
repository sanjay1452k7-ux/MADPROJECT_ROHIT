package com.cit.attendance.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.feature.auth.LoginScreen
import com.cit.attendance.feature.auth.RoleSelectionScreen
import com.cit.attendance.feature.shell.AdminShell
import com.cit.attendance.feature.shell.StudentShell
import com.cit.attendance.feature.shell.TeacherShell
import com.cit.attendance.feature.splash.SplashScreen

/**
 * The application's single navigation graph.
 *
 * Signing in and signing out both clear the back stack, so the system back
 * gesture can never return to a screen the user no longer has a session for.
 */
@Composable
fun AppNavHost(
    institutionName: String,
    isSignedOut: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val motion = AppTheme.motion

    // Signing out from anywhere returns to role selection.
    LaunchedEffect(isSignedOut) {
        if (isSignedOut) {
            val current = navController.currentDestination?.route
            if (current != null && current != Routes.SPLASH && current != Routes.ROLE_SELECTION) {
                navController.navigate(Routes.ROLE_SELECTION) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(motion.duration(motion.standard))) +
                slideInHorizontally(tween(motion.duration(motion.standard))) { it / 12 }
        },
        exitTransition = { fadeOut(tween(motion.duration(motion.quick))) },
        popEnterTransition = { fadeIn(tween(motion.duration(motion.standard))) },
        popExitTransition = {
            fadeOut(tween(motion.duration(motion.quick))) +
                slideOutHorizontally(tween(motion.duration(motion.quick))) { it / 12 }
        },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                institutionName = institutionName,
                onReady = { user ->
                    val destination = user?.let { Routes.rootFor(it.role) }
                        ?: Routes.ROLE_SELECTION
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(
                institutionName = institutionName,
                onContinue = { role -> navController.navigate(Routes.login(role)) },
            )
        }

        composable(
            route = Routes.LOGIN_PATTERN,
            arguments = listOf(
                navArgument(Routes.LOGIN_ARG_ROLE) { type = NavType.StringType },
            ),
        ) {
            LoginScreen(
                onSignedIn = { user ->
                    navController.navigate(Routes.rootFor(user.role)) {
                        // The credential screens must not remain on the stack.
                        popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.STUDENT_ROOT) { StudentShell() }
        composable(Routes.TEACHER_ROOT) { TeacherShell() }
        composable(Routes.ADMIN_ROOT) { AdminShell() }
    }
}
