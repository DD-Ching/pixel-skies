package com.example.wearshooter

import com.example.wearshooter.GameConfig.PARTICLES_PER_KILL
import com.example.wearshooter.GameConfig.STAR_COUNT
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Juice: particles, shockwaves, floating score popups, screen shake, banners,
 * and the parallax starfield. Everything here is cosmetic — nothing has a hitbox.
 */

internal fun World.explode(x: Float, y: Float, baseSize: Float) {
    repeat(PARTICLES_PER_KILL) {
        val ang = Random.nextFloat() * 6.283f
        val spd = 40f + Random.nextFloat() * 200f
        val p = particlePool.obtain().reset(x, y, cos(ang) * spd, sin(ang) * spd)
        p.maxLife = 0.35f + Random.nextFloat() * 0.45f
        p.life = p.maxLife
        p.size = baseSize * 0.22f * (0.6f + Random.nextFloat() * 0.8f)
        p.color = Palette.EXPLOSION_COLORS[Random.nextInt(Palette.EXPLOSION_COLORS.size)]
        particles.add(p)
    }
}

/** Integrates its own sweep: dead flecks go straight back to the pool. */
internal fun World.updateParticles(dt: Float) {
    var i = particles.size - 1
    while (i >= 0) {
        val p = particles[i]
        p.x += p.vx * dt; p.y += p.vy * dt
        p.vx *= 0.90f; p.vy = p.vy * 0.90f + 70f * dt
        p.life -= dt
        if (p.life <= 0f) {
            val last = particles.size - 1
            particles[i] = particles[last]
            particles.removeAt(last)
            particlePool.release(p)
        }
        i--
    }
}

internal fun World.shake(time: Float, mag: Float) {
    if (time > shakeTime) { shakeTime = time; shakeMag = mag }
}

internal fun World.showBanner(text: String, time: Float) {
    eventBanner = text; eventBannerTime = time
}

internal fun World.spawnShockwave(x: Float, y: Float, r: Float, color: Int) {
    if (shockwaves.size >= 12) return
    shockwaves.add(Shockwave(x, y, (r * 2.6f).coerceAtLeast(minDim * 0.4f), color).also {
        it.maxLife = 0.5f; it.life = 0.5f
    })
}

internal fun World.updateShockwaves(dt: Float) {
    var i = shockwaves.size - 1
    while (i >= 0) {
        val s = shockwaves[i]
        s.life -= dt
        if (s.life <= 0f) {
            val last = shockwaves.size - 1
            shockwaves[i] = shockwaves[last]
            shockwaves.removeAt(last)
        }
        i--
    }
}

internal fun World.spawnPopup(x: Float, y: Float, text: String, color: Int) {
    if (popups.size > 18) popups.removeAt(0)
    popups.add(Popup(x, y, text, color))
}

internal fun World.updatePopups(dt: Float) {
    var i = popups.size - 1
    while (i >= 0) {
        val p = popups[i]
        p.y -= minDim * 0.35f * dt          // drift upward
        p.life -= dt
        if (p.life <= 0f) popups.removeAt(i)   // keep insertion order: the 18-cap drops oldest first
        i--
    }
}

// =====================================================================
//  Starfield
// =====================================================================
internal fun World.initStars() {
    stars.clear()
    repeat(STAR_COUNT) { stars.add(makeStar(Random.nextFloat() * h)) }
}

private fun World.makeStar(y: Float): Star {
    val depth = Random.nextFloat()
    return Star(Random.nextFloat() * w, y, 18f + depth * 90f,
        minDim * (0.004f + depth * 0.010f), (70 + depth * 150).toInt())
}

internal fun World.updateStars(dt: Float, moving: Boolean) {
    val f = if (moving) 1f else 0.35f
    for (s in stars) {
        s.y += s.speed * dt * f
        if (s.y - s.size > h) { s.y = -s.size; s.x = Random.nextFloat() * w }
    }
}
