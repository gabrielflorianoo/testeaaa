package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.item.AlchemicalAnchorItem;
import com.gabrielflorianoo.peqol.item.AppraisalLensItem;
import com.gabrielflorianoo.peqol.item.PhilosophersWrenchItem;
import com.gabrielflorianoo.peqol.item.SmeltingCharmItem;
import com.gabrielflorianoo.peqol.item.EffectCharmItem;
import com.gabrielflorianoo.peqol.item.FeatherCharmItem;
import com.gabrielflorianoo.peqol.item.UndyingCharmItem;
import com.gabrielflorianoo.peqol.item.WisdomCharmItem;
import com.gabrielflorianoo.peqol.item.CelestialHourglassItem;
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

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_HARVESTER = ITEMS.register("alchemical_harvester",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_HARVESTER.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_harvester"));

    public static final RegistryObject<OwnedBlockItem> ALCHEMICAL_PASTURE = ITEMS.register("alchemical_pasture",
            () -> new OwnedBlockItem(ModBlocks.ALCHEMICAL_PASTURE.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "alchemical_pasture"));
    public static final RegistryObject<CelestialHourglassItem> CELESTIAL_HOURGLASS = ITEMS.register("celestial_hourglass",
            () -> new CelestialHourglassItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public static final RegistryObject<EffectCharmItem> VISION_CHARM = ITEMS.register("vision_charm",
            () -> new EffectCharmItem(charm(), () -> net.minecraft.world.effect.MobEffects.NIGHT_VISION, 0, () -> com.gabrielflorianoo.peqol.config.PEQoLConfig.COMMON.visionCost));
    public static final RegistryObject<EffectCharmItem> HASTE_CHARM = ITEMS.register("haste_charm",
            () -> new EffectCharmItem(charm(), () -> net.minecraft.world.effect.MobEffects.DIG_SPEED, 1, () -> com.gabrielflorianoo.peqol.config.PEQoLConfig.COMMON.hasteCost));
    public static final RegistryObject<EffectCharmItem> BREATH_CHARM = ITEMS.register("breath_charm",
            () -> new EffectCharmItem(charm(), () -> net.minecraft.world.effect.MobEffects.WATER_BREATHING, 0, () -> com.gabrielflorianoo.peqol.config.PEQoLConfig.COMMON.breathCost));
    public static final RegistryObject<EffectCharmItem> STRENGTH_CHARM = ITEMS.register("strength_charm",
            () -> new EffectCharmItem(charm(), () -> net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 0, () -> com.gabrielflorianoo.peqol.config.PEQoLConfig.COMMON.strengthCost));
    public static final RegistryObject<UndyingCharmItem> UNDYING_CHARM = ITEMS.register("undying_charm",
            () -> new UndyingCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<FeatherCharmItem> FEATHER_CHARM = ITEMS.register("feather_charm",
            () -> new FeatherCharmItem(charm()));
    public static final RegistryObject<WisdomCharmItem> WISDOM_CHARM = ITEMS.register("wisdom_charm",
            () -> new WisdomCharmItem(charm()));

    public static final RegistryObject<com.gabrielflorianoo.peqol.item.AlchemistToolkitItem> ALCHEMIST_TOOLKIT = ITEMS.register("alchemist_toolkit",
            () -> new com.gabrielflorianoo.peqol.item.AlchemistToolkitItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.PocketSmelterItem> POCKET_SMELTER = ITEMS.register("pocket_smelter",
            () -> new com.gabrielflorianoo.peqol.item.PocketSmelterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));

    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.BlinkRodItem> BLINK_ROD = ITEMS.register("blink_rod",
            () -> new com.gabrielflorianoo.peqol.item.magic.BlinkRodItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.ChronalEchoItem> CHRONAL_ECHO = ITEMS.register("chronal_echo",
            () -> new com.gabrielflorianoo.peqol.item.magic.ChronalEchoItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.LumenWandItem> LUMEN_WAND = ITEMS.register("lumen_wand",
            () -> new com.gabrielflorianoo.peqol.item.magic.LumenWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.TransmutationScepterItem> TRANSMUTATION_SCEPTER = ITEMS.register("transmutation_scepter",
            () -> new com.gabrielflorianoo.peqol.item.magic.TransmutationScepterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.VerdantOrbItem> VERDANT_ORB = ITEMS.register("verdant_orb",
            () -> new com.gabrielflorianoo.peqol.item.magic.VerdantOrbItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.VoidSpongeItem> VOID_SPONGE = ITEMS.register("void_sponge",
            () -> new com.gabrielflorianoo.peqol.item.magic.VoidSpongeItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.SeerEyeItem> SEER_EYE = ITEMS.register("seer_eye",
            () -> new com.gabrielflorianoo.peqol.item.magic.SeerEyeItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.GravityStaffItem> GRAVITY_STAFF = ITEMS.register("gravity_staff",
            () -> new com.gabrielflorianoo.peqol.item.magic.GravityStaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.StormStaffItem> STORM_STAFF = ITEMS.register("storm_staff",
            () -> new com.gabrielflorianoo.peqol.item.magic.StormStaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.PrismaticWandItem> PRISMATIC_WAND = ITEMS.register("prismatic_wand",
            () -> new com.gabrielflorianoo.peqol.item.magic.PrismaticWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.RecallMirrorItem> RECALL_MIRROR = ITEMS.register("recall_mirror",
            () -> new com.gabrielflorianoo.peqol.item.magic.RecallMirrorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.CloudScepterItem> CLOUD_SCEPTER = ITEMS.register("cloud_scepter",
            () -> new com.gabrielflorianoo.peqol.item.magic.CloudScepterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.SoulJarItem> SOUL_JAR = ITEMS.register("soul_jar",
            () -> new com.gabrielflorianoo.peqol.item.magic.SoulJarItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.WardingLanternItem> WARDING_LANTERN = ITEMS.register("warding_lantern",
            () -> new com.gabrielflorianoo.peqol.item.magic.WardingLanternItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.TempusStoneItem> TEMPUS_STONE = ITEMS.register("tempus_stone",
            () -> new com.gabrielflorianoo.peqol.item.magic.TempusStoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.DowsingRodItem> DOWSING_ROD = ITEMS.register("dowsing_rod",
            () -> new com.gabrielflorianoo.peqol.item.magic.DowsingRodItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.CourierSigilItem> COURIER_SIGIL = ITEMS.register("courier_sigil",
            () -> new com.gabrielflorianoo.peqol.item.magic.CourierSigilItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.GaiaBrushItem> GAIA_BRUSH = ITEMS.register("gaia_brush",
            () -> new com.gabrielflorianoo.peqol.item.magic.GaiaBrushItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.WayfinderCompassItem> WAYFINDER_COMPASS = ITEMS.register("wayfinder_compass",
            () -> new com.gabrielflorianoo.peqol.item.magic.WayfinderCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.SoulbindingRuneItem> SOULBINDING_RUNE = ITEMS.register("soulbinding_rune",
            () -> new com.gabrielflorianoo.peqol.item.magic.SoulbindingRuneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.magic.ArchitectBottleItem> ARCHITECT_BOTTLE = ITEMS.register("architect_bottle",
            () -> new com.gabrielflorianoo.peqol.item.magic.ArchitectBottleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> ALCHEMICAL_CRAFTING_TABLE = ITEMS.register("alchemical_crafting_table",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ALCHEMICAL_CRAFTING_TABLE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<com.gabrielflorianoo.peqol.item.AlchemicalCraftingTabletItem> ALCHEMICAL_CRAFTING_TABLET = ITEMS.register("alchemical_crafting_tablet",
            () -> new com.gabrielflorianoo.peqol.item.AlchemicalCraftingTabletItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    private static Item.Properties charm() {
        return new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant();
    }

    private ModItems() {
    }
}
