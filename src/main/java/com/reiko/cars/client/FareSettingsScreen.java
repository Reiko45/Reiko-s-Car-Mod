package com.reiko.cars.client;

import com.reiko.cars.fare.FareSettingsMenu;
import com.reiko.cars.network.FareActionPayload;
import com.reiko.economy.client.screen.ShopScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** The owner's fare screen for a city bus (one fare) or a taxi (a starting fare and a rate per 100 blocks). */
public final class FareSettingsScreen extends ShopScreenBase<FareSettingsMenu> {
    private static final int WIDTH = 194;
    private static final int BOX_X = 112;
    private static final int ROW = 20;

    private EditBox startBox;
    private EditBox per100Box;

    public FareSettingsScreen(FareSettingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 162);
    }

    private int setRow() {
        return menu.isMetered() ? ROW + 44 : ROW + 22;
    }

    @Override
    protected void init() {
        super.init();
        startBox = addRenderableWidget(new EditBox(font, leftPos + BOX_X, topPos + ROW, 74, 16, Component.translatable("gui.reikocars.price")));
        startBox.setFilter(MONEY_FILTER);
        startBox.setMaxLength(16);
        startBox.setValue(plain(menu.start()));
        per100Box = addRenderableWidget(new EditBox(font, leftPos + BOX_X, topPos + ROW + 22, 74, 16, Component.translatable("gui.reikocars.price")));
        per100Box.setFilter(MONEY_FILTER);
        per100Box.setMaxLength(16);
        per100Box.setValue(plain(menu.per100()));
        per100Box.visible = menu.isMetered();
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_fares"), button -> set())
                .bounds(leftPos + 8, topPos + setRow(), WIDTH - 16, 20).build());
    }

    /** 200 cents as "2.00", for the boxes. */
    private static String plain(long cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void set() {
        PacketDistributor.sendToServer(FareActionPayload.set(menu.containerId, startBox.getValue(), per100Box.getValue()));
    }

    @Override
    protected void onEnter() {
        set();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        if (menu.isMetered()) {
            graphics.drawString(font, Component.translatable("gui.reikocars.fare_start"), 8, ROW + 4, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.reikocars.fare_per_100"), 8, ROW + 26, LABEL_COLOR, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.reikocars.fare_per_ride"), 8, ROW + 4, LABEL_COLOR, false);
        }
        int y = setRow() + 26;
        Component earned = Component.translatable("gui.reikocars.fares_earned", format(menu.earned()), menu.rides());
        graphics.drawString(font, fit(earned, imageWidth - 16), 8, y, MUTED_COLOR, false);
        Component hint = Component.translatable(menu.isMetered() ? "gui.reikocars.fares_hint_taxi" : "gui.reikocars.fares_hint_bus");
        var lines = font.split(hint, imageWidth - 16);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 8, y + 12 + i * 10, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
