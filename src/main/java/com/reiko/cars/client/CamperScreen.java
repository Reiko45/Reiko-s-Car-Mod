package com.reiko.cars.client;

import com.reiko.cars.trailer.CamperMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Inside a camper: the cupboards along the top, the stove (food above the flame, fuel below it, an arrow to what's
 * cooked), a Sleep button, and the player's inventory.
 */
public final class CamperScreen extends AbstractContainerScreen<CamperMenu> {
    private static final int WIDTH = 176;
    private static final int LABEL_COLOR = 0x404040;

    public CamperScreen(CamperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = menu.inventoryTop() + 82;
        this.inventoryLabelY = menu.inventoryTop() - 11;
    }

    @Override
    protected void init() {
        super.init();
        Button sleep = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.camper.sleep"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CamperMenu.SLEEP);
            }
        }).bounds(leftPos + 126, topPos + menu.stoveTop() + 16, 42, 20).build());
        sleep.setTooltip(Tooltip.create(Component.translatable("gui.reikocars.camper.sleep.hint")));
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
        panel(graphics, x, y, imageWidth, imageHeight);
        for (int row = 0; row < menu.rows(); row++) {
            for (int col = 0; col < 9; col++) {
                slot(graphics, x + CamperMenu.LEFT + col * 18, y + CamperMenu.TOP + row * 18);
            }
        }
        int top = y + menu.stoveTop();
        // the stove: a dark hob behind the slots
        graphics.fill(x + 34, top - 4, x + 124, top + 56, 0xFF9A9A9A);
        graphics.fill(x + 35, top - 3, x + 123, top + 55, 0xFF7A7A7E);
        slot(graphics, x + 44, top);
        slot(graphics, x + 44, top + 36);
        slot(graphics, x + 100, top + 14, 4);
        flame(graphics, x + 45, top + 20, menu.flame());
        arrow(graphics, x + 68, top + 19, menu.cooking());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(graphics, x + CamperMenu.LEFT + col * 18, y + menu.inventoryTop() + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(graphics, x + CamperMenu.LEFT + col * 18, y + menu.inventoryTop() + 58);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int top = menu.stoveTop();
        graphics.drawString(font, Component.translatable("gui.reikocars.camper.stove"), 8, top - 13, LABEL_COLOR, false);
        Component status = Component.translatable(menu.parked() ? "gui.reikocars.camper.parked" : "gui.reikocars.camper.hitched");
        graphics.drawString(font, status, imageWidth - 8 - font.width(status), top - 13, menu.parked() ? 0x2E7D32 : 0x9E3A2E, false);
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
        slot(graphics, x, y, 0);
    }

    private static void slot(GuiGraphics graphics, int x, int y, int pad) {
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

    /** A gas flame 14 pixels high, lit up to {@code amount} of it from the bottom. */
    private static void flame(GuiGraphics graphics, int x, int y, float amount) {
        // rows of the flame's outline, widest at the bottom
        int[][] rows = {{6, 8}, {5, 9}, {5, 9}, {4, 10}, {4, 10}, {3, 11}, {3, 11}, {2, 12}, {2, 12}, {2, 12}, {3, 11}, {3, 11}, {4, 10}, {5, 9}};
        int lit = Math.round(amount * rows.length);
        for (int i = 0; i < rows.length; i++) {
            boolean on = i >= rows.length - lit;
            int color = on ? (i > 9 ? 0xFF3D7BFF : i > 5 ? 0xFFFFB21E : 0xFFFF6A1E) : 0xFF5E5E62;
            graphics.fill(x + rows[i][0], y + i, x + rows[i][1], y + i + 1, color);
        }
    }

    /** The cooking arrow, 22 by 15, filled to {@code amount}. */
    private static void arrow(GuiGraphics graphics, int x, int y, float amount) {
        int filled = Math.round(amount * 22);
        for (int i = 0; i < 22; i++) {
            int color = i < filled ? 0xFFFFFFFF : 0xFF5E5E62;
            if (i < 14) {
                graphics.fill(x + i, y + 5, x + i + 1, y + 10, color);
            } else {
                int k = i - 14;
                graphics.fill(x + i, y + k, x + i + 1, y + 15 - k, color);
            }
        }
    }
}
