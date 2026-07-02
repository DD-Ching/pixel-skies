package com.example.wearshooter

import com.example.wearshooter.GameConfig.BOSS_BASE_HP
import com.example.wearshooter.GameConfig.BOSS_HP_PER_KILL
import com.example.wearshooter.GameConfig.BOSS_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.COLOSSUS_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.EBULLET_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.EBULLET_SPEED
import com.example.wearshooter.GameConfig.ENRAGE_HP_FRACTION
import com.example.wearshooter.GameConfig.FINAL_BOSS_EVERY
import com.example.wearshooter.GameConfig.FINAL_BOSS_HP_MULT
import com.example.wearshooter.GameConfig.LASER_FIRE
import com.example.wearshooter.GameConfig.LASER_HALFW_FRACTION
import com.example.wearshooter.GameConfig.LASER_MAX
import com.example.wearshooter.GameConfig.LASER_SWEEP_SPEED
import com.example.wearshooter.GameConfig.LASER_WARN
import com.example.wearshooter.GameConfig.RING_GAP_HALF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Boss fights: five rotating bosses plus the colossal OVERLORD, their bullet
 * patterns, and the telegraphed laser set-pieces.
 */

internal fun World.spawnBoss() {
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
        it.name = Palette.BOSS_NAMES[variant]
    }
    releaseAll(enemyBullets, enemyBulletPool)
    stageBanner = 1.4f
    showBanner(if (isFinal) "!! OVERLORD !!" else Palette.BOSS_NAMES[variant], 1.8f)
    if (isFinal) { shake(0.4f, minDim * 0.03f); vibrate?.invoke(60) }
}

