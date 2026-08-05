package com.dschangmarket.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Re-encodes a video to a smaller H.264 file (max 720p, lower bitrate) so the
 * base64 payload sent to the upload endpoint stays small enough to succeed.
 */
object VideoCompressor {

    private const val MAX_DIM = 720
    private const val BIT_RATE = 1_500_000
    private const val FRAME_RATE = 24
    private const val I_FRAME_INTERVAL = 2

    /**
     * Compresses the video at [uri] into [outputFile]. Returns true on success.
     */
    suspend fun compress(context: Context, uri: Uri, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)

                var videoTrackIndex = -1
                var videoFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val f = extractor.getTrackFormat(i)
                    val mime = f.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
                        videoTrackIndex = i
                        videoFormat = f
                        break
                    }
                }
                if (videoTrackIndex < 0 || videoFormat == null) {
                    extractor.release()
                    return@withContext false
                }
                extractor.selectTrack(videoTrackIndex)

                val srcMime = videoFormat.getString(MediaFormat.KEY_MIME)!!
                val srcW = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
                val srcH = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
                val rotation = if (videoFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                    videoFormat.getInteger(MediaFormat.KEY_ROTATION)
                } else 0

                // Compute target dimensions keeping aspect ratio (even numbers).
                var outW = srcW
                var outH = srcH
                if (outW > MAX_DIM || outH > MAX_DIM) {
                    val scale = MAX_DIM.toFloat() / maxOf(outW, outH)
                    outW = (outW * scale).toInt()
                    outH = (outH * scale).toInt()
                    if (outW % 2 != 0) outW--
                    if (outH % 2 != 0) outH--
                }

                val decoder = MediaCodec.createDecoderByType(srcMime)
                decoder.configure(videoFormat, null, null, 0)
                decoder.start()

                val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                val encodeFormat = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC, outW, outH
                )
                encodeFormat.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                encodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                encodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val inputSurface = encoder.createInputSurface()
                encoder.start()

                val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                if (rotation != 0) muxer.setOrientationHint(rotation)
                var muxerTrackIndex = -1
                var muxerStarted = false

                val bufferInfo = MediaCodec.BufferInfo()
                var sawInputEOS = false
                var sawOutputEOS = false
                var sawEncoderEOS = false

                // Feed decoder from extractor.
                while (!sawInputEOS) {
                    val inIndex = decoder.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val inBuf = decoder.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                    drainDecoder(decoder, bufferInfo)
                }
                // Drain remaining decoder output.
                while (!sawOutputEOS) {
                    sawOutputEOS = drainDecoder(decoder, bufferInfo)
                }
                decoder.stop()
                decoder.release()
                extractor.release()

                // Signal end of input to encoder.
                encoder.signalEndOfInputStream()

                // Drain encoder output to muxer.
                while (!sawEncoderEOS) {
                    val encIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                    when {
                        encIndex >= 0 -> {
                            val encBuf = encoder.getOutputBuffer(encIndex)!!
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0) {
                                if (!muxerStarted) {
                                    muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                                    muxer.start()
                                    muxerStarted = true
                                }
                                muxer.writeSampleData(muxerTrackIndex, encBuf, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(encIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                sawEncoderEOS = true
                            }
                        }
                        encIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (!muxerStarted) {
                                muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                    }
                }

                encoder.stop()
                encoder.release()
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
                true
            } catch (e: Exception) {
                android.util.Log.e("VideoCompressor", "Compression failed", e)
                false
            }
        }
    }

    private fun drainDecoder(decoder: MediaCodec, bufferInfo: MediaCodec.BufferInfo): Boolean {
        val outIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
        return when {
            outIndex >= 0 -> {
                decoder.releaseOutputBuffer(outIndex, true)
                bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
            }
            outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> false
            else -> false
        }
    }
}