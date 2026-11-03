package com.gabrielflorianoo.peqol.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Pending "/peqol share" offers: target -> (sender, expiry game time).
 */
public final class KnowledgeShare {

    public static final int EXPIRY_TICKS = 20 * 60;
    private static final Map<UUID, Offer> OFFERS = new HashMap<>();

    public record Offer(UUID sender, long expiresAt) {
    }

    private KnowledgeShare() {
    }

    public static void offer(ServerPlayer sender, ServerPlayer target) {
        OFFERS.put(target.getUUID(), new Offer(sender.getUUID(), target.level().getGameTime() + EXPIRY_TICKS));
    }

    /**
     * @return the pending, non expired offer for the player (removing it), or null.
     */
    public static Offer take(ServerPlayer target) {
        Offer offer = OFFERS.remove(target.getUUID());
        if (offer == null || target.level().getGameTime() > offer.expiresAt()) {
            return null;
        }
        return offer;
    }

    /**
     * Teaches the target everything the sender knows (that has EMC).
     *
     * @return the amount of newly learned items.
     */
    public static int teach(ServerPlayer sender, ServerPlayer target) {
        IKnowledgeProvider from = EmcUtil.knowledge(sender).orElse(null);
        IKnowledgeProvider to = EmcUtil.knowledge(target).orElse(null);
        if (from == null || to == null) {
            return 0;
        }
        int learned = 0;
        for (ItemInfo info : from.getKnowledge()) {
            if (!EmcUtil.emc().hasValue(info)) {
                continue;
            }
            ItemInfo cleaned = EmcUtil.emc().getPersistentInfo(info);
            if (to.hasKnowledge(cleaned) || MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(target, info, cleaned))) {
                continue;
            }
            if (to.addKnowledge(cleaned)) {
                learned++;
            }
        }
        if (learned > 0) {
            to.sync(target);
        }
        return learned;
    }
}
