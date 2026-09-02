package com.tik_market.utils

/**
 * Monitors upload latency to determine connection quality level.
 * Used by the live streaming screen to adapt frame rate and JPEG quality
 * in real time.
 *
 * Levels:
 *   GOOD    → upload < 800ms  → 1 fps, high JPEG quality
 *   MEDIUM  → upload < 2000ms → ~1 fps every 2s, medium JPEG quality
 *   POOR    → upload >= 2000ms → 1 fps every 3s, low JPEG quality
 */
enum class ConnectionQuality(val label: String, val captureIntervalMs: Long, val jpegQuality: Int) {
    GOOD("HD", 1000L, 85),
    MEDIUM("SD", 2000L, 65),
    POOR("LD", 3000L, 45);

    companion object {
        fun fromLatencyMs(latencyMs: Long): ConnectionQuality = when {
            latencyMs < 800  -> GOOD
            latencyMs < 2000 -> MEDIUM
            else             -> POOR
        }
    }
}

/**
 * Thread-safe upload-latency tracker. Call [recordUpload] after each
 * successful frame upload. Call [currentQuality] to get the adapted level.
 */
class ConnectionQualityMonitor {
    private val latencies = mutableListOf<Long>()
    private val lock = Any()

    /** Record the time (ms) a single upload took. */
    fun recordUpload(durationMs: Long) {
        synchronized(lock) {
            latencies.add(durationMs)
            // Keep the last 5 samples for a rolling average.
            if (latencies.size > 5) latencies.removeAt(0)
        }
    }

    /** Average latency over the recent window. */
    fun averageLatencyMs(): Long {
        synchronized(lock) {
            if (latencies.isEmpty()) return 1000L
            return latencies.average().toLong()
        }
    }

    fun currentQuality(): ConnectionQuality =
        ConnectionQuality.fromLatencyMs(averageLatencyMs())
}

/**
 * Estimates the current connection quality for playback (reels, stories).
 * Uses a lightweight probe against the backend health endpoint and returns
 * a quality level. Falls back to GOOD if the probe fails.
 */
suspend fun estimateConnectionQuality(): ConnectionQuality {
    return try {
        val latencyMs = com.tik_market.api.ApiClient.probeLatencyMs()
        ConnectionQuality.fromLatencyMs(latencyMs)
    } catch (_: Exception) {
        ConnectionQuality.GOOD
    }
}
