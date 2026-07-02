package com.example.wearshooter

/**
 * Plain mutable data holders for every moving thing in the game.
 * Kept dependency-free so they are trivial to tune, test, or swap for sprites.
 *
 * High-churn types (Bullet, EnemyBullet, Enemy, Particle) are pooled: they have a
 * bare constructor plus a reset() that reinitialises EVERY field, so a recycled
 * instance can never leak state from its previous life.
 */

// New types are appended LAST so existing ordinals (used by the spawn-weight array) stay put.
// DARTER  = streaks in horizontally from a side edge, taking aimed pot-shots as it crosses.
// MINE    = slow drifting hazard; bursts into a (dodgeable) ring when shot or if it slips past you.
// DIVER   = cruises in, then locks on and lunges straight at where you were — sidestep it.
enum class EnemyType { GRUNT, WEAVER, RUSHER, GUNNER, TANK, SPLITTER, ORBITER, SHIELDER, DARTER, MINE, DIVER }
enum class PowerType { POWER, SHIELD, BOMB, WEAPON, LIFE, DRONE }
enum class WeaponType { VULCAN, LASER, WIDE, HOMING, HELIX }

class Player {
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f
    var alive = true
}

/** A player shot. vy is negative (up); vx allows angled shots at high power. */
class Bullet : Poolable {
    var x = 0f
    var y = 0f
    var vy = 0f
    var vx = 0f
    override var alive = true
    var damage = 1
    var pierce = 0       // extra enemies it can pass through before dying (laser / helix)
    var kind = 0         // 0 = vulcan, 1 = laser, 2 = wide, 3 = homing, 4 = helix  (drawing only)
    var homing = false   // steers toward the nearest enemy / boss
    var speed = 0f       // travel speed kept constant while homing
    var life = 0f        // seconds before a homing missile fizzles (0 = unlimited)
    var wob = 0f         // HELIX: phase of the side-to-side weave
    var wobAmp = 0f      // HELIX: lateral weave amplitude in px (0 = straight)
    var wobFreq = 0f     // HELIX: weave speed

    fun reset(x: Float, y: Float, vy: Float, vx: Float = 0f): Bullet {
        this.x = x; this.y = y; this.vy = vy; this.vx = vx
        alive = true; damage = 1; pierce = 0; kind = 0
        homing = false; speed = 0f; life = 0f
        wob = 0f; wobAmp = 0f; wobFreq = 0f
        return this
    }
}

/** An option drone (僚機): a small escort that trails the jet and fires alongside it. */
class Drone(var x: Float, var y: Float)

/** A descending enemy. Behaviour depends on [type]; tougher types have more hp. */
class Enemy : Poolable {
    var x = 0f
    var y = 0f
    var radius = 0f
    var type = EnemyType.GRUNT
    var vx = 0f
    var vy = 0f
    var hp = 1
    var maxHp = 1
    override var alive = true
    var baseX = 0f       // centre any sway around the spawn column
    var swayAmp = 0f
    var wobble = 0f
    var fireTimer = 0f   // counts down to the next shot (gunner / tank / orbiter)
    var hitFlash = 0f    // seconds of white flash after being hit but not killed
    var scoreValue = 1   // multiplied by the base score on death
    var orbitAng = 0f    // ORBITER: angle around its descending centre column
    var shieldHp = 0     // SHIELDER: frontal shield soak; the core (hp) is only hurt once this hits 0
    var maxShieldHp = 0  // for the shield gauge readout
    var diving = false   // DIVER: false = cruising/telegraphing, true = committed to its lunge

    fun reset(x: Float, y: Float, radius: Float, type: EnemyType): Enemy {
        this.x = x; this.y = y; this.radius = radius; this.type = type
        vx = 0f; vy = 0f; hp = 1; maxHp = 1; alive = true
        baseX = x; swayAmp = 0f; wobble = 0f; fireTimer = 0f; hitFlash = 0f
        scoreValue = 1; orbitAng = 0f; shieldHp = 0; maxShieldHp = 0; diving = false
        return this
    }
}

/** An enemy projectile — the thing the player has to dodge. */
class EnemyBullet : Poolable {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var radius = 0f
    override var alive = true
    var grazed = false   // already counted as a near-miss, so it won't re-award

    fun reset(x: Float, y: Float, vx: Float, vy: Float, radius: Float): EnemyBullet {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.radius = radius
        alive = true; grazed = false
        return this
    }
}

/**
 * A full-height vertical beam that the player dodges by moving left/right.
 * It telegraphs first (warnTime, no hitbox) then becomes lethal (fireTime). [vx] lets it
 * sweep sideways so the one safe lane keeps moving and the player must keep repositioning.
 */
class Laser(var x: Float, var halfW: Float) : Poolable {
    var warnTime = 0f    // counts down; while > 0 it's only a warning line (no hitbox)
    var maxWarn = 0f
    var fireTime = 0f    // counts down; while > 0 (and warn done) the beam is lethal
    var maxFire = 0f
    var vx = 0f          // sideways sweep speed
    override var alive = true
}

/** A floating reward dropped by enemies / bosses. */
class PowerUp(var x: Float, var y: Float, var vy: Float, var type: PowerType, var radius: Float) : Poolable {
    override var alive = true
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
    var variant = 0       // 0..4 = the five rotating bosses, 5 = OVERLORD (every 6th)
    var spin = 0f         // accumulates for rotating spoke / spiral patterns
    var enraged = false   // HP dropped below the enrage threshold → faster, denser
    var isFinal = false   // the OVERLORD mega-boss with HP-gated phases
    var colossal = false  // giant boss anchored at the top — only its lower hull shows (巨物感)
    var name = ""         // shown under the WARNING banner
    var invulnTime = 0f   // raises a barrier (no damage) while it commits to a laser attack
}

/** A short-lived floating bit of HUD text (score gains, GRAZE, 1UP, …). */
class Popup(var x: Float, var y: Float, var text: String, var color: Int) : Poolable {
    var life = 0.8f
    var maxLife = 0.8f
    override var alive = true
}

/** An expanding ring drawn when a boss looses a circular burst — telegraph plus flair. */
class Shockwave(var x: Float, var y: Float, var maxR: Float, var color: Int) : Poolable {
    var life = 0f
    var maxLife = 0f
    override var alive = true
}

/** Short-lived explosion fleck. life counts down from maxLife to 0. */
class Particle : Poolable {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var life = 0f
    var maxLife = 0f
    var size = 0f
    var color = 0
    override var alive = true

    fun reset(x: Float, y: Float, vx: Float, vy: Float): Particle {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy
        life = 0f; maxLife = 0f; size = 0f; color = 0; alive = true
        return this
    }
}

/** A background star. Fixed population, recycled by wrapping — never reallocated. */
internal class Star(var x: Float, var y: Float, var speed: Float, var size: Float, var bright: Int)
