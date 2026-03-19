package org.mateof24.rpg_tweaks.config;

public class XPValues {
    public Integer minXP;
    public Integer maxXP;

    public XPValues() {
        this.minXP = 0;
        this.maxXP = 0;
    }

    public XPValues(Integer minXP, Integer maxXP) {
        this.minXP = minXP;
        this.maxXP = maxXP;
    }

    public boolean shouldDropXP() {
        return (minXP != null && minXP > 0) || (maxXP != null && maxXP > 0);
    }

    public int getRandomXP() {
        if (minXP == null || maxXP == null) return 0;
        if (minXP.equals(maxXP)) return minXP;
        int min = Math.min(minXP, maxXP);
        int max = Math.max(minXP, maxXP);
        return min + (int) (Math.random() * (max - min + 1));
    }

    public void validate() {
        if (minXP == null) minXP = 0;
        if (maxXP == null) maxXP = 0;
        if (minXP < 0) minXP = 0;
        if (maxXP < 0) maxXP = 0;
        if (minXP > maxXP) {
            Integer temp = minXP;
            minXP = maxXP;
            maxXP = temp;
        }
    }
}