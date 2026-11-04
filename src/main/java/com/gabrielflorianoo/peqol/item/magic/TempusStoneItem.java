package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tempus Stone: right click a block to make time flow faster for it (crops, saplings, furnaces, brewing stands...).
 * Clicking the same block again doubles the speed.
 */
public class TempusStoneItem extends MagicItem {

    public static final TagKey<Block> BLACKLIST = TagKey.create(Registries.BLOCK, PEQoL.rl("tempus_blacklist"));
    public static final int BASE_SPEED = 4;

    public static final class Acceleration {
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        int speed;
        int ticksLeft;

        Acceleration(ResourceKey<Level> dimension, BlockPos pos, int speed, int ticksLeft) {
            this.dimension = dimension;
            this.pos = pos;
            this.speed = speed;
            this.ticksLeft = ticksLeft;
        }

        public int speed() {
            return speed;
        }
    }

    private static final List<Acceleration> ACTIVE = new ArrayList<>();

    public TempusStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{PEQoLConfig.COMMON.tempusDuration.get() / 20, EmcUtil.format(PEQoLConfig.COMMON.tempusCost.get())};
    }

    public static boolean canAccelerate(BlockState state, @Nullable BlockEntity be) {
        if (state.isAir() || state.is(BLACKLIST)) {
            return false;
        }
        return state.isRandomlyTicking() || be != null;
    }

    @Nullable
    public static Acceleration find(ResourceKey<Level> dimension, BlockPos pos) {
        for (Acceleration acceleration : ACTIVE) {
            if (acceleration.dimension == dimension && acceleration.pos.equals(pos)) {
                return acceleration;
            }
        }
        return null;
    }

    /**
     * Starts (or doubles) the acceleration of a block. Returns the new speed.
     */
    public static int accelerate(ResourceKey<Level> dimension, BlockPos pos) {
        Acceleration existing = find(dimension, pos);
        int duration = PEQoLConfig.COMMON.tempusDuration.get();
        if (existing != null) {
            existing.speed = Math.min(PEQoLConfig.COMMON.tempusMaxSpeed.get(), existing.speed * 2);
            existing.ticksLeft = duration;
            return existing.speed;
        }
        ACTIVE.add(new Acceleration(dimension, pos.immutable(), Math.min(BASE_SPEED, PEQoLConfig.COMMON.tempusMaxSpeed.get()), duration));
        return Math.min(BASE_SPEED, PEQoLConfig.COMMON.tempusMaxSpeed.get());
    }

    public static void clear() {
        ACTIVE.clear();
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void tickBlockEntity(Level level, BlockPos pos, BlockState state, T be) {
        BlockEntityTicker<T> ticker = state.getTicker(level, (BlockEntityType<T>) be.getType());
        if (ticker != null) {
            ticker.tick(level, pos, state, be);
        }
    }

    /**
     * Runs the extra ticks of one accelerated block. Exposed for tests.
     */
    public static void tickOnce(ServerLevel level, BlockPos pos, int speed) {
        for (int i = 0; i < speed - 1; i++) {
            BlockState state = level.getBlockState(pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (!canAccelerate(state, be)) {
                return;
            }
            if (be != null && !be.isRemoved()) {
                tickBlockEntity(level, pos, state, be);
            }
            //Vanilla gives a block a random tick about every 68 seconds; this gives one every ~2.5 s per speed step
            if (state.isRandomlyTicking() && level.random.nextInt(50) == 0) {
                state.randomTick(level, pos, level.random);
            }
        }
    }

    public static void tickAll(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Acceleration> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Acceleration acceleration = iterator.next();
            ServerLevel level = server.getLevel(acceleration.dimension);
            if (--acceleration.ticksLeft <= 0 || level == null) {
                iterator.remove();
                continue;
            }
            if (!level.isLoaded(acceleration.pos)) {
                continue;
            }
            tickOnce(level, acceleration.pos, acceleration.speed);
            if (acceleration.ticksLeft % 10 == 0) {
                BlockPos p = acceleration.pos;
                level.sendParticles(ParticleTypes.ENCHANT, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, Math.min(12, acceleration.speed), 0.3, 0.2, 0.3, 0.3);
            }
        }
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!canAccelerate(state, level.getBlockEntity(pos))) {
            if (!level.isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.peqol.tempus.refuse").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer) || !level.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }
        Acceleration existing = find(level.dimension(), pos);
        if (existing != null && existing.speed >= PEQoLConfig.COMMON.tempusMaxSpeed.get() && existing.ticksLeft > PEQoLConfig.COMMON.tempusDuration.get() / 2) {
            player.displayClientMessage(Component.translatable("message.peqol.tempus.max").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        if (!pay(serverPlayer, PEQoLConfig.COMMON.tempusCost.get())) {
            return InteractionResult.FAIL;
        }
        int speed = accelerate(level.dimension(), pos);
        player.displayClientMessage(Component.translatable("message.peqol.tempus.speed", state.getBlock().getName(), speed).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        ((ServerLevel) level).sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 0.5F + speed / 32F);
        return InteractionResult.CONSUME;
    }
}
