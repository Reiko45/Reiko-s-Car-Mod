package com.reiko.cars.traffic;

import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.ShopActionResult;
import java.util.OptionalLong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Owners setting their cameras' fines and their signs' limits. */
public final class RoadSignService {
    private RoadSignService() {
    }

    /**
     * @param fineText the fine, as money; 0 turns the camera off
     * @param limitText a speed sign's limit, a whole number (ignored for a traffic light)
     */
    public static ShopActionResult set(ServerPlayer player, OwnedRoadsideBlockEntity sign, String fineText, String limitText, boolean kmh) {
        if (!sign.canManage(player)) {
            return ShopActionResult.fail("traffic.reikocars.owner_only", sign.ownerName());
        }
        EconomyApi eco = EconomyApi.get();
        OptionalLong fine = eco.parse(fineText.trim());
        if (fine.isEmpty() || fine.getAsLong() < 0 || fine.getAsLong() > Money.MAX_BALANCE / 100) {
            return ShopActionResult.fail("traffic.reikocars.bad_fine");
        }
        if (sign instanceof SpeedSignBlockEntity speed) {
            int limit;
            try {
                limit = Integer.parseInt(limitText.trim());
            } catch (NumberFormatException e) {
                return ShopActionResult.fail("traffic.reikocars.bad_limit");
            }
            if (limit < 5 || limit > 200) {
                return ShopActionResult.fail("traffic.reikocars.bad_limit");
            }
            speed.setLimit(limit, kmh);
            speed.setFine(fine.getAsLong());
            Component shown = Component.translatable(kmh ? "traffic.reikocars.kmh" : "traffic.reikocars.mph", limit);
            return ShopActionResult.ok(fine.getAsLong() > 0
                    ? Component.translatable("traffic.reikocars.speed_set", shown, eco.format(fine.getAsLong()))
                    : Component.translatable("traffic.reikocars.speed_set_no_camera", shown));
        }
        sign.setFine(fine.getAsLong());
        return ShopActionResult.ok(fine.getAsLong() > 0
                ? Component.translatable("traffic.reikocars.light_set", eco.format(fine.getAsLong()))
                : Component.translatable("traffic.reikocars.light_set_no_camera"));
    }
}
