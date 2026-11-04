package com.gabrielflorianoo.peqol.item;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Every utility workstation in one item. Sneak + right click cycles the mode, right click opens it.
 */
public class AlchemistToolkitItem extends Item {

    public static final String TAG_MODE = "ToolkitMode";

    public enum Mode {
        CRAFTING("container.crafting"),
        ANVIL("container.repair"),
        STONECUTTER("container.stonecutter"),
        SMITHING("container.upgrade"),
        GRINDSTONE("container.grindstone_title"),
        LOOM("container.loom"),
        CARTOGRAPHY("container.cartography_table");

        public final String title;

        Mode(String title) {
            this.title = title;
        }

        AbstractContainerMenu create(int id, Inventory inventory, ContainerLevelAccess access) {
            return switch (this) {
                case CRAFTING -> new CraftingMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(@NotNull Player player) {
                        return true;
                    }
                };
                case ANVIL -> new AnvilMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(@NotNull Player player) {
                        return true;
                    }
                };
                case STONECUTTER -> new StonecutterMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(@NotNull Player player) {
                        return true;
                    }
                };
                case SMITHING -> new SmithingMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(@NotNull Player player) {
                        return true;
                    }
                };
                case GRINDSTONE -> new GrindstoneMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(@NotNull Player player) {
                        return true;
                    }
                };
                case LOOM -> new LoomMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(@NotNull Player player) {
                        return true;
                    }
                };
                case CARTOGRAPHY -> new CartographyTableMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(@NotNull Player player) {
                        return true;
                    }
                };
            };
        }
    }

    public AlchemistToolkitItem(Properties properties) {
        super(properties);
    }

    public static Mode getMode(ItemStack stack) {
        int index = stack.hasTag() ? stack.getTag().getInt(TAG_MODE) : 0;
        Mode[] modes = Mode.values();
        return modes[Math.floorMod(index, modes.length)];
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player.isShiftKeyDown()) {
            Mode next = Mode.values()[(getMode(stack).ordinal() + 1) % Mode.values().length];
            stack.getOrCreateTag().putInt(TAG_MODE, next.ordinal());
            player.displayClientMessage(Component.translatable("message.peqol.toolkit.mode", Component.translatable(next.title)).withStyle(ChatFormatting.AQUA), true);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.4F);
            return InteractionResultHolder.success(stack);
        }
        Mode mode = getMode(stack);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> mode.create(id, inventory, ContainerLevelAccess.create(level, p.blockPosition())),
                Component.translatable(mode.title)));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.peqol.toolkit.mode", Component.translatable(getMode(stack).title)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.peqol.alchemist_toolkit.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.peqol.alchemist_toolkit.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static String modeName(Mode mode) {
        return mode.name().toLowerCase(Locale.ROOT);
    }
}
