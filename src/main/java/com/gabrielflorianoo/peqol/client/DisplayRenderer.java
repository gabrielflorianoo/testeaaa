package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.block.entity.AlchemicalDisplayBlockEntity;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.math.BigInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Floating, camera-facing text above the Alchemical Display.
 */
public class DisplayRenderer implements BlockEntityRenderer<AlchemicalDisplayBlockEntity> {

    private final Font font;

    public DisplayRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(@NotNull AlchemicalDisplayBlockEntity display, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        Component[] lines = {
                Component.literal(display.getOwnerData().getOwnerName()).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(EmcUtil.compact(display.getShownEmc()) + " EMC").withStyle(ChatFormatting.YELLOW),
                rateLine(display.getRatePerSecond())
        };
        poseStack.pushPose();
        poseStack.translate(0.5, 1.6, 0.5);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = poseStack.last().pose();
        int background = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        for (int i = 0; i < lines.length; i++) {
            Component line = lines[i];
            if (line.getString().isEmpty()) {
                continue;
            }
            float x = -font.width(line) / 2F;
            font.drawInBatch(line, x, i * 10F, 0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL, background, 0xF000F0);
        }
        poseStack.popPose();
    }

    private static Component rateLine(BigInteger rate) {
        if (rate.signum() == 0) {
            return Component.empty();
        }
        boolean up = rate.signum() > 0;
        return Component.literal((up ? "+" : "") + EmcUtil.compact(rate) + "/s").withStyle(up ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull AlchemicalDisplayBlockEntity display) {
        return true;
    }
}
