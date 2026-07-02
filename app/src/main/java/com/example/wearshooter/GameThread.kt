package com.example.wearshooter

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * The game loop. Runs on its own thread so the simulation/render rate is
 * independent of the UI thread. Targets ~60 FPS and feeds the real frame
 * delta to the world so movement stays smooth even if a frame runs long.
 *
 * Rendering goes through [SurfaceHolder.lockHardwareCanvas] (GPU-composited —
 * far cheaper per frame and per watt than the software path on every Wear OS 3+
 * device); if the device refuses, it quietly drops back to the software canvas.
 *
 * Pacing uses an absolute frame deadline rather than "sleep the remainder", so
 * timing error from one frame doesn't accumulate into visible drift.
 */
class GameThread(
    private val holder: SurfaceHolder,
    private val view: GameView
) : Thread("GameLoop") {

    @Volatile
    var running = false

    private var useHardwareCanvas = true

    override fun run() {
        val frameNs = 1_000_000_000L / 60L
        var last = System.nanoTime()
        var deadline = last

        while (running) {
            val frameStart = System.nanoTime()
            var dt = (frameStart - last) / 1_000_000_000f
            last = frameStart
            if (dt > 0.05f) dt = 0.05f          // clamp after a stall (e.g. GC, app switch)

            view.update(dt)

            var canvas: Canvas? = null
            try {
                canvas = lockCanvas()
                if (canvas != null) view.render(canvas)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: IllegalStateException) {
                        // Surface went away mid-frame; the loop will stop shortly.
                    }
                }
            }

            deadline += frameNs
            val now = System.nanoTime()
            var sleepNs = deadline - now
            if (sleepNs < -frameNs) { deadline = now; sleepNs = 0 }   // fell badly behind — resync
            if (sleepNs > 0) {
                try {
                    sleep(sleepNs / 1_000_000L, (sleepNs % 1_000_000L).toInt())
                } catch (_: InterruptedException) {
                    // Ignore: stopping is driven by the `running` flag.
                }
            }
        }
    }

    private fun lockCanvas(): Canvas? {
        if (useHardwareCanvas) {
            try {
                return holder.lockHardwareCanvas()
            } catch (_: Throwable) {
                // No GPU canvas on this surface (or it's mid-teardown) — fall back for good.
                useHardwareCanvas = false
            }
        }
        return holder.lockCanvas()
    }
}
