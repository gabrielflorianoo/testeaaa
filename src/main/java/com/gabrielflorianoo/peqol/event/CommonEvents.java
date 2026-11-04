package com.gabrielflorianoo.peqol.event;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.RestockerTalismanItem;
import com.gabrielflorianoo.peqol.item.IlluminationCharmItem;
import com.gabrielflorianoo.peqol.item.MendingCharmItem;
import com.gabrielflorianoo.peqol.item.SatiationCharmItem;
import com.gabrielflorianoo.peqol.item.SiphonCharmItem;
import com.gabrielflorianoo.peqol.item.SmeltingCharmItem;
import com.gabrielflorianoo.peqol.registry.ModStats;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.gabrielflorianoo.peqol.item.StellarLinkItem;
import com.gabrielflorianoo.peqol.item.ToggleableItem;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.gabrielflorianoo.peqol.util.AutoStock;
import com.gabrielflorianoo.peqol.util.InventoryUtil;
import java.math.BigInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import com.gabrielflorianoo.peqol.block.AlchemicalExporterBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CommonEvents {

    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        int tick = player.tickCount;
        if (tick % PEQoLConfig.COMMON.restockerInterval.get() == 0 && hasActive(player, RestockerTalismanItem.class)) {
            RestockerTalismanItem.restock(player);
            AutoStock.tick(player);
        }
        if (tick % PEQoLConfig.COMMON.mendingInterval.get() == 0 && hasActive(player, MendingCharmItem.class)) {
            MendingCharmItem.mend(player);
        }
        if (tick % PEQoLConfig.COMMON.satiationInterval.get() == 0) {
            ItemStack charm = InventoryUtil.findFirstStack(player, s -> s.getItem() instanceof SatiationCharmItem && ToggleableItem.isActive(s));
            if (!charm.isEmpty()) {
                SatiationCharmItem.feed(player, charm);
            }
        }
        if (tick % 20 == 0 && hasActive(player, StellarLinkItem.class)) {
            StellarLinkItem.charge(player);
        }
        if (tick % PEQoLConfig.COMMON.illuminationInterval.get() == 0 && hasActive(player, IlluminationCharmItem.class)) {
            IlluminationCharmItem.illuminate(player);
        }
        if (tick % 20 == 0 && InventoryUtil.contains(player, s -> s.getItem() instanceof com.gabrielflorianoo.peqol.item.magic.ChronalEchoItem)) {
            com.gabrielflorianoo.peqol.item.magic.ChronalEchoItem.record(player);
        }
        if (tick % com.gabrielflorianoo.peqol.item.EffectCharmItem.INTERVAL == 0) {
            //One payment per kind of effect charm, even if several copies are carried
            java.util.Set<net.minecraft.world.item.Item> applied = new java.util.HashSet<>();
            InventoryUtil.forEachStack(player, stack -> {
                if (stack.getItem() instanceof com.gabrielflorianoo.peqol.item.EffectCharmItem charm && ToggleableItem.isActive(stack) && applied.add(charm)) {
                    charm.apply(player);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        com.gabrielflorianoo.peqol.item.magic.ChronalEchoItem.forget(event.getEntity().getUUID());
    }

    public static boolean hasActive(net.minecraft.world.entity.player.Player player, Class<? extends ToggleableItem> type) {
        return InventoryUtil.contains(player, s -> type.isInstance(s.getItem()) && ToggleableItem.isActive(s));
    }

    public static final String OBJECTIVE = "peqol_emc";

    /**
     * Keeps the peqol_emc scoreboard objective in sync with personal EMC.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !PEQoLConfig.COMMON.scoreboard.get()) {
            return;
        }
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        if (server.getTickCount() % 100 != 0) {
            return;
        }
        net.minecraft.server.ServerScoreboard scoreboard = server.getScoreboard();
        net.minecraft.world.scores.Objective objective = scoreboard.getObjective(OBJECTIVE);
        if (objective == null) {
            objective = scoreboard.addObjective(OBJECTIVE, net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                    Component.literal("EMC").withStyle(ChatFormatting.LIGHT_PURPLE), net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int value = EmcUtil.getEmc(player).min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
            net.minecraft.world.scores.Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
            if (score.getScore() != value) {
                score.setScore(value);
            }
        }
    }

    /**
     * Starter knowledge: learned once, the first time a player joins.
     */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        net.minecraft.nbt.CompoundTag persisted = player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean("peqol_handbook_given") && PEQoLConfig.COMMON.giveHandbook.get()) {
            persisted.putBoolean("peqol_handbook_given", true);
            player.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
            InventoryUtil.giveOrDrop(player, com.gabrielflorianoo.peqol.util.Handbook.create());
        }
        if (persisted.getBoolean("peqol_starter_given")) {
            return;
        }
        persisted.putBoolean("peqol_starter_given", true);
        player.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
        int learned = 0;
        for (String entry : PEQoLConfig.COMMON.starterKnowledge.get()) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(entry);
            net.minecraft.world.item.Item item = id == null ? null : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != net.minecraft.world.item.Items.AIR && EmcUtil.tryLearn(player, new ItemStack(item))) {
                learned++;
            }
        }
        if (learned > 0) {
            player.sendSystemMessage(Component.translatable("message.peqol.starter", learned).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * Optional: crafting an item learns it.
     */
    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (PEQoLConfig.COMMON.learnOnCraft.get() && event.getEntity() instanceof ServerPlayer player && EmcUtil.hasValue(event.getCrafting())) {
            if (EmcUtil.tryLearn(player, event.getCrafting())) {
                player.displayClientMessage(Component.translatable("message.peqol.learned", event.getCrafting().getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
    }

    /**
     * Charm of Undying: cheat death for EMC.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        ItemStack charm = InventoryUtil.findFirstStack(player, s -> s.getItem() instanceof com.gabrielflorianoo.peqol.item.UndyingCharmItem && ToggleableItem.isActive(s));
        if (!charm.isEmpty() && ((com.gabrielflorianoo.peqol.item.UndyingCharmItem) charm.getItem()).trySave(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Feather Charm: pay EMC instead of taking fall damage.
     */
    @SubscribeEvent
    public static void onFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDistance() <= 3 || !hasActive(player, com.gabrielflorianoo.peqol.item.FeatherCharmItem.class)) {
            return;
        }
        if (com.gabrielflorianoo.peqol.item.FeatherCharmItem.absorb(player, event.getDistance())) {
            event.setDamageMultiplier(0);
        }
    }

    /**
     * Charm of Wisdom: experience orbs become EMC.
     */
    @SubscribeEvent
    public static void onXpPickup(net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !hasActive(player, com.gabrielflorianoo.peqol.item.WisdomCharmItem.class)) {
            return;
        }
        net.minecraft.world.entity.ExperienceOrb orb = event.getOrb();
        int value = orb.getValue();
        if (value <= 0) {
            return;
        }
        event.setCanceled(true);
        orb.discard();
        player.take(orb, 1);
        EmcUtil.addEmc(player, BigInteger.valueOf(PEQoLConfig.COMMON.xpEmcPerPoint.get()).multiply(BigInteger.valueOf(value)));
    }

    /**
     * Siphon Charm also catches filtered drops of mobs you kill.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.isCanceled() || !PEQoLConfig.COMMON.siphonCatchesMobDrops.get() || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        java.util.Iterator<ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemEntity drop = iterator.next();
            ItemStack stack = drop.getItem();
            if (stack.isEmpty() || EmcUtil.isEmcHolder(stack)) {
                continue;
            }
            ItemStack charm = InventoryUtil.findFirstStack(player, s -> s.getItem() instanceof SiphonCharmItem && ToggleableItem.isActive(s) && SiphonCharmItem.matches(s, stack));
            if (charm.isEmpty()) {
                continue;
            }
            if (EmcUtil.burn(player, stack, PEQoLConfig.COMMON.siphonLearnsItems.get()).signum() > 0) {
                iterator.remove();
            }
        }
    }

    /**
     * Restocker: a learned tool that breaks in your hand is recreated from personal EMC.
     */
    @SubscribeEvent
    public static void onToolBroken(PlayerDestroyItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() == null || !PEQoLConfig.COMMON.restockerReplacesTools.get()
                || !hasActive(player, RestockerTalismanItem.class)) {
            return;
        }
        ItemStack original = event.getOriginal();
        if (original.isEmpty() || !original.isDamageableItem()) {
            return;
        }
        ItemStack fresh = new ItemStack(original.getItem());
        net.minecraft.world.InteractionHand hand = event.getHand();
        //The broken stack is removed after this event, so swap in the new tool on the next server task
        player.server.execute(() -> {
            if (!player.isAlive() || !player.getItemInHand(hand).isEmpty() || EmcUtil.isBlacklisted(fresh) || !EmcUtil.isKnown(player, fresh)) {
                return;
            }
            long value = EmcUtil.value(fresh);
            if (value <= 0 || !EmcUtil.tryRemoveEmc(player, BigInteger.valueOf(value))) {
                return;
            }
            player.setItemInHand(hand, fresh);
            player.awardStat(ModStats.ITEMS_CREATED);
            player.displayClientMessage(Component.translatable("message.peqol.tool_replaced", fresh.getHoverName(), EmcUtil.format(value))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.4F);
        });
    }

    /**
     * Sneak + right click with an item normally skips the block; the Exporter uses it to set stock targets.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown() && !event.getItemStack().isEmpty() && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof AlchemicalExporterBlock) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) {
            return;
        }
        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) {
            return;
        }
        ItemStack charm = InventoryUtil.findFirstStack(player, s -> s.getItem() instanceof SiphonCharmItem && ToggleableItem.isActive(s) && SiphonCharmItem.matches(s, stack));
        if (!charm.isEmpty() && !stack.getCapability(moze_intel.projecte.api.capabilities.PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).isPresent()) {
            BigInteger gained = EmcUtil.burn(player, stack, PEQoLConfig.COMMON.siphonLearnsItems.get());
            if (gained.signum() > 0) {
                player.take(itemEntity, stack.getCount());
                itemEntity.discard();
                event.setCanceled(true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.25F, 1.6F);
                return;
            }
        }
        if (hasActive(player, SmeltingCharmItem.class)) {
            ItemStack smelted = SmeltingCharmItem.trySmelt(player, stack);
            if (!smelted.isEmpty()) {
                //Let the normal pickup continue with the smelted stack
                itemEntity.setItem(smelted);
                return;
            }
        }
        if (PEQoLConfig.COMMON.autoLearnOnPickup.get() && EmcUtil.hasValue(stack) && !EmcUtil.isKnown(player, stack) && EmcUtil.hasTablet(player)) {
            if (EmcUtil.tryLearn(player, stack)) {
                player.displayClientMessage(Component.translatable("message.peqol.learned", stack.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
    }
}
