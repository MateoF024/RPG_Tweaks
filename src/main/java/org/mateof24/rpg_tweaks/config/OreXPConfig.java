package org.mateof24.rpg_tweaks.config;

import java.util.ArrayList;
import java.util.List;

public class OreXPConfig {

    public java.util.Map<String, OreXPValues> blockConfigs = new java.util.HashMap<>();
    public java.util.Map<String, OreXPValues> tagConfigs = new java.util.HashMap<>();

    public static class OreXPValues {
        public Integer minXP;
        public Integer maxXP;

        public OreXPValues() {
            this.minXP = 0;
            this.maxXP = 0;
        }

        public OreXPValues(Integer minXP, Integer maxXP) {
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


    public OreXPValues getConfigForBlock(String blockId, List<String> blockTags) {
        if (blockConfigs.containsKey(blockId)) {
            return blockConfigs.get(blockId);
        }
        for (String tag : blockTags) {
            if (tagConfigs.containsKey(tag)) {
                return tagConfigs.get(tag);
            }
        }

        return null;
    }

    public void validate() {
        if (blockConfigs != null) {
            blockConfigs.values().forEach(OreXPValues::validate);
        }
        if (tagConfigs != null) {
            tagConfigs.values().forEach(OreXPValues::validate);
        }
    }
}