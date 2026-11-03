package com.gabrielflorianoo.peqol.block.entity;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Block entities whose behaviour is bound to the personal EMC of the player that placed them.
 */
public interface IOwnedMachine {

    OwnerData getOwnerData();

    /**
     * Adds lines describing the machine state (shown in chat when right clicked).
     */
    void addInfo(List<Component> lines);

    /**
     * Called when a player right clicks the machine while holding something.
     *
     * @return true if the click was handled.
     */
    default boolean onItemUse(ServerPlayer player, InteractionHand hand, ItemStack held) {
        return false;
    }

    /**
     * Called when a player sneak + right clicks the machine with an empty hand.
     */
    default void onSneakUse(ServerPlayer player) {
    }
}
