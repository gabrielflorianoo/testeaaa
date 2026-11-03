package com.gabrielflorianoo.peqol.block.entity;

import com.gabrielflorianoo.peqol.registry.ModBlockEntities;
import com.gabrielflorianoo.peqol.util.EmcUtil;
import java.math.BigInteger;
import java.util.List;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A player shop: sells the configured product out of the inventory below it, for EMC paid to the owner.
 */
public class TradePostBlockEntity extends BlockEntity implements IOwnedMachine {

    private final OwnerData ownerData = new OwnerData();
    @Nullable
    private ItemInfo product;
    private BigInteger price = BigInteger.ZERO;
    private BigInteger pendingEmc = BigInteger.ZERO;
    private long sold;
    private int ticks;
    /**
     * false = the shop sells to customers, true = the shop buys from customers.
     */
    private boolean buyMode;

    public boolean isBuyMode() {
        return buyMode;
    }

    public TradePostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADE_POST.get(), pos, state);
    }

    @Override
    public OwnerData getOwnerData() {
        return ownerData;
    }

    /**
     * Keeps the world-wide shop list (/peqol shops) in sync with this post.
     */
    private void publish() {
        if (level == null || level.isClientSide || !ownerData.hasOwner()) {
            return;
        }
        String productId = product == null ? "" : String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(product.getItem()));
        com.gabrielflorianoo.peqol.util.ShopRegistry.update(level, worldPosition, ownerData.getOwnerName(), productId, price.toString(), buyMode);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        publish();
    }

    public void setPrice(BigInteger price) {
        this.price = price.signum() < 0 ? BigInteger.ZERO : price;
        setChanged();
    }

    public BigInteger getPrice() {
        return price;
    }

    public void setProduct(@Nullable ItemInfo product) {
        this.product = product;
        setChanged();
    }

    @Nullable
    public ItemInfo getProduct() {
        return product;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TradePostBlockEntity post) {
        if (++post.ticks % 40 != 0 || post.pendingEmc.signum() == 0) {
            return;
        }
        ServerPlayer owner = EmcUtil.onlinePlayer(level.getServer(), post.ownerData.getOwner());
        if (owner != null) {
            EmcUtil.addEmc(owner, post.pendingEmc);
            owner.sendSystemMessage(Component.translatable("message.peqol.shop.earnings", EmcUtil.format(post.pendingEmc)).withStyle(ChatFormatting.GOLD));
            post.pendingEmc = BigInteger.ZERO;
            post.setChanged();
        }
    }

    @Nullable
    private IItemHandler stock() {
        if (level == null) {
            return null;
        }
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        return below == null ? null : below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
    }

    public int stockCount() {
        IItemHandler stock = stock();
        if (stock == null || product == null) {
            return 0;
        }
        ItemStack template = product.createStack();
        int count = 0;
        for (int i = 0; i < stock.getSlots(); i++) {
            if (ItemHandlerHelper.canItemStacksStack(stock.getStackInSlot(i), template)) {
                count += stock.getStackInSlot(i).getCount();
            }
        }
        return count;
    }

    /**
     * Owner: right click with an item sets it as the product.
     */
    @Override
    public boolean onItemUse(ServerPlayer player, InteractionHand hand, ItemStack held) {
        if (!ownerData.isOwner(player) && !player.hasPermissions(2)) {
            //Customers holding something just get the info
            return false;
        }
        product = ItemInfo.fromStack(held.copyWithCount(1));
        if (price.signum() == 0) {
            price = BigInteger.valueOf(Math.max(1, EmcUtil.value(held)));
        }
        setChanged();
        player.displayClientMessage(Component.translatable("message.peqol.shop.product", held.getHoverName(), EmcUtil.format(price)).withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    /**
     * Customer purchase. Sneaking buys a whole stack.
     */
    public void buy(ServerPlayer buyer, boolean stack) {
        if (product == null || price.signum() <= 0) {
            buyer.displayClientMessage(Component.translatable("message.peqol.shop.closed").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (ownerData.isOwner(buyer)) {
            buyer.displayClientMessage(Component.translatable("message.peqol.shop.own").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        IItemHandler stock = stock();
        if (stock == null) {
            buyer.displayClientMessage(Component.translatable("message.peqol.shop.no_stock").withStyle(ChatFormatting.RED), true);
            return;
        }
        ItemStack template = product.createStack();
        int wanted = stack ? template.getMaxStackSize() : 1;
        int available = Math.min(wanted, stockCount());
        if (available <= 0) {
            buyer.displayClientMessage(Component.translatable("message.peqol.shop.no_stock").withStyle(ChatFormatting.RED), true);
            return;
        }
        int affordable = (int) Math.min(available, EmcUtil.affordable(EmcUtil.getEmc(buyer), price.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue()));
        if (affordable <= 0) {
            buyer.displayClientMessage(Component.translatable("command.peqol.not_enough_emc").withStyle(ChatFormatting.RED), true);
            return;
        }
        //Pull the items out of the stock first, then charge for what was actually taken
        int taken = 0;
        ItemStack bought = ItemStack.EMPTY;
        for (int i = 0; i < stock.getSlots() && taken < affordable; i++) {
            if (!ItemHandlerHelper.canItemStacksStack(stock.getStackInSlot(i), template)) {
                continue;
            }
            ItemStack extracted = stock.extractItem(i, affordable - taken, false);
            if (extracted.isEmpty()) {
                continue;
            }
            taken += extracted.getCount();
            if (bought.isEmpty()) {
                bought = extracted;
            } else {
                bought.grow(extracted.getCount());
            }
        }
        if (taken <= 0) {
            return;
        }
        BigInteger cost = price.multiply(BigInteger.valueOf(taken));
        if (!EmcUtil.tryRemoveEmc(buyer, cost)) {
            //Should not happen (checked above), but never give items for free
            ItemHandlerHelper.insertItemStacked(stock, bought, false);
            return;
        }
        ServerPlayer owner = level == null ? null : EmcUtil.onlinePlayer(level.getServer(), ownerData.getOwner());
        if (owner != null) {
            EmcUtil.addEmc(owner, cost);
            owner.sendSystemMessage(Component.translatable("message.peqol.shop.sold", buyer.getDisplayName(), taken, bought.getHoverName(), EmcUtil.format(cost))
                    .withStyle(ChatFormatting.GOLD));
        } else {
            pendingEmc = pendingEmc.add(cost);
        }
        sold += taken;
        setChanged();
        if (!buyer.getInventory().add(bought) && !bought.isEmpty()) {
            buyer.drop(bought, false);
        }
        buyer.displayClientMessage(Component.translatable("message.peqol.shop.bought", taken, template.getHoverName(), EmcUtil.format(cost)).withStyle(ChatFormatting.GREEN), true);
        buyer.level().playSound(null, worldPosition, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.7F, 1.1F);
    }

    /**
     * Owner: sneak + empty hand toggles between selling and buying.
     */
    @Override
    public void onSneakUse(ServerPlayer player) {
        if (!ownerData.isOwner(player) && !player.hasPermissions(2)) {
            return;
        }
        buyMode = !buyMode;
        setChanged();
        player.displayClientMessage(Component.translatable(buyMode ? "message.peqol.shop.mode_buy" : "message.peqol.shop.mode_sell")
                .withStyle(ChatFormatting.AQUA), true);
    }

    /**
     * Buy mode: a customer sells the held product to the shop; the owner pays.
     */
    public void sellToShop(ServerPlayer customer, ItemStack held) {
        if (product == null || price.signum() <= 0 || !ItemHandlerHelper.canItemStacksStack(held, product.createStack())) {
            customer.displayClientMessage(Component.translatable("message.peqol.shop.not_buying").withStyle(ChatFormatting.RED), true);
            return;
        }
        ServerPlayer owner = level == null ? null : EmcUtil.onlinePlayer(level.getServer(), ownerData.getOwner());
        IItemHandler stock = stock();
        if (owner == null || stock == null) {
            customer.displayClientMessage(Component.translatable("message.peqol.shop.owner_away").withStyle(ChatFormatting.RED), true);
            return;
        }
        int affordable = (int) Math.min(held.getCount(), EmcUtil.affordable(EmcUtil.getEmc(owner), price.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue()));
        if (affordable <= 0) {
            customer.displayClientMessage(Component.translatable("message.peqol.shop.owner_broke").withStyle(ChatFormatting.RED), true);
            return;
        }
        ItemStack offered = held.copyWithCount(affordable);
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(stock, offered, false);
        int accepted = affordable - leftover.getCount();
        if (accepted <= 0) {
            customer.displayClientMessage(Component.translatable("message.peqol.shop.full").withStyle(ChatFormatting.RED), true);
            return;
        }
        BigInteger cost = price.multiply(BigInteger.valueOf(accepted));
        if (!EmcUtil.tryRemoveEmc(owner, cost)) {
            return;
        }
        held.shrink(accepted);
        EmcUtil.addEmc(customer, cost);
        sold += accepted;
        setChanged();
        customer.displayClientMessage(Component.translatable("message.peqol.shop.sold_to", accepted, offered.getHoverName(), EmcUtil.format(cost)).withStyle(ChatFormatting.GREEN), true);
        owner.sendSystemMessage(Component.translatable("message.peqol.shop.bought_from", customer.getDisplayName(), accepted, offered.getHoverName(), EmcUtil.format(cost))
                .withStyle(ChatFormatting.GOLD));
        customer.level().playSound(null, worldPosition, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.7F, 0.9F);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.peqol.owner", ownerData.describe()).withStyle(ChatFormatting.GRAY));
        if (product == null) {
            lines.add(Component.translatable("info.peqol.shop.no_product").withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.translatable(buyMode ? "info.peqol.shop.buying" : "info.peqol.shop.product", product.createStack().getHoverName(), EmcUtil.format(price))
                    .withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("info.peqol.shop.stock", stockCount()).withStyle(ChatFormatting.GOLD));
        }
        lines.add(Component.translatable("info.peqol.shop.sold", sold).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.peqol.shop.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ownerData.save(tag);
        if (product != null) {
            tag.put("Product", product.write(new CompoundTag()));
        }
        tag.putString("Price", price.toString());
        tag.putString("PendingEmc", pendingEmc.toString());
        tag.putLong("Sold", sold);
        tag.putBoolean("BuyMode", buyMode);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerData.load(tag);
        product = tag.contains("Product") ? ItemInfo.read(tag.getCompound("Product")) : null;
        price = parse(tag.getString("Price"));
        pendingEmc = parse(tag.getString("PendingEmc"));
        sold = tag.getLong("Sold");
        buyMode = tag.getBoolean("BuyMode");
    }

    private static BigInteger parse(String text) {
        try {
            return text.isEmpty() ? BigInteger.ZERO : new BigInteger(text);
        } catch (NumberFormatException e) {
            return BigInteger.ZERO;
        }
    }
}
