package org.mateof24.rpg_tweaks.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLPaths;
import org.mateof24.rpg_tweaks.integration.ReskillableConfigManager;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class ReskillableProgressionScreen extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int C_BG           = 0xFF2A2A2A;
    private static final int C_BORDER       = 0xFF888888;
    private static final int C_TITLEBAR     = 0xFF1E1E1E;
    private static final int C_SIDEBAR      = 0xFF222222;
    private static final int C_DIVIDER      = 0xFF555555;
    private static final int C_PAGE         = 0xFF2E2E2E;
    private static final int C_PAGE_BORDER  = 0xFF555555;
    private static final int C_SLOT         = 0xFF383838;
    private static final int C_SLOT_HOVER   = 0xFF484848;
    private static final int C_TAB          = 0xFF303030;
    private static final int C_TAB_HOVER    = 0xFF404040;
    private static final int C_TAB_SEL      = 0xFF505050;
    private static final int C_TITLE        = 0xFFFFFFFF;
    private static final int C_SUBTEXT      = 0xFFCCCCCC;
    private static final int C_PAGENUM      = 0xFFAAAAAA;
    private static final int C_UNLOCKED     = 0xFF55FF55;
    private static final int C_LOCKED       = 0xFFFF5555;

    private static final int WIN_W          = 382;
    private static final int WIN_H          = 258;
    private static final int SIDEBAR_W      = 90;
    private static final int ITEMS_PER_PAGE = 2;
    private static final int PAGES_VISIBLE  = 2;
    private static final int ITEMS_VISIBLE  = ITEMS_PER_PAGE * PAGES_VISIBLE;

    private final Map<String, List<ItemEntry>> skillItems = new LinkedHashMap<>();
    private final Map<String, Integer> playerLevels = new HashMap<>();

    private String selectedSkill = null;
    private int pageOffset = 0;
    private int left, top;

    private ItemStack tooltipStack = ItemStack.EMPTY;
    private int tooltipX, tooltipY;

    public ReskillableProgressionScreen() {
        super(Component.translatable("gui.rpg_tweaks.progression.title"));
        loadSkillLocks();
        loadPlayerLevels();
        if (!skillItems.isEmpty()) {
            selectedSkill = skillItems.keySet().iterator().next();
        }
    }

    private void loadSkillLocks() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("reskillable/skill_locks.json");
        if (!Files.exists(path)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject locks = null;
            for (String key : List.of("skillLocks", "skill_locks", "locks")) {
                if (root.has(key) && root.get(key).isJsonObject()) {
                    locks = root.getAsJsonObject(key);
                    break;
                }
            }
            if (locks == null) return;
            for (var entry : locks.entrySet()) {
                String itemId = entry.getKey();
                if (!entry.getValue().isJsonArray()) continue;
                for (JsonElement el : entry.getValue().getAsJsonArray()) {
                    String[] parts = el.getAsString().split(":", 2);
                    if (parts.length != 2) continue;
                    String skill = parts[0].toLowerCase(Locale.ROOT);
                    try {
                        int level = Integer.parseInt(parts[1]);
                        skillItems.computeIfAbsent(skill, k -> new ArrayList<>())
                                .add(new ItemEntry(itemId, skill, level));
                    } catch (NumberFormatException ignored) {}
                }
            }
            skillItems.values().forEach(list -> list.sort(Comparator.comparingInt(e -> e.requiredLevel)));
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/Progression] Error loading skill_locks.json: {}", e.getMessage());
        }
    }

    private void loadPlayerLevels() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            Class<?> modelClass = Class.forName("net.bandit.reskillable.common.capabilities.SkillModel");
            Class<?> enumClass  = Class.forName("net.bandit.reskillable.common.skills.Skill");
            Method getModel = modelClass.getMethod("get", net.minecraft.world.entity.player.Player.class);
            Method getLevel = modelClass.getMethod("getSkillLevel", enumClass);
            Object model = getModel.invoke(null, mc.player);
            if (model == null) return;
            for (Object c : enumClass.getEnumConstants()) {
                playerLevels.put(c.toString().toLowerCase(Locale.ROOT),
                        ((Number) getLevel.invoke(model, c)).intValue());
            }
            Method getLevelById = null;
            for (Method m : modelClass.getMethods()) {
                if (m.getName().equals("getSkillLevel") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class) {
                    getLevelById = m;
                    break;
                }
            }
            if (getLevelById != null) {
                for (String custom : ReskillableConfigManager.getCustomSkillIds()) {
                    try {
                        playerLevels.put(custom, (int) getLevelById.invoke(model, custom));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void init() {
        super.init();
        left = (width  - WIN_W) / 2;
        top  = (height - WIN_H) / 2;
        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();
        int tabY = top + 32;
        for (String skill : skillItems.keySet()) {
            final String s = skill;
            addRenderableWidget(new TabButton(
                    left + 4, tabY, SIDEBAR_W - 8, 18,
                    Component.literal(capitalize(skill)),
                    () -> { selectedSkill = s; pageOffset = 0; rebuildWidgets(); },
                    skill.equals(selectedSkill),
                    skill));
            tabY += 20;
        }

        if (selectedSkill != null && getItemCount() > 0) {
            int navY  = top + WIN_H - 20;
            int cLeft = left + SIDEBAR_W + 8;
            int cW    = WIN_W - SIDEBAR_W - 16;

            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.translatable("gui.rpg_tweaks.progression.prev"),
                            b -> { pageOffset = Math.max(0, pageOffset - ITEMS_VISIBLE); rebuildWidgets(); })
                    .bounds(cLeft, navY, 58, 14).build());

            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.translatable("gui.rpg_tweaks.progression.next"),
                            b -> {
                                if (pageOffset + ITEMS_VISIBLE < getItemCount()) {
                                    pageOffset += ITEMS_VISIBLE;
                                    rebuildWidgets();
                                }
                            })
                    .bounds(cLeft + cW - 58, navY, 58, 14).build());
        }
    }

    private int getItemCount() {
        if (selectedSkill == null) return 0;
        List<ItemEntry> list = skillItems.get(selectedSkill);
        return list == null ? 0 : list.size();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        tooltipStack = ItemStack.EMPTY;

        g.fill(left, top, left + WIN_W, top + WIN_H, C_BG);
        g.fill(left + 1, top + 1, left + WIN_W - 1, top + 19, C_TITLEBAR);
        g.drawCenteredString(font, title, left + WIN_W / 2, top + 6, C_TITLE);

        g.fill(left + 2, top + 20, left + SIDEBAR_W, top + WIN_H - 2, C_SIDEBAR);
        g.drawString(font,
                Component.translatable("gui.rpg_tweaks.progression.skills"),
                left + 6, top + 23, 0xFFFFFFFF, true);

        g.fill(left + SIDEBAR_W, top + 19, left + SIDEBAR_W + 1, top + WIN_H - 2, C_DIVIDER);

        renderPages(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, delta);

        drawBorder(g, left, top, WIN_W, WIN_H, C_BORDER);

        if (!tooltipStack.isEmpty()) {
            g.renderTooltip(font, tooltipStack, tooltipX, tooltipY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x88000000);
    }

    private void renderPages(GuiGraphics g, int mouseX, int mouseY) {
        int cLeft = left + SIDEBAR_W + 6;
        int cW    = WIN_W - SIDEBAR_W - 10;
        int pTop  = top + 20;
        int pH    = WIN_H - 42;
        int pW    = (cW - 4) / 2;

        if (selectedSkill == null || skillItems.isEmpty()) {
            g.drawCenteredString(font,
                    Component.translatable("gui.rpg_tweaks.progression.no_data"),
                    cLeft + cW / 2, top + WIN_H / 2 - 4, 0xFFCCCCCC);
            return;
        }

        List<ItemEntry> items = skillItems.getOrDefault(selectedSkill, Collections.emptyList());
        if (items.isEmpty()) {
            g.drawCenteredString(font,
                    Component.translatable("gui.rpg_tweaks.progression.empty"),
                    cLeft + cW / 2, top + WIN_H / 2 - 4, 0xFFCCCCCC);
            return;
        }

        int totalHojas = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_VISIBLE));
        int curHoja    = pageOffset / ITEMS_VISIBLE + 1;
        g.drawCenteredString(font,
                Component.literal(curHoja + " / " + totalHojas),
                cLeft + cW / 2, top + WIN_H - 18, 0xFFAAAAAA);

        for (int p = 0; p < PAGES_VISIBLE; p++) {
            int px = cLeft + p * (pW + 4);

            g.fill(px, pTop, px + pW, pTop + pH, C_PAGE);
            drawBorder(g, px, pTop, pW, pH, C_PAGE_BORDER);

            int slotH = pH / ITEMS_PER_PAGE;
            for (int slot = 0; slot < ITEMS_PER_PAGE; slot++) {
                int idx = pageOffset + p * ITEMS_PER_PAGE + slot;
                if (idx >= items.size()) break;
                renderItemSlot(g, items.get(idx),
                        px + 3, pTop + slot * slotH,
                        pW - 6, slotH - 2,
                        mouseX, mouseY);
            }
        }
    }

    private void renderItemSlot(GuiGraphics g, ItemEntry entry,
                                int x, int y, int w, int h,
                                int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hovered ? C_SLOT_HOVER : C_SLOT);
        drawBorder(g, x, y, w, h, 0x33445588);

        ItemStack stack = resolveStack(entry.itemId);

        float iconScale = 1.5f;
        int iconSize    = (int)(16 * iconScale);
        int iconX       = x + 6;
        int iconY       = y + (h - iconSize) / 2;

        g.pose().pushPose();
        g.pose().translate(iconX, iconY, 0);
        g.pose().scale(iconScale, iconScale, 1f);
        g.renderItem(stack, 0, 0);
        g.renderItemDecorations(font, stack, 0, 0);
        g.pose().popPose();

        int textX  = iconX + iconSize + 5;
        int textW  = x + w - textX - 3;

        String name = stack.isEmpty() ? shortenId(entry.itemId) : stack.getHoverName().getString();
        while (font.width(name) > textW && name.length() > 2) {
            name = name.substring(0, name.length() - 1);
        }
        if (font.width(entry.itemId.contains(":") ? (stack.isEmpty() ? shortenId(entry.itemId) : stack.getHoverName().getString()) : name) > textW) {
            name = name + "..";
        }

        int playerLvl  = playerLevels.getOrDefault(entry.skill, 0);
        boolean unlocked = playerLvl >= entry.requiredLevel;
        String mark    = unlocked ? " §a✔" : " §c✘";
        String req     = capitalize(entry.skill) + " Lv." + entry.requiredLevel + mark;

        int totalTextH = 8 + 4 + 8;
        int textStartY = y + (h - totalTextH) / 2;

        g.drawString(font, name, textX, textStartY, 0xFFFFFFFF, true);
        g.drawString(font, req,  textX, textStartY + 12, unlocked ? C_UNLOCKED : C_LOCKED, true);

        boolean onIcon = mx >= iconX && mx < iconX + iconSize
                && my >= iconY && my < iconY + iconSize;
        if (onIcon && !stack.isEmpty()) {
            tooltipStack = stack;
            tooltipX     = mx;
            tooltipY     = my;
        }
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y,         x + 1,     y + h,     color);
        g.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private ItemStack resolveStack(String itemId) {
        try {
            String[] p = itemId.split(":", 2);
            if (p.length != 2) return ItemStack.EMPTY;
            Item item = BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.fromNamespaceAndPath(p[0], p[1]))
                    .orElse(null);
            return (item == null || item == Items.AIR) ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static String shortenId(String id) {
        int c = id.indexOf(':');
        return c >= 0 ? id.substring(c + 1) : id;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (ModKeybindings.OPEN_PROGRESSION.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return false;
    }

    private record ItemEntry(String itemId, String skill, int requiredLevel) {}

    private static final class TabButton extends AbstractButton {
        private final Runnable action;
        private final boolean selected;
        private final int accentColor;

        TabButton(int x, int y, int w, int h, Component label, Runnable action, boolean selected, String skill) {
            super(x, y, w, h, label);
            this.action      = action;
            this.selected    = selected;
            this.accentColor = ReskillableUnlockToast.SKILL_COLORS.getOrDefault(skill, 0xFFFFD700);
        }

        @Override
        public void onPress() { action.run(); }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float delta) {
            int bg = selected ? C_TAB_SEL : (isHoveredOrFocused() ? C_TAB_HOVER : C_TAB);
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            if (selected) {
                g.fill(getX(), getY(), getX() + 3, getY() + getHeight(), accentColor);
            } else if (isHoveredOrFocused()) {
                g.fill(getX(), getY(), getX() + 2, getY() + getHeight(), accentColor & 0x66FFFFFF);
            }
            g.drawString(Minecraft.getInstance().font, getMessage(),
                    getX() + 8, getY() + (getHeight() - 8) / 2,
                    selected ? 0xFFFFFFFF : 0xFFCCCCCC, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput n) {
            defaultButtonNarrationText(n);
        }
    }
}