package com.tik_market.db

import app.cash.sqldelight.db.SqlDriver

expect object DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

object DatabaseHolder {
    private var database: TikMarketDatabase? = null

    fun getDatabase(): TikMarketDatabase {
        if (database == null) {
            database = TikMarketDatabase(DatabaseDriverFactory.createDriver())
        }
        return database!!
    }
}
