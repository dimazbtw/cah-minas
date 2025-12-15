package github.dimazbtw.minas.data;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class MineBlock {

    private final Material material;
    private double chance;
    private int exp;
    private double money;
    private List<String> rewards;

    public MineBlock(Material material) {
        this.material = material;
        this.chance = 100.0;
        this.exp = 0;
        this.money = 0.0;
        this.rewards = new ArrayList<>();
    }

    public MineBlock(Material material, double chance, int exp, double money) {
        this.material = material;
        this.chance = chance;
        this.exp = exp;
        this.money = money;
        this.rewards = new ArrayList<>();
    }

    /**
     * Executa os rewards para um jogador
     */
    public void executeRewards(org.bukkit.entity.Player player) {
        if (rewards == null || rewards.isEmpty()) return;

        java.util.Random random = new java.util.Random();

        for (String rewardLine : rewards) {
            // Formato: "comando1:comando2, chance"
            String[] parts = rewardLine.split(",");
            if (parts.length < 2) continue;

            String commandsPart = parts[0].trim();
            double rewardChance;
            try {
                rewardChance = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }

            // Verificar chance
            if (random.nextDouble() * 100 > rewardChance) continue;

            // Executar comandos
            String[] commands = commandsPart.split(":");
            for (String command : commands) {
                command = command.trim().replace("{player}", player.getName());
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
            }
        }
    }

    // ============ GETTERS E SETTERS ============

    public Material getMaterial() {
        return material;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = Math.max(0, Math.min(100, chance));
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = Math.max(0, exp);
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = Math.max(0, money);
    }

    public List<String> getRewards() {
        return rewards;
    }

    public void setRewards(List<String> rewards) {
        this.rewards = rewards != null ? rewards : new ArrayList<>();
    }

    public void addReward(String reward) {
        if (rewards == null) rewards = new ArrayList<>();
        rewards.add(reward);
    }

    public void removeReward(int index) {
        if (rewards != null && index >= 0 && index < rewards.size()) {
            rewards.remove(index);
        }
    }

    @Override
    public String toString() {
        return "MineBlock{" +
                "material=" + material +
                ", chance=" + chance +
                ", exp=" + exp +
                ", money=" + money +
                '}';
    }
}