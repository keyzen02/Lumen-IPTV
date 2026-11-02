package tv.own.owntv

import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import tv.own.owntv.core.database.DatabaseKey
import tv.own.owntv.core.database.OwnTVDatabase
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.model.SourceType
import tv.own.owntv.core.player.EnginePreference
import tv.own.owntv.core.sync.SyncManager
import tv.own.owntv.player.LivePreviewEngine
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.player.StreamInfoValue

/** Network tests use only publisher-provided testing media. No provider credentials required. */
@RunWith(AndroidJUnit4::class)
class LumenPlaybackSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val koin get() = GlobalContext.get()

    @Test fun importM3uThroughRealSyncAndEncryptDatabase() = runBlocking<Unit> {
        val playlist = File(context.cacheDir, "lumen-test.m3u")
        playlist.writeText("#EXTM3U\n#EXTINF:-1 tvg-id=\"test\" group-title=\"Test media\",Apple Bip Bop\n$HLS\n")
        val db = koin.get<OwnTVDatabase>()
        val source = SourceEntity(name = "Lumen test", type = SourceType.M3U,
            url = playlist.absolutePath, username = "fixture-user", password = "fixture-secret")
        val id = db.sourceDao().insert(source)
        try {
            withTimeout(90_000) { koin.get<SyncManager>().sync(source.copy(id = id), {}) }
            val channels = db.channelDao().allForSources(listOf(id), 10)
            assertEquals(1, channels.size)
            assertEquals(HLS, channels.single().streamUrl)
            assertEquals("fixture-secret", db.sourceDao().getById(id)!!.password)
            assertArrayEquals(DatabaseKey.load(context), DatabaseKey.load(context))
            val file = context.getDatabasePath(OwnTVDatabase.NAME)
            val header = file.inputStream().use { it.readNBytes(16) }
            assertFalse("Database must be encrypted", String(header).startsWith("SQLite format 3"))
            android.util.Log.i("LumenTest", "M3U_SYNC_AND_DATABASE_ENCRYPTION_PASS")
        } finally {
            db.sourceDao().delete(source.copy(id = id))
            playlist.delete()
        }
    }

    @Test fun exoHlsDecodesAndRendersFrames() = runBlocking<Unit> {
        val activity = startSurface()
        val engine = koin.get<LivePreviewEngine>()
        try {
            withContext(Dispatchers.Main) {
                engine.setSurface(activity.video.holder.surface)
                engine.play(HLS, muted = false)
            }
            withTimeout(90_000) { while (!engine.isPlaying.value) delay(250) }
            delay(6_000)
            val rows = withContext(Dispatchers.Main) { engine.streamInfo() }
            assertTrue(rows.any { (it.value as? StreamInfoValue.Video)?.width?.let { w -> w > 0 } == true })
            assertRenderedPicture(activity, "exo-hls.png")
            android.util.Log.i("LumenTest", "EXO_HLS_RENDER_PASS $rows")
        } finally {
            withContext(Dispatchers.Main) { engine.stop(); engine.setSurface(null); activity.finish() }
        }
    }

    @Test fun mpvDecodesAndRendersFrames() = runBlocking<Unit> {
        val activity = startSurface()
        val engine = koin.get<OwnTVPlayer>()
        try {
            withContext(Dispatchers.Main) {
                engine.attachSurface(activity.video.holder.surface)
                engine.setSurfaceSize(activity.video.width, activity.video.height)
                engine.play(InstrumentationRegistry.getArguments().getString("mpvUrl") ?: HLS, title = "Licensed test media", vodEngineOverride = EnginePreference.MPV_ONLY)
            }
            withTimeout(90_000) { while (engine.position.value < 6_000) delay(250) }
            val rows = withContext(Dispatchers.Main) { engine.streamInfo() }
            assertTrue(rows.any { (it.value as? StreamInfoValue.Video)?.width?.let { w -> w > 0 } == true })
            assertRenderedPicture(activity, "mpv-render.png")
            android.util.Log.i("LumenTest", "MPV_RENDER_PASS $rows")
        } finally {
            withContext(Dispatchers.Main) { engine.stop(); activity.finish() }
        }
    }

    private suspend fun startSurface(): LumenPlaybackTestActivity {
        val intent = Intent(context, LumenPlaybackTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as LumenPlaybackTestActivity
        withTimeout(30_000) {
            while (!withContext(Dispatchers.Main) { activity.video.holder.surface.isValid }) delay(100)
        }
        return activity
    }

    private suspend fun assertRenderedPicture(activity: LumenPlaybackTestActivity, name: String) {
        val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var result = -1
        withContext(Dispatchers.Main) {
            PixelCopy.request(activity.video, bitmap, { result = it; latch.countDown() }, Handler(Looper.getMainLooper()))
        }
        assertTrue("Frame readback timed out", latch.await(10, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, result)
        val pixels = IntArray(320 * 180)
        bitmap.getPixels(pixels, 0, 320, 0, 0, 320, 180)
        assertTrue("Decoded surface is blank", pixels.toSet().size > 32)
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    companion object {
        const val HLS = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"
        const val MP4 = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}

