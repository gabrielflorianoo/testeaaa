package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.block.AlchemicalDownlinkBlock;
import com.gabrielflorianoo.peqol.block.AlchemicalExporterBlock;
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
