package com.example.wearshooter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.wearshooter.GameConfig.BULLET_H_FRACTION
import com.example.wearshooter.GameConfig.BULLET_W_FRACTION
import com.example.wearshooter.GameConfig.DRONE_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.MAX_MULTIPLIER
import com.example.wearshooter.GameConfig.OVERDRIVE_MAX
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Every pixel the game draws. The World never touches a Canvas; this class reads
 * its state once per frame on the game-loop thread.
 *
 * Allocation discipline: three Paints, one RectF, palette lookups from IntArrays,
 * and HUD strings that are rebuilt only when the underlying value changes — a
 * steady frame allocates nothing, so the GC never gets a chance to cause a hitch.
 *
 * Round-face discipline (see the wear-design notes): everything anchored to the
 * safe ring is placed polar-style — lives arc on the lower-left of the ring,
 * bombs on the lower-right, the Overdrive gauge dead-bottom between them — so
 * nothing drifts outside the inscribed circle the way the old corner HUD did.
 */
internal class Renderer(private val world: World) {

    private val shapePaint = Paint().apply { isAntiAlias = false }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val dimPaint = Paint()

    // Layout-derived resources, rebuilt only when the surface size changes.
    private var lastW = -1f
    private var lastH = -1f
    private var nebulaShader: Shader? = null
    private var starSurfaceShader: Shader? = null
    private var planetShader: Shader? = null
    private val arcRect = RectF()          // overdrive gauge arc bounds

    // ---- HUD string caches: rebuilt on change, never per frame ----
    private val multTexts = Array(MAX_MULTIPLIER + 1) { "x$it" }
    private var scoreCache = Int.MIN_VALUE
    private var scoreText = "0"
    private var bestCache = Int.MIN_VALUE
    private var bestText = ""
    private var stageCache = Int.MIN_VALUE
    private var stageText = ""
    private var statusWeapon: WeaponType? = null
    private var statusPower = -1
    private var statusText = ""
    private var overCache = Int.MIN_VALUE
    private var overStats = ""

    fun render(canvas: Canvas) {
        canvas.drawColor(Palette.SPACE)
        val w = world
        if (!w.initialized) return
        ensureLayout()

        drawBackdrop(canvas)

        val shaking = w.shakeTime > 0f
        if (shaking) {
            val k = w.shakeTime.coerceIn(0f, 1f) * w.shakeMag
            canvas.save()
            canvas.translate((Random.nextFloat() - 0.5f) * 2f * k, (Random.nextFloat() - 0.5f) * 2f * k)
        }

        drawStars(canvas)

        if (w.state != GameState.READY) {
            w.boss?.let { drawBoss(canvas, it) }
            drawShockwaves(canvas)
            drawPowerUps(canvas)
            drawEnemies(canvas)
            drawEnemyBullets(canvas)
            drawLasers(canvas)
            drawBullets(canvas)
            drawDrones(canvas)
            if (w.player.alive) drawPlayer(canvas)
        }
        drawParticles(canvas)

        // Faint ring so the round safe-area reads on the square emulator.
        shapePaint.style = Paint.Style.STROKE
        shapePaint.strokeWidth = w.minDim * 0.006f
        shapePaint.color = Palette.SAFE_RING
        canvas.drawCircle(w.cx, w.cy, w.radius - w.margin * 0.4f, shapePaint)
        shapePaint.style = Paint.Style.FILL

        if (shaking) canvas.restore()

        // Bomb / boss-death white flash (drawn after un-shaking, full screen).
        if (w.bombFlash > 0f) {
            dimPaint.color = Color.argb(((w.bombFlash / 0.3f) * 160).toInt().coerceIn(0, 200), 255, 255, 255)
            canvas.drawRect(0f, 0f, w.w, w.h, dimPaint)
        }

        drawPopups(canvas)

        when (w.state) {
            GameState.PLAYING -> { drawHud(canvas); drawEventBanner(canvas) }
            GameState.PAUSED -> { drawHud(canvas); drawPausedOverlay(canvas) }
            GameState.READY -> drawTitle(canvas)
            GameState.GAME_OVER -> drawGameOver(canvas)
        }
    }

