package github.dimazbtw.minas.data;

import java.util.UUID;

/**
 * Representa a sessão de um jogador em uma mina
 */
public class PlayerSession {

    private final UUID playerUUID;
    private Mine currentMine;
    private long sessionStart;
    private int blocksMined;

    public PlayerSession(UUID playerUUID, Mine mine) {
        this.playerUUID = playerUUID;
        this.currentMine = mine;
        this.sessionStart = System.currentTimeMillis();
        this.blocksMined = 0;
    }

    /**
     * Obtém o tempo de sessão em segundos
     */
    public long getSessionTime() {
        return (System.currentTimeMillis() - sessionStart) / 1000;
    }

    /**
     * Incrementa o contador de blocos minerados
     */
    public void incrementBlocksMined() {
        this.blocksMined++;
    }

    /**
     * Reseta as estatísticas da sessão
     */
    public void reset() {
        this.sessionStart = System.currentTimeMillis();
        this.blocksMined = 0;
    }

    // ============ GETTERS E SETTERS ============

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Mine getCurrentMine() {
        return currentMine;
    }

    public void setCurrentMine(Mine currentMine) {
        this.currentMine = currentMine;
        reset();
    }

    public long getSessionStart() {
        return sessionStart;
    }

    public int getBlocksMined() {
        return blocksMined;
    }
}