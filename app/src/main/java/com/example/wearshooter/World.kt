package com.example.wearshooter

import com.example.wearshooter.GameConfig.BOSS_INTENSITY_MAX
import com.example.wearshooter.GameConfig.BOSS_INTENSITY_PER_KILL
import com.example.wearshooter.GameConfig.BOSS_INTERVAL
import com.example.wearshooter.GameConfig.BOSS_INTERVAL_MIN
import com.example.wearshooter.GameConfig.BOSS_PLAYER_Y_FRACTION
import com.example.wearshooter.GameConfig.BACKDROP_COUNT
import com.example.wearshooter.GameConfig.BACKDROP_FADE
import com.example.wearshooter.GameConfig.BACKDROP_STAGES_EACH
import com.example.wearshooter.GameConfig.COMBO_WINDOW
import com.example.wearshooter.GameConfig.DIFFICULTY_CAP
import com.example.wearshooter.GameConfig.EBULLET_MAX
import com.example.wearshooter.GameConfig.EDGE_MARGIN_FRACTION
import com.example.wearshooter.GameConfig.ENEMY_HP_SCALE
import com.example.wearshooter.GameConfig.ENEMY_MAX
import com.example.wearshooter.GameConfig.EXTRA_LIFE_EVERY
import com.example.wearshooter.GameConfig.GRAZE_BAND_FRACTION
import com.example.wearshooter.GameConfig.GRAZE_BONUS
import com.example.wearshooter.GameConfig.INVULN_TIME
import com.example.wearshooter.GameConfig.KEY_SPEED
import com.example.wearshooter.GameConfig.LASER_MAX
import com.example.wearshooter.GameConfig.MAX_BOMBS
import com.example.wearshooter.GameConfig.MAX_DRONES
import com.example.wearshooter.GameConfig.MAX_LIVES
import com.example.wearshooter.GameConfig.MAX_MULTIPLIER
import com.example.wearshooter.GameConfig.OVERDRIVE_PER_GRAZE
import com.example.wearshooter.GameConfig.OVERDRIVE_PER_KILL
import com.example.wearshooter.GameConfig.PLAYER_SIZE_FRACTION
import com.example.wearshooter.GameConfig.PLAYER_Y_FRACTION
import com.example.wearshooter.GameConfig.ROTARY_INVERT
import com.example.wearshooter.GameConfig.ROTARY_SENSITIVITY
import com.example.wearshooter.GameConfig.SCORE_PER_KILL
import com.example.wearshooter.GameConfig.SPAWN_INTERVAL_MIN
import com.example.wearshooter.GameConfig.SPAWN_INTERVAL_START
import com.example.wearshooter.GameConfig.STAGE_DURATION
import com.example.wearshooter.GameConfig.START_BOMBS
import com.example.wearshooter.GameConfig.START_LIVES
import com.example.wearshooter.GameConfig.STAR_COUNT
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

internal enum class GameState { READY, PLAYING, PAUSED, GAME_OVER }

/**
 * The simulation core: run state, entities, difficulty, collisions, scoring.
 *
 * The rest of the game hangs off this class as focused modules:
 *  - [Weapons.kt]  player fire patterns, drones, Overdrive, power-ups
 *  - [Enemies.kt]  spawn schedule, per-type behaviour, enemy shots
 *  - [Bosses.kt]   boss patterns, telegraphed lasers
 *  - [Fx.kt]       particles, shockwaves, popups, shake, starfield
 *  - [Renderer.kt] every pixel drawn (World never touches a Canvas)
 *
 * Threading: [update] only ever runs on the game-loop thread, back to back with
 * the render, so the entity lists need no locking. Input methods (onTap /
 * onLongPress / onRotary / onKey) run on the UI thread and only touch @Volatile
 * flags or a synchronized rotary accumulator that the loop drains once per frame.
 */
class World {

    /** Optional hook so the view can fire haptics from the game thread. */
    var vibrate: ((Long) -> Unit)? = null

