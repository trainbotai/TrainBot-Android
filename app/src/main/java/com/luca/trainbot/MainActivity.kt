package com.luca.trainbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import com.luca.trainbot.ui.navigation.TrainBotNavGraph
import com.luca.trainbot.ui.theme.TrainBotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TrainBotApplication).container

        setContent {
            TrainBotTheme {
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    TrainBotNavGraph(authRepository = container.authRepository)
                }
            }
        }
    }
}
