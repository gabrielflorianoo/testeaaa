package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.registry.ModStats;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sneak + right click a block to anchor that spot; right click to teleport back, paying personal EMC based on distance.
 */
public class AlchemicalAnchorItem extends Item {

    public static final String TAG_POS = "AnchorPos";
    public static final String TAG_DIM = "AnchorDim";

    public AlchemicalAnchorItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static BlockPos getPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_POS) ? BlockPos.of(tag.getLong(TAG_POS)) : null;
    }

    @Nullable
    public static ResourceLocation getDimension(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_DIM) ? ResourceLocation.tryParse(tag.getString(TAG_DIM)) : null;
    }

    public static BigInteger cost(Player player, BlockPos target, ResourceLocation dimension) {
        BigInteger cost = BigInteger.valueOf(PEQoLConfig.COMMON.anchorBaseCost.get());
        if (!player.level().dimension().location().equals(dimension)) {
            return cost.add(BigInteger.valueOf(PEQoLConfig.COMMON.anchorCrossDimensionCost.get()));
        }
        long distance = (long) Math.sqrt(player.blockPosition().distSqr(target));
        return cost.add(BigInteger.valueOf(PEQoLConfig.COMMON.anchorCostPerBlock.get()).multiply(BigInteger.valueOf(distance)));
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            BlockPos spot = context.getClickedPos().relative(context.getClickedFace());
            CompoundTag tag = context.getItemInHand().getOrCreateTag();
            tag.putLong(TAG_POS, spot.asLong());
            tag.putString(TAG_DIM, context.getLevel().dimension().location().toString());
            player.displayClientMessage(Component.translatable("message.peqol.anchor.set", spot.getX(), spot.getY(), spot.getZ())
                    .withStyle(ChatFormatting.GREEN), true);
            context.getLevel().playSound(null, spot, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (!PEQoLConfig.COMMON.anchorEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.peqol.disabled").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos target = getPos(stack);
        ResourceLocation dimension = getDimension(stack);
        if (target == null || dimension == null) {
            player.displayClientMessage(Component.translatable("message.peqol.anchor.unset").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        if (player.isShiftKeyDown()) {
            //Sneak + right click in the air: show where it points and how much it costs
            player.displayClientMessage(Component.translatable("message.peqol.anchor.info", target.getX(), target.getY(), target.getZ(), dimension.toString(),
                    EmcUtil.format(cost(player, target, dimension))).withStyle(ChatFormatting.AQUA), false);
            return InteractionResultHolder.success(stack);
        }
        ServerLevel destination = serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.peqol.anchor.no_dimension").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!destination.getBlockState(target).getCollisionShape(destination, target).isEmpty()
                || !destination.getBlockState(target.above()).getCollisionShape(destination, target.above()).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.peqol.anchor.obstructed").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        BigInteger cost = player.getAbilities().instabuild ? BigInteger.ZERO : cost(player, target, dimension);
        if (cost.signum() > 0 && !EmcUtil.tryRemoveEmc(serverPlayer, cost)) {
            player.displayClientMessage(Component.translatable("message.peqol.anchor.too_expensive", EmcUtil.format(cost)).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel origin = serverPlayer.serverLevel();
        origin.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.3, 0.6, 0.3, 0.05);
        origin.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.8F);
        serverPlayer.teleportTo(destination, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
        serverPlayer.fallDistance = 0;
        destination.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, 40, 0.3, 0.6, 0.3, 0.05);
        destination.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        serverPlayer.getCooldowns().addCooldown(this, PEQoLConfig.COMMON.anchorCooldown.get());
        serverPlayer.awardStat(ModStats.ANCHOR_TELEPORTS);
        if (cost.signum() > 0) {
            player.displayClientMessage(Component.translatable("message.peqol.anchor.paid", EmcUtil.format(cost)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return getPos(stack) != null;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        BlockPos pos = getPos(stack);
        ResourceLocation dimension = getDimension(stack);
        if (pos != null && dimension != null) {
            tooltip.add(Component.translatable("tooltip.peqol.anchor.pos", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal(dimension.toString()).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tooltip.add(Component.translatable("message.peqol.anchor.unset").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("item.peqol.alchemical_anchor.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.peqol.alchemical_anchor.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
