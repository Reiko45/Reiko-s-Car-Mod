package com.reiko.cars.client;

import com.reiko.cars.road.PaverMenu;
import com.reiko.cars.road.PaverSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * A road paver's screen: the hopper and the player's inventory on the left, and the settings down the right, each with
 * arrows to step it.
 */
public final class PaverScreen extends AbstractContainerScreen<PaverMenu> {
    private static final int MAIN_WIDTH = 176;
    private static final int PANEL_WIDTH = 160;
    private static final int ROW_TOP = 18;
    private static final int ROW_HEIGHT = 13;
    private static final int LABEL_COLOR = 0x404040;
    /** Where the arrows and the value go, from the panel's left. */
    private static final int BACK_X = 92;
    private static final int FORWARD_X = 146;

    public PaverScreen(PaverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = MAIN_WIDTH + PANEL_WIDTH;
        this.imageHeight = PaverMenu.inventoryTop() + 82;
        this.inventoryLabelY = PaverMenu.inventoryTop() - 11;
    }

    @Override
    protected void init() {
        super.init();
        int panel = leftPos + MAIN_WIDTH;
        for (int setting = 0; setting < PaverSettings.COUNT; setting++) {
            int y = topPos + ROW_TOP + setting * ROW_HEIGHT;
            int id = setting * 2;
            Button back = addRenderableWidget(Button.builder(Component.literal("<"), button -> click(id)).bounds(panel + BACK_X, y - 1, 10, 11).build());
            Button forward = addRenderableWidget(Button.builder(Component.literal(">"), button -> click(id + 1))
                    .bounds(panel + FORWARD_X, y - 1, 10, 11).build());
            Tooltip hint = Tooltip.create(Component.translatable("gui.reikocars.paver.hint." + PaverSettings.key(setting)));
            back.setTooltip(hint);
            forward.setTooltip(hint);
        }
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        panel(graphics, x, y, MAIN_WIDTH, imageHeight);
        panel(graphics, x + MAIN_WIDTH - 2, y, PANEL_WIDTH + 2, imageHeight);
        for (int row = 0; row < PaverMenu.ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                slot(graphics, x + PaverMenu.LEFT + col * 18, y + PaverMenu.TOP + row * 18);
            }
        }
        int top = y + PaverMenu.inventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(graphics, x + PaverMenu.LEFT + col * 18, top + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(graphics, x + PaverMenu.LEFT + col * 18, top + 58);
        }
        // the values' wells
        int panelX = x + MAIN_WIDTH;
        for (int setting = 0; setting < PaverSettings.COUNT; setting++) {
            int rowY = y + ROW_TOP + setting * ROW_HEIGHT;
            graphics.fill(panelX + BACK_X + 11, rowY - 1, panelX + FORWARD_X - 1, rowY + 10, 0xFF8B8B8B);
            graphics.fill(panelX + BACK_X + 12, rowY, panelX + FORWARD_X - 2, rowY + 9, 0xFF2F2F33);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        PaverSettings settings = menu.settings();
        int panelX = MAIN_WIDTH;
        graphics.drawString(font, Component.translatable("gui.reikocars.paver.road"), panelX + 6, 6, LABEL_COLOR, false);
        for (int setting = 0; setting < PaverSettings.COUNT; setting++) {
            int y = ROW_TOP + setting * ROW_HEIGHT + 1;
            graphics.drawString(font, PaverSettings.label(setting), panelX + 6, y, LABEL_COLOR, false);
            Component value = settings.valueText(setting);
            int middle = panelX + (BACK_X + 11 + FORWARD_X - 1) / 2;
            int width = font.width(value);
            int room = FORWARD_X - BACK_X - 14;
            if (width > room) {
                graphics.pose().pushPose();
                float scale = room / (float) width;
                graphics.pose().translate(middle - room / 2.0F, y + (1.0F - scale) * 4.0F, 0.0F);
                graphics.pose().scale(scale, scale, 1.0F);
                graphics.drawString(font, value, 0, 0, 0xFFE082, false);
                graphics.pose().popPose();
            } else {
                graphics.drawString(font, value, middle - width / 2, y, 0xFFE082, false);
            }
        }
        // Paving or not, and what this run's cost so far.
        int statusY = ROW_TOP + PaverSettings.COUNT * ROW_HEIGHT + 4;
        Component status = Component.translatable(menu.paving() ? "gui.reikocars.paver.paving" : "gui.reikocars.paver.raised");
        graphics.drawString(font, status, panelX + 6, statusY, menu.paving() ? 0x2E7D32 : 0x6A6A6A, false);
        if (menu.spent() > 0) {
            Component spent = Component.translatable("gui.reikocars.paver.spent", com.reiko.economy.api.EconomyApi.get().format(menu.spent()));
            graphics.drawString(font, spent, panelX + PANEL_WIDTH - 6 - font.width(spent), statusY, LABEL_COLOR, false);
        }
    }

    // ---- drawing, in the economy mod's look ----

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + width - 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 3, y + height - 2, 0xFFFFFFFF);
        graphics.fill(x + 2, y + height - 3, x + width - 1, y + height - 1, 0xFF555555);
        graphics.fill(x + width - 3, y + 2, x + width - 1, y + height - 1, 0xFF555555);
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        int x0 = x - 1;
        int y0 = y - 1;
        int x1 = x + 17;
        int y1 = y + 17;
        graphics.fill(x0, y0, x1, y1, 0xFF8B8B8B);
        graphics.fill(x0, y0, x1, y0 + 1, 0xFF373737);
        graphics.fill(x0, y0, x0 + 1, y1, 0xFF373737);
        graphics.fill(x0 + 1, y1 - 1, x1, y1, 0xFFFFFFFF);
        graphics.fill(x1 - 1, y0 + 1, x1, y1, 0xFFFFFFFF);
    }
}
