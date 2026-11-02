package tv.own.owntv

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceView

/** Debug-only surface host used by the real-engine instrumentation tests. */
class LumenPlaybackTestActivity : Activity() {
    lateinit var video: SurfaceView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        video = SurfaceView(this)
        setContentView(video)
    }
}
