package com.reiko.cars.compat.jei;

import com.reiko.cars.car.CarItem;
import com.reiko.cars.car.CarType;
import com.reiko.cars.client.CarPreview;
import com.reiko.cars.client.CarWorkbenchScreen;
import com.reiko.cars.client.CarsClientConfig;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.trailer.TrailerItem;
import com.reiko.cars.workbench.CarAssemblyRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * A car or trailer built at the Car Workbench, laid out like the workbench: the parts in a car shape seen from above
 * (a wheel at each corner, the engine at the front, the chassis in the middle between the body and the interior;
 * a trailer leaves the engine and interior out), the vehicle turning on a stand, and the finished vehicle. A car's
 * seats, top speed and tank size are written underneath, or what a trailer carries.
 */
final class CarAssemblyCategory implements IRecipeCategory<RecipeHolder<CarAssemblyRecipe>> {
    private static final int WIDTH = 190;
    private static final int HEIGHT = 74;
    /** Where each part slot's item sits, in the recipe's slot order: chassis, body, interior, engine, then the wheels. */
    private static final int[][] SLOTS = {
            {21, 21}, {1, 21}, {41, 21}, {21, 1},
            {1, 1}, {41, 1}, {1, 41}, {41, 41}};
    static final String[] SLOT_NAMES = {"chassis", "body", "interior", "engine", "wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr"};
    private static final int PREVIEW_LEFT = 62;
    private static final int PREVIEW_RIGHT = 132;
    private static final int PREVIEW_BOTTOM = 59;
    private static final int ARROW_X = 136;
    private static final int ARROW_Y = 21;
    private static final int OUTPUT_X = 168;
    private static final int OUTPUT_Y = 22;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;
    private final IDrawableStatic arrow;

    CarAssemblyCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.CAR_WORKBENCH.get()));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<CarAssemblyRecipe>> getRecipeType() {
        return ReikoCarsJeiPlugin.CAR_ASSEMBLY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reikocars.car_workbench");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CarAssemblyRecipe> holder, IFocusGroup focuses) {
        CarAssemblyRecipe recipe = holder.value();
        for (int slot = 0; slot < CarAssemblyRecipe.SLOTS; slot++) {
            if (!recipe.needs(slot)) {
                continue; // a trailer has no interior or engine
            }
            Component role = Component.translatable(CarWorkbenchScreen.slotName(recipe, slot)).withStyle(ChatFormatting.GRAY);
            builder.addSlot(RecipeIngredientRole.INPUT, SLOTS[slot][0], SLOTS[slot][1])
                    .setStandardSlotBackground()
                    .setSlotName(SLOT_NAMES[slot])
                    .addIngredients(recipe.ingredient(slot))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(role));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .setBackground(guiHelper.getOutputSlot(), -5, -5)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<CarAssemblyRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        // The car's frame behind the slots: the two axles and the spine joining them.
        int line = 0xFF8B8B8B;
        graphics.fill(17, 8, 41, 10, line);
        graphics.fill(17, 48, 41, 50, line);
        graphics.fill(28, 18, 30, 49, line);

        graphics.fill(PREVIEW_LEFT - 1, 0, PREVIEW_RIGHT + 1, PREVIEW_BOTTOM + 1, 0xFF373737);
        graphics.fillGradient(PREVIEW_LEFT, 1, PREVIEW_RIGHT, PREVIEW_BOTTOM, 0xFF34465A, 0xFF161D26);
        arrow.draw(graphics, ARROW_X, ARROW_Y);

        ItemStack result = holder.value().result();
        Font font = Minecraft.getInstance().font;
        if (result.getItem() instanceof CarItem car) {
            CarType type = car.carType();
            float yaw = 215.0F + (Util.getMillis() % 12_000L) / 12_000.0F * 360.0F;
            CarPreview.draw(graphics, type, (PREVIEW_LEFT + PREVIEW_RIGHT) / 2.0F, PREVIEW_BOTTOM / 2.0F + 2.0F, 14.0F, yaw, CarPreview.Fitted.all());
            CarsClientConfig.SpeedUnit unit = CarsClientConfig.speedUnit();
            long speed = Math.round(type.topSpeed * CarsConfig.get(CarsConfig.SPEED) * unit.perBlockPerTick);
            Component stats = type.isElectric()
                    ? Component.translatable("gui.reikocars.workbench.jei_stats_ev", type.seatsText(), speed,
                            Component.translatable(unit.translationKey), Math.round(type.tankCapacity() / 1000.0))
                    : Component.translatable("gui.reikocars.workbench.jei_stats", type.seatsText(), speed,
                            Component.translatable(unit.translationKey), type.tankBuckets);
            graphics.drawString(font, stats, (WIDTH - font.width(stats)) / 2, HEIGHT - 9, 0xFF606060, false);
        } else if (result.getItem() instanceof TrailerItem trailer) {
            float yaw = 215.0F + (Util.getMillis() % 12_000L) / 12_000.0F * 360.0F;
            CarPreview.draw(graphics, trailer.trailerType(), (PREVIEW_LEFT + PREVIEW_RIGHT) / 2.0F, PREVIEW_BOTTOM / 2.0F + 2.0F, 14.0F, yaw,
                    CarPreview.Fitted.all());
            Component stats = CarWorkbenchScreen.trailerStats(trailer.trailerType());
            graphics.drawString(font, stats, (WIDTH - font.width(stats)) / 2, HEIGHT - 9, 0xFF606060, false);
        } else {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate((PREVIEW_LEFT + PREVIEW_RIGHT) / 2.0F - 16.0F, PREVIEW_BOTTOM / 2.0F - 16.0F, 0.0F);
            pose.scale(2.0F, 2.0F, 1.0F);
            graphics.renderItem(result, 0, 0);
            pose.popPose();
        }
    }
}
