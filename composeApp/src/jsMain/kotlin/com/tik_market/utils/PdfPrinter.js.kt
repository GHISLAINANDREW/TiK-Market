package com.tik_market.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPdfPrinter(): (htmlContent: String, jobName: String) -> Unit {
    return { _, _ -> }
}
