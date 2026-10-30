package com.reiko.cars.client;

import com.reiko.cars.car.CarType;
import com.reiko.cars.charge.ChargerMenu;
import com.reiko.cars.charge.ChargerService;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.network.ChargerActionPayload;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.TaxRates;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The charging station's screen. Built on the economy mod's shop screen, like the pumps', so paying looks and works the
 * same: pick a card (the ⇄ button switches when you carry several), type its PIN and press Card, or press Cash. The
 * station's owner charges free, and also gets a row for setting the price.
 *
 * <p>It shows the power the station has stored, the electric car at its cable and how full its battery is, and lets
 * the player pick how much to charge in steps of 5,000 MJ (1,000 with shift held), or fill it up.
 */
public final class ChargerScreen extends ShopScreenBase<ChargerMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 200;
    private static final int OWNER_HEIGHT = 238;
    private static final int BAR_WIDTH = 160;
    private static final int STOCK_ROW = 18;
    private static final int PRICE_ROW = 37;
    private static final int CAR_ROW = 48;
    private static final int AMOUNT_ROW = 67;
    private static final int TOTAL_ROW = 91;
    private static final int CARD_ROW = 102;
    private static final int PIN_ROW = 122;
    private static final int PAY_ROW = 141;
    private static final int FEEDBACK_ROW = 165;
    private static final int OWNER_ROW = 204;
    private static final int FEEDBACK_LINES = 3;
    private static final int POWER_COLOR = 0xFF29B6F6;

    /** MJ to charge. */
    private long amount;
    /** Keep {@link #amount} at "fill it up" as the car and the store change, until the player picks an amount. */
    private boolean fillUp = true;
    private long shownTotal = -1;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;
    private EditBox priceBox;

    public ChargerScreen(ChargerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isManager() ? OWNER_HEIGHT : HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("-"), button -> changeAmount(-1))
                .bounds(leftPos + 8, topPos + AMOUNT_ROW, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> changeAmount(1))
                .bounds(leftPos + 74, topPos + AMOUNT_ROW, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.charge_full"), button -> fillUp = true)
                .bounds(leftPos + 100, topPos + AMOUNT_ROW, 68, 20).build());

        pinBox = addRenderableWidget(createPinBox(leftPos + 32, topPos + PIN_ROW));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> charge(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 78, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> charge(Payment.cash()))
                .bounds(leftPos + 90, topPos + PAY_ROW, 78, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.charge_free"), button -> charge(Payment.cash()))
                .bounds(leftPos + 8, topPos + PAY_ROW, 160, 20).build());
        boolean free = menu.isOwner();
        pinBox.visible = cardButton.visible = cashButton.visible = !free;
        freeButton.visible = free;
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);

        if (menu.isManager()) {
            priceBox = addRenderableWidget(new EditBox(font, leftPos + 76, topPos + OWNER_ROW, 48, 16,
                    Component.translatable("gui.reikocars.price")));
            priceBox.setFilter(MONEY_FILTER);
            priceBox.setMaxLength(16);
            priceBox.setValue(plainAmount(menu.price()));
            addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"), button -> setPrice())
                    .bounds(leftPos + 128, topPos + OWNER_ROW - 2, 40, 20).build());
        }
        updateButtons();
    }

    /** 5 cents as "0.05", for the price box. */
    private static String plainAmount(long cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private long maxAmount() {
        return menu.carType() == null || !menu.carElectric() ? 0 : Math.min(menu.stock(), menu.carRoom());
    }

    private void changeAmount(int direction) {
        long step = hasShiftDown() ? ChargerService.STEP : 5 * ChargerService.STEP;
        long max = maxAmount();
        long base = fillUp ? max - max % step : amount;
        amount = Math.max(Math.min(step, max), Math.min(max, base + direction * step));
        fillUp = false;
        updateButtons();
    }

    private void charge(Payment payment) {
        PacketDistributor.sendToServer(ChargerActionPayload.charge(menu.containerId, amount, menu.price(), payment));
    }

    private void setPrice() {
        PacketDistributor.sendToServer(ChargerActionPayload.setPrice(menu.containerId, priceBox.getValue()));
    }

    @Override
    protected void onEnter() {
        if (priceBox != null && priceBox.isFocused()) {
            setPrice();
        } else if (cardButton.visible && cardButton.active) {
            cardButton.onPress();
        }
    }

    private long total() {
        return ChargerService.cost(menu.price(), amount);
    }

    private void updateButtons() {
        long max = maxAmount();
        amount = fillUp ? max : Math.min(amount, max);
        long total = total();
        if (total != shownTotal) {
            shownTotal = total;
            setPayButtons(cardButton, cashButton, total);
        }
        boolean canCharge = amount > 0;
        cardButton.active = canCharge && !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = canCharge && cashOnHand() >= total + salesTax(total);
        freeButton.active = canCharge;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    // ---- drawing ----

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        int barX = leftPos + 8;
        drawBar(graphics, barX, topPos + STOCK_ROW + 10, menu.capacity() <= 0 ? 0 : (float) menu.stock() / menu.capacity(), POWER_COLOR);
        if (menu.carType() != null && menu.carElectric()) {
            drawBar(graphics, barX, topPos + CAR_ROW + 10, menu.carCharge(), menu.carCharge() < 0.15F ? 0xFFE53935 : 0xFF43A047);
        }
        graphics.fill(leftPos + 30, topPos + AMOUNT_ROW + 1, leftPos + 72, topPos + AMOUNT_ROW + 19, 0xFF8B8B8B);
        if (menu.isManager()) {
            graphics.fill(leftPos + 6, topPos + OWNER_ROW - 8, leftPos + imageWidth - 6, topPos + OWNER_ROW - 7, 0xFF8B8B8B);
        }
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, float fraction, int color) {
        graphics.fill(x, y, x + BAR_WIDTH, y + 5, 0xFF373737);
        int filled = Math.round(BAR_WIDTH * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 5, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);

        Component stock = Component.translatable("gui.reikocars.mj_of", ChargerService.kmj(menu.stock()), ChargerService.kmj(menu.capacity()));
        Component power = menu.stock() > 0 ? Component.translatable("gui.reikocars.station_power")
                : Component.translatable("gui.reikocars.station_no_power");
        graphics.drawString(font, fit(power, imageWidth - 24 - font.width(stock)), 8, STOCK_ROW, menu.stock() > 0 ? LABEL_COLOR : 0xB3261E, false);
        graphics.drawString(font, stock, imageWidth - 8 - font.width(stock), STOCK_ROW, MUTED_COLOR, false);

        long basisPoints = EconomyConfig.taxRates().salesBasisPoints();
        Component price = menu.price() == 0
                ? Component.translatable("gui.reikocars.price_free")
                : basisPoints > 0
                        ? Component.translatable("gui.reikocars.price_per_1000_mj_tax", format(menu.price()), TaxRates.percentText(basisPoints))
                        : Component.translatable("gui.reikocars.price_per_1000_mj", format(menu.price()));
        graphics.drawString(font, fit(price, imageWidth - 16), 8, PRICE_ROW, LABEL_COLOR, false);

        CarType car = menu.carType();
        int textWidth = imageWidth - 16;
        if (car == null) {
            Component none = Component.translatable("gui.reikocars.no_ev", (int) CarsConfig.get(CarsConfig.HOSE_LENGTH));
            graphics.drawString(font, fit(none, textWidth), 8, CAR_ROW, 0xB3261E, false);
        } else {
            Component carName = Component.translatable("entity.reikocars." + car.id());
            boolean notElectric = !menu.carElectric();
            Component battery = notElectric
                    ? Component.translatable("gui.reikocars.car_wrong_fuel", car.fuel().displayName())
                    : menu.carRoom() > 0
                            ? Component.translatable("gui.reikocars.battery_room", Math.round(menu.carCharge() * 100), ChargerService.kmj(menu.carRoom()))
                            : Component.translatable("gui.reikocars.battery_full", Math.round(menu.carCharge() * 100));
            Component shown = fit(battery, textWidth - 6 - font.width(carName));
            graphics.drawString(font, carName, 8, CAR_ROW, LABEL_COLOR, false);
            graphics.drawString(font, shown, imageWidth - 8 - font.width(shown), CAR_ROW, notElectric ? 0xB3261E : MUTED_COLOR, false);
        }

        String shownAmount = ChargerService.kmj(amount) + " MJ";
        graphics.drawString(font, shownAmount, 51 - font.width(shownAmount) / 2, AMOUNT_ROW + 6, 0xFFFFFF, true);

        Component totalLine = menu.isOwner()
                ? Component.translatable("gui.reikocars.total_free")
                : Component.translatable("gui.reikocars.total", format(total() + salesTax(total())));
        graphics.drawString(font, totalLine, 8, TOTAL_ROW, LABEL_COLOR, false);

        if (!menu.isOwner()) {
            drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
            graphics.drawString(font, Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())),
                    8, CARD_ROW + 10, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.reikoeconomy.pin"), 8, PIN_ROW + 4, LABEL_COLOR, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.reikocars.your_station"), 8, CARD_ROW, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, FEEDBACK_ROW, imageWidth - 16, FEEDBACK_LINES);

        if (menu.isManager()) {
            graphics.drawString(font, Component.translatable("gui.reikocars.price_label_mj"), 8, OWNER_ROW + 4, LABEL_COLOR, false);
            Component sold = Component.translatable("gui.reikocars.charge_sold", ChargerService.kmj(menu.mjSold()), format(menu.earned()));
            graphics.drawString(font, fit(sold, imageWidth - 16), 8, OWNER_ROW + 21, MUTED_COLOR, false);
        }
    }
}
