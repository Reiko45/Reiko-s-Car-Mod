package com.reiko.cars.client;

import com.reiko.cars.car.CarType;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.network.PumpActionPayload;
import com.reiko.cars.pump.PumpMenu;
import com.reiko.cars.pump.PumpService;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.TaxRates;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The gas pump screen. Built on the economy mod's shop screen, so paying looks and works the same: pick a card
 * (the ⇄ button switches when you carry several), type its PIN and press Card, or press Cash. The pump's owner
 * fills up free, and also gets a row for setting the price.
 */
public final class PumpScreen extends ShopScreenBase<PumpMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 200;
    private static final int OWNER_HEIGHT = 238;
    private static final int BAR_WIDTH = 160;
    private static final int FUEL_ROW = 18;
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

    /** Millibuckets to pump. */
    private int amount;
    /** Keep {@link #amount} at "fill it up" as the car and the stock change, until the player picks an amount. */
    private boolean fillUp = true;
    private long shownTotal = -1;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;
    private EditBox priceBox;

    public PumpScreen(PumpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isManager() ? OWNER_HEIGHT : HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("-"), button -> changeAmount(-1))
                .bounds(leftPos + 8, topPos + AMOUNT_ROW, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> changeAmount(1))
                .bounds(leftPos + 74, topPos + AMOUNT_ROW, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.fill_up"), button -> fillUp = true)
                .bounds(leftPos + 100, topPos + AMOUNT_ROW, 68, 20).build());

        pinBox = addRenderableWidget(createPinBox(leftPos + 32, topPos + PIN_ROW));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> fill(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 78, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> fill(Payment.cash()))
                .bounds(leftPos + 90, topPos + PAY_ROW, 78, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.pump_free"), button -> fill(Payment.cash()))
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

    /** 400 cents as "4.00", for the price box. */
    private static String plainAmount(long cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private int maxAmount() {
        return Math.min(menu.stock(), menu.carType() == null ? 0 : menu.carRoom());
    }

    private void changeAmount(int direction) {
        int step = hasShiftDown() ? PumpService.STEP : 5 * PumpService.STEP;
        int base = fillUp ? maxAmount() - maxAmount() % PumpService.STEP : amount;
        amount = Math.max(PumpService.STEP, Math.min(maxAmount(), base + direction * step));
        fillUp = false;
        updateButtons();
    }

    private void fill(Payment payment) {
        PacketDistributor.sendToServer(PumpActionPayload.fill(menu.containerId, amount, menu.price(), payment));
    }

    private void setPrice() {
        PacketDistributor.sendToServer(PumpActionPayload.setPrice(menu.containerId, priceBox.getValue()));
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
        return PumpService.cost(menu.price(), amount);
    }

    private void updateButtons() {
        int max = maxAmount();
        amount = fillUp ? max : Math.min(amount, max);
        long total = total();
        if (total != shownTotal) {
            shownTotal = total;
            setPayButtons(cardButton, cashButton, total);
        }
        boolean canPump = amount > 0;
        cardButton.active = canPump && !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = canPump && cashOnHand() >= total + salesTax(total);
        freeButton.active = canPump;
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
        // Fuel in the pump, in its own colour.
        Fluid fluid = menu.fluid();
        drawBar(graphics, barX, topPos + FUEL_ROW + 10, (float) menu.stock() / menu.capacity(), fluidColor(fluid));
        // The car's tank.
        if (menu.carType() != null) {
            drawBar(graphics, barX, topPos + CAR_ROW + 10, menu.carFuel(), menu.carFuel() < 0.15F ? 0xFFE53935 : 0xFF43A047);
        }
        // The amount being bought.
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

    private static int fluidColor(Fluid fluid) {
        int tint = IClientFluidTypeExtensions.of(fluid).getTintColor();
        return tint == 0xFFFFFFFF ? 0xFFE0C040 : 0xFF000000 | tint;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);

        Fluid fluid = menu.fluid();
        Component stock = Component.translatable("gui.reikocars.buckets_of", PumpService.buckets(menu.stock()),
                PumpService.buckets(menu.capacity()));
        Component fuelName = menu.stock() > 0 ? new FluidStack(fluid, 1).getHoverName()
                : Component.translatable("gui.reikocars.pump_empty");
        graphics.drawString(font, fit(fuelName, imageWidth - 24 - font.width(stock)), 8, FUEL_ROW, LABEL_COLOR, false);
        graphics.drawString(font, stock, imageWidth - 8 - font.width(stock), FUEL_ROW, MUTED_COLOR, false);

        long basisPoints = EconomyConfig.taxRates().salesBasisPoints();
        Component price = menu.price() == 0
                ? Component.translatable("gui.reikocars.price_free")
                : basisPoints > 0
                        ? Component.translatable("gui.reikocars.price_per_bucket_tax", format(menu.price()), TaxRates.percentText(basisPoints))
                        : Component.translatable("gui.reikocars.price_per_bucket", format(menu.price()));
        graphics.drawString(font, price, 8, PRICE_ROW, LABEL_COLOR, false);

        CarType car = menu.carType();
        if (car == null) {
            Component none = Component.translatable("gui.reikocars.no_car", (int) CarsConfig.get(CarsConfig.HOSE_LENGTH));
            graphics.drawString(font, fit(none, imageWidth - 16), 8, CAR_ROW, 0xB3261E, false);
        } else {
            Component carName = Component.translatable("entity.reikocars." + car.id());
            Component tank = menu.carRoom() > 0
                    ? Component.translatable("gui.reikocars.car_room", Math.round(menu.carFuel() * 100), PumpService.buckets(menu.carRoom()))
                    : Component.translatable("gui.reikocars.car_full", Math.round(menu.carFuel() * 100));
            Component shownTank = fit(tank, imageWidth - 22 - font.width(carName));
            graphics.drawString(font, carName, 8, CAR_ROW, LABEL_COLOR, false);
            graphics.drawString(font, shownTank, imageWidth - 8 - font.width(shownTank), CAR_ROW, MUTED_COLOR, false);
        }

        String shown = PumpService.buckets(amount) + " B";
        graphics.drawString(font, shown, 51 - font.width(shown) / 2, AMOUNT_ROW + 6, 0xFFFFFF, true);

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
            graphics.drawString(font, Component.translatable("gui.reikocars.your_pump"), 8, CARD_ROW, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, FEEDBACK_ROW, imageWidth - 16, FEEDBACK_LINES);

        if (menu.isManager()) {
            graphics.drawString(font, Component.translatable("gui.reikocars.price_label"), 8, OWNER_ROW + 4, LABEL_COLOR, false);
            Component sold = Component.translatable("gui.reikocars.sold", PumpService.buckets((int) Math.min(Integer.MAX_VALUE, menu.millibucketsSold())),
                    format(menu.earned()));
            graphics.drawString(font, fit(sold, imageWidth - 16), 8, OWNER_ROW + 21, MUTED_COLOR, false);
        }
    }
}
