package github.dimazbtw.minas.actionbar;

import lombok.Getter;

@Getter
public class ActionBarEarnings {

    private double money;
    private int xp;

    public void addMoney(double amount) {
        money += amount;
    }

    public void addXp(int amount) {
        xp += amount;
    }

    public boolean hasEarnings() {
        return money > 0 || xp > 0;
    }

    public void reset() {
        money = 0;
        xp = 0;
    }
}