internal fun World.updateBoss(b: Boss, dt: Float, diff: Float) {
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

/** The five rotating bosses, each with its own four-beat pattern loop. */
private fun World.fireBossPattern(b: Boss, diff: Float) {
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

/** OVERLORD — the every-6th mega-boss. Three HP-gated phases that pile patterns on top. */
private fun World.fireFinalPattern(b: Boss, diff: Float) {
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

internal fun World.bossDefeated(b: Boss) {
    b.alive = false
    val mult = if (b.isFinal) 4 else 1
    val bonus = 2000 * (1 + bossesBeaten) * mult
    addScore(bonus)
    spawnPopup(b.x, b.y, "${b.name} DOWN +$bonus", Palette.MULTIPLIER)
    bossesBeaten++
    bossTimer = 0f
    stage++; stageBanner = 1.6f
    releaseAll(enemyBullets, enemyBulletPool)
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
//  Boss-only bullet patterns
// =====================================================================
/** A raking column of straight-down bullets; combined with the boss's sweep it scythes across. */
private fun World.streamDown(b: Boss, diff: Float) {
    val sp = EBULLET_SPEED * (1f + (diff - 1f) * 0.1f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    fireEnemyBullet(b.x, b.y + b.radius, 0f, sp, br)
    fireEnemyBullet(b.x - b.radius * 0.45f, b.y + b.radius, 0f, sp, br)
    fireEnemyBullet(b.x + b.radius * 0.45f, b.y + b.radius, 0f, sp, br)
}

/** A horizontal curtain of falling bullets with one opening — slide into the gap. */
private fun World.gapWall(y: Float, diff: Float) {
    val br = minDim * EBULLET_RADIUS_FRACTION
    val sp = EBULLET_SPEED * 0.8f * (1f + (diff - 1f) * 0.1f)
    val spanHalf = (safeHalfWidthAt(y) - margin).coerceAtLeast(minDim * 0.2f)
    val gapHalf = player.width * 1.15f
    val gapCenter = cx + (Random.nextFloat() * 2f - 1f) * spanHalf * 0.55f
    val count = 13
    for (i in 0 until count) {
        val x = cx - spanHalf + 2f * spanHalf * i / (count - 1)
        if (abs(x - gapCenter) < gapHalf) continue        // leave the opening
        if (!fireEnemyBullet(x, y, 0f, sp, br)) break
    }
}

/** Fired in rapid small bursts at a rotating angle, tracing two spiral arms. */
private fun World.spiral(b: Boss, diff: Float) {
    val sp = EBULLET_SPEED * 0.85f * (1f + (diff - 1f) * 0.1f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    val base = b.phase * 6f
    for (k in 0 until 2) {
        val a = base + k * 3.14159f
        if (!fireEnemyBullet(b.x, b.y, cos(a) * sp, sin(a) * sp + 25f, br)) break
    }
}

/** A tight shotgun cone aimed straight at the player — the spread is the pressure. */
private fun World.aimedSpread(x: Float, y: Float, r: Float, count: Int, spread: Float, diff: Float) {
    if (count <= 0) return
    val sp = EBULLET_SPEED * (1f + (diff - 1f) * 0.12f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    val center = atan2(player.y - y, player.x - x)
    for (i in 0 until count) {
        val a = center + (i - (count - 1) / 2f) * spread
        if (!fireEnemyBullet(x, y + r, cos(a) * sp, sin(a) * sp, br)) break
    }
}

/** A radial spray whose per-spoke speed pulses, so the bullets bloom into flower petals. */
private fun World.petalBurst(b: Boss, count: Int, diff: Float) {
    if (count <= 0) return
    val baseSp = EBULLET_SPEED * 0.85f * (1f + (diff - 1f) * 0.1f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    val rot = b.spin * 0.8f
    val gapCenter = 1.5708f + (Random.nextFloat() * 2f - 1f) * 0.3f
    val gapHalf = RING_GAP_HALF + count * 0.005f
    for (i in 0 until count) {
        val a = rot + i.toFloat() / count * 6.283f
        if (angGap(a, gapCenter) < gapHalf) continue          // keep the escape lane clear
        val sp = baseSp * (0.55f + 0.45f * abs(sin(a * 5f)))   // five speed lobes → five petals
        if (!fireEnemyBullet(b.x, b.y, cos(a) * sp, sin(a) * sp + 20f, br)) break
    }
    spawnShockwave(b.x, b.y, b.radius, Palette.SHOCK_CYAN)
}

/** Four fast beams in a plus that slowly rotates into an X and back — sweeping lasers of lead. */
private fun World.crossShot(x: Float, y: Float, diff: Float, rotate: Float) {
    val sp = EBULLET_SPEED * 1.2f * (1f + (diff - 1f) * 0.1f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    val arms = 4
    for (i in 0 until arms) {
        val a = rotate * 0.6f + i.toFloat() / arms * 6.283f
        if (!fireEnemyBullet(x, y, cos(a) * sp, sin(a) * sp, br)) break
    }
}

/** A rapid-fire rotating pinwheel: fired on a tiny cadence so the arms trace spiral curves. */
private fun World.spokeSpin(b: Boss, arms: Int, diff: Float) {
    val sp = EBULLET_SPEED * 0.9f * (1f + (diff - 1f) * 0.1f)
    val br = minDim * EBULLET_RADIUS_FRACTION
    val base = b.spin * 3.2f
    for (k in 0 until arms) {
        val a = base + k.toFloat() / arms * 6.283f
        if (!fireEnemyBullet(b.x, b.y, cos(a) * sp, sin(a) * sp, br)) break
    }
}

/** A full-width falling curtain where each column is offset sideways into a snaking wave. */
private fun World.waveColumn(b: Boss, diff: Float) {
    val br = minDim * EBULLET_RADIUS_FRACTION
    val sp = EBULLET_SPEED * 0.8f * (1f + (diff - 1f) * 0.1f)
    val n = 9
    val spanHalf = (safeHalfWidthAt(b.y) - margin).coerceAtLeast(minDim * 0.2f)
    for (i in 0 until n) {
        val t = i.toFloat() / (n - 1)
        val x = cx - spanHalf + 2f * spanHalf * t
        val vx = sin(b.phase * 3f + t * 6.283f) * sp * 0.5f
        if (!fireEnemyBullet(x, b.y, vx, sp, br)) break
    }
}

// =====================================================================
//  Lasers — telegraphed sweeping beams
// =====================================================================
internal fun World.updateLasers(dt: Float) {
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
internal fun laserLethal(l: Laser): Boolean = l.warnTime <= 0f && l.fireTime > 0f

/**
 * The boss commits to a laser set-piece. It raises an invulnerable barrier for the whole
 * attack — so the fight stops being a damage race and becomes a pure dodge for those seconds.
 */
private fun World.bossLaser(b: Boss, comb: Boolean) {
    if (lasers.isNotEmpty()) { b.fireTimer = 0.2f; return }   // already mid-beam, retry shortly
    if (comb) combLasers() else sweepLaser()
    val dur = if (comb) LASER_WARN + LASER_FIRE else LASER_WARN + 2.0f
    b.invulnTime = dur + 0.25f
    b.fireTimer = dur + 0.3f          // hold all other fire until the beam is spent
    vibrate?.invoke(30)
}

/**
 * A "comb" of vertical beams spanning the dodge band with one safe lane. The whole comb drifts
 * sideways so the lane keeps moving and the player must keep repositioning to stay alive.
 */
private fun World.combLasers() {
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
private fun World.sweepLaser() {
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
