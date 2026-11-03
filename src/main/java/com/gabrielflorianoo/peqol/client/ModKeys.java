package com.gabrielflorianoo.peqol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {

    public static final String CATEGORY = "key.categories.peqol";

    public static final KeyMapping OPEN_TABLET = new KeyMapping("key.peqol.open_tablet", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J), CATEGORY);
    public static final KeyMapping OPEN_BAG = new KeyMapping("key.peqol.open_bag", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B), CATEGORY);
    public static final KeyMapping QUICK_BURN = new KeyMapping("key.peqol.quick_burn", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);
    public static final KeyMapping TOGGLE_HUD = new KeyMapping("key.peqol.toggle_hud", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H), CATEGORY);
    public static final KeyMapping TOGGLE_RESTOCKER = new KeyMapping("key.peqol.toggle_restocker", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);
    public static final KeyMapping TOGGLE_SIPHON = new KeyMapping("key.peqol.toggle_siphon", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);

    public static final KeyMapping TOGGLE_MENDING = new KeyMapping("key.peqol.toggle_mending", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);
    public static final KeyMapping TOGGLE_SATIATION = new KeyMapping("key.peqol.toggle_satiation", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);
    public static final KeyMapping TOGGLE_STELLAR = new KeyMapping("key.peqol.toggle_stellar", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);

    public static final KeyMapping TOGGLE_ILLUMINATION = new KeyMapping("key.peqol.toggle_illumination", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);

    public static final KeyMapping TOGGLE_SMELTING = new KeyMapping("key.peqol.toggle_smelting", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);

    public static final KeyMapping LEARN_INVENTORY = new KeyMapping("key.peqol.learn_inventory", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, CATEGORY);

    public static final KeyMapping[] ALL = {OPEN_TABLET, OPEN_BAG, QUICK_BURN, TOGGLE_HUD, TOGGLE_RESTOCKER, TOGGLE_SIPHON,
            TOGGLE_MENDING, TOGGLE_SATIATION, TOGGLE_STELLAR, TOGGLE_ILLUMINATION, TOGGLE_SMELTING, LEARN_INVENTORY};

    private ModKeys() {
    }
}