    /** High-score persistence hooks (wired by the view to SharedPreferences). */
    var loadHighScore: (() -> Int)? = null
    var saveHighScore: ((Int) -> Unit)? = null

    // ---- Screen geometry ----
    internal var w = 0f
    internal var h = 0f
    internal var cx = 0f
    internal var cy = 0f
    internal var radius = 0f
    internal var minDim = 0f
    internal var margin = 0f
    internal var playerBaseY = 0f          // resting jet height; it rises from here during boss fights
    internal var initialized = false

    // ---- Object pools (see Pools.kt — the sim never allocates these mid-run) ----
    internal val bulletPool = Pool(64) { Bullet() }
    internal val enemyBulletPool = Pool(96) { EnemyBullet() }
    internal val enemyPool = Pool(ENEMY_MAX) { Enemy() }
    internal val particlePool = Pool(128) { Particle() }

    // ---- Entities ----
    internal val player = Player()
    internal val bullets = ArrayList<Bullet>(64)
    internal val enemies = ArrayList<Enemy>(ENEMY_MAX + 8)
    internal val enemyBullets = ArrayList<EnemyBullet>(EBULLET_MAX + 16)
    internal val powerUps = ArrayList<PowerUp>(16)
    internal val particles = ArrayList<Particle>(192)
    internal val popups = ArrayList<Popup>(24)
    internal val stars = ArrayList<Star>(STAR_COUNT)
    internal val lasers = ArrayList<Laser>(LASER_MAX + 4)
    internal val drones = ArrayList<Drone>(MAX_DRONES)
    internal val shockwaves = ArrayList<Shockwave>(12)
    internal var boss: Boss? = null
    // Enemies spawned mid-frame (e.g. splitter children) are buffered here and flushed
    // after the update loops so we never mutate `enemies` while iterating it.
    internal val spawnQueue = ArrayList<Enemy>(8)

    // ---- Run state ----
    internal var state = GameState.READY
    internal var score = 0
    internal var best = 0
    internal var elapsed = 0f
    internal var animTime = 0f
    internal var newBest = false           // this run beat the record (shown on game over)
    private var bestAtStart = 0

    // Player progression
    internal var lives = START_LIVES
    internal var bombs = START_BOMBS
    internal var powerLevel = 1
    internal var shield = 0
    internal var invuln = 0f
    internal var weapon = WeaponType.VULCAN
    internal var nextExtraLife = EXTRA_LIFE_EVERY
    internal var droneCount = 0            // how many escort drones are currently deployed
    internal var overdrive = 0f            // surge gauge, 0..OVERDRIVE_MAX
    internal var overdriveTime = 0f        // >0 while the surge is firing

    // Combo / multiplier / graze
    internal var multiplier = 1
    internal var comboKills = 0
    internal var comboTimer = 0f
    internal var grazeCount = 0

    // Pacing
    internal var fireTimer = 0f
    internal var spawnTimer = 0f
    internal var spawnInterval = SPAWN_INTERVAL_START
    internal var stage = 1
    internal var stageBanner = 0f
    internal var bossTimer = 0f
    internal var bossesBeaten = 0
    internal val announced = BooleanArray(EnemyType.entries.size)  // has each enemy type had its debut call-out?

    // FX
    internal var shakeTime = 0f
    internal var shakeMag = 0f
    internal var slowmoTime = 0f           // brief cinematic slow-motion (boss kill)
    internal var bombFlash = 0f
    internal var eventBanner = ""          // big centred call-out (boss name reveal, OVERLORD DOWN…)
    internal var eventBannerTime = 0f

    // Scenery: a themed backdrop that cycles as you progress (space → nebula → station → star → planet).
    internal var theme = 0
    internal var prevTheme = 0
    internal var themeFade = 1f            // 0..1 crossfade into the current theme

    // ---- Input ----
    private val inputLock = Any()
    private var rotaryAccum = 0f
    @Volatile private var keyDir = 0
    @Volatile private var tapPending = false
    @Volatile private var longPressPending = false

