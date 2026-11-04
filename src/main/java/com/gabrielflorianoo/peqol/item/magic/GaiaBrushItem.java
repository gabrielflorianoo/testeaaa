package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gaia's Brush: sneak + right click to soak the brush in the biome you stand in, then right click the ground to
 * paint that biome around it (grass/water colours, weather, mob spawns all follow).
 */
public class GaiaBrushItem extends MagicItem {

    public static final String TAG_BIOME = "GaiaBiome";

    public GaiaBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        int r = PEQoLConfig.COMMON.gaiaRadius.get();
        return new Object[]{r * 2 + 1, EmcUtil.format(PEQoLConfig.COMMON.gaiaCostPerColumn.get())};
    }

    @Nullable
    public static ResourceLocation getBiome(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_BIOME) ? ResourceLocation.tryParse(stack.getTag().getString(TAG_BIOME)) : null;
    }

    public static Component biomeName(ResourceLocation id) {
        return Component.translatable(Util.makeDescriptionId("biome", id));
    }

    /**
     * Paints the biome in the columns within {@code radius} of {@code center} (whole build height). Returns how
     * many chunks were changed.
     */
    public static int paint(ServerLevel level, BlockPos center, int radius, Holder<Biome> biome) {
        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minZ = center.getZ() - radius, maxZ = center.getZ() + radius;
        List<ChunkAccess> chunks = new ArrayList<>();
        for (int cx = SectionPos.blockToSectionCoord(minX); cx <= SectionPos.blockToSectionCoord(maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(minZ); cz <= SectionPos.blockToSectionCoord(maxZ); cz++) {
                chunks.add(level.getChunk(cx, cz));
            }
        }
        for (ChunkAccess chunk : chunks) {
            chunk.fillBiomesFromNoise((x, y, z, sampler) -> {
                Holder<Biome> current = chunk.getNoiseBiome(x, y, z);
                //Every 4x4x4 biome cell that touches the area is painted, so the edges are clean
                int bx = QuartPos.toBlock(x);
                int bz = QuartPos.toBlock(z);
                return bx + 3 >= minX && bx <= maxX && bz + 3 >= minZ && bz <= maxZ ? biome : current;
            }, level.getChunkSource().randomState().sampler());
            chunk.setUnsaved(true);
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
        return chunks.size();
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel serverLevel = (ServerLevel) level;
        ItemStack brush = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (player.isShiftKeyDown()) {
            Optional<ResourceKey<Biome>> key = level.getBiome(pos).unwrapKey();
            if (key.isPresent()) {
                brush.getOrCreateTag().putString(TAG_BIOME, key.get().location().toString());
                player.displayClientMessage(Component.translatable("message.peqol.gaia.soaked", biomeName(key.get().location())).withStyle(ChatFormatting.GREEN), true);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
            return InteractionResult.CONSUME;
        }
        ResourceLocation id = getBiome(brush);
        if (id == null) {
            player.displayClientMessage(Component.translatable("message.peqol.gaia.dry").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        Optional<Holder.Reference<Biome>> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME, id));
        if (biome.isEmpty() || !level.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }
        int radius = PEQoLConfig.COMMON.gaiaRadius.get();
        long columns = (long) (radius * 2 + 1) * (radius * 2 + 1);
        if (!pay(serverPlayer, columns * PEQoLConfig.COMMON.gaiaCostPerColumn.get())) {
            return InteractionResult.FAIL;
        }
        paint(serverLevel, pos, radius, biome.get());
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, radius / 2.0, 0.3, radius / 2.0, 0);
        serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 40, radius / 2.0, 1, radius / 2.0, 0);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 0.8F);
        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 1F, 1F);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        ResourceLocation id = getBiome(stack);
        tooltip.add(id == null ? Component.translatable("message.peqol.gaia.dry").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("tooltip.peqol.gaia.biome", biomeName(id)).withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
