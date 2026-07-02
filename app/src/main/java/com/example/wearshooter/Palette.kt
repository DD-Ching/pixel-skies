package com.example.wearshooter

/**
 * Every colour and display name in the game, in one place — indexed by enum ordinal
 * so the render path never allocates a Pair/Triple just to look up a palette.
 *
 * Aesthetic stance: retro neon arcade on a deep-navy space base (not pure black —
 * the tinted base reads as *designed* on an OLED watch while still costing almost
 * nothing in power). One dominant hue per element family, hot white cores.
 */
internal object Palette {

    // ---- Base ----
    val SPACE = 0xFF060A16.toInt()          // deep navy base; never pure black
    val SAFE_RING = 0x22FFFFFF              // faint ring marking the round safe area
    val SCRIM = 0xAA000000.toInt()          // pause / game-over dim
    val WHITE = 0xFFFFFFFF.toInt()

    // ---- Player ----
    val JET_WING = 0xFF2FA9D6.toInt()
    val JET_BODY = 0xFF6FE3FF.toInt()
    val JET_FLAME = 0xFFFFC24B.toInt()
    val SHIELD_RING = 0xCC66E0FF.toInt()
    val SHIELD_RING_DIM = 0x6666E0FF

    // ---- Player bullets, indexed by Bullet.kind (0 vulcan, 1 laser, 2 wide, 3 homing, 4 helix) ----
    val BULLET_BODY = intArrayOf(
        0xFF8CF6FF.toInt(), 0xFFFF6BE3.toInt(), 0xFFFFB23D.toInt(), 0xFF8AFF6B.toInt(), 0xFFB9A6FF.toInt()
    )
    val HOMING_TRAIL = 0x668AFF6B
    val WIDE_CORE = 0xFFFFF0C0.toInt()

    // ---- Enemy bullets ----
    val EBULLET = 0xFFFF3B30.toInt()        // bright red — must read instantly among friendly fire
    val EBULLET_CORE = 0xFFFFE08A.toInt()

    // ---- Enemies, indexed by EnemyType.ordinal ----
    val ENEMY_BODY = intArrayOf(
        0xFFFF4D8D.toInt(),  // GRUNT
        0xFFB36BFF.toInt(),  // WEAVER
        0xFFFF7A3D.toInt(),  // RUSHER
        0xFF35D07F.toInt(),  // GUNNER
        0xFF6E7BFF.toInt(),  // TANK
        0xFFE8E84A.toInt(),  // SPLITTER  toxic yellow
        0xFF35E0D0.toInt(),  // ORBITER   teal drone
        0xFF8794A8.toInt(),  // SHIELDER  steel fortress
        0xFFFF5AD0.toInt(),  // DARTER    magenta streaker
        0xFF6E7A3A.toInt(),  // MINE      olive shell
        0xFFE0364E.toInt()   // DIVER     crimson dart
    )
    val ENEMY_ACCENT = intArrayOf(
        0xFFFFC24B.toInt(), 0xFFE0B3FF.toInt(), 0xFFFFE08A.toInt(), 0xFFCFFF8A.toInt(),
        0xFF9AE0FF.toInt(), 0xFFFF9E3D.toInt(), 0xFFB3FFF4.toInt(), 0xFFC8D6E8.toInt(),
        0xFFFFD0F5.toInt(), 0xFFFF5030.toInt(), 0xFFFFD2D2.toInt()
    )
    val ENEMY_DARK = 0xFF120712.toInt()     // canopy / seam ink
    val ENEMY_NAMES = arrayOf(
        "GRUNT", "WEAVER", "RUSHER", "GUNNER", "TANK",
        "SPLITTER", "ORBITER", "SHIELDER", "DARTER", "MINE", "DIVER"
    )

