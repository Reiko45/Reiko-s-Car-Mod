package com.reiko.cars.client;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.gps.GpsInbox;
import com.reiko.cars.gps.GpsMath;
import com.reiko.cars.gps.PlaceKind;
import com.reiko.cars.network.GpsCarsPayload;
import com.reiko.cars.network.GpsPlacesPayload;
import com.reiko.cars.network.GpsRequestPayload;
import com.reiko.cars.registry.ModItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The GPS: pick where to go. Tabs for the places nearby (gas stations, chargers, mechanics, car washes, dealerships,
 * offices), your cars, Xaero's Minimap's waypoints (with that mod) and a spot by its coordinates.
 */
public final class GpsScreen extends Screen {
    private static final int WIDTH = 284;
    private static final int HEIGHT = 226;
    private static final int ROW = 22;
    private static final int ACCENT = 0xFF4FC3F7;

    private enum Tab {
        PLACES("gps.reikocars.tab.places"),
        CARS("gps.reikocars.tab.cars"),
        WAYPOINTS("gps.reikocars.tab.waypoints"),
        COORDINATES("gps.reikocars.tab.coordinates");

        final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    /** A line in a list: something to go to. */
    private record Row(@Nullable ItemStack icon, String initials, int color, Component title, Component detail, int detailColor, boolean dim,
            Vec3 pos, boolean hasY, ResourceKey<Level> dimension, @Nullable UUID car, String filter) {
    }

    /** The tab and search open last time, so the GPS opens where you left it. */
    private static Tab lastTab = Tab.PLACES;
    private static String lastSearch = "";

    private Tab tab = lastTab;
    private int left;
    private int top;
    private final List<Tab> tabs = new ArrayList<>();
    private List<Row> rows = List.of();
    private int scroll;
    private EditBox search;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private Button go;
    private Button here;
    private Button stop;
    private int placesSeen;
    private int carsSeen;
    private boolean placesArrived;
    private boolean carsArrived;
    @Nullable
    private Component coordinatesError;

    /** When the places were last asked for: the server answers once a second at most. */
    private static long lastAsked;

