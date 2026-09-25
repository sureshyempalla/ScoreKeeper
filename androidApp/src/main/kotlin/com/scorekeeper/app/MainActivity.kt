package com.scorekeeper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.scorekeeper.AppController
import com.scorekeeper.AuthController
import com.scorekeeper.app.ui.ScoreKeeperApp
import com.scorekeeper.data.DatabaseDriverFactory
import com.scorekeeper.data.GameRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 (targetSdk 35) forces edge-to-edge regardless of this call, so without
        // it content was drawing under the status bar and behind the gesture nav bar on every
        // screen -- this call plus the systemBarsPadding() in App.kt is the actual fix; this
        // just makes the system bar icon contrast follow the app's (light) theme too.
        enableEdgeToEdge()

        val repository = GameRepository(DatabaseDriverFactory(applicationContext))
        val controller = AppController(repository)
        val authController = AuthController()

        setContent {
            ScoreKeeperApp(controller, authController)
        }
    }
}