    // ---- Bosses, indexed by variant (0..4 rotating, 5 OVERLORD) ----
    val BOSS_BODY = intArrayOf(
        0xFF8A3DFF.toInt(), 0xFFE23B5A.toInt(), 0xFF2FBF71.toInt(),
        0xFF3D7BFF.toInt(), 0xFF6A1F8F.toInt(), 0xFFFFC24B.toInt()
    )
    val BOSS_EYE = intArrayOf(
        0xFFFF3B30.toInt(), 0xFFFFE08A.toInt(), 0xFFFFF36B.toInt(),
        0xFF9AE0FF.toInt(), 0xFFE36BFF.toInt(), 0xFFFF3B30.toInt()
    )
    val BOSS_FOOT = intArrayOf(
        0xFFFFC24B.toInt(), 0xFF7A1830.toInt(), 0xFF146C43.toInt(),
        0xFF1B3A8A.toInt(), 0xFF2A0A3A.toInt(), 0xFF8A5A12.toInt()
    )
    val BOSS_NAMES = arrayOf("WARDEN", "REAVER", "SEER", "STORMCALLER", "VOIDMAW", "OVERLORD")
    val BOSS_HP_BACK = 0xFF3A0A12.toInt()
    val BOSS_HP = 0xFFFF3B30.toInt()
    val BOSS_HP_FINAL = 0xFFFFC24B.toInt()

    // ---- Weapons, indexed by WeaponType.ordinal ----
    val WEAPON_COLORS = intArrayOf(
        0xFF8CF6FF.toInt(), 0xFFFF6BE3.toInt(), 0xFFFFB23D.toInt(), 0xFF8AFF6B.toInt(), 0xFFB9A6FF.toInt()
    )
    val WEAPON_NAMES = arrayOf("VULCAN", "LASER", "WIDE", "HOMING", "HELIX")

    // ---- Power-ups, indexed by PowerType.ordinal (POWER, SHIELD, BOMB, WEAPON, LIFE, DRONE) ----
    val POWERUP_COLORS = intArrayOf(
        0xFF35D07F.toInt(), 0xFF66E0FF.toInt(), 0xFFFFC24B.toInt(),
        0xFFFF7AE0.toInt(), 0xFF6FE3FF.toInt(), 0xFF8CF6FF.toInt()
    )
    val POWERUP_LABELS = arrayOf("P", "S", "B", "W", "1", "D")
    val POWERUP_INK = 0xFF06121A.toInt()

    // ---- HUD ----
    val SCORE = WHITE
    val MULTIPLIER = 0xFFFFF36B.toInt()
    val LIFE_PIP = 0xFF6FE3FF.toInt()
    val BOMB_PIP = 0xFFFFC24B.toInt()
    val OD_TRACK = 0x33FFFFFF
    val OD_FILL = 0xFF35C8E0.toInt()
    val OD_FULL = 0xFF66E0FF.toInt()
    val BANNER_INFO_R = 111; val BANNER_INFO_G = 227; val BANNER_INFO_B = 255
    val HINT = 0x99FFFFFF.toInt()
    val GOLD = 0xFFFFC24B.toInt()
    val PINK = 0xFFFF4D8D.toInt()
    val CYAN = 0xFF6FE3FF.toInt()
    val TITLE_GHOST = 0xFFFF4D8D.toInt()    // chromatic offset behind the title
    val PAUSE_SUB = 0xFFB0B8C4.toInt()

    // ---- Lasers ----
    val LASER_WARN_R = 255; val LASER_WARN_G = 70; val LASER_WARN_B = 70
    val LASER_IMMINENT_R = 255; val LASER_IMMINENT_G = 210; val LASER_IMMINENT_B = 120

    // ---- FX ----
    val EXPLOSION_COLORS = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFFFF36B.toInt(),
        0xFFFFC24B.toInt(), 0xFFFF7A3D.toInt(), 0xFFFF4D8D.toInt()
    )
    val SHOCK_PINK = 0xFFFF6BD0.toInt()
    val SHOCK_CYAN = 0xFF6BE0FF.toInt()
}
