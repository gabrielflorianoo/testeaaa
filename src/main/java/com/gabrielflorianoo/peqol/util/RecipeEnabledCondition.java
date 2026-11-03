package com.gabrielflorianoo.peqol.util;

import com.gabrielflorianoo.peqol.PEQoL;
import com.gabrielflorianoo.peqol.config.PEQoLConfig;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * Recipe condition {"type": "peqol:recipe_enabled", "item": "alchemical_anchor"}: false when the item is listed in
 * the disabledRecipes config option.
 */
public record RecipeEnabledCondition(String item) implements ICondition {

    public static final ResourceLocation ID = PEQoL.rl("recipe_enabled");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        try {
            return !PEQoLConfig.COMMON.disabledRecipes.get().contains(item);
        } catch (IllegalStateException configNotLoaded) {
            return true;
        }
    }

    public static final class Serializer implements IConditionSerializer<RecipeEnabledCondition> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, RecipeEnabledCondition value) {
            json.addProperty("item", value.item());
        }

        @Override
        public RecipeEnabledCondition read(JsonObject json) {
            return new RecipeEnabledCondition(GsonHelper.getAsString(json, "item"));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
