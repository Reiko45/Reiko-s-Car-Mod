package com.reiko.cars.client;

import com.reiko.cars.car.CarType;
import com.reiko.cars.network.SaleActionPayload;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.sale.DealershipMenu;
import com.reiko.cars.sale.SaleService;
import com.reiko.economy.client.screen.ShopScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A dealership's screen. Customers: Buy (browse the lot's cars for sale and buy one) and Trade in (sell their own car
 * on the lot to the dealer). The owner: Lot (price the cars they've parked there) and Trade-ins (what they'll pay for
 * each kind of car).
 */
public final class DealershipScreen extends ShopScreenBase<DealershipMenu> {
    private static final int WIDTH = 194;
    private static final int TABS_ROW = 16;
    private static final int TOP = 32;

    /** Setting an offer reopens the screen: on the Trade-ins tab, at the same kind of car, if it's the same dealership. */
    @Nullable
    private static BlockPos reopenOffersAt;
    private static int reopenType;

    private int tab;
    private int offerType;
    private Button[] tabs = new Button[0];
    private Button previous;
    private Button next;
    private Button buyButton;
    private Button sellButton;
    private Button payOffButton;
    private EditBox priceBox;
    private Button setPrice;
    private Button offSale;
    private Button typePrevious;
    private Button typeNext;
    private EditBox offerBox;
    private Button setOffer;
    private String shownCar = "";

