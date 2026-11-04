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
                output.accept(ModItems.MENDING_CHARM.get());
                output.accept(ModItems.SATIATION_CHARM.get());
                output.accept(ModItems.STELLAR_LINK.get());
                output.accept(ModItems.ILLUMINATION_CHARM.get());
                output.accept(ModItems.CONSTRUCTION_WAND.get());
                output.accept(ModItems.APPRAISAL_LENS.get());
                output.accept(ModItems.ALCHEMICAL_ANCHOR.get());
                output.accept(ModItems.SMELTING_CHARM.get());
                output.accept(ModItems.VISION_CHARM.get());
                output.accept(ModItems.HASTE_CHARM.get());
                output.accept(ModItems.BREATH_CHARM.get());
                output.accept(ModItems.STRENGTH_CHARM.get());
                output.accept(ModItems.FEATHER_CHARM.get());
                output.accept(ModItems.WISDOM_CHARM.get());
                output.accept(ModItems.UNDYING_CHARM.get());
                output.accept(ModItems.POCKET_WORKBENCH.get());
                output.accept(ModItems.ALCHEMIST_TOOLKIT.get());
                output.accept(ModItems.POCKET_SMELTER.get());
                output.accept(ModItems.BLINK_ROD.get());
                output.accept(ModItems.CHRONAL_ECHO.get());
                output.accept(ModItems.LUMEN_WAND.get());
                output.accept(ModItems.TRANSMUTATION_SCEPTER.get());
                output.accept(ModItems.VERDANT_ORB.get());
                output.accept(ModItems.VOID_SPONGE.get());
                output.accept(ModItems.SEER_EYE.get());
                output.accept(ModItems.GRAVITY_STAFF.get());
                output.accept(ModItems.STORM_STAFF.get());
                output.accept(ModItems.PRISMATIC_WAND.get());
                output.accept(ModItems.RECALL_MIRROR.get());
                output.accept(ModItems.CLOUD_SCEPTER.get());
                output.accept(ModItems.SOUL_JAR.get());
                output.accept(ModItems.WARDING_LANTERN.get());
                output.accept(ModItems.VOID_POUCH.get());
                output.accept(ModItems.PROSPECTOR.get());
                output.accept(ModItems.KNOWLEDGE_SCROLL.get());
                output.accept(ModItems.ALCHEMICAL_UPLINK.get());
                output.accept(ModItems.ALCHEMICAL_IMPORTER.get());
                output.accept(ModItems.ALCHEMICAL_EXPORTER.get());
                output.accept(ModItems.ALCHEMICAL_VACUUM.get());
                output.accept(ModItems.ALCHEMICAL_DOWNLINK.get());
                output.accept(ModItems.ALCHEMICAL_GAUGE.get());
                output.accept(ModItems.ALCHEMICAL_INTERFACE.get());
                output.accept(ModItems.TRADE_POST.get());
                output.accept(ModItems.ALCHEMICAL_SPRING.get());
                output.accept(ModItems.ALCHEMICAL_EXCAVATOR.get());
                output.accept(ModItems.ALCHEMICAL_DISPLAY.get());
                output.accept(ModItems.ALCHEMICAL_HARVESTER.get());
                output.accept(ModItems.ALCHEMICAL_PASTURE.get());
                output.accept(ModItems.CELESTIAL_HOURGLASS.get());
                output.accept(ModItems.MACHINE_UPGRADE.get());
                output.accept(ModItems.PHILOSOPHERS_WRENCH.get());
            })
            .build());

    private ModCreativeTab() {
    }
}
