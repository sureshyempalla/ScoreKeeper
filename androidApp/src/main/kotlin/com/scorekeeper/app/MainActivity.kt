package com.scorekeeper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.scorekeeper.AppController
import com.scorekeeper.AuthController
import com.scorekeeper.app.ui.ScoreKeeperApp
import com.scorekeeper.data.DatabaseDriverFactory
import com.scorekeeper.data.GameRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = GameRepository(DatabaseDriverFactory(applicationContext))
        val controller = AppController(repository)
        val authController = AuthController()

        setContent {
            ScoreKeeperApp(controller, authController)
        }
    }
}
