package com.reiko.cars.client;

import com.reiko.cars.garage.GarageDoorBlock;
import com.reiko.cars.garage.GarageMenu;
import com.reiko.cars.garage.GarageSettings;
import com.reiko.cars.network.GarageActionPayload;
import com.reiko.economy.client.screen.ShopScreenBase;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** A garage door's owner's screen: its name and switches, its remotes, and running it as a parking garage. */
public final class GarageScreen extends ShopScreenBase<GarageMenu> {
    private static final int WIDTH = 224;
    private static final int HALF = (WIDTH - 16 - 4) / 2;

    private EditBox nameBox;
    private EditBox hourBox;
    private EditBox dayBox;
    private Button windowsButton;
    private Button carsButton;
    private Button parkingButton;
    private boolean windows;
    private boolean autoOpen;
    private boolean inward;
    private boolean parking;

    public GarageScreen(GarageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 214);
        GarageSettings settings = menu.settings();
        windows = settings.windows();
        autoOpen = settings.autoOpen();
        inward = settings.inward();
        parking = settings.parking();
    }

    @Override
    protected void init() {
        super.init();
        GarageSettings settings = menu.settings();
        nameBox = addRenderableWidget(new EditBox(font, leftPos + 60, topPos + 18, WIDTH - 68, 16, Component.translatable("gui.reikocars.garage.name")));
        nameBox.setMaxLength(GarageSettings.MAX_NAME);
        nameBox.setValue(settings.name());
        nameBox.setHint(Component.translatable("gui.reikocars.garage.name_hint").withStyle(s -> s.withColor(0xFF808080)));
        boolean sectional = menu.style() == GarageDoorBlock.Style.SECTIONAL;
        windowsButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            windows = !windows;
            relabel();
        }).bounds(leftPos + 8, topPos + 38, HALF, 20).build());
        windowsButton.visible = sectional;
        carsButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            autoOpen = !autoOpen;
            relabel();
        }).bounds(sectional ? leftPos + 12 + HALF : leftPos + 8, topPos + 38, sectional ? HALF : WIDTH - 16, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.garage.swap"), button -> inward = !inward)
                .bounds(leftPos + WIDTH - 8 - 64, topPos + 60, 64, 20).build());
        parkingButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            parking = !parking;
            relabel();
        }).bounds(leftPos + 8, topPos + 84, WIDTH - 16, 20).build());
        hourBox = addRenderableWidget(new EditBox(font, leftPos + 58, topPos + 106, 50, 16, Component.translatable("gui.reikocars.garage.hour_price")));
        dayBox = addRenderableWidget(new EditBox(font, leftPos + WIDTH - 8 - 50, topPos + 106, 50, 16,
                Component.translatable("gui.reikocars.garage.day_price")));
        for (EditBox box : new EditBox[] {hourBox, dayBox}) {
            box.setFilter(MONEY_FILTER);
            box.setMaxLength(12);
        }
        hourBox.setValue(money(settings.hourPrice()));
        dayBox.setValue(money(settings.dayPrice()));
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.save"), button -> save())
                .bounds(leftPos + 8, topPos + 138, HALF, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.garage.reset"),
                        button -> PacketDistributor.sendToServer(GarageActionPayload.reset(menu.containerId)))
                .bounds(leftPos + 12 + HALF, topPos + 138, HALF, 20).build());
        relabel();
    }

    private static String money(long cents) {
        return String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void relabel() {
        windowsButton.setMessage(CommonComponents.optionStatus(Component.translatable("gui.reikocars.garage.windows"), windows));
        carsButton.setMessage(CommonComponents.optionStatus(Component.translatable("gui.reikocars.garage.auto_open"), autoOpen));
        parkingButton.setMessage(CommonComponents.optionStatus(Component.translatable("gui.reikocars.garage.parking"), parking));
        hourBox.setEditable(parking);
        dayBox.setEditable(parking);
    }

    private void save() {
        int flags = (windows ? 1 : 0) | (autoOpen ? 2 : 0) | (inward ? 4 : 0) | (parking ? 8 : 0);
        PacketDistributor.sendToServer(GarageActionPayload.save(menu.containerId, nameBox.getValue(), flags, hourBox.getValue(), dayBox.getValue()));
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
        Component size = Component.translatable("gui.reikocars.garage.size", menu.width(), menu.height());
        graphics.drawString(font, fit(title, imageWidth - 24 - font.width(size)), titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, size, imageWidth - 8 - font.width(size), titleLabelY, MUTED_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.reikocars.garage.name"), 8, 22, LABEL_COLOR, false);
        // Which side's the garage: it flips with the swap button, before it's saved.
        boolean inside = menu.inside() != (inward != menu.settings().inward());
        Component side = Component.translatable(inside ? "gui.reikocars.garage.you_inside" : "gui.reikocars.garage.you_outside");
        graphics.drawString(font, fit(side, imageWidth - 16 - 68), 8, 66, LABEL_COLOR, false);
        int fieldColor = parking ? LABEL_COLOR : MUTED_COLOR;
        graphics.drawString(font, Component.translatable("gui.reikocars.garage.hour_price"), 8, 110, fieldColor, false);
        Component day = Component.translatable("gui.reikocars.garage.day_price");
        graphics.drawString(font, day, imageWidth - 8 - 50 - 4 - font.width(day), 110, fieldColor, false);
        Component books = Component.translatable("gui.reikocars.garage.books", menu.parkedNow(), menu.visits(), format(menu.earned()));
        graphics.drawString(font, fit(books, imageWidth - 16), 8, 126, MUTED_COLOR, false);
        var hint = font.split(Component.translatable("gui.reikocars.garage.hint"), imageWidth - 16);
        for (int i = 0; i < Math.min(3, hint.size()); i++) {
            graphics.drawString(font, hint.get(i), 8, 162 + i * 10, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
