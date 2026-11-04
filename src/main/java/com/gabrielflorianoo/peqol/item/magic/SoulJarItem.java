package com.gabrielflorianoo.peqol.item.magic;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Soul Jar: right click a creature to trap it (villagers too), right click a block to release it there.
 */
public class SoulJarItem extends MagicItem {

    public static final String TAG_ENTITY = "CapturedEntity";

    public SoulJarItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Object[] costArgs() {
        return new Object[]{EmcUtil.format(PEQoLConfig.COMMON.soulJarCost.get())};
    }

    public static boolean isFilled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_ENTITY);
    }

    @Nullable
    public static EntityType<?> capturedType(ItemStack stack) {
        return isFilled(stack) ? EntityType.byString(stack.getTag().getCompound(TAG_ENTITY).getString("id")).orElse(null) : null;
    }

    public static boolean canCapture(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player) && entity.isAlive() && entity.getType().canSerialize()
                && !entity.getType().is(Tags.EntityTypes.BOSSES) && !entity.isPassenger() && !entity.isVehicle();
    }

    /**
     * Stores the entity into the jar and removes it from the world.
     */
    public static boolean store(ItemStack jar, Entity entity) {
        if (isFilled(jar) || !canCapture(entity)) {
            return false;
        }
        CompoundTag tag = new CompoundTag();
        if (!entity.saveAsPassenger(tag)) {
            return false;
        }
        tag.remove("UUID");
        jar.getOrCreateTag().put(TAG_ENTITY, tag);
        entity.discard();
        return true;
    }

    /**
     * Spawns the stored entity at {@code pos} and empties the jar.
     */
    @Nullable
    public static Entity release(ItemStack jar, ServerLevel level, Vec3 pos) {
        if (!isFilled(jar)) {
            return null;
        }
        CompoundTag tag = jar.getTag().getCompound(TAG_ENTITY);
        Entity entity = EntityType.loadEntityRecursive(tag, level, e -> {
            e.moveTo(pos.x, pos.y, pos.z, e.getYRot(), e.getXRot());
            return e;
        });
        if (entity == null || !level.tryAddFreshEntityWithPassengers(entity)) {
            return null;
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        jar.getTag().remove(TAG_ENTITY);
        return entity;
    }

    /**
     * Called from the entity interact event so it works on villagers and other clickable mobs.
     */
    public static boolean tryCapture(ServerPlayer player, InteractionHand hand, Entity target) {
        ItemStack jar = player.getItemInHand(hand);
        if (!(jar.getItem() instanceof SoulJarItem) || isFilled(jar)) {
            return false;
        }
        if (!canCapture(target) || !player.level().mayInteract(player, target.blockPosition())) {
            player.displayClientMessage(Component.translatable("message.peqol.soul_jar.refuse").withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (!pay(player, PEQoLConfig.COMMON.soulJarCost.get())) {
            return true;
        }
        Vec3 at = target.position();
        Component name = target.getDisplayName();
        if (store(jar, target)) {
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.6, at.z, 20, 0.3, 0.5, 0.3, 0.05);
            player.level().playSound(null, BlockPos.containing(at), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 1F);
            player.displayClientMessage(Component.translatable("message.peqol.soul_jar.captured", name).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return true;
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        ItemStack jar = context.getItemInHand();
        if (!isFilled(jar)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        if (player != null && !level.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }
        Entity entity = release(jar, (ServerLevel) level, Vec3.atBottomCenterOf(pos));
        if (entity == null) {
            return InteractionResult.FAIL;
        }
        ((ServerLevel) level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 0.5, entity.getZ(), 20, 0.3, 0.5, 0.3, 0.03);
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isFilled(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        EntityType<?> type = capturedType(stack);
        tooltip.add(type == null ? Component.translatable("tooltip.peqol.soul_jar.empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.peqol.soul_jar.holds", type.getDescription()).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
