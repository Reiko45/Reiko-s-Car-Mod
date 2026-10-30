package com.reiko.cars.client;

import com.reiko.cars.network.SaleActionPayload;
import com.reiko.cars.sale.CarSaleMenu;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One car's sale screen. Buying: what it is, its condition and fuel, the seller and the price, paid by card (with
 * PIN) or cash; at a dealership, outright or on finance (a down payment now, the rest a day at a time). Selling (its
 * owner): the asking price, and putting it up for sale or taking it off.
 */
public final class CarSaleScreen extends ShopScreenBase<CarSaleMenu> {
    private static final int WIDTH = 194;
    private static final int PIN_X = WIDTH - 8 - 34;
    // buying (a line lower with finance on offer, for the loan's terms)
    private static final int CARD_ROW = 58;
    private static final int PIN_ROW = 72;
    private static final int PAY_ROW = 90;
    private static final int FINANCE_SHIFT = 22;
    // selling
    private static final int PRICE_ROW = 46;
    private static final int SELL_ROW = 68;

    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;
    private EditBox priceBox;
    private Button outrightButton;
    private Button financeButton;
    /** Buying on finance rather than outright. */
    private boolean finance;

    public CarSaleScreen(CarSaleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isSelling() ? 162 : menu.terms() != null ? 188 : 142);
    }

    private int shift() {
        return menu.terms() != null ? FINANCE_SHIFT : 0;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.isSelling()) {
            priceBox = addRenderableWidget(new EditBox(font, leftPos + 50, topPos + PRICE_ROW - 4, 100, 16, Component.translatable("gui.reikocars.price")));
            priceBox.setFilter(MONEY_FILTER);
            priceBox.setMaxLength(16);
            priceBox.setValue(menu.price() >= 0 ? plain(menu.price()) : "");
            Button sell = addRenderableWidget(Button.builder(Component.translatable(menu.price() >= 0 ? "gui.reikocars.change_price" : "gui.reikocars.put_up_for_sale"),
                    button -> send(SaleActionPayload.SET_PRICE)).bounds(leftPos + 8, topPos + SELL_ROW, 87, 20).build());
            Button off = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.take_off_sale"),
                    button -> send(SaleActionPayload.OFF_SALE)).bounds(leftPos + 99, topPos + SELL_ROW, 87, 20).build());
            off.active = menu.price() >= 0;
            sell.active = true;
            setInitialFocus(priceBox);
            return;
        }
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW + shift() - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW + shift(), 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(Payment.cash()))
                .bounds(leftPos + 99, topPos + PAY_ROW + shift(), 87, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.take_it_free"), button -> buy(Payment.cash()))
                .bounds(leftPos + 8, topPos + PAY_ROW + shift(), WIDTH - 16, 20).build());
        outrightButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.pay_outright"), button -> setFinance(false))
                .bounds(leftPos + 8, topPos + PAY_ROW + shift() + 24, 87, 16).build());
        financeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.pay_finance"), button -> setFinance(true))
                .bounds(leftPos + 99, topPos + PAY_ROW + shift() + 24, 87, 16).build());
        outrightButton.visible = financeButton.visible = menu.terms() != null;
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW + shift() - 2);
        setFinance(false);
    }

    private void setFinance(boolean on) {
        finance = on && menu.terms() != null;
        if (menu.price() > 0) {
            if (finance) {
                long total = dueNow();
                cardButton.setMessage(Component.translatable("gui.reikoeconomy.pay_card", format(total)));
                cashButton.setMessage(Component.translatable("gui.reikoeconomy.pay_cash", format(total)));
            } else {
                setPayButtons(cardButton, cashButton, menu.price());
            }
        }
        if (outrightButton != null) {
            outrightButton.active = finance;
            financeButton.active = !finance;
        }
        updateButtons();
    }

    /** What's paid now: the price and tax, or on finance the down payment and the tax on the whole price. */
    private long dueNow() {
        long tax = salesTax(Math.max(0, menu.price()));
        return finance && menu.terms() != null ? menu.terms().down() + tax : menu.price() + tax;
    }

    /** 1250000 cents as "12500.00", for the price box. */
    private static String plain(long cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void send(int action) {
        PacketDistributor.sendToServer(SaleActionPayload.text(menu.containerId, action, priceBox.getValue(), 0));
    }

    private void buy(Payment payment) {
        PacketDistributor.sendToServer(finance ? SaleActionPayload.finance(menu.containerId, menu.price(), payment)
                : SaleActionPayload.buy(menu.containerId, menu.price(), payment));
    }

    @Override
    protected void onEnter() {
        if (menu.isSelling()) {
            send(SaleActionPayload.SET_PRICE);
        } else if (cardButton.visible && cardButton.active) {
            cardButton.onPress();
        }
    }

    private void updateButtons() {
        if (menu.isSelling()) {
            return;
        }
        boolean forSale = menu.price() >= 0;
        boolean free = menu.price() == 0;
        long total = dueNow();
        pinBox.visible = cardButton.visible = cashButton.visible = forSale && !free;
        freeButton.visible = forSale && free;
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

    /** A little square of the car's paint. */
    static void swatch(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 8, y + 8, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 7, y + 7, 0xFF000000 | color);
    }

    static Component conditionLine(int condition, int fuel) {
        return conditionLine(condition, fuel, 0);
    }

    /** Its condition and fuel, and how dirty it is unless it's clean. */
    static Component conditionLine(int condition, int fuel, int dirt) {
        com.reiko.cars.wash.Dirt.Grade grade = com.reiko.cars.wash.Dirt.Grade.of(dirt / 100.0F);
        if (grade == com.reiko.cars.wash.Dirt.Grade.CLEAN) {
            return Component.translatable("gui.reikocars.sale_condition", Math.round(condition / 10.0F), Math.round(fuel / 10.0F));
        }
        return Component.translatable("gui.reikocars.sale_condition_dirt", Math.round(condition / 10.0F), Math.round(fuel / 10.0F),
                Component.translatable("dirt.reikocars.adjective." + grade.name().toLowerCase(java.util.Locale.ROOT)));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        if (menu.isSelling()) {
            swatch(graphics, 8, 19, menu.color());
            graphics.drawString(font, conditionLine(menu.condition(), menu.fuel(), menu.dirt()), 20, 20, MUTED_COLOR, false);
            Component status = menu.price() >= 0
                    ? Component.translatable("gui.reikocars.on_sale_at", format(menu.price()))
                    : Component.translatable("gui.reikocars.not_on_sale");
            graphics.drawString(font, fit(status, imageWidth - 16), 8, 32, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.reikocars.price_label"), 8, PRICE_ROW, LABEL_COLOR, false);
            var lines = font.split(Component.translatable("gui.reikocars.selling_hint"), imageWidth - 16);
            for (int i = 0; i < Math.min(4, lines.size()); i++) {
                graphics.drawString(font, lines.get(i), 8, SELL_ROW + 26 + i * 10, MUTED_COLOR, false);
            }
            drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
            return;
        }
        swatch(graphics, 8, 19, menu.color());
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.seller", menu.ownerName()), imageWidth - 30), 20, 20, MUTED_COLOR, false);
        graphics.drawString(font, conditionLine(menu.condition(), menu.fuel(), menu.dirt()), 8, 32, MUTED_COLOR, false);
        long price = menu.price();
        long tax = salesTax(Math.max(0, price));
        Component line = price < 0 ? Component.translatable("gui.reikocars.no_longer_for_sale")
                : price == 0 ? Component.translatable("gui.reikocars.price_free")
                : tax > 0 ? Component.translatable("gui.reikocars.price_tax", format(price + tax), format(tax))
                : Component.translatable("gui.reikocars.price_is", format(price));
        graphics.drawString(font, fit(line, imageWidth - 16), 8, 44, LABEL_COLOR, false);
        if (menu.terms() != null && price > 0) {
            var terms = menu.terms();
            Component loan = finance
                    ? Component.translatable("gui.reikocars.finance_terms", format(terms.down()), format(terms.installment()), terms.days())
                    : Component.translatable("gui.reikocars.finance_offer", format(terms.down()), format(terms.installment()), terms.days());
            var lines = font.split(loan, imageWidth - 16);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.drawString(font, lines.get(i), 8, 56 + i * 10, finance ? 0xFF2E7D32 : MUTED_COLOR, false);
            }
        }
        if (price > 0) {
            drawCardLine(graphics, 8, CARD_ROW + shift(), imageWidth - 16);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                    8, PIN_ROW + shift(), LABEL_COLOR, false);
            graphics.drawString(font, pin, pinLabelX, PIN_ROW + shift(), LABEL_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
