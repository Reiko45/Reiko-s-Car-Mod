package com.reiko.cars.mail;

import com.mojang.authlib.GameProfile;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The mail. At a mailbox a player packs items into a parcel for another player. If the mailbox is the addressee's
 * own, the parcel goes straight in, free. Otherwise the sender pays postage (as at a shop: card with PIN or cash, plus
 * sales tax) and the parcel waits in the mailbox, flag up, for the mail van. Whoever drives the mail van up to a
 * mailbox and presses the hitch key (G) collects its outgoing parcels and delivers the ones for that mailbox's owner,
 * earning each one's postage.
 */
public final class MailService {
    private MailService() {
    }

    /** The postage for a parcel sent by mail van, in cents. */
    public static long postage() {
        return Math.round(CarsConfig.get(CarsConfig.POSTAGE) * 100.0);
    }

    // ---- sending ----

    /**
     * Sends the items packed in {@code menu} to the player named {@code recipientName}.
     *
     * @param expectedPostage the postage the screen showed, before tax (0 for hand delivery)
     */
    public static ShopActionResult send(ServerPlayer player, MailboxMenu menu, String recipientName, long expectedPostage, Payment payment) {
        MailboxBlockEntity box = menu.box();
        if (box == null || box.getLevel() == null) {
            return ShopActionResult.fail("mail.reikocars.no_mailbox_here");
        }
        SimpleContainer packing = menu.packing();
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < packing.getContainerSize(); slot++) {
            ItemStack stack = packing.getItem(slot);
            if (!stack.isEmpty()) {
                if (!MailboxMenu.fitsInParcel(stack)) {
                    return ShopActionResult.fail("mail.reikocars.cant_pack", stack.getHoverName());
                }
                items.add(stack.copy());
            }
        }
        if (items.isEmpty()) {
            return ShopActionResult.fail("mail.reikocars.nothing_packed");
        }
        String name = recipientName.trim();
        if (name.isEmpty()) {
            return ShopActionResult.fail("mail.reikocars.no_recipient");
        }
        GameProfile to = findPlayer(player.server, name);
        if (to == null) {
            return ShopActionResult.fail("mail.reikocars.no_such_player", name);
        }
        MinecraftServer server = player.server;
        ItemContainerContents contents = ItemContainerContents.fromItems(items);
        String fromName = player.getGameProfile().getName();

        // Into the addressee's own mailbox: delivered by hand, free.
        if (to.getId().equals(box.owner())) {
            ItemStack parcel = ParcelItem.of(new Parcel(to.getId(), to.getName(), player.getUUID(), fromName, 0, contents, Optional.empty()));
            if (!box.deliver(parcel)) {
                return ShopActionResult.fail("mail.reikocars.mailbox_full", to.getName());
            }
            packing.clearContent();
            if (!to.getId().equals(player.getUUID())) {
                notifyRecipient(server, to.getId(), box.getBlockPos(), fromName, false);
            }
            sound(box.getLevel(), box.getBlockPos(), 1.0F);
            return ShopActionResult.ok(Component.translatable("mail.reikocars.dropped_off", to.getName()));
        }

