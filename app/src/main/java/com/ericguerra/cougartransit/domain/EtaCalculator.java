package com.ericguerra.cougartransit.domain;

import com.ericguerra.cougartransit.model.Feed;
import com.ericguerra.cougartransit.model.Route;
import com.ericguerra.cougartransit.model.Stop;
import com.ericguerra.cougartransit.model.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estimates arrivals at a stop from vehicle positions: the hops remaining around the loop
 * times the route's average seconds per hop, minus time already elapsed since the vehicle
 * reported. Positions older than {@link #STALE_AFTER_MS} are ignored.
 */
public final class EtaCalculator {
    public static final long STALE_AFTER_MS = 2 * 60 * 1000;

    public static final class Arrival implements Comparable<Arrival> {
        public final String vehicleId;
        public final String routeId;
        public final String routeName;
        public final int seconds;

        Arrival(String vehicleId, String routeId, String routeName, int seconds) {
            this.vehicleId = vehicleId;
            this.routeId = routeId;
            this.routeName = routeName;
            this.seconds = seconds;
        }

        public String label() {
            if (seconds < 60) return "Arriving";
            return (seconds / 60) + " min";
        }

        @Override
        public int compareTo(Arrival o) {
            return Integer.compare(seconds, o.seconds);
        }
    }

    private EtaCalculator() {}

    public static List<Arrival> arrivalsAt(Feed feed, String stopId, long nowMillis) {
        List<Arrival> out = new ArrayList<>();
        for (Vehicle v : feed.vehicles) {
            if (nowMillis - v.reportedAtMillis > STALE_AFTER_MS) continue;
            Route route = feed.routes.get(v.routeId);
            if (route == null) continue;
            int target = route.indexOf(stopId);
            if (target < 0) continue;

            int hops = route.hopsBetween(v.lastStopIndex, target);
            if (hops == 0) hops = route.stopIds.size(); // just left this stop: next visit is a full loop away
            Integer perHop = feed.secondsPerHop.get(route.id);
            int seconds = hops * (perHop != null ? perHop : 120);
            seconds -= (int) ((nowMillis - v.reportedAtMillis) / 1000);
            out.add(new Arrival(v.id, route.id, route.name, Math.max(0, seconds)));
        }
        Collections.sort(out);
        return out;
    }

    /** Stops sorted by distance from the user, limited to {@code max}. */
    public static List<Stop> nearestStops(Feed feed, double lat, double lng, int max) {
        List<Stop> stops = new ArrayList<>(feed.stops.values());
        Collections.sort(stops, (a, b) -> Double.compare(
                Geo.distanceMeters(lat, lng, a.lat, a.lng), Geo.distanceMeters(lat, lng, b.lat, b.lng)));
        return stops.subList(0, Math.min(max, stops.size()));
    }
}
