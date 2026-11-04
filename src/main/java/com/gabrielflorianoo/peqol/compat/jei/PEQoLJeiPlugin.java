package com.gabrielflorianoo.peqol.compat.jei;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

/**
 * Adds a JEI info page to every QoL+ item. Only loaded by JEI when it is installed.
 */
@JeiPlugin
public class PEQoLJeiPlugin implements IModPlugin {

    private static final ResourceLocation ID = PEQoL.rl("jei_plugin");

    @NotNull
    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipeTransferHandlers(@NotNull mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new AlchemicalCraftingTransferHandler(registration.getTransferHelper()), mezz.jei.api.constants.RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeCatalysts(@NotNull mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.ALCHEMICAL_CRAFTING_TABLE.get()), mezz.jei.api.constants.RecipeTypes.CRAFTING);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ALCHEMICAL_CRAFTING_TABLET.get()), mezz.jei.api.constants.RecipeTypes.CRAFTING);
    }

    @Override
    public void registerGuiHandlers(@NotNull mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(com.gabrielflorianoo.peqol.client.AlchemicalCraftingScreen.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<com.gabrielflorianoo.peqol.client.AlchemicalCraftingScreen>() {
                    @NotNull
                    @Override
                    public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(@NotNull com.gabrielflorianoo.peqol.client.AlchemicalCraftingScreen screen) {
                        return java.util.List.of(screen.panelArea());
                    }
                });
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        for (RegistryObject<Item> entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) {
                continue;
            }
            registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable("jei.peqol.info." + id.getPath()));
        }
    }
}