        // Anyone else: by mail van, to their mailbox.
        GlobalPos address = MailRegistry.get(server).first(to.getId());
        if (address == null) {
            return ShopActionResult.fail("mail.reikocars.no_mailbox", to.getName());
        }
        long postage = postage();
        if (postage != expectedPostage) {
            return ShopActionResult.fail("mail.reikocars.postage_changed", EconomyApi.get().format(postage + EconomyConfig.taxRates().salesTax(postage)));
        }
        ItemStack parcel = ParcelItem.of(new Parcel(to.getId(), to.getName(), player.getUUID(), fromName, postage, contents, Optional.of(address)));
        // Check there's room before taking the money.
        if (!box.canPost(parcel)) {
            return ShopActionResult.fail("mail.reikocars.outbox_full");
        }
        long tax = postage > 0 ? EconomyConfig.taxRates().salesTax(postage) : 0;
        ShopPayments.Charge charge = null;
        if (postage > 0) {
            Component description = Component.translatable("mail.reikocars.postage_line", to.getName());
            charge = ShopPayments.charge(player, payment, postage, tax, null, "reikocars.postage", description);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }
        box.post(parcel);
        packing.clearContent();
        sound(box.getLevel(), box.getBlockPos(), 1.2F);
        if (postage <= 0) {
            return ShopActionResult.ok(Component.translatable("mail.reikocars.posted_free", to.getName()));
        }
        Component line = tax > 0
                ? Component.translatable("mail.reikocars.posted_tax", to.getName(), EconomyApi.get().format(postage + tax), EconomyApi.get().format(tax))
                : Component.translatable("mail.reikocars.posted", to.getName(), EconomyApi.get().format(postage));
        return ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** A player by name: online, or one who's played here before. */
    @Nullable
    static GameProfile findPlayer(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return online.getGameProfile();
        }
        if (server.getProfileCache() == null) {
            return null;
        }
        return server.getProfileCache().get(name).orElse(null);
    }

    // ---- the mail van ----

    /** The mailboxes beside the mail van, nearest first. Works on either side (the client only sees the blocks). */
    public static List<BlockPos> mailboxesNear(CarEntity van) {
        Level level = van.level();
        double reach = CarsConfig.get(CarsConfig.MAIL_REACH);
        AABB area = van.getBoundingBox().inflate(reach, 1.0, reach);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
            if (level.getBlockState(pos).getBlock() instanceof MailboxBlock) {
                found.add(pos.immutable());
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.getCenter().distanceToSqr(van.position())));
        return found;
    }

    /**
     * The mail van at the mailboxes beside it: collects their outgoing parcels into the mail compartment, then delivers
     * the parcels in it for those mailboxes' owners (and returns to sender the ones whose addressee has no mailbox any
     * more), paying {@code driver} the postage.
     */
    public static void serve(ServerPlayer driver, CarEntity van) {
        List<MailboxBlockEntity> boxes = new ArrayList<>();
        for (BlockPos pos : mailboxesNear(van)) {
            if (van.level().getBlockEntity(pos) instanceof MailboxBlockEntity box) {
                boxes.add(box);
            }
        }
        SimpleContainer bag = van.bed();
        int collected = 0;
        boolean bagFull = false;
        for (MailboxBlockEntity box : boxes) {
            var outbox = box.outbox();
            for (int slot = 0; slot < outbox.getSlots(); slot++) {
                ItemStack parcel = outbox.getStackInSlot(slot);
                if (parcel.isEmpty()) {
                    continue;
                }
                if (!bag.canAddItem(parcel)) {
                    bagFull = true;
                    continue;
                }
                ItemStack taken = outbox.extractItem(slot, parcel.getCount(), false);
                ItemStack left = bag.addItem(taken);
                if (!left.isEmpty()) {
                    outbox.insertItem(slot, left, false); // shouldn't happen: there was room
                }
                collected += taken.getCount() - left.getCount();
            }
        }

        MinecraftServer server = driver.server;
        MailRegistry registry = MailRegistry.get(server);
        int delivered = 0;
        long earned = 0;
        boolean boxFull = false;
        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            ItemStack stack = bag.getItem(slot);
            Parcel parcel = ParcelItem.parcel(stack);
            if (parcel == null) {
                continue;
            }
            MailboxBlockEntity target = mailboxFor(boxes, parcel.to());
            boolean returned = false;
            if (target == null && registry.first(parcel.to()) == null) {
                target = mailboxFor(boxes, parcel.from()); // the addressee has no mailbox any more: back to the sender
                returned = target != null;
            }
            if (target == null) {
                continue;
            }
            ItemStack one = stack.copyWithCount(1);
            int count = stack.getCount();
            int done = 0;
            while (done < count && target.deliver(one)) {
                done++;
            }
            if (done < count) {
                boxFull = true;
            }
            if (done > 0) {
                bag.setItem(slot, stack.copyWithCount(count - done));
                delivered += done;
                earned += parcel.postage() * done;
                notifyRecipient(server, target.owner(), target.getBlockPos(), returned ? parcel.toName() : parcel.fromName(), returned);
            }
        }
        if (earned > 0) {
            EconomyApi.get().deposit(server, driver.getUUID(), earned, "reikocars.mail_delivery");
        }
        bag.setChanged();

        if (collected == 0 && delivered == 0) {
            driver.displayClientMessage(Component.translatable(bagFull ? "mail.reikocars.van_full"
                    : boxFull ? "mail.reikocars.box_full_here" : "mail.reikocars.no_mail_here"), true);
            return;
        }
        sound(van.level(), van.blockPosition(), 1.0F);
        Component message;
        if (delivered > 0 && collected > 0) {
            message = Component.translatable("mail.reikocars.served", parcels(delivered), parcels(collected), EconomyApi.get().format(earned));
        } else if (delivered > 0) {
            message = Component.translatable("mail.reikocars.delivered", parcels(delivered), EconomyApi.get().format(earned));
        } else {
            message = Component.translatable("mail.reikocars.collected", parcels(collected));
        }
        if (bagFull) {
            message = Component.translatable("mail.reikocars.and_van_full", message);
        }
        driver.displayClientMessage(message, true);
    }

    /** "1 parcel" or "3 parcels". */
    private static Component parcels(int count) {
        return count == 1 ? Component.translatable("mail.reikocars.one_parcel") : Component.translatable("mail.reikocars.parcels", count);
    }

    @Nullable
    private static MailboxBlockEntity mailboxFor(List<MailboxBlockEntity> boxes, UUID owner) {
        for (MailboxBlockEntity box : boxes) {
            if (owner.equals(box.owner())) {
                return box;
            }
        }
        return null;
    }

    /** "You've got mail", if they're on. */
    private static void notifyRecipient(MinecraftServer server, @Nullable UUID to, BlockPos at, String name, boolean returned) {
        ServerPlayer player = to == null ? null : server.getPlayerList().getPlayer(to);
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.translatable(returned ? "mail.reikocars.got_returned" : "mail.reikocars.got_mail",
                name, at.getX(), at.getY(), at.getZ()));
        player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6F, 1.5F);
    }

    private static void sound(Level level, BlockPos at, float pitch) {
        level.playSound(null, at, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0F, pitch);
        level.playSound(null, at, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4F, pitch * 1.3F);
    }
}
