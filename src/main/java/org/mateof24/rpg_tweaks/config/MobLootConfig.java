package org.mateof24.rpg_tweaks.config;

import java.util.*;

public class MobLootConfig {

    public Map<String, MobSackDrops> mobDrops = new LinkedHashMap<>();

    public static class SackTierConfig {
        public float chance = 0f;
        public String requiredDimension = "";
        public String requiredBiome = "";
        public boolean onlyAtNight = false;
        public List<Integer> moonPhases = new ArrayList<>();
        public float minDistanceFromPlayer = 0f;
        public boolean onlySurface = false;
        public boolean onlyCave = false;

        public void ensureDefaults() {
            if (moonPhases == null) moonPhases = new ArrayList<>();
            if (requiredDimension == null) requiredDimension = "";
            if (requiredBiome == null) requiredBiome = "";
        }
    }

    public static class MobSackDrops {
        public SackTierConfig common = new SackTierConfig();
        public SackTierConfig uncommon = new SackTierConfig();
        public SackTierConfig rare = new SackTierConfig();
        public SackTierConfig epic = new SackTierConfig();
        public SackTierConfig legendary = new SackTierConfig();
        public List<String> removedDrops = new ArrayList<>();
        public XPValues xpOnKill = new XPValues(0, 0);

        public void ensureDefaults() {
            if (common == null) common = new SackTierConfig();
            if (uncommon == null) uncommon = new SackTierConfig();
            if (rare == null) rare = new SackTierConfig();
            if (epic == null) epic = new SackTierConfig();
            if (legendary == null) legendary = new SackTierConfig();
            if (removedDrops == null) removedDrops = new ArrayList<>();
            if (xpOnKill == null) xpOnKill = new XPValues(0, 0);
            common.ensureDefaults();
            uncommon.ensureDefaults();
            rare.ensureDefaults();
            epic.ensureDefaults();
            legendary.ensureDefaults();
        }
    }

    public void initDefaults() {
        if (mobDrops.isEmpty()) {
            for (String mobId : getDefaultHostileMobs()) {
                MobSackDrops drops = new MobSackDrops();
                drops.ensureDefaults();
                mobDrops.put(mobId, drops);
            }
        }
    }

    private static List<String> getDefaultHostileMobs() {
        return List.of(
                "minecraft:blaze",
                "minecraft:bogged",
                "minecraft:breeze",
                "minecraft:cave_spider",
                "minecraft:creaking",
                "minecraft:creeper",
                "minecraft:drowned",
                "minecraft:elder_guardian",
                "minecraft:enderman",
                "minecraft:endermite",
                "minecraft:evoker",
                "minecraft:ghast",
                "minecraft:guardian",
                "minecraft:hoglin",
                "minecraft:husk",
                "minecraft:illusioner",
                "minecraft:magma_cube",
                "minecraft:phantom",
                "minecraft:piglin",
                "minecraft:piglin_brute",
                "minecraft:pillager",
                "minecraft:ravager",
                "minecraft:shulker",
                "minecraft:silverfish",
                "minecraft:skeleton",
                "minecraft:slime",
                "minecraft:spider",
                "minecraft:stray",
                "minecraft:vex",
                "minecraft:vindicator",
                "minecraft:warden",
                "minecraft:witch",
                "minecraft:wither_skeleton",
                "minecraft:zoglin",
                "minecraft:zombie",
                "minecraft:zombie_villager"
        );
    }
}