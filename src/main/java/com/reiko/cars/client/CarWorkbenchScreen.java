package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.car.CarType;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.workbench.CarAssemblyRecipe;
import com.reiko.cars.workbench.CarWorkbenchMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The Car Workbench screen. Along the top, a button for each car it can build: clicking one shows that car's parts
 * in the empty slots and puts in any the player carries. Below, the part slots, laid out like the car seen from
 * above, and a turning 3D model of the car, with each part drawn as a blue ghost until it's fitted. Drag the model
 * to turn it.
 */
public final class CarWorkbenchScreen extends AbstractContainerScreen<CarWorkbenchMenu> {
    private static final int WIDTH = 236;
    private static final int HEIGHT = 212;
    private static final int BUTTON_X = 8;
    private static final int BUTTON_Y = 17;
    private static final int BUTTON_SIZE = 22;
    private static final int BUTTON_STEP = 24;
    private static final int PER_PAGE = 8;
    private static final int PREVIEW_LEFT = 82;
    private static final int PREVIEW_TOP = 43;
    private static final int PREVIEW_RIGHT = 180;
    private static final int PREVIEW_BOTTOM = 113;
    /** Where the arrow is, which JEI makes a button for showing every workbench recipe. */
    public static final int ARROW_X = 184;
    public static final int ARROW_Y = 71;
    public static final int ARROW_WIDTH = 22;
    public static final int ARROW_HEIGHT = 15;

    private static final int LABEL_COLOR = 0x404040;
    private static final int GOLD = 0xFFFFC83D;
    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final String[] SLOT_NAMES = {"chassis", "body", "interior", "engine", "wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr"};

    private int page;
    private int shownSelection = -1;
    private Button previousPage;
    private Button nextPage;
    /** The preview's heading in degrees, turning slowly unless the player is looking at or dragging it. */
    private float yaw = 215.0F;
    private long lastFrame = Util.getMillis();
    private boolean dragging;

