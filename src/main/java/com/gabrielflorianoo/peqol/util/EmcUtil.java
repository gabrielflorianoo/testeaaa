package com.gabrielflorianoo.peqol.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.api.proxy.ITransmutationProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Thin helpers on top of the public ProjectE API.
 */
public final class EmcUtil {

    public static final ResourceLocation TRANSMUTATION_TABLET = new ResourceLocation("projecte", "transmutation_tablet");
    private static final NumberFormat FULL_FORMAT = NumberFormat.getIntegerInstance(Locale.ROOT);
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};
    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);

    private EmcUtil() {
    }

    public static IEMCProxy emc() {
        return IEMCProxy.INSTANCE;
    }

    public static long value(ItemStack stack) {
        return stack.isEmpty() ? 0 : emc().getValue(stack);
    }

    public static long sellValue(ItemStack stack) {
        return stack.isEmpty() ? 0 : emc().getSellValue(stack);
    }

    public static boolean hasValue(ItemStack stack) {
        return value(stack) > 0;
    }

    /**
     * @return The total sell value of the whole stack.
     */
    public static BigInteger stackSellValue(ItemStack stack) {
        return BigInteger.valueOf(sellValue(stack)).multiply(BigInteger.valueOf(stack.getCount()));
    }

    public static BigInteger stackValue(ItemStack stack) {
        return BigInteger.valueOf(value(stack)).multiply(BigInteger.valueOf(stack.getCount()));
    }

    public static Optional<IKnowledgeProvider> knowledge(Player player) {
        return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve();
    }

    /**
     * Server side lookup of the knowledge of a (possibly offline) player. Offline providers are read-only copies.
     */
    public static IKnowledgeProvider knowledge(UUID uuid) {
        return ITransmutationProxy.INSTANCE.getKnowledgeProviderFor(uuid);
    }

    @Nullable
    public static ServerPlayer onlinePlayer(@Nullable MinecraftServer server, @Nullable UUID uuid) {
        if (server == null || uuid == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(uuid);
    }

    public static BigInteger getEmc(Player player) {
        return knowledge(player).map(IKnowledgeProvider::getEmc).orElse(BigInteger.ZERO);
    }

    /**
     * Adds (or removes when negative) personal EMC and syncs it to the client.
     */
    public static void addEmc(ServerPlayer player, BigInteger amount) {
        if (amount.signum() == 0) {
            return;
        }
        knowledge(player).ifPresent(provider -> {
            BigInteger result = provider.getEmc().add(amount);
            provider.setEmc(result.signum() < 0 ? BigInteger.ZERO : result);
            provider.syncEmc(player);
        });
    }

    /**
     * Removes EMC if the player has enough of it.
     *
     * @return true if the EMC was removed.
     */
    public static boolean tryRemoveEmc(ServerPlayer player, BigInteger amount) {
        Optional<IKnowledgeProvider> provider = knowledge(player);
        if (provider.isEmpty()) {
            return false;
        }
        BigInteger current = provider.get().getEmc();
        if (current.compareTo(amount) < 0) {
            return false;
        }
        provider.get().setEmc(current.subtract(amount));
        provider.get().syncEmc(player);
        return true;
    }

    public static boolean isKnown(Player player, ItemStack stack) {
        return knowledge(player).map(k -> k.hasKnowledge(stack)).orElse(false);
    }

    /**
     * Learns the given stack the same way the transmutation table does (fires {@link PlayerAttemptLearnEvent}).
     *
     * @return true if the item was newly learned.
     */
    public static boolean tryLearn(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !hasValue(stack)) {
            return false;
        }
        Optional<IKnowledgeProvider> optional = knowledge(player);
        if (optional.isEmpty()) {
            return false;
        }
        IKnowledgeProvider provider = optional.get();
        ItemInfo info = ItemInfo.fromStack(stack);
        ItemInfo cleaned = emc().getPersistentInfo(info);
        if (provider.hasKnowledge(cleaned) || MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, info, cleaned))) {
            return false;
        }
        if (provider.addKnowledge(cleaned)) {
            provider.syncKnowledgeChange(player, cleaned, true);
            return true;
        }
        return false;
    }

    /**
     * Converts a stack into personal EMC (learning it), exactly like dropping it into the transmutation table.
     *
     * @return the EMC gained, zero if the stack has no EMC (and was not consumed).
     */
    public static BigInteger burn(ServerPlayer player, ItemStack stack, boolean learn) {
        BigInteger gained = stackSellValue(stack);
        if (gained.signum() <= 0) {
            return BigInteger.ZERO;
        }
        if (learn) {
            tryLearn(player, stack);
        }
        addEmc(player, gained);
        player.awardStat(com.gabrielflorianoo.peqol.registry.ModStats.ITEMS_BURNED, stack.getCount());
        return gained;
    }

    /**
     * @return How many items of the stack's type the given EMC can create (capped at Integer.MAX_VALUE).
     */
    public static long affordable(BigInteger emc, long unitValue) {
        if (unitValue <= 0) {
            return 0;
        }
        BigInteger amount = emc.divide(BigInteger.valueOf(unitValue));
        return amount.min(BigInteger.valueOf(Integer.MAX_VALUE)).longValue();
    }

    /**
     * Inserts up to {@code budget} EMC into an EMC holding item (Klein Stars & co).
     *
     * @return the EMC actually inserted.
     */
    public static long chargeHolder(ItemStack stack, long budget) {
        if (budget <= 0 || stack.isEmpty()) {
            return 0;
        }
        IItemEmcHolder holder = stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve().orElse(null);
        if (holder == null) {
            return 0;
        }
        long toInsert = Math.min(budget, holder.getNeededEmc(stack));
        return toInsert <= 0 ? 0 : holder.insertEmc(stack, toInsert, IEmcStorage.EmcAction.EXECUTE);
    }

    /**
     * Pulls up to {@code max} EMC out of an EMC holding item.
     *
     * @return the EMC actually extracted.
     */
    public static long drainHolder(ItemStack stack, long max) {
        if (max <= 0 || stack.isEmpty()) {
            return 0;
        }
        IItemEmcHolder holder = stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve().orElse(null);
        if (holder == null) {
            return 0;
        }
        long toExtract = Math.min(max, holder.getStoredEmc(stack));
        return toExtract <= 0 ? 0 : holder.extractEmc(stack, toExtract, IEmcStorage.EmcAction.EXECUTE);
    }

    public static boolean isEmcHolder(ItemStack stack) {
        return !stack.isEmpty() && stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).isPresent();
    }

    /**
     * @return true if the server blacklisted this item from being created out of personal EMC.
     */
    public static boolean isBlacklisted(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        java.util.List<? extends String> entries = com.gabrielflorianoo.peqol.config.PEQoLConfig.COMMON.blacklist.get();
        if (entries.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
                if (tag != null && stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag))) {
                    return true;
                }
            } else if (id != null && entry.equals(id.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return EMC stored inside an EMC holder item (Klein Star...), 0 otherwise.
     */
    public static long storedEmc(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).map(holder -> holder.getStoredEmc(stack)).orElse(0L);
    }

    public static boolean isTablet(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return TRANSMUTATION_TABLET.equals(ForgeRegistries.ITEMS.getKey(item));
    }

    public static boolean hasTablet(Player player) {
        return InventoryUtil.contains(player, EmcUtil::isTablet);
    }

    public static String format(BigInteger value) {
        return FULL_FORMAT.format(value);
    }

    public static String format(long value) {
        return FULL_FORMAT.format(value);
    }

    public static String format(BigInteger value, boolean compact) {
        return compact ? compact(value) : format(value);
    }

    public static String format(long value, boolean compact) {
        return format(BigInteger.valueOf(value), compact);
    }

    /**
     * Formats big numbers like 1.25M, 3.4T, ...
     */
    public static String compact(BigInteger value) {
        boolean negative = value.signum() < 0;
        BigInteger abs = value.abs();
        if (abs.compareTo(THOUSAND) < 0) {
            return value.toString();
        }
        int tier = 0;
        BigDecimal decimal = new BigDecimal(abs);
        BigDecimal thousand = new BigDecimal(THOUSAND);
        while (decimal.compareTo(thousand) >= 0 && tier < SUFFIXES.length - 1) {
            decimal = decimal.divide(thousand, 3, RoundingMode.DOWN);
            tier++;
        }
        if (decimal.compareTo(thousand) >= 0) {
            //Out of suffixes, fall back to scientific notation
            String plain = abs.toString();
            return (negative ? "-" : "") + plain.charAt(0) + "." + plain.substring(1, Math.min(3, plain.length())) + "e" + (plain.length() - 1);
        }
        int scale = decimal.compareTo(BigDecimal.valueOf(100)) >= 0 ? 0 : decimal.compareTo(BigDecimal.TEN) >= 0 ? 1 : 2;
        String text = decimal.setScale(scale, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
        return (negative ? "-" : "") + text + SUFFIXES[tier];
    }

    /**
     * Parses user amounts like 1500, 1.5k, 20M, 3b, 1e6.
     */
    @Nullable
    public static BigInteger parseAmount(String input) {
        String text = input.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(",", "");
        if (text.isEmpty()) {
            return null;
        }
        BigDecimal multiplier = BigDecimal.ONE;
        String[] lowerSuffixes = {"k", "m", "b", "t", "qa", "qi"};
        for (int i = lowerSuffixes.length - 1; i >= 0; i--) {
            if (text.endsWith(lowerSuffixes[i])) {
                multiplier = BigDecimal.TEN.pow(3 * (i + 1));
                text = text.substring(0, text.length() - lowerSuffixes[i].length());
                break;
            }
        }
        try {
            BigDecimal result = new BigDecimal(text).multiply(multiplier);
            if (result.signum() <= 0) {
                return null;
            }
            return result.setScale(0, RoundingMode.DOWN).toBigIntegerExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }
}
