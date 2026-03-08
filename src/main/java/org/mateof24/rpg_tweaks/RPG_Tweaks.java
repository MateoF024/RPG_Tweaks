package org.mateof24.rpg_tweaks;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.mateof24.rpg_tweaks.command.RPGCommands;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.config.ModConfigScreen;
import org.mateof24.rpg_tweaks.data.PlayerDimensionData;
import org.mateof24.rpg_tweaks.integration.ReskillableCommands;
import org.mateof24.rpg_tweaks.integration.ReskillableConfigManager;
import org.mateof24.rpg_tweaks.integration.ItemObliteratorCommands;
import org.mateof24.rpg_tweaks.integration.ItemObliteratorConfigManager;
import org.mateof24.rpg_tweaks.item.ModItems;
import org.slf4j.Logger;

@Mod(org.mateof24.rpg_tweaks.RPG_Tweaks.MODID)
public class RPG_Tweaks {
    public static final String MODID = "rpg_tweaks";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RPG_Tweaks(IEventBus modEventBus, ModContainer modContainer) {
        // Cargar configuración
        LOGGER.info("Initializing RPG-Tweaks...");
        ModConfig.load();
        PlayerDimensionData.load();
        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> ModConfigScreen.createConfigScreen(parent));

        LOGGER.info("RPG Tweaks initialized correctly");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Running common RPG Tweaks setup");
        logConfigStatus();
        checkReskillableIntegration();
        checkItemObliteratorIntegration();
    }

    private void logConfigStatus() {
        ModConfig config = ModConfig.getInstance();

        LOGGER.info("=== RPG Tweaks Configuration ===");
        LOGGER.info("Advancements XP Lock: {}",
                config.blockAdvancementXP ? "ON" : "OFF");

        LOGGER.info("--- Block XP System ---");
        LOGGER.info("Custom XP system: {}",
                config.enableCustomOreXP ? "ON" : "OFF");

        int totalBlocks = config.oreXPConfig.blockConfigs.size();
        int totalTags = config.oreXPConfig.tagConfigs.size();
        LOGGER.info("Configured blocks: {} | Configured tags: {}", totalBlocks, totalTags);

        LOGGER.info("==========================================");
    }

    private void checkReskillableIntegration() {
        if (ReskillableConfigManager.isReskillableInstalled()) {
            LOGGER.info("=== Integration with Reskillable Reimagined ===");
            LOGGER.info("Reskillable Reimagined detected - Skill lock commands available");
            LOGGER.info("Use /rpg_tweaks skills add <skill> <level> ... <item>");
            LOGGER.info("Use /rpg_tweaks craftskills add <skill> <level> ... <item>");
            LOGGER.info("=============================================");
            org.mateof24.rpg_tweaks.integration.ReskillableSkillCache.load();
        } else {
            LOGGER.info("Reskillable Reimagined not detected - Skill lock commands unavailable");
        }
    }

    private void checkItemObliteratorIntegration() {
        if (ItemObliteratorConfigManager.isItemObliteratorInstalled()) {
            LOGGER.info("=== Integration with Item Obliterator ===");
            LOGGER.info("Item Obliterator detected - Available blacklist commands");
            LOGGER.info("Use /rpg_tweaks banitem [<item>]");
            LOGGER.info("Use /rpg_tweaks unbanitem [<item>]");
            LOGGER.info("Use /rpg_tweaks banitem list");
            LOGGER.info("========================================");
        } else {
            LOGGER.info("Item Obliterator not detected - Banitem commands unavailable");
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("RPG-Tweaks - Server initialized");
    }

    @EventBusSubscriber(modid = MODID)
    public static class ServerEvents {

        @SubscribeEvent
        public static void onCommandsRegister(RegisterCommandsEvent event) {
            LOGGER.info("Registering RPG Tweaks commands...");

            RPGCommands.register(event.getDispatcher());

            if (ReskillableConfigManager.isReskillableInstalled()) {
                ReskillableCommands.register(event.getDispatcher());
                LOGGER.info("Registered Reskillable integration commands");
            }

            if (ItemObliteratorConfigManager.isItemObliteratorInstalled()) {
                ItemObliteratorCommands.register(event.getDispatcher());
                LOGGER.info("Registered Item Obliterator integration commands");
            }
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents {
        @SubscribeEvent
        public static void onRegisterPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(
                    org.mateof24.rpg_tweaks.network.S2CUnlockNotificationPacket.TYPE,
                    org.mateof24.rpg_tweaks.network.S2CUnlockNotificationPacket.STREAM_CODEC,
                    org.mateof24.rpg_tweaks.network.S2CUnlockNotificationPacket::handle
            );
        }
    }


    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("RPG Tweaks - Client initialized");
        }
    }
}