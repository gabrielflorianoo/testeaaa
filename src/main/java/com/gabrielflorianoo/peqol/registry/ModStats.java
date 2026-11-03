package com.gabrielflorianoo.peqol.registry;

import com.gabrielflorianoo.peqol.PEQoL;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Custom statistics shown in Statistics > General.
 */
public final class ModStats {

    public static final DeferredRegister<ResourceLocation> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, PEQoL.MODID);

    public static final ResourceLocation ITEMS_BURNED = register("items_burned");
    public static final ResourceLocation ITEMS_CREATED = register("items_created");
    public static final ResourceLocation BLOCKS_BUILT = register("blocks_built");
    public static final ResourceLocation ITEMS_SMELTED = register("items_smelted");
    public static final ResourceLocation ITEMS_REPAIRED = register("items_repaired");
    public static final ResourceLocation LIGHTS_PLACED = register("lights_placed");
    public static final ResourceLocation ANCHOR_TELEPORTS = register("anchor_teleports");
    public static final ResourceLocation MEALS_EATEN = register("meals_eaten");

    private ModStats() {
    }

    private static ResourceLocation register(String name) {
        ResourceLocation id = PEQoL.rl(name);
        STATS.register(name, () -> id);
        return id;
    }

    /**
     * Must run after registration (common setup) so the stats get a formatter.
     */
    public static void init() {
        for (ResourceLocation id : new ResourceLocation[]{ITEMS_BURNED, ITEMS_CREATED, BLOCKS_BUILT, ITEMS_SMELTED, ITEMS_REPAIRED, LIGHTS_PLACED,
                ANCHOR_TELEPORTS, MEALS_EATEN}) {
            Stats.CUSTOM.get(id, StatFormatter.DEFAULT);
        }
    }
}