    /** Rebuild size-dependent resources (gradient shaders, gauge arc) after a resize. */
    private fun ensureLayout() {
        val w = world
        if (w.w == lastW && w.h == lastH) return
        lastW = w.w; lastH = w.h
        nebulaShader = LinearGradient(0f, 0f, 0f, w.h,
            intArrayOf(0xFF120A2A.toInt(), 0xFF3A1D5C.toInt(), 0xFF14243F.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        starSurfaceShader = RadialGradient(w.cx, w.h * 1.02f, w.minDim * 0.95f,
            intArrayOf(0xFFFFF1B0.toInt(), 0xFFFF7A1E.toInt(), 0xFF5A1402.toInt(), 0x00000000),
            floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP)
        planetShader = LinearGradient(0f, w.h * 0.55f, 0f, w.h,
            intArrayOf(0xFF0B2A4A.toInt(), 0xFF1E6FA8.toInt(), 0xFF6FD0E0.toInt()),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        // Overdrive gauge arc rides just inside the safe ring.
        val ar = w.radius - w.margin * 0.4f
        arcRect.set(w.cx - ar, w.cy - ar, w.cx + ar, w.cy + ar)
    }

    // =====================================================================
    //  Backdrops — themed scenery that crossfades as you progress
    // =====================================================================
    private fun drawBackdrop(canvas: Canvas) {
        val w = world
        if (w.themeFade < 1f && w.prevTheme != w.theme) drawTheme(canvas, w.prevTheme, 1f - w.themeFade)
        drawTheme(canvas, w.theme, w.themeFade)
    }

    private fun drawTheme(canvas: Canvas, t: Int, alpha: Float) {
        val a = alpha.coerceIn(0f, 1f)
        if (a <= 0f) return
        when (t) {
            1 -> drawNebula(canvas, a)
            2 -> drawStation(canvas, a)
            3 -> drawStarSurface(canvas, a)
            4 -> drawPlanet(canvas, a)
            else -> {}   // 0 = deep space: the bare starfield
        }
    }

    private fun drawNebula(canvas: Canvas, a: Float) {
        val w = world
        nebulaShader?.let {
            shapePaint.shader = it
            shapePaint.alpha = (a * 150).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w.w, w.h, shapePaint)
            shapePaint.shader = null
            shapePaint.alpha = 255
        }
        val drift = sin(w.animTime * 0.2f) * w.minDim * 0.05f
        shapePaint.color = Color.argb((a * 42).toInt(), 180, 110, 230)
        canvas.drawCircle(w.cx - w.minDim * 0.2f + drift, w.cy - w.minDim * 0.15f, w.minDim * 0.34f, shapePaint)
        shapePaint.color = Color.argb((a * 36).toInt(), 90, 140, 230)
        canvas.drawCircle(w.cx + w.minDim * 0.22f - drift, w.cy + w.minDim * 0.12f, w.minDim * 0.30f, shapePaint)
    }

    private fun drawStation(canvas: Canvas, a: Float) {
        val w = world
        // Side hull framing the lane.
        shapePaint.color = Color.argb((a * 230).toInt(), 14, 20, 32)
        canvas.drawRect(0f, 0f, w.minDim * 0.1f, w.h, shapePaint)
        canvas.drawRect(w.w - w.minDim * 0.1f, 0f, w.w, w.h, shapePaint)
        // Scrolling trusses with rows of lit windows, sliding downward like you're passing them.
        val spacing = w.minDim * 0.55f
        val scroll = (w.animTime * 38f) % spacing
        var y = -spacing + scroll
        while (y < w.h) {
            shapePaint.color = Color.argb((a * 200).toInt(), 24, 32, 48)
            canvas.drawRect(0f, y, w.w, y + w.minDim * 0.05f, shapePaint)
            shapePaint.color = Color.argb((a * 150).toInt(), 110, 190, 255)
            var x = w.minDim * 0.05f
            while (x < w.w) {
                canvas.drawRect(x, y + w.minDim * 0.014f, x + w.minDim * 0.018f, y + w.minDim * 0.03f, shapePaint)
                x += w.minDim * 0.12f
            }
            y += spacing
        }
    }

    private fun drawStarSurface(canvas: Canvas, a: Float) {
        val w = world
        starSurfaceShader?.let {
            shapePaint.shader = it
            shapePaint.alpha = (a * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w.w, w.h, shapePaint)
            shapePaint.shader = null
            shapePaint.alpha = 255
        }
        // Flares licking up from the star at the bottom edge.
        val n = 5
        for (i in 0 until n) {
            val fx = w.w * (i + 0.5f) / n
            val flick = 0.5f + 0.5f * sin(w.animTime * (3f + i) + i)
            val fh = w.minDim * (0.08f + 0.13f * flick)
            shapePaint.color = Color.argb((a * 120).toInt(), 255, 185, 70)
            canvas.drawRect(fx - w.minDim * 0.018f, w.h - fh, fx + w.minDim * 0.018f, w.h, shapePaint)
        }
    }

    private fun drawPlanet(canvas: Canvas, a: Float) {
        val w = world
        val pr = w.minDim * 1.1f
        shapePaint.color = Color.argb((a * 255).toInt(), 18, 54, 92)
        canvas.drawCircle(w.cx, w.h + pr * 0.62f, pr, shapePaint)        // planet rising from the bottom
        planetShader?.let {
            shapePaint.shader = it
            shapePaint.alpha = (a * 200).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, w.h - w.minDim * 0.18f, w.w, w.h, shapePaint)  // lit atmospheric limb
            shapePaint.shader = null
            shapePaint.alpha = 255
        }
        shapePaint.color = Color.argb((a * 180).toInt(), 200, 210, 230)
        canvas.drawCircle(w.cx + w.minDim * 0.28f, w.cy - w.minDim * 0.26f, w.minDim * 0.05f, shapePaint)  // a small moon
    }

    private fun drawStars(canvas: Canvas) {
        for (s in world.stars) {
            shapePaint.color = Color.argb(s.bright, 255, 255, 255)
            canvas.drawRect(s.x, s.y, s.x + s.size, s.y + s.size, shapePaint)
        }
    }

    // =====================================================================
    //  Field entities
    // =====================================================================
    /** The jet, drawn at an arbitrary spot so the title screen can reuse it. */
    private fun drawJet(canvas: Canvas, px: Float, py: Float, pw: Float, ph: Float) {
        val flame = 0.5f + 0.5f * sin(world.animTime * 30f)
        shapePaint.color = Palette.JET_FLAME
        canvas.drawRect(px - pw * 0.09f, py + ph * 0.42f, px + pw * 0.09f, py + ph * (0.42f + 0.30f * flame), shapePaint)
        shapePaint.color = Palette.JET_WING
        canvas.drawRect(px - pw * 0.50f, py - ph * 0.02f, px + pw * 0.50f, py + ph * 0.20f, shapePaint)
        canvas.drawRect(px - pw * 0.28f, py + ph * 0.30f, px + pw * 0.28f, py + ph * 0.44f, shapePaint)
        shapePaint.color = Palette.JET_BODY
        canvas.drawRect(px - pw * 0.13f, py - ph * 0.50f, px + pw * 0.13f, py + ph * 0.42f, shapePaint)
        canvas.drawRect(px - pw * 0.05f, py - ph * 0.62f, px + pw * 0.05f, py - ph * 0.46f, shapePaint)
        shapePaint.color = Palette.WHITE
        canvas.drawRect(px - pw * 0.06f, py - ph * 0.28f, px + pw * 0.06f, py - ph * 0.08f, shapePaint)
    }

    private fun drawPlayer(canvas: Canvas) {
        val w = world
        // Blink while invulnerable.
        if (w.invuln > 0f && ((w.invuln * 12f).toInt() and 1) == 0) return
        val p = w.player

        // Shield ring.
        if (w.shield > 0) {
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = w.minDim * 0.012f
            shapePaint.color = if (((w.animTime * 8f).toInt() and 1) == 0) Palette.SHIELD_RING else Palette.SHIELD_RING_DIM
            canvas.drawCircle(p.x, p.y, p.width * 0.85f, shapePaint)
            shapePaint.style = Paint.Style.FILL
        }
        drawJet(canvas, p.x, p.y, p.width, p.height)
    }

    private fun drawDrones(canvas: Canvas) {
        val w = world
        if (w.drones.isEmpty()) return
        val dr = w.minDim * DRONE_RADIUS_FRACTION
        val flame = 0.5f + 0.5f * sin(w.animTime * 26f)
        val surging = w.overdriveTime > 0f
        for (d in w.drones) {
            shapePaint.color = Palette.JET_FLAME            // thruster flicker
            canvas.drawRect(d.x - dr * 0.16f, d.y + dr * 0.55f, d.x + dr * 0.16f, d.y + dr * (0.55f + 0.5f * flame), shapePaint)
            shapePaint.color = if (surging) 0xFFB9F6FF.toInt() else Palette.JET_WING
            canvas.drawRect(d.x - dr * 0.7f, d.y - dr * 0.05f, d.x + dr * 0.7f, d.y + dr * 0.45f, shapePaint)  // wings
            shapePaint.color = Palette.JET_BODY
            canvas.drawRect(d.x - dr * 0.26f, d.y - dr * 0.7f, d.x + dr * 0.26f, d.y + dr * 0.5f, shapePaint)  // fuselage
            shapePaint.color = Palette.WHITE
            canvas.drawRect(d.x - dr * 0.1f, d.y - dr * 0.42f, d.x + dr * 0.1f, d.y - dr * 0.12f, shapePaint)  // cockpit
        }
    }

    private fun drawBullets(canvas: Canvas) {
        val w = world
        val bw = w.minDim * BULLET_W_FRACTION
        val bh = w.minDim * BULLET_H_FRACTION
        for (bl in w.bullets) {
            when (bl.kind) {
                1 -> { // LASER: narrow tall magenta beam, white core
                    val lw = bw * 0.55f; val lh = bh * 1.7f
                    shapePaint.color = Palette.BULLET_BODY[1]
                    canvas.drawRect(bl.x - lw, bl.y - lh, bl.x + lw, bl.y + lh, shapePaint)
                    shapePaint.color = Palette.WHITE
                    canvas.drawRect(bl.x - lw * 0.4f, bl.y - lh, bl.x + lw * 0.4f, bl.y + lh, shapePaint)
                }
                2 -> { // WIDE: small orange pellet
                    val s = bw * 1.05f
                    shapePaint.color = Palette.BULLET_BODY[2]
                    canvas.drawRect(bl.x - s, bl.y - s, bl.x + s, bl.y + s, shapePaint)
                    shapePaint.color = Palette.WIDE_CORE
                    canvas.drawRect(bl.x - s * 0.4f, bl.y - s * 0.4f, bl.x + s * 0.4f, bl.y + s * 0.4f, shapePaint)
                }
                3 -> { // HOMING: green diamond with a fading tail along its heading
                    val s = bw * 1.25f
                    val len = hypot(bl.vx, bl.vy).coerceAtLeast(1f)
                    val tx = bl.x - bl.vx / len * s * 2.4f
                    val ty = bl.y - bl.vy / len * s * 2.4f
                    shapePaint.color = Palette.HOMING_TRAIL
                    canvas.drawRect(tx - s * 0.4f, ty - s * 0.4f, tx + s * 0.4f, ty + s * 0.4f, shapePaint)
                    shapePaint.color = Palette.BULLET_BODY[3]
                    canvas.drawRect(bl.x - s, bl.y - s, bl.x + s, bl.y + s, shapePaint)
                    shapePaint.color = Palette.WHITE
                    canvas.drawRect(bl.x - s * 0.4f, bl.y - s * 0.4f, bl.x + s * 0.4f, bl.y + s * 0.4f, shapePaint)
                }
                4 -> { // HELIX: violet weaving bolt with a white core
                    val lw = bw * 0.85f; val lh = bh * 0.9f
                    shapePaint.color = Palette.BULLET_BODY[4]
                    canvas.drawRect(bl.x - lw, bl.y - lh, bl.x + lw, bl.y + lh, shapePaint)
                    shapePaint.color = Palette.WHITE
                    canvas.drawRect(bl.x - lw * 0.4f, bl.y - lh * 0.6f, bl.x + lw * 0.4f, bl.y + lh * 0.6f, shapePaint)
                }
                else -> { // VULCAN: cyan bolt
                    shapePaint.color = Palette.BULLET_BODY[0]
                    canvas.drawRect(bl.x - bw / 2f, bl.y - bh / 2f, bl.x + bw / 2f, bl.y + bh / 2f, shapePaint)
                    shapePaint.color = Palette.WHITE
                    canvas.drawRect(bl.x - bw * 0.22f, bl.y - bh * 0.40f, bl.x + bw * 0.22f, bl.y + bh * 0.40f, shapePaint)
                }
            }
        }
    }

    private fun drawLasers(canvas: Canvas) {
        val w = world
        for (l in w.lasers) {
            if (l.warnTime > 0f) {
                // Telegraph: a thin pulsing line at the exact column the beam will occupy. No hitbox.
                val pulse = 0.35f + 0.65f * abs(sin(w.animTime * 22f))
                val tw = (l.halfW * 0.14f).coerceAtLeast(1.5f)
                shapePaint.color = Color.argb((pulse * 200).toInt().coerceIn(40, 220),
                    Palette.LASER_WARN_R, Palette.LASER_WARN_G, Palette.LASER_WARN_B)
                canvas.drawRect(l.x - tw, 0f, l.x + tw, w.h, shapePaint)
                // brighter as it's about to fire, so the "it's coming" moment reads
                if (l.warnTime < 0.28f) {
                    shapePaint.color = Color.argb((pulse * 255).toInt(),
                        Palette.LASER_IMMINENT_R, Palette.LASER_IMMINENT_G, Palette.LASER_IMMINENT_B)
                    canvas.drawRect(l.x - tw * 0.5f, 0f, l.x + tw * 0.5f, w.h, shapePaint)
                }
            } else if (l.fireTime > 0f) {
                // Lethal beam: glow → bright body → white-hot core. Fades in fast, out near the end.
                val fadeIn = ((l.maxFire - l.fireTime) / 0.08f).coerceIn(0f, 1f)
                val fadeOut = (l.fireTime / 0.18f).coerceIn(0f, 1f)
                val a = (min(fadeIn, fadeOut) * 255).toInt()
                val hw = l.halfW
                shapePaint.color = Color.argb((a * 0.35f).toInt(), 255, 90, 200)
                canvas.drawRect(l.x - hw * 1.7f, 0f, l.x + hw * 1.7f, w.h, shapePaint)
                shapePaint.color = Color.argb(a, 255, 60, 150)
                canvas.drawRect(l.x - hw, 0f, l.x + hw, w.h, shapePaint)
                shapePaint.color = Color.argb(a, 255, 255, 255)
                canvas.drawRect(l.x - hw * 0.4f, 0f, l.x + hw * 0.4f, w.h, shapePaint)
            }
        }
    }

    private fun drawEnemyBullets(canvas: Canvas) {
        for (eb in world.enemyBullets) {
            val r = eb.radius
            shapePaint.color = Palette.EBULLET
            canvas.drawRect(eb.x - r, eb.y - r, eb.x + r, eb.y + r, shapePaint)
            shapePaint.color = Palette.EBULLET_CORE
            canvas.drawRect(eb.x - r * 0.45f, eb.y - r * 0.45f, eb.x + r * 0.45f, eb.y + r * 0.45f, shapePaint)
        }
    }

    private fun drawEnemies(canvas: Canvas) {
        val w = world
        for (e in w.enemies) {
            val ex = e.x; val ey = e.y; val r = e.radius
            val flash = e.hitFlash > 0f
            val body = if (flash) Palette.WHITE else Palette.ENEMY_BODY[e.type.ordinal]
            val accent = if (flash) Palette.WHITE else Palette.ENEMY_ACCENT[e.type.ordinal]
            shapePaint.color = body
            canvas.drawRect(ex - r, ey - r * 0.6f, ex + r, ey + r * 0.6f, shapePaint)
            canvas.drawRect(ex - r * 0.6f, ey - r, ex + r * 0.6f, ey + r, shapePaint)
            shapePaint.color = Palette.ENEMY_DARK
            canvas.drawRect(ex - r * 0.50f, ey - r * 0.18f, ex - r * 0.14f, ey + r * 0.22f, shapePaint)
            canvas.drawRect(ex + r * 0.14f, ey - r * 0.18f, ex + r * 0.50f, ey + r * 0.22f, shapePaint)
            shapePaint.color = accent
            canvas.drawRect(ex - r, ey + r * 0.5f, ex - r * 0.5f, ey + r, shapePaint)
            canvas.drawRect(ex + r * 0.5f, ey + r * 0.5f, ex + r, ey + r, shapePaint)
            // Type tell-tales so the new enemies read at a glance.
            if (!flash) when (e.type) {
                EnemyType.ORBITER -> {
                    shapePaint.style = Paint.Style.STROKE
                    shapePaint.strokeWidth = r * 0.12f
                    canvas.drawCircle(ex, ey, r * 1.05f, shapePaint)
                    shapePaint.style = Paint.Style.FILL
                }
                EnemyType.SPLITTER -> {
                    shapePaint.color = Palette.ENEMY_DARK                      // seam: it'll break apart
                    canvas.drawRect(ex - r * 0.06f, ey - r, ex + r * 0.06f, ey + r, shapePaint)
                }
                EnemyType.MINE -> {                                            // four spikes + a blinking core
                    shapePaint.color = 0xFFFF5030.toInt()
                    val sp = r * 0.5f
                    canvas.drawRect(ex - r * 0.09f, ey - r - sp, ex + r * 0.09f, ey - r, shapePaint)
                    canvas.drawRect(ex - r * 0.09f, ey + r, ex + r * 0.09f, ey + r + sp, shapePaint)
                    canvas.drawRect(ex - r - sp, ey - r * 0.09f, ex - r, ey + r * 0.09f, shapePaint)
                    canvas.drawRect(ex + r, ey - r * 0.09f, ex + r + sp, ey + r * 0.09f, shapePaint)
                    shapePaint.color = if (((w.animTime * 6f).toInt() and 1) == 0) 0xFFFF3020.toInt() else 0xFF601810.toInt()
                    canvas.drawRect(ex - r * 0.22f, ey - r * 0.22f, ex + r * 0.22f, ey + r * 0.22f, shapePaint)
                }
                EnemyType.DIVER -> {                                           // a forward (downward) point
                    shapePaint.color = Palette.ENEMY_ACCENT[e.type.ordinal]
                    canvas.drawRect(ex - r * 0.18f, ey + r * 0.2f, ex + r * 0.18f, ey + r * 0.95f, shapePaint)
                }
                EnemyType.SHIELDER -> {
                    if (e.shieldHp > 0 && e.maxShieldHp > 0) {                 // bright frontal shield, facing you
                        val sFrac = e.shieldHp.toFloat() / e.maxShieldHp
                        val pulse = 0.7f + 0.3f * sin(w.animTime * 6f)
                        shapePaint.color = Color.argb((180 * pulse).toInt().coerceIn(60, 235), 120, 220, 255)
                        val sy = ey + r * 0.78f
                        canvas.drawRect(ex - r * 1.05f, sy, ex + r * 1.05f, sy + r * 0.26f, shapePaint)
                        shapePaint.color = 0xFFEAF6FF.toInt()                  // gauge: shrinks as it breaks
                        canvas.drawRect(ex - r * 1.05f, sy, ex - r * 1.05f + 2f * r * 1.05f * sFrac, sy + r * 0.10f, shapePaint)
                    }
                }
                else -> {}
            }
            // Tiny HP pips for multi-hit enemies.
            if (e.maxHp > 1 && !flash) {
                shapePaint.color = Palette.WHITE
                val frac = e.hp.toFloat() / e.maxHp
                canvas.drawRect(ex - r, ey - r * 1.25f, ex - r + 2f * r * frac, ey - r * 1.05f, shapePaint)
            }
        }
    }

    private fun drawBoss(canvas: Canvas, b: Boss) {
        val w = world
        val r = b.radius
        val body = Palette.BOSS_BODY[b.variant]
        val eye = Palette.BOSS_EYE[b.variant]
        val foot = Palette.BOSS_FOOT[b.variant]
        if (b.colossal) { drawColossus(canvas, b, body, eye, foot); return }

        // Enrage aura: a pulsing red ring once the boss drops below the threshold.
        if (b.enraged) {
            val pulse = 0.5f + 0.5f * sin(w.animTime * 14f)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = w.minDim * 0.012f
            shapePaint.color = Color.argb((120 + pulse * 120).toInt().coerceIn(0, 255), 255, 60, 48)
            canvas.drawCircle(b.x, b.y, r * (1.12f + 0.06f * pulse), shapePaint)
            shapePaint.style = Paint.Style.FILL
        }

        // Barrier: a bright cyan shield while the boss commits to a laser (no damage gets through).
        if (b.invulnTime > 0f) {
            val pulse = 0.5f + 0.5f * sin(w.animTime * 16f)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = w.minDim * (0.016f + 0.008f * pulse)
            shapePaint.color = Color.argb((150 + pulse * 100).toInt().coerceIn(0, 255), 120, 230, 255)
            canvas.drawCircle(b.x, b.y, r * (1.22f + 0.05f * pulse), shapePaint)
            shapePaint.style = Paint.Style.FILL
        }

        shapePaint.color = if (b.hitFlash > 0f) Palette.WHITE else body
        canvas.drawRect(b.x - r, b.y - r * 0.65f, b.x + r, b.y + r * 0.65f, shapePaint)
        canvas.drawRect(b.x - r * 0.65f, b.y - r, b.x + r * 0.65f, b.y + r, shapePaint)

        // The OVERLORD wears a jagged crown of horns so it reads as the big one.
        if (b.isFinal) {
            shapePaint.color = if (b.hitFlash > 0f) Palette.WHITE else foot
            val hw = r * 0.15f
            for (k in -2..2) {
                val hx = b.x + k * r * 0.42f
                canvas.drawRect(hx - hw, b.y - r * 1.32f, hx + hw, b.y - r * 0.82f, shapePaint)
            }
        }

        shapePaint.color = eye
        canvas.drawRect(b.x - r * 0.45f, b.y - r * 0.15f, b.x - r * 0.10f, b.y + r * 0.25f, shapePaint)
        canvas.drawRect(b.x + r * 0.10f, b.y - r * 0.15f, b.x + r * 0.45f, b.y + r * 0.25f, shapePaint)
        shapePaint.color = foot
        canvas.drawRect(b.x - r, b.y + r * 0.55f, b.x - r * 0.5f, b.y + r, shapePaint)
        canvas.drawRect(b.x + r * 0.5f, b.y + r * 0.55f, b.x + r, b.y + r, shapePaint)

        drawBossHpBar(canvas, b)
    }

    /**
     * A colossus: a body far larger than the screen, anchored with its centre above the top edge so only
     * the lower hull, eyes and a glowing core loom into view. Sells the "巨物感" — a giant you fight a piece of.
     */
    private fun drawColossus(canvas: Canvas, b: Boss, body: Int, eye: Int, foot: Int) {
        val w = world
        val r = b.radius
        val flash = b.hitFlash > 0f
        val hullBottom = b.y + r * 0.92f

        // Enrage / barrier rings (vast — only the lower arc crosses the screen).
        if (b.enraged) {
            val pulse = 0.5f + 0.5f * sin(w.animTime * 14f)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = w.minDim * 0.02f
            shapePaint.color = Color.argb((120 + pulse * 120).toInt().coerceIn(0, 255), 255, 60, 48)
            canvas.drawCircle(b.x, b.y, r * 1.02f, shapePaint)
            shapePaint.style = Paint.Style.FILL
        }
        if (b.invulnTime > 0f) {
            val pulse = 0.5f + 0.5f * sin(w.animTime * 16f)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = w.minDim * (0.02f + 0.01f * pulse)
            shapePaint.color = Color.argb((150 + pulse * 100).toInt().coerceIn(0, 255), 120, 230, 255)
            canvas.drawCircle(b.x, b.y, r * 1.05f, shapePaint)
            shapePaint.style = Paint.Style.FILL
        }

        // Main hull slab — extends well off the top of the screen.
        shapePaint.color = if (flash) Palette.WHITE else body
        canvas.drawRect(b.x - r * 0.95f, b.y - r, b.x + r * 0.95f, hullBottom, shapePaint)

        // Lower lip + a jagged jaw of spikes (the "maw").
        shapePaint.color = if (flash) Palette.WHITE else foot
        canvas.drawRect(b.x - r * 0.95f, hullBottom - r * 0.14f, b.x + r * 0.95f, hullBottom, shapePaint)
        val spikes = 7
        val span = 2f * r * 0.95f
        val sw = span / spikes
        for (k in 0 until spikes) {
            val sx = b.x - r * 0.95f + (k + 0.5f) * sw
            canvas.drawRect(sx - sw * 0.32f, hullBottom, sx + sw * 0.32f, hullBottom + r * 0.12f, shapePaint)
        }

        if (!flash) {
            // Two big glowing eyes.
            val ey = hullBottom - r * 0.42f
            val ew = r * 0.20f; val eh = r * 0.15f
            shapePaint.color = eye
            canvas.drawRect(b.x - r * 0.5f - ew, ey - eh, b.x - r * 0.5f + ew, ey + eh, shapePaint)
            canvas.drawRect(b.x + r * 0.5f - ew, ey - eh, b.x + r * 0.5f + ew, ey + eh, shapePaint)
            // Pulsing central core (the obvious place to shoot).
            val glow = 0.6f + 0.4f * sin(w.animTime * 5f)
            val cr = r * 0.13f
            shapePaint.color = eye
            canvas.drawRect(b.x - cr * 1.3f, ey - cr * 0.8f, b.x + cr * 1.3f, ey + cr * 1.8f, shapePaint)
            shapePaint.color = Color.argb((glow * 255).toInt(), 255, 255, 255)
            canvas.drawRect(b.x - cr * 0.7f, ey - cr * 0.3f, b.x + cr * 0.7f, ey + cr * 1.3f, shapePaint)
        }

        drawBossHpBar(canvas, b)
    }

    /** HP bar across the top, inside the safe area (gold for the OVERLORD), with a thin frame. */
    private fun drawBossHpBar(canvas: Canvas, b: Boss) {
        val w = world
        val barW = w.minDim * 0.6f
        val barH = w.minDim * 0.03f
        val top = w.cy - w.radius + w.minDim * 0.055f
        shapePaint.color = Palette.BOSS_HP_BACK
        canvas.drawRect(w.cx - barW / 2f, top, w.cx + barW / 2f, top + barH, shapePaint)
        shapePaint.color = if (b.isFinal) Palette.BOSS_HP_FINAL else Palette.BOSS_HP
        val frac = (b.hp.toFloat() / b.maxHp).coerceIn(0f, 1f)
        canvas.drawRect(w.cx - barW / 2f, top, w.cx - barW / 2f + barW * frac, top + barH, shapePaint)
        shapePaint.style = Paint.Style.STROKE
        shapePaint.strokeWidth = w.minDim * 0.004f
        shapePaint.color = 0x66FFFFFF
        canvas.drawRect(w.cx - barW / 2f, top, w.cx + barW / 2f, top + barH, shapePaint)
        shapePaint.style = Paint.Style.FILL
    }

    private fun drawPowerUps(canvas: Canvas) {
        for (p in world.powerUps) {
            val r = p.radius
            val pulse = 0.7f + 0.3f * sin(p.wobble)
            val c = Palette.POWERUP_COLORS[p.type.ordinal]
            shapePaint.color = c
            val rr = r * pulse
            canvas.drawRect(p.x - rr, p.y - rr, p.x + rr, p.y + rr, shapePaint)
            shapePaint.color = Palette.POWERUP_INK
            canvas.drawRect(p.x - rr * 0.6f, p.y - rr * 0.6f, p.x + rr * 0.6f, p.y + rr * 0.6f, shapePaint)
            textPaint.color = c
            textPaint.textSize = r * 1.5f
            canvas.drawText(Palette.POWERUP_LABELS[p.type.ordinal], p.x, p.y + r * 0.55f, textPaint)
        }
    }

    private fun drawShockwaves(canvas: Canvas) {
        val w = world
        if (w.shockwaves.isEmpty()) return
        shapePaint.style = Paint.Style.STROKE
        for (s in w.shockwaves) {
            val t = (1f - s.life / s.maxLife).coerceIn(0f, 1f)   // 0 → 1 as it expands
            val rr = s.maxR * t
            val a = ((1f - t) * 200f).toInt().coerceIn(0, 255)
            shapePaint.strokeWidth = w.minDim * 0.012f * (1f - t * 0.6f)
            shapePaint.color = (s.color and 0x00FFFFFF) or (a shl 24)
            canvas.drawCircle(s.x, s.y, rr, shapePaint)
        }
        shapePaint.style = Paint.Style.FILL
    }

    private fun drawParticles(canvas: Canvas) {
        for (p in world.particles) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            shapePaint.color = (p.color and 0x00FFFFFF) or ((a * 255).toInt() shl 24)
            val s = p.size
            canvas.drawRect(p.x - s / 2f, p.y - s / 2f, p.x + s / 2f, p.y + s / 2f, shapePaint)
        }
    }

