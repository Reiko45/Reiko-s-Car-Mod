package com.reiko.cars.gps;

import com.reiko.cars.network.GpsCarsPayload;
import com.reiko.cars.network.GpsPlacesPayload;
import java.util.List;

/**
 * What the server last told this client's GPS: the places nearby and where the player's cars are. (Kept here, out of
 * the client classes, so the packet handlers don't touch them on a server.)
 */
public final class GpsInbox {
    private static List<GpsPlacesPayload.Entry> places = List.of();
    private static int placesVersion;
    private static List<GpsCarsPayload.Entry> cars = List.of();
    private static int carsVersion;

    private GpsInbox() {
    }

    public static void receive(GpsPlacesPayload payload) {
        places = List.copyOf(payload.places());
        placesVersion++;
    }

    public static void receive(GpsCarsPayload payload) {
        cars = List.copyOf(payload.cars());
        carsVersion++;
    }

    public static List<GpsPlacesPayload.Entry> places() {
        return places;
    }

    /** Goes up each time places arrive, so a screen can tell they have. */
    public static int placesVersion() {
        return placesVersion;
    }

    public static List<GpsCarsPayload.Entry> cars() {
        return cars;
    }

    public static int carsVersion() {
        return carsVersion;
    }

    /** Forgets it all, on leaving a world. */
    public static void clear() {
        places = List.of();
        cars = List.of();
        placesVersion++;
        carsVersion++;
    }
}
