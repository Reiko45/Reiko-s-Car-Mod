package com.reiko.cars.client;

import com.reiko.cars.network.LotActionPayload;
import com.reiko.cars.racing.RaceControlMenu;
import com.reiko.cars.racing.RaceService;
import com.reiko.economy.client.screen.ShopScreenBase;
import com.reiko.economy.shop.Payment;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A Race Control's screen. Race: the track, the race (enter by card or cash, leave, start; the owner can start or
 * call it off) and time trials. Best laps: the board. Settings (the owner): laps, entry fee, the owner's cut, books.
 */
public final class RaceControlScreen extends ShopScreenBase<RaceControlMenu> {
    private static final int WIDTH = 194;
    private static final int TABS_ROW = 16;
    private static final int TOP = 34;
    private static final int PIN_X = WIDTH - 8 - 34;
    private static final int CARD_ROW = 102;
    private static final int PIN_ROW = 116;
    private static final int PAY_ROW = 130;
    private static final int OWNER_ROW = 154;
    private static final int TRIAL_ROW = 178;
    private static final int BAD = 0xFFB3261E;
    private static final int GOOD = 0xFF2E7D32;

    private int tab = RaceControlMenu.TAB_RACE;
    private Button[] tabs = new Button[0];
    private EditBox pinBox;
    private Button cardButton;
    private Button cashButton;
    private Button freeButton;
    private Button leaveButton;
    private Button startButton;
    private Button ownerStart;
    private Button callOff;
    private Button trialButton;
    private final EditBox[] boxes = new EditBox[3];
    private final Button[] sets = new Button[3];

