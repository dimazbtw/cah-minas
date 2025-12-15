package github.dimazbtw.minas.data;

import java.util.UUID;

/**
 * Representa uma entrada no ranking
 */
public class RankingEntry {

    private final UUID uuid;
    private final String playerName;
    private final int blocksMined;
    private final int level;
    private final int exp;

    public RankingEntry(UUID uuid, String playerName, int blocksMined, int level, int exp) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.blocksMined = blocksMined;
        this.level = level;
        this.exp = exp;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getBlocksMined() {
        return blocksMined;
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
    }

    public long getTotalScore() {
        return blocksMined;
    }
}