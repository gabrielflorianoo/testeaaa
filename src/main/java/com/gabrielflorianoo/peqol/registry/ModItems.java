package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.item.AlchemicalAnchorItem;
import com.gabrielflorianoo.peqol.item.AppraisalLensItem;
import com.gabrielflorianoo.peqol.item.PhilosophersWrenchItem;
import com.gabrielflorianoo.peqol.item.SmeltingCharmItem;
import com.gabrielflorianoo.peqol.item.MachineUpgradeItem;
import com.gabrielflorianoo.peqol.item.ProspectorItem;
import com.gabrielflorianoo.peqol.item.PocketWorkbenchItem;
import com.gabrielflorianoo.peqol.item.VoidPouchItem;
import com.gabrielflorianoo.peqol.item.ConstructionWandItem;
import com.gabrielflorianoo.peqol.item.EmcChequeItem;
import com.gabrielflorianoo.peqol.item.IlluminationCharmItem;
import com.gabrielflorianoo.peqol.item.KnowledgeScrollItem;
import com.gabrielflorianoo.peqol.item.OwnedBlockItem;
import com.gabrielflorianoo.peqol.item.RestockerTalismanItem;
import com.gabrielflorianoo.peqol.item.MendingCharmItem;
import com.gabrielflorianoo.peqol.item.SatiationCharmItem;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
import com.gabrielflorianoo.peqol.item.StellarLinkItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PEQoL.MODID);

    public static final RegistryObject<RestockerTalismanItem> RESTOCKER_TALISMAN = ITEMS.register("restocker_talisman",
            () -> new RestockerTalismanItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<SiphonCharmItem> SIPHON_CHARM = ITEMS.register("siphon_charm",
            () -> new SiphonCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<MendingCharmItem> MENDING_CHARM = ITEMS.register("mending_charm",
            () -> new MendingCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<SatiationCharmItem> SATIATION_CHARM = ITEMS.register("satiation_charm",
            () -> new SatiationCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<StellarLinkItem> STELLAR_LINK = ITEMS.register("stellar_link",
            () -> new StellarLinkItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<IlluminationCharmItem> ILLUMINATION_CHARM = ITEMS.register("illumination_charm",
            () -> new IlluminationCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<ConstructionWandItem> CONSTRUCTION_WAND = ITEMS.register("construction_wand",
            () -> new ConstructionWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<AppraisalLensItem> APPRAISAL_LENS = ITEMS.register("appraisal_lens",
            () -> new AppraisalLensItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<AlchemicalAnchorItem> ALCHEMICAL_ANCHOR = ITEMS.register("alchemical_anchor",
            () -> new AlchemicalAnchorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<SmeltingCharmItem> SMELTING_CHARM = ITEMS.register("smelting_charm",
            () -> new SmeltingCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<EmcChequeItem> EMC_CHEQUE = ITEMS.register("emc_cheque",
            () -> new EmcChequeItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<KnowledgeScrollItem> KNOWLEDGE_SCROLL = ITEMS.register("knowledge_scroll",
            () -> new KnowledgeScrollItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_UPLINK = ITEMS.register("alchemical_uplink",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_UPLINK.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_uplink"));
    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_IMPORTER = ITEMS.register("alchemical_importer",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_IMPORTER.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_importer"));
    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_EXPORTER = ITEMS.register("alchemical_exporter",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_EXPORTER.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_exporter"));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_VACUUM = ITEMS.register("alchemical_vacuum",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_VACUUM.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_vacuum"));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_DOWNLINK = ITEMS.register("alchemical_downlink",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_DOWNLINK.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_downlink"));
    public static final RegistryObject<PhilosophersWrenchItem> PHILOSOPHERS_WRENCH = ITEMS.register("philosophers_wrench",
            () -> new PhilosophersWrenchItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_GAUGE = ITEMS.register("alchemical_gauge",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_GAUGE.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_gauge"));

    public static final RegistryObject<PocketWorkbenchItem> POCKET_WORKBENCH = ITEMS.register("pocket_workbench",
            () -> new PocketWorkbenchItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<VoidPouchItem> VOID_POUCH = ITEMS.register("void_pouch",
            () -> new VoidPouchItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<ProspectorItem> PROSPECTOR = ITEMS.register("prospector",
            () -> new ProspectorItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<MachineUpgradeItem> MACHINE_UPGRADE = ITEMS.register("machine_upgrade",
            () -> new MachineUpgradeItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_INTERFACE = ITEMS.register("alchemical_interface",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_INTERFACE.get(), new Item.Properties().rarity(Rarity.RARE), "alchemical_interface"));

    public static final RegistryObject<OwnedBlockItem> TRADE_POST = ITEMS.register("trade_post",
            () -> new OwnedBlockItem(ModBlocks.TRADE_POST.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "trade_post"));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_SPRING = ITEMS.register("alchemical_spring",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_SPRING.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_spring"));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_EXCAVATOR = ITEMS.register("alchemical_excavator",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_EXCAVATOR.get(), new Item.Properties().rarity(Rarity.RARE), "alchemical_excavator"));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_DISPLAY = ITEMS.register("alchemical_display",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_DISPLAY.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_display"));

    private ModItems() {
    }
}
