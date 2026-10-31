package com.reiko.cars.client;

import com.reiko.cars.garage.ParkingMenu;
import com.reiko.cars.garage.ParkingService;
import com.reiko.cars.network.GarageActionPayload;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A parking garage's pay screen, at its door: how long to park (some hours or days), or, leaving after the time's up,
 * the hours overstayed. By card or cash.
 */
public final class ParkingScreen extends ShopScreenBase<ParkingMenu> {
    private static final int WIDTH = 224;
    private static final int OPTION_WIDTH = 49;
    private static final int CARD_ROW = 96;
    private static final int PIN_ROW = 110;
    private static final int PAY_ROW = 126;

    private final Button[] options = new Button[ParkingService.OPTIONS];
    private int option;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;

    public ParkingScreen(ParkingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 174);
    }

    @Override
    protected void init() {
        super.init();
        if (!menu.leaving()) {
            for (int i = 0; i < ParkingService.OPTIONS; i++) {
                int index = i;
                int column = i % ParkingService.HOURS.length;
                int row = i / ParkingService.HOURS.length;
                options[i] = addRenderableWidget(Button.builder(ParkingService.optionName(i), button -> choose(index))
                        .bounds(leftPos + 8 + column * (OPTION_WIDTH + 4), topPos + 18 + row * 22, OPTION_WIDTH, 20).build());
            }
        }
        pinBox = addRenderableWidget(createPinBox(leftPos + WIDTH - 8 - 34, topPos + PIN_ROW - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, (WIDTH - 20) / 2, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(Payment.cash()))
                .bounds(leftPos + 12 + (WIDTH - 20) / 2, topPos + PAY_ROW, (WIDTH - 20) / 2, 20).build());
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
        choose(option);
        setInitialFocus(pinBox);
    }

    private void choose(int option) {
        this.option = option;
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null) {
                options[i].active = i != option;
            }
        }
        long price = price();
        setPayButtons(cardButton, cashButton, price);
        if (price <= 0) {
            cashButton.setMessage(Component.translatable(menu.leaving() ? "gui.reikocars.parking.leave_free" : "gui.reikocars.parking.park_free"));
        }
        clearFeedback();
    }

    /** The price before tax: the chosen time's, or the overstay's. */
    private long price() {
        return menu.leaving() ? menu.fee() : menu.price(option);
    }

    private void pay(Payment payment) {
        PacketDistributor.sendToServer(GarageActionPayload.pay(menu.containerId, menu.leaving() ? ParkingService.LEAVE : option, price(), payment));
    }

    @Override
    protected void onEnter() {
        if (cardButton.active && cardButton.visible) {
            cardButton.onPress();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        long price = price();
        long total = price + salesTax(price);
        cardButton.visible = price > 0;
        cardButton.active = !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = price <= 0 || cashOnHand() >= total;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        long price = price();
        long tax = salesTax(price);
        Component line;
        Component hint;
        if (menu.leaving()) {
            graphics.drawString(font, fit(Component.translatable("gui.reikocars.parking.overstayed", menu.overHours()), imageWidth - 16), 8, 20,
                    LABEL_COLOR, false);
            line = tax > 0 ? Component.translatable("gui.reikocars.parking.fee_tax", format(price + tax), format(tax))
                    : Component.translatable("gui.reikocars.parking.fee", format(price));
            hint = Component.translatable("gui.reikocars.parking.leave_hint");
        } else {
            Component what = ParkingService.optionName(option);
            line = tax > 0 ? Component.translatable("gui.reikocars.parking.due_tax", what, format(price + tax), format(tax))
                    : Component.translatable("gui.reikocars.parking.due", what, format(price));
            hint = Component.translatable("gui.reikocars.parking.hint");
        }
        graphics.drawString(font, fit(line, imageWidth - 16), 8, menu.leaving() ? 34 : 64, LABEL_COLOR, false);
        var lines = font.split(hint, imageWidth - 16);
        int hintTop = menu.leaving() ? 48 : 76;
        for (int i = 0; i < Math.min(menu.leaving() ? 4 : 2, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 8, hintTop + i * 10, MUTED_COLOR, false);
        }
        drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
        Component pin = Component.translatable("gui.reikoeconomy.pin");
        int pinLabelX = WIDTH - 8 - 34 - 4 - font.width(pin);
        graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                8, PIN_ROW, LABEL_COLOR, false);
        graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
