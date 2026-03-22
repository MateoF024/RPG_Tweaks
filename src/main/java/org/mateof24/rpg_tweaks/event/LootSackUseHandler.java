package org.mateof24.rpg_tweaks.event;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.item.LootSackItem;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class LootSackUseHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Random RANDOM = new Random();
    private static final java.util.Map<ResourceLocation, JsonObject> TABLE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide()) return;
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof LootSackItem sack)) return;

        Player player = event.getEntity();
        ServerLevel serverLevel = (ServerLevel) player.level();

        ResourceLocation location = sack.getLootTableKey().location();
        List<ItemStack> drops = resolve(location);

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(serverLevel,
                    player.getX(), player.getY() + 0.5, player.getZ(), drop.copy());
            entity.setPickUpDelay(10);
            serverLevel.addFreshEntity(entity);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        event.setCanceled(true);
    }

    private static List<ItemStack> resolve(ResourceLocation location) {
        if (!TABLE_CACHE.containsKey(location)) {
            String path = "/data/" + location.getNamespace() + "/loot_table/" + location.getPath() + ".json";
            try (InputStream is = LootSackUseHandler.class.getResourceAsStream(path)) {
                if (is != null) {
                    TABLE_CACHE.put(location, GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class));
                } else {
                    LOGGER.error("[LootSack] Resource not found: {}", path);
                    return List.of();
                }
            } catch (Exception e) {
                LOGGER.error("[LootSack] Error reading {}: {}", path, e.getMessage());
                return List.of();
            }
        }
        JsonObject root = TABLE_CACHE.get(location);
        return root != null ? roll(root) : List.of();
    }

    private static List<ItemStack> roll(JsonObject root) {
        List<ItemStack> result = new ArrayList<>();
        if (!root.has("pools")) return result;

        for (JsonElement poolEl : root.getAsJsonArray("pools")) {
            JsonObject pool = poolEl.getAsJsonObject();
            int rolls;
            if (pool.has("rolls") && pool.get("rolls").isJsonObject()) {
                JsonObject r = pool.getAsJsonObject("rolls");
                int min = r.has("min") ? r.get("min").getAsInt() : 1;
                int max = r.has("max") ? r.get("max").getAsInt() : 1;
                rolls = min + (max > min ? RANDOM.nextInt(max - min + 1) : 0);
            } else {
                rolls = pool.has("rolls") ? pool.get("rolls").getAsInt() : 1;
            }
            if (!pool.has("entries")) continue;

            List<int[]> weighted = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            List<int[]> counts = new ArrayList<>();
            int total = 0;

            for (JsonElement entryEl : pool.getAsJsonArray("entries")) {
                JsonObject entry = entryEl.getAsJsonObject();
                if (!entry.has("name")) continue;
                String itemId = entry.get("name").getAsString();
                int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
                int countMin = 1, countMax = 1;
                if (entry.has("count")) {
                    JsonElement countEl = entry.get("count");
                    if (countEl.isJsonObject()) {
                        JsonObject co = countEl.getAsJsonObject();
                        countMin = co.has("min") ? co.get("min").getAsInt() : 1;
                        countMax = co.has("max") ? co.get("max").getAsInt() : 1;
                    } else {
                        countMin = countMax = countEl.getAsInt();
                    }
                }
                ids.add(itemId);
                weighted.add(new int[]{total, total + weight});
                counts.add(new int[]{countMin, countMax});
                total += weight;
            }

            if (total == 0) continue;

            for (int i = 0; i < rolls; i++) {
                int r = RANDOM.nextInt(total);
                for (int j = 0; j < ids.size(); j++) {
                    if (r >= weighted.get(j)[0] && r < weighted.get(j)[1]) {
                        int[] c = counts.get(j);
                        int count = c[0] + (c[1] > c[0] ? RANDOM.nextInt(c[1] - c[0] + 1) : 0);
                        ItemStack s = createStack(ids.get(j), count);
                        if (!s.isEmpty()) result.add(s);
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
            Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item == null || item == Items.AIR) return ItemStack.EMPTY;
            ItemStack stack = new ItemStack(item);
            stack.setCount(Math.min(count, item.getDefaultMaxStackSize()));
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}