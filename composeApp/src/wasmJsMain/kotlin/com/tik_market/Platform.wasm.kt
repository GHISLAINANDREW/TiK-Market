package com.tik_market

actual fun getPlatformName(): String = "Web (Wasm)"

@JsFun("() => Date.now()")
private external fun dateNow(): Double

actual fun currentTimeMillis(): Long = dateNow().toLong()
