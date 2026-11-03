package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PEQoL.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.peqol"))
            .icon(() -> new ItemStack(ModItems.RESTOCKER_TALISMAN.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.RESTOCKER_TALISMAN.get());
                output.accept(ModItems.SIPHON_CHARM.get());
                output.accept(ModItems.KNOWLEDGE_SCROLL.get());
                output.accept(ModItems.ALCHEMICAL_UPLINK.get());
                output.accept(ModItems.ALCHEMICAL_IMPORTER.get());
                output.accept(ModItems.ALCHEMICAL_EXPORTER.get());
            })
            .build());

    private ModCreativeTab() {
    }
}
