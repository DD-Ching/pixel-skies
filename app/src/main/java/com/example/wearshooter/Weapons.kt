package com.example.wearshooter

import com.example.wearshooter.GameConfig.BULLET_SPEED
import com.example.wearshooter.GameConfig.DRONE_FOLLOW
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_HELIX
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_HOMING
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_LASER
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_VULCAN
import com.example.wearshooter.GameConfig.FIRE_INTERVAL_WIDE
import com.example.wearshooter.GameConfig.FIRE_RATE_POWER_MIN
import com.example.wearshooter.GameConfig.FIRE_RATE_POWER_STEP
import com.example.wearshooter.GameConfig.GUN_SPREAD
import com.example.wearshooter.GameConfig.MAX_BOMBS
import com.example.wearshooter.GameConfig.MAX_DRONES
import com.example.wearshooter.GameConfig.MAX_LIVES
import com.example.wearshooter.GameConfig.MAX_POWER
import com.example.wearshooter.GameConfig.OVERDRIVE_DURATION
import com.example.wearshooter.GameConfig.OVERDRIVE_MAX
import com.example.wearshooter.GameConfig.POWERUP_DROP_CHANCE
import com.example.wearshooter.GameConfig.POWERUP_RADIUS_FRACTION
import com.example.wearshooter.GameConfig.POWERUP_SPEED
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The player's arsenal: five weapon lines, escort drones (僚機), the Overdrive
 * surge, and the power-up economy that feeds them all.
 */

/** +1 to every shot's damage while Overdrive is surging. */
internal fun World.odBonus() = if (overdriveTime > 0f) 1 else 0