    public CarWorkbenchScreen(CarWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelX = CarWorkbenchMenu.INVENTORY_X;
        this.inventoryLabelY = CarWorkbenchMenu.INVENTORY_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        int arrows = leftPos + BUTTON_X + PER_PAGE * BUTTON_STEP + 2;
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), button -> turnPage(-1))
                .bounds(arrows, topPos + BUTTON_Y, 12, BUTTON_SIZE).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), button -> turnPage(1))
                .bounds(arrows + 14, topPos + BUTTON_Y, 12, BUTTON_SIZE).build());
        updatePageButtons();
    }

    private int pages() {
        return Math.max(1, (menu.recipes().size() + PER_PAGE - 1) / PER_PAGE);
    }

    private void turnPage(int direction) {
        page = Math.floorMod(page + direction, pages());
    }

    private void updatePageButtons() {
        boolean paged = pages() > 1;
        previousPage.visible = nextPage.visible = paged;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Follow the selection when the server changes it (the parts in the slots make a different car).
        int selected = menu.selectedIndex();
        if (selected != shownSelection) {
            shownSelection = selected;
            if (selected >= 0) {
                page = selected / PER_PAGE;
            }
        }
        updatePageButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    // ---- background ----

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        drawPanel(graphics, x, y, imageWidth, imageHeight);
        drawCarButtons(graphics, mouseX, mouseY);
        drawFrame(graphics);
        Optional<CarAssemblyRecipe> recipe = menu.selectedRecipe();
        for (int slot = 0; slot < CarWorkbenchMenu.PARTS; slot++) {
            int sx = x + CarWorkbenchMenu.PART_POSITIONS[slot][0];
            int sy = y + CarWorkbenchMenu.PART_POSITIONS[slot][1];
            drawSlot(graphics, sx, sy, 0);
            if (recipe.isPresent() && !menu.getSlot(slot).hasItem()) {
                ItemStack ghost = cycle(recipe.get().ingredient(slot));
                if (!ghost.isEmpty()) {
                    graphics.renderFakeItem(ghost, sx, sy);
                    graphics.fill(RenderType.guiGhostRecipeOverlay(), sx, sy, sx + 16, sy + 16, 0x30FFFFFF);
                }
            }
        }
        drawPreview(graphics, recipe, mouseX, mouseY);
        drawArrow(graphics, x + ARROW_X, y + ARROW_Y, menu.getSlot(CarWorkbenchMenu.RESULT).hasItem() ? 0xFF3FA34D : 0xFF8B8B8B);
        drawSlot(graphics, x + CarWorkbenchMenu.RESULT_X, y + CarWorkbenchMenu.RESULT_Y, 4);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + CarWorkbenchMenu.INVENTORY_X + col * 18, y + CarWorkbenchMenu.INVENTORY_Y + row * 18, 0);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + CarWorkbenchMenu.INVENTORY_X + col * 18, y + CarWorkbenchMenu.INVENTORY_Y + 58, 0);
        }
    }

    /** One of an ingredient's items, changing once a second when it accepts several (a tag). */
    private static ItemStack cycle(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[(int) (Util.getMillis() / 1000L % items.length)];
    }

    private int buttonX(int indexOnPage) {
        return leftPos + BUTTON_X + indexOnPage * BUTTON_STEP;
    }

    /** The car button under the mouse, as an index into the menu's recipe list, or -1. */
    private int buttonAt(double mouseX, double mouseY) {
        if (mouseY < topPos + BUTTON_Y || mouseY >= topPos + BUTTON_Y + BUTTON_SIZE) {
            return -1;
        }
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= menu.recipes().size()) {
                break;
            }
            int bx = buttonX(i);
            if (mouseX >= bx && mouseX < bx + BUTTON_SIZE) {
                return index;
            }
        }
        return -1;
    }

    private void drawCarButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        List<RecipeHolder<CarAssemblyRecipe>> recipes = menu.recipes();
        int hovered = buttonAt(mouseX, mouseY);
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= recipes.size()) {
                break;
            }
            int bx = buttonX(i);
            int by = topPos + BUTTON_Y;
            CarAssemblyRecipe recipe = recipes.get(index).value();
            graphics.blitSprite(index == hovered ? BUTTON_HIGHLIGHTED : BUTTON, bx, by, BUTTON_SIZE, BUTTON_SIZE);
            if (index == menu.selectedIndex()) {
                graphics.renderOutline(bx - 1, by - 1, BUTTON_SIZE + 2, BUTTON_SIZE + 2, GOLD);
            }
            graphics.renderItem(recipe.result(), bx + 3, by + 3);
            if (allPartsAvailable(recipe)) {
                drawTick(graphics, bx + BUTTON_SIZE - 7, by + BUTTON_SIZE - 7);
            }
        }
    }

    /** A small green tick in the corner of a car the player carries every part for. */
    private static void drawTick(GuiGraphics graphics, int x, int y) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 250.0F);
        int dark = 0xFF0F3D14;
        int green = 0xFF4CD964;
        graphics.fill(x - 1, y + 1, x + 6, y + 5, dark);
        graphics.fill(x + 1, y + 4, x + 3, y + 6, dark);
        graphics.fill(x + 4, y - 1, x + 6, y + 2, dark);
        graphics.fill(x, y + 2, x + 1, y + 3, green);
        graphics.fill(x + 1, y + 3, x + 2, y + 4, green);
        graphics.fill(x + 2, y + 4, x + 3, y + 5, green);
        graphics.fill(x + 3, y + 3, x + 4, y + 4, green);
        graphics.fill(x + 4, y + 1, x + 5, y + 3, green);
        graphics.fill(x + 5, y, x + 6, y + 1, green);
        pose.popPose();
    }

    /** The car's outline behind the part slots: the axles between the wheels and the spine through the middle. */
    private void drawFrame(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos;
        int dark = 0xFF8B8B8B;
        int[][] at = CarWorkbenchMenu.PART_POSITIONS;
        graphics.fill(x + 7, y + 41, x + 79, y + 112, 0xFFB4B4B4);
        graphics.renderOutline(x + 7, y + 41, 72, 71, dark);
        int front = y + at[CarAssemblyRecipe.FIRST_WHEEL][1] + 7;
        int rear = y + at[CarAssemblyRecipe.FIRST_WHEEL + 2][1] + 7;
        int left = x + at[CarAssemblyRecipe.FIRST_WHEEL][0] + 16;
        int right = x + at[CarAssemblyRecipe.FIRST_WHEEL + 1][0];
        int spine = x + at[CarAssemblyRecipe.CHASSIS][0] + 7;
        graphics.fill(left, front, right, front + 2, dark);
        graphics.fill(left, rear, right, rear + 2, dark);
        graphics.fill(spine, front, spine + 2, rear + 2, dark);
    }

    private void drawPreview(GuiGraphics graphics, Optional<CarAssemblyRecipe> recipe, int mouseX, int mouseY) {
        int x0 = leftPos + PREVIEW_LEFT;
        int y0 = topPos + PREVIEW_TOP;
        int x1 = leftPos + PREVIEW_RIGHT;
        int y1 = topPos + PREVIEW_BOTTOM;
        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF373737);
        graphics.fillGradient(x0, y0, x1, y1, 0xFF34465A, 0xFF161D26);
        graphics.fill(x0, y1 - 1, x1 + 1, y1 + 1, 0xFFFFFFFF);
        graphics.fill(x1, y0, x1 + 1, y1 + 1, 0xFFFFFFFF);

        long now = Util.getMillis();
        float seconds = Math.min(0.25F, (now - lastFrame) / 1000.0F);
        lastFrame = now;
        boolean over = mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
        if (!dragging && !over) {
            yaw = (yaw + seconds * 30.0F) % 360.0F;
        }
        if (recipe.isEmpty()) {
            return;
        }
        ItemStack result = recipe.get().result();
        graphics.enableScissor(x0, y0, x1, y1);
        if (result.getItem() instanceof CarItem car) {
            drawCar(graphics, car.carType(), recipe.get(), (x0 + x1) / 2.0F, (y0 + y1) / 2.0F + 1.0F);
        } else {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate((x0 + x1) / 2.0F - 24.0F, (y0 + y1) / 2.0F - 24.0F, 0.0F);
            pose.scale(3.0F, 3.0F, 1.0F);
            graphics.renderItem(result, 0, 0);
            pose.popPose();
        }
        graphics.disableScissor();
    }

    /** Whether part slot {@code slot} holds what {@code recipe} wants there. */
    private boolean fitted(CarAssemblyRecipe recipe, int slot) {
        ItemStack stack = menu.getSlot(slot).getItem();
        return !stack.isEmpty() && recipe.ingredient(slot).test(stack);
    }

    /** The car with its fitted parts drawn as they look in the world, the rest as ghosts. */
    private void drawCar(GuiGraphics graphics, CarType type, CarAssemblyRecipe recipe, float cx, float cy) {
        boolean[] wheels = new boolean[4];
        for (int wheel = 0; wheel < 4; wheel++) {
            wheels[wheel] = fitted(recipe, CarAssemblyRecipe.FIRST_WHEEL + wheel);
        }
        CarPreview.draw(graphics, type, cx, cy, 18.0F, yaw, new CarPreview.Fitted(
                fitted(recipe, CarAssemblyRecipe.BODY), fitted(recipe, CarAssemblyRecipe.INTERIOR), wheels));
    }

    // ---- labels and tooltips ----

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
        Optional<CarAssemblyRecipe> recipe = menu.selectedRecipe();
        if (recipe.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.reikocars.workbench.no_cars"),
                    (PREVIEW_LEFT + PREVIEW_RIGHT) / 2, PREVIEW_TOP + 30, 0xFFFFFF);
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 300.0F);
        int middle = (PREVIEW_LEFT + PREVIEW_RIGHT) / 2;
        ItemStack result = recipe.get().result();
        graphics.drawCenteredString(font, result.getHoverName(), middle, PREVIEW_TOP + 4, 0xFFFFFF);
        if (result.getItem() instanceof CarItem car) {
            CarType type = car.carType();
            CarsClientConfig.SpeedUnit unit = CarsClientConfig.speedUnit();
            long speed = Math.round(type.topSpeed * CarsConfig.get(CarsConfig.SPEED) * unit.perBlockPerTick);
            graphics.drawCenteredString(font, Component.translatable("gui.reikocars.workbench.stats", type.seatCount(), speed,
                    Component.translatable(unit.translationKey)), middle, PREVIEW_BOTTOM - 11, 0xB8C4D0);
        }
        pose.popPose();
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        int button = buttonAt(mouseX, mouseY);
        if (button >= 0) {
            graphics.renderComponentTooltip(font, carTooltip(menu.recipes().get(button).value()), mouseX, mouseY);
            return;
        }
        Slot slot = hoveredSlot;
        if (slot != null && slot.index < CarWorkbenchMenu.PARTS && !slot.hasItem()) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.reikocars.workbench.slot." + SLOT_NAMES[slot.index]));
            menu.selectedRecipe().ifPresent(recipe -> {
                ItemStack wanted = cycle(recipe.ingredient(slot.index));
                if (!wanted.isEmpty()) {
                    lines.add(Component.translatable("gui.reikocars.workbench.needs", wanted.getHoverName()).withStyle(ChatFormatting.GRAY));
                }
            });
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    /** The car's name, then each part with a tick or a cross for whether the player has it. */
    private List<Component> carTooltip(CarAssemblyRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(recipe.result().getHoverName());
        boolean[] have = menu.partsAvailable(minecraft.player.getInventory(), recipe);
        for (int slot = 0; slot < CarAssemblyRecipe.FIRST_WHEEL; slot++) {
            lines.add(partLine(cycle(recipe.ingredient(slot)).getHoverName(), have[slot], 1, have[slot] ? 1 : 0));
        }
        int wheels = 0;
        for (int slot = CarAssemblyRecipe.FIRST_WHEEL; slot < CarAssemblyRecipe.SLOTS; slot++) {
            wheels += have[slot] ? 1 : 0;
        }
        int needed = CarAssemblyRecipe.SLOTS - CarAssemblyRecipe.FIRST_WHEEL;
        lines.add(partLine(cycle(recipe.ingredient(CarAssemblyRecipe.FIRST_WHEEL)).getHoverName(), wheels == needed, needed, wheels));
        lines.add(Component.translatable("gui.reikocars.workbench.click_to_load").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static Component partLine(Component name, boolean have, int needed, int carried) {
        Component text = needed == 1 ? name
                : have ? Component.translatable("gui.reikocars.workbench.count", needed, name)
                : Component.translatable("gui.reikocars.workbench.count_short", name, carried, needed);
        return Component.literal(have ? "✔ " : "✘ ").append(text).withStyle(have ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private boolean allPartsAvailable(CarAssemblyRecipe recipe) {
        for (boolean have : menu.partsAvailable(minecraft.player.getInventory(), recipe)) {
            if (!have) {
                return false;
            }
        }
        return true;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int car = buttonAt(mouseX, mouseY);
        if (car >= 0 && button == 0) {
            if (menu.clickMenuButton(minecraft.player, car)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, car);
            }
            return true;
        }
        if (button == 0 && mouseX >= leftPos + PREVIEW_LEFT && mouseX < leftPos + PREVIEW_RIGHT
                && mouseY >= topPos + PREVIEW_TOP && mouseY < topPos + PREVIEW_BOTTOM) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            yaw = (float) ((yaw - dragX * 2.0) % 360.0);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pages() > 1 && mouseY >= topPos + BUTTON_Y && mouseY < topPos + BUTTON_Y + BUTTON_SIZE) {
            turnPage(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---- drawing helpers, in screen coordinates (the same look as the economy mod's screens) ----

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + width - 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 3, y + height - 2, 0xFFFFFFFF);
        graphics.fill(x + 2, y + height - 3, x + width - 1, y + height - 1, 0xFF555555);
        graphics.fill(x + width - 3, y + 2, x + width - 1, y + height - 1, 0xFF555555);
    }

    /** A sunken slot for an item drawn at (x, y), {@code pad} pixels bigger all round than a normal one. */
    private static void drawSlot(GuiGraphics graphics, int x, int y, int pad) {
        int x0 = x - 1 - pad;
        int y0 = y - 1 - pad;
        int x1 = x + 17 + pad;
        int y1 = y + 17 + pad;
        graphics.fill(x0, y0, x1, y1, 0xFF8B8B8B);
        graphics.fill(x0, y0, x1, y0 + 1, 0xFF373737);
        graphics.fill(x0, y0, x0 + 1, y1, 0xFF373737);
        graphics.fill(x0 + 1, y1 - 1, x1, y1, 0xFFFFFFFF);
        graphics.fill(x1 - 1, y0 + 1, x1, y1, 0xFFFFFFFF);
    }

    /** A right-pointing arrow, like the crafting table's, 22 by 15 pixels. */
    private static void drawArrow(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 5, x + 14, y + 10, color);
        for (int i = 0; i < 8; i++) {
            graphics.fill(x + 14 + i, y + i, x + 15 + i, y + 15 - i, color);
        }
    }
}
