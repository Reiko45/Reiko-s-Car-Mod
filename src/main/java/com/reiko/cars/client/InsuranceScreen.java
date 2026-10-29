package com.reiko.cars.client;

import com.reiko.cars.car.CarType;
import com.reiko.cars.insurance.InsuranceMenu;
import com.reiko.cars.insurance.InsuranceService;
import com.reiko.cars.network.InsuranceActionPayload;
import com.reiko.cars.registry.ModItems;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * An insurance office's screen. Customers pick one of their cars parked nearby, see its cover and the office's terms,
 * and buy a term of cover by card or cash. The owner sets the premium, deductible and claim limit, and sees the books.
 */
public final class InsuranceScreen extends ShopScreenBase<InsuranceMenu> {
    private static final int WIDTH = 194;
    private static final int TOP = 20;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int CARD_ROW = TOP + 78;
    private static final int PIN_ROW = TOP + 92;
    private static final int PAY_ROW = TOP + 106;

    private Button previous;
    private Button next;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private EditBox premiumBox;
    private EditBox deductibleBox;
    private EditBox limitBox;

    public InsuranceScreen(InsuranceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isOwner() ? 216 : 206);
    }

    @Override
    protected void init() {
        super.init();
        if (menu.isOwner()) {
            premiumBox = moneyBox(TOP + 2, menu.premium());
            deductibleBox = moneyBox(TOP + 22, menu.deductible());
            limitBox = moneyBox(TOP + 42, menu.limit());
            addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_prices"), button -> setTerms())
                    .bounds(leftPos + 8, topPos + TOP + 62, WIDTH - 16, 20).build());
            setInitialFocus(premiumBox);
            return;
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> pick(InsuranceMenu.PREVIOUS))
                .bounds(leftPos + imageWidth - 8 - 26, topPos + TOP + 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.previous_vehicle"))).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> pick(InsuranceMenu.NEXT))
                .bounds(leftPos + imageWidth - 8 - 12, topPos + TOP + 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.next_vehicle"))).build());
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(Payment.cash()))
                .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
        setPayButtons(cardButton, cashButton, menu.premium());
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
        updateWidgets();
    }

    private EditBox moneyBox(int y, long cents) {
        EditBox box = addRenderableWidget(new EditBox(font, leftPos + 120, topPos + y, 66, 16, Component.translatable("gui.reikocars.price")));
        box.setFilter(MONEY_FILTER);
        box.setMaxLength(16);
        box.setValue(String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100));
        return box;
    }

    private void setTerms() {
        PacketDistributor.sendToServer(InsuranceActionPayload.set(menu.containerId, premiumBox.getValue(), deductibleBox.getValue(), limitBox.getValue()));
    }

    private void buy(Payment payment) {
        PacketDistributor.sendToServer(InsuranceActionPayload.buy(menu.containerId, menu.premium(), payment));
    }

    private void pick(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private void updateWidgets() {
        if (menu.isOwner()) {
            return;
        }
        boolean car = menu.hasCar();
        previous.visible = next.visible = menu.carCount() > 1;
        long total = menu.premium() + salesTax(menu.premium());
        cardButton.visible = cashButton.visible = pinBox.visible = car;
        cardButton.active = car && !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = car && cashOnHand() >= total;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    @Override
    protected void onEnter() {
        if (menu.isOwner()) {
            setTerms();
        } else if (cardButton.visible && cardButton.active) {
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
        if (menu.isOwner()) {
            Component per = Component.translatable("gui.reikocars.premium_per", menu.termDays());
            graphics.drawString(font, fit(per, 108), 8, TOP + 6, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.reikocars.deductible"), 8, TOP + 26, LABEL_COLOR, false);
            graphics.drawString(font, Component.translatable("gui.reikocars.claim_limit"), 8, TOP + 46, LABEL_COLOR, false);
            lines(graphics, Component.translatable("gui.reikocars.insurance_books", menu.policies(), format(menu.premiums()), menu.claims(),
                    format(menu.paidOut())), TOP + 88, 2, MUTED_COLOR);
            lines(graphics, Component.translatable("gui.reikocars.insurer_hint", (int) InsuranceService.reach()), TOP + 112, 6, MUTED_COLOR);
            drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
            return;
        }
        CarType type = menu.carType();
        if (type == null) {
            lines(graphics, Component.translatable("gui.reikocars.insure_none", (int) InsuranceService.reach()), TOP + 2, 4, MUTED_COLOR);
        } else {
            Component name = Component.translatable("entity.reikocars." + type.id());
            if (menu.carCount() > 1) {
                name = Component.translatable("gui.reikocars.vehicle_of", name, menu.carIndex() + 1, menu.carCount());
            }
            graphics.renderFakeItem(carStack(type, menu.carColor()), 8, TOP - 2);
            int right = imageWidth - 8 - (previous.visible ? 30 : 0);
            graphics.drawString(font, fit(name, right - 28), 28, TOP + 2, LABEL_COLOR, false);
            Component status;
            int color;
            switch (menu.status()) {
                case InsuranceMenu.INSURED_HERE -> {
                    status = Component.translatable("gui.reikocars.insured_here", menu.daysLeft());
                    color = 0x1B7A2B;
                }
                case InsuranceMenu.INSURED_ELSEWHERE -> {
                    status = Component.translatable("gui.reikocars.insured_elsewhere", menu.daysLeft());
                    color = 0x9A6A00;
                }
                case InsuranceMenu.LAPSED -> {
                    status = Component.translatable("gui.reikocars.insurance_lapsed");
                    color = 0xB3261E;
                }
                default -> {
                    status = Component.translatable("gui.reikocars.not_insured");
                    color = MUTED_COLOR;
                }
            }
            graphics.drawString(font, fit(status, imageWidth - 36), 28, TOP + 13, color, false);
        }
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.insurance_terms", format(menu.premium()), menu.termDays()), imageWidth - 16),
                8, TOP + 28, LABEL_COLOR, false);
        lines(graphics, Component.translatable("gui.reikocars.insurance_cover", format(menu.deductible()), format(menu.limit())), TOP + 40, 2, MUTED_COLOR);
        if (type != null) {
            Component buying = Component.translatable(menu.status() == InsuranceMenu.INSURED_HERE ? "gui.reikocars.add_cover" : "gui.reikocars.buy_cover",
                    menu.termDays(), format(menu.premium() + salesTax(menu.premium())));
            graphics.drawString(font, fit(buying, imageWidth - 16), 8, TOP + 64, LABEL_COLOR, false);
            drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                    8, PIN_ROW, LABEL_COLOR, false);
            graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
        }
        lines(graphics, Component.translatable("gui.reikocars.insure_hint", menu.ownerName()), TOP + 132, 2, MUTED_COLOR);
        drawFeedback(graphics, 8, imageHeight - 32, imageWidth - 16, 3);
    }
}