    public GpsScreen() {
        super(Component.translatable("gps.reikocars.title"));
        placesSeen = GpsInbox.placesVersion();
        carsSeen = GpsInbox.carsVersion();
        long now = System.currentTimeMillis();
        if (now - lastAsked >= 1500) {
            lastAsked = now;
            PacketDistributor.sendToServer(new GpsRequestPayload(GpsRequestPayload.PLACES));
        } else {
            placesArrived = true; // (the ones that came a moment ago)
        }
        PacketDistributor.sendToServer(new GpsRequestPayload(GpsRequestPayload.CARS));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        tabs.clear();
        tabs.addAll(Arrays.asList(Tab.values()));
        if (!GpsClient.XAERO) {
            tabs.remove(Tab.WAYPOINTS);
        }
        if (!tabs.contains(tab)) {
            tab = Tab.PLACES;
        }
        search = new EditBox(font, left + 14, top + 42, WIDTH - 28, 14, Component.translatable("gps.reikocars.search"));
        search.setHint(Component.translatable("gps.reikocars.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(40);
        search.setValue(lastSearch);
        search.setResponder(text -> {
            lastSearch = text;
            scroll = 0;
            rebuild();
        });
        addRenderableWidget(search);

        int boxWidth = 66;
        int boxY = top + 66;
        xBox = coordinateBox(left + 34, boxY, boxWidth, "X");
        yBox = coordinateBox(left + 34 + boxWidth + 20, boxY, boxWidth, "Y");
        zBox = coordinateBox(left + 34 + (boxWidth + 20) * 2, boxY, boxWidth, "Z");
        yBox.setHint(Component.translatable("gps.reikocars.optional").withStyle(ChatFormatting.DARK_GRAY));
        go = addRenderableWidget(Button.builder(Component.translatable("gps.reikocars.go"), button -> goToCoordinates())
                .bounds(left + 34, boxY + 26, 80, 18).build());
        here = addRenderableWidget(Button.builder(Component.translatable("gps.reikocars.here"), button -> fillHere())
                .bounds(left + 120, boxY + 26, 80, 18).build());
        stop = addRenderableWidget(Button.builder(Component.translatable("gps.reikocars.stop"), button -> {
            GpsClient.stop();
            updateWidgets();
        }).bounds(left + WIDTH - 14 - 44, top + HEIGHT - 27, 44, 16).build());
        rebuild();
        updateWidgets();
    }

    private EditBox coordinateBox(int x, int y, int width, String axis) {
        EditBox box = new EditBox(font, x, y, width, 16, Component.literal(axis));
        box.setMaxLength(9);
        box.setFilter(text -> text.isEmpty() || text.equals("-") || text.matches("-?\\d{1,8}"));
        addRenderableWidget(box);
        return box;
    }

    private void updateWidgets() {
        search.visible = tab == Tab.PLACES;
        boolean coordinates = tab == Tab.COORDINATES;
        xBox.visible = coordinates;
        yBox.visible = coordinates;
        zBox.visible = coordinates;
        go.visible = coordinates;
        here.visible = coordinates;
        stop.visible = GpsClient.destination() != null;
        if (tab == Tab.PLACES) {
            setInitialFocus(search);
        } else if (coordinates) {
            setInitialFocus(xBox);
        } else {
            setFocused(null);
        }
    }

    @Override
    public void tick() {
        super.tick();
        boolean changed = false;
        if (GpsInbox.placesVersion() != placesSeen) {
            placesSeen = GpsInbox.placesVersion();
            placesArrived = true;
            changed = true;
        }
        if (GpsInbox.carsVersion() != carsSeen) {
            carsSeen = GpsInbox.carsVersion();
            carsArrived = true;
            changed = true;
        }
        if (changed) {
            rebuild();
        }
        stop.visible = GpsClient.destination() != null;
    }

    // ---- the lists ----

    private void rebuild() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            rows = List.of();
            return;
        }
        ResourceKey<Level> dimension = minecraft.level.dimension();
        List<Row> built = new ArrayList<>();
        switch (tab) {
            case PLACES -> {
                for (GpsPlacesPayload.Entry place : GpsInbox.places()) {
                    PlaceKind kind = place.kind();
                    Component title = place.owner().isEmpty() ? kind.displayName()
                            : Component.translatable("gps.reikocars.place_owner", kind.displayName(), place.owner());
                    String filter = (kind.displayName().getString() + " " + place.owner() + " " + place.detail().getString()).toLowerCase(Locale.ROOT);
                    built.add(new Row(kind.icon(), "", kind.color, title, place.detail(), place.open() ? 0xFFE6C36A : 0xFFEF9A9A, !place.open(),
                            place.pos().getBottomCenter(), true, dimension, null, filter));
                }
                String query = search == null ? lastSearch : search.getValue();
                if (!query.isBlank()) {
                    String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
                    built.removeIf(row -> !Arrays.stream(words).allMatch(row.filter()::contains));
                }
            }
            case CARS -> {
                UUID riding = minecraft.player.getRootVehicle() instanceof CarEntity car ? car.getUUID() : null;
                for (GpsCarsPayload.Entry car : GpsInbox.cars()) {
                    ItemStack icon = new ItemStack(ModItems.car(car.type()));
                    icon.set(DataComponents.DYED_COLOR, new DyedItemColor(car.color(), false));
                    Component detail = car.id().equals(riding) ? Component.translatable("gps.reikocars.car_riding")
                            : !car.dimension().equals(dimension) ? Component.translatable("gps.reikocars.in_dimension", dimensionName(car.dimension()))
                            : car.rented() ? Component.translatable("gps.reikocars.car_rented")
                            : Component.translatable("gps.reikocars.car_parked", car.pos().getX(), car.pos().getY(), car.pos().getZ());
                    CarEntity loaded = GpsClient.loadedCar(minecraft, car.id());
                    Vec3 pos = loaded != null ? loaded.position() : car.pos().getBottomCenter();
                    built.add(new Row(icon, "", 0xFF000000 | car.color(), GpsClient.carName(car), detail, 0xFF90A4AE, car.id().equals(riding),
                            pos, true, car.dimension(), car.id(), ""));
                }
            }
            case WAYPOINTS -> {
                for (GpsClient.WaypointPin pin : GpsClient.xaeroWaypoints(minecraft)) {
                    Component detail = pin.death() ? Component.translatable("gps.reikocars.death_point")
                            : Component.translatable("gps.reikocars.at", (int) Math.floor(pin.pos().x), (int) Math.floor(pin.pos().y),
                                    (int) Math.floor(pin.pos().z));
                    built.add(new Row(null, pin.initials(), pin.color(), Component.literal(pin.name()), detail, 0xFF90A4AE, false, pin.pos(),
                            pin.hasY(), dimension, null, ""));
                }
            }
            case COORDINATES -> {
            }
        }
        Vec3 from = GpsClient.traveller(minecraft.player).position();
        built.sort(Comparator.comparing((Row row) -> !row.dimension().equals(dimension)).thenComparingDouble(row -> row.pos().distanceToSqr(from)));
        rows = built;
        scroll = Math.min(scroll, Math.max(0, rows.size() - visibleRows()));
    }

