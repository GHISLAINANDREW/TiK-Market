package com.tik_market.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual object DatabaseDriverFactory {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun createDriver(): SqlDriver {
        val context = appContext ?: throw IllegalStateException("DatabaseDriverFactory.init(context) must be called")
        return AndroidSqliteDriver(TikMarketDatabase.Schema, context, "tik_market.db")
    }
}
