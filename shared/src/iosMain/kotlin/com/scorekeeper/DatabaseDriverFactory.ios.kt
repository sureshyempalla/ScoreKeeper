package com.scorekeeper.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.scorekeeper.db.ScoreKeeperDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(ScoreKeeperDatabase.Schema, "scorekeeper.db")
}
