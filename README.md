# PotatoBench

A Fabric mod for Minecraft 1.21.1. **Type `/bench` and it measures your real FPS**, so you can tell if a setting, mod or modpack actually helps.

Made to go with [PotatoPack](https://github.com/Hybridash/PotatoPack): run it once in vanilla and once in PotatoPack and you've got real numbers.

## What you get

```
PotatoBench results
  Average: 187 FPS
  1% low: 94 FPS   0.1% low: 61 FPS
  Worst frame: 21.4 ms   Frames: 5612
  Render distance 8, fast graphics, 24 mods (Sodium), Intel(R) UHD Graphics 620
  [Copy results]
```

- **Average FPS**: frames rendered ÷ seconds.
- **1% low / 0.1% low**: the average FPS of your slowest 1% (and 0.1%) of frames. These show **stutter**. A game averaging 200 FPS with a 1% low of 30 feels choppy.
- **Worst frame**: the single longest frame, in milliseconds.
- **[Copy results]**: click it to copy a one-line summary to paste into Discord or a GitHub issue.

Every run is also saved to `.minecraft/potatobench/results.csv`, which opens in Excel or Google Sheets.

## How it keeps runs fair

By default the camera slowly spins a full 360° during the test, so every run draws the same amount of world. There's a 3 second warm-up before measuring starts. For the fairest comparison, stand in the same spot each time.

## Commands

| Command | What it does |
|---|---|
| `/bench` | 30 second benchmark, camera spins 360° |
| `/bench <seconds>` | Pick the length (5 to 600 seconds) |
| `/bench still` / `/bench still <seconds>` | Don't spin the camera |
| `/bench stop` | Cancel |
| `/bench history` | Your last 5 results |

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder.
3. Download `potatobench-x.x.x.jar` from [**Releases**](../../releases) and put it in `mods`.

Client-side only.

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`. Pushing a tag like `v1.0.1` publishes a release automatically.
