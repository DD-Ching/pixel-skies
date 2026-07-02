# Pixel Skies — a retro vertical arcade shooter for Wear OS

A tiny, original pixel-art shoot-’em-up built for the Pixel Watch / Wear OS round
display. You pilot a small fighter near the bottom of the screen, it auto-fires
upward, and waves of enemies pour down — **shooting back**. Steer with the
**rotary crown** to dodge bullets and line up your shots.

Inspired by the *feel* of classic vertical shooters (1942 / Galaga / bullet-hell)
— **no copyrighted assets, characters, names, music, or designs** are used. Every
shape is drawn at runtime from primitives.

---

## Gameplay & mechanics

Because the watch only gives you **one axis (left/right)**, the challenge is built
around horizontal dodging:

- **Enemy bullets — the core challenge.** Gunners and the boss fire *aimed* shots
  and fans/rings at you (bright red, always readable). You must slide out of the
  way. *Standing still gets you killed.*
- **5 enemy types**, introduced as difficulty rises:
  | Type | Colour | Behaviour | HP |
  |---|---|---|---|
  | Grunt | pink | falls straight | 1 |
  | Weaver | purple | wide sine sway | 1 |
  | Rusher | orange | fast dive | 1 |
  | Gunner | green | aims & shoots | 3 |
  | Tank | blue | slow, fires fans | 7 |
  Multi-hit enemies show a tiny HP bar.
- **Lives (3) + i-frames.** A hit costs a life and gives ~1.6 s of blinking
  invulnerability (and clears nearby bullets so you don’t chain-die). 0 lives = game over.
- **Three weapon types.** Pick up **W** to cycle your gun:
  - **Vulcan** (cyan) — balanced straight shots.
  - **Laser** (magenta) — fast, **piercing** beams; melts lines of enemies and the boss.
  - **Wide** (orange) — a broad pellet spread that blankets the swarm.
- **Weapon power level (1→8), formula-scaled.** Collect green **P** drops to strengthen
  the current weapon — and it keeps growing: once stream/pellet count caps it converts
  to extra damage / pierce / angled shots. Taking a hit drops you one level.
- **Enemies scale to match.** Enemy HP grows continuously with difficulty (HP bars show
  it), difficulty is *coupled to your power level*, and gunners/tanks fire denser
  patterns as it climbs — so firepower and threat rise together. Difficulty is capped so
  it can't run away.
- **Graze.** Skim an enemy bullet (near-miss) for bonus points and to keep a fading
  combo warm — risk/reward dodging. Total grazes show on the game-over screen.
- **1UPs.** Rare **1** drop, plus a free life every 15,000 points (up to 6 lives).
- **Score popups.** Floating "+points", "POWER UP", weapon names, "GRAZE x10", "BOSS DOWN".
- **Formation waves.** Occasional synchronized rows of grunts that enter together.
- **Shield (S)** — a cyan ring that absorbs one hit without costing a life.
- **Bombs (B)** — **tap the screen to bomb**: clears all enemy bullets and heavily
  damages everything on screen. Start with 2; pick up more.
- **Combo multiplier (×1–×8).** Chain kills within ~1.6 s to raise the multiplier;
  each tier costs more kills, so ×8 is earned. Score = base × enemy value × multiplier.
- **Difficulty / stages.** Spawn rate, enemy speed, bullet speed and the enemy mix
  ramp with both time survived and score. A **STAGE n** banner marks each step.
- **Bosses every ~38 s, two variants that alternate:**
  - **Purple** — aimed **fan** → radial **ring** → **gap-wall** (a curtain with one
    opening to slide into) → rotating **spiral**.
  - **Crimson** — faster, more aggressive: tight bursts, gap-walls, wide rings, and a
    sweeping bullet **stream**.
  Each has a red HP bar; beat it for a big score bonus + guaranteed drops (incl. a
  weapon), then difficulty steps up.
- **Persistent high score** — your best is saved to `SharedPreferences` and shown
  on the title and game-over screens (survives quitting the app).
- **Juice:** screen shake, enemy hit-flash, player blink, particle explosions, a
  bomb/boss-kill white flash, and short **haptics** (hits, bombs, pickups).

---

## Controls

| Input | Action |
| --- | --- |
| **Rotary crown / bezel** | Move the jet left / right (clockwise → right) |
| **Tap screen** | **During play: drop a BOMB** · Start · Restart · Resume from pause |
| **Long-press screen** | Pause / unpause |
| Arrow keys (emulator only) | Left/Right move, Enter/D-pad-center = tap — testing convenience |

> If the crown direction feels reversed on your watch, flip one flag:
> `ROTARY_INVERT = true` in [`GameConfig.kt`](app/src/main/java/com/example/wearshooter/GameConfig.kt).

---

## Project structure

