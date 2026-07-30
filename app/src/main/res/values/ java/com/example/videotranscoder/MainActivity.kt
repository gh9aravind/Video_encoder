package com.example.videotranscoder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.videotranscoder.ui.screens.AboutScreen
import com.example.videotranscoder.ui.screens.TranscoderScreen
import com.example.videotranscoder.ui.theme.VideoTranscoderTheme
import com.example.videotranscoder.viewmodel.TranscoderViewModel

/**
 * MainActivity — the single Activity of the app.
 *
 * Architecture overview:
 *   MainActivity
 *   └── VideoTranscoderTheme (Material3 theme)
 *       └── NavHost
 *           ├── "transcoder" → TranscoderScreen(viewModel)
 *           └── "about"     → AboutScreen
 *
 * The [TranscoderViewModel] is created here (at the NavHost scope) so it
 * survives navigation between the transcoder and about screens.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw content behind the status bar and nav bar (edge-to-edge)
        enableEdgeToEdge()

        setContent {
            VideoTranscoderTheme {
                val navController = rememberNavController()

                // Single ViewModel for the whole app — stays alive across screens
                val viewModel: TranscoderViewModel = viewModel()

                NavHost(
                    navController    = navController,
                    startDestination = "transcoder"
                ) {
                    composable("transcoder") {
                        TranscoderScreen(
                            viewModel           = viewModel,
                            onNavigateToAbout   = { navController.navigate("about") }
                        )
                    }

                    composable("about") {
                        AboutScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
