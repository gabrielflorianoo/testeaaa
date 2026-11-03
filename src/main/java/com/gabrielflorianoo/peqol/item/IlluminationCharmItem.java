package com.gabrielflorianoo.peqol.item;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * While active, places a light source (torch by default) at your feet in dark places.
 * Uses torches from the inventory first, then personal EMC.
 */
public class IlluminationCharmItem extends ToggleableItem {

    public IlluminationCharmItem(Properties properties) {
        super(properties);
    }

    @Nullable
    private static BlockItem lightItem() {
        ResourceLocation id = ResourceLocation.tryParse(PEQoLConfig.COMMON.illuminationItem.get());
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item instanceof BlockItem blockItem ? blockItem : null;
    }

    public static void illuminate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        int threshold = PEQoLConfig.COMMON.illuminationThreshold.get();
        if (level.getBrightness(LightLayer.BLOCK, pos) >= threshold || level.getBrightness(LightLayer.SKY, pos) >= threshold) {
            return;
        }
        BlockItem lightItem = lightItem();
        if (lightItem == null) {
            return;
        }
        BlockState state = lightItem.getBlock().defaultBlockState();
        if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos) || !level.mayInteract(player, pos)) {
            return;
        }
        ItemStack template = new ItemStack(lightItem);
        Inventory inventory = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (stack.is(lightItem) && !stack.hasTag()) {
                slot = i;
                break;
            }
        }
        IKnowledgeProvider provider = null;
        long unit = 0;
        if (slot < 0 && !player.getAbilities().instabuild) {
            provider = EmcUtil.knowledge(player).orElse(null);
            unit = EmcUtil.value(template);
            if (provider == null || unit <= 0 || !provider.hasKnowledge(template) || EmcUtil.isBlacklisted(template) || provider.getEmc().compareTo(BigInteger.valueOf(unit)) < 0) {
                return;
            }
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return;
        }
        if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) {
            snapshot.restore(true, false);
            return;
        }
        if (!player.getAbilities().instabuild) {
            if (slot >= 0) {
                inventory.items.get(slot).shrink(1);
            } else if (provider != null) {
                provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(unit)));
                provider.syncEmc(player);
            }
        }
        player.awardStat(com.gabrielflorianoo.peqol.registry.ModStats.LIGHTS_PLACED);
        SoundType sound = state.getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, 0.6F, sound.getPitch());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        BlockItem lightItem = lightItem();
        if (lightItem != null) {
            tooltip.add(Component.translatable("item.peqol.illumination_charm.desc2", lightItem.getDescription(), PEQoLConfig.COMMON.illuminationThreshold.get())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
