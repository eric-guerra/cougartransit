package com.ericguerra.cougartransit.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** One snapshot of the transit feed. */
public final class Feed {
    public final Map<String, Stop> stops;
    public final Map<String, Route> routes;
    public final List<Vehicle> vehicles;
    /** Average seconds between consecutive stops, per route. */
    public final Map<String, Integer> secondsPerHop;
    public final long generatedAtMillis;

    public Feed(Map<String, Stop> stops, Map<String, Route> routes, List<Vehicle> vehicles,
                Map<String, Integer> secondsPerHop, long generatedAtMillis) {
        this.stops = Collections.unmodifiableMap(stops);
        this.routes = Collections.unmodifiableMap(routes);
        this.vehicles = Collections.unmodifiableList(vehicles);
        this.secondsPerHop = Collections.unmodifiableMap(secondsPerHop);
        this.generatedAtMillis = generatedAtMillis;
    }
}
