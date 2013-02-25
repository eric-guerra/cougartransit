package com.ericguerra.cougartransit.model;

import java.util.Collections;
import java.util.List;

/** A shuttle loop: an ordered list of stop ids, driven repeatedly in that order. */
public final class Route {
    public final String id;
    public final String name;
    public final int color;
    public final List<String> stopIds;

    public Route(String id, String name, int color, List<String> stopIds) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.stopIds = Collections.unmodifiableList(stopIds);
    }

    public int indexOf(String stopId) {
        return stopIds.indexOf(stopId);
    }

    /** Number of stops to travel from {@code from} to {@code to} going forward around the loop. */
    public int hopsBetween(int from, int to) {
        int n = stopIds.size();
        return ((to - from) % n + n) % n;
    }
}
