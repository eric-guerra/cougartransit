package com.ericguerra.cougartransit.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.ericguerra.cougartransit.BuildConfig;
import com.ericguerra.cougartransit.model.Feed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches the feed off the main thread, falls back to the cached copy on failure,
 * and delivers results on the main thread.
 */
public final class TransitRepository {
    public interface Callback {
        void onFeed(Feed feed, boolean fromCache, long fetchedAt);

        void onError(String message);
    }

    private static volatile TransitRepository instance;

    private final FeedCache cache;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private TransitRepository(Context context) {
        this.cache = new FeedCache(context.getApplicationContext());
    }

    public static TransitRepository get(Context context) {
        if (instance == null) {
            synchronized (TransitRepository.class) {
                if (instance == null) instance = new TransitRepository(context);
            }
        }
        return instance;
    }

    public FeedCache cache() {
        return cache;
    }

    public void refresh(final Callback cb) {
        io.execute(() -> {
            try {
                String json = download(BuildConfig.FEED_URL);
                Feed feed = FeedParser.parse(json);
                long now = System.currentTimeMillis();
                cache.saveFeed(json, now);
                main.post(() -> cb.onFeed(feed, false, now));
            } catch (Exception e) {
                Object[] cached = cache.loadFeed();
                if (cached != null) {
                    try {
                        Feed feed = FeedParser.parse((String) cached[0]);
                        main.post(() -> cb.onFeed(feed, true, (Long) cached[1]));
                        return;
                    } catch (Exception ignored) {
                        // fall through to error
                    }
                }
                main.post(() -> cb.onError("Couldn't reach the transit feed: " + e.getMessage()));
            }
        });
    }

    private static String download(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        try {
            if (conn.getResponseCode() != 200) throw new IOException("HTTP " + conn.getResponseCode());
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8");
            }
        } finally {
            conn.disconnect();
        }
    }
}
