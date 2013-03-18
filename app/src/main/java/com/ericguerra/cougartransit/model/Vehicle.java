package com.ericguerra.cougartransit.model;

/** Live position of a shuttle. {@code lastStopIndex} is the last stop it departed on its route. */
public final class Vehicle {
    public final String id;
    public final String routeId;
    public final double lat;
    public final double lng;
    public final int lastStopIndex;
    public final long reportedAtMillis;

    public Vehicle(String id, String routeId, double lat, double lng, int lastStopIndex, long reportedAtMillis) {
        this.id = id;
        this.routeId = routeId;
        this.lat = lat;
        this.lng = lng;
        this.lastStopIndex = lastStopIndex;
        this.reportedAtMillis = reportedAtMillis;
    }
}
