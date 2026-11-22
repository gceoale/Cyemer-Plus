package com.slither.cyemer.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_2680;

/**
 * A module that wants every block change the server sends. ClientWorldMixin
 * calls it on enabled modules, on the client thread, before the change is
 * applied, so before is still what the world holds.
 *
 * The mixin knows modules only through this interface, never by class:
 * that keeps them out of the core so HotReload can swap them in a running
 * game.
 */
@Environment(EnvType.CLIENT)
public interface BlockUpdateListener {
    void onBlockUpdate(class_2338 pos, class_2680 before, class_2680 after);
}
