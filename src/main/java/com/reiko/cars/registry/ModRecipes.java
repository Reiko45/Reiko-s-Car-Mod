package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.workbench.CarAssemblyRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ReikoCars.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ReikoCars.MODID);

    /** Cars put together at the Car Workbench. */
    public static final Supplier<RecipeType<CarAssemblyRecipe>> CAR_ASSEMBLY = TYPES.register("car_assembly",
            () -> RecipeType.simple(ReikoCars.id("car_assembly")));
    public static final Supplier<RecipeSerializer<CarAssemblyRecipe>> CAR_ASSEMBLY_SERIALIZER = SERIALIZERS.register("car_assembly",
            CarAssemblyRecipe.Serializer::new);

    private ModRecipes() {
    }
}
