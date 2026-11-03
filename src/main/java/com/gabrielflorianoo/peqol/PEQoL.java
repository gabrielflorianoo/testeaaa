package com.gabrielflorianoo.peqol;

import com.gabrielflorianoo.peqol.command.PEQoLCommand;
import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.event.CommonEvents;
import com.gabrielflorianoo.peqol.network.PEQoLNetwork;
import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.registry.ModBlocks;
import com.gabrielflorianoo.peqol.registry.ModCreativeTab;
import com.gabrielflorianoo.peqol.registry.ModItems;
import com.gabrielflorianoo.peqol.registry.ModStats;
import com.gabrielflorianoo.peqol.util.KnowledgeStats;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PEQoL.MODID)
public class PEQoL {

    public static final String MODID = "peqol";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PEQoL() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        ModStats.STATS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PEQoLConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PEQoLConfig.CLIENT_SPEC);

        modBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(CommonEvents.class);
        MinecraftForge.EVENT_BUS.register(KnowledgeStats.class);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PEQoLNetwork::register);
        event.enqueueWork(ModStats::init);
        LOGGER.info("ProjectE QoL+ ready to make alchemy less tedious");
    }

    private void registerCommands(RegisterCommandsEvent event) {
        PEQoLCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }
}
