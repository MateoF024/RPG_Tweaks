package org.mateof24.rpg_tweaks.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.item.LootSackItem;

import java.util.List;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class LootSackUseHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide()) return;
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof LootSackItem sack)) return;

        Player player = event.getEntity();
        ServerLevel serverLevel = (ServerLevel) player.level();

        ResourceKey<LootTable> key = sack.getLootTableKey();
        LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(serverLevel).create(LootContextParamSets.EMPTY);
        List<ItemStack> drops = lootTable.getRandomItems(params);

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
}