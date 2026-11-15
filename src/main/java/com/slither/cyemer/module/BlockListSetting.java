package com.slither.cyemer.module;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2248;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

/**
 * A set of blocks, picked from a searchable menu in the ClickGUI.
 *
 * Stored as comma-separated block ids ("minecraft:respawn_anchor,...") and
 * extends StringSetting, so configs save and load it with no extra code.
 * The set is parsed lazily whenever the stored text changes, which also
 * covers a config load; ids that no longer exist are dropped.
 */
@Environment(EnvType.CLIENT)
public class BlockListSetting extends StringSetting {
    private String parsedFrom = null;
    private Set<class_2248> blocks = Set.of();
    private int version = 0;

    public BlockListSetting(String name, String... defaultIds) {
        super(name, String.join(",", defaultIds));
    }

    public Set<class_2248> getBlocks() {
        String value = this.getValue();
        if (value != this.parsedFrom && (value == null || !value.equals(this.parsedFrom))) {
            Set<class_2248> parsed = new HashSet<>();
            if (value != null) {
                for (String part : value.split(",")) {
                    class_2960 id = class_2960.method_12829(part.trim());
                    if (id != null) {
                        class_7923.field_41175.method_17966(id).ifPresent(parsed::add);
                    }
                }
            }
            this.blocks = Collections.unmodifiableSet(parsed);
            this.parsedFrom = value;
            this.version++;
        }
        return this.blocks;
    }

    /** Bumps whenever the set changes, so users can tell without comparing sets. */
    public int getVersion() {
        this.getBlocks();
        return this.version;
    }

    public boolean contains(class_2248 block) {
        return this.getBlocks().contains(block);
    }

    public void toggle(class_2248 block) {
        Set<class_2248> next = new HashSet<>(this.getBlocks());
        if (!next.remove(block)) {
            next.add(block);
        }
        this.store(next);
    }

    public void clear() {
        this.store(Set.of());
    }

    private void store(Set<class_2248> next) {
        // Sorted so the saved config does not reshuffle on every change.
        TreeSet<String> ids = new TreeSet<>();
        for (class_2248 block : next) {
            ids.add(class_7923.field_41175.method_10221(block).toString());
        }
        this.setValue(String.join(",", ids));
        this.getBlocks();
        this.callListener();
    }
}
