package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.PEQoL;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PEQoL.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(com.gabrielflorianoo.peqol.registry.ModMenus.FILTER.get(), FilterScreen::new));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : ModKeys.ALL) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.gabrielflorianoo.peqol.registry.ModBlockEntities.ALCHEMICAL_DISPLAY.get(), DisplayRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("emc_hud", EmcHudOverlay.INSTANCE);
        event.registerAboveAll("machine_info", MachineInfoOverlay.INSTANCE);
    }
}
