package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.network.KeyAction;
import com.gabrielflorianoo.peqol.network.KeyActionPacket;
import com.gabrielflorianoo.peqol.network.PEQoLNetwork;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * Adds QoL+ buttons next to ProjectE's transmutation screen (no mixins: plain Forge screen events).
 */
public final class TransmutationScreenButtons {

    private TransmutationScreenButtons() {
    }

    public static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> container) || !isTransmutation(screen)) {
            return;
        }
        int x = container.getGuiLeft() + container.getXSize() + 4;
        int y = container.getGuiTop() + 4;
        event.addListener(button(x, y, "gui.peqol.learn_inventory", KeyAction.LEARN_INVENTORY));
        event.addListener(button(x, y + 24, "gui.peqol.burn_inventory", KeyAction.BURN_INVENTORY));
    }

    private static boolean isTransmutation(Screen screen) {
        try {
            return screen instanceof moze_intel.projecte.gameObjs.gui.GUITransmutation;
        } catch (Throwable e) {
            return false;
        }
    }

    private static Button button(int x, int y, String key, KeyAction action) {
        return Button.builder(Component.translatable(key), b -> PEQoLNetwork.sendToServer(new KeyActionPacket(action)))
                .bounds(x, y, 86, 20)
                .tooltip(Tooltip.create(Component.translatable(key + ".tooltip")))
                .build();
    }
}
