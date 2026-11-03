package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.network.KeyAction;
import com.gabrielflorianoo.peqol.network.KeyActionPacket;
import com.gabrielflorianoo.peqol.network.PEQoLNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PEQoL.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        EmcHudOverlay.INSTANCE.tick(mc.player);
        if (mc.screen != null) {
            return;
        }
        send(ModKeys.OPEN_TABLET, KeyAction.OPEN_TABLET);
        send(ModKeys.OPEN_BAG, KeyAction.OPEN_BAG);
        send(ModKeys.QUICK_BURN, KeyAction.QUICK_BURN);
        send(ModKeys.TOGGLE_RESTOCKER, KeyAction.TOGGLE_RESTOCKER);
        send(ModKeys.TOGGLE_SIPHON, KeyAction.TOGGLE_SIPHON);
        while (ModKeys.TOGGLE_HUD.consumeClick()) {
            boolean visible = EmcHudOverlay.INSTANCE.toggle();
            mc.player.displayClientMessage(Component.translatable(visible ? "message.peqol.hud_on" : "message.peqol.hud_off")
                    .withStyle(visible ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        }
    }

    private static void send(KeyMapping key, KeyAction action) {
        boolean sent = false;
        while (key.consumeClick()) {
            if (!sent) {
                PEQoLNetwork.sendToServer(new KeyActionPacket(action));
                sent = true;
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        EmcHudOverlay.INSTANCE.reset();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent event) {
        TooltipHandler.addLines(event);
    }
}
