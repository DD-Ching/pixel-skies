package com.example.wearshooter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.wearshooter.GameConfig.BOSS_BASE_HP
import com.example.wearshooter.GameConfig.BOSS_HP_PER_KILL
import com.example.wearshooter.GameConfig.BOSS_INTENSITY_MAX
import com.example.wearshooter.GameConfig.BOSS_INTENSITY_PER_KILL
import com.example.wearshooter.GameConfig.BOSS_INTERVAL
import com.example.wearshooter.GameConfig.BOSS_INTERVAL_MIN
import com.example.wearshooter.GameConfig.BOSS_PLAYER_Y_FRACTION
import com.example.wearshooter.GameConfig.BOSS_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.RING_GAP_HALF
import com.example.wearshooter.GameConfig.BACKDROP_COUNT
import com.example.wearshooter.GameConfig.BACKDROP_FADE
import com.example.wearshooter.GameConfig.BACKDROP_STAGES_EACH
import com.example.wearshooter.GameConfig.COLOSSUS_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.DIFFICULTY_CAP
import com.example.wearshooter.GameConfig.DRONE_FOLLOW
import com.example.wearshooter.GameConfig.DRONE_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.ENRAGE_HP_FRACTION
import com.example.wearshooter.GameConfig.FINAL_BOSS_EVERY
import com.example.wearshooter.GameConfig.FINAL_BOSS_HP_MULT
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
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_HELIX
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_HOMING
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_LASER
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_VULCAN
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_WIDE
import com.example.wearshooter.GameConfig.FIRE_RATE_POWER_MIN
import com.example.wearshooter.GameConfig.FIRE_RATE_POWER_STEP
import com.example.wearshooter.GameConfig.GUNNER_FIRE_INTERVAL
import com.example.wearshooter.GameConfig.GUN_SPREAD
import com.example.wearshooter.GameConfig.INVULN_TIME
import com.example.wearshooter.GameConfig.KEY_SPEED
import com.example.wearshooter.GameConfig.LASER_FIRE
import com.example.wearshooter.GameConfig.LASER_HALFW_FRACTION
import com.example.wearshooter.GameConfig.LASER_MAX
import com.example.wearshooter.GameConfig.LASER_SWEEP_SPEED
import com.example.wearshooter.GameConfig.LASER_WARN
import com.example.wearshooter.GameConfig.MAX_BOMBS
import com.example.wearshooter.GameConfig.MAX_DRONES
import com.example.wearshooter.GameConfig.MAX_MULTIPLIER
import com.example.wearshooter.GameConfig.MAX_POWER
import com.example.wearshooter.GameConfig.OVERDRIVE_DURATION
import com.example.wearshooter.GameConfig.OVERDRIVE_FIRE_MULT
import com.example.wearshooter.GameConfig.OVERDRIVE_MAX
import com.example.wearshooter.GameConfig.OVERDRIVE_PER_GRAZE
import com.example.wearshooter.GameConfig.OVERDRIVE_PER_KILL
import com.example.wearshooter.GameConfig.PARTICLES_PER_KILL
import com.example.wearshooter.GameConfig.PLAYER_SIZE_FRACTION
import com.example.wearshooter.GameConfig.PLAYER_Y_FRACTION
import com.example.wearshooter.GameConfig.POWERUP_DROP_CHANCE
import com.example.wearshooter.GameConfig.POWERUP_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.POWERUP_SPEED
import com.example.wearshooter.GameConfig.ROTARY_INVERT
import com.example.wearshooter.GameConfig.ROTARY_SENSITIVITY
import com.example.wearshooter.GameConfig.SCORE_PER_KILL
import com.example.wearshooter.GameConfig.SHIELDER_BODY_HP
import com.example.wearshooter.GameConfig.SHIELDER_SHIELD_HP
import com.example.wearshooter.GameConfig.SHIELDER_TRACK_SPEED
import com.example.wearshooter.GameConfig.SPAWN_INTERVAL_MIN
import com.example.wearshooter.GameConfig.SPAWN_INTERVAL_START
import com.example.wearshooter.GameConfig.STAGE_DURATION
import com.example.wearshooter.GameConfig.START_BOMBS
import com.example.wearshooter.GameConfig.START_LIVES
import com.example.wearshooter.GameConfig.STAR_COUNT
import com.example.wearshooter.GameConfig.TANK_FIRE_INTERVAL
import com.example.wearshooter.GameConfig.UNLOCK_BOSS_BONUS
import com.example.wearshooter.GameConfig.UNLOCK_RAMP
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
    private var playerBaseY = 0f          // resting jet height; it rises from here during boss fights
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
    private val lasers = ArrayList<Laser>(LASER_MAX + 4)
    private val drones = ArrayList<Drone>(MAX_DRONES)
    private val shockwaves = ArrayList<Shockwave>(12)
    private var boss: Boss? = null
    // Enemies spawned mid-frame (e.g. splitter children) are buffered here and flushed
    // after the update loops so we never mutate `enemies` while iterating it.
    private val spawnQueue = ArrayList<Enemy>(8)

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
    private var droneCount = 0            // how many escort drones are currently deployed
    private var overdrive = 0f           // surge gauge, 0..OVERDRIVE_MAX
    private var overdriveTime = 0f       // >0 while the surge is firing

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
    private val announced = BooleanArray(EnemyType.entries.size)  // has each enemy type had its debut call-out?

    // FX
    private var shakeTime = 0f
    private var shakeMag = 0f
    private var slowmoTime = 0f          // brief cinematic slow-motion (boss kill)
    private var bombFlash = 0f
    private var eventBanner = ""        // big centred call-out (boss name reveal, OVERLORD DOWN…)
    private var eventBannerTime = 0f

    // Scenery: a themed backdrop that cycles as you progress (space → nebula → station → star → planet).
    private var theme = 0
    private var prevTheme = 0
    private var themeFade = 1f          // 0..1 crossfade into the current theme
    private var nebulaShader: Shader? = null
    private var starSurfaceShader: Shader? = null
    private var planetShader: Shader? = null
    private val arcRect = RectF()       // overdrive gauge arc bounds (allocated once)

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
        playerBaseY = h * PLAYER_Y_FRACTION
        player.y = playerBaseY
        textPaint.textSize = minDim * 0.085f
        buildBackdropShaders()
        // Overdrive gauge arc rides just inside the safe ring.
        val ar = radius - margin * 0.4f
        arcRect.set(cx - ar, cy - ar, cx + ar, cy + ar)

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

    /** Build the gradient shaders the themed backdrops reuse (allocation-free per frame). */
    private fun buildBackdropShaders() {
        nebulaShader = LinearGradient(0f, 0f, 0f, h,
            intArrayOf(0xFF120A2A.toInt(), 0xFF3A1D5C.toInt(), 0xFF14243F.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        starSurfaceShader = RadialGradient(cx, h * 1.02f, minDim * 0.95f,
            intArrayOf(0xFFFFF1B0.toInt(), 0xFFFF7A1E.toInt(), 0xFF5A1402.toInt(), 0x00000000),
            floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP)
        planetShader = LinearGradient(0f, h * 0.55f, 0f, h,
            intArrayOf(0xFF0B2A4A.toInt(), 0xFF1E6FA8.toInt(), 0xFF6FD0E0.toInt()),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
    }

    // =====================================================================
    //  Update (game thread)
    // =====================================================================
    fun update(dt: Float) {
        if (!initialized) return
        animTime += dt
        if (shakeTime > 0f) shakeTime -= dt
        if (bombFlash > 0f) bombFlash -= dt
        if (eventBannerTime > 0f) eventBannerTime -= dt

        val rotary = consumeRotary()
        if (tapPending) { tapPending = false; handleTap() }
        if (longPressPending) { longPressPending = false; handleLongPress() }

        when (state) {
            State.PLAYING -> {
                // Slow-mo decays in real time, but the simulation runs at a reduced step while it lasts.
                if (slowmoTime > 0f) slowmoTime -= dt
                val sim = if (slowmoTime > 0f) dt * 0.4f else dt
                updatePlaying(sim, rotary)
            }
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
        powerUps.clear(); particles.clear(); popups.clear(); spawnQueue.clear(); lasers.clear()
        drones.clear(); shockwaves.clear(); boss = null
        eventBanner = ""; eventBannerTime = 0f
        score = 0; elapsed = 0f
        lives = START_LIVES; bombs = START_BOMBS; powerLevel = 1; shield = 0; invuln = 0f
        weapon = WeaponType.VULCAN; nextExtraLife = EXTRA_LIFE_EVERY
        droneCount = 0; overdrive = 0f; overdriveTime = 0f
        multiplier = 1; comboKills = 0; comboTimer = 0f; grazeCount = 0
        fireTimer = 0f; spawnTimer = 0f; spawnInterval = SPAWN_INTERVAL_START
        stage = 1; stageBanner = 1.4f; bossTimer = 0f; bossesBeaten = 0
        announced.fill(false)
        shakeTime = 0f; bombFlash = 0f; slowmoTime = 0f
        theme = 0; prevTheme = 0; themeFade = 1f
        player.alive = true; player.x = cx; player.y = playerBaseY
        state = State.PLAYING
    }

    private fun updatePlaying(dt: Float, rotary: Float) {
        elapsed += dt
        if (stageBanner > 0f) stageBanner -= dt
        if (invuln > 0f) invuln -= dt
        if (overdriveTime > 0f) overdriveTime -= dt

        // Stage / difficulty progression.
        val newStage = 1 + (elapsed / STAGE_DURATION).toInt()
        if (newStage != stage) { stage = newStage; stageBanner = 1.4f }
        // Backdrop scene follows progression, crossfading whenever it advances.
        val newTheme = ((stage - 1) / BACKDROP_STAGES_EACH) % BACKDROP_COUNT
        if (newTheme != theme) { prevTheme = theme; theme = newTheme; themeFade = 0f }
        if (themeFade < 1f) themeFade = (themeFade + dt / BACKDROP_FADE).coerceAtMost(1f)
        val diff = difficulty()

        // --- Give the jet more room during a boss fight: it rises so the dodge band is wider
        //     and bullets take longer to arrive (a bigger, fairer arena against the big patterns). ---
        val inBossFight = boss?.let { !it.entering } == true
        val targetY = if (inBossFight) h * BOSS_PLAYER_Y_FRACTION else playerBaseY
        player.y += (targetY - player.y) * (dt * 2.2f).coerceAtMost(1f)

        // --- Move jet (crown first, arrow keys as fallback) ---
        var dx = rotary * ROTARY_SENSITIVITY
        if (ROTARY_INVERT) dx = -dx
        dx += keyDir * KEY_SPEED * dt
        player.x += dx
        clampPlayer()

        // --- Auto-fire: per-weapon cadence, faster with power, surged further by Overdrive ---
        fireTimer += dt
        val interval = weaponInterval() * (if (overdriveTime > 0f) OVERDRIVE_FIRE_MULT else 1f)
        while (fireTimer >= interval) { fireTimer -= interval; fireWeapon() }
        updateDrones(dt)

        // --- Boss scheduling: between bosses, spawn the swarm ---
        // Bosses arrive more often the deeper you get, so late play is wall-to-wall fights.
        val bossInterval = (BOSS_INTERVAL - bossesBeaten * 2f).coerceAtLeast(BOSS_INTERVAL_MIN)
        val b = boss
        if (b == null) {
            bossTimer += dt
            if (bossTimer >= bossInterval) {
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
        updateLasers(dt)
        updatePowerUps(dt)
        updateParticles(dt)
        updateShockwaves(dt)
        updatePopups(dt)
        checkCollisions()

        // Flush anything spawned mid-frame (splitter children) now that the loops are done.
        if (spawnQueue.isNotEmpty()) {
            for (e in spawnQueue) if (enemies.size < ENEMY_MAX) enemies.add(e)
            spawnQueue.clear()
        }

        // Combo decay — let a chain lapse if you stop killing.
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) { multiplier = 1; comboKills = 0 }
        }

        bullets.removeAll { !it.alive }
        enemies.removeAll { !it.alive }
        enemyBullets.removeAll { !it.alive }
        lasers.removeAll { !it.alive }
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
        (1f + elapsed / 36f + bossesBeaten * 0.5f + (powerLevel - 1) * 0.16f +
            droneCount * 0.14f + score / 40000f)
            .coerceAtMost(DIFFICULTY_CAP)

    /**
     * Bullet-density multiplier for boss/enemy patterns. Unlike [difficulty] (which is capped so
     * enemy stats can't explode), this keeps climbing with every boss beaten — so the *amount* of
     * lead in the air ramps endlessly into a proper bullet-hell, while speeds/HP stay sane.
     */
    private fun intensity(): Float =
        (1f + bossesBeaten * BOSS_INTENSITY_PER_KILL).coerceAtMost(BOSS_INTENSITY_MAX)

    /** Enemy HP scales up continuously with difficulty so they never become trivial. */
    private fun scaledHp(base: Int, diff: Float): Int =
        max(base, (base * (1f + (diff - 1f) * ENEMY_HP_SCALE)).roundToInt())

    // =====================================================================
    //  Player weapon
    // =====================================================================
    /** +1 to every shot's damage while Overdrive is surging. */
    private fun odBonus() = if (overdriveTime > 0f) 1 else 0

    /** Per-weapon base cadence, sped up as power level climbs (floored so it can't run away). */
    private fun weaponInterval(): Float {
        val base = when (weapon) {
            WeaponType.VULCAN -> FIRE_INTERVAL_VULCAN
            WeaponType.LASER -> FIRE_INTERVAL_LASER
            WeaponType.WIDE -> FIRE_INTERVAL_WIDE
            WeaponType.HOMING -> FIRE_INTERVAL_HOMING
            WeaponType.HELIX -> FIRE_INTERVAL_HELIX
        }
        val powerScale = (1f - (powerLevel - 1) * FIRE_RATE_POWER_STEP).coerceAtLeast(FIRE_RATE_POWER_MIN)
        return base * powerScale
    }

    private fun fireWeapon() {
        val muzzleY = player.y - player.height * 0.55f
        val off = player.width * GUN_SPREAD
        when (weapon) {
            WeaponType.VULCAN -> fireVulcan(muzzleY, off)
            WeaponType.LASER -> fireLaser(muzzleY, off)
            WeaponType.WIDE -> fireWide(muzzleY, off)
            WeaponType.HOMING -> fireHoming(muzzleY, off)
            WeaponType.HELIX -> fireHelix(muzzleY, off)
        }
        fireDrones()
    }

    /**
     * Balanced all-rounder. Stream count grows to 5, then extra levels add damage and
     * a pair of angled wing shots — so it keeps getting stronger past the stream cap.
     */
    private fun fireVulcan(muzzleY: Float, off: Float) {
        val streams = min(2 + (powerLevel - 1), 6)        // 2..6
        val dmg = 1 + (powerLevel - 1) / 3 + odBonus()    // 1..4 (+OD)
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
        val bolts = 1 + powerLevel / 2                   // 1..6
        val dmg = 2 + powerLevel / 2 + odBonus()          // 2..7 (+OD)
        val pierce = 3 + powerLevel                       // 4..13
        val vy = -BULLET_SPEED * 1.35f
        val gap = off * 1.2f
        for (i in 0 until bolts) {
            val x = player.x + (i - (bolts - 1) / 2f) * gap
            bullets.add(Bullet(x, muzzleY, vy).also { it.damage = dmg; it.pierce = pierce; it.kind = 1 })
        }
    }

    /** A broad fan of pellets — weak per shot but blankets the swarm; widens with power. */
    private fun fireWide(muzzleY: Float, off: Float) {
        val count = min(3 + powerLevel * 2, 15)           // 5..15 pellets
        val dmg = 1 + (powerLevel - 1) / 4 + odBonus()    // 1..3 (+OD)
        val spread = 0.16f
        for (i in 0 until count) {
            val a = (i - (count - 1) / 2f) * spread
            bullets.add(Bullet(player.x, muzzleY, -BULLET_SPEED * 0.9f, sin(a) * BULLET_SPEED * 0.55f)
                .also { it.damage = dmg; it.kind = 2 })
        }
    }

    /**
     * Seeking missiles — fewer shots, slower, but they curve onto whatever's nearest, so the
     * swarm and the boss get hunted down even while you focus on dodging. A real late-game treat.
     */
    private fun fireHoming(muzzleY: Float, off: Float) {
        val count = min(1 + powerLevel / 2, 5)            // 1..5 missiles
        val dmg = 2 + (powerLevel - 1) / 2 + odBonus()    // 2..6 (+OD)
        val speed = BULLET_SPEED * 0.82f
        for (i in 0 until count) {
            val x = player.x + (i - (count - 1) / 2f) * off * 1.4f
            bullets.add(Bullet(x, muzzleY, -speed).also {
                it.damage = dmg; it.kind = 3; it.homing = true; it.speed = speed; it.life = 2.6f
            })
        }
    }

    /**
     * HELIX — twin (then quad) bolts that weave around their travel line in opposite phase, braiding
     * into a double helix. Pierces a little, so it drills lines and chews through shields and bosses.
     */
    private fun fireHelix(muzzleY: Float, off: Float) {
        val dmg = 2 + powerLevel / 2 + odBonus()          // 2..7 (+OD)
        val pierce = 1 + powerLevel / 3                    // 1..4
        val amp = player.width * (0.8f + powerLevel * 0.05f)
        val freq = 17f
        val vy = -BULLET_SPEED * 1.1f
        fun strand(phase: Float) = bullets.add(Bullet(player.x, muzzleY, vy).also {
            it.damage = dmg; it.pierce = pierce; it.kind = 4
            it.wob = phase; it.wobAmp = amp; it.wobFreq = freq
        })
        strand(0f); strand(3.14159f)                      // the core double strand
        if (powerLevel >= 6) { strand(1.5708f); strand(4.7124f) }   // a denser quad weave at high power
    }

    /** Each escort drone adds a straight bolt per volley (two when surging), scaling firepower with the squad. */
    private fun fireDrones() {
        if (drones.isEmpty()) return
        val dmg = 1 + powerLevel / 4 + odBonus()          // 1..3 (+OD)
        val vy = -BULLET_SPEED * 1.05f
        for (d in drones) {
            bullets.add(Bullet(d.x, d.y - player.height * 0.4f, vy).also { it.damage = dmg; it.kind = 0 })
            if (overdriveTime > 0f) {                      // a second angled bolt during Overdrive
                bullets.add(Bullet(d.x, d.y - player.height * 0.4f, vy, BULLET_SPEED * 0.12f)
                    .also { it.damage = dmg; it.kind = 0 })
            }
        }
    }

    private fun updateBullets(dt: Float) {
        for (bl in bullets) {
            if (bl.homing) steerHoming(bl, dt)
            if (bl.wobAmp != 0f) {                          // HELIX: weave sideways around the travel line
                val prev = sin(bl.wob)
                bl.wob += bl.wobFreq * dt
                bl.x += (sin(bl.wob) - prev) * bl.wobAmp
            }
            bl.x += bl.vx * dt
            bl.y += bl.vy * dt
            if (bl.life > 0f) { bl.life -= dt; if (bl.life <= 0f) bl.alive = false }
            val offTop = bl.y < -minDim * 0.05f
            val offSide = bl.x < -minDim * 0.1f || bl.x > w + minDim * 0.1f
            val offBottom = bl.homing && bl.y > h + minDim * 0.1f   // seekers can dive; cull below too
            if (offTop || offSide || offBottom) bl.alive = false
        }
    }

    /** Curve a homing missile toward the nearest enemy/boss centre, keeping its speed constant. */
    private fun steerHoming(bl: Bullet, dt: Float) {
        var tx = 0f; var ty = 0f; var best = Float.MAX_VALUE; var found = false
        for (e in enemies) {
            if (!e.alive) continue
            val d = (e.x - bl.x) * (e.x - bl.x) + (e.y - bl.y) * (e.y - bl.y)
            if (d < best) { best = d; tx = e.x; ty = e.y; found = true }
        }
        boss?.let { b ->
            if (b.alive && !b.entering) {
                val d = (b.x - bl.x) * (b.x - bl.x) + (b.y - bl.y) * (b.y - bl.y)
                if (d < best) { best = d; tx = b.x; ty = b.y; found = true }
            }
        }
        if (!found) return
        val ang = atan2(ty - bl.y, tx - bl.x)
        val desiredVx = cos(ang) * bl.speed
        val desiredVy = sin(ang) * bl.speed
        val k = (dt * 7f).coerceAtMost(1f)               // turn rate
        bl.vx += (desiredVx - bl.vx) * k
        bl.vy += (desiredVy - bl.vy) * k
    }

    // =====================================================================
    //  Option drones (僚機) & Overdrive
    // =====================================================================
    /** Slide each escort toward its formation slot, fanned out beside and just behind the jet. */
    private fun updateDrones(dt: Float) {
        if (drones.isEmpty()) return
        val k = (dt * DRONE_FOLLOW).coerceAtMost(1f)
        val ty = player.y + player.height * 0.18f
        for (i in drones.indices) {
            val d = drones[i]
            val side = if (i % 2 == 0) -1f else 1f
            val rank = i / 2
            var tx = player.x + side * player.width * (1.15f + rank * 0.55f)
            val band = (safeHalfWidthAt(ty) - margin).coerceAtLeast(minDim * 0.1f)
            tx = tx.coerceIn(cx - band, cx + band)
            d.x += (tx - d.x) * k
            d.y += (ty - d.y) * k
        }
    }

    private fun addOverdrive(amount: Float) {
        if (overdriveTime > 0f) return                    // can't recharge mid-surge
        overdrive += amount
        if (overdrive >= OVERDRIVE_MAX) triggerOverdrive()
    }

    /** The gauge is full: unleash a few seconds of doubled fire-rate and harder-hitting shots. */
    private fun triggerOverdrive() {
        overdrive = 0f
        overdriveTime = OVERDRIVE_DURATION
        showBanner("OVERDRIVE", 1.0f)
        bombFlash = 0.18f
        shake(0.25f, minDim * 0.02f)
        vibrate?.invoke(50)
    }

    private fun gainDrone(x: Float, y: Float) {
        if (droneCount >= MAX_DRONES) { addScore(750); spawnPopup(x, y, "+750", 0xFF8CF6FF.toInt()); return }
        droneCount++
        drones.add(Drone(player.x, player.y + player.height))
        spawnPopup(x, y, "DRONE x$droneCount", 0xFF8CF6FF.toInt())
        vibrate?.invoke(35)
    }

    // =====================================================================
    //  Enemies
    // =====================================================================
    private fun spawnEnemy(diff: Float) {
        val type = pickEnemyType(diff)
        // First time a new enemy joins the fight, flash its name — so each addition is a deliberate beat.
        if (!announced[type.ordinal]) {
            announced[type.ordinal] = true
            if (enemyUnlockTime(type) > 0f) showBanner(enemyDisplayName(type), 1.2f)
        }
        val r = minDim * ENEMY_RADIUS_FRACTION * when (type) {
            EnemyType.TANK -> 1.5f
            EnemyType.SHIELDER -> 1.6f
            EnemyType.SPLITTER -> 1.25f
            EnemyType.MINE -> 1.15f
            EnemyType.DARTER -> 0.9f
            EnemyType.DIVER -> 0.95f
            else -> 1f
        }
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
            EnemyType.SPLITTER -> {
                // Drifts in steadily; bursts into a spray of fast grunts when killed.
                e.scoreValue = 5; setHp(e, 4, diff)
                e.vy = ENEMY_SPEED_BASE * 0.65f * speedScale
            }
            EnemyType.ORBITER -> {
                // Spirals down its column while peppering aimed shots — hard to pin down.
                e.scoreValue = 4; setHp(e, 2, diff)
                e.vy = ENEMY_SPEED_BASE * 0.55f * speedScale
                e.swayAmp = minDim * (0.10f + Random.nextFloat() * 0.05f)   // orbit radius
                e.orbitAng = Random.nextFloat() * 6.283f
                e.fireTimer = 0.5f + Random.nextFloat() * 1.2f
            }
            EnemyType.SHIELDER -> {
                // Slow armoured blocker: slides to stay in your firing column and soaks shots
                // on its frontal shield until it shatters, exposing a soft core.
                e.scoreValue = 7; setHp(e, SHIELDER_BODY_HP, diff)
                e.maxShieldHp = scaledHp(SHIELDER_SHIELD_HP, diff)
                e.shieldHp = e.maxShieldHp
                e.vy = ENEMY_SPEED_BASE * 0.42f * speedScale
            }
            EnemyType.DARTER -> {
                // Streaks in from a side edge across the upper screen, firing aimed shots as it goes.
                e.scoreValue = 3; setHp(e, 1, diff)
                val fromLeft = Random.nextBoolean()
                e.y = cy * (0.55f + Random.nextFloat() * 0.5f)
                e.x = if (fromLeft) -r else w + r
                e.baseX = e.x
                e.vx = (if (fromLeft) 1f else -1f) * ENEMY_SPEED_BASE * 2.3f * speedScale
                e.vy = ENEMY_SPEED_BASE * 0.1f * speedScale
                e.fireTimer = 0.35f + Random.nextFloat() * 0.5f
            }
            EnemyType.MINE -> {
                // Drifts down slowly, no gun — but bursts into a (gapped) ring when destroyed or if it slips past.
                e.scoreValue = 4; setHp(e, 5, diff)
                e.vy = ENEMY_SPEED_BASE * 0.4f * speedScale
                e.swayAmp = minDim * 0.03f
                e.wobble = Random.nextFloat() * 6.283f
            }
            EnemyType.DIVER -> {
                // Cruises in, telegraphs, then lunges straight at where the jet was.
                e.scoreValue = 3; setHp(e, 2, diff)
                e.vy = ENEMY_SPEED_BASE * 0.5f * speedScale
                e.fireTimer = 0.6f + Random.nextFloat() * 0.5f
                e.diving = false
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

    /**
     * Progression clock that drives the unlock schedule: time survived, plus a chunk for each boss beaten
     * (so skill advances the reveal too). Kept separate from [difficulty] — that scales HP/speed/density;
     * this controls *which* enemies have entered the game yet.
     */
    private fun unlockProgress(): Float = elapsed + bossesBeaten * UNLOCK_BOSS_BONUS

    /** When (in [unlockProgress] seconds) each enemy type first joins the roster — one at a time, spaced out. */
    private fun enemyUnlockTime(type: EnemyType): Float = when (type) {
        EnemyType.GRUNT -> 0f       // the backbone, from the first second
        EnemyType.WEAVER -> 6f
        EnemyType.RUSHER -> 18f
        EnemyType.GUNNER -> 30f     // first shooter
        EnemyType.DARTER -> 45f     // side striker
        EnemyType.TANK -> 62f
        EnemyType.SHIELDER -> 80f
        EnemyType.SPLITTER -> 100f
        EnemyType.ORBITER -> 122f
        EnemyType.MINE -> 142f
        EnemyType.DIVER -> 162f     // the aggressive lunger arrives last
    }

    /** How common a type is once it has fully ramped in. */
    private fun enemyBaseWeight(type: EnemyType): Float = when (type) {
        EnemyType.GRUNT -> 3.0f
        EnemyType.WEAVER -> 1.6f
        EnemyType.RUSHER -> 1.6f
        EnemyType.GUNNER -> 1.4f
        EnemyType.DARTER -> 1.3f
        EnemyType.TANK -> 1.0f
        EnemyType.SHIELDER -> 1.0f
        EnemyType.SPLITTER -> 1.1f
        EnemyType.ORBITER -> 1.1f
        EnemyType.MINE -> 0.9f
        EnemyType.DIVER -> 1.2f
    }

    private fun enemyDisplayName(type: EnemyType): String = when (type) {
        EnemyType.GRUNT -> "GRUNT"
        EnemyType.WEAVER -> "WEAVER"
        EnemyType.RUSHER -> "RUSHER"
        EnemyType.GUNNER -> "GUNNER"
        EnemyType.TANK -> "TANK"
        EnemyType.SPLITTER -> "SPLITTER"
        EnemyType.ORBITER -> "ORBITER"
        EnemyType.SHIELDER -> "SHIELDER"
        EnemyType.DARTER -> "DARTER"
        EnemyType.MINE -> "MINE"
        EnemyType.DIVER -> "DIVER"
    }

    /**
     * Picks among only the enemy types unlocked so far. A freshly-unlocked type fades in over [UNLOCK_RAMP]
     * seconds (rare at first, then common), so every addition feels introduced rather than dumped in.
     */
    private fun pickEnemyType(diff: Float): EnemyType {
        val progress = unlockProgress()
        val types = EnemyType.entries
        var total = 0f
        // Two-pass weighted pick without allocating: sum, then roll.
        for (t in types) {
            val unlock = enemyUnlockTime(t)
            if (progress < unlock) continue
            val ramp = if (unlock <= 0f) 1f else ((progress - unlock) / UNLOCK_RAMP).coerceIn(0f, 1f)
            total += enemyBaseWeight(t) * ramp
        }
        if (total <= 0f) return EnemyType.GRUNT
        var pick = Random.nextFloat() * total
        var chosen = EnemyType.GRUNT
        for (t in types) {
            val unlock = enemyUnlockTime(t)
            if (progress < unlock) continue
            val ramp = if (unlock <= 0f) 1f else ((progress - unlock) / UNLOCK_RAMP).coerceIn(0f, 1f)
            chosen = t
            pick -= enemyBaseWeight(t) * ramp
            if (pick <= 0f) return t
        }
        return chosen
    }

    private fun updateEnemies(dt: Float, diff: Float) {
        for (e in enemies) {
            if (e.hitFlash > 0f) e.hitFlash -= dt
            e.y += e.vy * dt
            when {
                e.type == EnemyType.ORBITER -> {                    // fast circular descent
                    e.orbitAng += dt * 3.4f
                    e.x = e.baseX + cos(e.orbitAng) * e.swayAmp
                }
                e.type == EnemyType.SHIELDER -> {                   // slide to block the player's column
                    val step = SHIELDER_TRACK_SPEED * dt
                    e.x += (player.x - e.x).coerceIn(-step, step)
                    val band = (safeHalfWidthAt(e.y) - margin - e.radius).coerceAtLeast(0f)
                    e.x = e.x.coerceIn(cx - band, cx + band)
                }
                e.type == EnemyType.DARTER -> {                     // streaks across horizontally
                    e.x += e.vx * dt
                }
                e.type == EnemyType.DIVER -> {                      // cruise, telegraph, then lunge
                    if (!e.diving) {
                        e.fireTimer -= dt
                        if (e.fireTimer < 0.22f) e.hitFlash = 0.06f          // glow as the tell
                        if (e.fireTimer <= 0f) {
                            e.diving = true
                            val ang = atan2(player.y - e.y, player.x - e.x)
                            val sp = ENEMY_SPEED_BASE * 2.6f * (1f + (diff - 1f) * 0.16f)
                            e.vx = cos(ang) * sp
                            e.vy = sin(ang) * sp
                        }
                    } else {
                        e.x += e.vx * dt
                    }
                }
                e.swayAmp > 0f -> {                                 // weaver / mine / swaying formation
                    e.wobble += dt * 2.6f
                    e.x = e.baseX + sin(e.wobble) * e.swayAmp
                }
            }
            // A mine that slips down to the jet's level detonates into a (gapped, dodgeable) ring.
            if (e.type == EnemyType.MINE && e.alive && e.y > player.y - e.radius * 2f) {
                detonateMine(e, diff); continue
            }
            // Shooting enemies fire once on screen.
            val shooter = e.type == EnemyType.GUNNER || e.type == EnemyType.TANK ||
                e.type == EnemyType.ORBITER || e.type == EnemyType.DARTER
            if (shooter && e.y > 0f && e.y < cy * 1.2f) {
                e.fireTimer -= dt
                if (e.fireTimer <= 0f) {
                    when (e.type) {
                        EnemyType.GUNNER -> {
                            e.fireTimer = (GUNNER_FIRE_INTERVAL / (1f + (diff - 1f) * 0.25f)).coerceAtLeast(0.45f)
                            if (diff >= 3f) fanShot(e.x, e.y, e.radius, 3, diff)  // 3-fan once it's tough
                            else aimedShot(e.x, e.y, e.radius, diff)
                        }
                        EnemyType.TANK -> {
                            e.fireTimer = (TANK_FIRE_INTERVAL / (1f + (diff - 1f) * 0.25f)).coerceAtLeast(0.5f)
                            fanShot(e.x, e.y, e.radius, if (diff >= 4f) 5 else 3, diff)
                        }
                        EnemyType.DARTER -> {                        // a quick aimed tap as it streaks past
                            e.fireTimer = 0.55f
                            aimedShot(e.x, e.y, e.radius, diff)
                        }
                        else -> {                                    // ORBITER: quick aimed taps
                            e.fireTimer = (1.0f / (1f + (diff - 1f) * 0.2f)).coerceAtLeast(0.4f)
                            aimedShot(e.x, e.y, e.radius, diff)
                        }
                    }
                }
            }
            if (e.y - e.radius > h) e.alive = false
            // Side-exit cull (darters fly clean off the left/right edges).
            if (e.x + e.radius < -minDim * 0.12f || e.x - e.radius > w + minDim * 0.12f) e.alive = false
        }
    }

    /** A mine that reaches the jet's level bursts without scoring — punishing you for letting it through. */
    private fun detonateMine(e: Enemy, diff: Float) {
        e.alive = false
        explode(e.x, e.y, e.radius * 1.2f)
        radialBurst(e.x, e.y, e.radius, 9 + diff.toInt(), diff)
        vibrate?.invoke(20)
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
    //  Lasers — telegraphed sweeping beams
    // =====================================================================
    private fun updateLasers(dt: Float) {
        val pad = minDim + minDim * LASER_HALFW_FRACTION * 2f
        for (l in lasers) {
            if (l.warnTime > 0f) {                      // telegraph holds still so the player can read it
                l.warnTime -= dt
                if (l.warnTime <= 0f) {                 // the moment the beam goes live
                    shake(0.16f, minDim * 0.022f)
                    vibrate?.invoke(45)
                }
            } else if (l.fireTime > 0f) {               // only the live beam sweeps sideways
                l.x += l.vx * dt
                l.fireTime -= dt
                if (l.fireTime <= 0f) l.alive = false
            }
            if (l.x < -pad || l.x > w + pad) l.alive = false
        }
    }

    /** True only while the beam is actually lethal (telegraph finished, still firing). */
    private fun laserLethal(l: Laser): Boolean = l.warnTime <= 0f && l.fireTime > 0f

    /**
     * A "comb" of vertical beams spanning the dodge band with one safe lane. The whole comb drifts
     * sideways so the lane keeps moving and the player must keep repositioning to stay alive.
     */
    private fun combLasers() {
        if (lasers.isNotEmpty()) return                 // never stack laser set-pieces
        val bandHalf = (safeHalfWidthAt(player.y) - margin).coerceAtLeast(minDim * 0.2f)
        val halfW = minDim * LASER_HALFW_FRACTION
        val gapHalf = player.width * 1.5f               // a lane comfortably wider than the jet
        val gapCenter = cx + (Random.nextFloat() * 2f - 1f) * bandHalf * 0.45f
        val spacing = halfW * 2.3f                       // tight enough that only the lane is safe
        val drift = (if (Random.nextBoolean()) 1f else -1f) * LASER_SWEEP_SPEED
        var x = cx - bandHalf
        while (x <= cx + bandHalf && lasers.size < LASER_MAX) {
            if (abs(x - gapCenter) > gapHalf) {
                lasers.add(Laser(x, halfW).also {
                    it.warnTime = LASER_WARN; it.maxWarn = LASER_WARN
                    it.fireTime = LASER_FIRE; it.maxFire = LASER_FIRE
                    it.vx = drift
                })
            }
            x += spacing
        }
    }

    /** One thick beam that sweeps the whole width — wait for it, then slip behind it. */
    private fun sweepLaser() {
        if (lasers.isNotEmpty()) return
        val bandHalf = (safeHalfWidthAt(player.y) - margin).coerceAtLeast(minDim * 0.2f)
        val fromLeft = Random.nextBoolean()
        val edge = bandHalf + minDim * 0.06f
        val startX = if (fromLeft) cx - edge else cx + edge
        val dir = if (fromLeft) 1f else -1f
        val fire = 2.0f
        val speed = (2f * edge) / fire                   // crosses the band over its lifetime
        lasers.add(Laser(startX, minDim * LASER_HALFW_FRACTION * 1.7f).also {
            it.warnTime = LASER_WARN; it.maxWarn = LASER_WARN
            it.fireTime = fire; it.maxFire = fire
            it.vx = dir * speed
        })
    }

    // =====================================================================
    //  Boss
    // =====================================================================
    private fun spawnBoss() {
        val variant = bossesBeaten % FINAL_BOSS_EVERY          // 0..5
        val isFinal = variant == FINAL_BOSS_EVERY - 1          // every 6th boss = the colossal OVERLORD
        val baseHp = BOSS_BASE_HP + bossesBeaten * BOSS_HP_PER_KILL
        val hp = if (isFinal) (baseHp * FINAL_BOSS_HP_MULT).toInt() else baseHp
        // The OVERLORD is colossal: a body far larger than the screen, so only its lower hull looms in.
        val r = if (isFinal) minDim * COLOSSUS_RADIUS_FRACTION else minDim * BOSS_RADIUS_FRACTION
        boss = Boss(cx, -r, r, hp).also {
            it.variant = variant
            it.isFinal = isFinal
            it.colossal = isFinal
            it.name = bossName(variant)
        }
        enemyBullets.clear()
        stageBanner = 1.4f
        showBanner(if (isFinal) "!! OVERLORD !!" else bossName(variant), 1.8f)
        if (isFinal) { shake(0.4f, minDim * 0.03f); vibrate?.invoke(60) }
    }

    private fun bossName(variant: Int): String = when (variant) {
        0 -> "WARDEN"
        1 -> "REAVER"
        2 -> "SEER"
        3 -> "STORMCALLER"
        4 -> "VOIDMAW"
        else -> "OVERLORD"
    }

    private fun showBanner(text: String, time: Float) { eventBanner = text; eventBannerTime = time }

    private fun updateBoss(b: Boss, dt: Float, diff: Float) {
        if (b.hitFlash > 0f) b.hitFlash -= dt
        if (b.invulnTime > 0f) b.invulnTime -= dt
        b.phase += dt
        b.spin += dt
        if (b.entering) {
            b.y += (if (b.colossal) 150f else 90f) * dt
            // A colossus settles with its centre above the screen, so only the lower hull shows.
            val restY = if (b.colossal) -b.radius * 0.34f else if (b.isFinal) cy * 0.5f else cy * 0.55f
            if (b.y >= restY) { b.y = restY; b.entering = false }
            return
        }

        // Cross the enrage threshold once: tighter timers, denser bursts, a faster sweep — plus a
        // defiant barrier + signature burst so even DPS-heavy weapons can't just melt the boss.
        if (!b.enraged && b.hp <= b.maxHp * ENRAGE_HP_FRACTION) {
            b.enraged = true
            showBanner("ENRAGED", 0.9f)
            shake(0.3f, minDim * 0.025f)
            vibrate?.invoke(40)
            b.invulnTime = 1.6f
            radialBurst(b.x, b.y, b.radius, (18 * intensity()).toInt(), diff)
        }

        if (b.colossal) {
            // Too vast to sweep — it drifts slowly overhead, menacing rather than darting.
            val drift = minDim * (0.07f + if (b.enraged) 0.02f else 0f)
            b.x = cx + sin(b.phase * 0.4f) * drift
        } else {
            // Sweep horizontally within the safe band; speed depends on variant + enrage.
            val baseSpeed = when (b.variant) { 0 -> 70f; 1 -> 110f; 2 -> 95f; 3 -> 120f; else -> 105f }
            val speed = baseSpeed * if (b.enraged) 1.35f else 1f
            val half = safeHalfWidthAt(b.y) - margin - b.radius
            b.x += b.moveDir * speed * dt
            if (b.x > cx + half) { b.x = cx + half; b.moveDir = -1 }
            if (b.x < cx - half) { b.x = cx - half; b.moveDir = 1 }
        }

        b.fireTimer -= dt
        if (b.fireTimer <= 0f) {
            if (b.isFinal) fireFinalPattern(b, diff) else fireBossPattern(b, diff)
        }
    }

    /** The four rotating bosses, each with its own four-beat pattern loop. */
    private fun fireBossPattern(b: Boss, diff: Float) {
        val inten = intensity()
        val ef = if (b.enraged) 0.62f else 1f
        when (b.variant) {
            0 -> when ((b.phase / 3.2f).toInt() % 4) {                      // WARDEN — purple
                0 -> { b.fireTimer = 0.85f * ef; fanShot(b.x, b.y, b.radius, (5 * inten).toInt(), diff) }
                1 -> { b.fireTimer = 1.2f * ef; radialBurst(b.x, b.y, b.radius, (12 * inten).toInt(), diff) }
                2 -> { b.fireTimer = 1.15f * ef; gapWall(b.y, diff) }
                else -> { b.fireTimer = 0.12f * ef; spiral(b, diff) }
            }
            1 -> when ((b.phase / 3.0f).toInt() % 4) {                      // REAVER — crimson
                0 -> { b.fireTimer = 0.5f * ef; aimedSpread(b.x, b.y, b.radius, 3 + (inten - 1f).toInt(), 0.18f, diff) }
                1 -> { b.fireTimer = 1.0f * ef; gapWall(b.y, diff) }
                2 -> { b.fireTimer = 1.3f * ef; radialBurst(b.x, b.y, b.radius, (16 * inten).toInt(), diff) }
                else -> { b.fireTimer = 0.1f * ef; streamDown(b, diff) }
            }
            2 -> when ((b.phase / 3.0f).toInt() % 4) {                      // SEER — green, weaving curtains
                0 -> { b.fireTimer = 0.7f * ef; aimedSpread(b.x, b.y, b.radius, (5 * inten).toInt(), 0.16f, diff) }
                1 -> { b.fireTimer = 0.9f * ef; waveColumn(b, diff) }
                2 -> { b.fireTimer = 1.2f * ef; petalBurst(b, (10 * inten).toInt(), diff) }
                else -> { b.fireTimer = 0.1f * ef; spokeSpin(b, 4, diff) }
            }
            3 -> when ((b.phase / 3.0f).toInt() % 4) {                      // STORMCALLER — blue, laser specialist
                0 -> { b.fireTimer = 0.6f * ef; crossShot(b.x, b.y, diff, b.spin) }
                1 -> bossLaser(b, comb = true)                              // sweeping comb of beams
                2 -> { b.fireTimer = 0.1f * ef; spokeSpin(b, 6, diff) }
                else -> { b.fireTimer = 1.2f * ef; radialBurst(b.x, b.y, b.radius, (20 * inten).toInt(), diff) }
            }
            else -> when ((b.phase / 3.0f).toInt() % 4) {                   // VOIDMAW — void-purple, gravity wells
                0 -> { b.fireTimer = 0.1f * ef; spiral(b, diff) }           // relentless twin spiral
                1 -> { b.fireTimer = 1.25f * ef; radialBurst(b.x, b.y, b.radius, (22 * inten).toInt(), diff) }
                2 -> { b.fireTimer = 0.6f * ef; aimedSpread(b.x, b.y, b.radius, (5 * inten).toInt(), 0.14f, diff) }
                else -> { b.fireTimer = 0.1f * ef; spokeSpin(b, 7, diff) }  // dense seven-arm pinwheel
            }
        }
    }

    /** OVERLORD — the every-5th mega-boss. Three HP-gated phases that pile patterns on top. */
    private fun fireFinalPattern(b: Boss, diff: Float) {
        val inten = intensity()
        val frac = b.hp.toFloat() / b.maxHp
        val ef = if (b.enraged) 0.6f else 1f
        when {
            frac > 0.66f -> when ((b.phase / 2.6f).toInt() % 3) {           // Phase 1 — warm-up
                0 -> { b.fireTimer = 0.8f * ef; fanShot(b.x, b.y, b.radius, (7 * inten).toInt(), diff) }
                1 -> { b.fireTimer = 1.1f * ef; radialBurst(b.x, b.y, b.radius, (16 * inten).toInt(), diff) }
                else -> { b.fireTimer = 0.7f * ef; aimedSpread(b.x, b.y, b.radius, (5 * inten).toInt(), 0.16f, diff) }
            }
            frac > 0.33f -> when ((b.phase / 2.6f).toInt() % 5) {           // Phase 2 — spins, walls & a sweeping beam
                0 -> { b.fireTimer = 0.1f * ef; spokeSpin(b, 6, diff) }
                1 -> { b.fireTimer = 1.0f * ef; gapWall(b.y, diff) }
                2 -> { b.fireTimer = 0.6f * ef; crossShot(b.x, b.y, diff, b.spin) }
                3 -> { b.fireTimer = 1.1f * ef; petalBurst(b, (12 * inten).toInt(), diff) }
                else -> bossLaser(b, comb = false)                         // single sweeping beam
            }
            else -> when ((b.phase / 2.2f).toInt() % 5) {                   // Phase 3 — everything, fast, then the comb
                0 -> { b.fireTimer = 0.1f; spokeSpin(b, 8, diff) }
                1 -> { b.fireTimer = 0.85f; gapWall(b.y, diff); radialBurst(b.x, b.y, b.radius, (14 * inten).toInt(), diff) }
                2 -> { b.fireTimer = 0.1f; spiral(b, diff) }
                3 -> { b.fireTimer = 0.9f; petalBurst(b, (16 * inten).toInt(), diff); aimedSpread(b.x, b.y, b.radius, 5, 0.14f, diff) }
                else -> bossLaser(b, comb = true)                          // the moving-lane comb climax
            }
        }
    }

    /**
     * The boss commits to a laser set-piece. It raises an invulnerable barrier for the whole
     * attack — so the fight stops being a damage race and becomes a pure dodge for those seconds.
     */
    private fun bossLaser(b: Boss, comb: Boolean) {
        if (lasers.isNotEmpty()) { b.fireTimer = 0.2f; return }   // already mid-beam, retry shortly
        if (comb) combLasers() else sweepLaser()
        val dur = if (comb) LASER_WARN + LASER_FIRE else LASER_WARN + 2.0f
        b.invulnTime = dur + 0.25f
        b.fireTimer = dur + 0.3f          // hold all other fire until the beam is spent
        vibrate?.invoke(30)
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

    /** Angular distance between two headings, wrapped to [0, π]. */
    private fun angGap(a: Float, center: Float): Float {
        var d = (a - center) % 6.2831855f
        if (d < -3.1415927f) d += 6.2831855f
        if (d > 3.1415927f) d -= 6.2831855f
        return abs(d)
    }

    /**
     * A ring of bullets — but with a guaranteed downward escape lane carved out, so a jet that can only
     * slide left/right can always slip under the boss. The lane is centred roughly straight down (π/2)
     * with a slight random lean, and widens a touch for denser rings.
     */
    private fun radialBurst(x: Float, y: Float, r: Float, count: Int, diff: Float) {
        if (count <= 0) return
        val sp = EBULLET_SPEED * 0.88f * (1f + (diff - 1f) * 0.1f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        val base = (animTime * 0.7f)
        val gapCenter = 1.5708f + (Random.nextFloat() * 2f - 1f) * 0.35f
        val gapHalf = RING_GAP_HALF + count * 0.006f
        for (i in 0 until count) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val a = base + i.toFloat() / count * 6.283f
            if (angGap(a, gapCenter) < gapHalf) continue          // leave the escape lane open
            enemyBullets.add(EnemyBullet(x, y, cos(a) * sp, sin(a) * sp + 30f, br))
        }
        spawnShockwave(x, y, r, 0xFFFF6BD0.toInt())
        if (count >= 16) shake(0.12f, minDim * 0.012f)
    }

    /** A tight shotgun cone aimed straight at the player — the spread is the pressure. */
    private fun aimedSpread(x: Float, y: Float, r: Float, count: Int, spread: Float, diff: Float) {
        if (count <= 0) return
        val sp = EBULLET_SPEED * (1f + (diff - 1f) * 0.12f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        val center = atan2(player.y - y, player.x - x)
        for (i in 0 until count) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val a = center + (i - (count - 1) / 2f) * spread
            enemyBullets.add(EnemyBullet(x, y + r, cos(a) * sp, sin(a) * sp, br))
        }
    }

    /** A radial spray whose per-spoke speed pulses, so the bullets bloom into flower petals. */
    private fun petalBurst(b: Boss, count: Int, diff: Float) {
        if (count <= 0) return
        val baseSp = EBULLET_SPEED * 0.85f * (1f + (diff - 1f) * 0.1f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        val rot = b.spin * 0.8f
        val gapCenter = 1.5708f + (Random.nextFloat() * 2f - 1f) * 0.3f
        val gapHalf = RING_GAP_HALF + count * 0.005f
        for (i in 0 until count) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val a = rot + i.toFloat() / count * 6.283f
            if (angGap(a, gapCenter) < gapHalf) continue          // keep the escape lane clear
            val sp = baseSp * (0.55f + 0.45f * abs(sin(a * 5f)))   // five speed lobes → five petals
            enemyBullets.add(EnemyBullet(b.x, b.y, cos(a) * sp, sin(a) * sp + 20f, br))
        }
        spawnShockwave(b.x, b.y, b.radius, 0xFF6BE0FF.toInt())
    }

    /** Four fast beams in a plus that slowly rotates into an X and back — sweeping lasers of lead. */
    private fun crossShot(x: Float, y: Float, diff: Float, rotate: Float) {
        val sp = EBULLET_SPEED * 1.2f * (1f + (diff - 1f) * 0.1f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        val arms = 4
        for (i in 0 until arms) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val a = rotate * 0.6f + i.toFloat() / arms * 6.283f
            enemyBullets.add(EnemyBullet(x, y, cos(a) * sp, sin(a) * sp, br))
        }
    }

    /** A rapid-fire rotating pinwheel: fired on a tiny cadence so the arms trace spiral curves. */
    private fun spokeSpin(b: Boss, arms: Int, diff: Float) {
        val sp = EBULLET_SPEED * 0.9f * (1f + (diff - 1f) * 0.1f)
        val br = minDim * EBULLET_RADIUS_FRACTION
        val base = b.spin * 3.2f
        for (k in 0 until arms) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val a = base + k.toFloat() / arms * 6.283f
            enemyBullets.add(EnemyBullet(b.x, b.y, cos(a) * sp, sin(a) * sp, br))
        }
    }

    /** A full-width falling curtain where each column is offset sideways into a snaking wave. */
    private fun waveColumn(b: Boss, diff: Float) {
        val br = minDim * EBULLET_RADIUS_FRACTION
        val sp = EBULLET_SPEED * 0.8f * (1f + (diff - 1f) * 0.1f)
        val n = 9
        val spanHalf = (safeHalfWidthAt(b.y) - margin).coerceAtLeast(minDim * 0.2f)
        for (i in 0 until n) {
            if (enemyBullets.size >= EBULLET_MAX) break
            val t = i.toFloat() / (n - 1)
            val x = cx - spanHalf + 2f * spanHalf * t
            val vx = sin(b.phase * 3f + t * 6.283f) * sp * 0.5f
            enemyBullets.add(EnemyBullet(x, b.y, vx, sp, br))
        }
    }

    private fun bossDefeated(b: Boss) {
        b.alive = false
        val mult = if (b.isFinal) 4 else 1
        val bonus = 2000 * (1 + bossesBeaten) * mult
        addScore(bonus)
        spawnPopup(b.x, b.y, "${b.name} DOWN +$bonus", 0xFFFFF36B.toInt())
        bossesBeaten++
        bossTimer = 0f
        stage++; stageBanner = 1.6f
        enemyBullets.clear()
        lasers.clear()
        slowmoTime = if (b.isFinal) 0.7f else 0.45f      // cinematic beat on the kill

        // A colossus's body sits off the top edge — anchor the death FX and loot where the player can see them.
        val fxR = min(b.radius, minDim * 0.3f)
        val dy = if (b.colossal) minDim * 0.22f else b.y
        if (b.isFinal) {
            // A proper clear: screen-filling burst, long rumble, a victory banner, fat rewards.
            showBanner("WAVE CLEAR!", 2.4f)
            shake(0.9f, minDim * 0.07f)
            bombFlash = 0.4f
            vibrate?.invoke(220)
            repeat(16) { explode(b.x + (Random.nextFloat() - 0.5f) * fxR * 2f,
                dy + (Random.nextFloat() - 0.5f) * fxR * 2f, fxR * 0.7f) }
            dropPowerUp(b.x - fxR * 0.7f, dy, PowerType.POWER)
            dropPowerUp(b.x - fxR * 0.25f, dy, PowerType.WEAPON)
            dropPowerUp(b.x + fxR * 0.25f, dy, PowerType.BOMB)
            dropPowerUp(b.x + fxR * 0.7f, dy, PowerType.SHIELD)
            dropPowerUp(b.x - fxR * 0.45f, dy - fxR * 0.5f, PowerType.DRONE)   // OVERLORD always grants an escort
            dropPowerUp(b.x + fxR * 0.45f, dy - fxR * 0.5f, PowerType.LIFE)
        } else {
            shake(0.6f, minDim * 0.05f)
            bombFlash = 0.25f
            vibrate?.invoke(120)
            repeat(6) { explode(b.x + (Random.nextFloat() - 0.5f) * fxR,
                dy + (Random.nextFloat() - 0.5f) * fxR, fxR * 0.6f) }
            dropPowerUp(b.x - fxR * 0.5f, dy, PowerType.POWER)
            dropPowerUp(b.x, dy, PowerType.WEAPON)
            dropPowerUp(b.x + fxR * 0.5f, dy, PowerType.BOMB)
            if (bossesBeaten % 2 == 0) dropPowerUp(b.x, dy - fxR * 0.5f, PowerType.DRONE)  // an escort now and then
        }
    }

    // =====================================================================
    //  Power-ups
    // =====================================================================
    private fun maybeDrop(x: Float, y: Float) {
        if (Random.nextFloat() > POWERUP_DROP_CHANCE) return
        val roll = Random.nextFloat()
        val type = when {
            roll < 0.40f -> PowerType.POWER
            roll < 0.60f -> PowerType.SHIELD
            roll < 0.76f -> PowerType.BOMB
            roll < 0.90f -> PowerType.WEAPON
            roll < 0.97f -> PowerType.DRONE  // an escort drone
            else -> PowerType.LIFE           // rare 1UP
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
            PowerType.DRONE -> gainDrone(p.x, p.y)
            PowerType.LIFE -> gainLife(p.x, p.y)
        }
        vibrate?.invoke(25)
        explode(p.x, p.y, p.radius)
    }

    private fun cycleWeapon(x: Float, y: Float) {
        weapon = when (weapon) {
            WeaponType.VULCAN -> WeaponType.LASER
            WeaponType.LASER -> WeaponType.WIDE
            WeaponType.WIDE -> WeaponType.HOMING
            WeaponType.HOMING -> WeaponType.HELIX
            WeaponType.HELIX -> WeaponType.VULCAN
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
        WeaponType.HOMING -> "HOMING"
        WeaponType.HELIX -> "HELIX"
    }

    private fun weaponColor(wt: WeaponType): Int = when (wt) {
        WeaponType.VULCAN -> 0xFF8CF6FF.toInt()
        WeaponType.LASER -> 0xFFFF6BE3.toInt()
        WeaponType.WIDE -> 0xFFFFB23D.toInt()
        WeaponType.HOMING -> 0xFF8AFF6B.toInt()
        WeaponType.HELIX -> 0xFFB9A6FF.toInt()
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
        lasers.clear()                       // a bomb dispels the beams too
        // Heavy damage to everything on screen.
        for (e in enemies) {
            if (!e.alive) continue
            var dmg = 5
            if (e.shieldHp > 0) {            // crack the shield first, spill the rest onto the core
                val absorbed = min(e.shieldHp, dmg); e.shieldHp -= absorbed; dmg -= absorbed
            }
            if (dmg > 0 && e.shieldHp <= 0) e.hp -= dmg
            if (e.hp <= 0) killEnemy(e) else e.hitFlash = 0.1f
        }
        boss?.let { b ->
            if (b.invulnTime <= 0f) {        // the barrier shrugs off bombs too
                b.hp -= (b.maxHp * 0.12f).toInt().coerceAtLeast(8)
                b.hitFlash = 0.12f
                if (b.hp <= 0) bossDefeated(b)
            }
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
                    if (e.shieldHp > 0) {                       // chip the shield; the core stays safe
                        e.shieldHp -= bl.damage
                        if (e.shieldHp <= 0) {                  // shield shatters — core now exposed
                            e.shieldHp = 0
                            e.hitFlash = 0.14f
                            explode(e.x, e.y + e.radius * 0.5f, e.radius * 0.7f)
                            vibrate?.invoke(18)
                        } else e.hitFlash = 0.05f
                    } else {
                        e.hp -= bl.damage
                        if (e.hp <= 0) killEnemy(e) else e.hitFlash = 0.08f
                    }
                    if (bl.pierce > 0) bl.pierce-- else { bl.alive = false; break }
                }
            }
        }
        // Player bullets vs boss.
        boss?.let { b ->
            if (b.alive && !b.entering) {
                val shielded = b.invulnTime > 0f
                for (bl in bullets) {
                    if (!bl.alive) continue
                    val dx = bl.x - b.x; val dy = bl.y - b.y
                    if (dx * dx + dy * dy <= b.radius * b.radius) {
                        bl.alive = false
                        if (shielded) continue              // barrier eats the shot, no damage
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
                addOverdrive(OVERDRIVE_PER_GRAZE)
                if (comboTimer in 0.001f..0.45f) comboTimer = 0.45f   // keep a fading chain warm
                if (grazeCount % 10 == 0) {
                    spawnPopup(player.x, player.y - player.height * 1.1f, "GRAZE x$grazeCount", 0xFF66E0FF.toInt())
                    vibrate?.invoke(12)
                }
            }
        }
        // Lasers vs player — only the lethal (post-telegraph) beam has a hitbox.
        for (l in lasers) {
            if (!laserLethal(l)) continue
            if (abs(player.x - l.x) <= l.halfW + playerR) {
                playerHit()
                return
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
        addOverdrive(OVERDRIVE_PER_KILL)
        val gain = SCORE_PER_KILL * e.scoreValue * multiplier
        addScore(gain)
        if (e.scoreValue >= 4) spawnPopup(e.x, e.y, "+$gain", 0xFFFFFFFF.toInt())  // gunners/tanks
        explode(e.x, e.y, e.radius)
        if (e.type == EnemyType.SPLITTER) spawnSplitterChildren(e)
        if (e.type == EnemyType.MINE) radialBurst(e.x, e.y, e.radius, 9, difficulty())   // shrapnel ring
        maybeDrop(e.x, e.y)
    }

    /** A killed splitter bursts into a fan of fast little rushers (queued, never re-splits). */
    private fun spawnSplitterChildren(parent: Enemy) {
        val diff = difficulty()
        val n = if (diff >= 5f) 4 else 3
        val cr = parent.radius * 0.6f
        val spread = parent.radius * 1.1f
        for (i in 0 until n) {
            if (enemies.size + spawnQueue.size >= ENEMY_MAX) break
            val t = if (n == 1) 0f else (i.toFloat() / (n - 1)) * 2f - 1f   // -1..1
            val x = parent.x + t * spread
            val child = Enemy(x, parent.y, cr, EnemyType.RUSHER).apply {
                baseX = x; scoreValue = 1; maxHp = 1; hp = 1
                vy = ENEMY_SPEED_BASE * (1.7f + 0.1f * abs(t)) * (1f + (diff - 1f) * 0.16f)
                swayAmp = parent.radius * 0.4f; wobble = t * 1.5f
            }
            spawnQueue.add(child)
        }
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
        if (droneCount > 0) {                     // lose one escort, keep the rest of your investment
            droneCount--
            if (drones.isNotEmpty()) drones.removeAt(drones.size - 1)
        }
        overdrive = 0f; overdriveTime = 0f        // the surge is spent on a hit
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

    private fun spawnShockwave(x: Float, y: Float, r: Float, color: Int) {
        if (shockwaves.size >= 12) return
        shockwaves.add(Shockwave(x, y, (r * 2.6f).coerceAtLeast(minDim * 0.4f), color).also {
            it.maxLife = 0.5f; it.life = 0.5f
        })
    }

    private fun updateShockwaves(dt: Float) {
        var i = 0
        while (i < shockwaves.size) {
            val s = shockwaves[i]
            s.life -= dt
            if (s.life <= 0f) shockwaves.removeAt(i) else i++
        }
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

        drawBackdrop(canvas)

        val shaking = shakeTime > 0f
        if (shaking) {
            val k = (shakeTime).coerceIn(0f, 1f) * shakeMag
            canvas.save()
            canvas.translate((Random.nextFloat() - 0.5f) * 2f * k, (Random.nextFloat() - 0.5f) * 2f * k)
        }

        drawStars(canvas)

        if (state == State.PLAYING || state == State.PAUSED || state == State.GAME_OVER) {
            boss?.let { drawBoss(canvas, it) }
            drawShockwaves(canvas)
            drawPowerUps(canvas)
            drawEnemies(canvas)
            drawEnemyBullets(canvas)
            drawLasers(canvas)
            drawBullets(canvas)
            drawDrones(canvas)
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
            State.PLAYING -> { drawHud(canvas); drawEventBanner(canvas) }
            State.PAUSED -> { drawHud(canvas); drawPausedOverlay(canvas) }
            State.READY -> drawTitle(canvas)
            State.GAME_OVER -> drawGameOver(canvas)
        }
    }

    /** Big transient call-out: boss name reveal, ENRAGED, WAVE CLEAR — drawn below centre. */
    private fun drawEventBanner(canvas: Canvas) {
        if (eventBannerTime <= 0f) return
        val a = (eventBannerTime / 0.6f).coerceIn(0f, 1f)          // fade over the last 0.6s
        val (rr, gg, bb) = when {
            eventBanner == "ENRAGED" -> Triple(255, 60, 48)
            eventBanner == "WAVE CLEAR!" || eventBanner.contains("OVERLORD") -> Triple(255, 194, 75)
            else -> Triple(111, 227, 255)
        }
        textPaint.color = Color.argb((a * 255).toInt(), rr, gg, bb)
        textPaint.textSize = minDim * 0.085f
        canvas.drawText(eventBanner, cx, cy + minDim * 0.13f, textPaint)
    }

    private fun drawStars(canvas: Canvas) {
        for (s in stars) {
            shapePaint.color = Color.argb(s.bright, 255, 255, 255)
            canvas.drawRect(s.x, s.y, s.x + s.size, s.y + s.size, shapePaint)
        }
    }

    // =====================================================================
    //  Backdrops — themed scenery that crossfades as you progress
    // =====================================================================
    private fun drawBackdrop(canvas: Canvas) {
        if (themeFade < 1f && prevTheme != theme) drawTheme(canvas, prevTheme, 1f - themeFade)
        drawTheme(canvas, theme, themeFade)
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
        nebulaShader?.let {
            shapePaint.shader = it
            shapePaint.alpha = (a * 150).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, shapePaint)
            shapePaint.shader = null
            shapePaint.alpha = 255
        }
        val drift = sin(animTime * 0.2f) * minDim * 0.05f
        shapePaint.color = Color.argb((a * 42).toInt(), 180, 110, 230)
        canvas.drawCircle(cx - minDim * 0.2f + drift, cy - minDim * 0.15f, minDim * 0.34f, shapePaint)
        shapePaint.color = Color.argb((a * 36).toInt(), 90, 140, 230)
        canvas.drawCircle(cx + minDim * 0.22f - drift, cy + minDim * 0.12f, minDim * 0.30f, shapePaint)
    }

    private fun drawStation(canvas: Canvas, a: Float) {
        // Side hull framing the lane.
        shapePaint.color = Color.argb((a * 230).toInt(), 14, 20, 32)
        canvas.drawRect(0f, 0f, minDim * 0.1f, h, shapePaint)
        canvas.drawRect(w - minDim * 0.1f, 0f, w, h, shapePaint)
        // Scrolling trusses with rows of lit windows, sliding downward like you're passing them.
        val spacing = minDim * 0.55f
        val scroll = (animTime * 38f) % spacing
        var y = -spacing + scroll
        while (y < h) {
            shapePaint.color = Color.argb((a * 200).toInt(), 24, 32, 48)
            canvas.drawRect(0f, y, w, y + minDim * 0.05f, shapePaint)
            shapePaint.color = Color.argb((a * 150).toInt(), 110, 190, 255)
            var x = minDim * 0.05f
            while (x < w) {
                canvas.drawRect(x, y + minDim * 0.014f, x + minDim * 0.018f, y + minDim * 0.03f, shapePaint)
                x += minDim * 0.12f
            }
            y += spacing
        }
    }

    private fun drawStarSurface(canvas: Canvas, a: Float) {
        starSurfaceShader?.let {
            shapePaint.shader = it
            shapePaint.alpha = (a * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, shapePaint)
            shapePaint.shader = null
            shapePaint.alpha = 255
        }
        // Flares licking up from the star at the bottom edge.
        val n = 5
        for (i in 0 until n) {
            val fx = w * (i + 0.5f) / n
            val flick = 0.5f + 0.5f * sin(animTime * (3f + i) + i)
            val fh = minDim * (0.08f + 0.13f * flick)
            shapePaint.color = Color.argb((a * 120).toInt(), 255, 185, 70)
            canvas.drawRect(fx - minDim * 0.018f, h - fh, fx + minDim * 0.018f, h, shapePaint)
        }
    }

    private fun drawPlanet(canvas: Canvas, a: Float) {
        val pr = minDim * 1.1f
        shapePaint.color = Color.argb((a * 255).toInt(), 18, 54, 92)
        canvas.drawCircle(cx, h + pr * 0.62f, pr, shapePaint)        // planet rising from the bottom
        planetShader?.let {
            shapePaint.shader = it
            shapePaint.alpha = (a * 200).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, h - minDim * 0.18f, w, h, shapePaint)  // lit atmospheric limb
            shapePaint.shader = null
            shapePaint.alpha = 255
        }
        shapePaint.color = Color.argb((a * 180).toInt(), 200, 210, 230)
        canvas.drawCircle(cx + minDim * 0.28f, cy - minDim * 0.26f, minDim * 0.05f, shapePaint)  // a small moon
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

    private fun drawDrones(canvas: Canvas) {
        if (drones.isEmpty()) return
        val dr = minDim * DRONE_RADIUS_FRACTION
        val flame = 0.5f + 0.5f * sin(animTime * 26f)
        val surging = overdriveTime > 0f
        for (d in drones) {
            shapePaint.color = 0xFFFFC24B.toInt()            // thruster flicker
            canvas.drawRect(d.x - dr * 0.16f, d.y + dr * 0.55f, d.x + dr * 0.16f, d.y + dr * (0.55f + 0.5f * flame), shapePaint)
            shapePaint.color = if (surging) 0xFFB9F6FF.toInt() else 0xFF2FA9D6.toInt()
            canvas.drawRect(d.x - dr * 0.7f, d.y - dr * 0.05f, d.x + dr * 0.7f, d.y + dr * 0.45f, shapePaint)  // wings
            shapePaint.color = 0xFF6FE3FF.toInt()
            canvas.drawRect(d.x - dr * 0.26f, d.y - dr * 0.7f, d.x + dr * 0.26f, d.y + dr * 0.5f, shapePaint)  // fuselage
            shapePaint.color = 0xFFFFFFFF.toInt()
            canvas.drawRect(d.x - dr * 0.1f, d.y - dr * 0.42f, d.x + dr * 0.1f, d.y - dr * 0.12f, shapePaint)  // cockpit
        }
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
                3 -> { // HOMING: green diamond with a fading tail along its heading
                    val s = bw * 1.25f
                    val len = hypot(bl.vx, bl.vy).coerceAtLeast(1f)
                    val tx = bl.x - bl.vx / len * s * 2.4f
                    val ty = bl.y - bl.vy / len * s * 2.4f
                    shapePaint.color = 0x668AFF6B.toInt()
                    canvas.drawRect(tx - s * 0.4f, ty - s * 0.4f, tx + s * 0.4f, ty + s * 0.4f, shapePaint)
                    shapePaint.color = 0xFF8AFF6B.toInt()
                    canvas.drawRect(bl.x - s, bl.y - s, bl.x + s, bl.y + s, shapePaint)
                    shapePaint.color = 0xFFFFFFFF.toInt()
                    canvas.drawRect(bl.x - s * 0.4f, bl.y - s * 0.4f, bl.x + s * 0.4f, bl.y + s * 0.4f, shapePaint)
                }
                4 -> { // HELIX: violet weaving bolt with a white core
                    val lw = bw * 0.85f; val lh = bh * 0.9f
                    shapePaint.color = 0xFFB9A6FF.toInt()
                    canvas.drawRect(bl.x - lw, bl.y - lh, bl.x + lw, bl.y + lh, shapePaint)
                    shapePaint.color = 0xFFFFFFFF.toInt()
                    canvas.drawRect(bl.x - lw * 0.4f, bl.y - lh * 0.6f, bl.x + lw * 0.4f, bl.y + lh * 0.6f, shapePaint)
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

    private fun drawLasers(canvas: Canvas) {
        for (l in lasers) {
            if (l.warnTime > 0f) {
                // Telegraph: a thin pulsing line at the exact column the beam will occupy. No hitbox.
                val pulse = 0.35f + 0.65f * abs(sin(animTime * 22f))
                val tw = (l.halfW * 0.14f).coerceAtLeast(1.5f)
                shapePaint.color = Color.argb((pulse * 200).toInt().coerceIn(40, 220), 255, 70, 70)
                canvas.drawRect(l.x - tw, 0f, l.x + tw, h, shapePaint)
                // brighter as it's about to fire, so the "it's coming" moment reads
                if (l.warnTime < 0.28f) {
                    shapePaint.color = Color.argb((pulse * 255).toInt(), 255, 210, 120)
                    canvas.drawRect(l.x - tw * 0.5f, 0f, l.x + tw * 0.5f, h, shapePaint)
                }
            } else if (l.fireTime > 0f) {
                // Lethal beam: glow → bright body → white-hot core. Fades in fast, out near the end.
                val fadeIn = ((l.maxFire - l.fireTime) / 0.08f).coerceIn(0f, 1f)
                val fadeOut = (l.fireTime / 0.18f).coerceIn(0f, 1f)
                val a = (min(fadeIn, fadeOut) * 255).toInt()
                val hw = l.halfW
                shapePaint.color = Color.argb((a * 0.35f).toInt(), 255, 90, 200)
                canvas.drawRect(l.x - hw * 1.7f, 0f, l.x + hw * 1.7f, h, shapePaint)
                shapePaint.color = Color.argb(a, 255, 60, 150)
                canvas.drawRect(l.x - hw, 0f, l.x + hw, h, shapePaint)
                shapePaint.color = Color.argb(a, 255, 255, 255)
                canvas.drawRect(l.x - hw * 0.4f, 0f, l.x + hw * 0.4f, h, shapePaint)
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
            // Type tell-tales so the new enemies read at a glance.
            if (!flash) when (e.type) {
                EnemyType.ORBITER -> {
                    shapePaint.style = Paint.Style.STROKE
                    shapePaint.strokeWidth = r * 0.12f
                    canvas.drawCircle(ex, ey, r * 1.05f, shapePaint)
                    shapePaint.style = Paint.Style.FILL
                }
                EnemyType.SPLITTER -> {
                    shapePaint.color = 0xFF120712.toInt()                      // seam: it'll break apart
                    canvas.drawRect(ex - r * 0.06f, ey - r, ex + r * 0.06f, ey + r, shapePaint)
                }
                EnemyType.MINE -> {                                            // four spikes + a blinking core
                    shapePaint.color = 0xFFFF5030.toInt()
                    val sp = r * 0.5f
                    canvas.drawRect(ex - r * 0.09f, ey - r - sp, ex + r * 0.09f, ey - r, shapePaint)
                    canvas.drawRect(ex - r * 0.09f, ey + r, ex + r * 0.09f, ey + r + sp, shapePaint)
                    canvas.drawRect(ex - r - sp, ey - r * 0.09f, ex - r, ey + r * 0.09f, shapePaint)
                    canvas.drawRect(ex + r, ey - r * 0.09f, ex + r + sp, ey + r * 0.09f, shapePaint)
                    shapePaint.color = if (((animTime * 6f).toInt() and 1) == 0) 0xFFFF3020.toInt() else 0xFF601810.toInt()
                    canvas.drawRect(ex - r * 0.22f, ey - r * 0.22f, ex + r * 0.22f, ey + r * 0.22f, shapePaint)
                }
                EnemyType.DIVER -> {                                           // a forward (downward) point
                    shapePaint.color = 0xFFFFD2D2.toInt()
                    canvas.drawRect(ex - r * 0.18f, ey + r * 0.2f, ex + r * 0.18f, ey + r * 0.95f, shapePaint)
                }
                EnemyType.SHIELDER -> {
                    if (e.shieldHp > 0 && e.maxShieldHp > 0) {                 // bright frontal shield, facing you
                        val sFrac = e.shieldHp.toFloat() / e.maxShieldHp
                        val pulse = 0.7f + 0.3f * sin(animTime * 6f)
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
            EnemyType.SPLITTER -> Pair(0xFFE8E84A.toInt(), 0xFFFF9E3D.toInt())   // toxic yellow
            EnemyType.ORBITER -> Pair(0xFF35E0D0.toInt(), 0xFFB3FFF4.toInt())    // teal drone
            EnemyType.SHIELDER -> Pair(0xFF8794A8.toInt(), 0xFFC8D6E8.toInt())   // steel fortress
            EnemyType.DARTER -> Pair(0xFFFF5AD0.toInt(), 0xFFFFD0F5.toInt())     // magenta streaker
            EnemyType.MINE -> Pair(0xFF6E7A3A.toInt(), 0xFFFF5030.toInt())       // olive shell, red core
            EnemyType.DIVER -> Pair(0xFFE0364E.toInt(), 0xFFFFD2D2.toInt())      // crimson dart
        }
    }

    private fun drawBoss(canvas: Canvas, b: Boss) {
        val r = b.radius
        val (body, eye, foot) = bossPalette(b.variant)
        if (b.colossal) { drawColossus(canvas, b, body, eye, foot); return }

        // Enrage aura: a pulsing red ring once the boss drops below the threshold.
        if (b.enraged) {
            val pulse = 0.5f + 0.5f * sin(animTime * 14f)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = minDim * 0.012f
            shapePaint.color = Color.argb((120 + pulse * 120).toInt().coerceIn(0, 255), 255, 60, 48)
            canvas.drawCircle(b.x, b.y, r * (1.12f + 0.06f * pulse), shapePaint)
            shapePaint.style = Paint.Style.FILL
        }

        // Barrier: a bright cyan shield while the boss commits to a laser (no damage gets through).
        if (b.invulnTime > 0f) {
            val pulse = 0.5f + 0.5f * sin(animTime * 16f)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = minDim * (0.016f + 0.008f * pulse)
            shapePaint.color = Color.argb((150 + pulse * 100).toInt().coerceIn(0, 255), 120, 230, 255)
            canvas.drawCircle(b.x, b.y, r * (1.22f + 0.05f * pulse), shapePaint)
            shapePaint.style = Paint.Style.FILL
        }

        shapePaint.color = if (b.hitFlash > 0f) 0xFFFFFFFF.toInt() else body
        canvas.drawRect(b.x - r, b.y - r * 0.65f, b.x + r, b.y + r * 0.65f, shapePaint)
        canvas.drawRect(b.x - r * 0.65f, b.y - r, b.x + r * 0.65f, b.y + r, shapePaint)

        // The OVERLORD wears a jagged crown of horns so it reads as the big one.
        if (b.isFinal) {
            shapePaint.color = if (b.hitFlash > 0f) 0xFFFFFFFF.toInt() else foot
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

    /** HP bar across the top, inside the safe area (gold for the OVERLORD). */
    private fun drawBossHpBar(canvas: Canvas, b: Boss) {
        val barW = minDim * 0.6f
        val barH = minDim * 0.03f
        val top = cy - radius + minDim * 0.06f
        shapePaint.color = 0xFF3A0A12.toInt()
        canvas.drawRect(cx - barW / 2f, top, cx + barW / 2f, top + barH, shapePaint)
        shapePaint.color = if (b.isFinal) 0xFFFFC24B.toInt() else 0xFFFF3B30.toInt()
        val frac = (b.hp.toFloat() / b.maxHp).coerceIn(0f, 1f)
        canvas.drawRect(cx - barW / 2f, top, cx - barW / 2f + barW * frac, top + barH, shapePaint)
    }

    /**
     * A colossus: a body far larger than the screen, anchored with its centre above the top edge so only
     * the lower hull, eyes and a glowing core loom into view. Sells the "巨物感" — a giant you fight a piece of.
     */
    private fun drawColossus(canvas: Canvas, b: Boss, body: Int, eye: Int, foot: Int) {
        val r = b.radius
        val flash = b.hitFlash > 0f
        val hullBottom = b.y + r * 0.92f

        // Enrage / barrier rings (vast — only the lower arc crosses the screen).
        if (b.enraged) {
            val pulse = 0.5f + 0.5f * sin(animTime * 14f)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = minDim * 0.02f
            shapePaint.color = Color.argb((120 + pulse * 120).toInt().coerceIn(0, 255), 255, 60, 48)
            canvas.drawCircle(b.x, b.y, r * 1.02f, shapePaint)
            shapePaint.style = Paint.Style.FILL
        }
        if (b.invulnTime > 0f) {
            val pulse = 0.5f + 0.5f * sin(animTime * 16f)
            shapePaint.style = Paint.Style.STROKE
            shapePaint.strokeWidth = minDim * (0.02f + 0.01f * pulse)
            shapePaint.color = Color.argb((150 + pulse * 100).toInt().coerceIn(0, 255), 120, 230, 255)
            canvas.drawCircle(b.x, b.y, r * 1.05f, shapePaint)
            shapePaint.style = Paint.Style.FILL
        }

        // Main hull slab — extends well off the top of the screen.
        shapePaint.color = if (flash) 0xFFFFFFFF.toInt() else body
        canvas.drawRect(b.x - r * 0.95f, b.y - r, b.x + r * 0.95f, hullBottom, shapePaint)

        // Lower lip + a jagged jaw of spikes (the "maw").
        shapePaint.color = if (flash) 0xFFFFFFFF.toInt() else foot
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
            val glow = 0.6f + 0.4f * sin(animTime * 5f)
            val cr = r * 0.13f
            shapePaint.color = eye
            canvas.drawRect(b.x - cr * 1.3f, ey - cr * 0.8f, b.x + cr * 1.3f, ey + cr * 1.8f, shapePaint)
            shapePaint.color = Color.argb((glow * 255).toInt(), 255, 255, 255)
            canvas.drawRect(b.x - cr * 0.7f, ey - cr * 0.3f, b.x + cr * 0.7f, ey + cr * 1.3f, shapePaint)
        }

        drawBossHpBar(canvas, b)
    }

    private fun bossPalette(variant: Int): Triple<Int, Int, Int> = when (variant) {
        0 -> Triple(0xFF8A3DFF.toInt(), 0xFFFF3B30.toInt(), 0xFFFFC24B.toInt())    // WARDEN  purple
        1 -> Triple(0xFFE23B5A.toInt(), 0xFFFFE08A.toInt(), 0xFF7A1830.toInt())    // REAVER  crimson
        2 -> Triple(0xFF2FBF71.toInt(), 0xFFFFF36B.toInt(), 0xFF146C43.toInt())    // SEER    green
        3 -> Triple(0xFF3D7BFF.toInt(), 0xFF9AE0FF.toInt(), 0xFF1B3A8A.toInt())    // STORM    blue
        4 -> Triple(0xFF6A1F8F.toInt(), 0xFFE36BFF.toInt(), 0xFF2A0A3A.toInt())    // VOIDMAW  void purple
        else -> Triple(0xFFFFC24B.toInt(), 0xFFFF3B30.toInt(), 0xFF8A5A12.toInt()) // OVERLORD gold
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
                PowerType.DRONE -> Pair(0xFF8CF6FF.toInt(), "D")
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

    private fun drawShockwaves(canvas: Canvas) {
        if (shockwaves.isEmpty()) return
        shapePaint.style = Paint.Style.STROKE
        for (s in shockwaves) {
            val t = (1f - s.life / s.maxLife).coerceIn(0f, 1f)   // 0 → 1 as it expands
            val rr = s.maxR * t
            val a = ((1f - t) * 200f).toInt().coerceIn(0, 255)
            shapePaint.strokeWidth = minDim * 0.012f * (1f - t * 0.6f)
            shapePaint.color = (s.color and 0x00FFFFFF) or (a shl 24)
            canvas.drawCircle(s.x, s.y, rr, shapePaint)
        }
        shapePaint.style = Paint.Style.FILL
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

        // Current weapon + power level (and escort count), bottom centre.
        textPaint.color = weaponColor(weapon)
        textPaint.textSize = minDim * 0.05f
        val droneTag = if (droneCount > 0) "  ${droneCount}D" else ""
        canvas.drawText("${weaponName(weapon)}  Lv$powerLevel$droneTag", cx, h - margin - minDim * 0.03f, textPaint)

        // Overdrive gauge — a short arc along the bottom of the safe ring; glows while surging.
        shapePaint.style = Paint.Style.STROKE
        shapePaint.strokeWidth = minDim * 0.018f
        val startA = 70f; val sweep = 40f
        shapePaint.color = 0x33FFFFFF
        canvas.drawArc(arcRect, startA, sweep, false, shapePaint)
        if (overdriveTime > 0f) {
            val pulse = 0.5f + 0.5f * sin(animTime * 18f)
            shapePaint.color = Color.argb((180 + pulse * 70).toInt().coerceIn(0, 255), 120, 240, 255)
            canvas.drawArc(arcRect, startA, sweep, false, shapePaint)
        } else {
            val frac = (overdrive / OVERDRIVE_MAX).coerceIn(0f, 1f)
            if (frac > 0f) {
                shapePaint.color = if (frac >= 1f) 0xFF66E0FF.toInt() else 0xFF35C8E0.toInt()
                canvas.drawArc(arcRect, startA, sweep * frac, false, shapePaint)
            }
        }
        shapePaint.style = Paint.Style.FILL

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
