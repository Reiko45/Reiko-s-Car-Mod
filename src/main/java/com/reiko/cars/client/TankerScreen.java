package com.reiko.cars.client;

import com.reiko.cars.network.TrailerActionPayload;
import com.reiko.cars.trailer.TankerService;
import com.reiko.cars.trailer.TrailerEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The tanker's screen: what's in the tank, and buttons to fill it from the tanks parked beside it or empty it into
 * them. The load is shown live as it changes.
 */
public final class TankerScreen extends Screen {
    private static final int WIDTH = 196;
    private static final int HEIGHT = 112;
    private final TrailerEntity tanker;
    private int left;
    private int top;

    public TankerScreen(TrailerEntity tanker) {
        super(tanker.getDisplayName());
        this.tanker = tanker;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.tanker.load"), button -> send(TrailerActionPayload.LOAD))
                .bounds(left + 8, top + 62, 88, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.reikocars.tanker.unload"), button -> send(TrailerActionPayload.UNLOAD))
                .bounds(left + WIDTH - 96, top + 62, 88, 20).build());
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new TrailerActionPayload(tanker.getId(), action));
    }

    @Override
    public void tick() {
        if (!tanker.isAlive() || minecraft.player == null || minecraft.player.distanceToSqr(tanker) > 12.0 * 12.0) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xFF000000);
        graphics.fill(left + 1, top + 1, left + WIDTH - 1, top + HEIGHT - 1, 0xFFC6C6C6);
        graphics.fill(left + 1, top + 1, left + WIDTH - 2, top + 3, 0xFFFFFFFF);
        graphics.fill(left + 1, top + 1, left + 3, top + HEIGHT - 2, 0xFFFFFFFF);
        graphics.fill(left + 2, top + HEIGHT - 3, left + WIDTH - 1, top + HEIGHT - 1, 0xFF555555);
        graphics.fill(left + WIDTH - 3, top + 2, left + WIDTH - 1, top + HEIGHT - 1, 0xFF555555);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, left + 8, top + 7, 0x404040, false);
        FluidStack fluid = tanker.shownFluid();
        int capacity = tanker.trailerType().tankBuckets * 1000;
        Component name = fluid.isEmpty() ? Component.translatable("gui.reikocars.tanker.empty") : fluid.getHoverName();
        Component amount = Component.translatable("gui.reikocars.buckets_of", buckets(fluid.getAmount()), buckets(capacity));
        graphics.drawString(font, name, left + 8, top + 22, 0x404040, false);
        graphics.drawString(font, amount, left + WIDTH - 8 - font.width(amount), top + 22, 0x6B6B6B, false);
        int barX = left + 8;
        int barY = top + 33;
        int barWidth = WIDTH - 16;
        graphics.fill(barX, barY, barX + barWidth, barY + 8, 0xFF373737);
        if (!fluid.isEmpty()) {
            int tint = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
            int color = tint == 0xFFFFFFFF ? 0xFFE0C040 : 0xFF000000 | tint;
            graphics.fill(barX, barY, barX + Math.max(1, barWidth * fluid.getAmount() / capacity), barY + 8, color);
        }
        graphics.drawString(font, Component.translatable("gui.reikocars.tanker.hint", TankerService.REACH), left + 8, top + 47, 0x6B6B6B, false);
        graphics.drawWordWrap(font, Component.translatable("gui.reikocars.tanker.pumps"), left + 8, top + 88, WIDTH - 16, 0x6B6B6B);
    }

    private static String buckets(int millibuckets) {
        return millibuckets % 1000 == 0 ? Integer.toString(millibuckets / 1000)
                : String.format(java.util.Locale.ROOT, "%.1f", millibuckets / 1000.0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
