package org.mateof24.rpg_tweaks.item;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

import java.util.List;

public class LootSackItem extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceKey<LootTable> lootTableKey;
    private final boolean foil;
    private final ChatFormatting nameColor;

    public static Item.Properties createProperties(int stackSize, String namespace, String path) {
        Item.Properties props = new Item.Properties().stacksTo(stackSize);
        try {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(namespace, path));
            java.lang.reflect.Method setId = Item.Properties.class.getMethod("setId", ResourceKey.class);
            setId.invoke(props, key);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception ignored) {
        }
        return props;
    }

    public LootSackItem(Properties properties, ResourceKey<LootTable> lootTableKey, boolean foil, ChatFormatting nameColor) {
        super(properties);
        this.lootTableKey = lootTableKey;
        this.foil = foil;
        this.nameColor = nameColor;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(nameColor);
    }

    public ResourceLocation getLootTableLocation() {
        return lootTableKey.location();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}