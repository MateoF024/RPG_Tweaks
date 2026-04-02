package org.mateof24.rpg_tweaks.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    public static final KeyMapping OPEN_PROGRESSION = new KeyMapping(
            "key.rpg_tweaks.open_progression",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.rpg_tweaks"
    );
}