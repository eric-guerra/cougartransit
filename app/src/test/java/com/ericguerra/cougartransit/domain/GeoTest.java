package com.ericguerra.cougartransit.domain;

import com.ericguerra.cougartransit.model.Route;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class GeoTest {
    @Test
    public void haversineMatchesKnownDistance() {
        // Houston (IAH) to Austin (AUS) is about 225 km great-circle.
        double m = Geo.distanceMeters(29.9902, -95.3368, 30.1975, -97.6664);
        assertEquals(225_000, m, 10_000);
        assertEquals(0, Geo.distanceMeters(29.72, -95.34, 29.72, -95.34), 1e-9);
    }

    @Test
    public void walkingMinutesRoundsUp() {
        assertEquals(1, Geo.walkingMinutes(10));
        assertEquals(2, Geo.walkingMinutes(79));
        assertEquals(0, Geo.walkingMinutes(0));
    }

    @Test
    public void hopsWrapAroundTheLoop() {
        Route r = new Route("r", "R", 0, Arrays.asList("a", "b", "c", "d"));
        assertEquals(2, r.hopsBetween(0, 2));
        assertEquals(1, r.hopsBetween(3, 0));
        assertEquals(0, r.hopsBetween(2, 2));
    }
}