    private fun drawPopups(canvas: Canvas) {
        val w = world
        textPaint.textSize = w.minDim * 0.05f
        for (p in w.popups) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            textPaint.color = (p.color and 0x00FFFFFF) or ((a * 255).toInt() shl 24)
            canvas.drawText(p.text, p.x, p.y, textPaint)
        }
    }

    // =====================================================================
    //  HUD — everything anchored polar-style to the safe ring
    // =====================================================================
    private fun scoreString(): String {
        val s = world.score
        if (s != scoreCache) { scoreCache = s; scoreText = s.toString() }
        return scoreText
    }

    private fun bestString(): String {
        val b = world.best
        if (b != bestCache) { bestCache = b; bestText = "BEST $b" }
        return bestText
    }

    private fun statusString(): String {
        val w = world
        if (w.weapon != statusWeapon || w.powerLevel != statusPower) {
            statusWeapon = w.weapon; statusPower = w.powerLevel
            statusText = "${Palette.WEAPON_NAMES[w.weapon.ordinal]}·${w.powerLevel}"
        }
        return statusText
    }

    /** Draw a run of small pips along the safe ring, one per unit of [count]. */
    private fun drawRingPips(canvas: Canvas, count: Int, startDeg: Float, stepDeg: Float, color: Int, square: Boolean) {
        val w = world
        val rr = w.radius - w.margin * 1.35f
        val pip = w.minDim * 0.014f
        shapePaint.color = color
        for (i in 0 until count) {
            val a = (startDeg + i * stepDeg) * 0.017453292f      // deg → rad
            val px = w.cx + cos(a) * rr
            val py = w.cy + sin(a) * rr
            if (square) canvas.drawRect(px - pip, py - pip, px + pip, py + pip, shapePaint)
            else canvas.drawCircle(px, py, pip, shapePaint)
        }
    }

    private fun drawHud(canvas: Canvas) {
        val w = world
        // Score — the hero signal, top centre.
        textPaint.color = Palette.SCORE
        textPaint.textSize = w.minDim * 0.078f
        val top = w.cy - w.radius + w.minDim * 0.16f
        canvas.drawText(scoreString(), w.cx, top, textPaint)
        if (w.multiplier > 1) {
            textPaint.color = Palette.MULTIPLIER
            textPaint.textSize = w.minDim * 0.055f
            canvas.drawText(multTexts[w.multiplier], w.cx, top + w.minDim * 0.07f, textPaint)
        }

        // Lives arc lower-left of the ring, bombs lower-right — inside the circle,
        // where the old corner icons were clipped by the round mask.
        drawRingPips(canvas, w.lives, 122f, 11f, Palette.LIFE_PIP, square = false)
        drawRingPips(canvas, w.bombs, 58f, -11f, Palette.BOMB_PIP, square = true)

        // Current weapon + power level, bottom centre, just inside the chord.
        textPaint.color = Palette.WEAPON_COLORS[w.weapon.ordinal]
        textPaint.textSize = w.minDim * 0.045f
        val statusY = w.h - w.margin - w.minDim * 0.042f
        canvas.drawText(statusString(), w.cx, statusY, textPaint)
        // Escort drones read as tiny cyan dots beside the weapon tag.
        if (w.droneCount > 0) {
            val half = textPaint.measureText(statusText) / 2f
            val pip = w.minDim * 0.009f
            shapePaint.color = Palette.LIFE_PIP
            for (i in 0 until w.droneCount) {
                val px = w.cx + half + w.minDim * (0.03f + i * 0.028f)
                canvas.drawCircle(px, statusY - w.minDim * 0.014f, pip, shapePaint)
            }
        }

        // Overdrive gauge — a short arc dead-bottom of the safe ring; glows while surging.
        shapePaint.style = Paint.Style.STROKE
        shapePaint.strokeWidth = w.minDim * 0.018f
        val startA = 70f; val sweep = 40f
        shapePaint.color = Palette.OD_TRACK
        canvas.drawArc(arcRect, startA, sweep, false, shapePaint)
        if (w.overdriveTime > 0f) {
            val pulse = 0.5f + 0.5f * sin(w.animTime * 18f)
            shapePaint.color = Color.argb((180 + pulse * 70).toInt().coerceIn(0, 255), 120, 240, 255)
            canvas.drawArc(arcRect, startA, sweep, false, shapePaint)
        } else {
            val frac = (w.overdrive / OVERDRIVE_MAX).coerceIn(0f, 1f)
            if (frac > 0f) {
                shapePaint.color = if (frac >= 1f) Palette.OD_FULL else Palette.OD_FILL
                canvas.drawArc(arcRect, startA, sweep * frac, false, shapePaint)
            }
        }
        shapePaint.style = Paint.Style.FILL

        // Stage banner.
        if (w.stageBanner > 0f) {
            val a = (w.stageBanner / 1.4f).coerceIn(0f, 1f)
            textPaint.color = Color.argb((a * 255).toInt(),
                Palette.BANNER_INFO_R, Palette.BANNER_INFO_G, Palette.BANNER_INFO_B)
            textPaint.textSize = w.minDim * 0.10f
            val label = if (w.boss != null) "WARNING" else stageString()
            canvas.drawText(label, w.cx, w.cy - w.minDim * 0.02f, textPaint)
        }
    }

    private fun stageString(): String {
        val s = world.stage
        if (s != stageCache) { stageCache = s; stageText = "STAGE $s" }
        return stageText
    }

    /** Big transient call-out: boss name reveal, ENRAGED, WAVE CLEAR — drawn below centre. */
    private fun drawEventBanner(canvas: Canvas) {
        val w = world
        if (w.eventBannerTime <= 0f) return
        val a = (w.eventBannerTime / 0.6f).coerceIn(0f, 1f)          // fade over the last 0.6s
        val banner = w.eventBanner
        val rgb = when {
            banner == "ENRAGED" -> 0xFF3C30                          // alarm red
            banner == "WAVE CLEAR!" || banner.contains("OVERLORD") -> 0xFFC24B   // triumphant gold
            else -> 0x6FE3FF                                         // informational cyan
        }
        textPaint.color = ((a * 255).toInt() shl 24) or rgb
        textPaint.textSize = w.minDim * 0.085f
        canvas.drawText(banner, w.cx, w.cy + w.minDim * 0.13f, textPaint)
    }

    // =====================================================================
    //  Full-screen states
    // =====================================================================
    private fun drawTitle(canvas: Canvas) {
        val w = world
        val pulse = 0.6f + 0.4f * sin(w.animTime * 3f)

        if (w.best > 0) {
            textPaint.color = Palette.GOLD
            textPaint.textSize = w.minDim * 0.048f
            canvas.drawText(bestString(), w.cx, w.cy - w.minDim * 0.30f, textPaint)
        }

        // Retro chromatic title: a pink ghost offset under the cyan face.
        textPaint.textSize = w.minDim * 0.145f
        val ghost = w.minDim * 0.008f
        textPaint.color = Palette.TITLE_GHOST
        canvas.drawText("PIXEL", w.cx + ghost, w.cy - w.minDim * 0.13f + ghost, textPaint)
        canvas.drawText("SKIES", w.cx + ghost, w.cy + w.minDim * 0.015f + ghost, textPaint)
        textPaint.color = Palette.CYAN
        canvas.drawText("PIXEL", w.cx, w.cy - w.minDim * 0.13f, textPaint)
        canvas.drawText("SKIES", w.cx, w.cy + w.minDim * 0.015f, textPaint)

        // The jet idles under the title — a live screen, not a screenshot.
        val sway = sin(w.animTime * 1.3f) * w.minDim * 0.03f
        val bob = sin(w.animTime * 2.1f) * w.minDim * 0.012f
        drawJet(canvas, w.cx + sway, w.cy + w.minDim * 0.135f + bob, w.player.width, w.player.height)

        textPaint.color = Color.argb((pulse * 255).toInt(), 255, 243, 107)
        textPaint.textSize = w.minDim * 0.06f
        canvas.drawText("TAP TO START", w.cx, w.cy + w.minDim * 0.235f, textPaint)
        textPaint.color = Palette.HINT
        textPaint.textSize = w.minDim * 0.038f
        canvas.drawText("CROWN MOVES · TAP BOMBS", w.cx, w.cy + w.minDim * 0.31f, textPaint)
        canvas.drawText("HOLD TO PAUSE", w.cx, w.cy + w.minDim * 0.365f, textPaint)
    }

    private fun drawGameOver(canvas: Canvas) {
        val w = world
        dimPaint.color = Palette.SCRIM
        canvas.drawRect(0f, 0f, w.w, w.h, dimPaint)

        textPaint.color = Palette.PINK
        textPaint.textSize = w.minDim * 0.10f
        canvas.drawText("GAME OVER", w.cx, w.cy - w.minDim * 0.20f, textPaint)

        // The score is the hero — biggest thing on the screen.
        textPaint.color = Palette.WHITE
        textPaint.textSize = w.minDim * 0.115f
        canvas.drawText(scoreString(), w.cx, w.cy - w.minDim * 0.055f, textPaint)

        if (w.newBest) {
            val flash = 0.55f + 0.45f * sin(w.animTime * 6f)
            textPaint.color = Color.argb((flash * 255).toInt(), 255, 194, 75)
            textPaint.textSize = w.minDim * 0.06f
            canvas.drawText("NEW BEST!", w.cx, w.cy + w.minDim * 0.045f, textPaint)
        } else {
            textPaint.color = Palette.GOLD
            textPaint.textSize = w.minDim * 0.05f
            canvas.drawText(bestString(), w.cx, w.cy + w.minDim * 0.045f, textPaint)
        }

        if (w.score != overCache) {           // frozen at death, so this builds exactly once
            overCache = w.score
            overStats = "STAGE ${w.stage} · GRAZE ${w.grazeCount}"
        }
        textPaint.color = Palette.PAUSE_SUB
        textPaint.textSize = w.minDim * 0.042f
        canvas.drawText(overStats, w.cx, w.cy + w.minDim * 0.125f, textPaint)

        val pulse = 0.6f + 0.4f * sin(w.animTime * 3f)
        textPaint.color = Color.argb((pulse * 255).toInt(),
            Palette.BANNER_INFO_R, Palette.BANNER_INFO_G, Palette.BANNER_INFO_B)
        textPaint.textSize = w.minDim * 0.052f
        canvas.drawText("TAP TO RESTART", w.cx, w.cy + w.minDim * 0.225f, textPaint)
    }

    private fun drawPausedOverlay(canvas: Canvas) {
        val w = world
        dimPaint.color = Palette.SCRIM
        canvas.drawRect(0f, 0f, w.w, w.h, dimPaint)
        textPaint.color = Palette.WHITE
        textPaint.textSize = w.minDim * 0.12f
        canvas.drawText("PAUSED", w.cx, w.cy - w.minDim * 0.02f, textPaint)
        textPaint.color = Palette.PAUSE_SUB
        textPaint.textSize = w.minDim * 0.05f
        canvas.drawText(scoreString(), w.cx, w.cy + w.minDim * 0.085f, textPaint)
        canvas.drawText("TAP TO RESUME", w.cx, w.cy + w.minDim * 0.165f, textPaint)
    }
}
