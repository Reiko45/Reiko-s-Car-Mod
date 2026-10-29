package com.reiko.cars.client;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarType;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.network.RepairActionPayload;
import com.reiko.cars.repair.RepairBenchMenu;
import com.reiko.cars.repair.RepairJobs;
import com.reiko.cars.repair.RepairService;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The repair bench screen. The Repair tab shows the car beside the bench part by part: its condition, a Fit button
 * that fits parts from the player's inventory for nothing, and a button with the bench's price for having it done
 * from the bench's stock. Picked jobs add up to a bill paid as at a shop: by card with PIN, or cash. The owner also
 * gets a Stock tab, to load the bench with parts, and a Prices tab.
 */
public final class RepairBenchScreen extends ShopScreenBase<RepairBenchMenu> {
    private static final int WIDTH = 194;
    private static final int TABS_ROW = 16;
    private static final int ROW_HEIGHT = 18;
    private static final int BAR_WIDTH = 62;
    private static final int FIT_X = 96;
    private static final int SHOP_X = 130;
    private static final int SHOP_WIDTH = 56;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int FEEDBACK_LINES = 3;
    private static final Part[] PARTS = Part.values();

    /** A price change reopens the screen: on the Prices tab, if it's the same bench. */
    @Nullable
    private static BlockPos reopenPricesAt;

    /** Where the Repair tab's rows start: under the tabs, for the owner. */
    private final int top;
    private int tab = RepairBenchMenu.TAB_REPAIR;
    private final boolean[] chosen = new boolean[PARTS.length];
    private final Button[] fitButtons = new Button[PARTS.length];
    private final Button[] shopButtons = new Button[PARTS.length];
    private final EditBox[] priceBoxes = new EditBox[PARTS.length];
    private final Button[] setButtons = new Button[PARTS.length];
    private Button[] tabs = new Button[0];
    private long shownTotal = -1;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;

