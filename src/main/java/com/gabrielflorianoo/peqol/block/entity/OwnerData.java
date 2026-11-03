package com.gabrielflorianoo.peqol.block.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class OwnerData {

    @Nullable
    private UUID owner;
    private String ownerName = "";

    public void set(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isOwner(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    /**
     * Owners and operators may configure the machine.
     */
    public boolean canConfigure(ServerPlayer player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    public Component describe() {
        return owner == null ? Component.translatable("info.peqol.no_owner") : Component.literal(ownerName.isEmpty() ? owner.toString() : ownerName);
    }

    public void save(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
    }

    public void load(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
            ownerName = tag.getString("OwnerName");
        } else {
            owner = null;
            ownerName = "";
        }
    }
}
