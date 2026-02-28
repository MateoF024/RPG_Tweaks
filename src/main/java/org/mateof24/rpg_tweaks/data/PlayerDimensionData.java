package org.mateof24.rpg_tweaks.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDimensionData {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_PATH = FMLPaths.CONFIGDIR.get().resolve("rpg_tweaks_player_dimensions.json");

    private static Map<String, Set<String>> blocked = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> allowed = new ConcurrentHashMap<>();
    private static Map<String, String> uuidToName = new ConcurrentHashMap<>();
    private static Map<String, PendingEntry> pending = new ConcurrentHashMap<>();

    public static class PendingEntry {
        public final String dimension;
        public final boolean isAllow;
        public PendingEntry(String dimension, boolean isAllow) {
            this.dimension = dimension;
            this.isAllow = isAllow;
        }
    }

    public static void load() {
        blocked = new ConcurrentHashMap<>();
        allowed = new ConcurrentHashMap<>();
        uuidToName = new ConcurrentHashMap<>();
        pending = new ConcurrentHashMap<>();

        if (!Files.exists(DATA_PATH)) return;

        try {
            String json = Files.readString(DATA_PATH);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("names")) {
                root.getAsJsonObject("names").entrySet()
                        .forEach(e -> uuidToName.put(e.getKey(), e.getValue().getAsString()));
            }
            if (root.has("blocked")) {
                root.getAsJsonObject("blocked").entrySet().forEach(e -> {
                    Set<String> dims = new HashSet<>();
                    e.getValue().getAsJsonArray().forEach(el -> dims.add(el.getAsString()));
                    blocked.put(e.getKey(), dims);
                });
            }
            if (root.has("allowed")) {
                root.getAsJsonObject("allowed").entrySet().forEach(e -> {
                    Set<String> dims = new HashSet<>();
                    e.getValue().getAsJsonArray().forEach(el -> dims.add(el.getAsString()));
                    allowed.put(e.getKey(), dims);
                });
            }
            if (root.has("pending")) {
                root.getAsJsonObject("pending").entrySet().forEach(e -> {
                    JsonObject entry = e.getValue().getAsJsonObject();
                    pending.put(e.getKey(), new PendingEntry(
                            entry.get("dimension").getAsString(),
                            entry.get("isAllow").getAsBoolean()
                    ));
                });
            }
        } catch (Exception e) {
            LOGGER.error("[PlayerDimensionData] Error loading: {}", e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(DATA_PATH.getParent());
            JsonObject root = new JsonObject();

            JsonObject namesObj = new JsonObject();
            uuidToName.forEach(namesObj::addProperty);
            root.add("names", namesObj);

            JsonObject blockedObj = new JsonObject();
            blocked.forEach((uuid, dims) -> {
                JsonArray arr = new JsonArray();
                dims.forEach(arr::add);
                blockedObj.add(uuid, arr);
            });
            root.add("blocked", blockedObj);

            JsonObject allowedObj = new JsonObject();
            allowed.forEach((uuid, dims) -> {
                JsonArray arr = new JsonArray();
                dims.forEach(arr::add);
                allowedObj.add(uuid, arr);
            });
            root.add("allowed", allowedObj);

            JsonObject pendingObj = new JsonObject();
            pending.forEach((name, entry) -> {
                JsonObject e = new JsonObject();
                e.addProperty("dimension", entry.dimension);
                e.addProperty("isAllow", entry.isAllow);
                pendingObj.add(name, e);
            });
            root.add("pending", pendingObj);

            Files.writeString(DATA_PATH, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.error("[PlayerDimensionData] Error saving: {}", e.getMessage());
        }
    }

    public static void setDimension(UUID playerUUID, String playerName, String dimension, boolean isAllow) {
        String uuid = playerUUID.toString();
        uuidToName.put(uuid, playerName);
        if (isAllow) {
            allowed.computeIfAbsent(uuid, k -> new HashSet<>()).add(dimension);
            Set<String> b = blocked.get(uuid);
            if (b != null) b.remove(dimension);
        } else {
            blocked.computeIfAbsent(uuid, k -> new HashSet<>()).add(dimension);
            Set<String> a = allowed.get(uuid);
            if (a != null) a.remove(dimension);
        }
        cleanupPlayer(uuid);
        save();
    }

    public static void removeDimension(UUID playerUUID, String dimension) {
        String uuid = playerUUID.toString();
        Set<String> b = blocked.get(uuid);
        if (b != null) b.remove(dimension);
        Set<String> a = allowed.get(uuid);
        if (a != null) a.remove(dimension);
        cleanupPlayer(uuid);
        save();
    }

    public static void removeAllForPlayer(UUID playerUUID) {
        String uuid = playerUUID.toString();
        blocked.remove(uuid);
        allowed.remove(uuid);
        uuidToName.remove(uuid);
        save();
    }

    public static void addPending(String playerName, String dimension, boolean isAllow) {
        pending.put(playerName.toLowerCase(), new PendingEntry(dimension, isAllow));
        save();
        LOGGER.info("[PlayerDimensionData] Pending exception for '{}': {} {}", playerName, isAllow ? "allow" : "block", dimension);
    }

    public static void resolvePending(UUID playerUUID, String playerName) {
        PendingEntry entry = pending.remove(playerName.toLowerCase());
        if (entry == null) return;
        setDimension(playerUUID, playerName, entry.dimension, entry.isAllow);
        LOGGER.info("[PlayerDimensionData] Resolved pending for {}: {} {}", playerName, entry.isAllow ? "allow" : "block", entry.dimension);
    }

    public static boolean isBlocked(UUID playerUUID, String dimension) {
        String uuid = playerUUID.toString();
        Set<String> a = allowed.get(uuid);
        if (a != null && a.contains(dimension)) return false;
        Set<String> b = blocked.get(uuid);
        return b != null && b.contains(dimension);
    }

    public static boolean isAllowed(UUID playerUUID, String dimension) {
        Set<String> a = allowed.get(uuid(playerUUID));
        return a != null && a.contains(dimension);
    }

    public static Set<String> getBlockedDimensions(UUID playerUUID) {
        return Collections.unmodifiableSet(blocked.getOrDefault(uuid(playerUUID), Collections.emptySet()));
    }

    public static Set<String> getAllowedDimensions(UUID playerUUID) {
        return Collections.unmodifiableSet(allowed.getOrDefault(uuid(playerUUID), Collections.emptySet()));
    }

    public static Set<String> getAllConfiguredPlayers() {
        Set<String> all = new HashSet<>();
        all.addAll(blocked.keySet());
        all.addAll(allowed.keySet());
        return Collections.unmodifiableSet(all);
    }

    public static Map<String, PendingEntry> getPending() {
        return Collections.unmodifiableMap(pending);
    }

    public static String getPlayerName(String uuid) {
        return uuidToName.getOrDefault(uuid, uuid);
    }

    private static String uuid(UUID playerUUID) {
        return playerUUID.toString();
    }

    private static void cleanupPlayer(String uuid) {
        Set<String> b = blocked.get(uuid);
        Set<String> a = allowed.get(uuid);
        boolean emptyB = b == null || b.isEmpty();
        boolean emptyA = a == null || a.isEmpty();
        if (emptyB && emptyA) {
            blocked.remove(uuid);
            allowed.remove(uuid);
            uuidToName.remove(uuid);
        }
        if (emptyB) blocked.remove(uuid);
        if (emptyA) allowed.remove(uuid);
    }

    public static void removePending(String playerName) {
        pending.remove(playerName.toLowerCase());
        save();
    }
}