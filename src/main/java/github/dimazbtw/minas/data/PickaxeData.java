package github.dimazbtw.minas.data;

import java.util.UUID;

/**
 * Representa os dados da picareta de um jogador
 */
public class PickaxeData {

    private final UUID playerUUID;
    private int blocksMined;
    private int level;
    private int exp;
    private int efficiency;   // Aumenta velocidade de mineração
    private int fortune;      // Multiplica rewards (coins/exp)
    private int explosion;    // Chance de explosão 7x7
    private int multiplier;   // Multiplica XP do bloco para evoluir encantamentos
    private int experienced;  // Jogador recebe mais exp para o nível da picareta
    private int destroyer;    // Chance de quebrar uma camada da mina
    private int skinOrder;    // Ordem da skin ativada (0 = default)

    // Configurações de níveis máximos
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

    /**
     * Calcula o multiplicador de fortuna
     */
    public double getFortuneMultiplier() {
        return 1.0 + (fortune * 0.5);
    }

    /**
     * Calcula a chance de explosão 7x7 (0-100%)
     */
    public double getExplosionChance() {
        return explosion * 0.1;
    }

    /**
     * Calcula o multiplicador de XP do bloco para evoluir encantamentos
     */
    public double getMultiplierBonus() {
        return 1.0 + (multiplier * 0.01);
    }

    /**
     * Calcula o bónus de EXP para o nível da picareta
     */
    public double getExperiencedBonus() {
        return 1.0 + (experienced * 0.02);
    }

    /**
     * Calcula a chance de quebrar uma camada da mina (0-100%)
     */
    public double getDestroyerChance() {
        return destroyer * 0.1;
    }

    /**
     * Calcula o custo em XP para melhorar eficiência
     */
    public int getEfficiencyUpgradeCost() {
        return (efficiency + 1) * 100;
    }

    /**
     * Calcula o custo em XP para melhorar fortuna
     */
    public int getFortuneUpgradeCost() {
        return (fortune + 1) * 150;
    }

    /**
     * Calcula o custo em XP para melhorar explosão
     */
    public int getExplosionUpgradeCost() {
        return (explosion + 1) * 300;
    }

    /**
     * Verifica se pode melhorar eficiência
     */
    public boolean canUpgradeEfficiency() {
        return efficiency < MAX_EFFICIENCY;
    }

    /**
     * Verifica se pode melhorar fortuna
     */
    public boolean canUpgradeFortune() {
        return fortune < MAX_FORTUNE;
    }

    /**
     * Verifica se pode melhorar explosão
     */
    public boolean canUpgradeExplosion() {
        return explosion < MAX_EXPLOSION;
    }

    /**
     * Incrementa blocos minerados
     */
    public void incrementBlocksMined(int amount) {
        this.blocksMined += amount;
    }

    /**
     * Adiciona EXP à picareta e retorna o nível anterior (para verificar level up)
     */
    public int addExp(int amount) {
        int oldLevel = this.level;
        this.exp += amount;
        checkLevelUp();
        return oldLevel;
    }

    /**
     * Verifica e processa level up
     */
    private void checkLevelUp() {
        int expNeeded = getExpForNextLevel();
        while (exp >= expNeeded && level < 100) {
            exp -= expNeeded;
            level++;
            expNeeded = getExpForNextLevel();
        }
    }

    /**
     * Calcula EXP necessário para o próximo nível
     */
    public int getExpForNextLevel() {
        return level * 100;
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