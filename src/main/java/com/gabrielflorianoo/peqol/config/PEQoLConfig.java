package com.gabrielflorianoo.peqol.config;

import net.minecraftforge.common.ForgeConfigSpec;
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
