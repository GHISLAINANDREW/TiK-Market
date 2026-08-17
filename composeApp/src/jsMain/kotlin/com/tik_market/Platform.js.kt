package com.tik_market

actual fun getPlatformName(): String = "Web (JS)"

private fun dateNow(): Double = js("Date.now()").unsafeCast<Double>()

actual fun currentTimeMillis(): Long = dateNow().toLong()
