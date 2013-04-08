package com.ericguerra.cougartransit.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ericguerra.cougartransit.R;
import com.ericguerra.cougartransit.data.TransitRepository;
import com.ericguerra.cougartransit.domain.EtaCalculator;
import com.ericguerra.cougartransit.model.Feed;
import com.ericguerra.cougartransit.model.Stop;

import java.util.ArrayList;
import java.util.List;

/** Upcoming arrivals at one stop, refreshed every 15 seconds, with a favorite toggle. */
public final class StopActivity extends AppCompatActivity implements TransitRepository.Callback {
    public static final String EXTRA_STOP_ID = "stop_id";

    private String stopId;
    private ArrayAdapter<String> adapter;
    private TextView empty;
    private boolean favorite;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            TransitRepository.get(StopActivity.this).refresh(StopActivity.this);
            handler.postDelayed(this, 15_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        stopId = getIntent().getStringExtra(EXTRA_STOP_ID);
        favorite = TransitRepository.get(this).cache().favorites().contains(stopId);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        ListView list = findViewById(R.id.arrivals);
        empty = findViewById(R.id.empty);
        list.setEmptyView(empty);
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
    }

    @Override
    public void onFeed(Feed feed, boolean fromCache, long fetchedAt) {
        Stop stop = feed.stops.get(stopId);
        if (stop == null) {
            empty.setText(R.string.stop_gone);
            return;
        }
        setTitle(stop.name);
        List<String> rows = new ArrayList<>();
        for (EtaCalculator.Arrival a : EtaCalculator.arrivalsAt(feed, stopId, System.currentTimeMillis())) {
            rows.add(a.routeName + " — " + a.label() + "  (bus " + a.vehicleId + ")");
        }
        adapter.clear();
        adapter.addAll(rows);
        empty.setText(fromCache ? R.string.offline_no_live : R.string.no_shuttles);
    }

    @Override
    public void onError(String message) {
        empty.setText(message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, 1, Menu.NONE, favorite ? R.string.unfavorite : R.string.favorite);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            favorite = TransitRepository.get(this).cache().toggleFavorite(stopId);
            invalidateOptionsMenu();
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
