# Cyemer+

A rebuild of the **Cyemer** Fabric client for Minecraft **1.21.11**, decompiled from the public jar and set up to build as a proper Gradle project, plus a few additions on top.

## Credits

All of the original client code — every module, mixin, shader, and the entire architecture — is the work of **lime53._72375** (Discord). This repo would not exist without their permission to decompile and republish the source; that permission was given publicly and the additions here are strictly additive on top of their work.

If you use Cyemer+ or fork it further, credit lime53._72375 first.

## What's different from stock Cyemer

- **Gradle build restored.** The decompiled sources are laid out under `src/main/java` and `src/main/resources` and compile with Fabric Loom 1.17.11 against MC 1.21.11 + Loader 0.19.3 + Fabric API 0.141.4. Intermediary is used as the mapping layer, since the decompiled source ships with intermediary names.
- **`RotationMode.FPS`** — new per-frame exponential-lerp rotation mode in `RotationManager`. Frame-rate independent, no GCD snapping, no velocity smoothing, no easing. Substantially smoother than the stock SMOOTH / SINE / LINEAR modes at high refresh rates. Exposed in `AimAssist` under `Pattern`.
- **`AutoGG` module** — detects your kills (entity you attacked dies within 5 s) and sends "gg" in chat, alternating to "ggs" if enabled. Settings: `Alternate`, `Skip Friends`, `Players Only`, `Delay (ms)`.

## Building

Requires JDK 21.

```
./gradlew build
```

Output jar lands in `build/libs/`. Drop it in your Fabric mods folder alongside Fabric API 0.141.4+ for 1.21.11.

## Notes

- The mod ships with the shipped Cyemer `fabric.mod.json` (id `dynamic_fps`) — this is intentional and matches how the original was distributed. Do not change it unless you also refactor the hardcoded `"dynamic_fps"` asset namespace across ~100 call sites.
- License is `All-Rights-Reserved` per the original mod metadata. Redistribute with permission only.
