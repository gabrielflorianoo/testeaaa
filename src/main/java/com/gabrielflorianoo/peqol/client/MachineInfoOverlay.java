package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.network.MachineInfoRequestPacket;
import com.gabrielflorianoo.peqol.network.PEQoLNetwork;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Small "what am I looking at" panel for QoL+ machines and ProjectE EMC blocks, filled by the server.
 */
public final class MachineInfoOverlay implements IGuiOverlay {

    public static final MachineInfoOverlay INSTANCE = new MachineInfoOverlay();
    private static final int REQUEST_INTERVAL = 10;
    /**
     * With Jade installed the same info is shown inside Jade's tooltip instead.
     */
    private static final boolean JADE_LOADED = net.minecraftforge.fml.ModList.get().isLoaded("jade");

    private BlockPos target;
    private BlockPos infoPos;
    private List<Component> lines = new ArrayList<>();
    private long receivedAt;
    private long lastRequest;

    private MachineInfoOverlay() {
    }

    public void receive(BlockPos pos, List<Component> newLines) {
        Minecraft mc = Minecraft.getInstance();
        infoPos = pos;
        lines = newLines;
        receivedAt = mc.level == null ? 0 : mc.level.getGameTime();
    }

    public void tick(Minecraft mc) {
        target = null;
        if (!PEQoLConfig.CLIENT.machineInfo.get() || JADE_LOADED || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be == null || !interesting(be)) {
            return;
        }
        target = pos;
        long time = mc.level.getGameTime();
        if (!pos.equals(infoPos) || time - lastRequest >= REQUEST_INTERVAL) {
            lastRequest = time;
            PEQoLNetwork.sendToServer(new MachineInfoRequestPacket(pos));
        }
    }

    private static boolean interesting(BlockEntity be) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(be.getBlockState().getBlock());
        if (id != null && PEQoL.MODID.equals(id.getNamespace())) {
            return true;
        }
        return be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY).isPresent();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (target == null || !target.equals(infoPos) || lines.isEmpty() || mc.level == null || mc.options.hideGui
                || mc.level.getGameTime() - receivedAt > 40) {
            return;
        }
        Font font = mc.font;
        List<Component> shown = new ArrayList<>();
        shown.add(mc.level.getBlockState(target).getBlock().getName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        shown.addAll(lines);
        int width = 0;
        for (Component line : shown) {
            width = Math.max(width, font.width(line));
        }
        int lineHeight = font.lineHeight + 1;
        int x = screenWidth / 2 + 12;
        int y = screenHeight / 2 - (shown.size() * lineHeight) / 2;
        graphics.fill(x - 3, y - 3, x + width + 3, y + shown.size() * lineHeight + 1, 0xA0100818);
        graphics.fill(x - 3, y - 3, x - 2, y + shown.size() * lineHeight + 1, 0xFFB04BFF);
        for (int i = 0; i < shown.size(); i++) {
            graphics.drawString(font, shown.get(i), x, y + i * lineHeight, 0xFFFFFF, true);
        }
    }
}
