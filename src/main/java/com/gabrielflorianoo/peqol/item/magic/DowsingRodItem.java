package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Dowsing Rod: sneak + right click a block to attune the rod to it, then right click anywhere and a trail of light
 * leads to the nearest matching block (ores match their deepslate / nether variants through forge:ores tags).
 */
public class DowsingRodItem extends MagicItem {

    public static final String TAG_BLOCK = "DowsingBlock";

    public DowsingRodItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.dowsingRadius.get(), EmcUtil.format(PEQoLConfig.COMMON.dowsingCost.get())};
    }

    @Nullable
    public static Block getTarget(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_BLOCK)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(stack.getTag().getString(TAG_BLOCK));
        return id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
    }

    /**
     * forge:ores/xxx tags of the block, so iron ore also finds deepslate iron ore.
     */
    public static Set<TagKey<Block>> oreTags(Block block) {
        return block.builtInRegistryHolder().tags()
                .filter(tag -> tag.location().getNamespace().equals("forge") && tag.location().getPath().startsWith("ores/"))
                .collect(Collectors.toSet());
    }

    public static boolean matches(BlockState state, Block target, Set<TagKey<Block>> tags) {
        if (state.is(target)) {
            return true;
        }
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Nearest matching block within the radius, or null.
     */
    @Nullable
    public static BlockPos nearest(Level level, BlockPos center, Block target, int radius) {
        Set<TagKey<Block>> tags = oreTags(target);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius);
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                if (!level.hasChunkAt(new BlockPos(x, center.getY(), z))) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    double distance = pos.distSqr(center);
                    if (distance < bestDistance && matches(level.getBlockState(pos), target, tags)) {
                        best = pos.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide) {
            Block block = level.getBlockState(context.getClickedPos()).getBlock();
            context.getItemInHand().getOrCreateTag().putString(TAG_BLOCK, String.valueOf(ForgeRegistries.BLOCKS.getKey(block)));
            player.displayClientMessage(Component.translatable("message.peqol.dowsing.attuned", block.getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            level.playSound(null, context.getClickedPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 1.5F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel) || player.isShiftKeyDown()) {
            return InteractionResultHolder.success(stack);
        }
        Block target = getTarget(stack);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.peqol.dowsing.none").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.dowsingCost.get())) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, 30);
        BlockPos found = nearest(level, player.blockPosition(), target, PEQoLConfig.COMMON.dowsingRadius.get());
        if (found == null) {
            player.displayClientMessage(Component.translatable("message.peqol.dowsing.nothing", target.getName()).withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.PLAYERS, 1F, 0.5F);
            return InteractionResultHolder.success(stack);
        }
        Vec3 from = player.getEyePosition().subtract(0, 0.3, 0);
        Vec3 to = Vec3.atCenterOf(found);
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        dir = dir.normalize();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.4F, 1F, 0.9F), 1.2F);
        //Only the dowser sees the trail
        for (double d = 1; d < Math.min(length, 12); d += 0.4) {
            Vec3 p = from.add(dir.scale(d));
            serverLevel.sendParticles(serverPlayer, dust, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        serverLevel.sendParticles(serverPlayer, ParticleTypes.GLOW, true, to.x, to.y, to.z, 20, 0.3, 0.3, 0.3, 0.02);
        int dy = found.getY() - player.getBlockY();
        String vertical = dy > 2 ? "above" : dy < -2 ? "below" : "level";
        player.displayClientMessage(Component.translatable("message.peqol.dowsing.found", target.getName(), (int) Math.round(length),
                Component.translatable("message.peqol.dowsing." + vertical)).withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 0.8F + (float) Math.max(0, 1 - length / 24));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        Block target = getTarget(stack);
        tooltip.add(target == null ? Component.translatable("message.peqol.dowsing.none").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("tooltip.peqol.dowsing.target", target.getName()).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
