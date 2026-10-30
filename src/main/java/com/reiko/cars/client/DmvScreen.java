package com.reiko.cars.client;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.dmv.DmvMenu;
import com.reiko.cars.dmv.DmvOfficeBlockEntity;
import com.reiko.cars.dmv.DmvService;
import com.reiko.cars.license.LicenseService;
import com.reiko.cars.network.LotActionPayload;
import com.reiko.cars.plate.Plates;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.rental.RentalService;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import java.util.Locale;
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
 * A DMV's screen. License: your license and its points, and the driving test (or a new card), paid by card (with
 * PIN) or cash. Plates: a custom plate for one of your cars parked nearby. Settings (the owner): the three fees, the
 * test route and the books.
 */
public final class DmvScreen extends ShopScreenBase<DmvMenu> {
    private static final int WIDTH = 194;
    private static final int TABS_ROW = 16;
    private static final int TOP = 34;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int CARD_ROW = 106;
    private static final int PIN_ROW = 120;
    private static final int PAY_ROW = 134;
    private static final int GOOD = 0xFF2E7D32;
    private static final int BAD = 0xFFC62828;
    private static final int WARN = 0xFFB26A00;

    private int tab = DmvMenu.TAB_LICENSE;
    private Button[] tabs = new Button[0];
    private Button previous;
    private Button next;
    private EditBox plateBox;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;
    private final EditBox[] feeBoxes = new EditBox[3];
    private final Button[] feeButtons = new Button[3];

