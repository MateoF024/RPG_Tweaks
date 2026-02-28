package org.mateof24.rpg_tweaks.config;

import java.util.*;

public class MobLootConfig {

    public Map<String, MobSackDrops> mobDrops = new LinkedHashMap<>();

    public static class MobSackDrops {
        public float commonChance = 0f;
        public float uncommonChance = 0f;
        public float rareChance = 0f;
        public float epicChance = 0f;
        public float legendaryChance = 0f;
        public List<String> removedDrops = new ArrayList<>();
    }

    public void initDefaults() {
        if (mobDrops.isEmpty()) {
            MobSackDrops zombie = new MobSackDrops();
            zombie.commonChance = 30f;
            zombie.uncommonChance = 15f;
            zombie.rareChance = 5f;
            zombie.epicChance = 1f;
            zombie.legendaryChance = 0.1f;
            zombie.removedDrops = new ArrayList<>(List.of(
                    "minecraft:iron_ingot",
                    "minecraft:potato",
                    "minecraft:carrot"
            ));
            mobDrops.put("minecraft:zombie", zombie);
        }
    }
}