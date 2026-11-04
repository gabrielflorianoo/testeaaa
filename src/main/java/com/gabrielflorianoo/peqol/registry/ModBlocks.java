package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.block.AlchemicalDownlinkBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalExporterBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalGaugeBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalInterfaceBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalPastureBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalHarvesterBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalDisplayBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalExcavatorBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalSpringBlock;
import com.gabrielflorianoo.peqol.block.TradePostBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalImporterBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalUplinkBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalVacuumBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PEQoL.MODID);

    public static final RegistryObject<com.gabrielflorianoo.peqol.block.ConjuredCloudBlock> CONJURED_CLOUD = BLOCKS.register("conjured_cloud",
            () -> new com.gabrielflorianoo.peqol.block.ConjuredCloudBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1F)
                    .sound(SoundType.WOOL).noOcclusion().noLootTable().isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));

    public static final RegistryObject<com.gabrielflorianoo.peqol.block.AlchemicalCraftingTableBlock> ALCHEMICAL_CRAFTING_TABLE = BLOCKS.register("alchemical_crafting_table",
            () -> new com.gabrielflorianoo.peqol.block.AlchemicalCraftingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F, 6F)
                    .sound(SoundType.WOOD).lightLevel(state -> 7)));

    public static final RegistryObject<AlchemicalUplinkBlock> ALCHEMICAL_UPLINK = BLOCKS.register("alchemical_uplink",
            () -> new AlchemicalUplinkBlock(machineProperties(MapColor.COLOR_PURPLE)));
    public static final RegistryObject<AlchemicalImporterBlock> ALCHEMICAL_IMPORTER = BLOCKS.register("alchemical_importer",
            () -> new AlchemicalImporterBlock(machineProperties(MapColor.COLOR_BLACK)));
    public static final RegistryObject<AlchemicalExporterBlock> ALCHEMICAL_EXPORTER = BLOCKS.register("alchemical_exporter",
            () -> new AlchemicalExporterBlock(machineProperties(MapColor.COLOR_RED)));

    public static final RegistryObject<AlchemicalVacuumBlock> ALCHEMICAL_VACUUM = BLOCKS.register("alchemical_vacuum",
            () -> new AlchemicalVacuumBlock(machineProperties(MapColor.COLOR_CYAN)));

    public static final RegistryObject<AlchemicalDownlinkBlock> ALCHEMICAL_DOWNLINK = BLOCKS.register("alchemical_downlink",
            () -> new AlchemicalDownlinkBlock(machineProperties(MapColor.COLOR_ORANGE)));

    public static final RegistryObject<AlchemicalGaugeBlock> ALCHEMICAL_GAUGE = BLOCKS.register("alchemical_gauge",
            () -> new AlchemicalGaugeBlock(machineProperties(MapColor.COLOR_YELLOW)));

    public static final RegistryObject<AlchemicalInterfaceBlock> ALCHEMICAL_INTERFACE = BLOCKS.register("alchemical_interface",
            () -> new AlchemicalInterfaceBlock(machineProperties(MapColor.COLOR_LIGHT_BLUE)));

    public static final RegistryObject<TradePostBlock> TRADE_POST = BLOCKS.register("trade_post",
            () -> new TradePostBlock(machineProperties(MapColor.COLOR_GREEN)));

    public static final RegistryObject<AlchemicalSpringBlock> ALCHEMICAL_SPRING = BLOCKS.register("alchemical_spring",
            () -> new AlchemicalSpringBlock(machineProperties(MapColor.WATER)));

    public static final RegistryObject<AlchemicalExcavatorBlock> ALCHEMICAL_EXCAVATOR = BLOCKS.register("alchemical_excavator",
            () -> new AlchemicalExcavatorBlock(machineProperties(MapColor.COLOR_BROWN)));

    public static final RegistryObject<AlchemicalDisplayBlock> ALCHEMICAL_DISPLAY = BLOCKS.register("alchemical_display",
            () -> new AlchemicalDisplayBlock(machineProperties(MapColor.COLOR_MAGENTA)));

    public static final RegistryObject<AlchemicalHarvesterBlock> ALCHEMICAL_HARVESTER = BLOCKS.register("alchemical_harvester",
            () -> new AlchemicalHarvesterBlock(machineProperties(MapColor.PLANT)));

    public static final RegistryObject<AlchemicalPastureBlock> ALCHEMICAL_PASTURE = BLOCKS.register("alchemical_pasture",
            () -> new AlchemicalPastureBlock(machineProperties(MapColor.GRASS)));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties machineProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(3.5F, 1200F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .lightLevel(state -> 6);
    }
}
