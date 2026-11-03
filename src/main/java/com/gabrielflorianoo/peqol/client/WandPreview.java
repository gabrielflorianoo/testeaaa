package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.item.ConstructionWandItem;
import com.gabrielflorianoo.peqol.item.WandPlacement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHighlightEvent;

/**
 * Outlines where the Construction Wand would place blocks.
 */
public final class WandPreview {

    private static BlockPos cachedOrigin;
    private static net.minecraft.core.Direction cachedFace;
    private static int cachedSize;
    private static long cachedTick = -1;
    private static List<BlockPos> cachedTargets = List.of();

    private WandPreview() {
    }

    public static void render(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ConstructionWandItem)) {
            held = player.getOffhandItem();
            if (!(held.getItem() instanceof ConstructionWandItem)) {
                return;
            }
        }
        BlockHitResult hit = event.getTarget();
        int size = ConstructionWandItem.getSize(held);
        long tick = mc.level.getGameTime();
        //Recompute at most every few ticks or when the target changes
        if (!hit.getBlockPos().equals(cachedOrigin) || hit.getDirection() != cachedFace || size != cachedSize || tick - cachedTick > 5) {
            cachedOrigin = hit.getBlockPos();
            cachedFace = hit.getDirection();
            cachedSize = size;
            cachedTick = tick;
            cachedTargets = WandPlacement.findTargets(mc.level, cachedOrigin, cachedFace, size);
        }
        if (cachedTargets.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        for (BlockPos pos : cachedTargets) {
            AABB box = new AABB(pos).deflate(0.02).move(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(poseStack, lines, box, 0.75F, 0.4F, 1F, 0.8F);
        }
    }
}
