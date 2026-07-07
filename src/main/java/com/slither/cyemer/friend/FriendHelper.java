package com.slither.cyemer.friend;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_239;
import net.minecraft.class_310;
import net.minecraft.class_3966;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class FriendHelper {
    private static final class_310 mc = class_310.method_1551();

    public static boolean toggleTargetedPlayer() {
        class_1657 targetedPlayer = getTargetedPlayer();
        if (targetedPlayer == null) {
            return false;
        } else {
            String playerName = targetedPlayer.method_5477().getString();
            Module notifications = Cyemer.getInstance().getModuleManager().getModule("Notifications");
            if (FriendManager.getInstance().isFriend(targetedPlayer.method_5667())) {
                FriendManager.getInstance().removeFriend(targetedPlayer.method_5667());
                if (notifications != null) {
                    ModuleAccess.invoke(notifications, "error", new Class[]{String.class, String.class}, "Friends", "Removed " + playerName + ".");
                }
            } else {
                FriendManager.getInstance().addFriend(playerName, targetedPlayer.method_5667());
                if (notifications != null) {
                    ModuleAccess.invoke(notifications, "success", new Class[]{String.class, String.class}, "Friends", "Added " + playerName + ".");
                }
            }

            return true;
        }
    }

    public static class_1657 getTargetedPlayer() {
        if (mc.field_1724 != null && mc.field_1687 != null) {
            class_239 hitResult = mc.field_1765;
            if (hitResult != null && hitResult.method_17783() == class_240.field_1331) {
                class_3966 entityHit = (class_3966)hitResult;
                class_1297 entity = entityHit.method_17782();
                if (entity instanceof class_1657 && entity != mc.field_1724) {
                    return (class_1657)entity;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public static boolean isTargetingFriend() {
        class_1657 target = getTargetedPlayer();
        return target != null && FriendManager.getInstance().isFriend(target.method_5667());
    }
}
