package com.gabrielflorianoo.peqol.client;

import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.gabrielflorianoo.peqol.item.ToggleableItem;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import com.gabrielflorianoo.peqol.util.InventoryUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Small HUD element with the player's personal EMC, its change rate and the amount of learned items.
 */
public final class EmcHudOverlay implements IGuiOverlay {

    public static final EmcHudOverlay INSTANCE = new EmcHudOverlay();
    private static final int SAMPLE_TICKS = 100;

    private final BigInteger[] samples = new BigInteger[SAMPLE_TICKS];
    private int sampleIndex;
    private int samplesTaken;
    private boolean hiddenByKey;
    private boolean hasTablet;
    private BigInteger currentEmc = BigInteger.ZERO;
    private BigInteger ratePerSecond = BigInteger.ZERO;
    private int knowledge;
    private boolean fullKnowledge;
    private final List<Component> activeCharms = new ArrayList<>();

    private EmcHudOverlay() {
    }

    public boolean toggle() {
        hiddenByKey = !hiddenByKey;
        return !hiddenByKey;
    }

    public void reset() {
        sampleIndex = 0;
        samplesTaken = 0;
        currentEmc = BigInteger.ZERO;
        ratePerSecond = BigInteger.ZERO;
    }

    public void tick(LocalPlayer player) {
        IKnowledgeProvider provider = EmcUtil.knowledge(player).orElse(null);
        if (provider == null) {
            return;
        }
        BigInteger previous = currentEmc;
        currentEmc = provider.getEmc();
        if (samplesTaken >= SAMPLE_TICKS && PEQoLConfig.CLIENT.milestoneToasts.get()) {
            checkMilestone(previous, currentEmc);
        }
        if (player.tickCount % 20 == 0) {
            knowledge = provider.getKnowledge().size();
            fullKnowledge = provider.hasFullKnowledge();
            hasTablet = EmcUtil.hasTablet(player);
            activeCharms.clear();
            InventoryUtil.forEachStack(player, stack -> {
                if (stack.getItem() instanceof ToggleableItem && ToggleableItem.isActive(stack)) {
                    Component name = stack.getItem().getDescription();
                    if (activeCharms.stream().noneMatch(c -> c.getString().equals(name.getString()))) {
                        activeCharms.add(name);
                    }
                }
            });
        }
        //Ring buffer: the value that is about to be overwritten is the one from SAMPLE_TICKS ticks ago
        BigInteger oldest = samples[sampleIndex];
        samples[sampleIndex] = currentEmc;
        sampleIndex = (sampleIndex + 1) % SAMPLE_TICKS;
        if (samplesTaken < SAMPLE_TICKS) {
            samplesTaken++;
            ratePerSecond = BigInteger.ZERO;
        } else if (oldest != null) {
            ratePerSecond = currentEmc.subtract(oldest).divide(BigInteger.valueOf(SAMPLE_TICKS / 20));
        }
    }

    private static void checkMilestone(BigInteger before, BigInteger now) {
        if (now.compareTo(before) <= 0 || now.compareTo(BigInteger.valueOf(1000)) < 0) {
            return;
        }
        //Number of digits = order of magnitude; a new digit means we crossed 10^n
        int digitsBefore = before.signum() <= 0 ? 0 : before.toString().length();
        int digitsNow = now.toString().length();
        if (digitsNow > digitsBefore && digitsNow >= 4) {
            BigInteger milestone = BigInteger.TEN.pow(digitsNow - 1);
            Minecraft mc = Minecraft.getInstance();
            SystemToast.addOrUpdate(mc.getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                    Component.translatable("toast.peqol.milestone.title"),
                    Component.translatable("toast.peqol.milestone.body", EmcUtil.compact(milestone)));
        }
    }

    private boolean shouldRender(Minecraft mc) {
        if (hiddenByKey || mc.options.hideGui || mc.options.renderDebug || mc.player == null) {
            return false;
        }
        return switch (PEQoLConfig.CLIENT.hudMode.get()) {
            case ALWAYS -> true;
            case TABLET_IN_INVENTORY -> hasTablet;
            case NEVER -> false;
        };
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldRender(mc)) {
            return;
        }
        boolean compact = PEQoLConfig.CLIENT.hudCompactNumbers.get();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("hud.peqol.emc", Component.literal(EmcUtil.format(currentEmc, compact)).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.YELLOW));
        if (PEQoLConfig.CLIENT.hudShowRate.get() && ratePerSecond.signum() != 0) {
            boolean positive = ratePerSecond.signum() > 0;
            lines.add(Component.translatable("hud.peqol.rate", (positive ? "+" : "") + EmcUtil.format(ratePerSecond, compact))
                    .withStyle(positive ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (PEQoLConfig.CLIENT.hudShowKnowledge.get()) {
            lines.add(fullKnowledge ? Component.translatable("hud.peqol.knowledge_full").withStyle(ChatFormatting.LIGHT_PURPLE)
                    : Component.translatable("hud.peqol.knowledge", knowledge).withStyle(ChatFormatting.AQUA));
        }

        if (PEQoLConfig.CLIENT.hudShowHeldValue.get() && mc.player != null) {
            ItemStack held = mc.player.getMainHandItem();
            long unit = EmcUtil.value(held);
            if (unit > 0) {
                lines.add(Component.translatable("hud.peqol.held", held.getCount(), held.getHoverName(),
                        EmcUtil.format(EmcUtil.stackValue(held), compact)).withStyle(ChatFormatting.GRAY));
            }
        }
        if (PEQoLConfig.CLIENT.hudShowActiveCharms.get()) {
            for (Component charm : activeCharms) {
                lines.add(Component.literal("\u2726 ").append(charm).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }

        Font font = mc.font;
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int lineHeight = font.lineHeight + 1;
        int height = lines.size() * lineHeight - 1;
        int offsetX = PEQoLConfig.CLIENT.hudOffsetX.get();
        int offsetY = PEQoLConfig.CLIENT.hudOffsetY.get();
        PEQoLConfig.HudCorner corner = PEQoLConfig.CLIENT.hudCorner.get();
        boolean right = corner == PEQoLConfig.HudCorner.TOP_RIGHT || corner == PEQoLConfig.HudCorner.BOTTOM_RIGHT;
        boolean bottom = corner == PEQoLConfig.HudCorner.BOTTOM_LEFT || corner == PEQoLConfig.HudCorner.BOTTOM_RIGHT;
        int x = right ? screenWidth - width - offsetX : offsetX;
        int y = bottom ? screenHeight - height - offsetY : offsetY;

        if (PEQoLConfig.CLIENT.hudBackground.get()) {
            graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x90000000);
            graphics.fill(x - 2, y - 2, x - 1, y + height + 2, 0xFFB04BFF);
        }
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int lineX = right ? x + width - font.width(line) : x;
            graphics.drawString(font, line, lineX, y + i * lineHeight, 0xFFFFFF, true);
        }
    }
}
