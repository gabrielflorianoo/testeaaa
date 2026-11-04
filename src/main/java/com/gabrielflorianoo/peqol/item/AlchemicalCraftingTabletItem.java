package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.menu.AlchemicalCraftingMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Portable Alchemical Crafting Table: craft with learned items paid in EMC, anywhere.
 */
public class AlchemicalCraftingTabletItem extends Item {

    public static final Component TITLE = Component.translatable("container.peqol.alchemical_crafting");

    public AlchemicalCraftingTabletItem(Properties properties) {
        super(properties);
    }

    public static void open(ServerPlayer player, ContainerLevelAccess access) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new AlchemicalCraftingMenu(id, inventory, access), TITLE));
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, ContainerLevelAccess.NULL);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.peqol.alchemical_crafting_tablet.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.peqol.alchemical_crafting_tablet.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
