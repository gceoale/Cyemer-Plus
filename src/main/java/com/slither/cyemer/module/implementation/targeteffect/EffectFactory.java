package com.slither.cyemer.module.implementation.targeteffect;

import com.slither.cyemer.module.implementation.targeteffect.effects.CombinationEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.GalaxyEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.HelixEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.LightningCageEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.OrbitRingEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.OverlayEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.PulseEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.RingsEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.ScanLinesEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.ShockwaveRingsEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.SpinningSpheresEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.SpiralEffect;
import com.slither.cyemer.module.implementation.targeteffect.effects.TornadoEffect;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EffectFactory {
    private final Map<String, BaseEffect> effectCache = new HashMap<>();

    public EffectFactory() {
        this.effectCache.put("Spinning Spheres", new SpinningSpheresEffect());
        this.effectCache.put("Orbit Ring", new OrbitRingEffect());
        this.effectCache.put("Pulse", new PulseEffect());
        this.effectCache.put("Helix", new HelixEffect());
        this.effectCache.put("Galaxy", new GalaxyEffect());
        this.effectCache.put("Tornado", new TornadoEffect());
        this.effectCache.put("Rings", new RingsEffect());
        this.effectCache.put("Spiral", new SpiralEffect());
        this.effectCache.put("Lightning", new LightningCageEffect());
        this.effectCache.put("Shockwave", new ShockwaveRingsEffect());
        this.effectCache.put("Scanlines", new ScanLinesEffect());
        this.effectCache.put("Goofball", new CombinationEffect());
        this.effectCache.put("Overlay", new OverlayEffect());
    }

    public BaseEffect getEffect(String name) {
        return this.effectCache.getOrDefault(name, this.effectCache.get("Spinning Spheres"));
    }
}
