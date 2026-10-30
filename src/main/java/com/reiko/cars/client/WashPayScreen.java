package com.reiko.cars.client;

import com.reiko.cars.network.WashActionPayload;
import com.reiko.cars.wash.WashOwnerMenu;
import com.reiko.cars.wash.WashPayMenu;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** The pay screen at a Car Wash (a wash for the car being driven) or a Wash Bay (time on its pressure washer). */
public final class WashPayScreen extends ShopScreenBase<WashPayMenu> {
    private static final int WIDTH = 194;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int CARD_ROW = 46;
    private static final int PIN_ROW = 60;
    private static final int PAY_ROW = 76;

    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;

    public WashPayScreen(WashPayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 124);
    }

    @Override
    protected void init() {
        super.init();
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(Payment.cash()))
                .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
        setPayButtons(cardButton, cashButton, menu.price());
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
        setInitialFocus(pinBox);
    }

    private void pay(Payment payment) {
        PacketDistributor.sendToServer(WashActionPayload.pay(menu.containerId, menu.price(), payment));
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
        long total = menu.price() + salesTax(menu.price());
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
        long price = menu.price();
        long tax = salesTax(price);
        boolean bay = menu.kind() == WashOwnerMenu.BAY;
        Component what = bay ? Component.translatable("gui.reikocars.bay_time", menu.seconds()) : Component.translatable("gui.reikocars.a_wash");
        Component line = tax > 0
                ? Component.translatable("gui.reikocars.wash_due_tax", what, format(price + tax), format(tax))
                : Component.translatable("gui.reikocars.wash_due", what, format(price));
        graphics.drawString(font, fit(line, imageWidth - 16), 8, 20, LABEL_COLOR, false);
        Component hint = Component.translatable(bay ? "gui.reikocars.bay_pay_hint" : "gui.reikocars.wash_pay_hint");
        graphics.drawString(font, fit(hint, imageWidth - 16), 8, 31, MUTED_COLOR, false);
        drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
        Component pin = Component.translatable("gui.reikoeconomy.pin");
        int pinLabelX = PIN_X - 4 - font.width(pin);
        graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                8, PIN_ROW, LABEL_COLOR, false);
        graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
