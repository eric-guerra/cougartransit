package com.ericguerra.cougartransit.model;

public final class Stop {
    public final String id;
    public final String name;
    public final double lat;
    public final double lng;

    public Stop(String id, String name, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public String toString() {
        return name;
    }
}
