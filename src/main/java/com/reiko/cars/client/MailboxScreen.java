package com.reiko.cars.client;

import com.reiko.cars.mail.MailboxBlockEntity;
import com.reiko.cars.mail.MailboxMenu;
import com.reiko.cars.network.MailActionPayload;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The mailbox screen. The Send tab: who it's for, nine slots to pack the parcel, and the postage, paid as at a shop
 * (card with PIN, or cash), or nothing when it goes straight into its addressee's own mailbox. The owner's Inbox tab
 * holds the mail that's come for them, and says whether the flag's up for the mail van.
 */
public final class MailboxScreen extends ShopScreenBase<MailboxMenu> {
    private static final int WIDTH = MailboxMenu.WIDTH;
    private static final int TABS_ROW = 16;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int TO_X = 26;

    private final int top;
    private int tab = MailboxMenu.TAB_SEND;
    private Button[] tabs = new Button[0];
    private EditBox toBox;
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;
    private long shownPostage = -1;

    public MailboxScreen(MailboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, menu.top() + 208);
        this.top = menu.top();
        this.inventoryLabelY = menu.inventoryY() - 10;
        this.inventoryLabelX = MailboxMenu.SLOTS_X;
    }

    private int postageRow() {
        return top + 40;
    }

    private int cardRow() {
        return top + 54;
    }

    private int payRow() {
        return top + 70;
    }

    private int feedbackRow() {
        return top + 93;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.isManager()) {
            tabs = addTabs(8, TABS_ROW, new String[] {"gui.reikocars.tab_send", "gui.reikocars.tab_inbox"}, new int[] {58, 70}, this::selectTab);
        }
        String to = toBox == null ? (menu.isOwner() ? "" : menu.ownerName()) : toBox.getValue();
        toBox = addRenderableWidget(new EditBox(font, leftPos + TO_X, topPos + top, 110, 16, Component.translatable("gui.reikocars.mail_to")));
        toBox.setMaxLength(16);
        toBox.setFilter(text -> text.matches("[A-Za-z0-9_]*"));
        toBox.setHint(Component.translatable("gui.reikocars.mail_to_hint").withColor(MUTED_COLOR));
        toBox.setValue(to);
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + cardRow() - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> send(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + payRow(), 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> send(Payment.cash()))
                .bounds(leftPos + 99, topPos + payRow(), 87, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.empty(), button -> send(Payment.cash()))
                .bounds(leftPos + 8, topPos + payRow(), WIDTH - 16, 20).build());
        shownPostage = -1;
        selectTab(tab);
    }

    private void selectTab(int selected) {
        if (selected != tab) {
            clearFeedback();
        }
        tab = selected;
        menu.setTab(selected);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
        updateButtons();
    }

    /** Whether the parcel goes straight into this mailbox, its addressee's own: free. */
    private boolean handDelivery() {
        return !menu.ownerName().isEmpty() && toBox.getValue().trim().equalsIgnoreCase(menu.ownerName());
    }

    private long postage() {
        return handDelivery() ? 0 : menu.postage();
    }

    private boolean packed() {
        return !menu.packing().isEmpty();
    }

    private void send(Payment payment) {
        PacketDistributor.sendToServer(new MailActionPayload(menu.containerId, toBox.getValue().trim(), postage(), payment));
    }

    @Override
    protected void onEnter() {
        if (freeButton.visible && freeButton.active) {
            freeButton.onPress();
        } else if (cardButton.visible && cardButton.active) {
            cardButton.onPress();
        }
    }

    private void updateButtons() {
        boolean send = tab == MailboxMenu.TAB_SEND;
        long postage = postage();
        boolean free = postage == 0;
        toBox.visible = send;
        pinBox.visible = cardButton.visible = cashButton.visible = send && !free;
        freeButton.visible = send && free;
        if (postage != shownPostage) {
            shownPostage = postage;
            setPayButtons(cardButton, cashButton, postage);
        }
        freeButton.setMessage(Component.translatable(handDelivery() ? "gui.reikocars.mail_drop_in" : "gui.reikocars.mail_send_free"));
        boolean ready = packed() && !toBox.getValue().isBlank();
        cardButton.active = ready && !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
        cashButton.active = ready && cashOnHand() >= postage + salesTax(postage);
        freeButton.active = ready;
        if (tabs.length > 1) {
            tabs[1].setMessage(menu.inboxCount() > 0
                    ? Component.translatable("gui.reikocars.tab_inbox_count", menu.inboxCount())
                    : Component.translatable("gui.reikocars.tab_inbox"));
        }
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
        drawSlotGrid(graphics, leftPos + MailboxMenu.SLOTS_X, topPos + menu.slotsY(), MailboxBlockEntity.SLOTS, 1);
        drawPlayerInventory(graphics, leftPos + MailboxMenu.SLOTS_X, topPos + menu.inventoryY());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, fit(title, imageWidth - 16), titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
        if (tab < tabs.length) {
            drawSelectedTab(graphics, tabs[tab]);
        }
        if (tab == MailboxMenu.TAB_INBOX) {
            renderInbox(graphics);
        } else {
            renderSend(graphics);
        }
    }

    private void renderSend(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("gui.reikocars.mail_to"), 8, top + 4, LABEL_COLOR, false);
        long postage = postage();
        Component line;
        if (handDelivery()) {
            line = Component.translatable("gui.reikocars.mail_hand_delivery", menu.ownerName());
        } else if (postage == 0) {
            line = Component.translatable("gui.reikocars.mail_postage_free");
        } else {
            line = Component.translatable("gui.reikocars.mail_postage", format(postage + salesTax(postage)));
        }
        Component cash = Component.translatable("gui.reikocars.mail_cash", format(cashOnHand()));
        boolean showCash = postage > 0;
        int room = imageWidth - 16 - (showCash ? font.width(cash) + 6 : 0);
        graphics.drawString(font, fit(line, room), 8, postageRow(), LABEL_COLOR, false);
        if (showCash) {
            graphics.drawString(font, cash, imageWidth - 8 - font.width(cash), postageRow(), MUTED_COLOR, false);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            drawCardLine(graphics, 8, cardRow(), pinLabelX - 12);
            graphics.drawString(font, pin, pinLabelX, cardRow(), LABEL_COLOR, false);
        } else if (!handDelivery()) {
            graphics.drawString(font, fit(Component.translatable("gui.reikocars.mail_by_van"), imageWidth - 16), 8, cardRow(), MUTED_COLOR, false);
        }
        drawFeedback(graphics, 8, feedbackRow(), imageWidth - 16, 2);
    }

    private void renderInbox(GuiGraphics graphics) {
        Component heading = menu.isOwner()
                ? Component.translatable("gui.reikocars.mail_for_you")
                : Component.translatable("gui.reikocars.mail_for", menu.ownerName());
        graphics.drawString(font, fit(heading, imageWidth - 16), 8, top + 4, LABEL_COLOR, false);
        Component hint = menu.inboxCount() > 0
                ? Component.translatable("gui.reikocars.mail_open_hint")
                : Component.translatable("gui.reikocars.mail_inbox_empty");
        graphics.drawString(font, fit(hint, imageWidth - 16), 8, postageRow(), MUTED_COLOR, false);
        int waiting = menu.outboxCount();
        Component outbox = waiting > 0
                ? Component.translatable(waiting == 1 ? "gui.reikocars.mail_waiting_one" : "gui.reikocars.mail_waiting", waiting)
                : Component.translatable("gui.reikocars.mail_none_waiting");
        graphics.drawString(font, fit(outbox, imageWidth - 16), 8, cardRow(), waiting > 0 ? LABEL_COLOR : MUTED_COLOR, false);
    }
}
