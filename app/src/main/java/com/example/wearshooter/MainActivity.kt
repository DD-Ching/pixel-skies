package com.example.wearshooter

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager

/**
 * Single-activity host. Keeps the screen on while playing and forwards rotary
 * events that the framework sometimes delivers to the window before the view.
 */
class MainActivity : Activity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gameView = GameView(this)
        setContentView(gameView)
        gameView.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        gameView.pause()
        super.onPause()
    }

    // Some Wear builds route the crown to the Activity first; pass it to the view.
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return gameView.onGenericMotionEvent(event) || super.onGenericMotionEvent(event)
    }
}
