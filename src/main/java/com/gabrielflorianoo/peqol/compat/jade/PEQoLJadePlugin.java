package com.gabrielflorianoo.peqol.compat.jade;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.network.MachineInfoRequestPacket;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade integration: QoL+ machines (owner, buffer, tier, status) and ProjectE EMC blocks (stored EMC, rate,
 * condenser target/progress) are described in Jade's tooltip. Only loaded when Jade is installed.
 */
@WailaPlugin
public class PEQoLJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Provider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE, Block.class);
    }

    public enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY = "peqol_lines";
        private static final ResourceLocation UID = PEQoL.rl("machine_info");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            List<Component> lines = MachineInfoRequestPacket.describe(accessor.getBlockEntity());
            if (lines.isEmpty()) {
                return;
            }
            ListTag list = new ListTag();
            for (Component line : lines) {
                list.add(StringTag.valueOf(Component.Serializer.toJson(line)));
            }
            data.put(KEY, list);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data == null || !data.contains(KEY, Tag.TAG_LIST)) {
                return;
            }
            ListTag list = data.getList(KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                Component line = Component.Serializer.fromJson(list.getString(i));
                if (line != null) {
                    tooltip.add(line);
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
