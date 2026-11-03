package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.item.AppraisalLensItem;
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

    private ModItems() {
    }
}
