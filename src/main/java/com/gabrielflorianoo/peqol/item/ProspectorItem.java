package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right click: scans the area around you for ores and reports their EMC value.
 */
public class ProspectorItem extends Item {

    private static final int TOP = 6;

    public ProspectorItem(Properties properties) {
        super(properties);
    }

    public record OreReport(BigInteger total, Map<Block, Integer> counts) {
    }

    public static OreReport scan(Level level, BlockPos center, int radius) {
        Map<Block, Integer> counts = new HashMap<>();
        BigInteger total = BigInteger.ZERO;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(Tags.Blocks.ORES)) {
                        continue;
                    }
                    long value = EmcUtil.value(new ItemStack(state.getBlock()));
                    counts.merge(state.getBlock(), 1, Integer::sum);
                    total = total.add(BigInteger.valueOf(value));
                }
            }
        }
        return new OreReport(total, counts);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        int radius = PEQoLConfig.COMMON.prospectorRadius.get();
        OreReport report = scan(level, player.blockPosition(), radius);
        player.sendSystemMessage(Component.translatable("message.peqol.prospector.header", radius).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        if (report.counts().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.peqol.prospector.none").withStyle(ChatFormatting.GRAY));
        } else {
            int ores = report.counts().values().stream().mapToInt(Integer::intValue).sum();
            player.sendSystemMessage(Component.translatable("message.peqol.prospector.total", ores,
                    Component.literal(EmcUtil.format(report.total())).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
            List<Map.Entry<Block, Integer>> entries = new ArrayList<>(report.counts().entrySet());
            entries.sort((a, b) -> Long.compare(EmcUtil.value(new ItemStack(b.getKey())) * b.getValue(), EmcUtil.value(new ItemStack(a.getKey())) * a.getValue()));
            for (int i = 0; i < Math.min(TOP, entries.size()); i++) {
                Map.Entry<Block, Integer> entry = entries.get(i);
                long each = EmcUtil.value(new ItemStack(entry.getKey()));
                player.sendSystemMessage(Component.literal(" " + entry.getValue() + "x ").append(entry.getKey().getName())
                        .append(Component.literal(" (" + EmcUtil.compact(BigInteger.valueOf(each * entry.getValue())) + " EMC)").withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        player.getCooldowns().addCooldown(this, 40);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.4F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.peqol.prospector.desc", PEQoLConfig.COMMON.prospectorRadius.get()).withStyle(ChatFormatting.GRAY));
    }
}
