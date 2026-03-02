package org.mateof24.rpg_tweaks.event;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.item.LootSackItem;
import org.mateof24.rpg_tweaks.item.LootSackResolver;
import org.slf4j.Logger;

import java.util.List;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class LootSackUseHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide()) return;
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof LootSackItem sack)) return;

        Player player = event.getEntity();
        ServerLevel serverLevel = (ServerLevel) player.level();

        List<ItemStack> drops = LootSackResolver.resolve(sack.getLootTableLocation());
        LOGGER.debug("[LootSack] Opening {} - {} items rolled", sack.getLootTableLocation(), drops.size());

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