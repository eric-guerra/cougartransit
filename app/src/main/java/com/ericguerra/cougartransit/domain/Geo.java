package com.ericguerra.cougartransit.domain;

public final class Geo {
    private static final double EARTH_RADIUS_M = 6_371_000;

    private Geo() {}

    /** Great-circle distance in meters (haversine). */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    /** Walking time at 1.3 m/s, rounded up to whole minutes. */
    public static int walkingMinutes(double meters) {
        return (int) Math.ceil(meters / 1.3 / 60.0);
    }
}
