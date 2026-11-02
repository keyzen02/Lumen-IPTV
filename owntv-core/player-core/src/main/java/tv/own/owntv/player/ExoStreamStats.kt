package tv.own.owntv.player

import androidx.media3.common.Format
import androidx.media3.exoplayer.ExoPlayer

/**
 * Live: the measured throughput (a live stream arrives at its own bitrate), else the declared value.
 * Null when neither is known — a row reading "0 Mbps" claimed a playing stream carried no data.
 */
fun bitrateRow(f: Format, throughputTracker: ThroughputTracker): StreamInfoRow? {
    // HLS arrives in bursts: bytes / wall time is network throughput, not encoded bitrate.
    val declared = f.bitrate.takeIf { it > 0 }?.toLong()
    val measured = throughputTracker.takeIf { it.hasMeasured }?.bitsPerSecond?.takeIf { it > 0 }
    return diagnosticBitrateRow(declared, measured)
}

/**
 * A file's average bitrate: bytes downloaded over the media time they cover (how far the buffered
 * position moved). Network throughput is no use for a file — ExoPlayer fills its buffer in bursts at
 * line speed and then downloads nothing, so it read as the connection speed or as 0. This counts every
 * track the file carries (the whole file's rate), where mpv's row shows the video track alone.
 * Restarts after a seek ([restart]); null until [MIN_SPAN_MS] of media is covered.
 */
class FileBitrate {
    private var bytesAtStart = -1L
    private var bufferedAtStartMs = 0L

    fun restart() { bytesAtStart = -1L }

    fun bitsPerSecond(totalBytes: Long, bufferedMs: Long): Long? {
        // First read, or the byte count / buffer went backwards (tracker reset, buffer discarded).
        if (bytesAtStart < 0 || totalBytes < bytesAtStart || bufferedMs < bufferedAtStartMs) {
            bytesAtStart = totalBytes
            bufferedAtStartMs = bufferedMs
            return null
        }
        val spanMs = bufferedMs - bufferedAtStartMs
        if (spanMs < MIN_SPAN_MS) return null
        return ((totalBytes - bytesAtStart) * 8_000 / spanMs).takeIf { it > 0 }
    }

    private companion object {
        const val MIN_SPAN_MS = 5_000L
    }
}

/** Buffered duration + dropped frames since [dropsBaseline]. We can't reset ExoPlayer's own drop
 *  counter, so callers snapshot it per item and we subtract instead. */
fun bufferRow(p: ExoPlayer, dropsBaseline: Int): StreamInfoRow? {
    val drops = p.videoDecoderCounters?.let { it.ensureUpdated(); (it.droppedBufferCount - dropsBaseline).coerceAtLeast(0).toLong() }
    val buffered = p.totalBufferedDuration.takeIf { it > 0 }
    return if (buffered != null || drops != null) {
        StreamInfoRow(StreamInfoLabel.BUFFER, StreamInfoValue.Buffer(bufferedMs = buffered, droppedFrames = drops))
    } else null
}

/** Snapshot of the current drop count, for [bufferRow]'s baseline. */
fun currentDroppedFrames(p: ExoPlayer?): Int {
    val counters = p?.videoDecoderCounters ?: return 0
    counters.ensureUpdated()
    return counters.droppedBufferCount
}

fun diagnosticBitrateRow(declared: Long?, measured: Long?): StreamInfoRow? = when {
    declared != null && declared > 0 -> StreamInfoRow(StreamInfoLabel.BITRATE, StreamInfoValue.Bitrate(declared, BitrateOrigin.DECLARED))
    measured != null && measured > 0 -> StreamInfoRow(StreamInfoLabel.NETWORK, StreamInfoValue.Bitrate(measured, BitrateOrigin.NETWORK))
    else -> null
}

fun diagnosticHdr(mime: String?, codecs: String?, transfer: Int?): StreamHdrMode? = when {
    mime == "video/dolby-vision" || codecs?.split(',')?.any {
        it.trim().startsWith("dvhe.") || it.trim().startsWith("dvh1.") ||
            it.trim().startsWith("dvav.") || it.trim().startsWith("dva1.") || it.trim().startsWith("dav1.")
    } == true -> StreamHdrMode.DOLBY_VISION
    transfer == androidx.media3.common.C.COLOR_TRANSFER_ST2084 -> StreamHdrMode.HDR10_PQ
    transfer == androidx.media3.common.C.COLOR_TRANSFER_HLG -> StreamHdrMode.HLG
    transfer == androidx.media3.common.C.COLOR_TRANSFER_SDR -> StreamHdrMode.SDR
    else -> null
}
