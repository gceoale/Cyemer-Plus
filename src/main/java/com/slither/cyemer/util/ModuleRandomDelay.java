package com.slither.cyemer.util;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleRandomDelay {
    private static final Random RANDOM = new Random();
    private static final Map<String, Long> READY_AT_BY_ACTION = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> HELD_STATE = new ConcurrentHashMap<>();

    private ModuleRandomDelay() {
    }

    public static boolean gateAction(String actionKey, long minDelayMs, long maxDelayMs) {
        String effectiveKey = normalizeBucketKey(actionKey);
        long now = System.currentTimeMillis();
        Long readyAt = READY_AT_BY_ACTION.get(effectiveKey);
        if (readyAt == null) {
            long sampledDelay = sampleDelay(minDelayMs, maxDelayMs);
            if (sampledDelay <= 0L) {
                return true;
            } else {
                READY_AT_BY_ACTION.put(effectiveKey, now + sampledDelay);
                return false;
            }
        } else if (now < readyAt) {
            return false;
        } else {
            READY_AT_BY_ACTION.remove(effectiveKey);
            return true;
        }
    }

    public static boolean gatePress(String actionKey, boolean desiredPressed, long minDelayMs, long maxDelayMs) {
        String effectiveKey = normalizeBucketKey(actionKey);
        if (!desiredPressed) {
            HELD_STATE.put(effectiveKey, false);
            READY_AT_BY_ACTION.remove(effectiveKey);
            return false;
        } else if (HELD_STATE.getOrDefault(effectiveKey, false)) {
            return true;
        } else if (!gateAction(effectiveKey, minDelayMs, maxDelayMs)) {
            return false;
        } else {
            HELD_STATE.put(effectiveKey, true);
            return true;
        }
    }

    public static void clearActionState(String actionKey) {
        if (actionKey != null && !actionKey.isBlank()) {
            String effectiveKey = normalizeBucketKey(actionKey);
            READY_AT_BY_ACTION.remove(effectiveKey);
            HELD_STATE.remove(effectiveKey);
            READY_AT_BY_ACTION.remove(actionKey);
            HELD_STATE.remove(actionKey);
        }
    }

    private static String normalizeBucketKey(String actionKey) {
        if (actionKey != null && !actionKey.isBlank()) {
            String trimmed = actionKey.trim();
            String[] parts = trimmed.split("\\.");
            if (parts.length >= 3) {
                String domain = parts[0];
                if ("combat".equals(domain) || "player".equals(domain)) {
                    String moduleKey = parts[2];
                    if (!moduleKey.isBlank()) {
                        return domain + "." + moduleKey;
                    }
                }
            }

            return trimmed;
        } else {
            return "action.unknown";
        }
    }

    private static long sampleDelay(long minDelayMs, long maxDelayMs) {
        long min = Math.max(0L, minDelayMs);
        long max = Math.max(0L, maxDelayMs);
        if (max < min) {
            long temp = min;
            min = max;
            max = temp;
        }

        if (max == min) {
            return min;
        } else {
            long range = max - min + 1L;
            long offset = Math.floorMod(RANDOM.nextLong(), range);
            return min + offset;
        }
    }
}
