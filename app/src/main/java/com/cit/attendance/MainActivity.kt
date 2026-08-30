package com.cit.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.designsystem.theme.CitTheme
import com.cit.attendance.core.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash hands over to the branded splash composable, so the
        // transition from launcher icon to brand mark has no white flash.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CitTheme {
                val isSignedOut by viewModel.isSignedOut.collectAsStateWithLifecycle()
                AppNavHost(
                    institutionName = viewModel.institutionConfig.name,
                    isSignedOut = isSignedOut,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background),
                )
            }
        }
    }
}
