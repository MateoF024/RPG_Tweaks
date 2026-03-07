package org.mateof24.rpg_tweaks.item;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootSackResolver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Random RANDOM = new Random();
    private static final java.util.Map<ResourceLocation, JsonObject> TABLE_CACHE = new java.util.HashMap<>();

    public static List<ItemStack> resolve(ResourceLocation lootTableLocation) {
        if (!TABLE_CACHE.containsKey(lootTableLocation)) {
            String resourcePath = "/data/" + lootTableLocation.getNamespace()
                    + "/loot_table/" + lootTableLocation.getPath() + ".json";
            try (InputStream is = LootSackResolver.class.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    TABLE_CACHE.put(lootTableLocation,
                            GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class));
                } else {
                    LOGGER.error("[LootSack] Resource not found: {}", resourcePath);
                    return List.of();
                }
            } catch (Exception e) {
                LOGGER.error("[LootSack] Error reading loot table {}: {}", resourcePath, e.getMessage());
                return List.of();
            }
        }
        JsonObject root = TABLE_CACHE.get(lootTableLocation);
        return root != null ? parseAndRoll(root) : List.of();
    }

    private static List<ItemStack> parseAndRoll(JsonObject root) {
        List<ItemStack> result = new ArrayList<>();
        if (!root.has("pools")) return result;

        for (JsonElement poolEl : root.getAsJsonArray("pools")) {
            JsonObject pool = poolEl.getAsJsonObject();

            int rolls;
            if (pool.has("rolls") && pool.get("rolls").isJsonObject()) {
                JsonObject rollObj = pool.getAsJsonObject("rolls");
                int min = rollObj.has("min") ? rollObj.get("min").getAsInt() : 1;
                int max = rollObj.has("max") ? rollObj.get("max").getAsInt() : 1;
                rolls = min + (max > min ? RANDOM.nextInt(max - min + 1) : 0);
            } else {
                rolls = pool.has("rolls") ? pool.get("rolls").getAsInt() : 1;
            }

            if (!pool.has("entries")) continue;

            List<WeightedItem> weighted = new ArrayList<>();
            int totalWeight = 0;

            for (JsonElement entryEl : pool.getAsJsonArray("entries")) {
                JsonObject entry = entryEl.getAsJsonObject();
                if (!entry.has("name")) continue;
                String itemId = entry.get("name").getAsString();
                int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;

                int countMin = 1;
                int countMax = 1;
                if (entry.has("count")) {
                    JsonElement countEl = entry.get("count");
                    if (countEl.isJsonObject()) {
                        JsonObject countObj = countEl.getAsJsonObject();
                        countMin = countObj.has("min") ? countObj.get("min").getAsInt() : 1;
                        countMax = countObj.has("max") ? countObj.get("max").getAsInt() : 1;
                    } else {
                        countMin = countMax = countEl.getAsInt();
                    }
                }

                weighted.add(new WeightedItem(itemId, weight, countMin, countMax));
                totalWeight += weight;
            }

            if (weighted.isEmpty() || totalWeight == 0) continue;

            for (int i = 0; i < rolls; i++) {
                int roll = RANDOM.nextInt(totalWeight);
                int cumulative = 0;
                for (WeightedItem wi : weighted) {
                    cumulative += wi.weight;
                    if (roll < cumulative) {
                        int count = wi.countMin + (wi.countMax > wi.countMin ? RANDOM.nextInt(wi.countMax - wi.countMin + 1) : 0);
                        ItemStack stack = createStack(wi.itemId, count);
                        if (!stack.isEmpty()) result.add(stack);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static ItemStack createStack(String itemId, int count) {
        try {
            String[] parts = itemId.split(":", 2);
            if (parts.length != 2) return ItemStack.EMPTY;
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);

            Item item = null;

            try {
                java.util.Optional<?> opt = BuiltInRegistries.ITEM.getOptional(rl);
                if (opt.isPresent() && opt.get() instanceof Item i) item = i;
            } catch (Throwable ignored) {}

            if (item == null) {
                for (java.lang.reflect.Method m : BuiltInRegistries.ITEM.getClass().getMethods()) {
                    if (!m.getName().equals("get") || m.getParameterCount() != 1) continue;
                    if (!m.getParameterTypes()[0].isAssignableFrom(ResourceLocation.class)) continue;
                    try {
                        Object result = m.invoke(BuiltInRegistries.ITEM, rl);
                        if (result instanceof Item i) { item = i; break; }
                    } catch (Throwable ignored) {}
                }
            }

            if (item == null || item == Items.AIR) {
                LOGGER.warn("[LootSack] Unknown item: {}", itemId);
                return ItemStack.EMPTY;
            }

            ItemStack stack = new ItemStack(item);
            stack.setCount(Math.min(count, item.getDefaultMaxStackSize()));
            return stack;
        } catch (Exception e) {
            LOGGER.error("[LootSack] Error creating stack for {}: {}", itemId, e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    private record WeightedItem(String itemId, int weight, int countMin, int countMax) {}
}