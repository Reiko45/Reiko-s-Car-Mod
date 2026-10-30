package com.reiko.cars.client;

import com.reiko.cars.network.TuningActionPayload;
import com.reiko.cars.tuning.Livery;
import com.reiko.cars.tuning.TuningBenchMenu;
import com.reiko.cars.tuning.TuningKit;
import com.reiko.cars.tuning.TuningService;
import com.reiko.cars.tuning.Upgrades;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Tuning Bench screen. Tune: the car beside the bench kit by kit, how far it's tuned, a Fit button that fits a kit
 * from the player's inventory for nothing, and a button with the bench's price for one from its stock; picked kits
 * add up to a bill paid by card (with PIN) or cash. Livery: a race number and stripes, free. The owner also gets
 * Stock and Prices tabs.
 */
public final class TuningBenchScreen extends ShopScreenBase<TuningBenchMenu> {
    private static final int WIDTH = 194;
    private static final int TABS_ROW = 16;
    private static final int TOP = 32;
    private static final int ROW_HEIGHT = 15;
    private static final int FIT_X = 96;
    private static final int SHOP_X = 130;
    private static final int SHOP_WIDTH = 56;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final TuningKit[] KITS = TuningKit.values();
    /** The kits in the bench's order (tires together). */
    private static final java.util.List<TuningKit> ORDER = TuningKit.ORDER;
    private static final int BAD = 0xFFB3261E;

    @Nullable
    private static BlockPos reopenPricesAt;

    private int tab = TuningBenchMenu.TAB_TUNE;
    private final boolean[] chosen = new boolean[KITS.length];
    private final Button[] fitButtons = new Button[KITS.length];
    private final Button[] shopButtons = new Button[KITS.length];
    private final EditBox[] priceBoxes = new EditBox[KITS.length];
    private final Button[] setButtons = new Button[KITS.length];
    private Button[] tabs = new Button[0];
    private Button previousButton;
    private Button nextButton;
    private String shownTarget = "";
    private long shownTotal = -1;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;
    // the livery tab
    private EditBox numberBox;
    private Button stripesBack;
    private Button stripesOn;
    private Button applyLivery;
    private int stripes;