    public DealershipScreen(DealershipMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 212);
        this.tab = menu.isManager() ? DealershipMenu.TAB_LOT : DealershipMenu.TAB_BUY;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.isManager()) {
            tabs = addTabs(8, TABS_ROW, new String[] {"gui.reikocars.tab_lot", "gui.reikocars.tab_offers"}, new int[] {58, 70},
                    i -> selectTab(i == 0 ? DealershipMenu.TAB_LOT : DealershipMenu.TAB_OFFERS));
        } else {
            tabs = addTabs(8, TABS_ROW, new String[] {"gui.reikocars.tab_buy", "gui.reikocars.tab_trade"}, new int[] {58, 70},
                    i -> selectTab(i == 0 ? DealershipMenu.TAB_BUY : DealershipMenu.TAB_TRADE));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> pick(DealershipMenu.PREVIOUS))
                .bounds(leftPos + imageWidth - 8 - 26, topPos + TOP - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.previous_vehicle"))).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> pick(DealershipMenu.NEXT))
                .bounds(leftPos + imageWidth - 8 - 12, topPos + TOP - 2, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("gui.reikocars.next_vehicle"))).build());
        buyButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.buy_this_car"),
                button -> send(SaleActionPayload.of(menu.containerId, SaleActionPayload.OPEN_BUY))).bounds(leftPos + 8, topPos + TOP + 40, WIDTH - 16, 20).build());
        sellButton = addRenderableWidget(Button.builder(Component.empty(),
                button -> send(SaleActionPayload.tradeIn(menu.containerId, menu.carOffer()))).bounds(leftPos + 8, topPos + TOP + 40, WIDTH - 16, 20).build());
        payOffButton = addRenderableWidget(Button.builder(Component.empty(),
                button -> send(SaleActionPayload.of(menu.containerId, SaleActionPayload.PAY_OFF))).bounds(leftPos + 8, topPos + TOP + 40, WIDTH - 16, 20).build());
        priceBox = addRenderableWidget(new EditBox(font, leftPos + 50, topPos + TOP + 40, 90, 16, Component.translatable("gui.reikocars.price")));
        priceBox.setFilter(MONEY_FILTER);
        priceBox.setMaxLength(16);
        setPrice = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"),
                button -> send(SaleActionPayload.text(menu.containerId, SaleActionPayload.SET_PRICE, priceBox.getValue(), 0)))
                .bounds(leftPos + 144, topPos + TOP + 39, 42, 18).build());
        offSale = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.take_off_sale"),
                button -> send(SaleActionPayload.of(menu.containerId, SaleActionPayload.OFF_SALE))).bounds(leftPos + 8, topPos + TOP + 62, WIDTH - 16, 20).build());

        typePrevious = addRenderableWidget(Button.builder(Component.literal("<"), button -> pickType(-1))
                .bounds(leftPos + 8, topPos + TOP + 2, 12, 12).build());
        typeNext = addRenderableWidget(Button.builder(Component.literal(">"), button -> pickType(1))
                .bounds(leftPos + imageWidth - 20, topPos + TOP + 2, 12, 12).build());
        offerBox = addRenderableWidget(new EditBox(font, leftPos + 80, topPos + TOP + 24, 60, 16, Component.translatable("gui.reikocars.price")));
        offerBox.setFilter(MONEY_FILTER);
        offerBox.setMaxLength(16);
        setOffer = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"), button -> sendOffer())
                .bounds(leftPos + 144, topPos + TOP + 23, 42, 18).build());

        if (menu.isManager() && menu.pos().equals(reopenOffersAt)) {
            tab = DealershipMenu.TAB_OFFERS;
            offerType = reopenType;
        }
        reopenOffersAt = null;
        offerBox.setValue(plain(menu.offer(CarType.values()[offerType])));
        selectTab(tab);
    }

    private static String plain(long cents) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void send(SaleActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    private void sendOffer() {
        reopenOffersAt = menu.pos();
        reopenType = offerType;
        send(SaleActionPayload.text(menu.containerId, SaleActionPayload.SET_OFFER, offerBox.getValue(), offerType));
    }

    private void pick(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private void pickType(int step) {
        offerType = Math.floorMod(offerType + step, CarType.values().length);
        offerBox.setValue(plain(menu.offer(CarType.values()[offerType])));
    }

    private void selectTab(int selected) {
        if (selected != tab) {
            clearFeedback();
        }
        tab = selected;
        menu.setTab(selected);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
        shownCar = "";
        updateWidgets();
    }

    private int tabButton() {
        return tab == DealershipMenu.TAB_BUY || tab == DealershipMenu.TAB_LOT ? 0 : 1;
    }

    private void updateWidgets() {
        boolean car = menu.hasCar();
        boolean offers = tab == DealershipMenu.TAB_OFFERS;
        previous.visible = next.visible = !offers && menu.carCount() > 1;
        buyButton.visible = tab == DealershipMenu.TAB_BUY && car;
        long owed = menu.carOwed();
        sellButton.visible = tab == DealershipMenu.TAB_TRADE && car && owed < 0;
        payOffButton.visible = tab == DealershipMenu.TAB_TRADE && car && owed >= 0;
        payOffButton.setMessage(Component.translatable("gui.reikocars.pay_off_loan", format(Math.max(0, owed))));
        long offer = menu.carOffer();
        sellButton.active = offer > 0;
        sellButton.setMessage(offer > 0 ? Component.translatable("gui.reikocars.sell_for", format(offer)) : Component.translatable("gui.reikocars.not_buying_short"));
        boolean lot = tab == DealershipMenu.TAB_LOT && car;
        priceBox.visible = setPrice.visible = offSale.visible = lot;
        offSale.active = lot && menu.carPrice() >= 0;
        String key = car ? menu.carIndex() + ":" + menu.carType() : "";
        if (lot && !key.equals(shownCar)) {
            priceBox.setValue(menu.carPrice() >= 0 ? plain(menu.carPrice()) : "");
        }
        shownCar = key;
        typePrevious.visible = typeNext.visible = offerBox.visible = setOffer.visible = offers;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    @Override
    protected void onEnter() {
        if (offerBox.visible && offerBox.isFocused()) {
            sendOffer();
        } else if (priceBox.visible && priceBox.isFocused()) {
            setPrice.onPress();
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

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        if (tabs.length > 0) {
            drawSelectedTab(graphics, tabs[tabButton()]);
        }
        if (tab == DealershipMenu.TAB_OFFERS) {
            renderOffers(graphics);
        } else {
            renderCar(graphics);
        }
        drawFeedback(graphics, 8, imageHeight - 22, imageWidth - 16, 2);
    }

    private void renderCar(GuiGraphics graphics) {
        CarType type = menu.carType();
        if (type == null) {
            Component empty = Component.translatable(switch (tab) {
                case DealershipMenu.TAB_BUY -> "gui.reikocars.lot_empty";
                case DealershipMenu.TAB_TRADE -> "gui.reikocars.trade_none";
                default -> "gui.reikocars.stock_none";
            }, (int) SaleService.reach());
            var lines = font.split(empty, imageWidth - 16);
            for (int i = 0; i < Math.min(4, lines.size()); i++) {
                graphics.drawString(font, lines.get(i), 8, TOP + i * 10, MUTED_COLOR, false);
            }
            return;
        }
        Component name = Component.translatable("entity.reikocars." + type.id());
        if (menu.carCount() > 1) {
            name = Component.translatable("gui.reikocars.vehicle_of", name, menu.carIndex() + 1, menu.carCount());
        }
        graphics.renderFakeItem(carStack(type, menu.carColor()), 8, TOP - 4);
        int right = imageWidth - 8 - (previous.visible ? 30 : 0);
        graphics.drawString(font, fit(name, right - 28), 28, TOP, LABEL_COLOR, false);
        graphics.drawString(font, CarSaleScreen.conditionLine(menu.carCondition(), menu.carFuel()), 28, TOP + 12, MUTED_COLOR, false);
        long price = menu.carPrice();
        MutableComponent line = switch (tab) {
            case DealershipMenu.TAB_TRADE -> menu.carOwed() >= 0
                    ? Component.translatable("gui.reikocars.loan_owed", format(menu.carOwed()))
                    : menu.carOffer() > 0
                    ? Component.translatable("gui.reikocars.dealer_pays", menu.ownerName(), format(menu.carOffer()))
                    : Component.translatable("gui.reikocars.not_buying", menu.ownerName(), Component.translatable("entity.reikocars." + type.id()));
            default -> price >= 0
                    ? Component.translatable("gui.reikocars.on_sale_at", format(price))
                    : Component.translatable("gui.reikocars.not_on_sale");
        };
        graphics.drawString(font, fit(line, imageWidth - 16), 8, TOP + 26, LABEL_COLOR, false);
        if (tab == DealershipMenu.TAB_LOT) {
            graphics.drawString(font, Component.translatable("gui.reikocars.price_label"), 8, TOP + 44, LABEL_COLOR, false);
        }
        Component hint = Component.translatable(switch (tab) {
            case DealershipMenu.TAB_BUY -> "gui.reikocars.buy_hint";
            case DealershipMenu.TAB_TRADE -> menu.carOwed() >= 0 ? "gui.reikocars.loan_hint" : "gui.reikocars.trade_hint";
            default -> "gui.reikocars.lot_hint";
        }, menu.ownerName(), (int) SaleService.reach());
        int y = tab == DealershipMenu.TAB_LOT ? TOP + 88 : TOP + 66;
        var lines = font.split(hint, imageWidth - 16);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 8, y + i * 10, MUTED_COLOR, false);
        }
    }

    private void renderOffers(GuiGraphics graphics) {
        CarType type = CarType.values()[offerType];
        Component name = Component.translatable("entity.reikocars." + type.id());
        graphics.renderFakeItem(carStack(type, type.defaultColor), (imageWidth - 16) / 2 - font.width(name) / 2 - 10, TOP);
        graphics.drawString(font, name, (imageWidth - 16) / 2 - font.width(name) / 2 + 10, TOP + 4, LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.reikocars.pays_up_to"), 8, TOP + 28, LABEL_COLOR, false);
        var hint = font.split(Component.translatable("gui.reikocars.offers_hint", Math.round(SaleService.WORN_OUT_SHARE * 100)), imageWidth - 16);
        for (int i = 0; i < Math.min(3, hint.size()); i++) {
            graphics.drawString(font, hint.get(i), 8, TOP + 46 + i * 10, MUTED_COLOR, false);
        }
        // What it's buying now.
        MutableComponent buying = Component.empty();
        int count = 0;
        for (CarType each : CarType.values()) {
            long offer = menu.offer(each);
            if (offer > 0) {
                if (count++ > 0) {
                    buying.append(", ");
                }
                buying.append(Component.translatable("entity.reikocars." + each.id())).append(" " + format(offer));
            }
        }
        Component list = count == 0 ? Component.translatable("gui.reikocars.buying_nothing")
                : Component.translatable("gui.reikocars.buying", buying);
        var lines = font.split(list, imageWidth - 16);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), 8, TOP + 82 + i * 10, LABEL_COLOR, false);
        }
        Component books = Component.translatable("gui.reikocars.dealer_books", menu.sold(), format(menu.earned()), menu.bought(), format(menu.spent()));
        var bookLines = font.split(books, imageWidth - 16);
        for (int i = 0; i < Math.min(2, bookLines.size()); i++) {
            graphics.drawString(font, bookLines.get(i), 8, TOP + 126 + i * 10, MUTED_COLOR, false);
        }
    }
}
