package org.mateof24.rpg_tweaks.item;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.slf4j.Logger;

import java.util.List;

public class LootSackItem extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceKey<LootTable> lootTableKey;
    private final ChatFormatting nameColor;

    public LootSackItem(Properties properties, ResourceKey<LootTable> lootTableKey, ChatFormatting nameColor) {
        super(properties);
        this.lootTableKey = lootTableKey;
        this.nameColor = nameColor;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(nameColor);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) return InteractionResultHolder.success(stack);

        ServerLevel serverLevel = (ServerLevel) level;
        LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(lootTableKey);

        if (table == LootTable.EMPTY) {
            LOGGER.error("[LootSack] Loot table not found: {}", lootTableKey.location());
            return InteractionResultHolder.fail(stack);
        }

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.GIFT);

        List<ItemStack> drops = table.getRandomItems(params);
        LOGGER.debug("[LootSack] Opening {} - {} items rolled", lootTableKey.location(), drops.size());

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(serverLevel,
                    player.getX(), player.getY() + 0.5, player.getZ(), drop.copy());
            entity.setPickUpDelay(10);
            serverLevel.addFreshEntity(entity);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);

        return InteractionResultHolder.consume(stack);
    }
}