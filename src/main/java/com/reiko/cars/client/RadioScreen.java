package com.reiko.cars.client;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.radio.CarRadio;
import com.reiko.cars.radio.RadioMenu;
import com.reiko.cars.radio.RadioStation;
import com.reiko.economy.client.screen.ShopScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;

/**
 * A car's radio: a green display with what's playing, power, the three station presets and DISC, volume, and the
 * disc slot, over the rider's inventory.
 */
public final class RadioScreen extends ShopScreenBase<RadioMenu> {
    private static final int WIDTH = 176;
    private static final int LCD_TOP = 18;
    private static final int LCD_BOTTOM = 48;
    private static final int BUTTON_ROW = 52;
    private static final int VOLUME_ROW = 76;
    private static final int LCD_BG = 0xFF1E2A1A;
    private static final int LCD_ON = 0xFF9CFF7A;
    private static final int LCD_DIM = 0xFF3F5A36;

    private final Button[] sources = new Button[CarRadio.DISC + 1];
    private Button power;

    public RadioScreen(RadioMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, RadioMenu.INVENTORY_Y + 82);
        this.inventoryLabelY = RadioMenu.INVENTORY_Y - 11;
    }

    private CarEntity car() {
        return minecraft.level.getEntity(menu.carId()) instanceof CarEntity car ? car : null;
    }

    private void press(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 8;
        int y = topPos + BUTTON_ROW;
        power = addRenderableWidget(Button.builder(Component.literal("⏻"), b -> press(CarRadio.BUTTON_POWER))
                .bounds(x, y, 20, 18).tooltip(Tooltip.create(Component.translatable("gui.reikocars.radio_power"))).build());
        x += 22;
        for (RadioStation station : RadioStation.values()) {
            int i = station.ordinal();
            sources[i] = addRenderableWidget(Button.builder(Component.literal(station.frequency()), b -> press(CarRadio.BUTTON_SOURCE + i))
                    .bounds(x, y, 34, 18).tooltip(Tooltip.create(station.displayName())).build());
            x += 36;
        }
        sources[CarRadio.DISC] = addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.radio_disc"), b -> press(CarRadio.BUTTON_SOURCE + CarRadio.DISC))
                .bounds(x, y, WIDTH - 8 - (x - leftPos), 18).tooltip(Tooltip.create(Component.translatable("gui.reikocars.radio_disc_hint"))).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> press(CarRadio.BUTTON_QUIETER))
                .bounds(leftPos + 8, topPos + VOLUME_ROW, 16, 16).tooltip(Tooltip.create(Component.translatable("gui.reikocars.radio_quieter"))).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> press(CarRadio.BUTTON_LOUDER))
                .bounds(leftPos + 102, topPos + VOLUME_ROW, 16, 16).tooltip(Tooltip.create(Component.translatable("gui.reikocars.radio_louder"))).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        CarEntity car = car();
        if (car == null) {
            return;
        }
        int state = car.radioState();
        for (int i = 0; i < sources.length; i++) {
            // the button for what's playing is pressed in
            sources[i].active = !(CarRadio.on(state) && CarRadio.source(state) == i);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        // the display, set into the dash
        int x0 = leftPos + 8;
        int x1 = leftPos + imageWidth - 8;
        graphics.fill(x0 - 1, topPos + LCD_TOP - 1, x1 + 1, topPos + LCD_BOTTOM + 1, 0xFF373737);
        graphics.fill(x0, topPos + LCD_TOP, x1, topPos + LCD_BOTTOM, LCD_BG);
        drawSlot(graphics, leftPos + RadioMenu.DISC_X, topPos + RadioMenu.DISC_Y);
        drawPlayerInventory(graphics, leftPos + 8, topPos + RadioMenu.INVENTORY_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
        CarEntity car = car();
        if (car == null) {
            return;
        }
        int state = car.radioState();
        boolean on = CarRadio.on(state);
        int source = CarRadio.source(state);
        int textX = 12;
        int right = imageWidth - 12;
        if (!on) {
            graphics.drawString(font, Component.translatable("gui.reikocars.radio_off"), textX, LCD_TOP + 11, LCD_DIM, false);
        } else {
            Component top;
            if (source == CarRadio.DISC) {
                top = Component.translatable("gui.reikocars.radio_disc_mode");
            } else {
                RadioStation station = RadioStation.byIndex(source);
                top = Component.translatable("gui.reikocars.radio_fm", station.frequency(), station.displayName());
            }
            graphics.drawString(font, fit(top, right - textX - 30), textX, LCD_TOP + 5, LCD_ON, false);
            Component playing = RadioPlayer.trackTitle(car);
            if (playing == null) {
                ItemStack disc = car.radioDiscPlaying();
                playing = source == CarRadio.DISC
                        ? (disc.isEmpty() ? Component.translatable("gui.reikocars.radio_no_disc")
                        : JukeboxSong.fromStack(minecraft.level.registryAccess(), disc).map(song -> song.value().description()).orElse(disc.getHoverName()))
                        : Component.translatable("gui.reikocars.radio_tuning");
            }
            graphics.drawString(font, fit(playing, right - textX), textX, LCD_TOP + 17, LCD_ON, false);
            String vol = "VOL " + CarRadio.volume(state);
            graphics.drawString(font, vol, right - font.width(vol), LCD_TOP + 5, LCD_ON, false);
        }
        // the volume bar between - and +
        int bx = 28;
        int by = VOLUME_ROW + 4;
        int volume = CarRadio.volume(state);
        for (int i = 0; i < CarRadio.MAX_VOLUME; i++) {
            int h = 3 + i;
            int x = bx + i * 7;
            graphics.fill(x, by + 9 - h + 1, x + 5, by + 10, i < volume ? (on ? 0xFF4CAF50 : 0xFF6E8B6E) : 0xFF8B8B8B);
        }
        graphics.drawString(font, Component.translatable("gui.reikocars.radio_disc_slot"), RadioMenu.DISC_X - 4 - font.width(Component.translatable("gui.reikocars.radio_disc_slot")),
                RadioMenu.DISC_Y + 4, LABEL_COLOR, false);
    }
}
