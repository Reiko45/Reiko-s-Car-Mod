package com.reiko.cars.compat.jei;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.client.CarWorkbenchScreen;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.workbench.CarAssemblyRecipe;
import com.reiko.cars.workbench.CarWorkbenchMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * JEI support, loaded by JEI only when it's installed (nothing else in the mod touches JEI). Adds a "Car Workbench"
 * recipe page for building cars, with the workbench as the place to make them; makes the arrow in the workbench's
 * screen open that page; and lets JEI's "+" button put a car's parts into the workbench.
 *
 * <p>The car parts themselves are ordinary crafting-table recipes, which JEI shows on its own.
 */
@JeiPlugin
public final class ReikoCarsJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<CarAssemblyRecipe>> CAR_ASSEMBLY = RecipeType.createRecipeHolderType(ReikoCars.id("car_assembly"));

    @Nullable
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ReikoCars.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CarAssemblyCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(CAR_ASSEMBLY, CarWorkbenchMenu.sortedRecipes(level.getRecipeManager()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.CAR_WORKBENCH.get(), CAR_ASSEMBLY);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CarWorkbenchScreen.class, CarWorkbenchScreen.ARROW_X, CarWorkbenchScreen.ARROW_Y,
                CarWorkbenchScreen.ARROW_WIDTH, CarWorkbenchScreen.ARROW_HEIGHT, CAR_ASSEMBLY);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new WorkbenchTransferHandler(registration.getTransferHelper()), CAR_ASSEMBLY);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** JEI's runtime while a world is open, for the dev showcase to open recipe pages. */
    @Nullable
    public static IJeiRuntime runtime() {
        return runtime;
    }
}
