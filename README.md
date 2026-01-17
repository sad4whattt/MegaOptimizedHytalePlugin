## Join the Discord!

[Epic Discord Server with updates](https://discord.gg/7Tzqrbsdyp)

# MegaPerformance Plugin

Performance-focused drop-in tweaks for Hytale servers (2026.01.09-49e5904). Conservative defaults; minimal gameplay impact.

## Installation
1. Build or download `MegaPerformancePlugin.jar`.
2. Place into `mods/`.
3. Start server once; it will create `mods/Snipr_MegaOptimized/config.json`.
4. Tweak config if desired, then `/megaperf reload` (or restart) to apply.

## Defaults (config.json)
- Tick: `maxCatchupMillis=50`, `jitterClampMs=10` (reserved for future tick pacing).
- Async: `workerThreads=2`, `maxQueue=512` (bounded executor).
- Entities: `softCapPerChunk=35`, `distantAiSkipEvery=4` (skip work every 4s + trim beyond cap, non-players only).
- Network: `packetBurstLimit=256`, `coalesceChatMs=75ms` (chat burst suppression via formatter).
- World: `autosaveMinutes=10`, `staggerSaves=true` (uses Universe.runBackup on schedule).
- Particles: `densityScalar=0.85`, `applyToProjectiles=true` (exposed via system properties, safe no-op if unsupported).
- Bundled: `aggressiveTrimming=false`, `lowAiDepth=false` (aggressive trim toggle honored by entity optimizer).

## Features
- Bounded async executor with metrics logging.
- Autosave/backup pacing on fixed delay.
- Entity optimizer: soft-cap per chunk + periodic trimming (non-player) with optional aggressive mode.
- Chat burst limiter: coalesces rapid messages to reduce packet spam.
- Particle scalar: exposes density hints via system properties.
- Hot-reload command `/megaperf --action=reload`.

## Command
- `/megaperf --action=reload` — reload config from disk.

## Notes
- All optimizations are best-effort and wrapped to avoid crashing; if APIs change, they safely noop.
- Packet burst limit is exposed but not enforced without server networking hooks; chat coalescing is applied now.
- Logs: async queue/completed counts every 30s at DEBUG; entity trims logged at DEBUG.


## Looking for quality hosting?

[![Kinetic Hosting - Hytale Server Hosting](https://i.ibb.co/5XFkWtyy/KH-Curse-Forge-Final-Wide-Banner-Hytale-Small.png)](https://billing.kinetichosting.com/aff.php?aff=1251)
