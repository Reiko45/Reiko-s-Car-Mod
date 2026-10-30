package com.reiko.cars.client;

import com.reiko.cars.car.CarType;
import com.reiko.cars.network.LotActionPayload;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.rental.RentalMenu;
import com.reiko.cars.rental.RentalService;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A rental desk's screen. Customers browse the fleet, choose how many days, and rent the car picked by card (with
 * PIN) or cash. The owner: Fleet (their cars on the lot and out on rent) and Prices (a day's rent for each kind of car).
 */
public final class RentalScreen extends ShopScreenBase<RentalMenu> {
    private static final int WIDTH = 194;
    private static final int TABS_ROW = 16;
    private static final int PIN_X = WIDTH - 8 - 34;

    /** Setting a price reopens the screen: on the Prices tab, at the same kind of car, if it's the same desk. */
    @Nullable
    private static BlockPos reopenPricesAt;
    private static int reopenType;

    private int tab = RentalMenu.TAB_FLEET;
    private int priceType;
    private int days = 1;
    private Button[] tabs = new Button[0];
    private Button previous;
    private Button next;
    private Button fewer;
    private Button more;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button typePrevious;
    private Button typeNext;
    private EditBox priceBox;
    private Button setPrice;

    public RentalScreen(RentalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isManager() ? 176 : 192);
    }

    /** Where the car's details start: under the tabs for the owner. */
    private int top() {
        return menu.isManager() ? 34 : 20;
    }

    @Override
    protected void init() {
        super.init();
        int top = top();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> pick(RentalMenu.PREVIOUS))
                .bounds(leftPos + imageWidth - 8 - 26, topPos + top - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.previous_vehicle"))).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> pick(RentalMenu.NEXT))
                .bounds(leftPos + imageWidth - 8 - 12, topPos + top - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.next_vehicle"))).build());
        if (menu.isManager()) {
            tabs = addTabs(8, TABS_ROW, new String[] {"gui.reikocars.tab_fleet", "gui.reikocars.tab_prices"}, new int[] {58, 58}, this::selectTab);
            typePrevious = addRenderableWidget(Button.builder(Component.literal("<"), button -> pickType(-1))
                    .bounds(leftPos + 8, topPos + top + 2, 12, 12).build());
            typeNext = addRenderableWidget(Button.builder(Component.literal(">"), button -> pickType(1))
                    .bounds(leftPos + imageWidth - 20, topPos + top + 2, 12, 12).build());
            priceBox = addRenderableWidget(new EditBox(font, leftPos + 80, topPos + top + 24, 60, 16, Component.translatable("gui.reikocars.price")));
            priceBox.setFilter(MONEY_FILTER);
            priceBox.setMaxLength(16);
            setPrice = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"), button -> sendPrice())
                    .bounds(leftPos + 144, topPos + top + 23, 42, 18).build());
            if (menu.pos().equals(reopenPricesAt)) {
                tab = RentalMenu.TAB_PRICES;
                priceType = reopenType;
            }
            reopenPricesAt = null;
            priceBox.setValue(plain(menu.price(CarType.values()[priceType])));
        } else {
            fewer = addRenderableWidget(Button.builder(Component.literal("-"), button -> changeDays(-1)).bounds(leftPos + 44, topPos + 61, 14, 14).build());
            more = addRenderableWidget(Button.builder(Component.literal("+"), button -> changeDays(1)).bounds(leftPos + 80, topPos + 61, 14, 14).build());
            pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + 102));
            cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> rent(cardPayment(pinBox.getValue())))
                    .bounds(leftPos + 8, topPos + 122, 87, 20).build());
            cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> rent(Payment.cash()))
                    .bounds(leftPos + 99, topPos + 122, 87, 20).build());
            addCardSwitch(leftPos + imageWidth - 22, topPos + 90);
        }
        updateWidgets();
    }

    private static String plain(long cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void send(LotActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    private void pick(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private void selectTab(int selected) {
        if (selected != tab) {
            clearFeedback();
        }
        tab = selected;
        updateWidgets();
    }

    private void pickType(int step) {
        priceType = Math.floorMod(priceType + step, CarType.values().length);
        priceBox.setValue(plain(menu.price(CarType.values()[priceType])));
    }

    private void sendPrice() {
        reopenPricesAt = menu.pos();
        reopenType = priceType;
        send(LotActionPayload.text(menu.containerId, LotActionPayload.SET_RENT, priceBox.getValue(), priceType));
    }

    private void changeDays(int step) {
        days = Math.max(1, Math.min(RentalService.maxDays(), days + step));
    }

    /** The rent for the picked car for the days chosen, before tax. */
    private long cost() {
        CarType type = menu.carType();
        return type == null ? 0 : menu.price(type) * days;
    }

    private void rent(Payment payment) {
        send(LotActionPayload.pay(menu.containerId, LotActionPayload.RENT, cost(), payment, days));
    }

    private void updateWidgets() {
        boolean car = menu.hasCar();
        boolean prices = menu.isManager() && tab == RentalMenu.TAB_PRICES;
        previous.visible = next.visible = !prices && menu.carCount() > 1;
        if (menu.isManager()) {
            typePrevious.visible = typeNext.visible = priceBox.visible = setPrice.visible = prices;
            return;
        }
        long cost = cost();
        boolean rentable = car && cost > 0;
        fewer.visible = more.visible = car;
        fewer.active = days > 1;
        more.active = days < RentalService.maxDays();
        pinBox.visible = cardButton.visible = cashButton.visible = rentable;
        setPayButtons(cardButton, cashButton, cost);
        cardButton.active = !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = cashOnHand() >= cost + salesTax(cost);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    @Override
    protected void onEnter() {
        if (priceBox != null && priceBox.visible && priceBox.isFocused()) {
            sendPrice();
        } else if (cardButton != null && cardButton.visible && cardButton.active) {
            cardButton.onPress();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    private static ItemStack carStack(CarType type, int color) {
        ItemStack stack = new ItemStack(ModItems.car(type));
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, false));
        return stack;
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
        if (tabs.length > 0) {
            drawSelectedTab(graphics, tabs[tab]);
        }
        if (menu.isManager() && tab == RentalMenu.TAB_PRICES) {
            renderPrices(graphics);
        } else {
            renderCar(graphics);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }

    private void renderCar(GuiGraphics graphics) {
        int top = top();
        CarType type = menu.carType();
        if (type == null) {
            lines(graphics, Component.translatable(menu.isManager() ? "gui.reikocars.fleet_empty" : "gui.reikocars.rental_none",
                    menu.ownerName(), (int) RentalService.lotRadius()), top, 5, MUTED_COLOR);
            return;
        }
        Component name = Component.translatable("entity.reikocars." + type.id());
        if (menu.carCount() > 1) {
            name = Component.translatable("gui.reikocars.vehicle_of", name, menu.carIndex() + 1, menu.carCount());
        }
        graphics.renderFakeItem(carStack(type, menu.carColor()), 8, top - 4);
        int right = imageWidth - 8 - (previous.visible ? 30 : 0);
        graphics.drawString(font, fit(name, right - 28), 28, top, LABEL_COLOR, false);
        graphics.drawString(font, CarSaleScreen.conditionLine(menu.carCondition(), menu.carFuel()), 28, top + 12, MUTED_COLOR, false);
        long price = menu.price(type);
        Component rate = price > 0 ? Component.translatable("gui.reikocars.rent_per_day", format(price))
                : Component.translatable("gui.reikocars.not_for_rent");
        graphics.drawString(font, fit(rate, imageWidth - 16), 8, top + 26, LABEL_COLOR, false);
        if (menu.isManager()) {
            Component status = menu.carRented()
                    ? Component.translatable("gui.reikocars.rented_out", RentalService.timeLeft(menu.minutesLeft() * 1_200L))
                    : Component.translatable("gui.reikocars.available");
            graphics.drawString(font, fit(status, imageWidth - 16), 8, top + 38, menu.carRented() ? 0xFFB26A00 : 0xFF2E7D32, false);
            lines(graphics, Component.translatable("gui.reikocars.fleet_hint", (int) RentalService.lotRadius()), top + 54, 4, MUTED_COLOR);
            lines(graphics, Component.translatable("gui.reikocars.rental_books", menu.rentals(), format(menu.earned())), top + 100, 2, MUTED_COLOR);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.reikocars.days"), 8, 64, LABEL_COLOR, false);
        Component count = Component.literal(Integer.toString(days));
        graphics.drawString(font, count, 69 - font.width(count) / 2, 64, LABEL_COLOR, false);
        long cost = cost();
        if (cost > 0) {
            long tax = salesTax(cost);
            Component total = tax > 0 ? Component.translatable("gui.reikocars.price_tax", format(cost + tax), format(tax))
                    : Component.translatable("gui.reikocars.price_is", format(cost));
            graphics.drawString(font, fit(total, imageWidth - 16), 8, 78, LABEL_COLOR, false);
            drawCardLine(graphics, 8, 92, imageWidth - 16);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                    8, 106, LABEL_COLOR, false);
            graphics.drawString(font, pin, pinLabelX, 106, LABEL_COLOR, false);
        }
        lines(graphics, Component.translatable("gui.reikocars.rental_hint", RentalService.maxDays()), 148, 2, MUTED_COLOR);
    }

    private void renderPrices(GuiGraphics graphics) {
        int top = top();
        CarType type = CarType.values()[priceType];
        Component name = Component.translatable("entity.reikocars." + type.id());
        graphics.renderFakeItem(carStack(type, type.defaultColor), (imageWidth - 16) / 2 - font.width(name) / 2 - 10, top);
        graphics.drawString(font, name, (imageWidth - 16) / 2 - font.width(name) / 2 + 10, top + 4, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.reikocars.rent_a_day"), 8, top + 28, LABEL_COLOR, false);
        int count = 0;
        for (CarType each : CarType.values()) {
            if (menu.price(each) <= 0) {
                count++;
            }
        }
        lines(graphics, Component.translatable("gui.reikocars.prices_hint", count), top + 48, 4, MUTED_COLOR);
        lines(graphics, Component.translatable("gui.reikocars.rental_books", menu.rentals(), format(menu.earned())), top + 100, 2, MUTED_COLOR);
    }
}
