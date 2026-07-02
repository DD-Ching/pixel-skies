package com.example.wearshooter

import android.content.Context
import android.graphics.Canvas
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * The rendering surface and input hub.
 *
 *  - Owns the [GameThread] and ties it to the Surface lifecycle.
 *  - Receives rotary-crown scroll events (the primary control).
 *  - Detects tap (start/restart) and long-press (pause) gestures.
 *  - Accepts arrow keys as a convenience fallback for the emulator.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val world = World()
    private val renderer = Renderer(world)
    private var thread: GameThread? = null

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val prefs = context.getSharedPreferences("pixel_skies", Context.MODE_PRIVATE)

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                world.onTap(); return true
            }
            override fun onLongPress(e: MotionEvent) {
                world.onLongPress()
            }
        }
    )

    init {
        holder.addCallback(this)
        // Required so the view receives rotary + key events.
        isFocusable = true
        isFocusableInTouchMode = true
        // Let the game fire short haptics (hits, bombs, pickups) from the loop thread.
        val v = vibrator
        if (v != null && v.hasVibrator()) {
            world.vibrate = { ms ->
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        // Persist the high score across launches (apply() is thread-safe).
        world.loadHighScore = { prefs.getInt("best", 0) }
        world.saveHighScore = { value -> prefs.edit().putInt("best", value).apply() }
    }

    // ---- Surface lifecycle ----
    override fun surfaceCreated(holder: SurfaceHolder) = startLoop()

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        world.onResize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) = stopLoop()

    private fun startLoop() {
        if (thread?.running == true) return
        thread = GameThread(holder, this).apply {
            running = true
            start()
        }
    }

    private fun stopLoop() {
        val t = thread ?: return
        t.running = false
        var retry = true
        while (retry) {
            try {
                t.join()
                retry = false
            } catch (_: InterruptedException) {
                // retry the join until the loop thread has exited
            }
        }
        thread = null
    }

    // ---- Called from the Activity lifecycle ----
    fun resume() {
        requestFocus()
        if (holder.surface.isValid) startLoop()
    }

    fun pause() {
        world.onAppPause()
        stopLoop()
    }

    // ---- Called by the game loop ----
    fun update(dt: Float) = world.update(dt)
    fun render(canvas: Canvas) = renderer.render(canvas)

    // ---- Input ----
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    /** Rotary crown arrives here as an ACTION_SCROLL from the rotary encoder source. */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL &&
            event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
        ) {
            world.onRotary(event.getAxisValue(MotionEvent.AXIS_SCROLL))
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { world.onKey(-1); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { world.onKey(1); return true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { world.onTap(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            world.onKey(0)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
