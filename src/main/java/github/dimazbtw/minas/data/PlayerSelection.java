package github.dimazbtw.minas.data;

import org.bukkit.Location;

import java.util.UUID;

public class PlayerSelection {

    private final UUID playerUUID;
    private final String mineId;
    private Location pos1;
    private Location pos2;
    private final long createdAt;

    public PlayerSelection(UUID playerUUID, String mineId) {
        this.playerUUID = playerUUID;
        this.mineId = mineId;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public boolean isExpired() {
        // Expira após 5 minutos
        return System.currentTimeMillis() - createdAt > 300000;
    }

    // ============ GETTERS E SETTERS ============

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getMineId() {
        return mineId;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
