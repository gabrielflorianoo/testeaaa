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
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
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
 * /peqol balance|pay|withdraw|worth|burn|missing|top|knowledge
 */
public final class PEQoLCommand {

    private static final SimpleCommandExceptionType INVALID_AMOUNT = new SimpleCommandExceptionType(Component.translatable("command.peqol.invalid_amount"));
    private static final SimpleCommandExceptionType DISABLED = new SimpleCommandExceptionType(Component.translatable("message.peqol.disabled"));
    private static final SimpleCommandExceptionType NOT_ENOUGH = new SimpleCommandExceptionType(Component.translatable("command.peqol.not_enough_emc"));
    private static final SimpleCommandExceptionType NEED_TABLET = new SimpleCommandExceptionType(Component.translatable("message.peqol.need_tablet"));
    private static final SimpleCommandExceptionType SELF = new SimpleCommandExceptionType(Component.translatable("command.peqol.pay.self"));

    private PEQoLCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("peqol")
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
