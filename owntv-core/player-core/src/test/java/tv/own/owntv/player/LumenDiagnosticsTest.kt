package tv.own.owntv.player

import androidx.media3.common.C
import org.junit.Assert.*
import org.junit.Test

class LumenDiagnosticsTest {
    @Test fun hlsDownloadSpeedIsNotEncodedBitrate() {
        val row = diagnosticBitrateRow(8_000_000, 120_000_000)!!
        assertEquals(StreamInfoLabel.BITRATE, row.label)
        assertEquals(StreamInfoValue.Bitrate(8_000_000, BitrateOrigin.DECLARED), row.value)
    }

    @Test fun unlabelledTransportShowsNetworkRateHonestly() {
        val row = diagnosticBitrateRow(null, 20_000_000)!!
        assertEquals(StreamInfoLabel.NETWORK, row.label)
        assertEquals(BitrateOrigin.NETWORK, (row.value as StreamInfoValue.Bitrate).origin)
        assertNull(diagnosticBitrateRow(-1, 0))
    }

    @Test fun dolbyVisionIsNotMislabelledHdr10() {
        assertEquals(StreamHdrMode.DOLBY_VISION, diagnosticHdr("video/dolby-vision", null, C.COLOR_TRANSFER_ST2084))
        assertEquals(StreamHdrMode.DOLBY_VISION, diagnosticHdr("video/hevc", "dvhe.05.06", null))
        assertEquals(StreamHdrMode.HLG, diagnosticHdr("video/hevc", null, C.COLOR_TRANSFER_HLG))
        assertNull(diagnosticHdr("video/hevc", null, null))
    }

    @Test fun sourceCadenceDoesNotBecomeDisplayRefresh() {
        for (fps in listOf(24.0, 25.0, 30.0, 50.0, 59.94, 60.0)) {
            val meter = PtsFrameRate()
            for (i in 0..500) meter.add((i * 1_000_000.0 / fps).toLong())
            assertEquals(fps.toFloat(), meter.rate()!!, 0.001f)
        }
    }
}
