package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, feeds the player with the chosen (learned) food paid with personal EMC whenever it would not be wasted.
 * Sneak + right click with a food in the other hand to choose it.
 */
public class SatiationCharmItem extends ToggleableItem {

    public static final String TAG_FOOD = "Food";

    public SatiationCharmItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static Item getFood(ItemStack charm) {
        CompoundTag tag = charm.getTag();
        if (tag == null || !tag.contains(TAG_FOOD)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_FOOD));
        return id == null ? null : ForgeRegistries.ITEMS.getValue(id);
    }

    @Override
    protected InteractionResultHolder<ItemStack> useSneaking(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.isEmpty() || other.getFoodProperties(player) == null) {
            player.displayClientMessage(Component.translatable("message.peqol.satiation.need_food").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!EmcUtil.hasValue(other)) {
            player.displayClientMessage(Component.translatable("message.peqol.no_emc", other.getHoverName()).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(other.getItem());
        if (id == null) {
            return InteractionResultHolder.fail(stack);
        }
        stack.getOrCreateTag().putString(TAG_FOOD, id.toString());
        player.displayClientMessage(Component.translatable("message.peqol.satiation.set", other.getHoverName()).withStyle(ChatFormatting.GREEN), true);
        return InteractionResultHolder.success(stack);
    }

    public static void feed(ServerPlayer player, ItemStack charm) {
        Item food = getFood(charm);
        if (food == null || !player.getFoodData().needsFood()) {
            return;
        }
        ItemStack meal = new ItemStack(food);
        FoodProperties properties = meal.getFoodProperties(player);
        if (properties == null) {
            return;
        }
        //Only eat when the whole nutrition fits, so nothing is wasted
        if (player.getFoodData().getFoodLevel() + properties.getNutrition() > 20 && player.getFoodData().getFoodLevel() > 6) {
            return;
        }
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null || !provider.hasKnowledge(meal) || EmcUtil.isBlacklisted(meal)) {
            return;
        }
        long value = EmcUtil.value(meal);
        if (value <= 0) {
            return;
        }
        BigInteger cost = BigInteger.valueOf(value);
        if (provider.getEmc().compareTo(cost) < 0) {
            return;
        }
        provider.setEmc(provider.getEmc().subtract(cost));
        provider.syncEmc(player);
        player.eat(player.level(), meal);
        player.awardStat(com.gabrielflorianoo.peqol.registry.ModStats.MEALS_EATEN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5F, 1.2F);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        Item food = getFood(stack);
        tooltip.add(food == null ? Component.translatable("tooltip.peqol.satiation.no_food").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("tooltip.peqol.satiation.food", food.getDescription()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.peqol.satiation_charm.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
