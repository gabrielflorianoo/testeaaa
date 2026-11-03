package com.gabrielflorianoo.peqol.menu;

import com.gabrielflorianoo.peqol.registry.ModMenus;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 27 ghost slots: click with an item to add a copy, click with an empty hand to clear, shift click from the
 * inventory to add. Nothing is ever consumed.
 */
public class FilterMenu extends AbstractContainerMenu {

    public static final int SIZE = 27;
    public static final int BUTTON_JUNK = 0;

    private final Container filter = new SimpleContainer(SIZE);
    @Nullable
    private final FilterTarget target;
    private final boolean junkOption;
    private final int lockedSlot;
    private final boolean countMode;

    public boolean isCountMode() {
        return countMode;
    }
    private final DataSlot junk = DataSlot.standalone();

    /**
     * Client side.
     */
    public static FilterMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buf) {
        return new FilterMenu(id, inventory, null, buf.readBoolean(), buf.readVarInt(), buf.readBoolean());
    }

    public FilterMenu(int id, Inventory inventory, @Nullable FilterTarget target, boolean junkOption, int lockedSlot) {
        this(id, inventory, target, junkOption, lockedSlot, false);
    }

    /**
     * @param countMode ghost stacks keep the clicked stack's size (used as a "keep N in stock" amount).
     */
    public FilterMenu(int id, Inventory inventory, @Nullable FilterTarget target, boolean junkOption, int lockedSlot, boolean countMode) {
        super(ModMenus.FILTER.get(), id);
        this.target = target;
        this.junkOption = junkOption;
        this.lockedSlot = lockedSlot;
        this.countMode = countMode;
        if (target != null) {
            List<ItemStack> stored = target.load();
            for (int i = 0; i < Math.min(SIZE, stored.size()); i++) {
                ItemStack stack = stored.get(i);
                filter.setItem(i, stack.copyWithCount(countMode ? Math.max(1, Math.min(64, stack.getCount())) : 1));
            }
            junk.set(target.isJunkMode() ? 1 : 0);
        }
        addDataSlot(junk);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new GhostSlot(filter, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        int inventoryY = 85;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            int index = col;
            addSlot(new Slot(inventory, col, 8 + col * 18, inventoryY + 58) {
                @Override
                public boolean mayPickup(@NotNull Player player) {
                    return index != lockedSlot;
                }
            });
        }
    }

    public boolean hasJunkOption() {
        return junkOption;
    }

    public boolean isJunkMode() {
        return junk.get() != 0;
    }

    private void addGhost(ItemStack stack) {
        if (stack.isEmpty() || !EmcUtil.hasValue(stack)) {
            return;
        }
        for (int i = 0; i < SIZE; i++) {
            if (ItemStack.isSameItemSameTags(filter.getItem(i), stack)) {
                return;
            }
        }
        for (int i = 0; i < SIZE; i++) {
            if (filter.getItem(i).isEmpty()) {
                filter.setItem(i, stack.copyWithCount(1));
                return;
            }
        }
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < SIZE) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                filter.setItem(slotId, ItemStack.EMPTY);
            } else if (EmcUtil.hasValue(carried)) {
                boolean duplicate = false;
                for (int i = 0; i < SIZE; i++) {
                    if (i != slotId && ItemStack.isSameItemSameTags(filter.getItem(i), carried)) {
                        duplicate = true;
                    }
                }
                if (!duplicate) {
                    filter.setItem(slotId, carried.copyWithCount(countMode ? Math.min(64, carried.getCount()) : 1));
                }
            }
            return;
        }
        if (clickType == ClickType.SWAP && button == lockedSlot) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index >= SIZE && index < slots.size()) {
            addGhost(slots.get(index).getItem());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BUTTON_JUNK && junkOption && target != null) {
            boolean value = !target.isJunkMode();
            target.setJunkMode(value);
            junk.set(value ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return target == null || target.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (target != null && !player.level().isClientSide) {
            List<ItemStack> result = new ArrayList<>();
            for (int i = 0; i < SIZE; i++) {
                if (!filter.getItem(i).isEmpty()) {
                    result.add(filter.getItem(i));
                }
            }
            target.save(result);
        }
    }

    private static class GhostSlot extends Slot {

        GhostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }
    }
}
