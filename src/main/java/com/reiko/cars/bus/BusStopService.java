package com.reiko.cars.bus;

import com.reiko.economy.shop.ShopActionResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;

/** An owner naming their bus stop and putting it on routes. */
public final class BusStopService {
    /** The highest route number, and the highest stop number along one. */
    public static final int MAX_ROUTE = 99;
    public static final int MAX_NUMBER = 99;

    private BusStopService() {
    }

    /**
     * @param numbers the routes and the stop's number on each, in pairs (route, number); a pair of 0s is no route
     */
    public static ShopActionResult configure(ServerPlayer player, BusStopBlockEntity stop, String name, int[] numbers) {
        if (!stop.canManage(player) || stop.owner() == null) {
            return ShopActionResult.fail("bus.reikocars.owner_only", stop.ownerName());
        }
        String tidy = StringUtil.filterText(name.trim()).replaceAll("\\s+", " ");
        if (tidy.length() > BusStopBlockEntity.MAX_NAME) {
            return ShopActionResult.fail("bus.reikocars.name_too_long", BusStopBlockEntity.MAX_NAME);
        }
        List<BusStopBlockEntity.RouteStop> routes = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i + 1 < numbers.length && i / 2 < BusStopBlockEntity.MAX_ROUTES; i += 2) {
            int route = numbers[i];
            int number = numbers[i + 1];
            if (route == 0 && number == 0) {
                continue;
            }
            if (route < 1 || route > MAX_ROUTE || number < 1 || number > MAX_NUMBER) {
                return ShopActionResult.fail("bus.reikocars.bad_route", MAX_ROUTE, MAX_NUMBER);
            }
            if (!seen.add(route)) {
                return ShopActionResult.fail("bus.reikocars.route_twice", route);
            }
            routes.add(new BusStopBlockEntity.RouteStop(route, number));
        }
        stop.configure(tidy, routes);
        Component shown = stop.displayName();
        return ShopActionResult.ok(routes.isEmpty() ? Component.translatable("bus.reikocars.saved_no_routes", shown)
                : Component.translatable("bus.reikocars.saved", shown, stop.routesText()));
    }
}
