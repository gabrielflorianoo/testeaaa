package com.gabrielflorianoo.peqol.gametest;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalImporterBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalUplinkBlockEntity;
import com.gabrielflorianoo.peqol.item.EmcChequeItem;
import com.gabrielflorianoo.peqol.item.MendingCharmItem;
import com.gabrielflorianoo.peqol.item.SatiationCharmItem;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalGaugeBlockEntity;
import com.gabrielflorianoo.peqol.item.PhilosophersWrenchItem;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalDownlinkBlockEntity;
import com.gabrielflorianoo.peqol.util.AutoStock;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.gabrielflorianoo.peqol.item.SmeltingCharmItem;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalVacuumBlockEntity;
import com.gabrielflorianoo.peqol.registry.ModStats;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import com.gabrielflorianoo.peqol.item.WandPlacement;
import com.gabrielflorianoo.peqol.util.KnowledgeStats;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import com.gabrielflorianoo.peqol.registry.ModBlocks;
import com.gabrielflorianoo.peqol.registry.ModItems;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder(PEQoL.MODID)
@PrefixGameTestTemplate(false)
public class PEQoLGameTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty")
    public static void projecteEmcValuesAreLoaded(GameTestHelper helper) {
        helper.assertTrue(EmcUtil.value(new ItemStack(Items.COBBLESTONE)) > 0, "Cobblestone should have EMC");
        helper.assertTrue(EmcUtil.value(new ItemStack(Items.DIAMOND)) > EmcUtil.value(new ItemStack(Items.IRON_INGOT)), "Diamond should be worth more than iron");
        helper.assertTrue(EmcUtil.value(new ItemStack(ModItems.EMC_CHEQUE.get())) == 0, "EMC cheques must never have EMC");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void amountParsingAndFormatting(GameTestHelper helper) {
        helper.assertTrue(BigInteger.valueOf(1500).equals(EmcUtil.parseAmount("1.5k")), "1.5k");
        helper.assertTrue(BigInteger.valueOf(20_000_000).equals(EmcUtil.parseAmount("20M")), "20M");
        helper.assertTrue(BigInteger.valueOf(1234).equals(EmcUtil.parseAmount("1,234")), "1,234");
        helper.assertTrue(EmcUtil.parseAmount("-5") == null, "negative amounts are rejected");
        helper.assertTrue(EmcUtil.parseAmount("abc") == null, "garbage is rejected");
        helper.assertTrue("1.25M".equals(EmcUtil.compact(BigInteger.valueOf(1_250_000))), "compact 1.25M but was " + EmcUtil.compact(BigInteger.valueOf(1_250_000)));
        helper.assertTrue("999".equals(EmcUtil.compact(BigInteger.valueOf(999))), "compact 999");
        helper.assertTrue("12.3K".equals(EmcUtil.compact(BigInteger.valueOf(12_345))), "compact 12.3K but was " + EmcUtil.compact(BigInteger.valueOf(12_345)));
        helper.assertTrue(EmcUtil.affordable(BigInteger.valueOf(100), 7) == 14, "affordable");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chequeRoundTrip(GameTestHelper helper) {
        BigInteger amount = new BigInteger("123456789012345678901234567890");
        ItemStack cheque = EmcChequeItem.create(amount, "tester");
        helper.assertTrue(amount.equals(EmcChequeItem.getAmount(cheque)), "Cheque amount should survive NBT");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void siphonFilterMatching(GameTestHelper helper) {
        ItemStack charm = new ItemStack(ModItems.SIPHON_CHARM.get());
        ListTag list = new ListTag();
        list.add(StringTag.valueOf("minecraft:cobblestone"));
        charm.getOrCreateTag().put(SiphonCharmItem.TAG_FILTER, list);
        helper.assertTrue(SiphonCharmItem.matches(charm, new ItemStack(Items.COBBLESTONE)), "cobblestone should match");
        helper.assertFalse(SiphonCharmItem.matches(charm, new ItemStack(Items.DIRT)), "dirt should not match");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void importerRejectsWithoutOwner(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_IMPORTER.get());
        AlchemicalImporterBlockEntity importer = (AlchemicalImporterBlockEntity) helper.getBlockEntity(POS);
        IItemHandler handler = importer.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        ItemStack stack = new ItemStack(Items.COBBLESTONE, 10);
        helper.assertTrue(handler.insertItem(0, stack, false).getCount() == 10, "Ownerless importer must not eat items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void importerBuffersForOfflineOwner(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_IMPORTER.get());
        AlchemicalImporterBlockEntity importer = (AlchemicalImporterBlockEntity) helper.getBlockEntity(POS);
        //The mock player is not in the server player list, so it counts as an offline owner
        importer.getOwnerData().set(helper.makeMockPlayer());
        IItemHandler handler = importer.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.BEDROCK), false).getCount() == 1, "Items without EMC are rejected");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.COBBLESTONE, 10), false).isEmpty(), "Items with EMC are accepted");
        BigInteger expected = BigInteger.valueOf(EmcUtil.sellValue(new ItemStack(Items.COBBLESTONE)) * 10);
        helper.assertTrue(expected.equals(importer.getPendingEmc()), "Pending EMC should be " + expected + " but was " + importer.getPendingEmc());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void uplinkAcceptsCollectorEmc(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_UPLINK.get());
        AlchemicalUplinkBlockEntity uplink = (AlchemicalUplinkBlockEntity) helper.getBlockEntity(POS);
        helper.assertTrue(uplink.insertEmc(500, IEmcStorage.EmcAction.EXECUTE) == 500, "Uplink should accept EMC");
        helper.assertTrue(uplink.extractEmc(100, IEmcStorage.EmcAction.EXECUTE) == 0, "Uplink should not hand EMC to other blocks");
        helper.assertTrue(uplink.getStoredEmc() == 500, "Uplink should buffer without an owner");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mendingCostIsBasedOnEmc(GameTestHelper helper) {
        long cost = MendingCharmItem.costPerDurability(new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(cost > 0, "Iron pickaxe should be repairable with EMC");
        helper.assertTrue(MendingCharmItem.costPerDurability(new ItemStack(Items.DIRT)) == 0, "Dirt is not damageable");
        long diamond = MendingCharmItem.costPerDurability(new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(diamond > 0, "Diamond pickaxe should be repairable with EMC");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void kleinStarChargeAndDrain(GameTestHelper helper) {
        Item kleinStar = ForgeRegistries.ITEMS.getValue(new ResourceLocation("projecte", "klein_star_ein"));
        helper.assertTrue(kleinStar != null && kleinStar != Items.AIR, "ProjectE Klein Star should exist");
        ItemStack star = new ItemStack(kleinStar);
        helper.assertTrue(EmcUtil.isEmcHolder(star), "Klein Star should expose the EMC holder capability");
        helper.assertTrue(EmcUtil.chargeHolder(star, 1000) == 1000, "Klein Star should accept 1000 EMC");
        helper.assertTrue(EmcUtil.drainHolder(star, Long.MAX_VALUE) == 1000, "Klein Star should give the 1000 EMC back");
        helper.assertTrue(EmcUtil.drainHolder(star, Long.MAX_VALUE) == 0, "Klein Star should now be empty");
        helper.assertTrue(EmcUtil.chargeHolder(new ItemStack(Items.STONE), 1000) == 0, "Stone can't hold EMC");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void satiationFoodSelection(GameTestHelper helper) {
        ItemStack charm = new ItemStack(ModItems.SATIATION_CHARM.get());
        helper.assertTrue(SatiationCharmItem.getFood(charm) == null, "No food by default");
        charm.getOrCreateTag().putString(SatiationCharmItem.TAG_FOOD, "minecraft:cooked_beef");
        helper.assertTrue(SatiationCharmItem.getFood(charm) == Items.COOKED_BEEF, "Food should be cooked beef");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void uplinkComparatorSignal(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_UPLINK.get());
        AlchemicalUplinkBlockEntity uplink = (AlchemicalUplinkBlockEntity) helper.getBlockEntity(POS);
        helper.assertTrue(uplink.getComparatorSignal() == 0, "Empty uplink gives no signal");
        uplink.insertEmc(1, IEmcStorage.EmcAction.EXECUTE);
        helper.assertTrue(uplink.getComparatorSignal() >= 1, "Any EMC gives at least signal 1");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void constructionWandFindsFace(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
        }
        BlockPos middle = helper.absolutePos(new BlockPos(1, 1, 1));
        List<BlockPos> targets = WandPlacement.findTargets(helper.getLevel(), middle, Direction.UP, 9);
        helper.assertTrue(targets.size() == 3, "Expected 3 targets on top of the row, got " + targets.size());
        helper.assertTrue(targets.get(0).equals(middle.above()), "Closest target first");
        //Block one of the spots
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.DIRT);
        targets = WandPlacement.findTargets(helper.getLevel(), middle, Direction.UP, 9);
        helper.assertTrue(targets.size() == 2, "Expected 2 targets with one obstructed, got " + targets.size());
        helper.assertTrue(WandPlacement.findTargets(helper.getLevel(), middle, Direction.UP, 1).size() == 1, "Size limit");
        helper.assertFalse(WandPlacement.canCopy(Blocks.CHEST.defaultBlockState()), "Chests can't be copied");
        helper.assertFalse(WandPlacement.canCopy(Blocks.OAK_DOOR.defaultBlockState()), "Doors can't be copied");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void knowledgeStatsCountsItemsWithEmc(GameTestHelper helper) {
        int count = KnowledgeStats.itemsWithEmc();
        helper.assertTrue(count > 100, "Expected many items with EMC, got " + count);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smeltingCharmSmeltsOres(GameTestHelper helper) {
        ItemStack result = SmeltingCharmItem.smeltResult(helper.getLevel(), new ItemStack(Items.RAW_IRON, 5));
        helper.assertTrue(result.is(Items.IRON_INGOT) && result.getCount() == 5, "5 raw iron should smelt into 5 iron ingots, got " + result);
        helper.assertTrue(SmeltingCharmItem.smeltResult(helper.getLevel(), new ItemStack(Items.OAK_LOG)).isEmpty(), "Logs are not ores");
        helper.assertTrue(SmeltingCharmItem.smeltResult(helper.getLevel(), new ItemStack(Items.DIRT)).isEmpty(), "Dirt can't be smelted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blacklistIsEmptyByDefault(GameTestHelper helper) {
        helper.assertFalse(EmcUtil.isBlacklisted(new ItemStack(Items.DIAMOND)), "Nothing is blacklisted by default");
        helper.assertTrue(Stats.CUSTOM.contains(ModStats.ITEMS_BURNED), "Custom stats should be registered");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void vacuumCollectsDroppedItems(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_VACUUM.get());
        AlchemicalVacuumBlockEntity vacuum = (AlchemicalVacuumBlockEntity) helper.getBlockEntity(POS);
        vacuum.getOwnerData().set(helper.makeMockPlayer());
        ItemEntity dropped = helper.spawnItem(Items.COBBLESTONE, 1.5F, 2.5F, 1.5F);
        dropped.setItem(new ItemStack(Items.COBBLESTONE, 16));
        helper.succeedWhen(() -> {
            helper.assertTrue(vacuum.getPendingEmc().signum() > 0, "Vacuum should have buffered EMC for its offline owner");
            helper.assertFalse(dropped.isAlive(), "The item entity should be gone");
        });
    }

    @GameTest(template = "empty")
    public static void wrenchKeepsRelayEmc(GameTestHelper helper) {
        Block relay = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("projecte", "relay_mk1"));
        helper.assertTrue(relay != null && relay != Blocks.AIR, "ProjectE relay should exist");
        helper.setBlock(POS, relay);
        BlockEntity be = helper.getBlockEntity(POS);
        IEmcStorage storage = be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY).orElseThrow(IllegalStateException::new);
        long inserted = storage.insertEmc(1000, IEmcStorage.EmcAction.EXECUTE);
        helper.assertTrue(inserted > 0, "Relay should accept EMC");
        ItemStack picked = PhilosophersWrenchItem.pickUp(helper.getLevel(), helper.absolutePos(POS), helper.makeMockPlayer());
        helper.assertFalse(picked.isEmpty(), "Wrench should pick the relay up");
        CompoundTag data = BlockItem.getBlockEntityData(picked);
        helper.assertTrue(data != null && data.getLong("EMC") == inserted, "Picked relay should keep its EMC, data: " + data);
        helper.assertBlockPresent(Blocks.AIR, POS);
        helper.assertFalse(PhilosophersWrenchItem.canWrench(Blocks.CHEST.defaultBlockState(), null), "Vanilla blocks are not wrenchable");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void downlinkOnlyTakesPersonalEmc(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_DOWNLINK.get());
        AlchemicalDownlinkBlockEntity downlink = (AlchemicalDownlinkBlockEntity) helper.getBlockEntity(POS);
        helper.assertTrue(downlink.insertEmc(500, IEmcStorage.EmcAction.EXECUTE) == 0, "Downlink must not accept EMC from other blocks");
        helper.assertTrue(downlink.getStoredEmc() == 0, "Downlink starts empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void autostockRoundTrip(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ResourceLocation arrow = new ResourceLocation("minecraft", "arrow");
        AutoStock.set(player, arrow, 64);
        helper.assertTrue(AutoStock.get(player).getOrDefault(arrow, 0) == 64, "Autostock entry should be saved");
        player.getInventory().add(new ItemStack(Items.ARROW, 10));
        helper.assertTrue(AutoStock.count(player.getInventory(), Items.ARROW) == 10, "Should count 10 arrows");
        AutoStock.set(player, arrow, 0);
        helper.assertTrue(AutoStock.get(player).isEmpty(), "Entry should be removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gaugeSignalScale(GameTestHelper helper) {
        helper.assertTrue(AlchemicalGaugeBlockEntity.signalFor(BigInteger.ZERO) == 0, "0 EMC = 0");
        helper.assertTrue(AlchemicalGaugeBlockEntity.signalFor(BigInteger.valueOf(7)) == 1, "7 EMC = 1");
        helper.assertTrue(AlchemicalGaugeBlockEntity.signalFor(BigInteger.valueOf(1_000_000)) == 7, "1M EMC = 7");
        helper.assertTrue(AlchemicalGaugeBlockEntity.signalFor(BigInteger.TEN.pow(40)) == 15, "huge EMC caps at 15");
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_GAUGE.get());
        AlchemicalGaugeBlockEntity gauge = (AlchemicalGaugeBlockEntity) helper.getBlockEntity(POS);
        gauge.getOwnerData().set(helper.makeMockPlayer());
        helper.runAfterDelay(25, () -> {
            //Unknown offline owner reads as 0 EMC
            helper.assertTrue(gauge.getSignal() == 0, "Gauge of a player without data should read 0");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void machineInfoDescribesBlocks(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_UPLINK.get());
        List<net.minecraft.network.chat.Component> uplink = com.gabrielflorianoo.peqol.network.MachineInfoRequestPacket.describe(helper.getBlockEntity(POS));
        helper.assertTrue(uplink.size() >= 3, "Uplink should describe owner, buffer and rate, got " + uplink.size());
        Block relay = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("projecte", "relay_mk1"));
        BlockPos relayPos = new BlockPos(0, 1, 0);
        helper.setBlock(relayPos, relay);
        List<net.minecraft.network.chat.Component> relayInfo = com.gabrielflorianoo.peqol.network.MachineInfoRequestPacket.describe(helper.getBlockEntity(relayPos));
        helper.assertTrue(relayInfo.size() == 1, "Relay should show its stored EMC, got " + relayInfo.size());
        helper.assertTrue(com.gabrielflorianoo.peqol.network.MachineInfoRequestPacket.describe(null).isEmpty(), "No block entity = no info");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void backFindsSafeSpot(GameTestHelper helper) {
        BlockPos start = helper.absolutePos(new BlockPos(1, 0, 1));
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        BlockPos safe = com.gabrielflorianoo.peqol.command.PEQoLCommand.findSafeSpot(helper.getLevel(), start);
        helper.assertTrue(safe != null && safe.getY() == start.getY() + 2, "Safe spot should be right above the two stone blocks, got " + safe);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void prospectorCountsOres(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.DIAMOND_ORE);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.DIAMOND_ORE);
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.IRON_ORE);
        com.gabrielflorianoo.peqol.item.ProspectorItem.OreReport report =
                com.gabrielflorianoo.peqol.item.ProspectorItem.scan(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), 1);
        helper.assertTrue(report.counts().getOrDefault(Blocks.DIAMOND_ORE, 0) == 2, "Should find 2 diamond ores, got " + report.counts());
        helper.assertTrue(report.counts().getOrDefault(Blocks.IRON_ORE, 0) == 1, "Should find 1 iron ore");
        helper.assertTrue(report.total().signum() > 0, "Ores should be worth EMC");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void handbookHasAllPages(GameTestHelper helper) {
        ItemStack book = com.gabrielflorianoo.peqol.util.Handbook.create();
        helper.assertTrue(book.is(Items.WRITTEN_BOOK), "Handbook should be a written book");
        helper.assertTrue(book.getTag() != null && book.getTag().getList("pages", 8).size() == com.gabrielflorianoo.peqol.util.Handbook.PAGES,
                "Handbook should have all pages");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void siphonJunkMode(GameTestHelper helper) {
        ItemStack charm = new ItemStack(ModItems.SIPHON_CHARM.get());
        helper.assertFalse(SiphonCharmItem.matches(charm, new ItemStack(Items.COBBLESTONE)), "Junk mode is off by default");
        charm.getOrCreateTag().putBoolean(SiphonCharmItem.TAG_JUNK, true);
        helper.assertTrue(SiphonCharmItem.matches(charm, new ItemStack(Items.COBBLESTONE)), "Junk mode should take cobblestone");
        helper.assertTrue(SiphonCharmItem.matches(charm, new ItemStack(Items.NETHERRACK)), "Junk mode should take netherrack");
        helper.assertFalse(SiphonCharmItem.matches(charm, new ItemStack(Items.DIAMOND)), "Junk mode must not take diamonds");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineUpgradesPersist(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_VACUUM.get());
        AlchemicalVacuumBlockEntity vacuum = (AlchemicalVacuumBlockEntity) helper.getBlockEntity(POS);
        int baseRadius = vacuum.getRadius();
        vacuum.setTier(2);
        helper.assertTrue(vacuum.getRadius() == baseRadius + 4, "Tier 2 should add 4 blocks of radius");
        vacuum.setTier(99);
        helper.assertTrue(vacuum.getTier() == 3, "Tier is capped at 3");
        CompoundTag saved = vacuum.saveWithoutMetadata();
        helper.assertTrue(saved.getInt("Tier") == 3, "Tier should be saved");

        helper.setBlock(new BlockPos(0, 1, 0), ModBlocks.ALCHEMICAL_DOWNLINK.get());
        AlchemicalDownlinkBlockEntity downlink = (AlchemicalDownlinkBlockEntity) helper.getBlockEntity(new BlockPos(0, 1, 0));
        long baseMax = downlink.getMaximumEmc();
        downlink.setTier(1);
        helper.assertTrue(downlink.getMaximumEmc() == baseMax * 2, "Tier 1 doubles the downlink rate");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void homesAndFavoritesRoundTrip(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        net.minecraft.core.GlobalPos pos = net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), new BlockPos(10, 64, -5));
        com.gabrielflorianoo.peqol.util.PlayerData.setHome(player, "base", pos);
        helper.assertTrue(pos.equals(com.gabrielflorianoo.peqol.util.PlayerData.homes(player).get("base")), "Home should be saved");
        helper.assertTrue(com.gabrielflorianoo.peqol.util.PlayerData.removeHome(player, "base"), "Home should be removed");
        helper.assertTrue(com.gabrielflorianoo.peqol.util.PlayerData.homes(player).isEmpty(), "No homes left");
        ResourceLocation diamond = new ResourceLocation("minecraft", "diamond");
        helper.assertTrue(com.gabrielflorianoo.peqol.util.PlayerData.setFavorite(player, diamond, true), "Favorite added");
        helper.assertFalse(com.gabrielflorianoo.peqol.util.PlayerData.setFavorite(player, diamond, true), "Favorite not added twice");
        helper.assertTrue(com.gabrielflorianoo.peqol.util.PlayerData.favorites(player).size() == 1, "One favorite");
        helper.assertTrue(com.gabrielflorianoo.peqol.util.PlayerData.setFavorite(player, diamond, false), "Favorite removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void interfaceIsInertWhileOwnerOffline(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_INTERFACE.get());
        com.gabrielflorianoo.peqol.block.entity.AlchemicalInterfaceBlockEntity be =
                (com.gabrielflorianoo.peqol.block.entity.AlchemicalInterfaceBlockEntity) helper.getBlockEntity(POS);
        be.getOwnerData().set(helper.makeMockPlayer());
        be.refresh();
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.getSlots() == 1, "Offline owner exposes no items");
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 8);
        helper.assertTrue(handler.insertItem(0, cobble, false).getCount() == 8, "Offline owner: inserts are refused");
        helper.assertTrue(handler.extractItem(0, 64, false).isEmpty(), "Offline owner: nothing to extract");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesEnabledByDefault(GameTestHelper helper) {
        helper.assertTrue(new com.gabrielflorianoo.peqol.util.RecipeEnabledCondition("alchemical_anchor").test(null), "Recipes are enabled by default");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(new ResourceLocation("peqol", "alchemical_interface")).isPresent(),
                "The Interface recipe should be loaded");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tradePostCountsStockBelow(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 0, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        BlockEntity chest = helper.getBlockEntity(chestPos);
        IItemHandler chestInv = chest.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        chestInv.insertItem(0, new ItemStack(Items.DIAMOND, 10), false);
        chestInv.insertItem(1, new ItemStack(Items.DIRT, 5), false);
        helper.setBlock(POS, ModBlocks.TRADE_POST.get());
        com.gabrielflorianoo.peqol.block.entity.TradePostBlockEntity post =
                (com.gabrielflorianoo.peqol.block.entity.TradePostBlockEntity) helper.getBlockEntity(POS);
        helper.assertTrue(post.stockCount() == 0, "No product = no stock");
        post.setProduct(moze_intel.projecte.api.ItemInfo.fromItem(Items.DIAMOND));
        helper.assertTrue(post.stockCount() == 10, "Should count the 10 diamonds in the chest below, got " + post.stockCount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void springPricesFluids(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_SPRING.get());
        com.gabrielflorianoo.peqol.block.entity.AlchemicalSpringBlockEntity spring =
                (com.gabrielflorianoo.peqol.block.entity.AlchemicalSpringBlockEntity) helper.getBlockEntity(POS);
        helper.assertTrue(spring.fluid() == net.minecraft.world.level.material.Fluids.WATER, "Starts with water");
        helper.assertTrue(spring.costPerBucket() >= 0, "Water has a non negative cost");
        net.minecraftforge.fluids.capability.IFluidHandler tank = spring.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
        helper.assertTrue(tank.drain(1000, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Ownerless spring gives nothing");
        helper.assertTrue(tank.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0, "Spring never accepts fluids");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void excavatorWaitsForOwner(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_EXCAVATOR.get());
        com.gabrielflorianoo.peqol.block.entity.AlchemicalExcavatorBlockEntity excavator =
                (com.gabrielflorianoo.peqol.block.entity.AlchemicalExcavatorBlockEntity) helper.getBlockEntity(POS);
        excavator.getOwnerData().set(helper.makeMockPlayer());
        helper.runAfterDelay(40, () -> {
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(1, 0, 1));
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void displaySyncsData(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_DISPLAY.get());
        com.gabrielflorianoo.peqol.block.entity.AlchemicalDisplayBlockEntity display =
                (com.gabrielflorianoo.peqol.block.entity.AlchemicalDisplayBlockEntity) helper.getBlockEntity(POS);
        display.getOwnerData().set(helper.makeMockPlayer());
        CompoundTag update = display.getUpdateTag();
        helper.assertTrue(update.contains("ShownEmc") && update.hasUUID("Owner"), "Update tag should carry the owner and EMC");
        helper.assertTrue(display.getUpdatePacket() != null, "Display should send block entity update packets");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void harvesterClassifiesPlants(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(1, 1, 1), ((net.minecraft.world.level.block.CropBlock) Blocks.WHEAT).getStateForAge(7));
        helper.setBlock(new BlockPos(2, 0, 1), Blocks.FARMLAND);
        helper.setBlock(new BlockPos(2, 1, 1), ((net.minecraft.world.level.block.CropBlock) Blocks.WHEAT).getStateForAge(2));
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.SAND);
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.SUGAR_CANE);
        helper.setBlock(new BlockPos(0, 2, 0), Blocks.SUGAR_CANE);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        java.util.function.Function<BlockPos, com.gabrielflorianoo.peqol.block.entity.AlchemicalHarvesterBlockEntity.HarvestType> classify = rel -> {
            BlockPos abs = helper.absolutePos(rel);
            return com.gabrielflorianoo.peqol.block.entity.AlchemicalHarvesterBlockEntity.classify(level, abs, level.getBlockState(abs));
        };
        helper.assertTrue(classify.apply(new BlockPos(1, 1, 1)) == com.gabrielflorianoo.peqol.block.entity.AlchemicalHarvesterBlockEntity.HarvestType.REPLANT, "Mature wheat is replanted");
        helper.assertTrue(classify.apply(new BlockPos(2, 1, 1)) == com.gabrielflorianoo.peqol.block.entity.AlchemicalHarvesterBlockEntity.HarvestType.NONE, "Young wheat is left alone");
        helper.assertTrue(classify.apply(new BlockPos(0, 2, 0)) == com.gabrielflorianoo.peqol.block.entity.AlchemicalHarvesterBlockEntity.HarvestType.CUT_ABOVE, "Upper sugar cane is cut");
        helper.assertTrue(classify.apply(new BlockPos(0, 1, 0)) == com.gabrielflorianoo.peqol.block.entity.AlchemicalHarvesterBlockEntity.HarvestType.NONE, "Sugar cane base is kept");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pastureKnowsAnimalFood(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 1, 1));
        helper.assertTrue(com.gabrielflorianoo.peqol.block.entity.AlchemicalPastureBlockEntity.foodFor(cow).is(Items.WHEAT), "Cows eat wheat");
        net.minecraft.world.entity.animal.Pig pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 1, 1));
        helper.assertTrue(!com.gabrielflorianoo.peqol.block.entity.AlchemicalPastureBlockEntity.foodFor(pig).isEmpty(), "Pigs have a food");
        cow.discard();
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void filterMenuGhostSlots(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        java.util.List<ItemStack> saved = new java.util.ArrayList<>();
        com.gabrielflorianoo.peqol.menu.FilterTarget target = new com.gabrielflorianoo.peqol.menu.FilterTarget() {
            @Override
            public java.util.List<ItemStack> load() {
                return java.util.List.of(new ItemStack(Items.COBBLESTONE));
            }

            @Override
            public void save(java.util.List<ItemStack> filter) {
                saved.addAll(filter);
            }

            @Override
            public boolean stillValid(Player viewer) {
                return true;
            }
        };
        com.gabrielflorianoo.peqol.menu.FilterMenu menu = new com.gabrielflorianoo.peqol.menu.FilterMenu(1, player.getInventory(), target, false, -1);
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.COBBLESTONE), "Stored filter is loaded");
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 5);
        menu.setCarried(diamonds);
        menu.clicked(3, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        helper.assertTrue(diamonds.getCount() == 5, "Ghost slots never consume items");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        menu.removed(player);
        helper.assertTrue(saved.size() == 1 && saved.get(0).is(Items.DIAMOND), "Only the diamond should remain, got " + saved);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shopRegistryTracksPosts(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.TRADE_POST.get());
        com.gabrielflorianoo.peqol.block.entity.TradePostBlockEntity post =
                (com.gabrielflorianoo.peqol.block.entity.TradePostBlockEntity) helper.getBlockEntity(POS);
        post.getOwnerData().set(helper.makeMockPlayer());
        post.setProduct(moze_intel.projecte.api.ItemInfo.fromItem(Items.EMERALD));
        BlockPos abs = helper.absolutePos(POS);
        com.gabrielflorianoo.peqol.util.ShopRegistry registry = com.gabrielflorianoo.peqol.util.ShopRegistry.get(helper.getLevel().getServer());
        helper.assertTrue(registry != null && registry.entries().stream().anyMatch(e -> e.pos().pos().equals(abs) && e.product().equals("minecraft:emerald")),
                "The post should be listed");
        helper.setBlock(POS, Blocks.AIR);
        helper.assertFalse(registry.entries().stream().anyMatch(e -> e.pos().pos().equals(abs)), "Removed posts are unlisted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exporterFilterStacksRoundTrip(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ALCHEMICAL_EXPORTER.get());
        com.gabrielflorianoo.peqol.block.entity.AlchemicalExporterBlockEntity exporter =
                (com.gabrielflorianoo.peqol.block.entity.AlchemicalExporterBlockEntity) helper.getBlockEntity(POS);
        exporter.setFilterStacks(java.util.List.of(new ItemStack(Items.COBBLESTONE, 32), new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.BEDROCK, 5)));
        java.util.List<ItemStack> stacks = exporter.filterStacks();
        helper.assertTrue(stacks.size() == 2, "Bedrock has no EMC and is dropped, got " + stacks);
        helper.assertTrue(stacks.get(0).is(Items.COBBLESTONE) && stacks.get(0).getCount() == 32, "Keep 32 cobblestone");
        helper.assertTrue(stacks.get(1).is(Items.DIAMOND) && stacks.get(1).getCount() == 1, "Diamonds unlimited");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void effectAndFeatherCharmCosts(GameTestHelper helper) {
        com.gabrielflorianoo.peqol.item.EffectCharmItem vision = ModItems.VISION_CHARM.get();
        helper.assertTrue(vision.getEffect() == net.minecraft.world.effect.MobEffects.NIGHT_VISION, "Vision charm gives night vision");
        helper.assertTrue(vision.costPerInterval() == 6, "64 EMC/min over 5 s should cost 6, got " + vision.costPerInterval());
        helper.assertTrue(ModItems.HASTE_CHARM.get().getEffect() == net.minecraft.world.effect.MobEffects.DIG_SPEED, "Haste charm gives haste");
        helper.assertTrue(com.gabrielflorianoo.peqol.item.FeatherCharmItem.costFor(2F) == 0, "Short falls are free");
        helper.assertTrue(com.gabrielflorianoo.peqol.item.FeatherCharmItem.costFor(10F) == 7 * 32, "10 block fall costs 7 blocks worth");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pocketSmelterAndToolkit(GameTestHelper helper) {
        ItemStack stone = com.gabrielflorianoo.peqol.item.PocketSmelterItem.result(helper.getLevel(), new ItemStack(Items.COBBLESTONE, 3));
        helper.assertTrue(stone.is(Items.STONE) && stone.getCount() == 3, "3 cobblestone smelt into 3 stone, got " + stone);
        helper.assertTrue(com.gabrielflorianoo.peqol.item.PocketSmelterItem.result(helper.getLevel(), new ItemStack(Items.DIAMOND)).isEmpty(), "Diamonds can't be smelted");
        ItemStack toolkit = new ItemStack(ModItems.ALCHEMIST_TOOLKIT.get());
        helper.assertTrue(com.gabrielflorianoo.peqol.item.AlchemistToolkitItem.getMode(toolkit) == com.gabrielflorianoo.peqol.item.AlchemistToolkitItem.Mode.CRAFTING, "Default mode is crafting");
        toolkit.getOrCreateTag().putInt(com.gabrielflorianoo.peqol.item.AlchemistToolkitItem.TAG_MODE, 1);
        helper.assertTrue(com.gabrielflorianoo.peqol.item.AlchemistToolkitItem.getMode(toolkit) == com.gabrielflorianoo.peqol.item.AlchemistToolkitItem.Mode.ANVIL, "Mode 1 is the anvil");
        helper.succeed();
    }
}