    private int listTop() {
        return top + (tab == Tab.PLACES ? 60 : 42);
    }

    private int listBottom() {
        return top + HEIGHT - 34;
    }

    private int visibleRows() {
        return (listBottom() - listTop()) / ROW;
    }

    @Nullable
    private Row rowAt(double mouseX, double mouseY) {
        if (tab == Tab.COORDINATES || mouseX < left + 10 || mouseX >= left + WIDTH - 14 || mouseY < listTop()) {
            return null;
        }
        int index = (int) ((mouseY - listTop()) / ROW);
        if (index >= visibleRows()) {
            return null;
        }
        index += scroll;
        return index < rows.size() ? rows.get(index) : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // The tabs
            int tabWidth = (WIDTH - 20) / tabs.size();
            if (mouseY >= top + 22 && mouseY < top + 36 && mouseX >= left + 10 && mouseX < left + 10 + tabWidth * tabs.size()) {
                Tab picked = tabs.get((int) ((mouseX - left - 10) / tabWidth));
                if (picked != tab) {
                    tab = picked;
                    lastTab = picked;
                    scroll = 0;
                    coordinatesError = null;
                    rebuild();
                    updateWidgets();
                    playClick();
                }
                return true;
            }
            Row row = rowAt(mouseX, mouseY);
            if (row != null) {
                GpsClient.navigate(row.title(), row.dimension(), row.pos(), row.hasY(), row.car(), row.color(),
                        row.initials().isEmpty() ? "GPS" : row.initials());
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab != Tab.COORDINATES && rows.size() > visibleRows()) {
            scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, rows.size() - visibleRows());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---- coordinates ----

    private void fillHere() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            xBox.setValue(Integer.toString(minecraft.player.getBlockX()));
            yBox.setValue(Integer.toString(minecraft.player.getBlockY()));
            zBox.setValue(Integer.toString(minecraft.player.getBlockZ()));
        }
    }

    private void goToCoordinates() {
        Minecraft minecraft = Minecraft.getInstance();
        Integer x = parse(xBox.getValue());
        Integer z = parse(zBox.getValue());
        Integer y = parse(yBox.getValue());
        if (x == null || z == null || minecraft.level == null) {
            coordinatesError = Component.translatable("gps.reikocars.need_x_z");
            return;
        }
        boolean hasY = y != null;
        Component name = hasY ? Component.translatable("gps.reikocars.spot_xyz", x, y, z) : Component.translatable("gps.reikocars.spot_xz", x, z);
        GpsClient.navigate(name, minecraft.level.dimension(), new Vec3(x + 0.5, hasY ? y : 0.0, z + 0.5), hasY, null, 0xFFFFFFFF, "GPS");
        onClose();
    }

    @Nullable
    private static Integer parse(String text) {
        try {
            return text.isBlank() ? null : Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tab == Tab.COORDINATES && (keyCode == 257 || keyCode == 335)) { // Enter
            goToCoordinates();
            return true;
        }
        // The GPS key closes it again, unless you're typing.
        if (GpsClient.KEY.matches(keyCode, scanCode) && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        // Header: the GPS, and where you are.
        graphics.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), left + 14, top + 10, ACCENT, false);
        Entity traveller = GpsClient.traveller(minecraft.player);
        Vec3 at = traveller.position();
        Vec3 ahead = GpsMath.forward(minecraft.player.getYRot());
        String facing = GpsMath.compass(ahead.x, ahead.z);
        Component where = Component.translatable("gps.reikocars.you_are", (int) Math.floor(at.x), (int) Math.floor(at.y), (int) Math.floor(at.z), facing);
        graphics.drawString(font, where, left + WIDTH - 14 - font.width(where), top + 10, 0xFF90A4AE, false);