    public RaceControlScreen(RaceControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 226);
    }

    @Override
    protected void init() {
        super.init();
        String[] keys = menu.isManager() ? new String[] {"gui.reikocars.tab_race", "gui.reikocars.tab_board", "gui.reikocars.tab_settings"}
                : new String[] {"gui.reikocars.tab_race", "gui.reikocars.tab_board"};
        tabs = addTabs(8, TABS_ROW, keys, menu.isManager() ? new int[] {58, 58, 58} : new int[] {58, 58}, this::selectTab);
        pinBox = addRenderableWidget(createPinBox(leftPos + PIN_X, topPos + PIN_ROW - 4));
        cardButton = addRenderableWidget(Button.builder(Component.empty(), button -> enter(cardPayment(pinBox.getValue())))
                .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
        cashButton = addRenderableWidget(Button.builder(Component.empty(), button -> enter(Payment.cash()))
                .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
        freeButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.race_enter_free"), button -> enter(Payment.cash()))
                .bounds(leftPos + 8, topPos + PAY_ROW, WIDTH - 16, 20).build());
        leaveButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.race_leave"), button -> act(LotActionPayload.RACE_LEAVE))
                .bounds(leftPos + 8, topPos + PAY_ROW, 87, 20).build());
        startButton = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.race_start"), button -> act(LotActionPayload.RACE_START))
                .bounds(leftPos + 99, topPos + PAY_ROW, 87, 20).build());
        ownerStart = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.race_start"), button -> act(LotActionPayload.RACE_START))
                .bounds(leftPos + 8, topPos + OWNER_ROW, 87, 20).build());
        callOff = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.race_call_off"), button -> act(LotActionPayload.RACE_CANCEL))
                .bounds(leftPos + 99, topPos + OWNER_ROW, 87, 20).build());
        trialButton = addRenderableWidget(Button.builder(Component.empty(), button -> act(menu.onTrial() ? LotActionPayload.RACE_END_TRIAL
                : LotActionPayload.RACE_TRIAL)).bounds(leftPos + 8, topPos + TRIAL_ROW, WIDTH - 16, 20).build());
        addCardSwitch(leftPos + imageWidth - 22, topPos + CARD_ROW - 2);
        int[] actions = {LotActionPayload.RACE_SET_LAPS, LotActionPayload.RACE_SET_FEE, LotActionPayload.RACE_SET_CUT};
        String[] values = {Integer.toString(menu.info().laps()), plain(menu.info().entryFee()), Integer.toString(menu.info().cut())};
        for (int i = 0; i < 3; i++) {
            int action = actions[i];
            boxes[i] = addRenderableWidget(new EditBox(font, leftPos + 96, topPos + TOP + 4 + i * 20, 52, 16, Component.translatable("gui.reikocars.price")));
            boxes[i].setFilter(i == 1 ? MONEY_FILTER : text -> text.matches("[0-9]{0,3}"));
            boxes[i].setMaxLength(16);
            boxes[i].setValue(values[i]);
            EditBox box = boxes[i];
            sets[i] = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.set_price"),
                    button -> PacketDistributor.sendToServer(LotActionPayload.text(menu.containerId, action, box.getValue(), 0)))
                    .bounds(leftPos + 152, topPos + TOP + 3 + i * 20, 34, 18).build());
        }
        updateWidgets();
    }

    private static String plain(long cents) {
        return String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100);
    }

    private void selectTab(int selected) {
        if (selected != tab) {
            clearFeedback();
        }
        tab = selected;
        updateWidgets();
    }

    private void act(int action) {
        PacketDistributor.sendToServer(LotActionPayload.pay(menu.containerId, action, 0, Payment.cash(), 0));
    }

    private void enter(Payment payment) {
        PacketDistributor.sendToServer(LotActionPayload.pay(menu.containerId, LotActionPayload.RACE_ENTER, menu.info().entryFee(), payment, 0));
    }

    private void updateWidgets() {
        boolean race = tab == RaceControlMenu.TAB_RACE;
        RaceService.State state = menu.state();
        boolean open = state == null || state == RaceService.State.OPEN;
        boolean track = menu.info().flags() > 0;
        boolean entering = race && track && open && !menu.entered() && !menu.onTrial();
        long fee = menu.info().entryFee();
        pinBox.visible = cardButton.visible = cashButton.visible = entering && fee > 0;
        freeButton.visible = entering && fee == 0;
        if (entering && fee > 0) {
            cardButton.setMessage(Component.translatable("gui.reikoeconomy.pay_card", format(fee)));
            cashButton.setMessage(Component.translatable("gui.reikoeconomy.pay_cash", format(fee)));
            cardButton.active = !chosenCard().isEmpty() && pinBox.getValue().length() == 4;
            cashButton.active = cashOnHand() >= fee;
        }
        leaveButton.visible = startButton.visible = race && menu.entered() && state == RaceService.State.OPEN;
        startButton.active = menu.entrants() >= 2 || menu.isManager();
        ownerStart.visible = callOff.visible = race && menu.isManager() && state != null && !menu.entered();
        ownerStart.active = state == RaceService.State.OPEN;
        trialButton.visible = race && track && (!menu.entered() || menu.onTrial());
        trialButton.setMessage(Component.translatable(menu.onTrial() ? "gui.reikocars.trial_end" : "gui.reikocars.trial_start"));
        for (int i = 0; i < 3; i++) {
            boxes[i].visible = sets[i].visible = tab == RaceControlMenu.TAB_SETTINGS;
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
            if (boxes[i].visible && boxes[i].isFocused()) {
                sets[i].onPress();
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
            case RaceControlMenu.TAB_BOARD -> renderBoard(graphics);
            case RaceControlMenu.TAB_SETTINGS -> renderSettings(graphics);
            default -> renderRace(graphics);
        }
        drawFeedback(graphics, 8, imageHeight - 24, imageWidth - 16, 2);
    }

    private void renderRace(GuiGraphics graphics) {
        RaceControlMenu.Info info = menu.info();
        if (info.flags() == 0) {
            lines(graphics, Component.translatable("gui.reikocars.race_no_track", info.ownerName()), TOP, 3, BAD);
            return;
        }
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.race_track", info.flags(), info.laps()), imageWidth - 16), 8, TOP,
                LABEL_COLOR, false);
        graphics.drawString(font, fit(Component.translatable("gui.reikocars.race_terms", format(info.entryFee()), info.cut()), imageWidth - 16), 8,
                TOP + 11, MUTED_COLOR, false);
        RaceService.State state = menu.state();
        Component status = state == null ? Component.translatable("gui.reikocars.race_none")
                : state == RaceService.State.OPEN ? Component.translatable("gui.reikocars.race_open", menu.entrants(), format(menu.pot()))
                : Component.translatable("gui.reikocars.race_running", menu.entrants());
        graphics.drawString(font, fit(status, imageWidth - 16), 8, TOP + 26, state == null ? MUTED_COLOR : GOOD, false);
        if (!info.entrants().isEmpty()) {
            lines(graphics, Component.translatable("gui.reikocars.race_entrants", String.join(", ", info.entrants())), TOP + 37, 2, LABEL_COLOR);
        }
        if (menu.onTrial()) {
            graphics.drawString(font, fit(Component.translatable("gui.reikocars.trial_on"), imageWidth - 16), 8, TOP + 59, GOOD, false);
        } else if (menu.entered() && state == RaceService.State.OPEN) {
            lines(graphics, Component.translatable("gui.reikocars.race_get_ready", (int) RaceService.GRID_REACH), TOP + 59, 3, MUTED_COLOR);
        }
        if (pinBox.visible) {
            drawCardLine(graphics, 8, CARD_ROW, imageWidth - 16);
            Component pin = Component.translatable("gui.reikoeconomy.pin");
            int pinLabelX = PIN_X - 4 - font.width(pin);
            graphics.drawString(font, fit(Component.translatable("gui.reikoeconomy.cash_on_hand", format(cashOnHand())), pinLabelX - 12),
                    8, PIN_ROW, LABEL_COLOR, false);
            graphics.drawString(font, pin, pinLabelX, PIN_ROW, LABEL_COLOR, false);
        }
    }

    private void renderBoard(GuiGraphics graphics) {
        var board = menu.info().board();
        if (board.isEmpty()) {
            lines(graphics, Component.translatable("gui.reikocars.board_empty"), TOP, 3, MUTED_COLOR);
            return;
        }
        for (int i = 0; i < board.size(); i++) {
            RaceControlMenu.BoardLine line = board.get(i);
            int y = TOP + i * 12;
            int color = i == 0 ? 0xFFB8860B : LABEL_COLOR;
            graphics.drawString(font, (i + 1) + ".", 8, y, color, false);
            Component car = line.car().isEmpty() ? Component.empty() : Component.translatable("entity.reikocars." + line.car());
            graphics.drawString(font, fit(Component.literal(line.name()), 70), 24, y, color, false);
            graphics.drawString(font, fit(car, 58), 96, y, MUTED_COLOR, false);
            String time = RaceService.lapTime(line.ticks());
            graphics.drawString(font, time, imageWidth - 8 - font.width(time), y, color, false);
        }
    }

    private void renderSettings(GuiGraphics graphics) {
        String[] keys = {"gui.reikocars.race_laps", "gui.reikocars.race_fee", "gui.reikocars.race_cut"};
        for (int i = 0; i < 3; i++) {
            graphics.drawString(font, Component.translatable(keys[i]), 8, TOP + 8 + i * 20, LABEL_COLOR, false);
        }
        RaceControlMenu.Info info = menu.info();
        lines(graphics, Component.translatable("gui.reikocars.race_books", info.races(), info.trials(), format(info.earned())), TOP + 70, 2, MUTED_COLOR);
        lines(graphics, Component.translatable("gui.reikocars.race_flags_hint"), TOP + 96, 4, MUTED_COLOR);
    }
}
