package com.reiko.cars.car;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.finance.LoanService;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Every second, cars on their way to a lot are put down where they can be; every five seconds, car loans fall due. */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class LotTicker {
    private LotTicker() {
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % 20 == 0) {
            CarDeliveries.get(server).tick(server);
        }
        if (tick % 100 == 50) {
            LoanService.tick(server);
        }
        if (tick % 5 == 0) {
            com.reiko.cars.dmv.DrivingTests.tick(server, tick % 20 == 0);
        }
    }
}
