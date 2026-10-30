package com.reiko.cars.client;

import com.reiko.cars.network.WashActionPayload;
import com.reiko.cars.wash.WashOwnerMenu;
import com.reiko.economy.client.screen.ShopScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** A Car Wash's or Wash Bay's owner screen: its price, and what it's taken. */
public final class WashOwnerScreen extends ShopScreenBase<WashOwnerMenu> {
    private static final int WIDTH = 194;

    private EditBox priceBox;

    public WashOwnerScreen(WashOwnerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 150);
    }

    @Override
    protected void init() {
        super.init();
        priceBox = addRenderableWidget(new EditBox(font, leftPos + 60, topPos + 20, 80, 16, Component.translatable("gui.reikocars.price")));
        priceBox.setFilter(MONEY_FILTER);
        priceBox.setMaxLength(16);
        priceBox.setValue(String.format(java.util.Locale.ROOT, "%d.%02d", menu.price() / 100, menu.price() % 100));
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"), button -> set())
                .bounds(leftPos + 144, topPos + 19, 42, 18).build());
    }

    private void set() {
        PacketDistributor.sendToServer(WashActionPayload.set(menu.containerId, priceBox.getValue()));
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
        boolean bay = menu.kind() == WashOwnerMenu.BAY;
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable(bay ? "gui.reikocars.bay_price_label" : "gui.reikocars.wash_price_label"), 8, 24, LABEL_COLOR,
                false);
        Component books = Component.translatable("gui.reikocars.wash_books", menu.washes(), format(menu.earned()));
        graphics.drawString(font, fit(books, imageWidth - 16), 8, 44, MUTED_COLOR, false);
        var hint = font.split(Component.translatable(bay ? "gui.reikocars.bay_owner_hint" : "gui.reikocars.wash_owner_hint"), imageWidth - 16);
        for (int i = 0; i < Math.min(7, hint.size()); i++) {
            graphics.drawString(font, hint.get(i), 8, 58 + i * 10, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
