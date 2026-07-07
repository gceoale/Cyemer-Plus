package com.slither.cyemer.hud;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;

@Environment(EnvType.CLIENT)
public class TotemPopManager {
    private static final TotemPopManager INSTANCE = new TotemPopManager();
    private final Map<UUID, Integer> popCounts = new ConcurrentHashMap<>();

    private TotemPopManager() {
    }

    public static TotemPopManager getInstance() {
        return INSTANCE;
    }

    public void onTotemPop(class_1657 player) {
        int currentPops = this.popCounts.getOrDefault(player.method_5667(), 0);
        this.popCounts.put(player.method_5667(), currentPops + 1);
    }

    public int getPopCount(class_1297 entity) {
        return !(entity instanceof class_1657) ? 0 : this.popCounts.getOrDefault(entity.method_5667(), 0);
    }

    public void resetPopCount(class_1297 entity) {
        if (entity != null) {
            this.popCounts.remove(entity.method_5667());
        }
    }

    public void clear() {
        this.popCounts.clear();
    }
}
