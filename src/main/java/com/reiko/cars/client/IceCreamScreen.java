package com.reiko.cars.client;

import com.reiko.cars.icecream.IceCreamMenu;
import com.reiko.cars.network.IceCreamActionPayload;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * An ice cream truck's shop screen. Customers pick something from the menu with the arrows and buy one at a time by
 * card or cash; the owner sees everything in the freezer and prices each kind.
 */
public final class IceCreamScreen extends ShopScreenBase<IceCreamMenu> {
    private static final int WIDTH = 194;
    private static final int TOP = 20;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int CARD_ROW = TOP + 58;
    private static final int PIN_ROW = TOP + 72;
    private static final int PAY_ROW = TOP + 86;

    private Button previous;
    private Button next;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private EditBox priceBox;
    private Button setButton;
    private long shownPrice = Long.MIN_VALUE;
    private ItemStack shownItem = ItemStack.EMPTY;

    public IceCreamScreen(IceCreamMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isOwner() ? 184 : 188);
    }

    @Override
    protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> pick(IceCreamMenu.PREVIOUS))
                .bounds(leftPos + imageWidth - 8 - 26, topPos + TOP + 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.previous_treat"))).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> pick(IceCreamMenu.NEXT))
                .bounds(leftPos + imageWidth - 8 - 12, topPos + TOP + 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.next_treat"))).build());
        if (menu.isOwner()) {
            priceBox = addRenderableWidget(new EditBox(font, leftPos + 120, topPos + TOP + 28, 66, 16, Component.translatable("gui.reikocars.price")));
            priceBox.setFilter(text -> text.isEmpty() || MONEY_FILTER.test(text));
            priceBox.setMaxLength(16);
            setButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"), button -> setPrice())
                    .bounds(leftPos + 8, topPos + TOP + 48, WIDTH - 16, 20).build());
            shownPrice = Long.MIN_VALUE;
            shownItem = ItemStack.EMPTY;
            updateWidgets();
            setInitialFocus(priceBox);
            return;
        }
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(Payment.cash()))
                .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
        updateWidgets();
    }

    private void pick(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private void buy(Payment payment) {
        PacketDistributor.sendToServer(IceCreamActionPayload.buy(menu.containerId, menu.price(), payment));
    }

    private void setPrice() {
        PacketDistributor.sendToServer(IceCreamActionPayload.price(menu.containerId, priceBox.getValue()));
    }

    private static String money(long cents) {
        return String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void updateWidgets() {
        boolean any = menu.count() > 0 && !menu.item().isEmpty();
        previous.visible = next.visible = menu.count() > 1;
        if (menu.isOwner()) {
            priceBox.visible = setButton.visible = any;
            // Show the picked item's price, whenever the pick (or its price) changes.
            ItemStack item = menu.item();
            long price = menu.price();
            if (!ItemStack.isSameItem(item, shownItem) || price != shownPrice) {
                priceBox.setValue(price < 0 ? "" : money(price));
                shownItem = item;
                shownPrice = price;
            }
            return;
        }
        long price = menu.price();
        boolean forSale = any && price >= 0 && menu.stock() > 0;
        cardButton.visible = cashButton.visible = pinBox.visible = forSale;
        if (forSale) {
            setPayButtons(cardButton, cashButton, price);
        }
        long total = price + salesTax(Math.max(0, price));
        cardButton.active = forSale && (price == 0 || (!chosenCard().isEmpty() && pinBox.getValue().length() == 4));
        cashButton.active = forSale && cashOnHand() >= total;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    @Override
    protected void onEnter() {
        if (menu.isOwner()) {
            if (setButton.visible) {
                setPrice();
            }
        } else if (cardButton.visible && cardButton.active) {
            cardButton.onPress();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    private void lines(GuiGraphics graphics, Component text, int y, int max, int color) {
        var lines = font.split(text, imageWidth - 16);
        for (int i = 0; i < Math.min(max, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 8, y + i * 10, color, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        ItemStack item = menu.item();
        boolean any = menu.count() > 0 && !item.isEmpty();
        if (!any) {
            lines(graphics, Component.translatable(menu.isOwner() ? "gui.reikocars.freezer_empty" : "gui.reikocars.nothing_for_sale"),
                    TOP + 2, 4, MUTED_COLOR);
        } else {
            graphics.renderFakeItem(item, 8, TOP - 2);
            int right = imageWidth - 8 - (previous.visible ? 30 : 0);
            graphics.drawString(font, fit(item.getHoverName(), right - 28), 28, TOP + 2, LABEL_COLOR, false);
            if (menu.count() > 1) {
                // Which of how many, under the arrows.
                String which = (menu.index() + 1) + "/" + menu.count();
                graphics.drawString(font, which, imageWidth - 8 - 13 - font.width(which) / 2, TOP + 15, MUTED_COLOR, false);
            }
            Component left = menu.stock() > 0
                    ? Component.translatable("gui.reikocars.in_stock", menu.stock())
                    : Component.translatable("gui.reikocars.sold_out");
            graphics.drawString(font, fit(left, imageWidth - 36), 28, TOP + 13, menu.stock() > 0 ? 0x1B7A2B : 0xB3261E, false);
        }
        if (menu.isOwner()) {
            if (any) {
                Component priced = menu.price() < 0 ? Component.translatable("gui.reikocars.not_on_menu") : Component.translatable("gui.reikocars.price_each");
                graphics.drawString(font, fit(priced, 108), 8, TOP + 32, LABEL_COLOR, false);
            }
            lines(graphics, Component.translatable("gui.reikocars.ice_cream_owner_hint"), TOP + 74, 6, MUTED_COLOR);
            drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
            return;
        }
        if (any && menu.price() >= 0) {
            graphics.drawString(font, fit(buyPriceLine(menu.price()), imageWidth - 16), 8, TOP + 30, LABEL_COLOR, false);
            if (menu.stock() > 0) {
                drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
                Component pin = Component.translatable("gui.reikoeconomy.pin");
                int pinLabelX = PIN_X - 4 - font.width(pin);
                graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                        8, PIN_ROW, LABEL_COLOR, false);
                graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
            }
        }
        lines(graphics, Component.translatable("gui.reikocars.ice_cream_hint", menu.ownerName()), TOP + 112, 2, MUTED_COLOR);
        drawFeedback(graphics, 8, imageHeight - 32, imageWidth - 16, 3);
    }
}
