package org.mateof24.rpg_tweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.mateof24.rpg_tweaks.config.MobLootConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;


public class ModConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("rpg_tweaks.json");

    private static ModConfig INSTANCE;

    // Advancements System
    public boolean blockAdvancementXP = true;
    public boolean logXPBlocking = false;
    public int advancementXPReward = 0;
    public int goalXPReward = 0;
    public int challengeXPReward = 0;

    // Ores System
    public boolean enableCustomOreXP = true;
    public float oreXPFortuneBonus = 0.0f;
    public boolean logOreXP = false;
    public OreXPConfig oreXPConfig = new OreXPConfig();

    // Player System
    public float exhaustionRateMultiplier = 1.0f;
    public float naturalRegenRateMultiplier = 1.0f;
    public float durabilityMultiplier = 0f;
    public int maxStorableXP = 0;

    // PvP System
    public boolean pvpEnabled = true;

    // Sleep System
    public int sleepFromNight = 0;
    public float sleepHealPercent = 0f;
    public int sleepHungerPoints = 0;
    public float sleepHungerChance = 0f;

    // Chat System
    public boolean chatEnabled = true;
    public java.util.List<String> chatBlockedPlayers = new ArrayList<>();

    // Death System
    public float deathXPLossPercent = 0f;
    public boolean deathRespawnEnabled = false;
    public int deathRespawnMinDistance = 128;
    public int deathRespawnMaxDistance = 320;

    // Mobs Systems
    public MobLootConfig mobLootConfig = new MobLootConfig();
    public float lootSackLootingBonus = 5.0f;
    public boolean enableMobXP = false;
    public float mobXPLootingBonus = 0f;

    // Dimension System
    public Map<String, String> blockedDimensions = new LinkedHashMap<>();



    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    // Default Ores XP Values
    private void initializeDefaultOreXP() {
        if (oreXPConfig == null) {
            oreXPConfig = new OreXPConfig();
        }

        if (oreXPConfig.blockConfigs.isEmpty() && oreXPConfig.tagConfigs.isEmpty()) {
            oreXPConfig.tagConfigs.put("minecraft:coal_ores", new XPValues(0, 4));
            oreXPConfig.tagConfigs.put("minecraft:copper_ores", new XPValues(1, 4));
            oreXPConfig.tagConfigs.put("minecraft:iron_ores", new XPValues(2, 5));
            oreXPConfig.tagConfigs.put("minecraft:gold_ores", new XPValues(3, 5));
            oreXPConfig.tagConfigs.put("minecraft:redstone_ores", new XPValues(5, 7));
            oreXPConfig.tagConfigs.put("minecraft:lapis_ores", new XPValues(5, 8));
            oreXPConfig.tagConfigs.put("minecraft:diamond_ores", new XPValues(5, 10));
            oreXPConfig.tagConfigs.put("minecraft:emerald_ores", new XPValues(7, 12));
            oreXPConfig.blockConfigs.put("minecraft:nether_quartz_ore", new XPValues(8, 16));

            LOGGER.info("Default mineral configuration initialized with tags");
        }
    }

    // Load config files
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
                LOGGER.info("Configuration loaded from: {}", CONFIG_PATH);

                INSTANCE.validate();
                if (INSTANCE.chatBlockedPlayers == null) INSTANCE.chatBlockedPlayers = new ArrayList<>();
                for (MobLootConfig.MobSackDrops drops : INSTANCE.mobLootConfig.mobDrops.values()) {
                    drops.ensureDefaults();
                }
                INSTANCE.initializeDefaultOreXP();
            } catch (Exception e) {
                LOGGER.error("Error loading configuration, using default values.", e);
                INSTANCE = new ModConfig();
                INSTANCE.initializeDefaultOreXP();
                save();
            }
        } else {
            LOGGER.info("Configuration file not found, creating a new one with default values.");
            INSTANCE = new ModConfig();
            INSTANCE.initializeDefaultOreXP();
            INSTANCE.mobLootConfig.initDefaults();
            save();
        }
    }

    // Save config files
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            String json = GSON.toJson(INSTANCE);
            Files.writeString(CONFIG_PATH, json);

            LOGGER.info("Configuration saved in: {}", CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.error("Error saving configuration.", e);
        }
    }

    // Validate Config Files for Ore and Mob systems
    private void validate() {
        if (oreXPConfig != null) {
            oreXPConfig.validate();
        }
        if (mobLootConfig != null) {
            for (MobLootConfig.MobSackDrops drops : mobLootConfig.mobDrops.values()) {
                if (drops.xpOnKill != null) drops.xpOnKill.validate();
            }
        }
    }

    public static void reload() {
        LOGGER.info("Reloading settings...");
        load();
        org.mateof24.rpg_tweaks.event.OreXPHandler.invalidateCache();
        org.mateof24.rpg_tweaks.integration.ReskillableSkillCache.load();
    }
}