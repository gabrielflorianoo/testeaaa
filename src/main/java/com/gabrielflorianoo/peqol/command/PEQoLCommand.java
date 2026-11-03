package com.gabrielflorianoo.peqol.command;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.EmcChequeItem;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.gabrielflorianoo.peqol.util.InventoryUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.gabrielflorianoo.peqol.util.AutoStock;
import com.gabrielflorianoo.peqol.util.KnowledgeStats;
import com.gabrielflorianoo.peqol.registry.ModStats;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.registries.ForgeRegistries;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * /peqol help|balance|pay|withdraw|buy|value|worth|burn|missing|unlearned|stars|top|knowledge
 */
public final class PEQoLCommand {

    private static final SimpleCommandExceptionType INVALID_AMOUNT = new SimpleCommandExceptionType(Component.translatable("command.peqol.invalid_amount"));
    private static final SimpleCommandExceptionType DISABLED = new SimpleCommandExceptionType(Component.translatable("message.peqol.disabled"));
    private static final SimpleCommandExceptionType NOT_ENOUGH = new SimpleCommandExceptionType(Component.translatable("command.peqol.not_enough_emc"));
    private static final SimpleCommandExceptionType NEED_TABLET = new SimpleCommandExceptionType(Component.translatable("message.peqol.need_tablet"));
    private static final DynamicCommandExceptionType NOT_LEARNED = new DynamicCommandExceptionType(
            name -> Component.translatable("command.peqol.buy.not_learned", name));
    private static final DynamicCommandExceptionType TOO_MANY = new DynamicCommandExceptionType(
            max -> Component.translatable("command.peqol.buy.too_many", max));
    private static final DynamicCommandExceptionType BLACKLISTED = new DynamicCommandExceptionType(
            name -> Component.translatable("message.peqol.blacklisted", name));
    private static final SimpleCommandExceptionType SELF = new SimpleCommandExceptionType(Component.translatable("command.peqol.pay.self"));

