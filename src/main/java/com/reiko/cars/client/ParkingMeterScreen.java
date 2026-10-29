package com.reiko.cars.client;

import com.reiko.cars.car.CarType;
import com.reiko.cars.network.MeterActionPayload;
import com.reiko.cars.parking.MeterService;
import com.reiko.cars.parking.ParkingMeterMenu;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A parking meter's screen: the car parked at it and its time, more time bought in tens of minutes by card (with PIN)
 * or cash. Its owner gets a Settings tab for the rate and fine.
 */
public final class ParkingMeterScreen extends ShopScreenBase<ParkingMeterMenu> {
    private static final int WIDTH = 194;
    private static final int TABS_ROW = 16;
    private static final int PIN_X = WIDTH - 8 - 34;

    private final int top;
    private boolean settings;
    private int minutes = 30;
    private Button[] tabs = new Button[0];
    private Button less;
    private Button more;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;
    private EditBox rateBox;
    private EditBox fineBox;
    private Button setButton;
    private long shownCost = -1;

    public ParkingMeterScreen(ParkingMeterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isOwner() ? 30 + 188 : 18 + 158);
        this.top = menu.isOwner() ? 30 : 18;
    }

    private int pickRow() {
        return top + 38;
    }

    private int cardRow() {
        return top + 74;
    }

    private int pinRow() {
        return top + 88;
    }

    private int payRow() {
        return top + 104;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.isOwner()) {
            tabs = addTabs(8, TABS_ROW, new String[] {"gui.reikocars.tab_meter", "gui.reikocars.tab_settings"}, new int[] {58, 70},
                    i -> selectTab(i == 1));
        }
        less = addRenderableWidget(Button.builder(Component.literal("-"), button -> step(-10))
                .bounds(leftPos + 8, topPos + pickRow(), 20, 20).build());
        more = addRenderableWidget(Button.builder(Component.literal("+"), button -> step(10))
                .bounds(leftPos + WIDTH - 28, topPos + pickRow(), 20, 20).build());
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + pinRow() - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + payRow(), 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(Payment.cash()))
                .bounds(leftPos + 99, topPos + payRow(), 87, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.add_time_free"), button -> pay(Payment.cash()))
                .bounds(leftPos + 8, topPos + payRow(), WIDTH - 16, 20).build());
        addCardSwitch(leftPos + imageWidth - 22, topPos + cardRow() - 2);

        rateBox = addRenderableWidget(new EditBox(font, leftPos + 120, topPos + top + 2, 66, 16, Component.translatable("gui.reikocars.price")));
        rateBox.setFilter(MONEY_FILTER);
        rateBox.setMaxLength(16);
        rateBox.setValue(plain(menu.rate()));
        fineBox = addRenderableWidget(new EditBox(font, leftPos + 120, topPos + top + 24, 66, 16, Component.translatable("gui.reikocars.price")));
        fineBox.setFilter(MONEY_FILTER);
        fineBox.setMaxLength(16);
        fineBox.setValue(plain(menu.fine()));
        setButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_prices"),
                button -> PacketDistributor.sendToServer(MeterActionPayload.set(menu.containerId, rateBox.getValue(), fineBox.getValue())))
                .bounds(leftPos + 8, topPos + top + 46, WIDTH - 16, 20).build());
        shownCost = -1;
        selectTab(settings);
    }

    private static String plain(long cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void selectTab(boolean showSettings) {
        if (showSettings != settings) {
            clearFeedback();
        }
        settings = showSettings;
        updateWidgets();
    }

    private void step(int by) {
        minutes = Math.max(10, Math.min(MeterService.MAX_MINUTES, minutes + by));
        updateWidgets();
    }

    private long cost() {
        return menu.rate() * (minutes / 10);
    }

    private void pay(Payment payment) {
        PacketDistributor.sendToServer(MeterActionPayload.pay(menu.containerId, minutes, cost(), payment));
    }

    private void updateWidgets() {
        boolean meter = !settings;
        boolean free = menu.isOwner() || cost() == 0;
        less.visible = more.visible = meter;
        pinBox.visible = cardButton.visible = cashButton.visible = meter && !free;
        freeButton.visible = meter && free;
        rateBox.visible = fineBox.visible = setButton.visible = settings;
        long cost = cost();
        if (cost != shownCost) {
            shownCost = cost;
            setPayButtons(cardButton, cashButton, cost);
        }
        boolean car = menu.hasCar();
        cardButton.active = car && !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = car && cashOnHand() >= cost + salesTax(cost);
        freeButton.active = car;
        less.active = minutes > 10;
        more.active = minutes < MeterService.MAX_MINUTES;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    @Override
    protected void onEnter() {
        if (settings) {
            setButton.onPress();
        } else if (cardButton.visible && cardButton.active) {
            cardButton.onPress();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        if (tabs.length > 1) {
            drawSelectedTab(graphics, tabs[settings ? 1 : 0]);
        }
        if (settings) {
            graphics.drawString(font, Component.translatable("gui.reikocars.rate_per_10"), 8, top + 6, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.reikocars.overstay_fine"), 8, top + 28, LABEL_COLOR, false);
            Component books = Component.translatable("gui.reikocars.meter_books", format(menu.earned()), menu.tickets(), format(menu.fined()));
            var lines = font.split(books, imageWidth - 16);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.drawString(font, lines.get(i), 8, top + 74 + i * 10, MUTED_COLOR, false);
            }
            var hint = font.split(Component.translatable("gui.reikocars.meter_hint", (int) MeterService.REACH), imageWidth - 16);
            for (int i = 0; i < Math.min(6, hint.size()); i++) {
                graphics.drawString(font, hint.get(i), 8, top + 98 + i * 10, MUTED_COLOR, false);
            }
            drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
            return;
        }
        CarType type = menu.carType();
        Component car = type == null ? Component.translatable("gui.reikocars.meter_no_car")
                : Component.translatable("gui.reikocars.meter_car", Component.translatable("entity.reikocars." + type.id()));
        graphics.drawString(font, fit(car, imageWidth - 16), 8, top, LABEL_COLOR, false);
        int seconds = menu.secondsLeft();
        Component status;
        int color;
        if (type == null) {
            status = Component.empty();
            color = MUTED_COLOR;
        } else if (seconds > 0) {
            status = Component.translatable("gui.reikocars.meter_left", String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60));
            color = 0x1B7A2B;
        } else if (menu.expired()) {
            status = Component.translatable("gui.reikocars.meter_expired");
            color = 0xB3261E;
        } else {
            status = Component.translatable("gui.reikocars.meter_unpaid");
            color = 0x9A6A00;
        }
        graphics.drawString(font, fit(status, imageWidth - 16), 8, top + 12, color, false);
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.meter_rates", format(menu.rate()), format(menu.fine())), imageWidth - 16),
                8, top + 24, MUTED_COLOR, false);
        Component amount = Component.translatable("gui.reikocars.meter_minutes", minutes);
        graphics.drawString(font, amount, (imageWidth - font.width(amount)) / 2, pickRow() + 6, LABEL_COLOR, false);
        long cost = cost();
        Component total = menu.isOwner() ? Component.translatable("gui.reikocars.meter_yours")
                : Component.translatable("gui.reikocars.total", format(cost + salesTax(cost)));
        graphics.drawString(font, fit(total, imageWidth - 16), 8, pickRow() + 24, LABEL_COLOR, false);
        if (!menu.isOwner() && cost > 0) {
            drawCardLine(graphics, 8, cardRow(), imageWidth - 16);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                    8, pinRow(), LABEL_COLOR, false);
            graphics.drawString(font, pin, pinLabelX, pinRow(), LABEL_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
