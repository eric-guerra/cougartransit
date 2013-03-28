package com.ericguerra.cougartransit.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ericguerra.cougartransit.R;
import com.ericguerra.cougartransit.data.TransitRepository;
import com.ericguerra.cougartransit.domain.EtaCalculator;
import com.ericguerra.cougartransit.model.Feed;
import com.ericguerra.cougartransit.model.Route;
import com.ericguerra.cougartransit.model.Stop;
import com.ericguerra.cougartransit.model.Vehicle;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Map of routes, live shuttles and stops, plus a list of the nearest stops with next arrivals. */
public final class MainActivity extends AppCompatActivity implements OnMapReadyCallback, TransitRepository.Callback {
    private static final int REQ_LOCATION = 1;
    private static final long REFRESH_MS = 15_000;

    private GoogleMap map;
    private Feed feed;
    private final Map<String, Marker> vehicleMarkers = new HashMap<>();
    private StopAdapter adapter;
    private SwipeRefreshLayout swipe;
    private TextView status;
    private FusedLocationProviderClient location;
    private double userLat = Double.NaN, userLng = Double.NaN;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        swipe = findViewById(R.id.swipe);
        swipe.setOnRefreshListener(this::refresh);

        RecyclerView list = findViewById(R.id.stops);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StopAdapter(TransitRepository.get(this).cache().favorites(), stop -> {
            Intent i = new Intent(this, StopActivity.class);
            i.putExtra(StopActivity.EXTRA_STOP_ID, stop.id);
            startActivity(i);
        });
        list.setAdapter(adapter);

        location = LocationServices.getFusedLocationProviderClient(this);
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        requestLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.setFavorites(TransitRepository.get(this).cache().favorites());
        handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll); // don't poll the feed in the background
    }

    private void refresh() {
        swipe.setRefreshing(true);
        TransitRepository.get(this).refresh(this);
    }

    @Override
    public void onFeed(Feed feed, boolean fromCache, long fetchedAt) {
        boolean first = this.feed == null;
        this.feed = feed;
        swipe.setRefreshing(false);
        status.setText(fromCache
                ? getString(R.string.offline_since, DateUtils.getRelativeTimeSpanString(fetchedAt))
                : getString(R.string.updated_just_now));
        drawMap(first);
        updateList();
    }

    @Override
    public void onError(String message) {
        swipe.setRefreshing(false);
        status.setText(message);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Stop) {
                Intent i = new Intent(this, StopActivity.class);
                i.putExtra(StopActivity.EXTRA_STOP_ID, ((Stop) tag).id);
                startActivity(i);
            }
        });
        enableMyLocation();
        if (feed != null) drawMap(true);
    }

    private void drawMap(boolean fitBounds) {
        if (map == null || feed == null) return;
        if (fitBounds) {
            map.clear();
            vehicleMarkers.clear();
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            for (Route r : feed.routes.values()) {
                PolylineOptions line = new PolylineOptions().color(r.color).width(8f);
                for (String id : r.stopIds) line.add(latLng(feed.stops.get(id)));
                line.add(latLng(feed.stops.get(r.stopIds.get(0)))); // close the loop
                map.addPolyline(line);
            }
            for (Stop s : feed.stops.values()) {
                Marker m = map.addMarker(new MarkerOptions().position(latLng(s)).title(s.name)
                        .snippet(getString(R.string.tap_for_arrivals))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                if (m != null) m.setTag(s);
                bounds.include(latLng(s));
            }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
        }
        // Move existing vehicle markers rather than recreating them, so they don't flicker.
        List<String> seen = new ArrayList<>();
        for (Vehicle v : feed.vehicles) {
            seen.add(v.id);
            Marker m = vehicleMarkers.get(v.id);
            LatLng pos = new LatLng(v.lat, v.lng);
            if (m == null) {
                Route r = feed.routes.get(v.routeId);
                m = map.addMarker(new MarkerOptions().position(pos).title(r != null ? r.name : v.routeId)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                vehicleMarkers.put(v.id, m);
            } else {
                m.setPosition(pos);
            }
        }
        vehicleMarkers.keySet().retainAll(seen);
    }

    private void updateList() {
        if (feed == null) return;
        List<Stop> stops = Double.isNaN(userLat)
                ? new ArrayList<>(feed.stops.values())
                : EtaCalculator.nearestStops(feed, userLat, userLng, 8);
        adapter.submit(feed, stops, userLat, userLng, System.currentTimeMillis());
    }

    private static LatLng latLng(Stop s) {
        return new LatLng(s.lat, s.lng);
    }

    // ---- location ----

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
        } else {
            fetchLastLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLastLocation() {
        location.getLastLocation().addOnSuccessListener(loc -> {
            if (loc == null) return;
            userLat = loc.getLatitude();
            userLng = loc.getLongitude();
            updateList();
        });
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (map != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_LOCATION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLastLocation();
            enableMyLocation();
        }
    }
}
