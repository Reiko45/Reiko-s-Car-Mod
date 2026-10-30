package com.reiko.cars.client;

import com.reiko.cars.bus.BusStopBlockEntity;
import com.reiko.cars.bus.BusStopMenu;
import com.reiko.cars.network.BusStopActionPayload;
import com.reiko.economy.client.screen.ShopScreenBase;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** A bus stop's owner screen: its name, and up to three routes with its number along each. */
public final class BusStopScreen extends ShopScreenBase<BusStopMenu> {
    private static final int WIDTH = 194;
    private static final int NAME_Y = 20;
    private static final int ROWS_Y = 54;
    private static final int ROW = 20;
    private static final int ROUTE_X = 40;
    private static final int NUMBER_X = 118;

    private EditBox nameBox;
    private final List<EditBox> routeBoxes = new ArrayList<>();
    private final List<EditBox> numberBoxes = new ArrayList<>();

    public BusStopScreen(BusStopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 214);
    }

    @Override
    protected void init() {
        super.init();
        nameBox = addRenderableWidget(new EditBox(font, leftPos + 50, topPos + NAME_Y, WIDTH - 58, 16, Component.translatable("gui.reikocars.stop_name")));
        nameBox.setMaxLength(BusStopBlockEntity.MAX_NAME);
        nameBox.setValue(menu.name());
        routeBoxes.clear();
        numberBoxes.clear();
        for (int i = 0; i < BusStopBlockEntity.MAX_ROUTES; i++) {
            BusStopBlockEntity.RouteStop route = i < menu.routes().size() ? menu.routes().get(i) : null;
            int y = topPos + ROWS_Y + i * ROW;
            EditBox routeBox = addRenderableWidget(new EditBox(font, leftPos + ROUTE_X, y, 40, 16, Component.translatable("gui.reikocars.route")));
            routeBox.setFilter(text -> text.matches("\\d{0,2}"));
            routeBox.setMaxLength(2);
            routeBox.setValue(route == null ? "" : Integer.toString(route.route()));
            EditBox numberBox = addRenderableWidget(new EditBox(font, leftPos + NUMBER_X, y, 40, 16, Component.translatable("gui.reikocars.stop_number")));
            numberBox.setFilter(text -> text.matches("\\d{0,2}"));
            numberBox.setMaxLength(2);
            numberBox.setValue(route == null ? "" : Integer.toString(route.number()));
            routeBoxes.add(routeBox);
            numberBoxes.add(numberBox);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.save"), button -> save())
                .bounds(leftPos + 8, topPos + ROWS_Y + BusStopBlockEntity.MAX_ROUTES * ROW + 4, WIDTH - 16, 20).build());
        setInitialFocus(nameBox);
    }

    private static int number(EditBox box) {
        String text = box.getValue().trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    private void save() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < routeBoxes.size(); i++) {
            numbers.add(number(routeBoxes.get(i)));
            numbers.add(number(numberBoxes.get(i)));
        }
        PacketDistributor.sendToServer(new BusStopActionPayload(menu.containerId, nameBox.getValue(), numbers));
    }

    @Override
    protected void onEnter() {
        save();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.reikocars.stop_name"), 8, NAME_Y + 4, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.reikocars.route"), ROUTE_X, ROWS_Y - 11, MUTED_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.reikocars.stop_number"), NUMBER_X, ROWS_Y - 11, MUTED_COLOR, false);
        for (int i = 0; i < BusStopBlockEntity.MAX_ROUTES; i++) {
            // "Route [ 1 ]  stop no. [ 2 ]"
            graphics.drawString(font, "#", ROUTE_X - 9, ROWS_Y + i * ROW + 4, MUTED_COLOR, false);
            graphics.drawString(font, "#", NUMBER_X - 9, ROWS_Y + i * ROW + 4, MUTED_COLOR, false);
        }
        int y = ROWS_Y + BusStopBlockEntity.MAX_ROUTES * ROW + 30;
        var lines = font.split(Component.translatable("gui.reikocars.bus_stop_hint"), imageWidth - 16);
        for (int i = 0; i < Math.min(6, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 8, y + i * 10, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
