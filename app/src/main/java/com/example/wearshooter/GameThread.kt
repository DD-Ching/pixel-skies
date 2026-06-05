package com.example.wearshooter

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * The game loop. Runs on its own thread so the simulation/render rate is
 * independent of the UI thread. Targets ~60 FPS and feeds the real frame
 * delta to the world so movement stays smooth even if a frame runs long.
 */
class GameThread(
    private val holder: SurfaceHolder,
    private val view: GameView
) : Thread("GameLoop") {

    @Volatile
    var running = false

    override fun run() {
        val targetFrameNs = 1_000_000_000L / 60L
        var last = System.nanoTime()

        while (running) {
            val frameStart = System.nanoTime()
            var dt = (frameStart - last) / 1_000_000_000f
            last = frameStart
            if (dt > 0.05f) dt = 0.05f          // clamp after a stall (e.g. GC, app switch)

            view.update(dt)

            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) { view.render(canvas) }
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: IllegalStateException) {
                        // Surface went away mid-frame; the loop will stop shortly.
                    }
                }
            }

            val sleepNs = targetFrameNs - (System.nanoTime() - frameStart)
            if (sleepNs > 0) {
                try {
                    sleep(sleepNs / 1_000_000L, (sleepNs % 1_000_000L).toInt())
                } catch (_: InterruptedException) {
                    // Ignore: stopping is driven by the `running` flag.
                }
            }
        }
    }
}
