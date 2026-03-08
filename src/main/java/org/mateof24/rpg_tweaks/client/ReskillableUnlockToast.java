package org.mateof24.rpg_tweaks.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ReskillableUnlockToast implements Toast {

    private static final long DISPLAY_TIME = 5000L;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 32;

    private static final Map<String, Integer> SKILL_COLORS = Map.of(
            "attack",    0xFFFF5555,
            "defense",   0xFF5555FF,
            "mining",    0xFFAAAAAA,
            "gathering", 0xFF55FF55,
            "farming",   0xFFAAFF55,
            "building",  0xFFFFAA55,
            "agility",   0xFF55FFFF,
            "magic",     0xFFFF55FF
    );

    private final ItemStack itemStack;
    private final Component itemName;
    private final Component subtitle;
    private final int accentColor;

    private ReskillableUnlockToast(ItemStack stack, String skill, int level) {
        this.itemStack = stack;
        this.itemName = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
        String skillLabel = skill.substring(0, 1).toUpperCase(Locale.ROOT) + skill.substring(1);
        this.subtitle = Component.literal(skillLabel + " Lv." + level).withStyle(ChatFormatting.GRAY);
        this.accentColor = SKILL_COLORS.getOrDefault(skill, 0xFFFFD700);
    }

    public static void show(String skill, int level, List<ResourceLocation> items) {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation rl : items) {
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item == null || item == Items.AIR) continue;
            mc.getToasts().addToast(new ReskillableUnlockToast(new ItemStack(item), skill, level));
        }
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        guiGraphics.fill(0, 0, WIDTH, HEIGHT, 0xEE111122);
        guiGraphics.fill(0, 0, 3, HEIGHT, accentColor);
        guiGraphics.renderItem(itemStack, 7, 8);
        Minecraft mc = Minecraft.getInstance();
        guiGraphics.drawString(mc.font, itemName, 28, 7, 0xFFFFFFFF, false);
        guiGraphics.drawString(mc.font, subtitle, 28, 18, 0xFFAAAAAA, false);
        return timeSinceLastVisible >= DISPLAY_TIME ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public int width() { return WIDTH; }

    @Override
    public int height() { return HEIGHT; }
}