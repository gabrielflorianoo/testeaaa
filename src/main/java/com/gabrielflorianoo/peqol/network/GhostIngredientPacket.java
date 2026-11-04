package com.gabrielflorianoo.peqol.network;

import com.gabrielflorianoo.peqol.menu.AlchemicalCraftingMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -> server: change the ghost ingredients of the open Alchemical Crafting Table.
 * {@code slot} 0-8 sets one grid slot (empty stack clears it), -1 puts it in the first free slot,
 * -2 replaces the whole grid with {@code stacks} (JEI transfer / clear button).
 */
public record GhostIngredientPacket(int containerId, int slot, List<ItemStack> stacks) {

    public static final int FIRST_FREE = -1;
    public static final int WHOLE_GRID = -2;

    public static GhostIngredientPacket single(int containerId, int slot, ItemStack stack) {
        return new GhostIngredientPacket(containerId, slot, List.of(stack));
    }

    public static void encode(GhostIngredientPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        buf.writeVarInt(packet.slot);
        buf.writeVarInt(Math.min(9, packet.stacks.size()));
        for (int i = 0; i < Math.min(9, packet.stacks.size()); i++) {
            buf.writeItem(packet.stacks.get(i));
        }
    }

    public static GhostIngredientPacket decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int slot = buf.readVarInt();
        int size = Math.min(9, buf.readVarInt());
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            stacks.add(buf.readItem());
        }
        return new GhostIngredientPacket(containerId, slot, stacks);
    }

    public static void handle(GhostIngredientPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        context.get().setPacketHandled(true);
        if (player == null || !(player.containerMenu instanceof AlchemicalCraftingMenu menu) || menu.containerId != packet.containerId) {
            return;
        }
        if (packet.slot == WHOLE_GRID) {
            menu.fillGhosts(packet.stacks);
            return;
        }
        ItemStack stack = packet.stacks.isEmpty() ? ItemStack.EMPTY : packet.stacks.get(0);
        int slot = packet.slot;
        if (slot == FIRST_FREE) {
            slot = -1;
            for (int i = 0; i < 9; i++) {
                if (menu.getCraftSlots().getItem(i).isEmpty()) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot >= 0 && slot < 9) {
            menu.setGhost(slot, stack);
        }
    }
}
