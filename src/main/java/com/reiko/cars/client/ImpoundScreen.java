package com.reiko.cars.client;

import com.reiko.cars.car.CarType;
import com.reiko.cars.impound.ImpoundMenu;
import com.reiko.cars.impound.ImpoundService;
import com.reiko.cars.network.LotActionPayload;
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
 * An impound office's screen. A car owner sees their cars held here and pays to get one back, by card (with PIN) or
 * cash; the lot's owner sees every car held here, can let a towed one go, and sets the release fee.
 */
public final class ImpoundScreen extends ShopScreenBase<ImpoundMenu> {
    private static final int WIDTH = 194;
    private static final int TOP = 20;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int CARD_ROW = 80;
    private static final int PIN_ROW = 94;
    private static final int PAY_ROW = 110;
    private static final int FEE_ROW = 108;

    private Button previous;
    private Button next;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button letGo;
    private EditBox feeBox;
    private Button setFee;

    public ImpoundScreen(ImpoundMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isManager() ? 172 : 168);
    }

    @Override
    protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> pick(ImpoundMenu.PREVIOUS))
                .bounds(leftPos + imageWidth - 8 - 26, topPos + TOP - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.previous_vehicle"))).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> pick(ImpoundMenu.NEXT))
                .bounds(leftPos + imageWidth - 8 - 12, topPos + TOP - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.next_vehicle"))).build());
        if (menu.isManager()) {
            letGo = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.let_it_go"),
                    button -> send(LotActionPayload.pay(menu.containerId, LotActionPayload.RELEASE_FREE, 0, Payment.cash(), 0)))
                    .bounds(leftPos + 8, topPos + 78, WIDTH - 16, 20).build());
            feeBox = addRenderableWidget(new EditBox(font, leftPos + 80, topPos + FEE_ROW - 4, 60, 16, Component.translatable("gui.reikocars.price")));
            feeBox.setFilter(MONEY_FILTER);
            feeBox.setMaxLength(16);
            feeBox.setValue(plain(menu.fee()));
            setFee = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"),
                    button -> send(LotActionPayload.text(menu.containerId, LotActionPayload.SET_FEE, feeBox.getValue(), 0)))
                    .bounds(leftPos + 144, topPos + FEE_ROW - 5, 42, 18).build());
        } else {
            pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW - 4));
            cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(cardPayment(pinBox.getValue())))
                    .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
            cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(Payment.cash()))
                    .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
            addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
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

    private void pay(Payment payment) {
        send(LotActionPayload.pay(menu.containerId, LotActionPayload.RELEASE, menu.cost(), payment, 0));
    }

    /** What getting the picked car back costs, with tax on the fee. */
    private long total() {
        return menu.cost() + salesTax(Math.max(0, menu.cost() - menu.arrears()));
    }

    private void updateWidgets() {
        boolean car = menu.hasCar();
        previous.visible = next.visible = menu.carCount() > 1;
        if (menu.isManager()) {
            letGo.visible = car;
            letGo.active = car && !menu.repossessed();
            return;
        }
        boolean owes = car && menu.cost() > 0;
        pinBox.visible = cardButton.visible = owes;
        cashButton.visible = car;
        cardButton.setMessage(Component.translatable("gui.reikoeconomy.pay_card", format(total())));
        cashButton.setMessage(owes ? Component.translatable("gui.reikoeconomy.pay_cash", format(total()))
                : Component.translatable("gui.reikocars.take_it_back"));
        cardButton.active = !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = !owes || cashOnHand() >= total();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    @Override
    protected void onEnter() {
        if (feeBox != null && feeBox.isFocused()) {
            setFee.onPress();
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
        CarType type = menu.carType();
        if (type == null) {
            lines(graphics, Component.translatable(menu.isManager() ? "gui.reikocars.impound_empty" : "gui.reikocars.impound_none_yours",
                    (int) ImpoundService.lotRadius()), TOP, 4, MUTED_COLOR);
        } else {
            Component name = Component.translatable("entity.reikocars." + type.id());
            if (menu.carCount() > 1) {
                name = Component.translatable("gui.reikocars.vehicle_of", name, menu.carIndex() + 1, menu.carCount());
            }
            graphics.renderFakeItem(carStack(type, menu.carColor()), 8, TOP - 4);
            int right = imageWidth - 8 - (previous.visible ? 30 : 0);
            graphics.drawString(font, fit(name, right - 28), 28, TOP, LABEL_COLOR, false);
            String key = menu.repossessed() ? "gui.reikocars.impound_repossessed" : "gui.reikocars.impound_towed";
            int days = menu.daysHeld();
            Component why = Component.translatable(key, Component.translatable(days == 0 ? "gui.reikocars.held_today"
                    : days == 1 ? "gui.reikocars.held_yesterday" : "gui.reikocars.held_days_ago", days));
            lines(graphics, why, TOP + 14, 2, MUTED_COLOR);
            long fee = menu.cost() - menu.arrears();
            Component cost = menu.arrears() > 0
                    ? Component.translatable("gui.reikocars.impound_cost_loan", format(fee), format(menu.arrears()))
                    : Component.translatable("gui.reikocars.impound_cost", format(fee));
            lines(graphics, cost, TOP + 36, 2, LABEL_COLOR);
        }
        if (menu.isManager()) {
            graphics.drawString(font, Component.translatable("gui.reikocars.release_fee"), 8, FEE_ROW, LABEL_COLOR, false);
            lines(graphics, Component.translatable("gui.reikocars.impound_books", menu.impounded(), menu.released(), format(menu.earned())),
                    FEE_ROW + 18, 2, MUTED_COLOR);
        } else if (menu.hasCar() && menu.cost() > 0) {
            drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                    8, PIN_ROW, LABEL_COLOR, false);
            graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }
}
