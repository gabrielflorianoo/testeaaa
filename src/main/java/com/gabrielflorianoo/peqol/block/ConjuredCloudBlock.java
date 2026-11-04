package com.gabrielflorianoo.peqol.block;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Temporary block conjured by the Cloud Scepter: no drops, cushions falls, vanishes after a while.
 */
public class ConjuredCloudBlock extends Block {

    public ConjuredCloudBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, PEQoLConfig.COMMON.cloudLifetime.get());
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        level.removeBlock(pos, false);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    public void fallOn(@NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull Entity entity, float distance) {
        //Soft as a cloud
        entity.causeFallDamage(distance, 0.0F, level.damageSources().fall());
    }
}
