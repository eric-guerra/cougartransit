package com.ericguerra.cougartransit.domain;

import com.ericguerra.cougartransit.data.FeedParser;
import com.ericguerra.cougartransit.model.Feed;
import com.ericguerra.cougartransit.model.Stop;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EtaCalculatorTest {
    private static final long NOW = 1_350_000_000_000L;

    private static final String FEED = "{"
            + "\"generatedAt\":" + NOW + ","
            + "\"stops\":["
            + "{\"id\":\"a\",\"name\":\"Student Center\",\"lat\":29.7199,\"lng\":-95.3422},"
            + "{\"id\":\"b\",\"name\":\"Library\",\"lat\":29.7213,\"lng\":-95.3410},"
            + "{\"id\":\"c\",\"name\":\"Engineering\",\"lat\":29.7240,\"lng\":-95.3400},"
            + "{\"id\":\"d\",\"name\":\"Stadium\",\"lat\":29.7225,\"lng\":-95.3490}],"
            + "\"routes\":["
            + "{\"id\":\"red\",\"name\":\"Red Loop\",\"color\":\"#C8102E\",\"stops\":[\"a\",\"b\",\"c\",\"d\"],\"secondsPerHop\":100},"
            + "{\"id\":\"blue\",\"name\":\"Blue Loop\",\"stops\":[\"d\",\"a\"]}],"
            + "\"vehicles\":["
            + "{\"id\":\"11\",\"route\":\"red\",\"lat\":29.72,\"lng\":-95.34,\"lastStop\":0,\"reportedAt\":" + (NOW - 10_000) + "},"
            + "{\"id\":\"12\",\"route\":\"red\",\"lat\":29.72,\"lng\":-95.34,\"lastStop\":2,\"reportedAt\":" + NOW + "},"
            + "{\"id\":\"21\",\"route\":\"blue\",\"lat\":29.72,\"lng\":-95.34,\"lastStop\":0,\"reportedAt\":" + NOW + "},"
            + "{\"id\":\"old\",\"route\":\"red\",\"lat\":29.72,\"lng\":-95.34,\"lastStop\":1,\"reportedAt\":" + (NOW - 600_000) + "},"
            + "{\"id\":\"bad\",\"route\":\"red\",\"lastStop\":9}"
            + "]}";

    private static Feed feed() throws Exception {
        return FeedParser.parse(FEED);
    }

    @Test
    public void parsesFeedAndSkipsInvalidVehicles() throws Exception {
        Feed f = feed();
        assertEquals(4, f.stops.size());
        assertEquals(2, f.routes.size());
        assertEquals(4, f.vehicles.size()); // "bad" has an out-of-range stop index
        assertEquals(0xFFC8102E, f.routes.get("red").color);
        assertEquals(120, (int) f.secondsPerHop.get("blue"));
    }

    @Test
    public void etaUsesHopsAroundTheLoopMinusElapsedTime() throws Exception {
        List<EtaCalculator.Arrival> atC = EtaCalculator.arrivalsAt(feed(), "c", NOW);
        // bus 11 left a (index 0): 2 hops * 100s - 10s elapsed = 190s
        // bus 12 left c (index 2): full loop = 4 hops * 100s = 400s
        assertEquals(2, atC.size());
        assertEquals("11", atC.get(0).vehicleId);
        assertEquals(190, atC.get(0).seconds);
        assertEquals("12", atC.get(1).vehicleId);
        assertEquals(400, atC.get(1).seconds);
    }

    @Test
    public void includesEveryRouteServingTheStopSortedBySoonest() throws Exception {
        List<EtaCalculator.Arrival> atA = EtaCalculator.arrivalsAt(feed(), "a", NOW);
        // blue bus left d (index 0) -> a is 1 hop at default 120s; red 12 left c -> a is 2 hops = 200s
        assertEquals("21", atA.get(0).vehicleId);
        assertEquals(120, atA.get(0).seconds);
        assertEquals("2 min", atA.get(0).label());
        for (int i = 1; i < atA.size(); i++) assertTrue(atA.get(i - 1).seconds <= atA.get(i).seconds);
    }

    @Test
    public void staleVehiclesAreIgnored() throws Exception {
        for (EtaCalculator.Arrival a : EtaCalculator.arrivalsAt(feed(), "b", NOW)) {
            assertTrue(!a.vehicleId.equals("old"));
        }
    }

    @Test
    public void nearestStopsSortedByDistance() throws Exception {
        List<Stop> near = EtaCalculator.nearestStops(feed(), 29.7200, -95.3420, 2);
        assertEquals(2, near.size());
        assertEquals("a", near.get(0).id);
        assertEquals("b", near.get(1).id);
    }

    @Test(expected = org.json.JSONException.class)
    public void rejectsRoutesWithUnknownStops() throws Exception {
        FeedParser.parse("{\"stops\":[],\"routes\":[{\"id\":\"x\",\"name\":\"X\",\"stops\":[\"nope\"]}]}");
    }
}
