package com.slither.cyemer.util;

import com.slither.cyemer.module.implementation.donutsmp.FarmFinder;
import com.slither.cyemer.module.implementation.donutsmp.SpotFinder;
import com.slither.cyemer.module.implementation.render.BlockESP;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;

/** What FarmFinder's and BlockESP's AutoRTP share. */
@Environment(EnvType.CLIENT)
public final class AutoRtp {
    private AutoRtp() {
    }

    /**
     * Sent as a command, not chat. A message starting with a slash that goes
     * through the chat path is posted to chat as literal text; the chat
     * screen strips the slash and routes commands through method_45730
     * instead, so this does the same. Accepts the command with or without
     * its slash.
     */
    public static void send(String raw) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1724.field_3944 == null) {
            return;
        }
        if (SpotFinder.isDigging()) {
            return; // an /rtp now would carry the player away from the room Auto Mine is digging
        }
        String command = raw == null ? "" : raw.trim();
        while (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (!command.isEmpty()) {
            mc.field_1724.field_3944.method_45730(command);
        }
    }

    /**
     * A find stops AutoRTP in every module, not just the one that found it;
     * otherwise the other module's next /rtp would carry us away from it.
     */
    public static void stopAll() {
        FarmFinder finder = FarmFinder.getInstance();
        if (finder != null) {
            finder.cancelAutoRtp();
        }
        BlockESP esp = BlockESP.getInstance();
        if (esp != null) {
            esp.cancelAutoRtp();
        }
    }
}