        // Tabs
        int tabWidth = (WIDTH - 20) / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            Tab each = tabs.get(i);
            int tx = left + 10 + i * tabWidth;
            boolean active = each == tab;
            boolean hover = mouseY >= top + 22 && mouseY < top + 36 && mouseX >= tx && mouseX < tx + tabWidth;
            Component label = Component.translatable(each.key);
            graphics.drawString(font, label, tx + (tabWidth - font.width(label)) / 2, top + 25, active ? 0xFFFFFFFF : hover ? 0xFFCFD8DC : 0xFF78909C,
                    false);
            graphics.fill(tx + 2, top + 35, tx + tabWidth - 2, top + 36, active ? ACCENT : 0x30FFFFFF);
        }

        if (tab == Tab.COORDINATES) {
            renderCoordinates(graphics);
        } else {
            renderList(graphics, minecraft, mouseX, mouseY, at);
        }
        renderFooter(graphics, minecraft, at);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        // The unit: a dark bezel round a darker display.
        graphics.fill(left - 3, top - 3, left + WIDTH + 3, top + HEIGHT + 3, 0xFF2B3036);
        graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, 0xFF16191E);
        graphics.fill(left + 4, top + 4, left + WIDTH - 4, top + HEIGHT - 4, 0xFF0B1118);
        graphics.renderOutline(left + 4, top + 4, WIDTH - 8, HEIGHT - 8, 0xFF22313F);
    }

    private void renderList(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY, Vec3 at) {
        int listTop = listTop();
        int x0 = left + 10;
        int x1 = left + WIDTH - 14;
        boolean metric = CarsClientConfig.speedUnit() == CarsClientConfig.SpeedUnit.KMH;
        if (rows.isEmpty()) {
            boolean waiting = tab == Tab.PLACES ? !placesArrived : tab == Tab.CARS && !carsArrived;
            Component empty = waiting ? Component.translatable("gps.reikocars.searching")
                    : Component.translatable("gps.reikocars.none." + tab.name().toLowerCase(Locale.ROOT));
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(empty, WIDTH - 40);
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawString(font, lines.get(i), left + WIDTH / 2 - font.width(lines.get(i)) / 2, listTop + 20 + i * 11, 0xFF78909C, false);
            }
            return;
        }
        Row hovered = rowAt(mouseX, mouseY);
        int count = Math.min(visibleRows(), rows.size() - scroll);
        for (int i = 0; i < count; i++) {
            Row row = rows.get(scroll + i);
            int y = listTop + i * ROW;
            if (row == hovered) {
                graphics.fill(x0, y, x1, y + ROW, 0x2E29B6F6);
            }
            if (i > 0) {
                graphics.fill(x0 + 4, y, x1 - 4, y + 1, 0x14FFFFFF);
            }
            // The icon: the place's block, your car in its paint, or the waypoint's letters.
            if (row.icon() != null) {
                graphics.renderItem(row.icon(), x0 + 3, y + 3);
            } else {
                int boxWidth = Math.max(12, font.width(row.initials()) + 4);
                graphics.fill(x0 + 11 - boxWidth / 2, y + 5, x0 + 11 - boxWidth / 2 + boxWidth, y + 17, row.color());
                graphics.drawString(font, row.initials(), x0 + 11 - font.width(row.initials()) / 2, y + 7, 0xFFFFFFFF, true);
            }
            // How far, and which way; or where it is, if it's in another dimension.
            String distance;
            int distanceColor = 0xFF80DEEA;
            if (row.car() != null && row.dim()) {
                distance = ""; // the car you're in
            } else if (row.dimension().equals(minecraft.level.dimension())) {
                double dx = row.pos().x - at.x;
                double dz = row.pos().z - at.z;
                distance = GpsMath.distance(Math.sqrt(dx * dx + dz * dz), metric) + " " + GpsMath.compass(dx, dz);
            } else {
                distance = "";
                distanceColor = 0xFF78909C;
            }
            int textRoom = x1 - 4 - font.width(distance) - 6 - (x0 + 24);
            int titleColor = row.dim() ? 0xFF9E9E9E : 0xFFFFFFFF;
            graphics.drawString(font, fit(font, row.title().getString(), textRoom), x0 + 24, y + 3, titleColor, false);
            graphics.drawString(font, fit(font, row.detail().getString(), x1 - 4 - (x0 + 24)), x0 + 24, y + 12, row.detailColor(), false);
            graphics.drawString(font, distance, x1 - 4 - font.width(distance), y + 3, distanceColor, false);
            // A swatch of the place's color down the left edge.
            graphics.fill(x0, y + 3, x0 + 1, y + ROW - 3, row.color());
        }
        // The scroll bar
        if (rows.size() > visibleRows()) {
            int trackTop = listTop;
            int trackHeight = visibleRows() * ROW;
            int thumb = Math.max(10, trackHeight * visibleRows() / rows.size());
            int thumbTop = trackTop + (trackHeight - thumb) * scroll / Math.max(1, rows.size() - visibleRows());
            graphics.fill(x1 + 2, trackTop, x1 + 4, trackTop + trackHeight, 0x30FFFFFF);
            graphics.fill(x1 + 2, thumbTop, x1 + 4, thumbTop + thumb, ACCENT);
        }
    }

    private void renderCoordinates(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("gps.reikocars.coordinates_help"), left + 14, top + 46, 0xFF90A4AE, false);
        int boxY = top + 66;
        graphics.drawString(font, "X", xBox.getX() - 10, boxY + 4, ACCENT, false);
        graphics.drawString(font, "Y", yBox.getX() - 10, boxY + 4, ACCENT, false);
        graphics.drawString(font, "Z", zBox.getX() - 10, boxY + 4, ACCENT, false);
        if (coordinatesError != null) {
            graphics.drawString(font, coordinatesError, left + 34, boxY + 52, 0xFFEF9A9A, false);
        }
    }

    private void renderFooter(GuiGraphics graphics, Minecraft minecraft, Vec3 at) {
        int y = top + HEIGHT - 30;
        graphics.fill(left + 10, y - 2, left + WIDTH - 10, y - 1, 0x30FFFFFF);
        GpsClient.Destination going = GpsClient.destination();
        if (going == null) {
            graphics.drawString(font, Component.translatable(tab == Tab.COORDINATES ? "gps.reikocars.hint_coordinates" : "gps.reikocars.hint"),
                    left + 14, y + 7, 0xFF78909C, false);
            return;
        }
        String distance = going.dimension().equals(minecraft.level.dimension())
                ? GpsMath.distance(Math.sqrt(Math.pow(going.pos().x - at.x, 2) + Math.pow(going.pos().z - at.z, 2)),
                        CarsClientConfig.speedUnit() == CarsClientConfig.SpeedUnit.KMH)
                : dimensionName(going.dimension()).getString();
        graphics.drawString(font, Component.translatable("gps.reikocars.navigating_short"), left + 14, y + 2, 0xFF78909C, false);
        int room = WIDTH - 28 - 50 - font.width(distance) - 8;
        String name = fit(font, going.name().getString(), room);
        graphics.drawString(font, name, left + 14, y + 12, 0xFFFFFFFF, false);
        graphics.drawString(font, distance, left + 14 + font.width(name) + 8, y + 12, 0xFF80DEEA, false);
    }

    @Override
    public void onClose() {
        if (search != null) {
            lastSearch = search.getValue();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- helpers ----

    /** {@code text}, cut short with "…" to fit {@code width}. */
    static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }

    /** "the Overworld", "the Nether", or a modded dimension's name made readable. */
    static Component dimensionName(ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD || dimension == Level.NETHER || dimension == Level.END) {
            return Component.translatable("gps.reikocars.dimension." + dimension.location().getPath());
        }
        StringBuilder name = new StringBuilder();
        for (String word : dimension.location().getPath().split("[_/]")) {
            if (!word.isEmpty()) {
                name.append(name.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return Component.literal(name.toString());
    }
}
