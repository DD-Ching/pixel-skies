package com.example.wearshooter

/**
 * Plain mutable data holders for every moving thing in the game.
 * Kept dependency-free so they are trivial to tune, test, or swap for sprites.
 */

enum class EnemyType { GRUNT, WEAVER, RUSHER, GUNNER, TANK, SPLITTER, ORBITER }
enum class PowerType { POWER, SHIELD, BOMB, WEAPON, LIFE }
enum class WeaponType { VULCAN, LASER, WIDE, HOMING }

class Player {
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f
    var alive = true
}

/** A player shot. vy is negative (up); vx allows angled shots at high power. */
class Bullet(var x: Float, var y: Float, var vy: Float, var vx: Float = 0f) {
    var alive = true
    var damage = 1
    var pierce = 0       // extra enemies it can pass through before dying (laser)
    var kind = 0         // 0 = vulcan, 1 = laser, 2 = wide, 3 = homing  (drawing only)
    var homing = false   // steers toward the nearest enemy / boss
    var speed = 0f       // travel speed kept constant while homing
    var life = 0f        // seconds before a homing missile fizzles (0 = unlimited)
}

/** A descending enemy. Behaviour depends on [type]; tougher types have more hp. */
class Enemy(var x: Float, var y: Float, var radius: Float, var type: EnemyType) {
    var vx = 0f
    var vy = 0f
    var hp = 1
    var maxHp = 1
    var alive = true
    var baseX = x        // centre any sway around the spawn column
    var swayAmp = 0f
    var wobble = 0f
    var fireTimer = 0f   // counts down to the next shot (gunner / tank / orbiter)
    var hitFlash = 0f    // seconds of white flash after being hit but not killed
    var scoreValue = 1   // multiplied by the base score on death
    var orbitAng = 0f    // ORBITER: angle around its descending centre column
}

/** An enemy projectile — the thing the player has to dodge. */
class EnemyBullet(var x: Float, var y: Float, var vx: Float, var vy: Float, var radius: Float) {
    var alive = true
    var grazed = false   // already counted as a near-miss, so it won't re-award
}

/** A floating reward dropped by enemies / bosses. */
class PowerUp(var x: Float, var y: Float, var vy: Float, var type: PowerType, var radius: Float) {
    var alive = true
    var wobble = 0f
}

/** A periodic boss with a health bar and bullet patterns. */
class Boss(var x: Float, var y: Float, var radius: Float, hp: Int) {
    var maxHp = hp
    var hp = hp
    var alive = true
    var entering = true   // sliding in from the top
    var phase = 0f        // pattern timer
    var fireTimer = 0f
    var moveDir = 1
    var hitFlash = 0f
    var variant = 0       // 0..3 = the four rotating bosses, 4 = OVERLORD (every 5th)
    var spin = 0f         // accumulates for rotating spoke / spiral patterns
    var enraged = false   // HP dropped below the enrage threshold → faster, denser
    var isFinal = false   // the OVERLORD mega-boss with HP-gated phases
    var name = ""         // shown under the WARNING banner
}

/** A short-lived floating bit of HUD text (score gains, GRAZE, 1UP, …). */
class Popup(var x: Float, var y: Float, var text: String, var color: Int) {
    var life = 0.8f
    var maxLife = 0.8f
}

/** Short-lived explosion fleck. life counts down from maxLife to 0. */
class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float) {
    var life = 0f
    var maxLife = 0f
    var size = 0f
    var color = 0
}
