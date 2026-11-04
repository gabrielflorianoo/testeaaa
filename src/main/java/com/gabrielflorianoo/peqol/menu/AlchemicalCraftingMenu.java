package com.gabrielflorianoo.peqol.menu;

import com.gabrielflorianoo.peqol.registry.ModMenus;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Crafting table whose grid slots can hold "ghost" ingredients: learned items that are not really there and are
 * paid with personal EMC every time the recipe is crafted. Real items work like in a normal crafting table.
 * <p>
 * Slot layout matches the vanilla crafting table: 0 = result, 1-9 = grid, 10-36 = inventory, 37-45 = hotbar.
 */
public class AlchemicalCraftingMenu extends AbstractContainerMenu {

    public static final int RESULT = 0;
    public static final int GRID_START = 1;
    public static final int INV_START = 10;
    public static final int INV_END = 46;

    private final TransientCraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot ghostMask = DataSlot.standalone();
    private boolean updating;

    public AlchemicalCraftingMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.ALCHEMICAL_CRAFTING.get(), id);
        this.access = access;
        this.player = inventory.player;
        addSlot(new ResultSlot(resultSlots, 0, 124, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new GridSlot(craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlot(ghostMask);
    }

    public static AlchemicalCraftingMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buf) {
        return new AlchemicalCraftingMenu(id, inventory, ContainerLevelAccess.NULL);
    }

    public TransientCraftingContainer getCraftSlots() {
        return craftSlots;
    }

    public boolean isGhost(int gridIndex) {
        return (ghostMask.get() & (1 << gridIndex)) != 0;
    }

    private void setGhostBit(int gridIndex, boolean ghost) {
        int mask = ghostMask.get();
        ghostMask.set(ghost ? mask | (1 << gridIndex) : mask & ~(1 << gridIndex));
    }

    /**
     * EMC paid per craft for the ghost ingredients currently in the grid.
     */
    public long ghostCost() {
        long total = 0;
        for (int i = 0; i < 9; i++) {
            if (isGhost(i)) {
                total += EmcUtil.value(craftSlots.getItem(i));
            }
        }
        return total;
    }

    public boolean canAfford() {
        long cost = ghostCost();
        return cost <= 0 || player.getAbilities().instabuild || EmcUtil.getEmc(player).compareTo(BigInteger.valueOf(cost)) >= 0;
    }

    /**
     * Whether {@code stack} may become a ghost ingredient for this player.
     */
    public static boolean canBeGhost(Player player, ItemStack stack) {
        return !stack.isEmpty() && EmcUtil.value(stack) > 0 && !EmcUtil.isBlacklisted(stack)
                && (player.getAbilities().instabuild || EmcUtil.isKnown(player, stack));
    }

    /**
     * Puts a ghost of {@code stack} in a grid slot (or clears it when empty). Real items in the slot go back to the
     * player first. Server side.
     */
    public void setGhost(int gridIndex, ItemStack stack) {
        if (gridIndex < 0 || gridIndex >= 9) {
            return;
        }
        if (!isGhost(gridIndex) && !craftSlots.getItem(gridIndex).isEmpty()) {
            player.getInventory().placeItemBackInInventory(craftSlots.removeItemNoUpdate(gridIndex));
        }
        if (stack.isEmpty()) {
            if (isGhost(gridIndex)) {
                craftSlots.removeItemNoUpdate(gridIndex);
                setGhostBit(gridIndex, false);
            }
        } else if (canBeGhost(player, stack)) {
            craftSlots.setItem(gridIndex, stack.copyWithCount(1));
            setGhostBit(gridIndex, true);
        }
        slotsChanged(craftSlots);
    }

    /**
     * Replaces the whole grid with ghosts (used by the JEI "+" button). Server side.
     */
    public void fillGhosts(List<ItemStack> stacks) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            setGhost(i, ItemStack.EMPTY);
            if (!craftSlots.getItem(i).isEmpty() && !isGhost(i)) {
                player.getInventory().placeItemBackInInventory(craftSlots.removeItemNoUpdate(i));
            }
            if (!stack.isEmpty()) {
                setGhost(i, stack);
            }
        }
        slotsChanged(craftSlots);
    }

    public void clearGrid() {
        fillGhosts(List.of());
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        if (updating) {
            return;
        }
        access.execute((level, pos) -> updateResult(level));
        if (access == ContainerLevelAccess.NULL) {
            updateResult(player.level());
        }
    }

    private void updateResult(Level level) {
        if (level.isClientSide) {
            return;
        }
        ItemStack result = ItemStack.EMPTY;
        Optional<CraftingRecipe> recipe = level.getServer() == null ? Optional.empty()
                : level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftSlots, level);
        if (recipe.isPresent()) {
            CraftingRecipe found = recipe.get();
            if (!(player instanceof ServerPlayer serverPlayer) || resultSlots.setRecipeUsed(level, serverPlayer, found)) {
                result = found.assemble(craftSlots, level.registryAccess());
            }
        }
        resultSlots.setItem(0, result);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }
        //Ghosts vanish, real ingredients go back to the player
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(i);
            if (!isGhost(i) && !stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        ghostMask.set(0);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return access.evaluate((level, pos) -> player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slot) {
        return slot.container != resultSlots && !(slot instanceof GridSlot grid && isGhost(grid.getContainerSlot())) && super.canTakeItemForPickAll(stack, slot);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (index == RESULT) {
            if (!slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }
            ItemStack result = slot.getItem();
            ItemStack copy = result.copy();
            if (!moveItemStackTo(result, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(result, copy);
            if (result.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, copy);
            if (!result.isEmpty()) {
                player.drop(result, false);
            }
            return copy;
        }
        if (index < INV_START) {
            if (slot instanceof GridSlot grid && isGhost(grid.getContainerSlot())) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
            slot.setChanged();
            return copy;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        //Inventory <-> hotbar (the grid is filled by hand or with ghosts)
        if (index < 37 ? !moveItemStackTo(stack, 37, INV_END, false) : !moveItemStackTo(stack, INV_START, 37, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    private class GridSlot extends Slot {

        GridSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return !isGhost(getContainerSlot());
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return !isGhost(getContainerSlot());
        }
    }

    private class ResultSlot extends Slot {

        private int removeCount;

        ResultSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return player.level().isClientSide || canAfford();
        }

        @NotNull
        @Override
        public ItemStack remove(int amount) {
            if (hasItem()) {
                removeCount += Math.min(amount, getItem().getCount());
            }
            return super.remove(amount);
        }

        @Override
        protected void onQuickCraft(@NotNull ItemStack stack, int amount) {
            removeCount += amount;
            checkTakeAchievements(stack);
        }

        @Override
        protected void checkTakeAchievements(@NotNull ItemStack stack) {
            if (removeCount > 0) {
                stack.onCraftedBy(player.level(), player, removeCount);
                ForgeEventFactory.firePlayerCraftingEvent(player, stack, craftSlots);
            }
            removeCount = 0;
        }

        @Override
        public void onTake(@NotNull Player taker, @NotNull ItemStack stack) {
            checkTakeAchievements(stack);
            Level level = taker.level();
            if (level.isClientSide) {
                return;
            }
            long cost = ghostCost();
            if (cost > 0 && !taker.getAbilities().instabuild && taker instanceof ServerPlayer serverPlayer) {
                EmcUtil.tryRemoveEmc(serverPlayer, BigInteger.valueOf(cost));
            }
            NonNullList<ItemStack> remaining = level.getServer() == null ? NonNullList.withSize(9, ItemStack.EMPTY)
                    : level.getServer().getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, craftSlots, level);
            updating = true;
            for (int i = 0; i < remaining.size() && i < 9; i++) {
                ItemStack leftover = remaining.get(i);
                if (isGhost(i)) {
                    //Ghosts stay; containers they leave behind (empty buckets...) go to the player
                    if (!leftover.isEmpty() && !taker.getInventory().add(leftover)) {
                        taker.drop(leftover, false);
                    }
                    continue;
                }
                ItemStack current = craftSlots.getItem(i);
                if (!current.isEmpty()) {
                    craftSlots.removeItem(i, 1);
                    current = craftSlots.getItem(i);
                }
                if (!leftover.isEmpty()) {
                    if (current.isEmpty()) {
                        craftSlots.setItem(i, leftover);
                    } else if (ItemStack.isSameItemSameTags(current, leftover)) {
                        leftover.grow(current.getCount());
                        craftSlots.setItem(i, leftover);
                    } else if (!taker.getInventory().add(leftover)) {
                        taker.drop(leftover, false);
                    }
                }
            }
            updating = false;
            slotsChanged(craftSlots);
        }
    }
}
