package com.example.wearshooter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.wearshooter.GameConfig.BOSS_BASE_HP
import com.example.wearshooter.GameConfig.BOSS_INTERVAL
import com.example.wearshooter.GameConfig.BOSS_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.BULLET_H_FRACTION
import com.example.wearshooter.GameConfig.BULLET_SPEED
import com.example.wearshooter.GameConfig.BULLET_W_FRACTION
import com.example.wearshooter.GameConfig.COMBO_WINDOW
import com.example.wearshooter.GameConfig.EBULLET_MAX
import com.example.wearshooter.GameConfig.EBULLET_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.EBULLET_SPEED
import com.example.wearshooter.GameConfig.EDGE_MARGIN_FRACTION
import com.example.wearshooter.GameConfig.ENEMY_HP_SCALE
import com.example.wearshooter.GameConfig.ENEMY_MAX
import com.example.wearshooter.GameConfig.EXTRA_LIFE_EVERY
import com.example.wearshooter.GameConfig.GRAZE_BAND_FRACTION
import com.example.wearshooter.GameConfig.GRAZE_BONUS
import com.example.wearshooter.GameConfig.MAX_LIVES
import com.example.wearshooter.GameConfig.ENEMY_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.ENEMY_SPEED_BASE
import com.example.wearshooter.GameConfig.FIRE_INTERVAL
import com.example.wearshooter.GameConfig.GUNNER_FIRE_INTERVAL
import com.example.wearshooter.GameConfig.GUN_SPREAD
import com.example.wearshooter.GameConfig.INVULN_TIME
import com.example.wearshooter.GameConfig.KEY_SPEED
import com.example.wearshooter.GameConfig.MAX_BOMBS
import com.example.wearshooter.GameConfig.MAX_MULTIPLIER
import com.example.wearshooter.GameConfig.MAX_POWER
import com.example.wearshooter.GameConfig.PARTICLES_PER_KILL
import com.example.wearshooter.GameConfig.PLAYER_SIZE_FRACTION
import com.example.wearshooter.GameConfig.PLAYER_Y_FRACTION
import com.example.wearshooter.GameConfig.POWERUP_DROP_CHANCE
import com.example.wearshooter.GameConfig.POWERUP_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.POWERUP_SPEED
import com.example.wearshooter.GameConfig.ROTARY_INVERT
import com.example.wearshooter.GameConfig.ROTARY_SENSITIVITY
import com.example.wearshooter.GameConfig.SCORE_PER_KILL
import com.example.wearshooter.GameConfig.SPAWN_INTERVAL_MIN
import com.example.wearshooter.GameConfig.SPAWN_INTERVAL_START
import com.example.wearshooter.GameConfig.STAGE_DURATION
import com.example.wearshooter.GameConfig.START_BOMBS
import com.example.wearshooter.GameConfig.START_LIVES
import com.example.wearshooter.GameConfig.STAR_COUNT
import com.example.wearshooter.GameConfig.TANK_FIRE_INTERVAL
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Owns the whole game: state, entities, simulation, and drawing.
 *
 * Threading: [update] and [render] only ever run on the game-loop thread, back to
 * back, so the entity lists need no locking. Input methods (onTap / onLongPress /
 * onRotary / onKey) run on the UI thread and only touch @Volatile flags or a
 * synchronized rotary accumulator that the loop drains once per frame.
 */
class World {

    private enum class State { READY, PLAYING, PAUSED, GAME_OVER }

    /** Optional hook so the view can fire haptics from the game thread. */
    var vibrate: ((Long) -> Unit)? = null

    /** High-score persistence hooks (wired by the view to SharedPreferences). */
    var loadHighScore: (() -> Int)? = null
    var saveHighScore: ((Int) -> Unit)? = null

    // ---- Screen geometry ----
    private var w = 0f
    private var h = 0f
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private var minDim = 0f
    private var margin = 0f
    private var initialized = false

    // ---- Entities ----
    private val player = Player()
    private val bullets = ArrayList<Bullet>(64)
    private val enemies = ArrayList<Enemy>(ENEMY_MAX + 8)
    private val enemyBullets = ArrayList<EnemyBullet>(EBULLET_MAX + 16)
    private val powerUps = ArrayList<PowerUp>(16)
    private val particles = ArrayList<Particle>(192)
    private val popups = ArrayList<Popup>(24)
    private val stars = ArrayList<Star>(STAR_COUNT)
    private var boss: Boss? = null

    // ---- Run state ----
    private var state = State.READY
    private var score = 0
    private var best = 0
    private var elapsed = 0f
    private var animTime = 0f

    // Player progression
    private var lives = START_LIVES
    private var bombs = START_BOMBS
    private var powerLevel = 1
    private var shield = 0
    private var invuln = 0f
    private var weapon = WeaponType.VULCAN
    private var nextExtraLife = EXTRA_LIFE_EVERY

    // Combo / multiplier / graze
    private var multiplier = 1
    private var comboKills = 0
    private var comboTimer = 0f
    private var grazeCount = 0

    // Pacing
    private var fireTimer = 0f
    private var spawnTimer = 0f
    private var spawnInterval = SPAWN_INTERVAL_START
    private var stage = 1
    private var stageBanner = 0f
    private var bossTimer = 0f
    private var bossesBeaten = 0

    // FX
    private var shakeTime = 0f
    private var shakeMag = 0f
    private var bombFlash = 0f

    // ---- Input ----
    private val inputLock = Any()
    private var rotaryAccum = 0f
    @Volatile private var keyDir = 0
    @Volatile private var tapPending = false
    @Volatile private var longPressPending = false

    // ---- Reused paints ----
    private val shapePaint = Paint().apply { isAntiAlias = false }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val dimPaint = Paint()

    // =====================================================================
    //  Input (UI thread)
    // =====================================================================
    fun onRotary(delta: Float) { synchronized(inputLock) { rotaryAccum += delta } }
    fun onKey(dir: Int) { keyDir = dir }
    fun onTap() { tapPending = true }
    fun onLongPress() { longPressPending = true }
    fun onAppPause() { if (state == State.PLAYING) state = State.PAUSED }

    private fun consumeRotary(): Float =
        synchronized(inputLock) { val v = rotaryAccum; rotaryAccum = 0f; v }

    // =====================================================================
    //  Layout
    // =====================================================================
    fun onResize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        w = width.toFloat(); h = height.toFloat()
        cx = w / 2f; cy = h / 2f
        minDim = min(w, h); radius = minDim / 2f
        margin = minDim * EDGE_MARGIN_FRACTION

        player.width = minDim * PLAYER_SIZE_FRACTION
        player.height = player.width * 1.15f
        player.y = h * PLAYER_Y_FRACTION
        textPaint.textSize = minDim * 0.085f

