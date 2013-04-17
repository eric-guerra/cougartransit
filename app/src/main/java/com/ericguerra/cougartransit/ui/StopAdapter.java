package com.ericguerra.cougartransit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ericguerra.cougartransit.R;
import com.ericguerra.cougartransit.domain.EtaCalculator;
import com.ericguerra.cougartransit.domain.Geo;
import com.ericguerra.cougartransit.model.Feed;
import com.ericguerra.cougartransit.model.Stop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Stop rows: favorites first, then by distance; each shows the next two arrivals. */
final class StopAdapter extends RecyclerView.Adapter<StopAdapter.Holder> {
    interface OnStopClick {
        void onClick(Stop stop);
    }

    private final OnStopClick listener;
    private Set<String> favorites;
    private final List<Row> rows = new ArrayList<>();

    private static final class Row {
        final Stop stop;
        final String arrivals;
        final String distance;
        final boolean favorite;

        Row(Stop stop, String arrivals, String distance, boolean favorite) {
            this.stop = stop;
            this.arrivals = arrivals;
            this.distance = distance;
            this.favorite = favorite;
        }
    }

    StopAdapter(Set<String> favorites, OnStopClick listener) {
        this.favorites = favorites;
        this.listener = listener;
    }

    void setFavorites(Set<String> favorites) {
        this.favorites = favorites;
    }

    void submit(Feed feed, List<Stop> stops, double lat, double lng, long now) {
        rows.clear();
        for (Stop s : stops) {
            List<EtaCalculator.Arrival> arrivals = EtaCalculator.arrivalsAt(feed, s.id, now);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(2, arrivals.size()); i++) {
                if (i > 0) sb.append(" · ");
                sb.append(arrivals.get(i).routeName).append(": ").append(arrivals.get(i).label());
            }
            String distance = Double.isNaN(lat) ? "" :
                    Geo.walkingMinutes(Geo.distanceMeters(lat, lng, s.lat, s.lng)) + " min walk";
            rows.add(new Row(s, sb.length() > 0 ? sb.toString() : "No shuttles running", distance, favorites.contains(s.id)));
        }
        // Stable sort keeps the distance ordering within favorites and non-favorites.
        Collections.sort(rows, (a, b) -> Boolean.compare(b.favorite, a.favorite));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stop, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Row r = rows.get(position);
        h.name.setText((r.favorite ? "★ " : "") + r.stop.name);
        h.arrivals.setText(r.arrivals);
        h.distance.setText(r.distance);
        h.itemView.setOnClickListener(v -> listener.onClick(r.stop));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, arrivals, distance;

        Holder(View v) {
            super(v);
            name = v.findViewById(R.id.stop_name);
            arrivals = v.findViewById(R.id.stop_arrivals);
            distance = v.findViewById(R.id.stop_distance);
        }
    }
}
