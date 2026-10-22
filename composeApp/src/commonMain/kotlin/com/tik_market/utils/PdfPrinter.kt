package com.tik_market.utils

import androidx.compose.runtime.Composable

/**
 * Multiplatform helper to generate PDF and print HTML documents natively.
 */
@Composable
expect fun rememberPdfPrinter(): (htmlContent: String, jobName: String) -> Unit
