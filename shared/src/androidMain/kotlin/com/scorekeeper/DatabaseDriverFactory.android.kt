package com.scorekeeper.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.scorekeeper.db.ScoreKeeperDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(ScoreKeeperDatabase.Schema, context, "scorekeeper.db")
}
