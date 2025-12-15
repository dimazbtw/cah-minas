package github.dimazbtw.minas.data;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class PickaxeSkin {

    private final String id;
    private final Material material;
    private final int order;
    private final String display;
    private final double bonus;

    public PickaxeSkin(String id, Material material, int order, String display, double bonus) {
        this.id = id;
        this.material = material;
        this.order = order;
        this.display = display;
        this.bonus = bonus;
    }

    public static PickaxeSkin fromConfig(String id, ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("material", "WOODEN_PICKAXE").toUpperCase());
        int order = section.getInt("order", 0);
        String display = section.getString("display", "&6Picareta");
        double bonus = section.getDouble("bonus", 1.0);

        return new PickaxeSkin(id, material, order, display, bonus);
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplay() {
        return display;
    }

    public double getBonus() {
        return bonus;
    }
}