```
WearGame/
├─ settings.gradle.kts          Module list + repositories
├─ build.gradle.kts             Root: AGP 8.7.2 + Kotlin 2.0.21 plugin versions
├─ gradle.properties            AndroidX, JVM args
├─ gradle/wrapper/…             Gradle 8.11.1 wrapper
├─ local.properties            sdk.dir (generated; git-ignored)
└─ app/
   ├─ build.gradle.kts          minSdk 30, targetSdk/compileSdk 34, 1 dependency (core-ktx)
   ├─ proguard-rules.pro
   └─ src/main/
      ├─ AndroidManifest.xml    uses-feature watch · standalone meta-data · launcher activity
      ├─ res/
      │  ├─ values/strings.xml, themes.xml, ic_launcher_background.xml
      │  ├─ drawable/ic_launcher_foreground.xml   (vector jet icon)
      │  └─ mipmap-anydpi-v26/ic_launcher*.xml     (adaptive icon, no PNGs)
      └─ java/com/example/wearshooter/
         ├─ MainActivity.kt     Activity host; keep-screen-on; forwards crown events
         ├─ GameView.kt         SurfaceView: owns loop + Renderer, handles crown/tap/long-press/keys
         ├─ GameThread.kt       ~60 FPS loop: hardware-accelerated canvas (software fallback),
         │                        absolute-deadline frame pacing so timing error can't drift
         ├─ World.kt            ★ Simulation core: state machine, geometry, collisions,
         │                        scoring/combo/graze, bomb, difficulty — no drawing
         ├─ Weapons.kt          Player fire patterns, escort drones, Overdrive, power-ups
         ├─ Enemies.kt          Unlock schedule, per-type behaviour, enemy shot patterns
         ├─ Bosses.kt           Six bosses, HP-gated phases, telegraphed laser set-pieces
         ├─ Fx.kt               Particles, shockwaves, popups, shake, banners, starfield
         ├─ Renderer.kt         ★ Every pixel drawn: backdrops, entities, round-face HUD,
         │                        title / pause / game-over screens
         ├─ Palette.kt          Every colour + display name, indexed by enum ordinal
         ├─ Pools.kt            Object pools + swap-remove sweep (zero-GC entity churn)
         ├─ Entities.kt         Player / Bullet / Enemy / … data holders with pooled reset()
         └─ GameConfig.kt       ★ Every tuning knob (speeds, lives, spawn ramp, boss, scoring)
```

`★` = the files you’ll touch most for gameplay tuning and art.

### How it fits together

- **Game loop** — `GameThread` runs on its own thread. Each frame it measures the
  real delta time, calls `World.update(dt)`, then locks a **hardware-accelerated**
  `SurfaceHolder` canvas (GPU-composited; falls back to software if the device
  refuses) and calls `Renderer.render(canvas)`. Delta is clamped so a stall
  (GC / app switch) can’t teleport entities, and frames are paced against an
  absolute deadline so timing error never accumulates.
- **Zero-allocation steady state** — bullets, enemy bullets, enemies and particles
  live in object pools (`Pools.kt`); collision code only flips an `alive` flag and
  a once-per-frame sweep swap-removes the dead back into the pool. HUD strings are
  cached and rebuilt only when the value changes, and all palette lookups are
  plain array indexing — a steady frame allocates nothing, so the GC never gets a
  chance to hitch a dodge.
- **Round-face HUD** — everything anchored to the watch bezel is placed
  polar-style on the safe ring: lives arc on the lower-left, bombs on the
  lower-right, the Overdrive gauge dead-bottom between them. Nothing sits outside
  the inscribed circle, so nothing gets clipped by the round mask.
- **Rotary crown** — `GameView.onGenericMotionEvent` filters for
  `ACTION_SCROLL` from `SOURCE_ROTARY_ENCODER` and reads `AXIS_SCROLL`. The value
  is accumulated under a lock and drained once per frame, so input rate and frame
  rate stay decoupled. `MainActivity` also forwards crown events that some Wear
  builds deliver to the window first.
- **Round safe area** — the screen is treated as a circle of radius `min(w,h)/2`.
  `safeHalfWidthAt(y)` returns the chord half-width at any height, so the jet is
  clamped to the visible round area and enemies only spawn within the band the jet
  can actually reach (every enemy is fair).
- **Difficulty ramp** — spawn interval decays from `1.10s` toward `0.34s` and enemy
  speed gains `+8 px/s` for every second survived.
- **State machine** — `READY → PLAYING → (PAUSED) → GAME_OVER → PLAYING …`,
  all inside `World`.

---

## Build & run

Prerequisites: JDK 17+ and the Android SDK (build-tools 34, an android-34 **Wear**
system image). The Gradle wrapper pulls Gradle 8.11.1 automatically.

### Build the APK

```bash
cd WearGame
./gradlew :app:assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

If `local.properties` is missing, create it:

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS default
```

### Run on the Wear OS emulator

