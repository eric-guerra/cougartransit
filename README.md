# CougarTransit

A native **Android (Java)** app for tracking campus shuttles: a live map of routes and buses, the nearest stops to you with walking time, predicted arrivals, favorite stops, and offline fallback.

> Portfolio reference implementation of a 2012 university project, built with Java on the Android SDK and Google Maps, using current AndroidX dependencies.

## Features

- Google Map with each route drawn as a colored loop, stop markers, and live bus markers that move without flicker
- Bottom list of the nearest stops (fused location), favorites pinned to the top, with the next two arrivals and walking minutes
- Stop screen with all upcoming arrivals and a favorite toggle
- Polls the feed every 15 seconds only while the screen is visible, plus pull-to-refresh
- **Offline mode**: the last good feed is cached in SQLite and shown with an "Offline since…" banner when the network fails
- ETA model: hops remaining around the loop × the route's average seconds per hop, minus seconds since the bus reported; positions older than 2 minutes are ignored

## Structure

```
model/    Stop, Route, Vehicle, Feed
domain/   EtaCalculator, Geo (haversine, walking time)   ← pure Java, unit tested
data/     FeedParser (org.json), FeedCache (SQLiteOpenHelper), TransitRepository (background fetch + fallback)
ui/       MainActivity (map + list), StopActivity, StopAdapter
```

## Build

Add your keys to `local.properties`:

```
MAPS_API_KEY=your-google-maps-key
FEED_URL=https://your-campus-feed/feed.json
```

```bash
./gradlew assembleDebug
./gradlew test          # JVM unit tests for parsing, ETA and geo logic
```

The feed format is documented in [FeedParser.java](app/src/main/java/com/ericguerra/cougartransit/data/FeedParser.java).
