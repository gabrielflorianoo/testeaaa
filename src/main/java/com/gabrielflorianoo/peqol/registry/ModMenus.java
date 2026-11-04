package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.menu.FilterMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, PEQoL.MODID);

    public static final RegistryObject<MenuType<FilterMenu>> FILTER = MENUS.register("filter", () -> IForgeMenuType.create(FilterMenu::fromNetwork));

    public static final RegistryObject<MenuType<com.gabrielflorianoo.peqol.menu.AlchemicalCraftingMenu>> ALCHEMICAL_CRAFTING = MENUS.register("alchemical_crafting",
            () -> IForgeMenuType.create(com.gabrielflorianoo.peqol.menu.AlchemicalCraftingMenu::fromNetwork));

    private ModMenus() {
    }
}
