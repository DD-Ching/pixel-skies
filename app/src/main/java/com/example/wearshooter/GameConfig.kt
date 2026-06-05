package com.example.wearshooter

/**
 * All gameplay tuning lives here so balancing never means hunting through logic.
 * Sizes expressed as a "fraction" are multiplied by min(screenWidth, screenHeight),
 * so the game scales to any round Wear OS display (Pixel Watch is 384x384).
 */
object GameConfig {

    // ---- Player ----
    const val PLAYER_Y_FRACTION = 0.80f      // 0 = top, 1 = bottom. Keeps the jet low.
    const val PLAYER_SIZE_FRACTION = 0.090f  // jet width relative to screen
    const val EDGE_MARGIN_FRACTION = 0.045f  // keep the jet this far from the round edge
    const val START_LIVES = 3
    const val MAX_LIVES = 6
    const val START_BOMBS = 2
    const val MAX_BOMBS = 5
    const val MAX_POWER = 8                   // weapon level cap (firepower scales by formula)
    const val INVULN_TIME = 1.6f              // i-frames after taking a hit (seconds)
    const val EXTRA_LIFE_EVERY = 15000        // free life each time score crosses a multiple

    // ---- Rotary crown ----
    const val ROTARY_SENSITIVITY = 52f       // px the jet moves per unit of AXIS_SCROLL
    const val ROTARY_INVERT = false          // flip if clockwise moves the jet LEFT
    const val KEY_SPEED = 540f               // arrow-key fallback speed (emulator testing)

    // ---- Player bullets ----
    const val BULLET_SPEED = 660f            // px/sec, travelling up
    const val BULLET_W_FRACTION = 0.013f
    const val BULLET_H_FRACTION = 0.048f
    const val FIRE_INTERVAL = 0.22f          // seconds between volleys
    const val GUN_SPREAD = 0.26f             // horizontal barrel offset (fraction of player width)

    // ---- Enemy bullets (the thing you dodge) ----
    const val EBULLET_RADIUS_FRACTION = 0.018f
    const val EBULLET_SPEED = 150f           // base downward/aimed speed
    const val EBULLET_MAX = 150              // hard cap so a boss spread can't flood the watch

    // ---- Enemies ----
    const val ENEMY_MAX = 28
    const val ENEMY_RADIUS_FRACTION = 0.050f
    const val ENEMY_SPEED_BASE = 80f
    const val GUNNER_FIRE_INTERVAL = 1.7f    // seconds; scaled down as difficulty rises
    const val TANK_FIRE_INTERVAL = 2.4f
    const val ENEMY_HP_SCALE = 0.45f         // enemy HP grows by this × (difficulty-1)

    // ---- Difficulty / pacing ----
    const val STAGE_DURATION = 20f           // seconds per stage (drives the STAGE banner)
    const val SPAWN_INTERVAL_START = 1.05f
    const val SPAWN_INTERVAL_MIN = 0.32f
    const val BOSS_INTERVAL = 38f            // seconds of normal play between bosses

    // ---- Boss ----
    const val BOSS_RADIUS_FRACTION = 0.17f
    const val BOSS_BASE_HP = 60              // grows each time a boss is beaten

    // ---- Power-ups ----
    const val POWERUP_DROP_CHANCE = 0.12f    // chance a normal kill drops something
    const val POWERUP_SPEED = 95f
    const val POWERUP_RADIUS_FRACTION = 0.035f

    // ---- Scoring ----
    const val SCORE_PER_KILL = 100           // base, multiplied by enemy value & combo
    const val COMBO_WINDOW = 1.6f            // seconds to keep a chain alive
    const val MAX_MULTIPLIER = 8

    // ---- Graze (skim an enemy bullet for points) ----
    const val GRAZE_BAND_FRACTION = 0.05f    // ring just outside the hitbox that counts as a graze
    const val GRAZE_BONUS = 30               // points per near-miss

    // ---- FX ----
    const val PARTICLES_PER_KILL = 14
    const val STAR_COUNT = 40
}