    public RepairBenchScreen(RepairBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.isManager() ? 237 : 225);
        this.top = menu.isManager() ? 30 : 18;
    }

    private int partsRow() {
        return top + 12;
    }

    private int totalRow() {
        return partsRow() + PARTS.length * ROW_HEIGHT + 3;
    }

    private int cardRow() {
        return totalRow() + 12;
    }

    /** Cash on hand, with the PIN box beside it. */
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
        if (menu.isManager()) {
            tabs = addTabs(8, TABS_ROW, new String[] {"gui.reikocars.tab_repair", "gui.reikocars.tab_stock", "gui.reikocars.tab_prices"},
                    new int[] {58, 58, 58}, this::selectTab);
        }
        for (Part part : PARTS) {
            int i = part.ordinal();
            int y = topPos + partsRow() + i * ROW_HEIGHT;
            fitButtons[i] = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.fit"), button -> fit(part))
                    .bounds(leftPos + FIT_X, y + 1, 30, 16).build());
            shopButtons[i] = addRenderableWidget(Button.builder(Component.empty(), button -> {
                chosen[i] = !chosen[i];
                updateButtons();
            }).bounds(leftPos + SHOP_X, y + 1, SHOP_WIDTH, 16).build());

            int priceY = topPos + top + 14 + i * 22;
            priceBoxes[i] = addRenderableWidget(new EditBox(font, leftPos + 108, priceY + 1, 46, 16, Component.translatable("gui.reikocars.price")));
            priceBoxes[i].setFilter(MONEY_FILTER);
            priceBoxes[i].setMaxLength(16);
            priceBoxes[i].setValue(plainAmount(menu.price(part)));
            setButtons[i] = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"), button -> setPrice(part))
                    .bounds(leftPos + 158, priceY, 28, 18).build());
        }

        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + cashRow()));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + payRow(), 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> buy(Payment.cash()))
                .bounds(leftPos + 99, topPos + payRow(), 87, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.repair_free"), button -> buy(Payment.cash()))
                .bounds(leftPos + 8, topPos + payRow(), WIDTH - 16, 20).build());
        addCardSwitch(leftPos + imageWidth - 22, topPos + cardRow() - 2);

        if (menu.isManager() && menu.pos().equals(reopenPricesAt)) {
            tab = RepairBenchMenu.TAB_PRICES;
        }
        reopenPricesAt = null;
        selectTab(tab);
    }

    private void selectTab(int selected) {
        if (selected != tab) {
            clearFeedback();
        }
        tab = selected;
        menu.setTab(selected);
        boolean repair = tab == RepairBenchMenu.TAB_REPAIR;
        boolean prices = tab == RepairBenchMenu.TAB_PRICES;
        for (int i = 0; i < PARTS.length; i++) {
            fitButtons[i].visible = shopButtons[i].visible = repair;
            priceBoxes[i].visible = setButtons[i].visible = prices;
        }
        boolean free = menu.isOwner();
        pinBox.visible = cardButton.visible = cashButton.visible = repair && !free;
        freeButton.visible = repair && free;
        updateButtons();
    }

    /** 400 cents as "4.00", for the price boxes. */
    private static String plainAmount(long cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    // ---- the jobs ----

    /** Items the bench would fit for {@code part}: 0 if it can't (or there's nothing to do). */
    private int benchJob(Part part) {
        return RepairService.jobSize(part, menu.needed(part), menu.stock(part));
    }

    /** Items of the player's own the Fit button would fit for {@code part}. */
    private int ownJob(Part part) {
        return RepairService.jobSize(part, menu.needed(part), RepairService.carried(minecraft.player, part));
    }

    private int chosenMask() {
        int mask = 0;
        for (Part part : PARTS) {
            if (chosen[part.ordinal()] && benchJob(part) > 0) {
                mask |= 1 << part.ordinal();
            }
        }
        return mask;
    }

    /** The bill for the picked jobs, before tax. */
    private long total() {
        long total = 0;
        for (Part part : PARTS) {
            if (chosen[part.ordinal()]) {
                total += benchJob(part) * menu.price(part);
            }
        }
        return total;
    }

    private void fit(Part part) {
        PacketDistributor.sendToServer(RepairActionPayload.fit(menu.containerId, 1 << part.ordinal()));
    }

    private void buy(Payment payment) {
        PacketDistributor.sendToServer(RepairActionPayload.buy(menu.containerId, chosenMask(), total(), payment));
    }

    private void setPrice(Part part) {
        reopenPricesAt = menu.pos();
        PacketDistributor.sendToServer(RepairActionPayload.setPrice(menu.containerId, part.ordinal(), priceBoxes[part.ordinal()].getValue()));
    }

    @Override
    protected void onEnter() {
        for (Part part : PARTS) {
            if (priceBoxes[part.ordinal()].isFocused()) {
                setPrice(part);
                return;
            }
        }
        if (cardButton.visible && cardButton.active) {
            cardButton.onPress();
        }
    }

    private void updateButtons() {
        boolean hasCar = menu.carType() != null;
        for (Part part : PARTS) {
            int i = part.ordinal();
            int needed = menu.needed(part);
            int bench = benchJob(part);
            if (bench <= 0) {
                chosen[i] = false;
            }
            int own = ownJob(part);
            int carried = RepairService.carried(minecraft.player, part);
            Component item = new ItemStack(RepairJobs.item(part)).getHoverName();
            fitButtons[i].active = hasCar && own > 0;
            fitButtons[i].setTooltip(Tooltip.create(!hasCar || needed <= 0
                    ? Component.translatable("gui.reikocars.fit_nothing")
                    : Component.translatable("gui.reikocars.fit_tooltip", own > 0 ? own : needed, item, carried)));

            shopButtons[i].active = hasCar && bench > 0;
            Component label;
            Component tip;
            if (!hasCar || needed <= 0) {
                label = Component.translatable("gui.reikocars.part_ok");
                tip = Component.translatable("gui.reikocars.fit_nothing");
            } else if (bench <= 0) {
                label = Component.translatable("gui.reikocars.no_stock");
                tip = Component.translatable("gui.reikocars.no_stock_tooltip", needed, item, menu.stock(part));
            } else {
                long price = bench * menu.price(part);
                label = Component.literal((chosen[i] ? "✔ " : "") + (menu.isOwner() || price == 0 ? "" : format(price)))
                        .append(menu.isOwner() || price == 0 ? Component.translatable("gui.reikocars.shop_free") : Component.empty());
                tip = Component.translatable(chosen[i] ? "gui.reikocars.shop_tooltip_chosen" : "gui.reikocars.shop_tooltip", bench, item,
                        format(menu.price(part)), menu.stock(part));
            }
            shopButtons[i].setMessage(label);
            shopButtons[i].setTooltip(Tooltip.create(tip));
        }
        long total = total();
        if (total != shownTotal) {
            shownTotal = total;
            setPayButtons(cardButton, cashButton, total);
        }
        boolean any = chosenMask() != 0;
        cardButton.active = any && !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = any && cashOnHand() >= total + salesTax(total);
        freeButton.active = any;
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
        if (tab == RepairBenchMenu.TAB_REPAIR) {
            CarCondition condition = menu.condition();
            boolean hasCar = menu.carType() != null;
            for (Part part : PARTS) {
                int y = topPos + partsRow() + part.ordinal() * ROW_HEIGHT;
                drawSlot(graphics, leftPos + 8, y + 1);
                if (hasCar) {
                    drawBar(graphics, leftPos + 28, y + 12, condition.get(part), barColor(condition.level(part)));
                }
            }
            if (!menu.isOwner()) {
                graphics.fill(leftPos + 6, topPos + totalRow() - 4, leftPos + imageWidth - 6, topPos + totalRow() - 3, 0xFF8B8B8B);
            }
        } else if (tab == RepairBenchMenu.TAB_STOCK) {
            drawSlotGrid(graphics, leftPos + RepairBenchMenu.STOCK_X, topPos + RepairBenchMenu.STOCK_Y, 9, 2);
            drawPlayerInventory(graphics, leftPos + RepairBenchMenu.STOCK_X, topPos + RepairBenchMenu.INVENTORY_Y);
        } else {
            for (Part part : PARTS) {
                drawSlot(graphics, leftPos + 8, topPos + top + 15 + part.ordinal() * 22);
            }
        }
    }

    /** Darker than the bar colours, to read on the grey panel. */
    private static int textColor(CarCondition.Level level) {
        return switch (level) {
            case GOOD -> MUTED_COLOR;
            case LOW -> 0x9A6A00;
            case CRITICAL -> 0xC2410C;
            case BROKEN -> 0xB3261E;
        };
    }

    private static int barColor(CarCondition.Level level) {
        return switch (level) {
            case GOOD -> 0xFF43A047;
            case LOW -> 0xFFF2C014;
            case CRITICAL -> 0xFFEF6C00;
            case BROKEN -> 0xFFE53935;
        };
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, float fraction, int color) {
        graphics.fill(x, y, x + BAR_WIDTH, y + 4, 0xFF373737);
        int filled = Math.round(BAR_WIDTH * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 4, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        if (tab < tabs.length) {
            drawSelectedTab(graphics, tabs[tab]);
        }
        switch (tab) {
            case RepairBenchMenu.TAB_STOCK -> renderStock(graphics);
            case RepairBenchMenu.TAB_PRICES -> renderPrices(graphics);
            default -> renderRepair(graphics);
        }
    }

    private void renderRepair(GuiGraphics graphics) {
        CarType car = menu.carType();
        CarCondition condition = menu.condition();
        if (car == null) {
            Component none = Component.translatable("gui.reikocars.repair_no_car", (int) CarsConfig.get(CarsConfig.BENCH_REACH));
            graphics.drawString(font, fit(none, imageWidth - 16), 8, top, 0xB3261E, false);
        } else {
            Component name = Component.translatable("entity.reikocars." + car.id());
            Component overall = Component.translatable("gui.reikocars.overall", CarCondition.percent(condition.overall()));
            graphics.drawString(font, fit(name, imageWidth - 22 - font.width(overall)), 8, top, LABEL_COLOR, false);
            graphics.drawString(font, overall, imageWidth - 8 - font.width(overall), top, MUTED_COLOR, false);
        }
        for (Part part : PARTS) {
            int y = partsRow() + part.ordinal() * ROW_HEIGHT;
            graphics.renderFakeItem(new ItemStack(RepairJobs.item(part)), 8, y + 1);
            graphics.drawString(font, part.displayName(), 28, y + 2, LABEL_COLOR, false);
            if (car != null) {
                CarCondition.Level level = condition.level(part);
                String percent = CarCondition.percent(condition.get(part));
                graphics.drawString(font, percent, 28 + BAR_WIDTH - font.width(percent), y + 2, textColor(level), false);
            }
        }
        for (Button button : shopButtons) {
            int i = java.util.Arrays.asList(shopButtons).indexOf(button);
            if (chosen[i] && button.visible) {
                drawSelectedTab(graphics, button);
            }
        }

        long total = total();
        Component totalLine = menu.isOwner()
                ? Component.translatable("gui.reikocars.repair_total_free")
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
        drawFeedback(graphics, 8, feedbackRow(), imageWidth - 16, FEEDBACK_LINES);
    }

    private void renderStock(GuiGraphics graphics) {
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.stock_label"), imageWidth - 16), 8, top + 1, LABEL_COLOR, false);
        // How many of each part, under the slots.
        int x = RepairBenchMenu.STOCK_X;
        int y = RepairBenchMenu.STOCK_Y + 38;
        for (Part part : PARTS) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0.0F);
            pose.scale(0.75F, 0.75F, 1.0F);
            graphics.renderFakeItem(new ItemStack(RepairJobs.item(part)), 0, 0);
            pose.popPose();
            graphics.drawString(font, "×" + menu.stock(part), x + 13, y + 3, LABEL_COLOR, false);
            x += 34;
        }
        graphics.drawString(font, playerInventoryTitle, RepairBenchMenu.STOCK_X, RepairBenchMenu.INVENTORY_Y - 10, LABEL_COLOR, false);
    }

    private void renderPrices(GuiGraphics graphics) {
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.prices_label"), imageWidth - 16), 8, top + 1, LABEL_COLOR, false);
        for (Part part : PARTS) {
            int y = top + 15 + part.ordinal() * 22;
            graphics.renderFakeItem(new ItemStack(RepairJobs.item(part)), 8, y);
            Component item = new ItemStack(RepairJobs.item(part)).getHoverName();
            graphics.drawString(font, fit(item, 76), 28, y + 4, LABEL_COLOR, false);
        }
        int y = top + 15 + PARTS.length * 22 + 4;
        Component earned = Component.translatable("gui.reikocars.bench_earned", format(menu.earned()), menu.repairs());
        graphics.drawString(font, fit(earned, imageWidth - 16), 8, y, MUTED_COLOR, false);
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.prices_hint"), imageWidth - 16), 8, y + 12, MUTED_COLOR, false);
    }
}
