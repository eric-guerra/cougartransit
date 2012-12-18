package com.ericguerra.cougartransit.data;

import com.ericguerra.cougartransit.model.Feed;
import com.ericguerra.cougartransit.model.Route;
import com.ericguerra.cougartransit.model.Stop;
import com.ericguerra.cougartransit.model.Vehicle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the campus transit JSON feed:
 * <pre>
 * { "generatedAt": 1350000000000,
 *   "stops":   [{ "id", "name", "lat", "lng" }],
 *   "routes":  [{ "id", "name", "color": "#C8102E", "stops": ["s1","s2"], "secondsPerHop": 95 }],
 *   "vehicles":[{ "id", "route", "lat", "lng", "lastStop": 3, "reportedAt": 1350000000000 }] }
 * </pre>
 * Malformed vehicles are skipped rather than failing the whole feed.
 */
public final class FeedParser {
    private FeedParser() {}

    public static Feed parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        Map<String, Stop> stops = new LinkedHashMap<>();
        JSONArray stopArr = root.getJSONArray("stops");
        for (int i = 0; i < stopArr.length(); i++) {
            JSONObject s = stopArr.getJSONObject(i);
            stops.put(s.getString("id"), new Stop(s.getString("id"), s.getString("name"), s.getDouble("lat"), s.getDouble("lng")));
        }

        Map<String, Route> routes = new LinkedHashMap<>();
        Map<String, Integer> perHop = new LinkedHashMap<>();
        JSONArray routeArr = root.getJSONArray("routes");
        for (int i = 0; i < routeArr.length(); i++) {
            JSONObject r = routeArr.getJSONObject(i);
            List<String> ids = new ArrayList<>();
            JSONArray rs = r.getJSONArray("stops");
            for (int j = 0; j < rs.length(); j++) {
                String id = rs.getString(j);
                if (!stops.containsKey(id)) throw new JSONException("route " + r.getString("id") + " references unknown stop " + id);
                ids.add(id);
            }
            if (ids.isEmpty()) continue;
            String id = r.getString("id");
            routes.put(id, new Route(id, r.getString("name"), parseColor(r.optString("color", "#555555")), ids));
            perHop.put(id, r.optInt("secondsPerHop", 120));
        }

        List<Vehicle> vehicles = new ArrayList<>();
        JSONArray vehArr = root.optJSONArray("vehicles");
        if (vehArr != null) {
            for (int i = 0; i < vehArr.length(); i++) {
                JSONObject v = vehArr.optJSONObject(i);
                if (v == null) continue;
                Route route = routes.get(v.optString("route"));
                int last = v.optInt("lastStop", -1);
                if (route == null || last < 0 || last >= route.stopIds.size()) continue;
                vehicles.add(new Vehicle(v.getString("id"), route.id, v.optDouble("lat"), v.optDouble("lng"), last, v.optLong("reportedAt")));
            }
        }

        return new Feed(stops, routes, vehicles, perHop, root.optLong("generatedAt"));
    }

    static int parseColor(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) return 0xFF555555;
        try {
            return 0xFF000000 | Integer.parseInt(h, 16);
        } catch (NumberFormatException e) {
            return 0xFF555555;
        }
    }
}
