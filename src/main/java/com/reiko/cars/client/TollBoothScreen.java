package com.reiko.cars.client;

import com.reiko.cars.network.TollActionPayload;
import com.reiko.cars.toll.TollBoothMenu;
import com.reiko.cars.toll.TollService;
import com.reiko.economy.client.screen.ShopScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** A toll booth's owner screen: the toll for its gates, and what they've taken. */
public final class TollBoothScreen extends ShopScreenBase<TollBoothMenu> {
    private static final int WIDTH = 194;

    private EditBox tollBox;

    public TollBoothScreen(TollBoothMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 150);
    }

    @Override
    protected void init() {
        super.init();
        tollBox = addRenderableWidget(new EditBox(font, leftPos + 60, topPos + 20, 80, 16, Component.translatable("gui.reikocars.price")));
        tollBox.setFilter(MONEY_FILTER);
        tollBox.setMaxLength(16);
        tollBox.setValue(String.format(java.util.Locale.ROOT, "%d.%02d", menu.toll() / 100, menu.toll() % 100));
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"), button -> set())
                .bounds(leftPos + 144, topPos + 19, 42, 18).build());
    }

    private void set() {
        PacketDistributor.sendToServer(TollActionPayload.set(menu.containerId, tollBox.getValue()));
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
        graphics.drawString(font, Component.translatable("gui.reikocars.toll_label"), 8, 24, LABEL_COLOR, false);
        Component books = Component.translatable("gui.reikocars.toll_books", menu.cars(), format(menu.earned()));
        graphics.drawString(font, fit(books, imageWidth - 16), 8, 44, MUTED_COLOR, false);
        var hint = font.split(Component.translatable("gui.reikocars.toll_hint", TollService.BOOTH_REACH), imageWidth - 16);
        for (int i = 0; i < Math.min(6, hint.size()); i++) {
            graphics.drawString(font, hint.get(i), 8, 58 + i * 10, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
