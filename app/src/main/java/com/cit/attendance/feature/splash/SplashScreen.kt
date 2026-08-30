package com.cit.attendance.feature.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.branding.ButterflyMark
import com.cit.attendance.core.designsystem.branding.CitBrandLockup
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.domain.model.AuthenticatedUser

/**
 * The first thing the user sees: brand mark, butterfly, nothing else
 * (Section 8). It holds only as long as the session restore needs.
 */
@Composable
fun SplashScreen(
    institutionName: String,
    onReady: (AuthenticatedUser?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val motion = AppTheme.motion

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(motion.duration(motion.deliberate)),
        label = "splashFade",
    )

    LaunchedEffect(state) {
        val current = state
        if (current is SplashState.Ready) {
            onReady(current.user)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xl),
            modifier = Modifier.alpha(fade),
        ) {
            ButterflyMark(modifier = Modifier.size(150.dp), emphasis = 0.35f)
            CitBrandLockup(institutionName = institutionName)
        }
    }
}