1. Create a round Wear AVD in Android Studio: **Device Manager → Create device →
   Wear → “Wear OS Small Round” → android-34 Wear image**. (A 384×384 AVD named
   `ccu_wear` is what this project was verified on.)
2. Boot it and install + launch:

```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.example.wearshooter/.MainActivity
```

3. **Drive the crown on the emulator:** open the emulator’s **⋮ Extended Controls →
   (Wear) Rotary input**, or click the small rotary slider on the side toolbar and
   drag it. The jet moves left/right.

Or just open the project in **Android Studio**, pick the Wear AVD, and press **Run ▶**.

### Run on a real Pixel Watch

1. On the watch: **Settings → System → About → tap Build number 7×** to enable
   Developer options, then **Settings → Developer options → ADB debugging** (and
   **Debug over Wi-Fi** if not using a USB dock).
2. Pair: `adb connect <watch-ip>:5555` (or via USB), then:

```bash
adb -s <watch-serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

3. Launch **Pixel Skies** from the watch app list. Use the real crown to steer.
   If clockwise moves the jet left, set `ROTARY_INVERT = true` and rebuild.

---

## Tuning the game

Open [`GameConfig.kt`](app/src/main/java/com/example/wearshooter/GameConfig.kt) —
everything is a named constant. To make it **easier or harder**, the high-impact knobs are:

- `ROTARY_SENSITIVITY` — px moved per crown unit (raise for snappier dodging).
- `START_LIVES`, `START_BOMBS`, `INVULN_TIME` — survivability.
- `EBULLET_SPEED`, `GUNNER_FIRE_INTERVAL`, `TANK_FIRE_INTERVAL` — incoming-fire pressure.
- `SPAWN_INTERVAL_START / _MIN`, `BOSS_INTERVAL`, `BOSS_BASE_HP` — pacing & boss cadence.
- `FIRE_INTERVAL`, `MAX_POWER` — your offense.
- `COMBO_WINDOW`, `MAX_MULTIPLIER`, `POWERUP_DROP_CHANCE` — scoring & rewards.
- The enemy mix is shaped by `pickEnemyType()` and the per-type stats in
  `spawnEnemy()` in [`World.kt`](app/src/main/java/com/example/wearshooter/World.kt).

All sizes are fractions of `min(width, height)`, so the game scales to any watch.

---

## Replacing the placeholder shapes with real pixel-art sprites

Right now every entity is drawn with `canvas.drawRect(...)` calls inside
**`World.kt`** (anti-aliasing is off, so they look crisp/pixelated). To swap in real
sprites later:

1. **Add your art** under `app/src/main/res/drawable-nodpi/` (use `-nodpi` so Android
   doesn’t rescale your pixel art), e.g. `player.png`, `enemy.png`, `bullet.png`.
   For crisp scaling, set the bitmap to nearest-neighbour:
   ```kotlin
   val opts = BitmapFactory.Options().apply { inScaled = false }
   val playerBmp = BitmapFactory.decodeResource(resources, R.drawable.player, opts)
   ```
2. **Pass them into `World`** (e.g. via a `setSprites(...)` method, or load them in
   `World` with a `Resources` handle) and keep a `Paint` with
   `isFilterBitmap = false` for that nearest-neighbour pixel look.
3. **Replace the draw methods.** Each entity already has one self-contained method:
   - `drawPlayer(canvas)` → `canvas.drawBitmap(playerBmp, srcRect, dstRect, paint)`
   - `drawEnemies(canvas)` → pick a sprite per `e.type` (see `enemyColors()` for the
     type→look mapping) and draw it centered on `Enemy(e.x, e.y)`
   - `drawBoss(canvas, boss)` → draw a big boss sprite; keep the HP-bar code
   - `drawBullets` / `drawEnemyBullets` / `drawPowerUps` → swap rects for sprites
   - `drawParticles(canvas)` → keep as rects, or use a small spark sprite
   The destination `RectF` is just the existing center ± half-size, so the collision
   sizes (`player.width`, `e.radius`, …) don’t change — gameplay stays identical.
4. **Animation (optional):** store a sprite sheet and pick a frame with
   `animTime` (already ticking in `World`) → `frame = ((animTime * fps).toInt()) % frameCount`.

Because rendering is fully isolated in the `draw*` methods and all hitboxes come
from numeric fields, you can replace the art **without touching any game logic.**

---

## Notes / ideas to extend

- Persist the high score with `SharedPreferences` (currently `best` lives for the
  session only).
- Add a soft haptic tick on kills via `view.performHapticFeedback(...)`.
- Enemy variety: shooters, zig-zaggers, mini-bosses (the `Enemy` already supports a
  sway amplitude you can build on).
- Ambient/always-on handling if you want it to behave on the watch face dim cycle.

Licensed for you to do whatever you like with — all assets here are original.
