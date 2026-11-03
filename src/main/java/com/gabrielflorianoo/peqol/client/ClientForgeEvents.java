package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.network.KeyAction;
import com.gabrielflorianoo.peqol.network.KeyActionPacket;
import com.gabrielflorianoo.peqol.network.PEQoLNetwork;
import com.gabrielflorianoo.peqol.network.PickBlockPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
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
        MachineInfoOverlay.INSTANCE.tick(mc);
        if (mc.screen != null) {
            return;
        }
        send(ModKeys.OPEN_TABLET, KeyAction.OPEN_TABLET);
        send(ModKeys.OPEN_BAG, KeyAction.OPEN_BAG);
        send(ModKeys.QUICK_BURN, KeyAction.QUICK_BURN);
        send(ModKeys.TOGGLE_RESTOCKER, KeyAction.TOGGLE_RESTOCKER);
        send(ModKeys.TOGGLE_SIPHON, KeyAction.TOGGLE_SIPHON);
        send(ModKeys.TOGGLE_MENDING, KeyAction.TOGGLE_MENDING);
        send(ModKeys.TOGGLE_SATIATION, KeyAction.TOGGLE_SATIATION);
        send(ModKeys.TOGGLE_STELLAR, KeyAction.TOGGLE_STELLAR);
        send(ModKeys.TOGGLE_ILLUMINATION, KeyAction.TOGGLE_ILLUMINATION);
        send(ModKeys.TOGGLE_SMELTING, KeyAction.TOGGLE_SMELTING);
        send(ModKeys.LEARN_INVENTORY, KeyAction.LEARN_INVENTORY);
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

    /**
     * Alchemical pick block: when the picked block isn't in the inventory ask the server to create it from EMC.
     */
    @SubscribeEvent
    public static void onPickBlock(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.getAbilities().instabuild || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) {
            return;
        }
        ItemStack picked = state.getCloneItemStack(hit, mc.level, hit.getBlockPos(), mc.player);
        if (!picked.isEmpty() && mc.player.getInventory().findSlotMatchingItem(picked) == -1) {
            PEQoLNetwork.sendToServer(new PickBlockPacket(hit.getBlockPos()));
        }
    }

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        WandPreview.render(event);
    }

    @SubscribeEvent
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        UnlearnedMarker.render(event);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        TransmutationScreenButtons.onInit(event);
    }

    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        ClientCommands.register(event.getDispatcher(), event.getBuildContext());
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