    public TuningBenchScreen(TuningBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 273);
    }

    private int kitsRow() {
        return TOP + 12;
    }

    private int totalRow() {
        return kitsRow() + (KITS.length - 1) * ROW_HEIGHT + 3; // a fuel tank or a battery: one row short of every kit
    }

    /** The kits on the Tune tab, in order: those that suit the car (a fuel tank or a battery, never both). */
    private java.util.List<TuningKit> shown() {
        com.reiko.cars.car.CarType type = menu.carType();
        return ORDER.stream().filter(kit -> type == null ? kit != TuningKit.BATTERY : kit.suits(type)).toList();
    }

    private int cardRow() {
        return totalRow() + 12;
    }

    private int cashRow() {
        return cardRow() + 12;
    }

    private int payRow() {
        return cashRow() + 20;
    }

    private int feedbackRow() {
        return payRow() + 24;
    }

    @Override
    protected void init() {
        super.init();
        tabs = menu.isManager()
                ? addTabs(8, TABS_ROW, new String[] {"gui.reikocars.tab_tune", "gui.reikocars.tab_livery", "gui.reikocars.tab_stock", "gui.reikocars.tab_prices"},
                new int[] {43, 43, 43, 43}, this::selectTab)
                : addTabs(8, TABS_ROW, new String[] {"gui.reikocars.tab_tune", "gui.reikocars.tab_livery"}, new int[] {58, 58}, this::selectTab);
        previousButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> pickCar(TuningBenchMenu.PREVIOUS))
                .bounds(leftPos + imageWidth - 8 - 26, topPos + TOP - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.previous_vehicle"))).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> pickCar(TuningBenchMenu.NEXT))
                .bounds(leftPos + imageWidth - 8 - 12, topPos + TOP - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.next_vehicle"))).build());
        for (TuningKit kit : KITS) {
            int i = kit.ordinal();
            int y = topPos + kitsRow() + i * ROW_HEIGHT;
            fitButtons[i] = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.fit"), button -> send(TuningActionPayload.simple(
                    menu.containerId, TuningActionPayload.FIT, kit.ordinal()))).bounds(leftPos + FIT_X, y + 1, 30, 14).build());
            shopButtons[i] = addRenderableWidget(Button.builder(Component.empty(), button -> choose(kit))
                    .bounds(leftPos + SHOP_X, y + 1, SHOP_WIDTH, 14).build());
            int priceY = topPos + TOP + 14 + ORDER.indexOf(kit) * 20;
            priceBoxes[i] = addRenderableWidget(new EditBox(font, leftPos + 108, priceY + 1, 46, 16, Component.translatable("gui.reikocars.price")));
            priceBoxes[i].setFilter(MONEY_FILTER);
            priceBoxes[i].setMaxLength(16);
            priceBoxes[i].setValue(plain(menu.price(kit)));
            setButtons[i] = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"), button -> setPrice(kit))
                    .bounds(leftPos + 158, priceY, 28, 18).build());
        }
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + cashRow()));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + payRow(), 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(Payment.cash()))
                .bounds(leftPos + 99, topPos + payRow(), 87, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.tune_free"), button -> buy(Payment.cash()))
                .bounds(leftPos + 8, topPos + payRow(), WIDTH - 16, 20).build());
        addCardSwitch(leftPos + imageWidth - 22, topPos + cardRow() - 2);

        numberBox = addRenderableWidget(new EditBox(font, leftPos + 110, topPos + TOP + 22, 30, 16, Component.translatable("gui.reikocars.race_number")));
        numberBox.setFilter(text -> text.matches("[0-9]{0,2}"));
        numberBox.setMaxLength(2);
        stripesBack = addRenderableWidget(Button.builder(Component.literal("<"), button -> stripes = Math.floorMod(stripes - 1, DyeColor.values().length + 1))
                .bounds(leftPos + 110, topPos + TOP + 45, 12, 14).build());
        stripesOn = addRenderableWidget(Button.builder(Component.literal(">"), button -> stripes = Math.floorMod(stripes + 1, DyeColor.values().length + 1))
                .bounds(leftPos + imageWidth - 20, topPos + TOP + 45, 12, 14).build());
        applyLivery = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.apply_livery"), button -> applyLivery())
                .bounds(leftPos + 8, topPos + TOP + 70, WIDTH - 16, 20).build());
        resetLivery();

        if (menu.isManager() && menu.pos().equals(reopenPricesAt)) {
            tab = TuningBenchMenu.TAB_PRICES;
        }
        reopenPricesAt = null;
        selectTab(tab);
    }

    private static String plain(long cents) {
        return String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void send(TuningActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    private void pickCar(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private void selectTab(int selected) {
        if (selected != tab) {
            clearFeedback();
        }
        tab = selected;
        menu.setTab(selected);
        updateButtons();
    }

    private void resetLivery() {
        Livery livery = menu.livery();
        numberBox.setValue(livery.number() == 0 ? "" : Integer.toString(livery.number()));
        stripes = livery.stripes();
    }

    private void applyLivery() {
        int number = numberBox.getValue().isEmpty() ? 0 : Integer.parseInt(numberBox.getValue());
        send(TuningActionPayload.simple(menu.containerId, TuningActionPayload.LIVERY, new Livery(number, stripes).pack()));
    }

    private void choose(TuningKit kit) {
        chosen[kit.ordinal()] = !chosen[kit.ordinal()];
        if (chosen[kit.ordinal()]) {
            // One set of tires, one set of springs.
            for (TuningKit other : KITS) {
                if (other != kit && (other.isTires() && kit.isTires() || kit == TuningKit.LOWERING && other == TuningKit.LIFT
                        || kit == TuningKit.LIFT && other == TuningKit.LOWERING)) {
                    chosen[other.ordinal()] = false;
                }
            }
        }
        updateButtons();
    }

    /** Whether the bench could fit {@code kit} from its stock now. */
    private boolean benchCan(TuningKit kit) {
        return menu.hasTarget() && menu.mayTune() && kit.suits(menu.carType()) && kit.fit(menu.upgrades()) != null && menu.stock(kit) > 0;
    }

    private int chosenMask() {
        int mask = 0;
        for (TuningKit kit : KITS) {
            if (chosen[kit.ordinal()] && benchCan(kit)) {
                mask |= 1 << kit.ordinal();
            }
        }
        return mask;
    }

    private long total() {
        long total = 0;
        for (TuningKit kit : KITS) {
            if (chosen[kit.ordinal()] && benchCan(kit)) {
                total += menu.price(kit);
            }
        }
        return total;
    }

    private void buy(Payment payment) {
        send(new TuningActionPayload(menu.containerId, TuningActionPayload.BUY, chosenMask(), total(), payment, ""));
    }

    private void setPrice(TuningKit kit) {
        reopenPricesAt = menu.pos();
        send(new TuningActionPayload(menu.containerId, TuningActionPayload.SET_PRICE, kit.ordinal(), 0, Payment.cash(),
                priceBoxes[kit.ordinal()].getValue()));
    }

    @Override
    protected void onEnter() {
        for (TuningKit kit : KITS) {
            if (priceBoxes[kit.ordinal()].visible && priceBoxes[kit.ordinal()].isFocused()) {
                setPrice(kit);
                return;
            }
        }
        if (numberBox.visible && numberBox.isFocused()) {
            applyLivery();
        } else if (cardButton.visible && cardButton.active) {
            cardButton.onPress();
        }
    }

    private void updateButtons() {
        boolean tune = tab == TuningBenchMenu.TAB_TUNE;
        boolean livery = tab == TuningBenchMenu.TAB_LIVERY;
        boolean prices = tab == TuningBenchMenu.TAB_PRICES;
        boolean hasCar = menu.hasTarget();
        String target = menu.carType() + "#" + menu.targetIndex() + "#" + menu.carCount();
        if (!target.equals(shownTarget)) {
            shownTarget = target;
            java.util.Arrays.fill(chosen, false);
            resetLivery();
        }
        previousButton.visible = nextButton.visible = (tune || livery) && menu.carCount() > 1;
        Upgrades upgrades = menu.upgrades();
        java.util.List<TuningKit> shown = shown();
        for (TuningKit kit : KITS) {
            int i = kit.ordinal();
            int row = shown.indexOf(kit);
            fitButtons[i].visible = shopButtons[i].visible = tune && row >= 0;
            fitButtons[i].setY(topPos + kitsRow() + Math.max(0, row) * ROW_HEIGHT + 1);
            shopButtons[i].setY(topPos + kitsRow() + Math.max(0, row) * ROW_HEIGHT + 1);
            priceBoxes[i].visible = setButtons[i].visible = prices;
            boolean fits = hasCar && menu.mayTune() && kit.suits(menu.carType()) && kit.fit(upgrades) != null;
            int carried = TuningService.carried(minecraft.player, kit);
            fitButtons[i].active = fits && carried > 0;
            fitButtons[i].setTooltip(Tooltip.create(!fits ? Component.translatable(hasCar && !kit.suits(menu.carType()) ? "gui.reikocars.kit_unsuited"
                    : hasCar && kit.fit(upgrades) == null ? "gui.reikocars.kit_maxed"
                    : "gui.reikocars.fit_nothing") : Component.translatable("gui.reikocars.kit_fit_tooltip", kit.displayName(), carried)));
            boolean can = benchCan(kit);
            if (!can) {
                chosen[i] = false;
            }
            shopButtons[i].active = can;
            long price = menu.price(kit);
            Component label = !fits ? Component.literal("—")
                    : menu.stock(kit) <= 0 ? Component.translatable("gui.reikocars.no_stock")
                    : Component.literal((chosen[i] ? "✔ " : "") + (menu.isOwner() || price == 0 ? "" : format(price)))
                    .append(menu.isOwner() || price == 0 ? Component.translatable("gui.reikocars.shop_free") : Component.empty());
            shopButtons[i].setMessage(label);
            shopButtons[i].setTooltip(Tooltip.create(Component.translatable("gui.reikocars.kit_shop_tooltip", kit.displayName(), format(price),
                    menu.stock(kit))));
        }
        boolean free = menu.isOwner();
        pinBox.visible = cardButton.visible = cashButton.visible = tune && !free;
        freeButton.visible = tune && free;
        long total = total();
        if (total != shownTotal) {
            shownTotal = total;
            setPayButtons(cardButton, cashButton, total);
        }
        boolean any = chosenMask() != 0;
        cardButton.active = any && !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = any && cashOnHand() >= total + salesTax(total);
        freeButton.active = any;
        numberBox.visible = stripesBack.visible = stripesOn.visible = applyLivery.visible = livery;
        applyLivery.active = hasCar && menu.mayTune();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        if (tab == TuningBenchMenu.TAB_TUNE) {
            for (int row = 0; row < shown().size(); row++) {
                drawSlot(graphics, leftPos + 8, topPos + kitsRow() + row * ROW_HEIGHT);
            }
            if (!menu.isOwner()) {
                graphics.fill(leftPos + 6, topPos + totalRow() - 4, leftPos + imageWidth - 6, topPos + totalRow() - 3, 0xFF8B8B8B);
            }
        } else if (tab == TuningBenchMenu.TAB_STOCK) {
            drawSlotGrid(graphics, leftPos + TuningBenchMenu.STOCK_X, topPos + TuningBenchMenu.STOCK_Y, 9, 2);
            drawPlayerInventory(graphics, leftPos + TuningBenchMenu.STOCK_X, topPos + TuningBenchMenu.INVENTORY_Y);
        } else if (tab == TuningBenchMenu.TAB_PRICES) {
            for (int row = 0; row < ORDER.size(); row++) {
                drawSlot(graphics, leftPos + 8, topPos + TOP + 15 + row * 20);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        if (tab < tabs.length) {
            drawSelectedTab(graphics, tabs[tab]);
        }
        switch (tab) {
            case TuningBenchMenu.TAB_LIVERY -> renderLivery(graphics);
            case TuningBenchMenu.TAB_STOCK -> renderStock(graphics);
            case TuningBenchMenu.TAB_PRICES -> renderPrices(graphics);
            default -> renderTune(graphics);
        }
    }

    /** The car's name (and which of how many), or why there's none. */
    private void carLine(GuiGraphics graphics) {
        if (!menu.hasTarget()) {
            graphics.drawString(font, fit(Component.translatable("gui.reikocars.tune_no_car", (int) TuningService.reach()), imageWidth - 16), 8, TOP, BAD,
                    false);
            return;
        }
        Component name = Component.translatable("entity.reikocars." + menu.carType().id());
        if (menu.carCount() > 1) {
            name = Component.translatable("gui.reikocars.vehicle_of", name, menu.targetIndex() + 1, menu.carCount());
        }
        int right = imageWidth - 8 - (previousButton.visible ? 30 : 0);
        if (!menu.mayTune()) {
            name = Component.translatable("gui.reikocars.not_yours_to_tune", name);
        }
        graphics.drawString(font, fit(name, right - 8), 8, TOP, menu.mayTune() ? LABEL_COLOR : BAD, false);
    }

    private void renderTune(GuiGraphics graphics) {
        carLine(graphics);
        Upgrades upgrades = menu.upgrades();
        java.util.List<TuningKit> shown = shown();
        for (TuningKit kit : shown) {
            int y = kitsRow() + shown.indexOf(kit) * ROW_HEIGHT;
            graphics.renderFakeItem(TuningService.stackOf(kit), 8, y);
            Component name = Component.translatable("tuning.reikocars.kit_short." + kit.key());
            if (menu.hasTarget() && !kit.suits(menu.carType())) {
                name = Component.translatable("tuning.reikocars.kit_row", name, Component.translatable("tuning.reikocars.state.unsuited"));
            } else if (menu.hasTarget()) {
                // "Engine: stage 1 of 3", "Turbo: fitted"
                name = Component.translatable("tuning.reikocars.kit_row", name, kit.state(upgrades));
            }
            graphics.drawString(font, fit(name, FIT_X - 30), 28, y + 4, kit.fit(upgrades) == null && menu.hasTarget() ? 0xFF2E7D32 : LABEL_COLOR, false);
        }
        for (int i = 0; i < KITS.length; i++) {
            if (chosen[i] && shopButtons[i].visible) {
                drawSelectedTab(graphics, shopButtons[i]);
            }
        }
        long total = total();
        Component totalLine = menu.isOwner() ? Component.translatable("gui.reikocars.tune_total_free")
                : Component.translatable("gui.reikocars.total", format(total + salesTax(total)));
        graphics.drawString(font, totalLine, 8, totalRow(), LABEL_COLOR, false);
        if (!menu.isOwner()) {
            drawCardLine(graphics, 8, cardRow(), imageWidth - 16);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                    8, cashRow() + 4, LABEL_COLOR, false);
            graphics.drawString(font, pin, pinLabelX, cashRow() + 4, LABEL_COLOR, false);
        } else {
            graphics.drawString(font, fit(Component.translatable("gui.reikocars.your_bench"), imageWidth - 16), 8, cardRow(), MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, feedbackRow(), imageWidth - 16, 2);
    }

    private void renderLivery(GuiGraphics graphics) {
        carLine(graphics);
        graphics.drawString(font, Component.translatable("gui.reikocars.race_number"), 8, TOP + 26, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.reikocars.race_number_hint"), 146, TOP + 26, MUTED_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.reikocars.stripes"), 8, TOP + 48, LABEL_COLOR, false);
        DyeColor color = stripes == 0 ? null : DyeColor.byId(stripes - 1);
        Component name = color == null ? Component.translatable("gui.reikocars.stripes_none")
                : Component.translatable("color.minecraft." + color.getName());
        int mid = (126 + imageWidth - 20) / 2;
        if (color != null) {
            int swatch = 0xFF000000 | color.getTextureDiffuseColor();
            graphics.fill(mid - font.width(name) / 2 - 12, TOP + 47, mid - font.width(name) / 2 - 3, TOP + 56, 0xFF373737);
            graphics.fill(mid - font.width(name) / 2 - 11, TOP + 48, mid - font.width(name) / 2 - 4, TOP + 55, swatch);
        }
        graphics.drawString(font, name, mid - font.width(name) / 2, TOP + 48, LABEL_COLOR, false);
        var lines = font.split(Component.translatable("gui.reikocars.livery_hint"), imageWidth - 16);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 8, TOP + 98 + i * 10, MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, TOP + 146, imageWidth - 16, 3);
    }

    private void renderStock(GuiGraphics graphics) {
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.kit_stock_label"), imageWidth - 16), 8, TOP + 1, LABEL_COLOR, false);
        int x = 8;
        int y = TuningBenchMenu.STOCK_Y + 39;
        for (TuningKit kit : ORDER) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0.0F);
            pose.scale(0.625F, 0.625F, 1.0F);
            graphics.renderFakeItem(TuningService.stackOf(kit), 0, 0);
            pose.popPose();
            pose.pushPose();
            pose.translate(x + 10.5F, y + 3.0F, 0.0F);
            pose.scale(0.75F, 0.75F, 1.0F);
            graphics.drawString(font, Integer.toString(menu.stock(kit)), 0, 0, LABEL_COLOR, false);
            pose.popPose();
            x += 18;
        }
        graphics.drawString(font, playerInventoryTitle, TuningBenchMenu.STOCK_X, TuningBenchMenu.INVENTORY_Y - 10, LABEL_COLOR, false);
    }

    private void renderPrices(GuiGraphics graphics) {
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.kit_prices_label"), imageWidth - 16), 8, TOP + 1, LABEL_COLOR, false);
        for (TuningKit kit : KITS) {
            int y = TOP + 15 + ORDER.indexOf(kit) * 20;
            graphics.renderFakeItem(TuningService.stackOf(kit), 8, y);
            graphics.drawString(font, fit(kit.displayName(), 76), 28, y + 4, LABEL_COLOR, false);
        }
        int y = TOP + 15 + KITS.length * 20 + 4;
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.tuning_earned", format(menu.earned()), menu.jobs()), imageWidth - 16), 8, y,
                MUTED_COLOR, false);
    }
}
