package com.tik_market.db

import app.cash.sqldelight.db.SqlDriver
// import app.cash.sqldelight.driver.worker.WebWorkerDriver
// import org.w3c.dom.Worker

actual object DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // En attendant une implémentation stable pour WasmJs, on peut utiliser un driver en mémoire
        // ou une erreur explicite si le cache n'est pas supporté sur le web.
        throw UnsupportedOperationException("SQLDelight driver not yet implemented for WasmJs")
    }
}