    // =====================================================================
    //  Input (UI thread)
    // =====================================================================
    fun onRotary(delta: Float) { synchronized(inputLock) { rotaryAccum += delta } }
    fun onKey(dir: Int) { keyDir = dir }
    fun onTap() { tapPending = true }
    fun onLongPress() { longPressPending = true }
    fun onAppPause() { if (state == GameState.PLAYING) state = GameState.PAUSED }

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

        if (!initialized) {
            player.x = cx; initStars()
            best = loadHighScore?.invoke() ?: 0   // restore persisted high score
            initialized = true
        }
        clampPlayer()
    }

    /** Half-width of the round screen at a given y (chord of the circle). */
    internal fun safeHalfWidthAt(y: Float): Float {
        val dy = y - cy
        val inside = radius * radius - dy * dy
        return if (inside <= 0f) 0f else sqrt(inside)
    }

    internal fun clampPlayer() {
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
        if (eventBannerTime > 0f) eventBannerTime -= dt

        val rotary = consumeRotary()
        if (tapPending) { tapPending = false; handleTap() }
        if (longPressPending) { longPressPending = false; handleLongPress() }

        when (state) {
            GameState.PLAYING -> {
                // Slow-mo decays in real time, but the simulation runs at a reduced step while it lasts.
                if (slowmoTime > 0f) slowmoTime -= dt
                val sim = if (slowmoTime > 0f) dt * 0.4f else dt
                updatePlaying(sim, rotary)
            }
            GameState.READY, GameState.GAME_OVER -> { updateParticles(dt); updatePopups(dt) }
            GameState.PAUSED -> { /* frozen */ }
        }
        updateStars(dt, moving = state == GameState.PLAYING)
    }

    private fun handleTap() {
        when (state) {
            GameState.READY, GameState.GAME_OVER -> startGame()
            GameState.PAUSED -> state = GameState.PLAYING
            GameState.PLAYING -> useBomb()          // tap mid-flight drops a bomb
        }
    }

    private fun handleLongPress() {
        when (state) {
            GameState.PLAYING -> state = GameState.PAUSED
            GameState.PAUSED -> state = GameState.PLAYING
            else -> {}
        }
    }

    private fun startGame() {
        releaseAll(bullets, bulletPool); releaseAll(enemies, enemyPool)
        releaseAll(enemyBullets, enemyBulletPool); releaseAll(particles, particlePool)
        releaseAll(spawnQueue, enemyPool)
        powerUps.clear(); popups.clear(); lasers.clear()
        drones.clear(); shockwaves.clear(); boss = null
        eventBanner = ""; eventBannerTime = 0f
        score = 0; elapsed = 0f
        newBest = false; bestAtStart = best
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
        state = GameState.PLAYING
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
        val interval = weaponInterval() * (if (overdriveTime > 0f) GameConfig.OVERDRIVE_FIRE_MULT else 1f)
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
            for (e in spawnQueue) {
                if (enemies.size < ENEMY_MAX) enemies.add(e) else enemyPool.release(e)
            }
            spawnQueue.clear()
        }

        // Combo decay — let a chain lapse if you stop killing.
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) { multiplier = 1; comboKills = 0 }
        }

        sweep(bullets, bulletPool)
        sweep(enemies, enemyPool)
        sweep(enemyBullets, enemyBulletPool)
        sweep(lasers)
        sweep(powerUps)
        if (boss?.alive == false) boss = null
    }

    /**
     * Continuous difficulty scalar, capped so it can't run away. Driven mainly by time
     * survived, bosses beaten, and the player's own power level (so enemies toughen as
     * your firepower grows). Score adds only a little — combos inflate score fast, so a
     * big score weight here would explode difficulty.
     */
    internal fun difficulty(): Float =
        (1f + elapsed / 36f + bossesBeaten * 0.5f + (powerLevel - 1) * 0.16f +
            droneCount * 0.14f + score / 40000f)
            .coerceAtMost(DIFFICULTY_CAP)

    /**
     * Bullet-density multiplier for boss/enemy patterns. Unlike [difficulty] (which is capped so
     * enemy stats can't explode), this keeps climbing with every boss beaten — so the *amount* of
     * lead in the air ramps endlessly into a proper bullet-hell, while speeds/HP stay sane.
     */
    internal fun intensity(): Float =
        (1f + bossesBeaten * BOSS_INTENSITY_PER_KILL).coerceAtMost(BOSS_INTENSITY_MAX)

    /** Enemy HP scales up continuously with difficulty so they never become trivial. */
    internal fun scaledHp(base: Int, diff: Float): Int =
        max(base, (base * (1f + (diff - 1f) * ENEMY_HP_SCALE)).roundToInt())

    // =====================================================================
    //  Bomb
    // =====================================================================
    private fun useBomb() {
        if (bombs <= 0) { vibrate?.invoke(15); return }
        bombs--
        bombFlash = 0.3f
        shake(0.4f, minDim * 0.04f)
        vibrate?.invoke(60)
        releaseAll(enemyBullets, enemyBulletPool)
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
    //  Collisions — all squared-distance; hypot() is too slow for the hot loops
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
            val dx = p.x - player.x; val dy = p.y - player.y
            val rr = grabR + p.radius
            if (dx * dx + dy * dy <= rr * rr) {
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
            val dx = eb.x - player.x; val dy = eb.y - player.y
            val d2 = dx * dx + dy * dy
            val hitR = playerR + eb.radius
            if (d2 <= hitR * hitR) {
                eb.alive = false
                playerHit()
                return
            } else if (!eb.grazed && d2 <= (hitR + grazeBand) * (hitR + grazeBand)) {
                eb.grazed = true
                grazeCount++
                addScore(GRAZE_BONUS)
                addOverdrive(OVERDRIVE_PER_GRAZE)
                if (comboTimer in 0.001f..0.45f) comboTimer = 0.45f   // keep a fading chain warm
                if (grazeCount % 10 == 0) {
                    spawnPopup(player.x, player.y - player.height * 1.1f, "GRAZE x$grazeCount", Palette.OD_FULL)
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
            if (b.alive) {
                val dx = b.x - player.x; val dy = b.y - player.y
                val rr = playerR + b.radius * 0.85f
                if (dx * dx + dy * dy <= rr * rr) playerHit()
            }
        }
    }

    internal fun killEnemy(e: Enemy) {
        e.alive = false
        bumpCombo()
        addOverdrive(OVERDRIVE_PER_KILL)
        val gain = SCORE_PER_KILL * e.scoreValue * multiplier
        addScore(gain)
        if (e.scoreValue >= 4) spawnPopup(e.x, e.y, "+$gain", Palette.WHITE)  // gunners/tanks
        explode(e.x, e.y, e.radius)
        if (e.type == EnemyType.SPLITTER) spawnSplitterChildren(e)
        if (e.type == EnemyType.MINE) radialBurst(e.x, e.y, e.radius, 9, difficulty())   // shrapnel ring
        maybeDrop(e.x, e.y)
    }

    private fun bumpCombo() {
        comboTimer = COMBO_WINDOW
        comboKills++
        // Each multiplier tier costs more kills, so x8 is genuinely earned.
        multiplier = (1 + comboKills / 3).coerceAtMost(MAX_MULTIPLIER)
    }

    internal fun addScore(points: Int) {
        score += points
        if (score > best) best = score
        // Free life each time the score crosses a milestone.
        while (score >= nextExtraLife) {
            nextExtraLife += EXTRA_LIFE_EVERY
            if (lives < MAX_LIVES) {
                lives++
                spawnPopup(player.x, player.y - player.height * 1.4f, "1UP", Palette.CYAN)
                vibrate?.invoke(40)
            }
        }
    }

    internal fun playerHit() {
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
            newBest = best > bestAtStart
            saveHighScore?.invoke(best)           // persist on game over
            state = GameState.GAME_OVER
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
            val dx = eb.x - player.x; val dy = eb.y - player.y
            if (dx * dx + dy * dy <= r * r) eb.alive = false
        }
    }
}
