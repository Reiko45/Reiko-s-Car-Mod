package com.reiko.cars.client;

import com.reiko.cars.network.RoadSignActionPayload;
import com.reiko.cars.traffic.RoadSignMenu;
import com.reiko.cars.traffic.TrafficCameras;
import com.reiko.economy.client.screen.ShopScreenBase;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** A traffic light's or speed sign's owner screen: the camera's fine, a sign's limit, and what the camera's taken. */
public final class RoadSignScreen extends ShopScreenBase<RoadSignMenu> {
    private static final int WIDTH = 194;

    private EditBox fineBox;
    private EditBox limitBox;
    private Button unitButton;
    private boolean kmh;

    public RoadSignScreen(RoadSignMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.speedSign() ? 196 : 176);
        this.kmh = menu.kmh();
    }

    @Override
    protected void init() {
        super.init();
        int y = 20;
        if (menu.speedSign()) {
            limitBox = addRenderableWidget(new EditBox(font, leftPos + 96, topPos + y, 40, 16, Component.translatable("gui.reikocars.speed_limit")));
            limitBox.setFilter(text -> text.matches("\\d{0,3}"));
            limitBox.setMaxLength(3);
            limitBox.setValue(Integer.toString(menu.limit()));
            unitButton = addRenderableWidget(Button.builder(unitLabel(), button -> {
                kmh = !kmh;
                button.setMessage(unitLabel());
            }).bounds(leftPos + 140, topPos + y - 1, 46, 18).build());
            y += 22;
        }
        fineBox = addRenderableWidget(new EditBox(font, leftPos + 96, topPos + y, 90, 16, Component.translatable("gui.reikocars.camera_fine")));
        fineBox.setFilter(MONEY_FILTER);
        fineBox.setMaxLength(16);
        fineBox.setValue(String.format(Locale.ROOT, "%d.%02d", menu.fine() / 100, menu.fine() % 100));
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_prices"), button -> set())
                .bounds(leftPos + 8, topPos + y + 22, WIDTH - 16, 20).build());
        setInitialFocus(menu.speedSign() ? limitBox : fineBox);
    }

    private Component unitLabel() {
        return Component.translatable(kmh ? "gui.reikocars.unit_kmh" : "gui.reikocars.unit_mph");
    }

    private void set() {
        PacketDistributor.sendToServer(new RoadSignActionPayload(menu.containerId, fineBox.getValue(),
                limitBox == null ? "" : limitBox.getValue(), kmh));
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
        int y = 24;
        if (menu.speedSign()) {
            graphics.drawString(font, Component.translatable("gui.reikocars.speed_limit"), 8, y, LABEL_COLOR, false);
            y += 22;
        }
        graphics.drawString(font, Component.translatable("gui.reikocars.camera_fine"), 8, y, LABEL_COLOR, false);
        y += 46;
        Component books = Component.translatable("gui.reikocars.camera_books", menu.tickets(), format(menu.collected()));
        graphics.drawString(font, fit(books, imageWidth - 16), 8, y, MUTED_COLOR, false);
        Component hint = menu.speedSign()
                ? Component.translatable("gui.reikocars.speed_sign_hint", (int) TrafficCameras.SPEED_REACH)
                : Component.translatable("gui.reikocars.traffic_light_hint", (int) TrafficCameras.LIGHT_REACH);
        var lines = font.split(hint, imageWidth - 16);
        for (int i = 0; i < Math.min(6, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 8, y + 14 + i * 10, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
