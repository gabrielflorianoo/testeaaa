package com.gabrielflorianoo.peqol.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public final class PEQoLConfig {

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<Common, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
        Pair<Client, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    private PEQoLConfig() {
    }

    public enum HudCorner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public enum HudMode {
        ALWAYS, TABLET_IN_INVENTORY, NEVER
    }

    public static final class Common {
        // Keybind actions
        public final ForgeConfigSpec.BooleanValue keybindTabletRequiresItem;
        public final ForgeConfigSpec.BooleanValue keybindBagRequiresItem;
        public final ForgeConfigSpec.BooleanValue quickBurnEnabled;
        public final ForgeConfigSpec.BooleanValue quickBurnRequiresTablet;

        // Commands
        public final ForgeConfigSpec.BooleanValue payEnabled;
        public final ForgeConfigSpec.BooleanValue withdrawEnabled;
        public final ForgeConfigSpec.BooleanValue burnCommandEnabled;
        public final ForgeConfigSpec.BooleanValue burnCommandRequiresTablet;
        public final ForgeConfigSpec.BooleanValue burnSkipsItemsWithNbt;

        // Restocker
        public final ForgeConfigSpec.IntValue restockerInterval;
        public final ForgeConfigSpec.IntValue restockerThreshold;
        public final ForgeConfigSpec.BooleanValue restockerBlocksOnly;
        public final ForgeConfigSpec.BooleanValue restockerWholeHotbar;

        // Pick block
        public final ForgeConfigSpec.BooleanValue pickBlockEnabled;
        public final ForgeConfigSpec.BooleanValue pickBlockRequiresRestocker;
        public final ForgeConfigSpec.IntValue pickBlockAmount;

        // Charms
        public final ForgeConfigSpec.IntValue mendingInterval;
        public final ForgeConfigSpec.IntValue mendingPointsPerCycle;
        public final ForgeConfigSpec.DoubleValue mendingCostMultiplier;
        public final ForgeConfigSpec.IntValue satiationInterval;
        public final ForgeConfigSpec.LongValue stellarLinkRate;

        // Construction wand
        public final ForgeConfigSpec.BooleanValue wandEnabled;
        public final ForgeConfigSpec.BooleanValue wandUsesEmc;
        public final ForgeConfigSpec.IntValue wandMaxBlocks;

        // Illumination
        public final ForgeConfigSpec.IntValue illuminationInterval;
        public final ForgeConfigSpec.IntValue illuminationThreshold;
        public final ForgeConfigSpec.ConfigValue<String> illuminationItem;

        // Vacuum
        public final ForgeConfigSpec.IntValue vacuumRadius;
        public final ForgeConfigSpec.IntValue vacuumInterval;

        // Anchor
        public final ForgeConfigSpec.BooleanValue anchorEnabled;
        public final ForgeConfigSpec.LongValue anchorBaseCost;
        public final ForgeConfigSpec.LongValue anchorCostPerBlock;
        public final ForgeConfigSpec.LongValue anchorCrossDimensionCost;
        public final ForgeConfigSpec.IntValue anchorCooldown;

        // Smelting charm
        public final ForgeConfigSpec.LongValue smeltingCostPerItem;
        public final ForgeConfigSpec.BooleanValue smeltingOnlyOres;

        // Restocker extras
        public final ForgeConfigSpec.BooleanValue restockerReplacesTools;

        // Wrench / downlink
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> wrenchNamespaces;
        public final ForgeConfigSpec.LongValue downlinkRate;

        // XP conversion
        public final ForgeConfigSpec.BooleanValue xpEnabled;
        public final ForgeConfigSpec.LongValue xpEmcPerPoint;

        // Misc items / commands
        public final ForgeConfigSpec.LongValue voidPouchCost;
        public final ForgeConfigSpec.BooleanValue backEnabled;
        public final ForgeConfigSpec.IntValue maxHomes;
        public final ForgeConfigSpec.BooleanValue siphonCatchesMobDrops;

        // Prospector / server QoL
        public final ForgeConfigSpec.IntValue prospectorRadius;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> starterKnowledge;
        public final ForgeConfigSpec.BooleanValue learnOnCraft;
        public final ForgeConfigSpec.BooleanValue giftEnabled;
        public final ForgeConfigSpec.BooleanValue giveHandbook;
        public final ForgeConfigSpec.BooleanValue scoreboard;

        // Blacklist
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklist;

        // Buy command
        public final ForgeConfigSpec.BooleanValue buyEnabled;
        public final ForgeConfigSpec.BooleanValue buyRequiresTablet;
        public final ForgeConfigSpec.IntValue buyMaxCount;

        // Siphon
        public final ForgeConfigSpec.IntValue siphonMaxFilters;
        public final ForgeConfigSpec.BooleanValue siphonLearnsItems;

        // Pickup
        public final ForgeConfigSpec.BooleanValue autoLearnOnPickup;

        // Blocks
        public final ForgeConfigSpec.LongValue uplinkBufferCapacity;
        public final ForgeConfigSpec.IntValue interfaceMaxSlots;
        public final ForgeConfigSpec.IntValue excavatorRadius;
        public final ForgeConfigSpec.IntValue excavatorInterval;
        public final ForgeConfigSpec.LongValue excavatorCostPerBlock;
        public final ForgeConfigSpec.BooleanValue excavatorBurnsDrops;
        public final ForgeConfigSpec.LongValue springWaterCost;
        public final ForgeConfigSpec.LongValue springLavaCost;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledRecipes;
        public final ForgeConfigSpec.BooleanValue importerLearnsItems;
        public final ForgeConfigSpec.IntValue exporterInterval;
        public final ForgeConfigSpec.IntValue exporterItemsPerOperation;
        public final ForgeConfigSpec.IntValue exporterMaxFilters;

        // Knowledge scroll
        public final ForgeConfigSpec.BooleanValue knowledgeScrollEnabled;
        public final ForgeConfigSpec.IntValue knowledgeScrollMaxEntries;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Keybind driven actions (validated on the server)").push("keybinds");
            keybindTabletRequiresItem = builder.comment("Opening the transmutation GUI with the keybind requires a Transmutation Tablet somewhere in the inventory.")
                    .define("tabletRequiresItem", true);
            keybindBagRequiresItem = builder.comment("Opening an alchemical bag with the keybind requires that bag to be in the inventory. If false the white bag is opened when none is carried.")
                    .define("bagRequiresItem", true);
            quickBurnEnabled = builder.comment("Allow the 'Quick Burn' keybind which converts the held stack into personal EMC.")
                    .define("quickBurnEnabled", true);
            quickBurnRequiresTablet = builder.comment("Quick Burn requires a Transmutation Tablet in the inventory.")
                    .define("quickBurnRequiresTablet", true);
            builder.pop();

            builder.comment("/peqol command settings").push("commands");
            payEnabled = builder.comment("Allow players to send personal EMC to each other with /peqol pay.")
                    .define("payEnabled", true);
            withdrawEnabled = builder.comment("Allow players to withdraw personal EMC into EMC Cheques with /peqol withdraw.")
                    .define("withdrawEnabled", true);
            burnCommandEnabled = builder.comment("Allow /peqol burn to convert inventory items into personal EMC.")
                    .define("burnEnabled", true);
            burnCommandRequiresTablet = builder.comment("/peqol burn requires a Transmutation Tablet in the inventory.")
                    .define("burnRequiresTablet", true);
            burnSkipsItemsWithNbt = builder.comment("/peqol burn inventory skips items with NBT (enchanted gear, filled shulkers, Klein Stars...) for safety.")
                    .define("burnSkipsItemsWithNbt", true);
            builder.pop();

            builder.comment("Alchemical Restocker").push("restocker");
            restockerInterval = builder.comment("Ticks between restock checks.")
                    .defineInRange("interval", 10, 1, 1200);
            restockerThreshold = builder.comment("A stack is refilled once its count drops to this value or lower.")
                    .defineInRange("threshold", 16, 1, 64);
            restockerBlocksOnly = builder.comment("Only restock block items.")
                    .define("blocksOnly", true);
            restockerWholeHotbar = builder.comment("Restock the whole hotbar instead of only the selected slot and the offhand.")
                    .define("wholeHotbar", true);
            restockerReplacesTools = builder.comment("When a learned tool breaks in your hand, a fresh one is created from personal EMC.")
                    .define("replacesBrokenTools", true);
            builder.pop();

            builder.comment("Alchemical pick block: middle clicking a block you don't carry creates it from personal EMC").push("pickBlock");
            pickBlockEnabled = builder.define("enabled", true);
            pickBlockRequiresRestocker = builder.comment("Requires an active Alchemical Restocker in the inventory.")
                    .define("requiresRestocker", true);
            pickBlockAmount = builder.comment("How many items are created per pick (capped at the max stack size).")
                    .defineInRange("amount", 64, 1, 64);
            builder.pop();

            builder.comment("Alchemical Mending Charm").push("mending");
            mendingInterval = builder.comment("Ticks between repair cycles.")
                    .defineInRange("interval", 20, 1, 1200);
            mendingPointsPerCycle = builder.comment("Maximum durability points restored per item per cycle.")
                    .defineInRange("pointsPerCycle", 32, 1, 100_000);
            mendingCostMultiplier = builder.comment("Repairing the full durability costs (item EMC value * this).")
                    .defineInRange("costMultiplier", 1.0, 0.0, 1000.0);
            builder.pop();

            builder.comment("Charm of Satiation").push("satiation");
            satiationInterval = builder.comment("Ticks between hunger checks.")
                    .defineInRange("interval", 40, 1, 1200);
            builder.pop();

            builder.comment("Stellar Link").push("stellarLink");
            stellarLinkRate = builder.comment("Maximum EMC per second moved from personal EMC into Klein Stars.")
                    .defineInRange("ratePerSecond", 1_000_000L, 1L, Long.MAX_VALUE);
            builder.pop();

            builder.comment("Alchemical Construction Wand").push("constructionWand");
            wandEnabled = builder.define("enabled", true);
            wandUsesEmc = builder.comment("When the inventory runs out of the block, pay with personal EMC (learned blocks only).")
                    .define("usesEmc", true);
            wandMaxBlocks = builder.comment("Maximum blocks placed per use.")
                    .defineInRange("maxBlocks", 81, 1, 81);
            builder.pop();

            builder.comment("Charm of Illumination").push("illumination");
            illuminationInterval = builder.comment("Ticks between light checks.")
                    .defineInRange("interval", 10, 1, 1200);
            illuminationThreshold = builder.comment("A light is placed when both block and sky light at your feet are below this value.")
                    .defineInRange("threshold", 8, 1, 15);
            illuminationItem = builder.comment("Block item that is placed (must be a block item, e.g. minecraft:torch or minecraft:soul_torch).")
                    .define("item", "minecraft:torch");
            builder.pop();

            builder.comment("Alchemical Vacuum").push("vacuum");
            vacuumRadius = builder.comment("Radius (in blocks) around the vacuum where items are collected.")
                    .defineInRange("radius", 6, 1, 16);
            vacuumInterval = builder.comment("Ticks between collections.")
                    .defineInRange("interval", 10, 1, 1200);
            builder.pop();

            builder.comment("Alchemical Anchor (EMC teleport)").push("anchor");
            anchorEnabled = builder.define("enabled", true);
            anchorBaseCost = builder.comment("Flat EMC cost of every teleport.")
                    .defineInRange("baseCost", 1_000L, 0L, Long.MAX_VALUE);
            anchorCostPerBlock = builder.comment("Extra EMC per block of distance (same dimension).")
                    .defineInRange("costPerBlock", 16L, 0L, Long.MAX_VALUE);
            anchorCrossDimensionCost = builder.comment("Extra EMC when the anchor is in another dimension.")
                    .defineInRange("crossDimensionCost", 100_000L, 0L, Long.MAX_VALUE);
            anchorCooldown = builder.comment("Cooldown in ticks.")
                    .defineInRange("cooldown", 60, 0, 72_000);
            builder.pop();

            builder.comment("Charm of Smelting").push("smelting");
            smeltingCostPerItem = builder.comment("EMC paid for every item smelted on pickup (1/8 of coal's 128 EMC by default).")
                    .defineInRange("costPerItem", 16L, 0L, Long.MAX_VALUE);
            smeltingOnlyOres = builder.comment("Only smelt ores and raw materials (forge:ores / forge:raw_materials). If false, anything with a smelting recipe is smelted.")
                    .define("onlyOres", true);
            builder.pop();

            builder.comment("Philosopher's Wrench").push("wrench");
            wrenchNamespaces = builder.comment("Mod ids whose blocks (with block entities) can be picked up with their contents.")
                    .defineListAllowEmpty(List.of("namespaces"), () -> List.of("projecte", "peqol"), o -> o instanceof String);
            builder.pop();

            builder.comment("Alchemical Downlink").push("downlink");
            downlinkRate = builder.comment("Maximum EMC per half second drawn from personal EMC and pushed into adjacent blocks (also the buffer size).")
                    .defineInRange("rate", 50_000L, 1L, Long.MAX_VALUE);
            builder.pop();

            builder.comment("/peqol xp - convert experience points to EMC and back").push("experience");
            xpEnabled = builder.define("enabled", true);
            xpEmcPerPoint = builder.comment("EMC per experience point (same rate both ways).")
                    .defineInRange("emcPerPoint", 32L, 1L, 1_000_000_000L);
            builder.pop();

            builder.comment("Misc").push("misc");
            voidPouchCost = builder.comment("EMC paid every time the Void Pouch opens your ender chest.")
                    .defineInRange("voidPouchCost", 64L, 0L, Long.MAX_VALUE);
            backEnabled = builder.comment("Allow /peqol back (teleport to your last death point, paid like the Alchemical Anchor).")
                    .define("backEnabled", true);
            maxHomes = builder.comment("Maximum named homes per player for /peqol home (0 disables homes).")
                    .defineInRange("maxHomes", 5, 0, 100);
            siphonCatchesMobDrops = builder.comment("Mobs you kill drop filtered items straight into your EMC when a Siphon Charm is active.")
                    .define("siphonCatchesMobDrops", true);
            builder.pop();

            builder.comment("Server quality of life").push("server");
            prospectorRadius = builder.comment("Radius scanned by the Alchemical Prospector.")
                    .defineInRange("prospectorRadius", 12, 2, 32);
            starterKnowledge = builder.comment("Items every player learns the first time they join (item ids), e.g. [\"minecraft:cobblestone\", \"minecraft:dirt\"].")
                    .defineListAllowEmpty(List.of("starterKnowledge"), () -> List.<String>of(), o -> o instanceof String);
            learnOnCraft = builder.comment("Crafting an item with EMC also learns it (no burning needed).")
                    .define("learnOnCraft", false);
            scoreboard = builder.comment("Keep a 'peqol_emc' scoreboard objective updated with every online player's EMC (capped at 2,147,483,647).",
                    "Show it with: /scoreboard objectives setdisplay sidebar peqol_emc")
                    .define("scoreboard", true);
            giveHandbook = builder.comment("Give the Alchemist's Handbook (a guide book) to players on their first join.")
                    .define("giveHandbook", true);
            giftEnabled = builder.comment("Allow /peqol gift (create an item from your EMC directly in another player's inventory).")
                    .define("giftEnabled", true);
            builder.pop();

            builder.comment("Blacklist for every feature that CREATES items from personal EMC (buy, pick block, restocker, exporter, wand...).",
                    "Accepts item ids (minecraft:diamond) and item tags (#forge:ores).").push("blacklist");
            blacklist = builder.defineListAllowEmpty(List.of("items"), () -> List.<String>of(), o -> o instanceof String);
            builder.pop();

            builder.comment("/peqol buy").push("buy");
            buyEnabled = builder.comment("Allow creating learned items from personal EMC with /peqol buy.")
                    .define("enabled", true);
            buyRequiresTablet = builder.comment("/peqol buy requires a Transmutation Tablet in the inventory.")
                    .define("requiresTablet", true);
            buyMaxCount = builder.comment("Maximum items per /peqol buy.")
                    .defineInRange("maxCount", 2304, 1, 100_000);
            builder.pop();

            builder.comment("Siphon Charm").push("siphon");
            siphonMaxFilters = builder.comment("Maximum amount of items a Siphon Charm can filter.")
                    .defineInRange("maxFilters", 27, 1, 256);
            siphonLearnsItems = builder.comment("Items converted by the Siphon Charm are also learned.")
                    .define("learnsItems", true);
            builder.pop();

            builder.comment("Item pickup").push("pickup");
            autoLearnOnPickup = builder.comment("Automatically learn (without consuming) items with EMC that a player picks up while carrying a Transmutation Tablet. Off by default as it skips the classic 'burn to learn' loop.")
                    .define("autoLearnOnPickup", false);
            builder.pop();

            builder.comment("Personal EMC automation blocks").push("blocks");
            uplinkBufferCapacity = builder.comment("EMC an Alchemical Uplink can buffer while its owner is offline.")
                    .defineInRange("uplinkBufferCapacity", 1_000_000_000L, 1L, Long.MAX_VALUE);
            excavatorRadius = builder.comment("Alchemical Excavator: radius of the square mined below it (4 = 9x9).")
                    .defineInRange("excavatorRadius", 4, 1, 16);
            excavatorInterval = builder.comment("Alchemical Excavator: ticks between mining operations.")
                    .defineInRange("excavatorInterval", 10, 1, 1200);
            excavatorCostPerBlock = builder.comment("Alchemical Excavator: EMC paid per block mined.")
                    .defineInRange("excavatorCostPerBlock", 8L, 0L, Long.MAX_VALUE);
            excavatorBurnsDrops = builder.comment("Alchemical Excavator: drops with EMC become personal EMC instead of items.")
                    .define("excavatorBurnsDrops", true);
            springWaterCost = builder.comment("Alchemical Spring: EMC per bucket of water when ProjectE gives buckets no usable value.")
                    .defineInRange("springWaterCost", 1L, 0L, Long.MAX_VALUE);
            springLavaCost = builder.comment("Alchemical Spring: EMC per bucket of lava when ProjectE gives buckets no usable value.")
                    .defineInRange("springLavaCost", 64L, 0L, Long.MAX_VALUE);
            interfaceMaxSlots = builder.comment("Maximum learned items an Alchemical Interface exposes (large numbers can slow storage mods).")
                    .defineInRange("interfaceMaxSlots", 4096, 1, 100_000);
            disabledRecipes = builder.comment("QoL+ recipes to disable (item paths, e.g. [\"alchemical_anchor\", \"alchemical_interface\"]). Needs a world reload.")
                    .defineListAllowEmpty(List.of("disabledRecipes"), () -> List.<String>of(), o -> o instanceof String);
            importerLearnsItems = builder.comment("Items inserted into an Alchemical Importer are learned by its owner.")
                    .define("importerLearnsItems", true);
            exporterInterval = builder.comment("Ticks between Alchemical Exporter push operations.")
                    .defineInRange("exporterInterval", 10, 1, 1200);
            exporterItemsPerOperation = builder.comment("Maximum items per filter an Alchemical Exporter pushes per operation.")
                    .defineInRange("exporterItemsPerOperation", 16, 1, 64);
            exporterMaxFilters = builder.comment("Maximum amount of filter entries in an Alchemical Exporter.")
                    .defineInRange("exporterMaxFilters", 9, 1, 64);
            builder.pop();

            builder.comment("Knowledge Scroll").push("knowledgeScroll");
            knowledgeScrollEnabled = builder.comment("Allow Knowledge Scrolls to record and teach knowledge.")
                    .define("enabled", true);
            knowledgeScrollMaxEntries = builder.comment("Maximum amount of entries recorded in a single scroll (keeps item NBT small).")
                    .defineInRange("maxEntries", 4096, 1, 100_000);
            builder.pop();
        }
    }

    public static final class Client {
        public final ForgeConfigSpec.EnumValue<HudMode> hudMode;
        public final ForgeConfigSpec.EnumValue<HudCorner> hudCorner;
        public final ForgeConfigSpec.IntValue hudOffsetX;
        public final ForgeConfigSpec.IntValue hudOffsetY;
        public final ForgeConfigSpec.BooleanValue hudShowRate;
        public final ForgeConfigSpec.BooleanValue hudShowKnowledge;
        public final ForgeConfigSpec.BooleanValue hudCompactNumbers;
        public final ForgeConfigSpec.BooleanValue hudBackground;
        public final ForgeConfigSpec.BooleanValue hudShowActiveCharms;
        public final ForgeConfigSpec.BooleanValue hudShowHeldValue;
        public final ForgeConfigSpec.BooleanValue milestoneToasts;
        public final ForgeConfigSpec.BooleanValue hudShowKleinStars;
        public final ForgeConfigSpec.BooleanValue learnToasts;
        public final ForgeConfigSpec.BooleanValue machineInfo;
        public final ForgeConfigSpec.BooleanValue markUnlearned;
        public final ForgeConfigSpec.ConfigValue<String> goalItem;
        public final ForgeConfigSpec.IntValue goalCount;

        public final ForgeConfigSpec.BooleanValue tooltipAffordable;
        public final ForgeConfigSpec.BooleanValue tooltipSellValue;
        public final ForgeConfigSpec.BooleanValue tooltipContainerContents;
        public final ForgeConfigSpec.BooleanValue tooltipCompactNumbers;
        public final ForgeConfigSpec.BooleanValue tooltipNoEmcWarning;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Personal EMC HUD").push("hud");
            hudMode = builder.comment("When to show the EMC HUD. Can also be toggled in-game with a keybind.")
                    .defineEnum("mode", HudMode.TABLET_IN_INVENTORY);
            hudCorner = builder.defineEnum("corner", HudCorner.TOP_LEFT);
            hudOffsetX = builder.defineInRange("offsetX", 4, 0, 4000);
            hudOffsetY = builder.defineInRange("offsetY", 4, 0, 4000);
            hudShowRate = builder.comment("Show EMC gained/lost per second.")
                    .define("showRate", true);
            hudShowKnowledge = builder.comment("Show the amount of learned items.")
                    .define("showKnowledge", true);
            hudCompactNumbers = builder.comment("Use compact numbers like 1.25M instead of 1,250,000.")
                    .define("compactNumbers", true);
            hudBackground = builder.define("background", true);
            hudShowActiveCharms = builder.comment("List the active QoL+ charms under the EMC line.")
                    .define("showActiveCharms", true);
            hudShowHeldValue = builder.comment("Show the EMC value of the held stack.")
                    .define("showHeldValue", true);
            goalItem = builder.comment("EMC goal shown on the HUD (item id, empty = none). Set it in game with /peqolclient goal <item> [count].")
                    .define("goalItem", "");
            goalCount = builder.defineInRange("goalCount", 1, 1, 1_000_000);
            markUnlearned = builder.comment("Mark inventory slots holding items that have EMC but aren't learned yet with a small purple dot.")
                    .define("markUnlearned", true);
            machineInfo = builder.comment("Show a small info panel when looking at QoL+ machines and ProjectE EMC blocks.")
                    .define("machineInfo", true);
            learnToasts = builder.comment("Show a toast when you learn new items.")
                    .define("learnToasts", true);
            hudShowKleinStars = builder.comment("Show the total EMC stored in the Klein Stars you carry.")
                    .define("showKleinStars", true);
            milestoneToasts = builder.comment("Show a toast when your personal EMC crosses 1K, 10K, 100K, 1M, ...")
                    .define("milestoneToasts", true);
            builder.pop();

            builder.comment("Extra tooltip lines (shown while holding SHIFT, like ProjectE's own)").push("tooltips");
            tooltipAffordable = builder.comment("Show how many of an item you can transmute with your current EMC.")
                    .define("affordable", true);
            tooltipSellValue = builder.comment("Show the sell value when it differs from the transmutation value.")
                    .define("sellValue", true);
            tooltipContainerContents = builder.comment("Show the total EMC of items stored inside shulker boxes and similar containers.")
                    .define("containerContents", true);
            tooltipCompactNumbers = builder.comment("Use compact numbers in the extra tooltip lines.")
                    .define("compactNumbers", false);
            tooltipNoEmcWarning = builder.comment("Show a line on items that have no EMC value at all.")
                    .define("noEmcWarning", true);
            builder.pop();
        }
    }
}
