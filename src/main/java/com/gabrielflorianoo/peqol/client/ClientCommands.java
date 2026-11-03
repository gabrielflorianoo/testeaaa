package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only /peqolclient command to tweak the HUD and tooltips without editing config files.
 */
public final class ClientCommands {

    private ClientCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, net.minecraft.commands.CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> corner = Commands.literal("corner");
        for (PEQoLConfig.HudCorner value : PEQoLConfig.HudCorner.values()) {
            corner.then(Commands.literal(value.name().toLowerCase(Locale.ROOT)).executes(ctx -> set(ctx.getSource(), PEQoLConfig.CLIENT.hudCorner, value)));
        }
        LiteralArgumentBuilder<CommandSourceStack> mode = Commands.literal("mode");
        for (PEQoLConfig.HudMode value : PEQoLConfig.HudMode.values()) {
            mode.then(Commands.literal(value.name().toLowerCase(Locale.ROOT)).executes(ctx -> set(ctx.getSource(), PEQoLConfig.CLIENT.hudMode, value)));
        }
        dispatcher.register(Commands.literal("peqolclient")
                .then(Commands.literal("hud")
                        .then(corner)
                        .then(mode)
                        .then(Commands.literal("offset")
                                .then(Commands.argument("x", IntegerArgumentType.integer(0, 4000))
                                        .then(Commands.argument("y", IntegerArgumentType.integer(0, 4000))
                                                .executes(ctx -> {
                                                    PEQoLConfig.CLIENT.hudOffsetX.set(IntegerArgumentType.getInteger(ctx, "x"));
                                                    PEQoLConfig.CLIENT.hudOffsetX.save();
                                                    return set(ctx.getSource(), PEQoLConfig.CLIENT.hudOffsetY, IntegerArgumentType.getInteger(ctx, "y"));
                                                }))))
                        .then(toggle("compact", PEQoLConfig.CLIENT.hudCompactNumbers))
                        .then(toggle("rate", PEQoLConfig.CLIENT.hudShowRate))
                        .then(toggle("charms", PEQoLConfig.CLIENT.hudShowActiveCharms))
                        .then(toggle("held", PEQoLConfig.CLIENT.hudShowHeldValue))
                        .then(toggle("stars", PEQoLConfig.CLIENT.hudShowKleinStars))
                        .then(toggle("background", PEQoLConfig.CLIENT.hudBackground)))
                .then(Commands.literal("tooltips")
                        .then(toggle("affordable", PEQoLConfig.CLIENT.tooltipAffordable))
                        .then(toggle("sellvalue", PEQoLConfig.CLIENT.tooltipSellValue))
                        .then(toggle("contents", PEQoLConfig.CLIENT.tooltipContainerContents))
                        .then(toggle("noemc", PEQoLConfig.CLIENT.tooltipNoEmcWarning)))
                .then(Commands.literal("goal")
                        .then(Commands.literal("clear").executes(ctx -> set(ctx.getSource(), PEQoLConfig.CLIENT.goalItem, "")))
                        .then(Commands.argument("item", net.minecraft.commands.arguments.item.ItemArgument.item(buildContext))
                                .executes(ctx -> setGoal(ctx.getSource(), net.minecraft.commands.arguments.item.ItemArgument.getItem(ctx, "item").getItem(), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(ctx -> setGoal(ctx.getSource(), net.minecraft.commands.arguments.item.ItemArgument.getItem(ctx, "item").getItem(),
                                                IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(toggle("machineinfo", PEQoLConfig.CLIENT.machineInfo))
                .then(toggle("learntoasts", PEQoLConfig.CLIENT.learnToasts))
                .then(toggle("milestones", PEQoLConfig.CLIENT.milestoneToasts)));
    }

    private static int setGoal(CommandSourceStack source, net.minecraft.world.item.Item item, int count) {
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (id == null) {
            return 0;
        }
        PEQoLConfig.CLIENT.goalCount.set(count);
        PEQoLConfig.CLIENT.goalCount.save();
        return set(source, PEQoLConfig.CLIENT.goalItem, id.toString());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, ForgeConfigSpec.BooleanValue value) {
        return Commands.literal(name).executes(ctx -> set(ctx.getSource(), value, !value.get()));
    }

    private static <T> int set(CommandSourceStack source, ForgeConfigSpec.ConfigValue<T> config, T value) {
        config.set(value);
        config.save();
        String path = String.join(".", config.getPath());
        source.sendSuccess(() -> Component.translatable("command.peqol.client.set", path, String.valueOf(value)).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
