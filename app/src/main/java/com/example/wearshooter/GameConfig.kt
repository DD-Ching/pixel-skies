package com.example.wearshooter

/**
 * All gameplay tuning lives here so balancing never means hunting through logic.
 * Sizes expressed as a "fraction" are multiplied by min(screenWidth, screenHeight),
 * so the game scales to any round Wear OS display (Pixel Watch is 384x384).
 */
object GameConfig {

    // ---- Player ----
    const val PLAYER_Y_FRACTION = 0.80f      // 0 = top, 1 = bottom. Keeps the jet low.
    const val BOSS_PLAYER_Y_FRACTION = 0.72f // the jet rises during boss fights: wider lane + more reaction time
    const val PLAYER_SIZE_FRACTION = 0.090f  // jet width relative to screen
    const val EDGE_MARGIN_FRACTION = 0.045f  // keep the jet this far from the round edge
    const val START_LIVES = 3
    const val MAX_LIVES = 6
    const val START_BOMBS = 2
    const val MAX_BOMBS = 5
    const val MAX_POWER = 10                  // weapon level cap (firepower scales by formula)
    const val INVULN_TIME = 1.6f              // i-frames after taking a hit (seconds)
    const val EXTRA_LIFE_EVERY = 15000        // free life each time score crosses a multiple

    // ---- Option drones (僚機 — companion ships that trail you and add firepower) ----
    const val MAX_DRONES = 3                  // up to three escorts flanking the jet
    const val DRONE_FOLLOW = 9f               // higher = snappier follow toward formation slot
    const val DRONE_RADIUS_FRACTION = 0.030f

    // ---- Overdrive (auto-charging power surge — the late-game spike) ----
    const val OVERDRIVE_MAX = 100f            // gauge cap
    const val OVERDRIVE_PER_KILL = 5.5f       // gauge gained per enemy killed
    const val OVERDRIVE_PER_GRAZE = 1.3f      // and per bullet grazed
    const val OVERDRIVE_DURATION = 5.0f       // seconds of surge once it triggers
    const val OVERDRIVE_FIRE_MULT = 0.5f      // fire interval × this while surging (twice as fast)

    // ---- Rotary crown ----
    const val ROTARY_SENSITIVITY = 52f       // px the jet moves per unit of AXIS_SCROLL
    const val ROTARY_INVERT = false          // flip if clockwise moves the jet LEFT
    const val KEY_SPEED = 540f               // arrow-key fallback speed (emulator testing)

    // ---- Player bullets ----
    const val BULLET_SPEED = 660f            // px/sec, travelling up
    const val BULLET_W_FRACTION = 0.013f
    const val BULLET_H_FRACTION = 0.048f
    // Fire cadence is per-weapon (lower = faster) and ALSO speeds up as your power level climbs.
    const val FIRE_INTERVAL_VULCAN = 0.155f  // rapid all-rounder
    const val FIRE_INTERVAL_LASER = 0.26f    // slower, but each bolt is heavy & piercing
    const val FIRE_INTERVAL_WIDE = 0.20f     // brisk wall of pellets
    const val FIRE_INTERVAL_HOMING = 0.30f   // deliberate — the missiles do the work
    const val FIRE_INTERVAL_HELIX = 0.18f    // fast weaving drill
    const val FIRE_RATE_POWER_STEP = 0.045f  // each power level shaves this fraction off the interval
    const val FIRE_RATE_POWER_MIN = 0.55f    // floor so fire rate can't run away completely
    const val GUN_SPREAD = 0.26f             // horizontal barrel offset (fraction of player width)

    // ---- Enemy bullets (the thing you dodge) ----
    const val EBULLET_RADIUS_FRACTION = 0.018f
    const val EBULLET_SPEED = 150f           // base downward/aimed speed
    const val EBULLET_MAX = 220              // hard cap; raised for the hardcore bullet-hell curtains

    // ---- Enemies ----
    const val ENEMY_MAX = 30
    const val ENEMY_RADIUS_FRACTION = 0.050f
    const val ENEMY_SPEED_BASE = 80f
    const val GUNNER_FIRE_INTERVAL = 1.7f    // seconds; scaled down as difficulty rises
    const val TANK_FIRE_INTERVAL = 2.4f
    const val ENEMY_HP_SCALE = 0.45f         // enemy HP grows by this × (difficulty-1)

    // ---- Shielder (armoured blocker) ----
    const val SHIELDER_SHIELD_HP = 12        // frontal shield you must break before the core is exposed
    const val SHIELDER_BODY_HP = 3           // soft core once the shield is down
    const val SHIELDER_TRACK_SPEED = 75f     // px/s it slides sideways to stay in front of you

    // ---- Lasers (telegraphed sweeping beams — the dodge-or-die set piece) ----
    const val LASER_MAX = 14                 // hard cap on simultaneous beams
    const val LASER_WARN = 0.95f             // telegraph time (no hitbox) — fair reaction window
    const val LASER_FIRE = 1.25f             // how long the lethal beam stays on
    const val LASER_HALFW_FRACTION = 0.030f  // beam half-width (× minDim)
    const val LASER_SWEEP_SPEED = 62f        // px/s the comb drifts so the safe lane keeps moving

    // ---- Progressive unlocks (introduce one enemy type at a time, like a well-paced game) ----
    const val UNLOCK_RAMP = 12f              // seconds a freshly-introduced enemy eases from rare → common
    const val UNLOCK_BOSS_BONUS = 12f        // each boss beaten counts as this many seconds of progression

    // ---- Difficulty / pacing ----
    const val STAGE_DURATION = 20f           // seconds per stage (drives the STAGE banner)
    const val SPAWN_INTERVAL_START = 1.05f
    const val SPAWN_INTERVAL_MIN = 0.28f
    const val BOSS_INTERVAL = 38f            // seconds between bosses; shrinks as you beat more
    const val BOSS_INTERVAL_MIN = 22f        // bosses come this often at the late game
    const val DIFFICULTY_CAP = 9f            // ceiling so stats can't run away (was 8)

    // ---- Boss ----
    const val BOSS_RADIUS_FRACTION = 0.17f
    const val BOSS_BASE_HP = 110             // grows each time a boss is beaten (raised so they stop melting)
    const val BOSS_HP_PER_KILL = 50          // HP added per boss already beaten (player scales up too now)
    const val ENRAGE_HP_FRACTION = 0.35f     // below this fraction the boss fires faster & denser
    const val RING_GAP_HALF = 0.62f          // half-width (radians) of the guaranteed downward escape lane
                                             // carved into every circular burst — a 1-D mover must be able to slip through
    const val FINAL_BOSS_EVERY = 6           // every Nth boss is the colossal multi-phase OVERLORD
    const val FINAL_BOSS_HP_MULT = 2.6f      // OVERLORD is this much tougher than a normal boss
    const val COLOSSUS_RADIUS_FRACTION = 0.62f // OVERLORD body radius — far bigger than the screen, so only
                                               // its lower hull shows: a true "巨物感" giant looming overhead
    const val BOSS_INTENSITY_PER_KILL = 0.12f // bullet-density ramp per boss beaten (endless)
    const val BOSS_INTENSITY_MAX = 2.2f

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

    // ---- Backdrops (scenery that cycles as you progress: space → nebula → station → star → planet) ----
    const val BACKDROP_COUNT = 5             // number of themed scenes
    const val BACKDROP_STAGES_EACH = 3       // advance to the next scene every N stages
    const val BACKDROP_FADE = 1.6f           // seconds to crossfade between scenes

    // ---- FX ----
    const val PARTICLES_PER_KILL = 14
    const val STAR_COUNT = 40
}
