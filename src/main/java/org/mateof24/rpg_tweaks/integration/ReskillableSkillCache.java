package org.mateof24.rpg_tweaks.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ReskillableSkillCache {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("reskillable");

    private static Map<String, Map<Integer, List<String>>> skillLocks = new HashMap<>();

    public static void load() {
        skillLocks = new HashMap<>();
        if (!ReskillableConfigManager.isReskillableInstalled()) return;

        Path path = CONFIG_DIR.resolve("skill_locks.json");
        if (!Files.exists(path)) {
            LOGGER.warn("[RPGTweaks/ReskillableCache] skill_locks.json not found at {}", path);
            return;
        }

        try {
            String raw = Files.readString(path);
            LOGGER.info("[RPGTweaks/ReskillableCache] Raw skill_locks.json content (first 500 chars): {}",
                    raw.substring(0, Math.min(500, raw.length())));

            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            LOGGER.info("[RPGTweaks/ReskillableCache] Root keys: {}", root.keySet());

            JsonObject locks = null;
            for (String key : List.of("skillLocks", "skill_locks", "locks", "skills")) {
                if (root.has(key) && root.get(key).isJsonObject()) {
                    locks = root.getAsJsonObject(key);
                    LOGGER.info("[RPGTweaks/ReskillableCache] Using JSON key '{}'", key);
                    break;
                }
            }
            if (locks == null) {
                if (root.size() > 0 && root.entrySet().iterator().next().getValue().isJsonArray()) {
                    locks = root;
                    LOGGER.info("[RPGTweaks/ReskillableCache] Using root object directly as lock map");
                }
            }
            if (locks == null) {
                LOGGER.warn("[RPGTweaks/ReskillableCache] Could not find skill lock entries in JSON. Keys found: {}", root.keySet());
                return;
            }

            int total = 0;
            for (var entry : locks.entrySet()) {
                String itemId = entry.getKey();
                if (!entry.getValue().isJsonArray()) continue;
                JsonArray reqs = entry.getValue().getAsJsonArray();
                for (var el : reqs) {
                    String req = el.getAsString();
                    String[] parts = req.split(":", 2);
                    if (parts.length != 2) continue;
                    String skill = parts[0].toLowerCase(Locale.ROOT);
                    try {
                        int level = Integer.parseInt(parts[1]);
                        skillLocks.computeIfAbsent(skill, s -> new HashMap<>())
                                .computeIfAbsent(level, l -> new ArrayList<>())
                                .add(itemId);
                        total++;
                    } catch (NumberFormatException ignored) {}
                }
            }
            LOGGER.info("[RPGTweaks/ReskillableCache] Loaded {} item-skill entries from skill_locks.json", total);
        } catch (Exception e) {
            LOGGER.error("[RPGTweaks/ReskillableCache] Error loading skill_locks.json: {}", e.getMessage(), e);
        }
    }

    public static List<String> getUnlockedItems(String skill, int level) {
        return skillLocks.getOrDefault(skill, Map.of()).getOrDefault(level, List.of());
    }

    public static boolean isEmpty() {
        return skillLocks.isEmpty();
    }
}