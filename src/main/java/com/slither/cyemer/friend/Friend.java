package com.slither.cyemer.friend;

import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Friend {
    private final String name;
    private final UUID uuid;
    private long addedTime;
    private String note;

    public Friend(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
        this.addedTime = System.currentTimeMillis();
        this.note = "";
    }

    public Friend(String name, UUID uuid, long addedTime, String note) {
        this.name = name;
        this.uuid = uuid;
        this.addedTime = addedTime;
        this.note = note != null ? note : "";
    }

    public String getName() {
        return this.name;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public long getAddedTime() {
        return this.addedTime;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note != null ? note : "";
    }

    public String getDisplayName() {
        return this.note.isEmpty() ? this.name : this.note + " (" + this.name + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else {
            return !(obj instanceof Friend other) ? false : this.uuid.equals(other.uuid);
        }
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }
}