    private PEQoLCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("peqol")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
                .then(Commands.literal("buy")
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> buy(ctx, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> buy(ctx, IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("value")
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(PEQoLCommand::value)))
                .then(Commands.literal("unlearned").executes(ctx -> unlearned(ctx.getSource())))
                .then(Commands.literal("sell")
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> sell(ctx, Integer.MAX_VALUE))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(ctx -> sell(ctx, IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("xp")
                        .then(Commands.literal("toemc")
                                .executes(ctx -> xpToEmc(ctx.getSource(), -1))
                                .then(Commands.argument("points", IntegerArgumentType.integer(1))
                                        .executes(ctx -> xpToEmc(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "points")))))
                        .then(Commands.literal("fromemc")
                                .then(Commands.argument("points", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(ctx -> xpFromEmc(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "points"))))))
                .then(Commands.literal("stats").executes(ctx -> stats(ctx.getSource())))
                .then(Commands.literal("back").executes(ctx -> back(ctx.getSource())))
                .then(Commands.literal("gift")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(ctx -> gift(ctx, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> gift(ctx, IntegerArgumentType.getInteger(ctx, "count")))))))
                .then(Commands.literal("learnall").executes(ctx -> {
                    int learned = com.gabrielflorianoo.peqol.network.ServerActions.learnInventory(ctx.getSource().getPlayerOrException(), true);
                    return Math.max(learned, 0);
                }))
                .then(Commands.literal("autostock")
                        .executes(ctx -> autostockList(ctx.getSource()))
                        .then(Commands.literal("list").executes(ctx -> autostockList(ctx.getSource())))
                        .then(Commands.literal("clear").executes(ctx -> autostockClear(ctx.getSource())))
                        .then(Commands.literal("add")
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 2304))
                                                .executes(ctx -> autostockSet(ctx, IntegerArgumentType.getInteger(ctx, "count"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(ctx -> autostockSet(ctx, 0)))))
                .then(Commands.literal("known")
                        .executes(ctx -> known(ctx.getSource(), "", 1))
                        .then(Commands.literal("page")
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> known(ctx.getSource(), "", IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("search")
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> known(ctx.getSource(), StringArgumentType.getString(ctx, "text"), 1)))))
                .then(Commands.literal("stars")
                        .then(Commands.literal("fill").executes(ctx -> starsFill(ctx.getSource())))
                        .then(Commands.literal("drain").executes(ctx -> starsDrain(ctx.getSource()))))
                .then(Commands.literal("balance")
                        .executes(ctx -> balance(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> balance(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("knowledge")
                        .executes(ctx -> knowledge(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> knowledge(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("pay")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", StringArgumentType.word())
                                        .executes(PEQoLCommand::pay))))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(ctx -> withdraw(ctx, 1))
                                .then(Commands.argument("cheques", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> withdraw(ctx, IntegerArgumentType.getInteger(ctx, "cheques"))))))
                .then(Commands.literal("worth")
                        .executes(ctx -> worthHand(ctx.getSource()))
                        .then(Commands.literal("hand").executes(ctx -> worthHand(ctx.getSource())))
                        .then(Commands.literal("inventory").executes(ctx -> worthInventory(ctx.getSource()))))
                .then(Commands.literal("burn")
                        .then(Commands.literal("hand").executes(ctx -> burnHand(ctx.getSource())))
                        .then(Commands.literal("inventory").executes(ctx -> burnInventory(ctx.getSource()))))
                .then(Commands.literal("missing").executes(ctx -> missing(ctx.getSource())))
                .then(Commands.literal("top").executes(ctx -> top(ctx.getSource())))
        );
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.peqol.help.header").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        String[] entries = {"balance", "pay", "withdraw", "buy", "sell", "value", "known", "autostock", "learnall", "gift", "back", "xp", "stats", "worth", "burn", "missing", "unlearned", "stars", "knowledge", "top"};
        for (String entry : entries) {
            source.sendSuccess(() -> Component.literal("/peqol " + entry).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable("command.peqol.help." + entry).withStyle(ChatFormatting.GRAY)), false);
        }
        return entries.length;
    }

    private static int buy(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        if (!PEQoLConfig.COMMON.buyEnabled.get()) {
            throw DISABLED.create();
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (PEQoLConfig.COMMON.buyRequiresTablet.get() && !EmcUtil.hasTablet(player)) {
            throw NEED_TABLET.create();
        }
        int max = PEQoLConfig.COMMON.buyMaxCount.get();
        if (count > max) {
            throw TOO_MANY.create(max);
        }
        ItemInput input = ItemArgument.getItem(ctx, "item");
        ItemStack template = input.createItemStack(1, false);
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        long unit = EmcUtil.value(template);
        if (provider == null || unit <= 0 || !provider.hasKnowledge(template)) {
            throw NOT_LEARNED.create(template.getHoverName());
        }
        if (EmcUtil.isBlacklisted(template)) {
            throw BLACKLISTED.create(template.getHoverName());
        }
        BigInteger cost = BigInteger.valueOf(unit).multiply(BigInteger.valueOf(count));
        if (!EmcUtil.tryRemoveEmc(player, cost)) {
            throw NOT_ENOUGH.create();
        }
        int remaining = count;
        int maxStack = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(remaining, maxStack);
            InventoryUtil.giveOrDrop(player, template.copyWithCount(amount));
            remaining -= amount;
        }
        player.awardStat(ModStats.ITEMS_CREATED, count);
        Component name = template.getHoverName();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.peqol.buy", count, name, EmcUtil.format(cost)).withStyle(ChatFormatting.GREEN), false);
        return count;
    }

    private static int value(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
        long unit = EmcUtil.value(stack);
        if (unit <= 0) {
            ctx.getSource().sendFailure(Component.translatable("message.peqol.no_emc", stack.getHoverName()));
            return 0;
        }
        long sell = EmcUtil.sellValue(stack);
        ServerPlayer player = ctx.getSource().getPlayer();
        Component known = player == null ? Component.empty() : EmcUtil.isKnown(player, stack)
                ? Component.translatable("command.peqol.value.known").withStyle(ChatFormatting.GREEN)
                : Component.translatable("command.peqol.value.unknown").withStyle(ChatFormatting.RED);
        long affordable = player == null ? 0 : EmcUtil.affordable(EmcUtil.getEmc(player), unit);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.peqol.value", stack.getHoverName(),
                Component.literal(EmcUtil.format(unit)).withStyle(ChatFormatting.YELLOW), EmcUtil.format(sell), known, EmcUtil.format(affordable)), false);
        if (player != null && EmcUtil.isKnown(player, stack) && !stack.hasTag()) {
            BigInteger emc = EmcUtil.getEmc(player);
            int maxStack = Math.max(1, stack.getMaxStackSize());
            ctx.getSource().sendSuccess(() -> Component.literal(" ").append(buyButton(stack, 1, unit, emc)).append(" ")
                    .append(buyButton(stack, maxStack, unit, emc)), false);
        }
        return (int) Math.min(Integer.MAX_VALUE, unit);
    }

    private static int unlearned(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return 0;
        }
        Map<Item, ItemStack> unknown = new LinkedHashMap<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && EmcUtil.hasValue(stack) && !provider.hasKnowledge(stack)) {
                unknown.putIfAbsent(stack.getItem(), stack);
            }
        }
        if (unknown.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.peqol.unlearned.none").withStyle(ChatFormatting.GREEN), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.peqol.unlearned.header", unknown.size()).withStyle(ChatFormatting.GOLD), false);
        for (ItemStack stack : unknown.values()) {
            source.sendSuccess(() -> Component.literal(" - ").append(stack.getHoverName())
                    .append(Component.literal(" (" + EmcUtil.format(EmcUtil.value(stack)) + " EMC)").withStyle(ChatFormatting.DARK_GRAY))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return unknown.size();
    }

    private static int sell(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        checkBurn(player);
        ItemStack template = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
        if (!burnable(template)) {
            throw NOT_LEARNED.create(template.getHoverName());
        }
        Inventory inventory = player.getInventory();
        int remaining = count;
        int sold = 0;
        BigInteger total = BigInteger.ZERO;
        //Main inventory first (not the hotbar), then the hotbar
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            int from = pass == 0 ? Inventory.getSelectionSize() : 0;
            int to = pass == 0 ? inventory.items.size() : Inventory.getSelectionSize();
            for (int i = from; i < to && remaining > 0; i++) {
                ItemStack stack = inventory.items.get(i);
                if (!ItemStack.isSameItemSameTags(stack, template)) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                ItemStack portion = stack.split(take);
                total = total.add(EmcUtil.burn(player, portion, true));
                sold += take;
                remaining -= take;
            }
        }
        if (sold == 0) {
            ctx.getSource().sendFailure(Component.translatable("command.peqol.sell.none", template.getHoverName()));
            return 0;
        }
        int soldCount = sold;
        BigInteger gained = total;
        ctx.getSource().sendSuccess(() -> Component.translatable("message.peqol.burned", soldCount, template.getHoverName(), EmcUtil.format(gained))
                .withStyle(ChatFormatting.GOLD), false);
        return sold;
    }

    /**
     * Total experience points the player currently has.
     */
    private static int totalXp(ServerPlayer player) {
        int level = player.experienceLevel;
        int points;
        if (level <= 16) {
            points = level * level + 6 * level;
        } else if (level <= 31) {
            points = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            points = (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        return points + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    private static int xpToEmc(CommandSourceStack source, int requested) throws CommandSyntaxException {
        if (!PEQoLConfig.COMMON.xpEnabled.get()) {
            throw DISABLED.create();
        }
        ServerPlayer player = source.getPlayerOrException();
        int available = totalXp(player);
        int points = requested < 0 ? available : Math.min(requested, available);
        if (points <= 0) {
            source.sendFailure(Component.translatable("command.peqol.xp.none"));
            return 0;
        }
        player.giveExperiencePoints(-points);
        BigInteger emc = BigInteger.valueOf(PEQoLConfig.COMMON.xpEmcPerPoint.get()).multiply(BigInteger.valueOf(points));
        EmcUtil.addEmc(player, emc);
        source.sendSuccess(() -> Component.translatable("command.peqol.xp.toemc", points, EmcUtil.format(emc)).withStyle(ChatFormatting.GREEN), false);
        return points;
    }

    private static int xpFromEmc(CommandSourceStack source, int points) throws CommandSyntaxException {
        if (!PEQoLConfig.COMMON.xpEnabled.get()) {
            throw DISABLED.create();
        }
        ServerPlayer player = source.getPlayerOrException();
        BigInteger cost = BigInteger.valueOf(PEQoLConfig.COMMON.xpEmcPerPoint.get()).multiply(BigInteger.valueOf(points));
        if (!EmcUtil.tryRemoveEmc(player, cost)) {
            throw NOT_ENOUGH.create();
        }
        player.giveExperiencePoints(points);
        source.sendSuccess(() -> Component.translatable("command.peqol.xp.fromemc", points, EmcUtil.format(cost)).withStyle(ChatFormatting.GREEN), false);
        return points;
    }

    private static int gift(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        if (!PEQoLConfig.COMMON.giftEnabled.get() || !PEQoLConfig.COMMON.buyEnabled.get()) {
            throw DISABLED.create();
        }
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int max = PEQoLConfig.COMMON.buyMaxCount.get();
        if (count > max) {
            throw TOO_MANY.create(max);
        }
        ItemStack template = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
        IKnowledgeProvider provider = EmcUtil.knowledge(sender).orElse(null);
        long unit = EmcUtil.value(template);
        if (provider == null || unit <= 0 || !provider.hasKnowledge(template)) {
            throw NOT_LEARNED.create(template.getHoverName());
        }
        if (EmcUtil.isBlacklisted(template)) {
            throw BLACKLISTED.create(template.getHoverName());
        }
        BigInteger cost = BigInteger.valueOf(unit).multiply(BigInteger.valueOf(count));
        if (!EmcUtil.tryRemoveEmc(sender, cost)) {
            throw NOT_ENOUGH.create();
        }
        int remaining = count;
        int maxStack = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(remaining, maxStack);
            InventoryUtil.giveOrDrop(target, template.copyWithCount(amount));
            remaining -= amount;
        }
        sender.awardStat(ModStats.ITEMS_CREATED, count);
        Component name = template.getHoverName();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.peqol.gift.sent", count, name, target.getDisplayName(), EmcUtil.format(cost))
                .withStyle(ChatFormatting.GREEN), false);
        target.sendSystemMessage(Component.translatable("command.peqol.gift.received", count, name, sender.getDisplayName()).withStyle(ChatFormatting.GREEN));
        return count;
    }

    private static int back(CommandSourceStack source) throws CommandSyntaxException {
        if (!PEQoLConfig.COMMON.backEnabled.get()) {
            throw DISABLED.create();
        }
        ServerPlayer player = source.getPlayerOrException();
        java.util.Optional<net.minecraft.core.GlobalPos> death = player.getLastDeathLocation();
        if (death.isEmpty()) {
            source.sendFailure(Component.translatable("command.peqol.back.none"));
            return 0;
        }
        net.minecraft.server.level.ServerLevel level = player.server.getLevel(death.get().dimension());
        if (level == null) {
            source.sendFailure(Component.translatable("message.peqol.anchor.no_dimension"));
            return 0;
        }
        net.minecraft.core.BlockPos safe = findSafeSpot(level, death.get().pos());
        if (safe == null) {
            source.sendFailure(Component.translatable("message.peqol.anchor.obstructed"));
            return 0;
        }
        BigInteger cost = player.getAbilities().instabuild ? BigInteger.ZERO
                : com.gabrielflorianoo.peqol.item.AlchemicalAnchorItem.cost(player, safe, death.get().dimension().location());
        if (cost.signum() > 0 && !EmcUtil.tryRemoveEmc(player, cost)) {
            source.sendFailure(Component.translatable("message.peqol.anchor.too_expensive", EmcUtil.format(cost)));
            return 0;
        }
        player.teleportTo(level, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        level.playSound(null, safe, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1F);
        player.awardStat(ModStats.ANCHOR_TELEPORTS);
        source.sendSuccess(() -> Component.translatable("command.peqol.back.done", EmcUtil.format(cost)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    /**
     * First spot at or above {@code start} (up to 64 blocks) where a player fits and isn't standing in lava.
     */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.core.BlockPos findSafeSpot(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos start) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(start.getX(), Math.max(start.getY(), level.getMinBuildHeight() + 1), start.getZ());
        for (int i = 0; i < 64 && pos.getY() < level.getMaxBuildHeight() - 1; i++, pos = pos.above()) {
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                    && !level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA)
                    && !level.getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.LAVA)) {
                return pos;
            }
        }
        return null;
    }

    private static int stats(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.translatable("command.peqol.stats.header").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        for (net.minecraft.resources.ResourceLocation id : ModStats.ALL) {
            int value = player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(id));
            source.sendSuccess(() -> Component.literal(" ").append(Component.translatable("stat." + id.getNamespace() + "." + id.getPath()))
                    .append(Component.literal(": " + EmcUtil.format(value)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int autostockList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Map<net.minecraft.resources.ResourceLocation, Integer> entries = AutoStock.get(player);
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.peqol.autostock.empty").withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.peqol.autostock.header", entries.size()).withStyle(ChatFormatting.GOLD), false);
        for (Map.Entry<net.minecraft.resources.ResourceLocation, Integer> entry : entries.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getKey());
            Component name = item == null ? Component.literal(entry.getKey().toString()) : item.getDescription();
            int have = item == null ? 0 : AutoStock.count(player.getInventory(), item);
            source.sendSuccess(() -> Component.literal(" - ").append(name).append(Component.literal(" " + have + "/" + entry.getValue())
                    .withStyle(have >= entry.getValue() ? ChatFormatting.GREEN : ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY), false);
        }
        return entries.size();
    }

    private static int autostockClear(CommandSourceStack source) throws CommandSyntaxException {
        AutoStock.clear(source.getPlayerOrException());
        source.sendSuccess(() -> Component.translatable("command.peqol.autostock.cleared").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int autostockSet(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack template = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
        net.minecraft.resources.ResourceLocation id = ForgeRegistries.ITEMS.getKey(template.getItem());
        if (id == null) {
            return 0;
        }
        AutoStock.set(player, id, count);
        Component name = template.getHoverName();
        if (count > 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.peqol.autostock.added", name, count).withStyle(ChatFormatting.GREEN), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.peqol.autostock.removed", name).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static final int PAGE_SIZE = 10;

    private static int known(CommandSourceStack source, String search, int page) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return 0;
        }
        String needle = search.toLowerCase(java.util.Locale.ROOT).trim();
        List<ItemStack> matches = new ArrayList<>();
        for (ItemInfo info : provider.getKnowledge()) {
            ItemStack stack = info.createStack();
            if (stack.isEmpty() || !EmcUtil.hasValue(stack)) {
                continue;
            }
            if (!needle.isEmpty()) {
                String id = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
                if (!id.contains(needle) && !stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                    continue;
                }
            }
            matches.add(stack);
        }
        if (matches.isEmpty()) {
            source.sendFailure(Component.translatable("command.peqol.known.none"));
            return 0;
        }
        matches.sort((a, b) -> a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString()));
        int pages = (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int current = Math.min(page, pages);
        source.sendSuccess(() -> Component.translatable("command.peqol.known.header", matches.size(), current, pages).withStyle(ChatFormatting.GOLD), false);
        BigInteger emc = provider.getEmc();
        for (int i = (current - 1) * PAGE_SIZE; i < Math.min(matches.size(), current * PAGE_SIZE); i++) {
            ItemStack stack = matches.get(i);
            long unit = EmcUtil.value(stack);
            MutableComponent line = Component.literal(" ").append(stack.getHoverName().copy().withStyle(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)))))
                    .append(Component.literal(" " + EmcUtil.compact(BigInteger.valueOf(unit)) + " EMC ").withStyle(ChatFormatting.YELLOW));
            if (!stack.hasTag()) {
                line.append(buyButton(stack, 1, unit, emc)).append(" ").append(buyButton(stack, Math.max(1, stack.getMaxStackSize()), unit, emc));
            }
            source.sendSuccess(() -> line.withStyle(ChatFormatting.GRAY), false);
        }
        if (current < pages && needle.isEmpty()) {
            int next = current + 1;
            source.sendSuccess(() -> Component.translatable("command.peqol.known.next").withStyle(style -> style.withColor(ChatFormatting.AQUA)
                    .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/peqol known page " + next))), false);
        }
        return matches.size();
    }

    /**
     * A clickable [xN] chat button that runs /peqol buy.
     */
    public static Component buyButton(ItemStack stack, int count, long unit, BigInteger emc) {
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        boolean affordable = emc.compareTo(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(count))) >= 0;
        return Component.literal("[x" + count + "]").withStyle(style -> style
                .withColor(affordable ? ChatFormatting.GREEN : ChatFormatting.DARK_RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/peqol buy " + id + " " + count))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("command.peqol.buy.hover", count, stack.getHoverName(), EmcUtil.format(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(count)))))));
    }

    private static int starsFill(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return 0;
        }
        BigInteger emc = provider.getEmc();
        long[] budget = {emc.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue()};
        long start = budget[0];
        InventoryUtil.forEachStack(player, stack -> budget[0] -= EmcUtil.chargeHolder(stack, budget[0]));
        long spent = start - budget[0];
        if (spent <= 0) {
            source.sendFailure(Component.translatable("command.peqol.stars.nothing"));
            return 0;
        }
        provider.setEmc(emc.subtract(BigInteger.valueOf(spent)));
        provider.syncEmc(player);
        source.sendSuccess(() -> Component.translatable("command.peqol.stars.filled", EmcUtil.format(spent)).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int starsDrain(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BigInteger[] total = {BigInteger.ZERO};
        InventoryUtil.forEachStack(player, stack -> total[0] = total[0].add(BigInteger.valueOf(EmcUtil.drainHolder(stack, Long.MAX_VALUE))));
        if (total[0].signum() <= 0) {
            source.sendFailure(Component.translatable("command.peqol.stars.nothing"));
            return 0;
        }
        EmcUtil.addEmc(player, total[0]);
        BigInteger drained = total[0];
        source.sendSuccess(() -> Component.translatable("command.peqol.stars.drained", EmcUtil.format(drained)).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static BigInteger parseAmount(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BigInteger amount = EmcUtil.parseAmount(StringArgumentType.getString(ctx, "amount"));
        if (amount == null) {
            throw INVALID_AMOUNT.create();
        }
        return amount;
    }

    private static int balance(CommandSourceStack source, ServerPlayer target) {
        BigInteger emc = EmcUtil.getEmc(target);
        source.sendSuccess(() -> Component.translatable("command.peqol.balance", target.getDisplayName(),
                Component.literal(EmcUtil.format(emc)).withStyle(ChatFormatting.YELLOW),
                Component.literal(EmcUtil.compact(emc)).withStyle(ChatFormatting.GRAY)), false);
        return emc.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
    }

    private static int knowledge(CommandSourceStack source, ServerPlayer target) {
        IKnowledgeProvider provider = EmcUtil.knowledge(target).orElse(null);
        if (provider == null) {
            return 0;
        }
        int count = provider.getKnowledge().size();
        source.sendSuccess(() -> Component.translatable(provider.hasFullKnowledge() ? "command.peqol.knowledge.full" : "command.peqol.knowledge",
                target.getDisplayName(), Component.literal(String.valueOf(count)).withStyle(ChatFormatting.YELLOW)), false);
        int total = KnowledgeStats.itemsWithEmc();
        if (total > 0 && !provider.hasFullKnowledge()) {
            String percent = String.format(java.util.Locale.ROOT, "%.1f", Math.min(100.0, count * 100.0 / total));
            source.sendSuccess(() -> Component.translatable("command.peqol.knowledge.progress", count, total, percent).withStyle(ChatFormatting.AQUA), false);
        }
        return count;
    }

    private static int pay(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!PEQoLConfig.COMMON.payEnabled.get()) {
            throw DISABLED.create();
        }
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        if (sender == target) {
            throw SELF.create();
        }
        BigInteger amount = parseAmount(ctx);
        if (!EmcUtil.tryRemoveEmc(sender, amount)) {
            throw NOT_ENOUGH.create();
        }
        EmcUtil.addEmc(target, amount);
        String formatted = EmcUtil.format(amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.peqol.pay.sent", formatted, target.getDisplayName()).withStyle(ChatFormatting.GREEN), false);
        target.sendSystemMessage(Component.translatable("command.peqol.pay.received", formatted, sender.getDisplayName()).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int withdraw(CommandContext<CommandSourceStack> ctx, int cheques) throws CommandSyntaxException {
        if (!PEQoLConfig.COMMON.withdrawEnabled.get()) {
            throw DISABLED.create();
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BigInteger amount = parseAmount(ctx);
        BigInteger total = amount.multiply(BigInteger.valueOf(cheques));
        if (!EmcUtil.tryRemoveEmc(player, total)) {
            throw NOT_ENOUGH.create();
        }
        ItemStack stack = EmcChequeItem.create(amount, player.getGameProfile().getName());
        stack.setCount(cheques);
        InventoryUtil.giveOrDrop(player, stack);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.peqol.withdraw", cheques, EmcUtil.format(amount)).withStyle(ChatFormatting.GREEN), false);
        return cheques;
    }

    private static int worthHand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !EmcUtil.hasValue(held)) {
            source.sendFailure(Component.translatable("message.peqol.no_emc", held.getHoverName()));
            return 0;
        }
        long unit = EmcUtil.value(held);
        long sell = EmcUtil.sellValue(held);
        source.sendSuccess(() -> Component.translatable("command.peqol.worth.hand", held.getCount(), held.getHoverName(),
                Component.literal(EmcUtil.format(EmcUtil.stackValue(held))).withStyle(ChatFormatting.YELLOW),
                EmcUtil.format(unit), EmcUtil.format(sell)), false);
        return 1;
    }

    private static int worthInventory(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Inventory inventory = player.getInventory();
        BigInteger total = BigInteger.ZERO;
        int stacks = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && EmcUtil.hasValue(stack)) {
                total = total.add(EmcUtil.stackSellValue(stack));
                stacks++;
            }
        }
        BigInteger result = total;
        int counted = stacks;
        source.sendSuccess(() -> Component.translatable("command.peqol.worth.inventory",
                Component.literal(EmcUtil.format(result)).withStyle(ChatFormatting.YELLOW), counted), false);
        return counted;
    }

    private static void checkBurn(ServerPlayer player) throws CommandSyntaxException {
        if (!PEQoLConfig.COMMON.burnCommandEnabled.get()) {
            throw DISABLED.create();
        }
        if (PEQoLConfig.COMMON.burnCommandRequiresTablet.get() && !EmcUtil.hasTablet(player)) {
            throw NEED_TABLET.create();
        }
    }

    private static boolean burnable(ItemStack stack) {
        return !stack.isEmpty() && !EmcUtil.isTablet(stack) && !(stack.getItem() instanceof EmcChequeItem)
                && !stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).isPresent() && EmcUtil.hasValue(stack);
    }

    private static int burnHand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        checkBurn(player);
        ItemStack held = player.getMainHandItem();
        if (!burnable(held)) {
            source.sendFailure(Component.translatable("message.peqol.no_emc", held.getHoverName()));
            return 0;
        }
        int count = held.getCount();
        Component name = held.getHoverName();
        BigInteger gained = EmcUtil.burn(player, held, true);
        player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
        source.sendSuccess(() -> Component.translatable("message.peqol.burned", count, name, EmcUtil.format(gained)).withStyle(ChatFormatting.GOLD), false);
        return count;
    }

    /**
     * Burns the main inventory (not hotbar, armor or offhand).
     */
    private static int burnInventory(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        checkBurn(player);
        Inventory inventory = player.getInventory();
        boolean skipNbt = PEQoLConfig.COMMON.burnSkipsItemsWithNbt.get();
        BigInteger total = BigInteger.ZERO;
        int items = 0;
        for (int i = Inventory.getSelectionSize(); i < inventory.items.size(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (!burnable(stack) || (skipNbt && stack.hasTag())) {
                continue;
            }
            items += stack.getCount();
            total = total.add(EmcUtil.burn(player, stack, true));
            inventory.items.set(i, ItemStack.EMPTY);
        }
        if (items == 0) {
            source.sendFailure(Component.translatable("command.peqol.burn.nothing"));
            return 0;
        }
        BigInteger result = total;
        int burned = items;
        source.sendSuccess(() -> Component.translatable("command.peqol.burn.inventory", burned, EmcUtil.format(result)).withStyle(ChatFormatting.GOLD), false);
        return burned;
    }

    private static int missing(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Inventory inventory = player.getInventory();
        Map<Item, ItemStack> missing = new LinkedHashMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && !EmcUtil.hasValue(stack)) {
                missing.putIfAbsent(stack.getItem(), stack);
            }
        }
        if (missing.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.peqol.missing.none").withStyle(ChatFormatting.GREEN), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.peqol.missing.header", missing.size()).withStyle(ChatFormatting.GOLD), false);
        for (ItemStack stack : missing.values()) {
            source.sendSuccess(() -> Component.literal(" - ").append(stack.getHoverName()).withStyle(ChatFormatting.GRAY), false);
        }
        return missing.size();
    }

    private static int top(CommandSourceStack source) {
        List<ServerPlayer> players = new ArrayList<>(source.getServer().getPlayerList().getPlayers());
        players.sort((a, b) -> EmcUtil.getEmc(b).compareTo(EmcUtil.getEmc(a)));
        source.sendSuccess(() -> Component.translatable("command.peqol.top.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        int shown = Math.min(10, players.size());
        for (int i = 0; i < shown; i++) {
            ServerPlayer player = players.get(i);
            int rank = i + 1;
            BigInteger emc = EmcUtil.getEmc(player);
            source.sendSuccess(() -> Component.literal(rank + ". ").withStyle(ChatFormatting.GRAY)
                    .append(player.getDisplayName())
                    .append(Component.literal(" - " + EmcUtil.compact(emc) + " EMC").withStyle(ChatFormatting.YELLOW)), false);
        }
        return shown;
    }
}
