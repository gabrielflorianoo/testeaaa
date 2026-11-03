package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalDownlinkBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalExporterBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalImporterBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalUplinkBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalVacuumBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PEQoL.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalUplinkBlockEntity>> ALCHEMICAL_UPLINK = BLOCK_ENTITIES.register("alchemical_uplink",
            () -> BlockEntityType.Builder.of(AlchemicalUplinkBlockEntity::new, ModBlocks.ALCHEMICAL_UPLINK.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalImporterBlockEntity>> ALCHEMICAL_IMPORTER = BLOCK_ENTITIES.register("alchemical_importer",
            () -> BlockEntityType.Builder.of(AlchemicalImporterBlockEntity::new, ModBlocks.ALCHEMICAL_IMPORTER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalExporterBlockEntity>> ALCHEMICAL_EXPORTER = BLOCK_ENTITIES.register("alchemical_exporter",
            () -> BlockEntityType.Builder.of(AlchemicalExporterBlockEntity::new, ModBlocks.ALCHEMICAL_EXPORTER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalVacuumBlockEntity>> ALCHEMICAL_VACUUM = BLOCK_ENTITIES.register("alchemical_vacuum",
            () -> BlockEntityType.Builder.of(AlchemicalVacuumBlockEntity::new, ModBlocks.ALCHEMICAL_VACUUM.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalDownlinkBlockEntity>> ALCHEMICAL_DOWNLINK = BLOCK_ENTITIES.register("alchemical_downlink",
            () -> BlockEntityType.Builder.of(AlchemicalDownlinkBlockEntity::new, ModBlocks.ALCHEMICAL_DOWNLINK.get()).build(null));

    private ModBlockEntities() {
    }
}
