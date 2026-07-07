package com.slither.cyemer.hud;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.impl.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public class ClientEventHandler {
    private static boolean wasPlayerAlive = false;

    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> TotemPopManager.getInstance().clear());
        ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
            EventBus.post(new TickEvent());
            class_746 player = client.field_1724;
            if (player == null) {
                wasPlayerAlive = false;
            } else {
                boolean isPlayerAliveNow = player.method_5805();
                if (isPlayerAliveNow && !wasPlayerAlive) {
                    TotemPopManager.getInstance().clear();
                }

                wasPlayerAlive = isPlayerAliveNow;
            }
        });
    }
}