    public DmvScreen(DmvMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 182);
    }

    @Override
    protected void init() {
        super.init();
        String[] keys = menu.isManager() ? new String[] {"gui.reikocars.tab_license", "gui.reikocars.tab_plates", "gui.reikocars.tab_settings"}
                : new String[] {"gui.reikocars.tab_license", "gui.reikocars.tab_plates"};
        int[] widths = menu.isManager() ? new int[] {58, 58, 58} : new int[] {58, 58};
        tabs = addTabs(8, TABS_ROW, keys, widths, this::selectTab);
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> pick(DmvMenu.PREVIOUS))
                .bounds(leftPos + imageWidth - 8 - 26, topPos + TOP - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.previous_vehicle"))).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> pick(DmvMenu.NEXT))
                .bounds(leftPos + imageWidth - 8 - 12, topPos + TOP - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.next_vehicle"))).build());
        plateBox = addRenderableWidget(new EditBox(font, leftPos + 74, topPos + 62, 70, 16, Component.translatable("gui.reikocars.new_plate")));
        plateBox.setMaxLength(Plates.MAX_LENGTH);
        plateBox.setFilter(text -> text.matches("[A-Za-z0-9 ]*"));
        plateBox.setResponder(text -> {
            String upper = text.toUpperCase(Locale.ROOT);
            if (!upper.equals(text)) {
                plateBox.setValue(upper);
            }
        });
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(Payment.cash()))
                .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.empty(), button -> pay(Payment.cash()))
                .bounds(leftPos + 8, topPos + PAY_ROW, WIDTH - 16, 20).build());
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
        for (int i = 0; i < 3; i++) {
            int which = i;
            feeBoxes[i] = addRenderableWidget(new EditBox(font, leftPos + 96, topPos + 38 + i * 20, 52, 16, Component.translatable("gui.reikocars.price")));
            feeBoxes[i].setFilter(MONEY_FILTER);
            feeBoxes[i].setMaxLength(16);
            feeBoxes[i].setValue(plain(menu.info().fees()[i]));
            feeButtons[i] = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"),
                    button -> send(LotActionPayload.text(menu.containerId, LotActionPayload.SET_DMV_FEE, feeBoxes[which].getValue(), which)))
                    .bounds(leftPos + 152, topPos + 37 + i * 20, 34, 18).build());
        }
        updateWidgets();
    }

    private static String plain(long cents) {
        return String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
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

    /** What the License tab's payment is for: the test (no license) or a new card (any license); -1 for neither. */
    private int licenseAction() {
        DmvMenu.Info info = menu.info();
        if (info.onTest()) {
            return -1;
        }
        if (info.status() == LicenseService.Status.NONE) {
            return info.routeCones() > 0 ? LotActionPayload.TAKE_TEST : -1;
        }
        return LotActionPayload.REPLACE_LICENSE;
    }

    /** What the viewer pays (before tax) for what this tab sells: nothing at their own DMV. */
    private long price() {
        DmvMenu.Info info = menu.info();
        int which = tab == DmvMenu.TAB_PLATES ? DmvOfficeBlockEntity.PLATE_FEE
                : licenseAction() == LotActionPayload.TAKE_TEST ? DmvOfficeBlockEntity.TEST_FEE : DmvOfficeBlockEntity.REPLACE_FEE;
        return info.manager() ? 0 : info.fees()[which];
    }

    private void pay(Payment payment) {
        if (tab == DmvMenu.TAB_PLATES) {
            send(new LotActionPayload(menu.containerId, LotActionPayload.BUY_PLATE, price(), payment, plateBox.getValue(), 0));
        } else if (licenseAction() >= 0) {
            send(LotActionPayload.pay(menu.containerId, licenseAction(), price(), payment, 0));
        }
    }

    private void updateWidgets() {
        boolean license = tab == DmvMenu.TAB_LICENSE;
        boolean plates = tab == DmvMenu.TAB_PLATES;
        boolean settings = tab == DmvMenu.TAB_SETTINGS;
        previous.visible = next.visible = plates && menu.carCount() > 1;
        plateBox.visible = plates && menu.hasCar();
        boolean selling = license && licenseAction() >= 0 || plates && menu.hasCar();
        long price = price();
        boolean free = selling && price == 0;
        pinBox.visible = cardButton.visible = cashButton.visible = selling && !free;
        freeButton.visible = free;
        if (selling && !free) {
            setPayButtons(cardButton, cashButton, price);
            cardButton.active = !chosenCard().isEmpty() && pinBox.getValue().length() == 4 && (!plates || !plateBox.getValue().isBlank());
            cashButton.active = cashOnHand() >= price + salesTax(price) && (!plates || !plateBox.getValue().isBlank());
        }
        if (free) {
            freeButton.setMessage(Component.translatable(plates ? "gui.reikocars.free_plate"
                    : licenseAction() == LotActionPayload.TAKE_TEST ? "gui.reikocars.free_test" : "gui.reikocars.free_card"));
            freeButton.active = !plates || !plateBox.getValue().isBlank();
        }
        for (int i = 0; i < 3; i++) {
            feeBoxes[i].visible = feeButtons[i].visible = settings;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    @Override
    protected void onEnter() {
        for (int i = 0; i < 3; i++) {
            if (feeBoxes[i].visible && feeBoxes[i].isFocused()) {
                feeButtons[i].onPress();
                return;
            }
        }
        if (cardButton.visible && cardButton.active) {
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
        drawSelectedTab(graphics, tabs[tab]);
        switch (tab) {
            case DmvMenu.TAB_PLATES -> renderPlates(graphics);
            case DmvMenu.TAB_SETTINGS -> renderSettings(graphics);
            default -> renderLicense(graphics);
        }
        if (pinBox.visible) {
            drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                    8, PIN_ROW, LABEL_COLOR, false);
            graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
        }
        drawFeedback(graphics, 8, imageHeight - 24, imageWidth - 16, 2);
    }

    private void renderLicense(GuiGraphics graphics) {
        DmvMenu.Info info = menu.info();
        switch (info.status()) {
            case VALID -> {
                graphics.drawString(font, fit(Component.translatable("gui.reikocars.your_license", info.number()), imageWidth - 16), 8, TOP, LABEL_COLOR,
                        false);
                graphics.drawString(font, Component.translatable("gui.reikocars.license_points", info.points(), info.maxPoints()), 8, TOP + 12,
                        info.points() == 0 ? GOOD : WARN, false);
            }
            case SUSPENDED -> {
                graphics.drawString(font, fit(Component.translatable("gui.reikocars.license_suspended", info.number()), imageWidth - 16), 8, TOP, BAD,
                        false);
                graphics.drawString(font, Component.translatable("gui.reikocars.suspended_for", RentalService.timeLeft(info.suspendedLeft())), 8,
                        TOP + 12, BAD, false);
            }
            default -> graphics.drawString(font, fit(Component.translatable(info.onTest() ? "gui.reikocars.on_test" : "gui.reikocars.no_license"),
                    imageWidth - 16), 8, TOP, info.onTest() ? WARN : LABEL_COLOR, false);
        }
        Component about = info.routeCones() > 0
                ? Component.translatable("gui.reikocars.test_about", info.routeCones(), info.testMinutes())
                : Component.translatable("gui.reikocars.test_no_route", info.ownerName());
        lines(graphics, about, TOP + 28, 3, MUTED_COLOR);
        int action = licenseAction();
        if (action >= 0) {
            long price = price();
            Component what = Component.translatable(action == LotActionPayload.TAKE_TEST ? "gui.reikocars.buy_test" : "gui.reikocars.buy_card",
                    price == 0 ? Component.translatable("gui.reikocars.price_free_short") : Component.literal(format(price)));
            graphics.drawString(font, fit(what, imageWidth - 16), 8, CARD_ROW - 12, LABEL_COLOR, false);
        }
    }

    private void renderPlates(GuiGraphics graphics) {
        CarType type = menu.carType();
        if (type == null) {
            lines(graphics, Component.translatable("gui.reikocars.plates_none", (int) DmvService.reach()), TOP, 4, MUTED_COLOR);
            return;
        }
        Component name = Component.translatable("entity.reikocars." + type.id());
        if (menu.carCount() > 1) {
            name = Component.translatable("gui.reikocars.vehicle_of", name, menu.carIndex() + 1, menu.carCount());
        }
        ItemStack stack = new ItemStack(ModItems.car(type));
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(menu.carColor(), false));
        graphics.renderFakeItem(stack, 8, TOP - 4);
        int right = imageWidth - 8 - (previous.visible ? 30 : 0);
        graphics.drawString(font, fit(name, right - 28), 28, TOP, LABEL_COLOR, false);
        String plate = minecraft.level != null && minecraft.level.getEntity(menu.carEntityId()) instanceof CarEntity car ? car.plate() : "";
        graphics.drawString(font, Component.translatable("gui.reikocars.current_plate", plate.isEmpty() ? "-" : plate), 28, TOP + 12, MUTED_COLOR,
                false);
        graphics.drawString(font, Component.translatable("gui.reikocars.new_plate"), 8, 66, LABEL_COLOR, false);
        long price = price();
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.buy_plate",
                price == 0 ? Component.translatable("gui.reikocars.price_free_short") : Component.literal(format(price)), Plates.MAX_LENGTH),
                imageWidth - 16), 8, CARD_ROW - 12, LABEL_COLOR, false);
    }

    private void renderSettings(GuiGraphics graphics) {
        String[] keys = {"gui.reikocars.fee_test", "gui.reikocars.fee_card", "gui.reikocars.fee_plate"};
        for (int i = 0; i < 3; i++) {
            graphics.drawString(font, Component.translatable(keys[i]), 8, 42 + i * 20, LABEL_COLOR, false);
        }
        DmvMenu.Info info = menu.info();
        Component route = info.routeCones() > 0 ? Component.translatable("gui.reikocars.route_cones", info.routeCones())
                : Component.translatable("gui.reikocars.route_none");
        lines(graphics, route, 102, 2, info.routeCones() > 0 ? GOOD : WARN);
        lines(graphics, Component.translatable("gui.reikocars.dmv_books", info.tests(), info.passes(), info.plates(), format(info.earned())), 126, 2,
                MUTED_COLOR);
    }
}