/** Per-weapon base cadence, sped up as power level climbs (floored so it can't run away). */
internal fun World.weaponInterval(): Float {
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

private fun World.shoot(x: Float, y: Float, vy: Float, vx: Float = 0f): Bullet =
    bulletPool.obtain().reset(x, y, vy, vx).also { bullets.add(it) }

internal fun World.fireWeapon() {
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
private fun World.fireVulcan(muzzleY: Float, off: Float) {
    val streams = min(2 + (powerLevel - 1), 6)        // 2..6
    val dmg = 1 + (powerLevel - 1) / 3 + odBonus()    // 1..4 (+OD)
    val gap = off * 1.1f
    for (i in 0 until streams) {
        val x = player.x + (i - (streams - 1) / 2f) * gap
        shoot(x, muzzleY, -BULLET_SPEED).damage = dmg
    }
    if (powerLevel >= 6) {                            // outer angled wings at high power
        val edge = (streams / 2f + 0.6f) * gap
        shoot(player.x - edge, muzzleY, -BULLET_SPEED, -BULLET_SPEED * 0.22f).damage = dmg
        shoot(player.x + edge, muzzleY, -BULLET_SPEED, BULLET_SPEED * 0.22f).damage = dmg
    }
}

/** Piercing, high-speed, high-damage — punches through lines and shreds the boss. */
private fun World.fireLaser(muzzleY: Float, off: Float) {
    val bolts = 1 + powerLevel / 2                   // 1..6
    val dmg = 2 + powerLevel / 2 + odBonus()          // 2..7 (+OD)
    val pierce = 3 + powerLevel                       // 4..13
    val vy = -BULLET_SPEED * 1.35f
    val gap = off * 1.2f
    for (i in 0 until bolts) {
        val x = player.x + (i - (bolts - 1) / 2f) * gap
        shoot(x, muzzleY, vy).also { it.damage = dmg; it.pierce = pierce; it.kind = 1 }
    }
}

/** A broad fan of pellets — weak per shot but blankets the swarm; widens with power. */
private fun World.fireWide(muzzleY: Float, off: Float) {
    val count = min(3 + powerLevel * 2, 15)           // 5..15 pellets
    val dmg = 1 + (powerLevel - 1) / 4 + odBonus()    // 1..3 (+OD)
    val spread = 0.16f
    for (i in 0 until count) {
        val a = (i - (count - 1) / 2f) * spread
        shoot(player.x, muzzleY, -BULLET_SPEED * 0.9f, sin(a) * BULLET_SPEED * 0.55f)
            .also { it.damage = dmg; it.kind = 2 }
    }
}

/**
 * Seeking missiles — fewer shots, slower, but they curve onto whatever's nearest, so the
 * swarm and the boss get hunted down even while you focus on dodging. A real late-game treat.
 */
private fun World.fireHoming(muzzleY: Float, off: Float) {
    val count = min(1 + powerLevel / 2, 5)            // 1..5 missiles
    val dmg = 2 + (powerLevel - 1) / 2 + odBonus()    // 2..6 (+OD)
    val speed = BULLET_SPEED * 0.82f
    for (i in 0 until count) {
        val x = player.x + (i - (count - 1) / 2f) * off * 1.4f
        shoot(x, muzzleY, -speed).also {
            it.damage = dmg; it.kind = 3; it.homing = true; it.speed = speed; it.life = 2.6f
        }
    }
}

/**
 * HELIX — twin (then quad) bolts that weave around their travel line in opposite phase, braiding
 * into a double helix. Pierces a little, so it drills lines and chews through shields and bosses.
 */
private fun World.fireHelix(muzzleY: Float, off: Float) {
    val dmg = 2 + powerLevel / 2 + odBonus()          // 2..7 (+OD)
    val pierce = 1 + powerLevel / 3                    // 1..4
    val amp = player.width * (0.8f + powerLevel * 0.05f)
    val freq = 17f
    val vy = -BULLET_SPEED * 1.1f
    fun strand(phase: Float) = shoot(player.x, muzzleY, vy).also {
        it.damage = dmg; it.pierce = pierce; it.kind = 4
        it.wob = phase; it.wobAmp = amp; it.wobFreq = freq
    }
    strand(0f); strand(3.14159f)                      // the core double strand
    if (powerLevel >= 6) { strand(1.5708f); strand(4.7124f) }   // a denser quad weave at high power
}

/** Each escort drone adds a straight bolt per volley (two when surging), scaling firepower with the squad. */
private fun World.fireDrones() {
    if (drones.isEmpty()) return
    val dmg = 1 + powerLevel / 4 + odBonus()          // 1..3 (+OD)
    val vy = -BULLET_SPEED * 1.05f
    for (d in drones) {
        shoot(d.x, d.y - player.height * 0.4f, vy).damage = dmg
        if (overdriveTime > 0f) {                      // a second angled bolt during Overdrive
            shoot(d.x, d.y - player.height * 0.4f, vy, BULLET_SPEED * 0.12f).damage = dmg
        }
    }
}

internal fun World.updateBullets(dt: Float) {
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
private fun World.steerHoming(bl: Bullet, dt: Float) {
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
internal fun World.updateDrones(dt: Float) {
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

internal fun World.addOverdrive(amount: Float) {
    if (overdriveTime > 0f) return                    // can't recharge mid-surge
    overdrive += amount
    if (overdrive >= OVERDRIVE_MAX) triggerOverdrive()
}

/** The gauge is full: unleash a few seconds of doubled fire-rate and harder-hitting shots. */
private fun World.triggerOverdrive() {
    overdrive = 0f
    overdriveTime = OVERDRIVE_DURATION
    showBanner("OVERDRIVE", 1.0f)
    bombFlash = 0.18f
    shake(0.25f, minDim * 0.02f)
    vibrate?.invoke(50)
}

private fun World.gainDrone(x: Float, y: Float) {
    if (droneCount >= MAX_DRONES) { addScore(750); spawnPopup(x, y, "+750", Palette.WEAPON_COLORS[0]); return }
    droneCount++
    drones.add(Drone(player.x, player.y + player.height))
    spawnPopup(x, y, "DRONE x$droneCount", Palette.WEAPON_COLORS[0])
    vibrate?.invoke(35)
}

// =====================================================================
//  Power-ups
// =====================================================================
internal fun World.maybeDrop(x: Float, y: Float) {
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

internal fun World.dropPowerUp(x: Float, y: Float, type: PowerType) {
    powerUps.add(PowerUp(x, y, POWERUP_SPEED, type, minDim * POWERUP_RADIUS_FRACTION))
}

internal fun World.updatePowerUps(dt: Float) {
    for (p in powerUps) {
        p.wobble += dt * 4f          // drives the draw-time pulse only
        p.y += p.vy * dt
        if (p.y - p.radius > h) p.alive = false
    }
}

internal fun World.applyPowerUp(p: PowerUp) {
    when (p.type) {
        PowerType.POWER -> {
            if (powerLevel < MAX_POWER) { powerLevel++; spawnPopup(p.x, p.y, "POWER UP", Palette.POWERUP_COLORS[0]) }
            else { addScore(500); spawnPopup(p.x, p.y, "+500", Palette.POWERUP_COLORS[0]) }
        }
        PowerType.SHIELD -> { shield = (shield + 1).coerceAtMost(2); spawnPopup(p.x, p.y, "SHIELD", Palette.POWERUP_COLORS[1]) }
        PowerType.BOMB -> { bombs = (bombs + 1).coerceAtMost(MAX_BOMBS); spawnPopup(p.x, p.y, "BOMB", Palette.POWERUP_COLORS[2]) }
        PowerType.WEAPON -> cycleWeapon(p.x, p.y)
        PowerType.DRONE -> gainDrone(p.x, p.y)
        PowerType.LIFE -> gainLife(p.x, p.y)
    }
    vibrate?.invoke(25)
    explode(p.x, p.y, p.radius)
}

private fun World.cycleWeapon(x: Float, y: Float) {
    weapon = when (weapon) {
        WeaponType.VULCAN -> WeaponType.LASER
        WeaponType.LASER -> WeaponType.WIDE
        WeaponType.WIDE -> WeaponType.HOMING
        WeaponType.HOMING -> WeaponType.HELIX
        WeaponType.HELIX -> WeaponType.VULCAN
    }
    spawnPopup(x, y, Palette.WEAPON_NAMES[weapon.ordinal], Palette.POWERUP_COLORS[3])
}

private fun World.gainLife(x: Float, y: Float) {
    if (lives < MAX_LIVES) lives++ else addScore(1000)
    spawnPopup(x, y, "1UP", Palette.CYAN)
    vibrate?.invoke(40)
}
