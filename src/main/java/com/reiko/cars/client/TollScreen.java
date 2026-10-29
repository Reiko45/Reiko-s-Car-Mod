package com.reiko.cars.client;

import com.reiko.cars.network.TollActionPayload;
import com.reiko.cars.toll.TollMenu;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** The pay screen at a toll gate: the toll, by card (with PIN) or cash. Paying lifts the gate. */
public final class TollScreen extends ShopScreenBase<TollMenu> {
    private static final int WIDTH = 194;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int CARD_ROW = 36;
    private static final int PIN_ROW = 50;
    private static final int PAY_ROW = 66;

    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;

    public TollScreen(TollMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 114);
    }

    @Override
    protected void init() {
        super.init();
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(Payment.cash()))
                .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
        setPayButtons(cardButton, cashButton, menu.toll());
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
        setInitialFocus(pinBox);
    }

    private void pay(Payment payment) {
        PacketDistributor.sendToServer(TollActionPayload.pay(menu.containerId, menu.toll(), payment));
    }

    @Override
    protected void onEnter() {
        if (cardButton.active) {
            cardButton.onPress();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        long total = menu.toll() + salesTax(menu.toll());
        cardButton.active = !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = cashOnHand() >= total;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        long toll = menu.toll();
        long tax = salesTax(toll);
        Component line = tax > 0
                ? Component.translatable("gui.reikocars.toll_due_tax", format(toll + tax), format(tax))
                : Component.translatable("gui.reikocars.toll_due", format(toll));
        graphics.drawString(font, fit(line, imageWidth - 16), 8, 20, LABEL_COLOR, false);
        drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
        Component pin = Component.translatable("gui.reikoeconomy.pin");
        int pinLabelX = PIN_X - 4 - font.width(pin);
        graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                8, PIN_ROW, LABEL_COLOR, false);
        graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
