package com.gabrielflorianoo.peqol.compat.jei;

import com.gabrielflorianoo.peqol.menu.AlchemicalCraftingMenu;
import com.gabrielflorianoo.peqol.network.GhostIngredientPacket;
import com.gabrielflorianoo.peqol.network.PEQoLNetwork;
import com.gabrielflorianoo.peqol.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JEI "+" button: fills the Alchemical Crafting Table grid with ghosts of learned ingredients.
 */
public class AlchemicalCraftingTransferHandler implements IRecipeTransferHandler<AlchemicalCraftingMenu, CraftingRecipe> {

    private final IRecipeTransferHandlerHelper helper;

    public AlchemicalCraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @NotNull
    @Override
    public Class<? extends AlchemicalCraftingMenu> getContainerClass() {
        return AlchemicalCraftingMenu.class;
    }

    @NotNull
    @Override
    public Optional<MenuType<AlchemicalCraftingMenu>> getMenuType() {
        return Optional.of(ModMenus.ALCHEMICAL_CRAFTING.get());
    }

    @NotNull
    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(@NotNull AlchemicalCraftingMenu container, @NotNull CraftingRecipe recipe, @NotNull IRecipeSlotsView recipeSlots,
                                               @NotNull Player player, boolean maxTransfer, boolean doTransfer) {
        List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        List<ItemStack> ghosts = new ArrayList<>();
        List<IRecipeSlotView> missing = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i >= inputs.size()) {
                ghosts.add(ItemStack.EMPTY);
                continue;
            }
            IRecipeSlotView view = inputs.get(i);
            List<ItemStack> options = view.getIngredients(VanillaTypes.ITEM_STACK).toList();
            if (options.isEmpty()) {
                ghosts.add(ItemStack.EMPTY);
                continue;
            }
            ItemStack choice = ItemStack.EMPTY;
            //Prefer the one JEI is showing, then any other learned option
            ItemStack displayed = view.getDisplayedIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
            if (AlchemicalCraftingMenu.canBeGhost(player, displayed)) {
                choice = displayed;
            } else {
                for (ItemStack option : options) {
                    if (AlchemicalCraftingMenu.canBeGhost(player, option)) {
                        choice = option;
                        break;
                    }
                }
            }
            if (choice.isEmpty()) {
                missing.add(view);
            }
            ghosts.add(choice.copyWithCount(1));
        }
        if (!missing.isEmpty()) {
            return helper.createUserErrorForMissingSlots(Component.translatable("gui.peqol.alchemical_crafting.not_learned"), missing);
        }
        if (doTransfer) {
            PEQoLNetwork.sendToServer(new GhostIngredientPacket(container.containerId, GhostIngredientPacket.WHOLE_GRID, ghosts));
        }
        return null;
    }
}
