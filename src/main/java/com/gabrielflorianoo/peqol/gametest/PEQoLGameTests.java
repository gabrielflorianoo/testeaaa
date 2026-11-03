package com.gabrielflorianoo.peqol.gametest;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalImporterBlockEntity;
import com.gabrielflorianoo.peqol.block.entity.AlchemicalUplinkBlockEntity;
import com.gabrielflorianoo.peqol.item.EmcChequeItem;
import com.gabrielflorianoo.peqol.item.MendingCharmItem;
import com.gabrielflorianoo.peqol.item.SatiationCharmItem;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
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
}