        if (!initialized) {
            player.x = cx; initStars()
            best = loadHighScore?.invoke() ?: 0   // restore persisted high score
            initialized = true
        }
        clampPlayer()
    }

    /** Half-width of the round screen at a given y (chord of the circle). */
    private fun safeHalfWidthAt(y: Float): Float {
        val dy = y - cy
        val inside = radius * radius - dy * dy
        return if (inside <= 0f) 0f else sqrt(inside)
    }

    private fun clampPlayer() {
        val half = safeHalfWidthAt(player.y) - margin
        val minX = cx - half + player.width / 2f
        val maxX = cx + half - player.width / 2f
        if (minX <= maxX) player.x = player.x.coerceIn(minX, maxX)
    }

    // =====================================================================
    //  Update (game thread)
    // =====================================================================
    fun update(dt: Float) {
        if (!initialized) return
        animTime += dt
        if (shakeTime > 0f) shakeTime -= dt
        if (bombFlash > 0f) bombFlash -= dt

        val rotary = consumeRotary()
        if (tapPending) { tapPending = false; handleTap() }
        if (longPressPending) { longPressPending = false; handleLongPress() }

        when (state) {
            State.PLAYING -> updatePlaying(dt, rotary)
            State.READY, State.GAME_OVER -> { updateParticles(dt); updatePopups(dt) }
            State.PAUSED -> { /* frozen */ }
        }
        updateStars(dt, moving = state == State.PLAYING)
    }

    private fun handleTap() {
        when (state) {
            State.READY, State.GAME_OVER -> startGame()
            State.PAUSED -> state = State.PLAYING
            State.PLAYING -> useBomb()          // tap mid-flight drops a bomb
        }
    }

    private fun handleLongPress() {
        when (state) {
            State.PLAYING -> state = State.PAUSED
            State.PAUSED -> state = State.PLAYING
            else -> {}
        }
    }

    private fun startGame() {
        bullets.clear(); enemies.clear(); enemyBullets.clear()
        powerUps.clear(); particles.clear(); popups.clear(); boss = null
        score = 0; elapsed = 0f
        lives = START_LIVES; bombs = START_BOMBS; powerLevel = 1; shield = 0; invuln = 0f
        weapon = WeaponType.VULCAN; nextExtraLife = EXTRA_LIFE_EVERY
        multiplier = 1; comboKills = 0; comboTimer = 0f; grazeCount = 0
        fireTimer = 0f; spawnTimer = 0f; spawnInterval = SPAWN_INTERVAL_START
        stage = 1; stageBanner = 1.4f; bossTimer = 0f; bossesBeaten = 0
        shakeTime = 0f; bombFlash = 0f
        player.alive = true; player.x = cx
        state = State.PLAYING
    }

    private fun updatePlaying(dt: Float, rotary: Float) {
        elapsed += dt
        if (stageBanner > 0f) stageBanner -= dt
        if (invuln > 0f) invuln -= dt

        // Stage / difficulty progression.
        val newStage = 1 + (elapsed / STAGE_DURATION).toInt()
        if (newStage != stage) { stage = newStage; stageBanner = 1.4f }
        val diff = difficulty()

        // --- Move jet (crown first, arrow keys as fallback) ---
        var dx = rotary * ROTARY_SENSITIVITY
        if (ROTARY_INVERT) dx = -dx
        dx += keyDir * KEY_SPEED * dt
        player.x += dx
        clampPlayer()

        // --- Auto-fire ---
        fireTimer += dt
        while (fireTimer >= FIRE_INTERVAL) { fireTimer -= FIRE_INTERVAL; fireWeapon() }

        // --- Boss scheduling: between bosses, spawn the swarm ---
        val b = boss
        if (b == null) {
            bossTimer += dt
            if (bossTimer >= BOSS_INTERVAL) {
                spawnBoss()
            } else {
                spawnInterval = (SPAWN_INTERVAL_START / (1f + (diff - 1f) * 0.65f))
                    .coerceAtLeast(SPAWN_INTERVAL_MIN)
                spawnTimer += dt
                while (spawnTimer >= spawnInterval && enemies.size < ENEMY_MAX) {
                    spawnTimer -= spawnInterval
                    if (stage >= 2 && Random.nextFloat() < 0.14f) spawnFormation(diff)
                    else spawnEnemy(diff)
                }
            }
        } else {
            updateBoss(b, dt, diff)
        }

        updateBullets(dt)
        updateEnemies(dt, diff)
        updateEnemyBullets(dt)
        updatePowerUps(dt)
        updateParticles(dt)
        updatePopups(dt)
        checkCollisions()

        // Combo decay — let a chain lapse if you stop killing.
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) { multiplier = 1; comboKills = 0 }
        }

        bullets.removeAll { !it.alive }
        enemies.removeAll { !it.alive }
        enemyBullets.removeAll { !it.alive }
        powerUps.removeAll { !it.alive }
        if (boss?.alive == false) boss = null
    }

    /**
     * Continuous difficulty scalar, capped so it can't run away. Driven mainly by time
     * survived, bosses beaten, and the player's own power level (so enemies toughen as
     * your firepower grows). Score adds only a little — combos inflate score fast, so a
     * big score weight here would explode difficulty.
     */
    private fun difficulty(): Float =
        (1f + elapsed / 30f + bossesBeaten * 0.5f + (powerLevel - 1) * 0.16f + score / 30000f)
            .coerceAtMost(8f)

    /** Enemy HP scales up continuously with difficulty so they never become trivial. */
    private fun scaledHp(base: Int, diff: Float): Int =
        max(base, (base * (1f + (diff - 1f) * ENEMY_HP_SCALE)).roundToInt())

    // =====================================================================
    //  Player weapon
    // =====================================================================
    private fun fireWeapon() {
        val muzzleY = player.y - player.height * 0.55f
        val off = player.width * GUN_SPREAD
        when (weapon) {
            WeaponType.VULCAN -> fireVulcan(muzzleY, off)
            WeaponType.LASER -> fireLaser(muzzleY, off)
            WeaponType.WIDE -> fireWide(muzzleY, off)
        }
    }

    /**
     * Balanced all-rounder. Stream count grows to 5, then extra levels add damage and
     * a pair of angled wing shots — so it keeps getting stronger past the stream cap.
     */
    private fun fireVulcan(muzzleY: Float, off: Float) {
        val streams = min(2 + (powerLevel - 1), 5)        // 2..5
        val dmg = 1 + (powerLevel - 1) / 3                // 1..3
        val gap = off * 1.1f
        for (i in 0 until streams) {
            val x = player.x + (i - (streams - 1) / 2f) * gap
            bullets.add(Bullet(x, muzzleY, -BULLET_SPEED).also { it.damage = dmg })
        }
        if (powerLevel >= 6) {                            // outer angled wings at high power
            val edge = (streams / 2f + 0.6f) * gap
            bullets.add(Bullet(player.x - edge, muzzleY, -BULLET_SPEED, -BULLET_SPEED * 0.22f).also { it.damage = dmg })
            bullets.add(Bullet(player.x + edge, muzzleY, -BULLET_SPEED, BULLET_SPEED * 0.22f).also { it.damage = dmg })
        }
    }

    /** Piercing, high-speed, high-damage — punches through lines and shreds the boss. */
    private fun fireLaser(muzzleY: Float, off: Float) {
        val bolts = 1 + powerLevel / 2                   // 1..5
        val dmg = 2 + powerLevel / 2                      // 2..6
        val pierce = 3 + powerLevel                       // 4..11
        val vy = -BULLET_SPEED * 1.35f
        val gap = off * 1.2f
        for (i in 0 until bolts) {
            val x = player.x + (i - (bolts - 1) / 2f) * gap
            bullets.add(Bullet(x, muzzleY, vy).also { it.damage = dmg; it.pierce = pierce; it.kind = 1 })
        }
    }

    /** A broad fan of pellets — weak per shot but blankets the swarm; widens with power. */
    private fun fireWide(muzzleY: Float, off: Float) {
        val count = min(3 + powerLevel * 2, 13)           // 5..13 pellets
        val dmg = 1 + (powerLevel - 1) / 4                // 1..2
        val spread = 0.16f
        for (i in 0 until count) {
            val a = (i - (count - 1) / 2f) * spread
            bullets.add(Bullet(player.x, muzzleY, -BULLET_SPEED * 0.9f, sin(a) * BULLET_SPEED * 0.55f)
                .also { it.damage = dmg; it.kind = 2 })
        }
    }

    private fun updateBullets(dt: Float) {
        for (bl in bullets) {
            bl.x += bl.vx * dt
            bl.y += bl.vy * dt
            if (bl.y < -minDim * 0.05f || bl.x < -minDim * 0.1f || bl.x > w + minDim * 0.1f) {
                bl.alive = false
            }
        }
    }

    // =====================================================================
    //  Enemies
    // =====================================================================
    private fun spawnEnemy(diff: Float) {
        val type = pickEnemyType(diff)
        val r = minDim * ENEMY_RADIUS_FRACTION * if (type == EnemyType.TANK) 1.5f else 1f
        val half = (safeHalfWidthAt(player.y) - margin - r).coerceAtLeast(minDim * 0.10f)
        val x = cx + (Random.nextFloat() * 2f - 1f) * half
        val e = Enemy(x, -r, r, type)
        e.baseX = x
        val speedScale = 1f + (diff - 1f) * 0.16f
        // Base HP per type; scaledHp() grows it with difficulty.
        when (type) {
            EnemyType.GRUNT -> {
                e.scoreValue = 1; setHp(e, 1, diff)
                e.vy = ENEMY_SPEED_BASE * speedScale
            }
            EnemyType.WEAVER -> {
                e.scoreValue = 2; setHp(e, 1, diff)
                e.vy = ENEMY_SPEED_BASE * 0.8f * speedScale
                e.swayAmp = minDim * (0.05f + Random.nextFloat() * 0.06f)
                e.wobble = Random.nextFloat() * 6.283f
            }
            EnemyType.RUSHER -> {
                e.scoreValue = 2; setHp(e, 1, diff)
                e.vy = ENEMY_SPEED_BASE * 2.0f * speedScale
            }
            EnemyType.GUNNER -> {
                e.scoreValue = 4; setHp(e, 3, diff)
                e.vy = ENEMY_SPEED_BASE * 0.7f * speedScale
                e.fireTimer = 0.6f + Random.nextFloat() * GUNNER_FIRE_INTERVAL
            }
            EnemyType.TANK -> {
                e.scoreValue = 8; setHp(e, 7, diff)
                e.vy = ENEMY_SPEED_BASE * 0.5f * speedScale
                e.fireTimer = 1.0f + Random.nextFloat() * TANK_FIRE_INTERVAL
            }
        }
        enemies.add(e)
    }

    private fun setHp(e: Enemy, base: Int, diff: Float) {
        e.maxHp = scaledHp(base, diff); e.hp = e.maxHp
    }

    /** A synchronized row of grunts that enters together — gives the swarm rhythm. */
    private fun spawnFormation(diff: Float) {
        val r = minDim * ENEMY_RADIUS_FRACTION
        val n = 3 + Random.nextInt(3)                       // 3..5 abreast
        val half = (safeHalfWidthAt(player.y) - margin - r).coerceAtLeast(minDim * 0.12f)
        val vy = ENEMY_SPEED_BASE * 0.9f * (1f + (diff - 1f) * 0.16f)
        val sway = if (Random.nextFloat() < 0.5f) minDim * 0.03f else 0f
        for (i in 0 until n) {
            if (enemies.size >= ENEMY_MAX) break
            val t = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            val x = cx - half + 2f * half * t
            val e = Enemy(x, -r - i * r * 0.4f, r, EnemyType.GRUNT)
            e.scoreValue = 1; setHp(e, 1, diff); e.vy = vy; e.baseX = x
            if (sway > 0f) { e.swayAmp = sway; e.wobble = t * 6.283f }
            enemies.add(e)
        }
    }

    /** Type weights shift toward tougher enemies as difficulty climbs. */
    private fun pickEnemyType(diff: Float): EnemyType {
        val gunner = ((diff - 1f) * 0.9f).coerceIn(0f, 3f)
        val tank = ((diff - 1.8f) * 0.5f).coerceIn(0f, 2f)
        val rusher = ((diff - 0.5f) * 0.5f).coerceIn(0f, 2f)
        val weights = floatArrayOf(3f, 1.4f, rusher, gunner, tank) // GRUNT,WEAVER,RUSHER,GUNNER,TANK
        val total = weights.sum()
        var pick = Random.nextFloat() * total
        for (i in weights.indices) {
            pick -= weights[i]
            if (pick <= 0f) return EnemyType.entries[i]
        }
        return EnemyType.GRUNT
    }

    private fun updateEnemies(dt: Float, diff: Float) {
        for (e in enemies) {
            if (e.hitFlash > 0f) e.hitFlash -= dt
            e.y += e.vy * dt
            if (e.swayAmp > 0f) {
                e.wobble += dt * 2.6f
                e.x = e.baseX + sin(e.wobble) * e.swayAmp
            }
            // Shooting enemies fire once on screen.
            if ((e.type == EnemyType.GUNNER || e.type == EnemyType.TANK) && e.y > 0f && e.y < cy) {
                e.fireTimer -= dt
                if (e.fireTimer <= 0f) {
                    val base = if (e.type == EnemyType.GUNNER) GUNNER_FIRE_INTERVAL else TANK_FIRE_INTERVAL
                    e.fireTimer = (base / (1f + (diff - 1f) * 0.25f)).coerceAtLeast(0.5f)
                    if (e.type == EnemyType.GUNNER) {
                        if (diff >= 3f) fanShot(e.x, e.y, e.radius, 3, diff)  // 3-fan once it's tough
                        else aimedShot(e.x, e.y, e.radius, diff)
                    } else {
                        fanShot(e.x, e.y, e.radius, if (diff >= 4f) 5 else 3, diff)
                    }
                }
            }
            if (e.y - e.radius > h) e.alive = false
        }
    }

    private fun aimedShot(x: Float, y: Float, r: Float, diff: Float) {
        if (enemyBullets.size >= EBULLET_MAX) return
        val ang = atan2(player.y - y, player.x - x)
        val sp = EBULLET_SPEED * (1f + (diff - 1f) * 0.12f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        enemyBullets.add(EnemyBullet(x, y + r, cos(ang) * sp, sin(ang) * sp, br))
    }

    private fun fanShot(x: Float, y: Float, r: Float, count: Int, diff: Float) {
        val sp = EBULLET_SPEED * (1f + (diff - 1f) * 0.12f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        val center = atan2(player.y - y, player.x - x)
        val spread = 0.5f
        for (i in 0 until count) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val a = center + (i - (count - 1) / 2f) * spread
            enemyBullets.add(EnemyBullet(x, y + r, cos(a) * sp, sin(a) * sp, br))
        }
    }

    private fun updateEnemyBullets(dt: Float) {
        val pad = minDim * 0.12f
        for (eb in enemyBullets) {
            eb.x += eb.vx * dt
            eb.y += eb.vy * dt
            if (eb.y > h + pad || eb.y < -pad || eb.x < -pad || eb.x > w + pad) eb.alive = false
        }
    }

    // =====================================================================
    //  Boss
    // =====================================================================
    private fun spawnBoss() {
        val r = minDim * BOSS_RADIUS_FRACTION
        val hp = BOSS_BASE_HP + bossesBeaten * 35
        boss = Boss(cx, -r, r, hp).also { it.variant = bossesBeaten % 2 }
        enemyBullets.clear()
        stageBanner = 1.4f
    }

    private fun updateBoss(b: Boss, dt: Float, diff: Float) {
        if (b.hitFlash > 0f) b.hitFlash -= dt
        b.phase += dt
        if (b.entering) {
            b.y += 90f * dt
            if (b.y >= cy * 0.55f) { b.y = cy * 0.55f; b.entering = false }
            return
        }
        // Sweep horizontally within the safe band (the crimson boss moves faster).
        val speed = if (b.variant == 0) 70f else 110f
        val half = safeHalfWidthAt(b.y) - margin - b.radius
        b.x += b.moveDir * speed * dt
        if (b.x > cx + half) { b.x = cx + half; b.moveDir = -1 }
        if (b.x < cx - half) { b.x = cx - half; b.moveDir = 1 }

        b.fireTimer -= dt
        if (b.fireTimer <= 0f) {
            if (b.variant == 0) {
                // Purple boss: fan -> ring -> gap-wall -> spiral.
                when ((b.phase / 3.2f).toInt() % 4) {
                    0 -> { b.fireTimer = 0.9f; fanShot(b.x, b.y, b.radius, 5, diff) }
                    1 -> { b.fireTimer = 1.3f; radialBurst(b.x, b.y, b.radius, 12, diff) }
                    2 -> { b.fireTimer = 1.25f; gapWall(b.y, diff) }
                    else -> { b.fireTimer = 0.12f; spiral(b, diff) }
                }
            } else {
                // Crimson boss: tight aimed bursts, gap-wall, wide ring, sweeping stream.
                when ((b.phase / 3.0f).toInt() % 4) {
                    0 -> { b.fireTimer = 0.55f; fanShot(b.x, b.y, b.radius, 3, diff) }
                    1 -> { b.fireTimer = 1.1f; gapWall(b.y, diff) }
                    2 -> { b.fireTimer = 1.4f; radialBurst(b.x, b.y, b.radius, 16, diff) }
                    else -> { b.fireTimer = 0.1f; streamDown(b, diff) }
                }
            }
        }
    }

    /** A raking column of straight-down bullets; combined with the boss's sweep it scythes across. */
    private fun streamDown(b: Boss, diff: Float) {
        val sp = EBULLET_SPEED * (1f + (diff - 1f) * 0.1f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        val xs = floatArrayOf(b.x, b.x - b.radius * 0.45f, b.x + b.radius * 0.45f)
        for (x in xs) {
            if (enemyBullets.size >= EBULLET_MAX) break
            enemyBullets.add(EnemyBullet(x, b.y + b.radius, 0f, sp, br))
        }
    }

    /** A horizontal curtain of falling bullets with one opening — slide into the gap. */
    private fun gapWall(y: Float, diff: Float) {
        val br = minDim * EBULLET_RADIUS_FRACTION
        val sp = EBULLET_SPEED * 0.8f * (1f + (diff - 1f) * 0.1f)
        val spanHalf = (safeHalfWidthAt(y) - margin).coerceAtLeast(minDim * 0.2f)
        val gapHalf = player.width * 1.15f
        val gapCenter = cx + (Random.nextFloat() * 2f - 1f) * spanHalf * 0.55f
        val count = 13
        for (i in 0 until count) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val x = cx - spanHalf + 2f * spanHalf * i / (count - 1)
            if (abs(x - gapCenter) < gapHalf) continue        // leave the opening
            enemyBullets.add(EnemyBullet(x, y, 0f, sp, br))
        }
    }

    /** Fired in rapid small bursts at a rotating angle, tracing two spiral arms. */
    private fun spiral(b: Boss, diff: Float) {
        val br = minDim * EBULLET_RADIUS_FRACTION
        val sp = EBULLET_SPEED * 0.85f * (1f + (diff - 1f) * 0.1f)
        val base = b.phase * 6f
        for (k in 0 until 2) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val a = base + k * 3.14159f
            enemyBullets.add(EnemyBullet(b.x, b.y, cos(a) * sp, sin(a) * sp + 25f, br))
        }
    }

    private fun radialBurst(x: Float, y: Float, r: Float, count: Int, diff: Float) {
        val sp = EBULLET_SPEED * 0.9f * (1f + (diff - 1f) * 0.1f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        val base = (animTime * 0.7f)
        for (i in 0 until count) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val a = base + i.toFloat() / count * 6.283f
            enemyBullets.add(EnemyBullet(x, y, cos(a) * sp, sin(a) * sp + 30f, br))
        }
    }

    private fun bossDefeated(b: Boss) {
        b.alive = false
        val bonus = 2000 * (1 + bossesBeaten)
        addScore(bonus)
        spawnPopup(b.x, b.y, "BOSS DOWN +$bonus", 0xFFFFF36B.toInt())
        bossesBeaten++
        bossTimer = 0f
        stage++; stageBanner = 1.6f
        shake(0.6f, minDim * 0.05f)
        bombFlash = 0.25f
        vibrate?.invoke(120)
        // Big burst + guaranteed rewards (incl. a weapon to try).
        repeat(6) { explode(b.x + (Random.nextFloat() - 0.5f) * b.radius,
            b.y + (Random.nextFloat() - 0.5f) * b.radius, b.radius * 0.6f) }
        dropPowerUp(b.x - b.radius * 0.5f, b.y, PowerType.POWER)
        dropPowerUp(b.x, b.y, PowerType.WEAPON)
        dropPowerUp(b.x + b.radius * 0.5f, b.y, PowerType.BOMB)
        enemyBullets.clear()
    }

    // =====================================================================
    //  Power-ups
    // =====================================================================
    private fun maybeDrop(x: Float, y: Float) {
        if (Random.nextFloat() > POWERUP_DROP_CHANCE) return
        val roll = Random.nextFloat()
        val type = when {
            roll < 0.42f -> PowerType.POWER
            roll < 0.64f -> PowerType.SHIELD
            roll < 0.82f -> PowerType.BOMB
            roll < 0.96f -> PowerType.WEAPON
            else -> PowerType.LIFE          // rare 1UP
        }
        dropPowerUp(x, y, type)
    }

    private fun dropPowerUp(x: Float, y: Float, type: PowerType) {
        powerUps.add(PowerUp(x, y, POWERUP_SPEED, type, minDim * POWERUP_RADIUS_FRACTION))
    }

    private fun updatePowerUps(dt: Float) {
        for (p in powerUps) {
            p.wobble += dt * 4f          // drives the draw-time pulse only
            p.y += p.vy * dt
            if (p.y - p.radius > h) p.alive = false
        }
    }

    private fun applyPowerUp(p: PowerUp) {
        when (p.type) {
            PowerType.POWER -> {
                if (powerLevel < MAX_POWER) { powerLevel++; spawnPopup(p.x, p.y, "POWER UP", 0xFF35D07F.toInt()) }
                else { addScore(500); spawnPopup(p.x, p.y, "+500", 0xFF35D07F.toInt()) }
            }
            PowerType.SHIELD -> { shield = (shield + 1).coerceAtMost(2); spawnPopup(p.x, p.y, "SHIELD", 0xFF66E0FF.toInt()) }
            PowerType.BOMB -> { bombs = (bombs + 1).coerceAtMost(MAX_BOMBS); spawnPopup(p.x, p.y, "BOMB", 0xFFFFC24B.toInt()) }
            PowerType.WEAPON -> cycleWeapon(p.x, p.y)
            PowerType.LIFE -> gainLife(p.x, p.y)
        }
        vibrate?.invoke(25)
        explode(p.x, p.y, p.radius)
    }

    private fun cycleWeapon(x: Float, y: Float) {
        weapon = when (weapon) {
            WeaponType.VULCAN -> WeaponType.LASER
            WeaponType.LASER -> WeaponType.WIDE
            WeaponType.WIDE -> WeaponType.VULCAN
        }
        spawnPopup(x, y, weaponName(weapon), 0xFFFF7AE0.toInt())
    }

    private fun gainLife(x: Float, y: Float) {
        if (lives < MAX_LIVES) lives++ else addScore(1000)
        spawnPopup(x, y, "1UP", 0xFF6FE3FF.toInt())
        vibrate?.invoke(40)
    }

    private fun weaponName(wt: WeaponType): String = when (wt) {
        WeaponType.VULCAN -> "VULCAN"
        WeaponType.LASER -> "LASER"
        WeaponType.WIDE -> "WIDE"
    }

    private fun weaponColor(wt: WeaponType): Int = when (wt) {
        WeaponType.VULCAN -> 0xFF8CF6FF.toInt()
        WeaponType.LASER -> 0xFFFF6BE3.toInt()
        WeaponType.WIDE -> 0xFFFFB23D.toInt()
    }

    // =====================================================================
    //  Bomb
    // =====================================================================
    private fun useBomb() {
        if (bombs <= 0) { vibrate?.invoke(15); return }
        bombs--
        bombFlash = 0.3f
        shake(0.4f, minDim * 0.04f)
        vibrate?.invoke(60)
        enemyBullets.clear()
        // Heavy damage to everything on screen.
        for (e in enemies) {
            if (!e.alive) continue
            e.hp -= 5
            if (e.hp <= 0) killEnemy(e) else e.hitFlash = 0.1f
        }
        boss?.let { b ->
            b.hp -= (b.maxHp * 0.12f).toInt().coerceAtLeast(8)
            b.hitFlash = 0.12f
            if (b.hp <= 0) bossDefeated(b)
        }
    }

    // =====================================================================
    //  Collisions
    // =====================================================================
    private fun checkCollisions() {
        // Player bullets vs enemies (laser shots pierce through several).
        for (bl in bullets) {
            if (!bl.alive) continue
            for (e in enemies) {
                if (!e.alive) continue
                val dx = bl.x - e.x; val dy = bl.y - e.y
                val rr = e.radius + minDim * 0.01f
                if (dx * dx + dy * dy <= rr * rr) {
                    e.hp -= bl.damage
                    if (e.hp <= 0) killEnemy(e) else e.hitFlash = 0.08f
                    if (bl.pierce > 0) bl.pierce-- else { bl.alive = false; break }
                }
            }
        }
        // Player bullets vs boss.
        boss?.let { b ->
            if (b.alive && !b.entering) {
                for (bl in bullets) {
                    if (!bl.alive) continue
                    val dx = bl.x - b.x; val dy = bl.y - b.y
                    if (dx * dx + dy * dy <= b.radius * b.radius) {
                        bl.alive = false
                        b.hp -= bl.damage
                        b.hitFlash = 0.05f
                        if (b.hp <= 0) { bossDefeated(b); return@let }
                    }
                }
            }
        }

        // Pick up power-ups (generous radius).
        val grabR = player.width * 0.8f
        for (p in powerUps) {
            if (!p.alive) continue
            if (hypot(p.x - player.x, p.y - player.y) <= grabR + p.radius) {
                p.alive = false
                applyPowerUp(p)
            }
        }

        val vulnerable = invuln <= 0f && player.alive
        if (!vulnerable) return
        val playerR = player.width * 0.36f
        val grazeBand = minDim * GRAZE_BAND_FRACTION

        // Enemy bullets vs player — a direct hit costs a life, a near-miss earns a graze.
        for (eb in enemyBullets) {
            if (!eb.alive) continue
            val d = hypot(eb.x - player.x, eb.y - player.y)
            val hitR = playerR + eb.radius
            if (d <= hitR) {
                eb.alive = false
                playerHit()
                return
            } else if (!eb.grazed && d <= hitR + grazeBand) {
                eb.grazed = true
                grazeCount++
                addScore(GRAZE_BONUS)
                if (comboTimer in 0.001f..0.45f) comboTimer = 0.45f   // keep a fading chain warm
                if (grazeCount % 10 == 0) {
                    spawnPopup(player.x, player.y - player.height * 1.1f, "GRAZE x$grazeCount", 0xFF66E0FF.toInt())
                    vibrate?.invoke(12)
                }
            }
        }
        // Enemy bodies vs player.
        for (e in enemies) {
            if (!e.alive) continue
            val hit = playerR + e.radius * 0.8f
            val dx = e.x - player.x; val dy = e.y - player.y
            if (dx * dx + dy * dy <= hit * hit) {
                e.hp -= 2; if (e.hp <= 0) killEnemy(e) else e.hitFlash = 0.1f
                playerHit()
                return
            }
        }
        // Boss body vs player.
        boss?.let { b ->
            if (b.alive && hypot(b.x - player.x, b.y - player.y) <= playerR + b.radius * 0.85f) {
                playerHit()
            }
        }
    }

    private fun killEnemy(e: Enemy) {
        e.alive = false
        bumpCombo()
        val gain = SCORE_PER_KILL * e.scoreValue * multiplier
        addScore(gain)
        if (e.scoreValue >= 4) spawnPopup(e.x, e.y, "+$gain", 0xFFFFFFFF.toInt())  // gunners/tanks
        explode(e.x, e.y, e.radius)
        maybeDrop(e.x, e.y)
    }

    private fun bumpCombo() {
        comboTimer = COMBO_WINDOW
        comboKills++
        // Each multiplier tier costs more kills, so x8 is genuinely earned.
        multiplier = (1 + comboKills / 3).coerceAtMost(MAX_MULTIPLIER)
    }

    private fun addScore(points: Int) {
        score += points
        if (score > best) best = score
        // Free life each time the score crosses a milestone.
        while (score >= nextExtraLife) {
            nextExtraLife += EXTRA_LIFE_EVERY
            if (lives < MAX_LIVES) {
                lives++
                spawnPopup(player.x, player.y - player.height * 1.4f, "1UP", 0xFF6FE3FF.toInt())
                vibrate?.invoke(40)
            }
        }
    }

    private fun spawnPopup(x: Float, y: Float, text: String, color: Int) {
        if (popups.size > 18) popups.removeAt(0)
        popups.add(Popup(x, y, text, color))
    }

    private fun updatePopups(dt: Float) {
        var i = 0
        while (i < popups.size) {
            val p = popups[i]
            p.y -= minDim * 0.35f * dt          // drift upward
            p.life -= dt
            if (p.life <= 0f) popups.removeAt(i) else i++
        }
    }

    private fun playerHit() {
        if (shield > 0) {
            shield--
            invuln = INVULN_TIME * 0.5f
            shake(0.25f, minDim * 0.03f)
            vibrate?.invoke(30)
            clearBulletsNearPlayer()
            return
        }
        lives--
        shake(0.5f, minDim * 0.05f)
        vibrate?.invoke(90)
        explode(player.x, player.y, player.width * 1.3f)
        if (lives <= 0) {
            player.alive = false
            if (score > best) best = score
            saveHighScore?.invoke(best)           // persist on game over
            state = State.GAME_OVER
            return
        }
        invuln = INVULN_TIME
        if (powerLevel > 1) powerLevel--          // classic on-death penalty
        multiplier = 1; comboKills = 0; comboTimer = 0f
        clearBulletsNearPlayer()
    }

    private fun clearBulletsNearPlayer() {
        val r = minDim * 0.28f
        for (eb in enemyBullets) {
            if (hypot(eb.x - player.x, eb.y - player.y) <= r) eb.alive = false
        }
    }

    // =====================================================================
    //  Particles & FX
    // =====================================================================
    private fun explode(x: Float, y: Float, baseSize: Float) {
        repeat(PARTICLES_PER_KILL) {
            val ang = Random.nextFloat() * 6.283f
            val spd = 40f + Random.nextFloat() * 200f
            val p = Particle(x, y, cos(ang) * spd, sin(ang) * spd)
            p.maxLife = 0.35f + Random.nextFloat() * 0.45f
            p.life = p.maxLife
            p.size = baseSize * 0.22f * (0.6f + Random.nextFloat() * 0.8f)
            p.color = EXPLOSION_COLORS[Random.nextInt(EXPLOSION_COLORS.size)]
            particles.add(p)
        }
    }

    private fun updateParticles(dt: Float) {
        var i = 0
        while (i < particles.size) {
            val p = particles[i]
            p.x += p.vx * dt; p.y += p.vy * dt
            p.vx *= 0.90f; p.vy = p.vy * 0.90f + 70f * dt
            p.life -= dt
            if (p.life <= 0f) particles.removeAt(i) else i++
        }
    }

    private fun shake(time: Float, mag: Float) {
        if (time > shakeTime) { shakeTime = time; shakeMag = mag }
    }

    // =====================================================================
    //  Starfield
    // =====================================================================
    private class Star(var x: Float, var y: Float, var speed: Float, var size: Float, var bright: Int)

    private fun initStars() {
        stars.clear()
        repeat(STAR_COUNT) { stars.add(makeStar(Random.nextFloat() * h)) }
    }

    private fun makeStar(y: Float): Star {
        val depth = Random.nextFloat()
        return Star(Random.nextFloat() * w, y, 18f + depth * 90f,
            minDim * (0.004f + depth * 0.010f), (70 + depth * 150).toInt())
    }

    private fun updateStars(dt: Float, moving: Boolean) {
        val f = if (moving) 1f else 0.35f
        for (s in stars) {
            s.y += s.speed * dt * f
            if (s.y - s.size > h) { s.y = -s.size; s.x = Random.nextFloat() * w }
        }
    }

    // =====================================================================
    //  Render (game thread)
    // =====================================================================
    fun render(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        if (!initialized) return

        val shaking = shakeTime > 0f
        if (shaking) {
            val k = (shakeTime).coerceIn(0f, 1f) * shakeMag
            canvas.save()
            canvas.translate((Random.nextFloat() - 0.5f) * 2f * k, (Random.nextFloat() - 0.5f) * 2f * k)
        }

        drawStars(canvas)

        if (state == State.PLAYING || state == State.PAUSED || state == State.GAME_OVER) {
            boss?.let { drawBoss(canvas, it) }
            drawPowerUps(canvas)
            drawEnemies(canvas)
            drawEnemyBullets(canvas)
            drawBullets(canvas)
            if (player.alive) drawPlayer(canvas)
        }
        drawParticles(canvas)

        // Faint ring so the round safe-area reads on the square emulator.
        shapePaint.style = Paint.Style.STROKE
        shapePaint.strokeWidth = minDim * 0.006f
        shapePaint.color = 0x22FFFFFF
        canvas.drawCircle(cx, cy, radius - margin * 0.4f, shapePaint)
        shapePaint.style = Paint.Style.FILL

        if (shaking) canvas.restore()

        // Bomb / boss-death white flash (drawn after un-shaking, full screen).
        if (bombFlash > 0f) {
            dimPaint.color = Color.argb(((bombFlash / 0.3f) * 160).toInt().coerceIn(0, 200), 255, 255, 255)
            canvas.drawRect(0f, 0f, w, h, dimPaint)
        }

        drawPopups(canvas)

        when (state) {
            State.PLAYING -> drawHud(canvas)
            State.PAUSED -> { drawHud(canvas); drawPausedOverlay(canvas) }
            State.READY -> drawTitle(canvas)
            State.GAME_OVER -> drawGameOver(canvas)
        }
    }

    private fun drawStars(canvas: Canvas) {
        for (s in stars) {
            shapePaint.color = Color.argb(s.bright, 255, 255, 255)
            canvas.drawRect(s.x, s.y, s.x + s.size, s.y + s.size, shapePaint)
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        // Blink while invulnerable.
        if (invuln > 0f && ((invuln * 12f).toInt() and 1) == 0) return
        val px = player.x; val py = player.y
        val pw = player.width; val ph = player.height

        // Shield ring.
        if (shield > 0) {
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = minDim * 0.012f
            shapePaint.color = if (((animTime * 8f).toInt() and 1) == 0) 0xCC66E0FF.toInt() else 0x6666E0FF
            canvas.drawCircle(px, py, pw * 0.85f, shapePaint)
            shapePaint.style = Paint.Style.FILL
        }

        val flame = 0.5f + 0.5f * sin(animTime * 30f)
        shapePaint.color = 0xFFFFC24B.toInt()
        canvas.drawRect(px - pw * 0.09f, py + ph * 0.42f, px + pw * 0.09f, py + ph * (0.42f + 0.30f * flame), shapePaint)
        shapePaint.color = 0xFF2FA9D6.toInt()
        canvas.drawRect(px - pw * 0.50f, py - ph * 0.02f, px + pw * 0.50f, py + ph * 0.20f, shapePaint)
        canvas.drawRect(px - pw * 0.28f, py + ph * 0.30f, px + pw * 0.28f, py + ph * 0.44f, shapePaint)
        shapePaint.color = 0xFF6FE3FF.toInt()
        canvas.drawRect(px - pw * 0.13f, py - ph * 0.50f, px + pw * 0.13f, py + ph * 0.42f, shapePaint)
        canvas.drawRect(px - pw * 0.05f, py - ph * 0.62f, px + pw * 0.05f, py - ph * 0.46f, shapePaint)
        shapePaint.color = 0xFFFFFFFF.toInt()
        canvas.drawRect(px - pw * 0.06f, py - ph * 0.28f, px + pw * 0.06f, py - ph * 0.08f, shapePaint)
    }

    private fun drawBullets(canvas: Canvas) {
        val bw = minDim * BULLET_W_FRACTION
        val bh = minDim * BULLET_H_FRACTION
        for (bl in bullets) {
            when (bl.kind) {
                1 -> { // LASER: narrow tall magenta beam, white core
                    val lw = bw * 0.55f; val lh = bh * 1.7f
                    shapePaint.color = 0xFFFF6BE3.toInt()
                    canvas.drawRect(bl.x - lw, bl.y - lh, bl.x + lw, bl.y + lh, shapePaint)
                    shapePaint.color = 0xFFFFFFFF.toInt()
                    canvas.drawRect(bl.x - lw * 0.4f, bl.y - lh, bl.x + lw * 0.4f, bl.y + lh, shapePaint)
                }
                2 -> { // WIDE: small orange pellet
                    val s = bw * 1.05f
                    shapePaint.color = 0xFFFFB23D.toInt()
                    canvas.drawRect(bl.x - s, bl.y - s, bl.x + s, bl.y + s, shapePaint)
                    shapePaint.color = 0xFFFFF0C0.toInt()
                    canvas.drawRect(bl.x - s * 0.4f, bl.y - s * 0.4f, bl.x + s * 0.4f, bl.y + s * 0.4f, shapePaint)
                }
                else -> { // VULCAN: cyan bolt
                    shapePaint.color = 0xFF8CF6FF.toInt()
                    canvas.drawRect(bl.x - bw / 2f, bl.y - bh / 2f, bl.x + bw / 2f, bl.y + bh / 2f, shapePaint)
                    shapePaint.color = 0xFFFFFFFF.toInt()
                    canvas.drawRect(bl.x - bw * 0.22f, bl.y - bh * 0.40f, bl.x + bw * 0.22f, bl.y + bh * 0.40f, shapePaint)
                }
            }
        }
    }

    private fun drawEnemyBullets(canvas: Canvas) {
        for (eb in enemyBullets) {
            val r = eb.radius
            shapePaint.color = 0xFFFF3B30.toInt()                       // bright red, readable
            canvas.drawRect(eb.x - r, eb.y - r, eb.x + r, eb.y + r, shapePaint)
            shapePaint.color = 0xFFFFE08A.toInt()                       // hot core
            canvas.drawRect(eb.x - r * 0.45f, eb.y - r * 0.45f, eb.x + r * 0.45f, eb.y + r * 0.45f, shapePaint)
        }
    }

    private fun drawEnemies(canvas: Canvas) {
        for (e in enemies) {
            val ex = e.x; val ey = e.y; val r = e.radius
            val flash = e.hitFlash > 0f
            val (body, accent) = enemyColors(e.type, flash)
            shapePaint.color = body
            canvas.drawRect(ex - r, ey - r * 0.6f, ex + r, ey + r * 0.6f, shapePaint)
            canvas.drawRect(ex - r * 0.6f, ey - r, ex + r * 0.6f, ey + r, shapePaint)
            shapePaint.color = 0xFF120712.toInt()
            canvas.drawRect(ex - r * 0.50f, ey - r * 0.18f, ex - r * 0.14f, ey + r * 0.22f, shapePaint)
            canvas.drawRect(ex + r * 0.14f, ey - r * 0.18f, ex + r * 0.50f, ey + r * 0.22f, shapePaint)
            shapePaint.color = accent
            canvas.drawRect(ex - r, ey + r * 0.5f, ex - r * 0.5f, ey + r, shapePaint)
            canvas.drawRect(ex + r * 0.5f, ey + r * 0.5f, ex + r, ey + r, shapePaint)
            // Tiny HP pips for multi-hit enemies.
            if (e.maxHp > 1 && !flash) {
                shapePaint.color = 0xFFFFFFFF.toInt()
                val frac = e.hp.toFloat() / e.maxHp
                canvas.drawRect(ex - r, ey - r * 1.25f, ex - r + 2f * r * frac, ey - r * 1.05f, shapePaint)
            }
        }
    }

    private fun enemyColors(type: EnemyType, flash: Boolean): Pair<Int, Int> {
        if (flash) return Pair(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        return when (type) {
            EnemyType.GRUNT -> Pair(0xFFFF4D8D.toInt(), 0xFFFFC24B.toInt())
            EnemyType.WEAVER -> Pair(0xFFB36BFF.toInt(), 0xFFE0B3FF.toInt())
            EnemyType.RUSHER -> Pair(0xFFFF7A3D.toInt(), 0xFFFFE08A.toInt())
            EnemyType.GUNNER -> Pair(0xFF35D07F.toInt(), 0xFFCFFF8A.toInt())
            EnemyType.TANK -> Pair(0xFF6E7BFF.toInt(), 0xFF9AE0FF.toInt())
        }
    }

    private fun drawBoss(canvas: Canvas, b: Boss) {
        val r = b.radius
        val crimson = b.variant != 0
        shapePaint.color = when {
            b.hitFlash > 0f -> 0xFFFFFFFF.toInt()
            crimson -> 0xFFE23B5A.toInt()
            else -> 0xFF8A3DFF.toInt()
        }
        canvas.drawRect(b.x - r, b.y - r * 0.65f, b.x + r, b.y + r * 0.65f, shapePaint)
        canvas.drawRect(b.x - r * 0.65f, b.y - r, b.x + r * 0.65f, b.y + r, shapePaint)
        shapePaint.color = if (crimson) 0xFFFFE08A.toInt() else 0xFFFF3B30.toInt()   // eyes
        canvas.drawRect(b.x - r * 0.45f, b.y - r * 0.15f, b.x - r * 0.10f, b.y + r * 0.25f, shapePaint)
        canvas.drawRect(b.x + r * 0.10f, b.y - r * 0.15f, b.x + r * 0.45f, b.y + r * 0.25f, shapePaint)
        shapePaint.color = if (crimson) 0xFF7A1830.toInt() else 0xFFFFC24B.toInt()   // feet
        canvas.drawRect(b.x - r, b.y + r * 0.55f, b.x - r * 0.5f, b.y + r, shapePaint)
        canvas.drawRect(b.x + r * 0.5f, b.y + r * 0.55f, b.x + r, b.y + r, shapePaint)

        // HP bar across the top, inside the safe area.
        val barW = minDim * 0.6f
        val barH = minDim * 0.03f
        val top = cy - radius + minDim * 0.06f
        shapePaint.color = 0xFF3A0A12.toInt()
        canvas.drawRect(cx - barW / 2f, top, cx + barW / 2f, top + barH, shapePaint)
        shapePaint.color = 0xFFFF3B30.toInt()
        val frac = (b.hp.toFloat() / b.maxHp).coerceIn(0f, 1f)
        canvas.drawRect(cx - barW / 2f, top, cx - barW / 2f + barW * frac, top + barH, shapePaint)
    }

    private fun drawPowerUps(canvas: Canvas) {
        for (p in powerUps) {
            val r = p.radius
            val pulse = 0.7f + 0.3f * sin(p.wobble)
            val (c, label) = when (p.type) {
                PowerType.POWER -> Pair(0xFF35D07F.toInt(), "P")
                PowerType.SHIELD -> Pair(0xFF66E0FF.toInt(), "S")
                PowerType.BOMB -> Pair(0xFFFFC24B.toInt(), "B")
                PowerType.WEAPON -> Pair(0xFFFF7AE0.toInt(), "W")
                PowerType.LIFE -> Pair(0xFF6FE3FF.toInt(), "1")
            }
            shapePaint.color = c
            val rr = r * pulse
            canvas.drawRect(p.x - rr, p.y - rr, p.x + rr, p.y + rr, shapePaint)
            shapePaint.color = 0xFF06121A.toInt()
            canvas.drawRect(p.x - rr * 0.6f, p.y - rr * 0.6f, p.x + rr * 0.6f, p.y + rr * 0.6f, shapePaint)
            textPaint.color = c
            textPaint.textSize = r * 1.5f
            canvas.drawText(label, p.x, p.y + r * 0.55f, textPaint)
        }
    }

    private fun drawParticles(canvas: Canvas) {
        for (p in particles) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            shapePaint.color = (p.color and 0x00FFFFFF) or ((a * 255).toInt() shl 24)
            val s = p.size
            canvas.drawRect(p.x - s / 2f, p.y - s / 2f, p.x + s / 2f, p.y + s / 2f, shapePaint)
        }
    }

    private fun drawPopups(canvas: Canvas) {
        textPaint.textSize = minDim * 0.05f
        for (p in popups) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            textPaint.color = (p.color and 0x00FFFFFF) or ((a * 255).toInt() shl 24)
            canvas.drawText(p.text, p.x, p.y, textPaint)
        }
    }

    // ---- HUD ----
    private fun drawHud(canvas: Canvas) {
        // Score + multiplier, top centre.
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = minDim * 0.072f
        val top = cy - radius + minDim * 0.16f
        canvas.drawText(score.toString(), cx, top, textPaint)
        if (multiplier > 1) {
            textPaint.color = 0xFFFFF36B.toInt()
            textPaint.textSize = minDim * 0.055f
            canvas.drawText("x$multiplier", cx, top + minDim * 0.07f, textPaint)
        }

        // Lives (left) and bombs (right) along the lower arc.
        val iconY = h * PLAYER_Y_FRACTION + player.height * 0.1f
        val s = minDim * 0.022f
        shapePaint.color = 0xFF6FE3FF.toInt()
        for (i in 0 until lives) {
            val lx = margin + minDim * 0.03f + i * s * 3.2f
            canvas.drawRect(lx - s, iconY, lx + s, iconY + s * 1.6f, shapePaint)         // tiny jet body
            canvas.drawRect(lx - s * 1.8f, iconY + s * 0.6f, lx + s * 1.8f, iconY + s * 1.1f, shapePaint) // wings
        }
        shapePaint.color = 0xFFFFC24B.toInt()
        for (i in 0 until bombs) {
            val bx = w - margin - minDim * 0.03f - i * s * 3.0f
            canvas.drawRect(bx - s, iconY - s, bx + s, iconY + s, shapePaint)
        }

        // Current weapon + power level, bottom centre (a number, so it scales forever).
        textPaint.color = weaponColor(weapon)
        textPaint.textSize = minDim * 0.05f
        canvas.drawText("${weaponName(weapon)}  Lv$powerLevel", cx, h - margin - minDim * 0.03f, textPaint)

        // Stage banner.
        if (stageBanner > 0f) {
            val a = (stageBanner / 1.4f).coerceIn(0f, 1f)
            textPaint.color = Color.argb((a * 255).toInt(), 111, 227, 255)
            textPaint.textSize = minDim * 0.10f
            val label = if (boss != null) "WARNING" else "STAGE $stage"
            canvas.drawText(label, cx, cy - minDim * 0.02f, textPaint)
        }
    }

    private fun drawTitle(canvas: Canvas) {
        val pulse = 0.6f + 0.4f * sin(animTime * 3f)
        if (best > 0) {
            textPaint.color = 0xFFFFC24B.toInt()
            textPaint.textSize = minDim * 0.05f
            canvas.drawText("BEST $best", cx, cy - minDim * 0.30f, textPaint)
        }
        textPaint.color = 0xFF6FE3FF.toInt()
        textPaint.textSize = minDim * 0.14f
        canvas.drawText("PIXEL", cx, cy - minDim * 0.12f, textPaint)
        canvas.drawText("SKIES", cx, cy + minDim * 0.02f, textPaint)
        textPaint.color = Color.argb((pulse * 255).toInt(), 255, 243, 107)
        textPaint.textSize = minDim * 0.062f
        canvas.drawText("TAP TO START", cx, cy + minDim * 0.18f, textPaint)
        textPaint.color = 0x99FFFFFF.toInt()
        textPaint.textSize = minDim * 0.045f
        canvas.drawText("CROWN = MOVE", cx, cy + minDim * 0.27f, textPaint)
        canvas.drawText("TAP = BOMB", cx, cy + minDim * 0.33f, textPaint)
    }

    private fun drawGameOver(canvas: Canvas) {
        textPaint.color = 0xFFFF4D8D.toInt()
        textPaint.textSize = minDim * 0.115f
        canvas.drawText("GAME OVER", cx, cy - minDim * 0.16f, textPaint)
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = minDim * 0.082f
        canvas.drawText("SCORE $score", cx, cy - minDim * 0.02f, textPaint)
        textPaint.color = 0xFFFFC24B.toInt()
        textPaint.textSize = minDim * 0.058f
        canvas.drawText("BEST $best", cx, cy + minDim * 0.07f, textPaint)
        if (grazeCount > 0) {
            textPaint.color = 0xFF66E0FF.toInt()
            textPaint.textSize = minDim * 0.05f
            canvas.drawText("GRAZE $grazeCount", cx, cy + minDim * 0.15f, textPaint)
        }
        val pulse = 0.6f + 0.4f * sin(animTime * 3f)
        textPaint.color = Color.argb((pulse * 255).toInt(), 111, 227, 255)
        textPaint.textSize = minDim * 0.055f
        canvas.drawText("TAP TO RESTART", cx, cy + minDim * 0.23f, textPaint)
    }

    private fun drawPausedOverlay(canvas: Canvas) {
        dimPaint.color = 0xAA000000.toInt()
        canvas.drawRect(0f, 0f, w, h, dimPaint)
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = minDim * 0.13f
        canvas.drawText("PAUSED", cx, cy - minDim * 0.02f, textPaint)
        textPaint.color = 0xFFB0B8C4.toInt()
        textPaint.textSize = minDim * 0.052f
        canvas.drawText("TAP TO RESUME", cx, cy + minDim * 0.12f, textPaint)
    }

    companion object {
        private val EXPLOSION_COLORS = intArrayOf(
            0xFFFFFFFF.toInt(), 0xFFFFF36B.toInt(),
            0xFFFFC24B.toInt(), 0xFFFF7A3D.toInt(), 0xFFFF4D8D.toInt()
        )
    }
}
