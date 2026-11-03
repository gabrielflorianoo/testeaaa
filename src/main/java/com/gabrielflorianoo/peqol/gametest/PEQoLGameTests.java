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
}
