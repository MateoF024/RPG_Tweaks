package org.mateof24.rpg_tweaks.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ReskillableConfigManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path RESKILLABLE_CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("reskillable");
    private static final Path SKILL_LOCKS_PATH = RESKILLABLE_CONFIG_DIR.resolve("skill_locks.json");
    private static final Path CRAFT_SKILL_LOCKS_PATH = RESKILLABLE_CONFIG_DIR.resolve("craft_skill_locks.json");
    private static final Path ATTACK_SKILL_LOCKS_PATH = RESKILLABLE_CONFIG_DIR.resolve("attack_skill_locks.json");

    public enum ConfigType {
        SKILL_LOCKS("skill_locks.json", "skillLocks"),
        CRAFT_LOCKS("craft_skill_locks.json", "craftSkillLocks"),
        ATTACK_LOCKS("attack_skill_locks.json", "attackSkillLocks");

        public final String filename;
        public final String jsonKey;

        ConfigType(String filename, String jsonKey) {
            this.filename = filename;
            this.jsonKey = jsonKey;
        }

        public Path getPath() {
            return RESKILLABLE_CONFIG_DIR.resolve(filename);
        }

        public String getJsonKey() {
            return jsonKey;
        }
    }


    public static boolean isReskillableInstalled() {
        return Files.exists(RESKILLABLE_CONFIG_DIR);
    }


    private static void ensureConfigDirectoryExists() throws IOException {
        if (!Files.exists(RESKILLABLE_CONFIG_DIR)) {
            Files.createDirectories(RESKILLABLE_CONFIG_DIR);
            LOGGER.info("Reskillable configuration directory created: {}", RESKILLABLE_CONFIG_DIR);
        }
    }

    private static void ensureConfigFileExists(ConfigType type) throws IOException {
        Path configPath = type.getPath();

        if (!Files.exists(configPath)) {
            JsonObject root = new JsonObject();
            JsonObject locks = new JsonObject();
            root.add(type.getJsonKey(), locks);

            String json = GSON.toJson(root);
            Files.writeString(configPath, json);

            LOGGER.info("Reskillable configuration file created: {}", configPath);
        }
    }


    public static boolean addItemSkillLock(ConfigType type, String itemId, List<String> skillRequirements) {
        try {
            ensureConfigDirectoryExists();
            ensureConfigFileExists(type);

            Path configPath = type.getPath();

            String jsonContent = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();

            JsonObject locks = root.getAsJsonObject(type.getJsonKey());
            if (locks == null) {
                locks = new JsonObject();
                root.add(type.getJsonKey(), locks);
            }

            JsonArray requirementsArray = new JsonArray();
            for (String requirement : skillRequirements) {
                requirementsArray.add(requirement);
            }

            locks.add(itemId, requirementsArray);

            String updatedJson = GSON.toJson(root);
            Files.writeString(configPath, updatedJson);

            LOGGER.info("Item '{}' added to {} with requirements: {}",
                    itemId, type.filename, skillRequirements);

            return true;

        } catch (IOException e) {
            LOGGER.error("Error adding item to {}: {}", type.filename, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing {}: {}", type.filename, e.getMessage(), e);
            return false;
        }
    }

    public static List<String> getItemSkillLock(ConfigType type, String itemId) {
        try {
            Path configPath = type.getPath();

            if (!Files.exists(configPath)) {
                return null;
            }

            String jsonContent = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonObject locks = root.getAsJsonObject(type.getJsonKey());

            if (locks == null || !locks.has(itemId)) {
                return null;
            }

            JsonArray requirementsArray = locks.getAsJsonArray(itemId);
            List<String> requirements = new ArrayList<>();

            requirementsArray.forEach(element -> requirements.add(element.getAsString()));

            return requirements;

        } catch (Exception e) {
            LOGGER.error("Error reading requirements of '{}': {}", itemId, e.getMessage());
            return null;
        }
    }

    public static boolean removeItemSkillLock(ConfigType type, String itemId) {
        try {
            Path configPath = type.getPath();

            if (!Files.exists(configPath)) {
                return false;
            }

            String jsonContent = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonObject locks = root.getAsJsonObject(type.getJsonKey());

            if (locks == null || !locks.has(itemId)) {
                return false;
            }

            locks.remove(itemId);

            String updatedJson = GSON.toJson(root);
            Files.writeString(configPath, updatedJson);

            LOGGER.info("Item '{}' removed from {}", itemId, type.filename);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error deleting item from {}: {}", type.filename, e.getMessage());
            return false;
        }
    }

    public static boolean isValidSkillRequirement(String requirement) {
        if (requirement == null || !requirement.contains(":")) {
            return false;
        }

        String[] parts = requirement.split(":");
        if (parts.length != 2) {
            return false;
        }

        String skill = parts[0].toLowerCase();
        String levelStr = parts[1];

        List<String> validSkills = List.of("attack", "defense", "mining", "gathering",
                "farming", "building", "agility", "magic");

        if (!validSkills.contains(skill)) {
            return false;
        }

        try {
            int level = Integer.parseInt(levelStr);
            return level > 0 && level <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidItemId(String itemId) {
        if (itemId == null || !itemId.contains(":")) {
            return false;
        }

        String[] parts = itemId.split(":");
        return parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty();
    }
}