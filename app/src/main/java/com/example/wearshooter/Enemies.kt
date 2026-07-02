package com.example.wearshooter

import com.example.wearshooter.GameConfig.EBULLET_MAX
import com.example.wearshooter.GameConfig.EBULLET_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.EBULLET_SPEED
import com.example.wearshooter.GameConfig.ENEMY_MAX
import com.example.wearshooter.GameConfig.ENEMY_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.ENEMY_SPEED_BASE
import com.example.wearshooter.GameConfig.GUNNER_FIRE_INTERVAL
import com.example.wearshooter.GameConfig.RING_GAP_HALF
import com.example.wearshooter.GameConfig.SHIELDER_BODY_HP
import com.example.wearshooter.GameConfig.SHIELDER_SHIELD_HP
import com.example.wearshooter.GameConfig.SHIELDER_TRACK_SPEED
import com.example.wearshooter.GameConfig.TANK_FIRE_INTERVAL
import com.example.wearshooter.GameConfig.UNLOCK_BOSS_BONUS
import com.example.wearshooter.GameConfig.UNLOCK_RAMP
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The swarm: the progressive unlock schedule, per-type spawn stats and behaviour,
 * and the shot patterns shared by regular enemies (bosses layer their own on top).
 */

internal fun World.spawnEnemy(diff: Float) {
    val type = pickEnemyType(diff)
    // First time a new enemy joins the fight, flash its name — so each addition is a deliberate beat.
    if (!announced[type.ordinal]) {
        announced[type.ordinal] = true
        if (enemyUnlockTime(type) > 0f) showBanner(Palette.ENEMY_NAMES[type.ordinal], 1.2f)
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
    val e = enemyPool.obtain().reset(x, -r, r, type)
    val speedScale = 1f + (diff - 1f) * 0.16f
    // Base HP per type; scaledHp() grows it with difficulty.
    when (type) {
        EnemyType.GRUNT -> {
            e.scoreValue = 1; setEnemyHp(e, 1, diff)
            e.vy = ENEMY_SPEED_BASE * speedScale
        }
        EnemyType.WEAVER -> {
            e.scoreValue = 2; setEnemyHp(e, 1, diff)
            e.vy = ENEMY_SPEED_BASE * 0.8f * speedScale
            e.swayAmp = minDim * (0.05f + Random.nextFloat() * 0.06f)
            e.wobble = Random.nextFloat() * 6.283f
        }
        EnemyType.RUSHER -> {
            e.scoreValue = 2; setEnemyHp(e, 1, diff)
            e.vy = ENEMY_SPEED_BASE * 2.0f * speedScale
        }
        EnemyType.GUNNER -> {
            e.scoreValue = 4; setEnemyHp(e, 3, diff)
            e.vy = ENEMY_SPEED_BASE * 0.7f * speedScale
            e.fireTimer = 0.6f + Random.nextFloat() * GUNNER_FIRE_INTERVAL
        }
        EnemyType.TANK -> {
            e.scoreValue = 8; setEnemyHp(e, 7, diff)
            e.vy = ENEMY_SPEED_BASE * 0.5f * speedScale
            e.fireTimer = 1.0f + Random.nextFloat() * TANK_FIRE_INTERVAL
        }
        EnemyType.SPLITTER -> {
            // Drifts in steadily; bursts into a spray of fast grunts when killed.
            e.scoreValue = 5; setEnemyHp(e, 4, diff)
            e.vy = ENEMY_SPEED_BASE * 0.65f * speedScale
        }
        EnemyType.ORBITER -> {
            // Spirals down its column while peppering aimed shots — hard to pin down.
            e.scoreValue = 4; setEnemyHp(e, 2, diff)
            e.vy = ENEMY_SPEED_BASE * 0.55f * speedScale
            e.swayAmp = minDim * (0.10f + Random.nextFloat() * 0.05f)   // orbit radius
            e.orbitAng = Random.nextFloat() * 6.283f
            e.fireTimer = 0.5f + Random.nextFloat() * 1.2f
        }
        EnemyType.SHIELDER -> {
            // Slow armoured blocker: slides to stay in your firing column and soaks shots
            // on its frontal shield until it shatters, exposing a soft core.
            e.scoreValue = 7; setEnemyHp(e, SHIELDER_BODY_HP, diff)
            e.maxShieldHp = scaledHp(SHIELDER_SHIELD_HP, diff)
            e.shieldHp = e.maxShieldHp
            e.vy = ENEMY_SPEED_BASE * 0.42f * speedScale
        }
        EnemyType.DARTER -> {
            // Streaks in from a side edge across the upper screen, firing aimed shots as it goes.
            e.scoreValue = 3; setEnemyHp(e, 1, diff)
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
            e.scoreValue = 4; setEnemyHp(e, 5, diff)
            e.vy = ENEMY_SPEED_BASE * 0.4f * speedScale
            e.swayAmp = minDim * 0.03f
            e.wobble = Random.nextFloat() * 6.283f
        }
        EnemyType.DIVER -> {
            // Cruises in, telegraphs, then lunges straight at where the jet was.
            e.scoreValue = 3; setEnemyHp(e, 2, diff)
            e.vy = ENEMY_SPEED_BASE * 0.5f * speedScale
            e.fireTimer = 0.6f + Random.nextFloat() * 0.5f
            e.diving = false
        }
    }
    enemies.add(e)
}

private fun World.setEnemyHp(e: Enemy, base: Int, diff: Float) {
    e.maxHp = scaledHp(base, diff); e.hp = e.maxHp
}

/** A synchronized row of grunts that enters together — gives the swarm rhythm. */
internal fun World.spawnFormation(diff: Float) {
    val r = minDim * ENEMY_RADIUS_FRACTION
    val n = 3 + Random.nextInt(3)                       // 3..5 abreast
    val half = (safeHalfWidthAt(player.y) - margin - r).coerceAtLeast(minDim * 0.12f)
    val vy = ENEMY_SPEED_BASE * 0.9f * (1f + (diff - 1f) * 0.16f)
    val sway = if (Random.nextFloat() < 0.5f) minDim * 0.03f else 0f
    for (i in 0 until n) {
        if (enemies.size >= ENEMY_MAX) break
        val t = if (n == 1) 0.5f else i.toFloat() / (n - 1)
        val x = cx - half + 2f * half * t
        val e = enemyPool.obtain().reset(x, -r - i * r * 0.4f, r, EnemyType.GRUNT)
        e.scoreValue = 1; setEnemyHp(e, 1, diff); e.vy = vy
        if (sway > 0f) { e.swayAmp = sway; e.wobble = t * 6.283f }
        enemies.add(e)
    }
}

/**
 * Progression clock that drives the unlock schedule: time survived, plus a chunk for each boss beaten
 * (so skill advances the reveal too). Kept separate from [World.difficulty] — that scales HP/speed/density;
 * this controls *which* enemies have entered the game yet.
 */
private fun World.unlockProgress(): Float = elapsed + bossesBeaten * UNLOCK_BOSS_BONUS

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

/**
 * Picks among only the enemy types unlocked so far. A freshly-unlocked type fades in over [UNLOCK_RAMP]
 * seconds (rare at first, then common), so every addition feels introduced rather than dumped in.
 */
private fun World.pickEnemyType(diff: Float): EnemyType {
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

internal fun World.updateEnemies(dt: Float, diff: Float) {
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
private fun World.detonateMine(e: Enemy, diff: Float) {
    e.alive = false
    explode(e.x, e.y, e.radius * 1.2f)
    radialBurst(e.x, e.y, e.radius, 9 + diff.toInt(), diff)
    vibrate?.invoke(20)
}

/** A killed splitter bursts into a fan of fast little rushers (queued, never re-splits). */
internal fun World.spawnSplitterChildren(parent: Enemy) {
    val diff = difficulty()
    val n = if (diff >= 5f) 4 else 3
    val cr = parent.radius * 0.6f
    val spread = parent.radius * 1.1f
    for (i in 0 until n) {
        if (enemies.size + spawnQueue.size >= ENEMY_MAX) break
        val t = if (n == 1) 0f else (i.toFloat() / (n - 1)) * 2f - 1f   // -1..1
        val x = parent.x + t * spread
        val child = enemyPool.obtain().reset(x, parent.y, cr, EnemyType.RUSHER).apply {
            vy = ENEMY_SPEED_BASE * (1.7f + 0.1f * abs(t)) * (1f + (diff - 1f) * 0.16f)
            swayAmp = parent.radius * 0.4f; wobble = t * 1.5f
        }
        spawnQueue.add(child)
    }
}

// =====================================================================
//  Shot patterns shared by enemies and bosses
// =====================================================================
internal fun World.fireEnemyBullet(x: Float, y: Float, vx: Float, vy: Float, r: Float): Boolean {
    if (enemyBullets.size >= EBULLET_MAX) return false
    enemyBullets.add(enemyBulletPool.obtain().reset(x, y, vx, vy, r))
    return true
}

internal fun World.aimedShot(x: Float, y: Float, r: Float, diff: Float) {
    val ang = atan2(player.y - y, player.x - x)
    val sp = EBULLET_SPEED * (1f + (diff - 1f) * 0.12f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    fireEnemyBullet(x, y + r, cos(ang) * sp, sin(ang) * sp, br)
}

internal fun World.fanShot(x: Float, y: Float, r: Float, count: Int, diff: Float) {
    val sp = EBULLET_SPEED * (1f + (diff - 1f) * 0.12f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    val center = atan2(player.y - y, player.x - x)
    val spread = 0.5f
    for (i in 0 until count) {
        val a = center + (i - (count - 1) / 2f) * spread
        if (!fireEnemyBullet(x, y + r, cos(a) * sp, sin(a) * sp, br)) break
    }
}

/** Angular distance between two headings, wrapped to [0, π]. */
internal fun angGap(a: Float, center: Float): Float {
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
internal fun World.radialBurst(x: Float, y: Float, r: Float, count: Int, diff: Float) {
    if (count <= 0) return
    val sp = EBULLET_SPEED * 0.88f * (1f + (diff - 1f) * 0.1f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    val base = (animTime * 0.7f)
    val gapCenter = 1.5708f + (Random.nextFloat() * 2f - 1f) * 0.35f
    val gapHalf = RING_GAP_HALF + count * 0.006f
    for (i in 0 until count) {
        val a = base + i.toFloat() / count * 6.283f
        if (angGap(a, gapCenter) < gapHalf) continue          // leave the escape lane open
        if (!fireEnemyBullet(x, y, cos(a) * sp, sin(a) * sp + 30f, br)) break
    }
    spawnShockwave(x, y, r, Palette.SHOCK_PINK)
    if (count >= 16) shake(0.12f, minDim * 0.012f)
}

internal fun World.updateEnemyBullets(dt: Float) {
    val pad = minDim * 0.12f
    for (eb in enemyBullets) {
        eb.x += eb.vx * dt
        eb.y += eb.vy * dt
        if (eb.y > h + pad || eb.y < -pad || eb.x < -pad || eb.x > w + pad) eb.alive = false
    }
}
