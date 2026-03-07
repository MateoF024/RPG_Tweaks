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

    public boolean blockAdvancementXP = true;
    public boolean logXPBlocking = false;
    public int advancementXPReward = 0;
    public int goalXPReward = 0;
    public int challengeXPReward = 0;

    public boolean enableCustomOreXP = true;
    public float oreXPFortuneBonus = 0.0f;
    public boolean logOreXP = false;
    public OreXPConfig oreXPConfig = new OreXPConfig();

    public float exhaustionRateMultiplier = 1.0f;
    public float naturalRegenRateMultiplier = 1.0f;
    public float durabilityMultiplier = 0f;
    public int maxStorableXP = 0;

    public int sleepFromNight = 0;
    public float sleepHealPercent = 0f;
    public int sleepHungerPoints = 0;
    public float sleepHungerChance = 0f;

    public boolean pvpEnabled = true;
    public Map<String, String> blockedDimensions = new LinkedHashMap<>();

    public MobLootConfig mobLootConfig = new MobLootConfig();
    public float lootSackLootingBonus = 5.0f;

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    private void initializeDefaultOreXP() {
        if (oreXPConfig == null) {
            oreXPConfig = new OreXPConfig();
        }

        if (oreXPConfig.blockConfigs.isEmpty() && oreXPConfig.tagConfigs.isEmpty()) {

            // Coal
            oreXPConfig.tagConfigs.put("minecraft:coal_ores", new OreXPConfig.OreXPValues(1, 3));

            // Copper
            oreXPConfig.tagConfigs.put("minecraft:copper_ores", new OreXPConfig.OreXPValues(1, 3));

            // Iron
            oreXPConfig.tagConfigs.put("minecraft:iron_ores", new OreXPConfig.OreXPValues(2, 4));

            // Gold
            oreXPConfig.tagConfigs.put("minecraft:gold_ores", new OreXPConfig.OreXPValues(2, 5));

            // Redstone
            oreXPConfig.tagConfigs.put("minecraft:redstone_ores", new OreXPConfig.OreXPValues(3, 7));

            // Lapis
            oreXPConfig.tagConfigs.put("minecraft:lapis_ores", new OreXPConfig.OreXPValues(4, 8));

            // Diamond
            oreXPConfig.tagConfigs.put("minecraft:diamond_ores", new OreXPConfig.OreXPValues(5, 12));

            // Emerald
            oreXPConfig.tagConfigs.put("minecraft:emerald_ores", new OreXPConfig.OreXPValues(5, 12));

            // Quartz
            oreXPConfig.blockConfigs.put("minecraft:nether_quartz_ore", new OreXPConfig.OreXPValues(7, 15));

            LOGGER.info("Default mineral configuration initialized with tags");
        }
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
                LOGGER.info("Configuration loaded from: {}", CONFIG_PATH);

                INSTANCE.validate();

                INSTANCE.initializeDefaultOreXP();
                INSTANCE.mobLootConfig.initDefaults();
            } catch (Exception e) {
                LOGGER.error("Error loading configuration, using default values.", e);
                INSTANCE = new ModConfig();
                INSTANCE.initializeDefaultOreXP();
                INSTANCE.mobLootConfig.initDefaults();
                save();
            }
        } else {
            LOGGER.info("Configuration file not found, creating a new one with default values.");
            INSTANCE = new ModConfig();
            INSTANCE.initializeDefaultOreXP();
            save();
        }
    }

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

    private void validate() {
        if (oreXPConfig != null) {
            oreXPConfig.validate();
        }
    }

    public static void reload() {
        LOGGER.info("Reloading settings...");
        load();
        org.mateof24.rpg_tweaks.event.OreXPHandler.invalidateCache();
    }
}