package com.reiko.cars.compat.jei;

import com.reiko.cars.registry.ModMenus;
import com.reiko.cars.workbench.CarAssemblyRecipe;
import com.reiko.cars.workbench.CarWorkbenchMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * JEI's "+" button on a car recipe, while the workbench is open. It does what clicking the car's button in the
 * workbench does (picks the car and moves its parts in from the player's inventory), so it uses the workbench's own
 * code rather than JEI's slot-by-slot transfer. When parts are missing, JEI greys the button out and marks them.
 */
final class WorkbenchTransferHandler implements IRecipeTransferHandler<CarWorkbenchMenu, RecipeHolder<CarAssemblyRecipe>> {
    private final IRecipeTransferHandlerHelper helper;

    WorkbenchTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends CarWorkbenchMenu> getContainerClass() {
        return CarWorkbenchMenu.class;
    }

    @Override
    public Optional<MenuType<CarWorkbenchMenu>> getMenuType() {
        return Optional.of(ModMenus.CAR_WORKBENCH.get());
    }

    @Override
    public RecipeType<RecipeHolder<CarAssemblyRecipe>> getRecipeType() {
        return ReikoCarsJeiPlugin.CAR_ASSEMBLY;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(CarWorkbenchMenu menu, RecipeHolder<CarAssemblyRecipe> recipe, IRecipeSlotsView recipeSlots,
            Player player, boolean maxTransfer, boolean doTransfer) {
        int index = menu.indexOf(recipe.id());
        if (index < 0) {
            return helper.createInternalError();
        }
        boolean[] have = menu.partsAvailable(player.getInventory(), recipe.value());
        List<IRecipeSlotView> missing = new ArrayList<>();
        for (int slot = 0; slot < have.length; slot++) {
            if (!have[slot]) {
                recipeSlots.findSlotByName(CarAssemblyCategory.SLOT_NAMES[slot]).ifPresent(missing::add);
            }
        }
        if (!missing.isEmpty()) {
            return helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), missing);
        }
        if (doTransfer) {
            menu.clickMenuButton(player, index);
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, index);
        }
        return null;
    }
}
