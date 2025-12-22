// PickaxeData.java - Sem limite de nível
package github.dimazbtw.minas.data;

import java.util.UUID;

public class PickaxeData {

    private final UUID playerUUID;
    private int blocksMined;
    private int level;
    private int exp;
    private int efficiency;
    private int fortune;
    private int explosion;
    private int multiplier;
    private int experienced;
    private int destroyer;
    private int skinOrder;

    public static final int MAX_EFFICIENCY = 1000;
    public static final int MAX_FORTUNE = 1000;
    public static final int MAX_EXPLOSION = 1000;
    public static final int MAX_MULTIPLIER = 1000;
    public static final int MAX_EXPERIENCED = 1000;
    public static final int MAX_DESTROYER = 100;

    public PickaxeData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.blocksMined = 0;
        this.level = 1;
        this.exp = 0;
        this.efficiency = 0;
        this.fortune = 0;
        this.explosion = 0;
        this.multiplier = 0;
        this.experienced = 0;
        this.destroyer = 0;
        this.skinOrder = 0;
    }

    public PickaxeData(UUID playerUUID, int blocksMined, int level, int exp,
                       int efficiency, int fortune, int explosion,
                       int multiplier, int experienced, int destroyer, int skinOrder) {
        this.playerUUID = playerUUID;
        this.blocksMined = blocksMined;
        this.level = level;
        this.exp = exp;
        this.efficiency = efficiency;
        this.fortune = fortune;
        this.explosion = explosion;
        this.multiplier = multiplier;
        this.experienced = experienced;
        this.destroyer = destroyer;
        this.skinOrder = skinOrder;
    }

    public double getFortuneMultiplier() {
        return 1.0 + (fortune * 0.5);
    }

    public double getExplosionChance() {
        return explosion * 0.1;
    }

    public double getMultiplierBonus() {
        return 1.0 + (multiplier * 0.01);
    }

    public double getExperiencedBonus() {
        return 1.0 + (experienced * 0.02);
    }

    public double getDestroyerChance() {
        return destroyer * 0.1;
    }

    public int getEfficiencyUpgradeCost() {
        return (efficiency + 1) * 100;
    }

    public int getFortuneUpgradeCost() {
        return (fortune + 1) * 150;
    }

    public int getExplosionUpgradeCost() {
        return (explosion + 1) * 300;
    }

    public boolean canUpgradeEfficiency() {
        return efficiency < MAX_EFFICIENCY;
    }

    public boolean canUpgradeFortune() {
        return fortune < MAX_FORTUNE;
    }

    public boolean canUpgradeExplosion() {
        return explosion < MAX_EXPLOSION;
    }

    public void incrementBlocksMined(int amount) {
        this.blocksMined += amount;
    }

    /**
     * Adiciona EXP à picareta e retorna o nível anterior (para verificar level up)
     * SEM LIMITE DE NÍVEL
     */
    public int addExp(int amount) {
        int oldLevel = this.level;
        this.exp += amount;
        checkLevelUp();
        return oldLevel;
    }

    /**
     * Verifica e processa level up - SEM LIMITE
     */
    private void checkLevelUp() {
        int expNeeded = getExpForNextLevel();

        // Loop infinito até não ter mais EXP suficiente
        while (exp >= expNeeded) {
            exp -= expNeeded;
            level++;
            expNeeded = getExpForNextLevel();
        }
    }

    /**
     * Calcula EXP necessário para o próximo nível
     * Fórmula escalonada para evitar números muito altos
     */
    public int getExpForNextLevel() {
        if (level <= 100) {
            return level * 100;
        } else if (level <= 500) {
            return 10000 + ((level - 100) * 150);
        } else if (level <= 1000) {
            return 70000 + ((level - 500) * 200);
        } else {
            // Níveis acima de 1000 têm crescimento mais lento
            return 170000 + ((level - 1000) * 250);
        }
    }

    /**
     * Calcula a porcentagem de progresso para o próximo nível
     * Retorna valor entre 0.0 e 1.0
     */
    public double getProgressToNextLevel() {
        int expNeeded = getExpForNextLevel();
        if (expNeeded == 0) return 1.0;
        return Math.min(1.0, (double) exp / expNeeded);
    }

    public void reset() {
        this.blocksMined = 0;
        this.level = 1;
        this.exp = 0;
        this.efficiency = 0;
        this.fortune = 0;
        this.explosion = 0;
        this.multiplier = 0;
        this.experienced = 0;
        this.destroyer = 0;
        this.skinOrder = 0;
    }

    // ============ GETTERS E SETTERS ============

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public int getBlocksMined() {
        return blocksMined;
    }

    public void setBlocksMined(int blocksMined) {
        this.blocksMined = blocksMined;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = Math.max(0, exp);
    }

    public int getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(int efficiency) {
        this.efficiency = Math.min(MAX_EFFICIENCY, Math.max(0, efficiency));
    }

    public int getFortune() {
        return fortune;
    }

    public void setFortune(int fortune) {
        this.fortune = Math.min(MAX_FORTUNE, Math.max(0, fortune));
    }

    public int getExplosion() {
        return explosion;
    }

    public void setExplosion(int explosion) {
        this.explosion = Math.min(MAX_EXPLOSION, Math.max(0, explosion));
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = Math.min(MAX_MULTIPLIER, Math.max(0, multiplier));
    }

    public int getExperienced() {
        return experienced;
    }

    public void setExperienced(int experienced) {
        this.experienced = Math.min(MAX_EXPERIENCED, Math.max(0, experienced));
    }

    public int getDestroyer() {
        return destroyer;
    }

    public void setDestroyer(int destroyer) {
        this.destroyer = Math.min(MAX_DESTROYER, Math.max(0, destroyer));
    }

    public int getSkinOrder() {
        return skinOrder;
    }

    public void setSkinOrder(int skinOrder) {
        this.skinOrder = skinOrder;
    }
}