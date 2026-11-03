package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalDownlinkBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalExporterBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalGaugeBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalInterfaceBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalHarvesterBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalDisplayBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalExcavatorBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalSpringBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.TradePostBlockEntity;
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

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalGaugeBlockEntity>> ALCHEMICAL_GAUGE = BLOCK_ENTITIES.register("alchemical_gauge",
            () -> BlockEntityType.Builder.of(AlchemicalGaugeBlockEntity::new, ModBlocks.ALCHEMICAL_GAUGE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalInterfaceBlockEntity>> ALCHEMICAL_INTERFACE = BLOCK_ENTITIES.register("alchemical_interface",
            () -> BlockEntityType.Builder.of(AlchemicalInterfaceBlockEntity::new, ModBlocks.ALCHEMICAL_INTERFACE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TradePostBlockEntity>> TRADE_POST = BLOCK_ENTITIES.register("trade_post",
            () -> BlockEntityType.Builder.of(TradePostBlockEntity::new, ModBlocks.TRADE_POST.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalSpringBlockEntity>> ALCHEMICAL_SPRING = BLOCK_ENTITIES.register("alchemical_spring",
            () -> BlockEntityType.Builder.of(AlchemicalSpringBlockEntity::new, ModBlocks.ALCHEMICAL_SPRING.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalExcavatorBlockEntity>> ALCHEMICAL_EXCAVATOR = BLOCK_ENTITIES.register("alchemical_excavator",
            () -> BlockEntityType.Builder.of(AlchemicalExcavatorBlockEntity::new, ModBlocks.ALCHEMICAL_EXCAVATOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalDisplayBlockEntity>> ALCHEMICAL_DISPLAY = BLOCK_ENTITIES.register("alchemical_display",
            () -> BlockEntityType.Builder.of(AlchemicalDisplayBlockEntity::new, ModBlocks.ALCHEMICAL_DISPLAY.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AlchemicalHarvesterBlockEntity>> ALCHEMICAL_HARVESTER = BLOCK_ENTITIES.register("alchemical_harvester",
            () -> BlockEntityType.Builder.of(AlchemicalHarvesterBlockEntity::new, ModBlocks.ALCHEMICAL_HARVESTER.get()).build(null));

    private ModBlockEntities() {
    }
}
