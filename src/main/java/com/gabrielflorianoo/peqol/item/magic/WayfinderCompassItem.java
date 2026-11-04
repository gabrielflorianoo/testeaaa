package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wayfinder's Compass: pick a kind of place (sneak + right click cycles), right click to find the nearest one.
 * While held, a trail of light points towards the last place found.
 */
public class WayfinderCompassItem extends MagicItem {

    public static final String TAG_MODE = "WayfinderMode";
    public static final String TAG_TARGET = "WayfinderTarget";
    public static final String TAG_DIMENSION = "WayfinderDimension";
    public static final String[] TARGETS = {"village", "temple", "ruins", "shipwreck", "treasure", "mineshaft", "ruined_portal", "monument",
            "mansion", "ancient_city", "stronghold", "fortress", "bastion", "end_city"};

    public WayfinderCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{EmcUtil.format(PEQoLConfig.COMMON.wayfinderCost.get())};
    }

    public static String getMode(ItemStack stack) {
        int index = stack.hasTag() ? stack.getTag().getInt(TAG_MODE) : 0;
        return TARGETS[Math.floorMod(index, TARGETS.length)];
    }

    public static TagKey<Structure> tagFor(String target) {
        return TagKey.create(Registries.STRUCTURE, PEQoL.rl("wayfinder/" + target));
    }

    @Nullable
    public static BlockPos getTarget(ItemStack stack, Level level) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_TARGET) || !level.dimension().location().toString().equals(stack.getTag().getString(TAG_DIMENSION))) {
            return null;
        }
        return BlockPos.of(stack.getTag().getLong(TAG_TARGET));
    }

    public static Component direction(BlockPos from, BlockPos to) {
        double angle = Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX()));
        //0 = east, 90 = south
        String[] names = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};
        int index = Math.floorMod((int) Math.round(angle / 45.0), 8);
        return Component.translatable("message.peqol.wayfinder.dir." + names[index]);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (player.isShiftKeyDown()) {
            int index = (stack.getOrCreateTag().getInt(TAG_MODE) + 1) % TARGETS.length;
            stack.getOrCreateTag().putInt(TAG_MODE, index);
            player.displayClientMessage(Component.translatable("message.peqol.wayfinder.mode",
                    Component.translatable("wayfinder.peqol." + getMode(stack))).withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.success(stack);
        }
        String mode = getMode(stack);
        if (!pay(serverPlayer, PEQoLConfig.COMMON.wayfinderCost.get())) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, 60);
        BlockPos found = serverLevel.findNearestMapStructure(tagFor(mode), player.blockPosition(), PEQoLConfig.COMMON.wayfinderRadius.get(), false);
        Component name = Component.translatable("wayfinder.peqol." + mode);
        if (found == null) {
            //Refund: nothing to find here (wrong dimension or too far)
            if (!player.getAbilities().instabuild) {
                EmcUtil.addEmc(serverPlayer, java.math.BigInteger.valueOf(PEQoLConfig.COMMON.wayfinderCost.get()));
            }
            player.displayClientMessage(Component.translatable("message.peqol.wayfinder.none", name).withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.getOrCreateTag().putLong(TAG_TARGET, found.asLong());
        stack.getOrCreateTag().putString(TAG_DIMENSION, level.dimension().location().toString());
        int distance = (int) Math.sqrt(Math.pow(found.getX() - player.getX(), 2) + Math.pow(found.getZ() - player.getZ(), 2));
        player.displayClientMessage(Component.translatable("message.peqol.wayfinder.found", name, distance, direction(player.blockPosition(), found))
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getEyeY(), player.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1F, 1F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.peqol.wayfinder.mode", Component.translatable("wayfinder.peqol." + getMode(stack))).withStyle(ChatFormatting.AQUA));
        if (stack.hasTag() && stack.getTag().contains(TAG_TARGET)) {
            BlockPos target = BlockPos.of(stack.getTag().getLong(TAG_TARGET));
            tooltip.add(Component.translatable("tooltip.peqol.wayfinder.target", target.getX(), target.getZ()).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
