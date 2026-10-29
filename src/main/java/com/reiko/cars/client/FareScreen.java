package com.reiko.cars.client;

import com.reiko.cars.fare.FareMenu;
import com.reiko.cars.network.FareActionPayload;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** A bus passenger's pay screen: the fare, by card (with PIN) or cash, or get off. Closing it is getting off. */
public final class FareScreen extends ShopScreenBase<FareMenu> {
    private static final int WIDTH = 194;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int CARD_ROW = 46;
    private static final int PIN_ROW = 60;
    private static final int PAY_ROW = 78;
    private static final int OFF_ROW = 102;
    private static final int FEEDBACK_ROW = 126;

    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;

    public FareScreen(FareMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 150);
    }

    @Override
    protected void init() {
        super.init();
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(Payment.cash()))
                .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.get_off"), button -> onClose())
                .bounds(leftPos + (WIDTH - 90) / 2, topPos + OFF_ROW, 90, 20).build());
        setPayButtons(cardButton, cashButton, menu.fare());
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
        updateButtons();
    }

    private void pay(Payment payment) {
        PacketDistributor.sendToServer(FareActionPayload.pay(menu.containerId, menu.fare(), payment));
    }

    @Override
    protected void onEnter() {
        if (cardButton.active) {
            cardButton.onPress();
        }
    }

    private void updateButtons() {
        long total = menu.fare() + salesTax(menu.fare());
        cardButton.active = !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = cashOnHand() >= total;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.fare_driver", menu.driverName()), imageWidth - 16), 8, 20,
                MUTED_COLOR, false);
        long fare = menu.fare();
        long tax = salesTax(fare);
        Component line = tax > 0
                ? Component.translatable("gui.reikocars.fare_due_tax", format(fare + tax), format(tax))
                : Component.translatable("gui.reikocars.fare_due", format(fare));
        graphics.drawString(font, fit(line, imageWidth - 16), 8, 32, LABEL_COLOR, false);
        drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
        Component pin = Component.translatable("gui.reikoeconomy.pin");
        int pinLabelX = PIN_X - 4 - font.width(pin);
        graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                8, PIN_ROW, LABEL_COLOR, false);
        graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
        drawFeedback(graphics, 8, FEEDBACK_ROW, imageWidth - 16, 2);
    }
}
