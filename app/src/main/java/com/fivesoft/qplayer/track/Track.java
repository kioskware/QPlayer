package com.fivesoft.qplayer.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Objects;

public abstract class Track {

    public static final int AUDIO = 0, VIDEO = 1, SUBTITLE = 2, UNKNOWN = -1;

    /**
     * Track duration when it is unknown, mostly for live streams.
     */
    public static final long DURATION_UNKNOWN = -1;

    /**
     * Track id. Must be unique among all tracks.
     */

    public String id;

    /**
     * Track title. May be null.
     */
    @Nullable
    public String title;

    /**
     * Track description. May be null.
     */

    @Nullable
    public String description;

    /**
     * Track format. May be null if unknown.<br>
     * Example: "avc", "aac", "srt"
     */
    public String format;

    /**
     * Track duration in milliseconds. May be {@link #DURATION_UNKNOWN}.
     */
    public long duration;

    /**
     * Track language. May be null.
     */

    @Nullable
    public String language;

    /**
     * Track tag. May be used for any purpose.
     */

    public int tag;

    /**
     * Track payload type.
     */

    public int payloadType = UNKNOWN;

    /**
     * Track metadata. May be empty.
     */

    private final HashMap<String, Object> metadata = new HashMap<>();

    public Track(@Nullable String title, @Nullable String format, long duration) {
        this.title = title;
        this.format = format;
        this.duration = duration;
    }

    public Track() {
        this(null, null, DURATION_UNKNOWN);
    }

    /**
     * Gets track metadata value by key.
     * @param key metadata key
     */

    @Nullable
    public Object getMetadata(@NonNull String key) {
        synchronized (metadata) {
            return metadata.get(key);
        }
    }

    /**
     * Sets track metadata value by key.
     * @param key metadata key
     * @param value metadata value
     */

    public void setMetadata(@NonNull String key, @Nullable Object value) {
        synchronized (metadata) {
            metadata.put(key, value);
        }
    }

    /**
     * Removes track metadata value by key.
     * @param key metadata key
     */

    public void removeMetadata(@NonNull String key) {
        synchronized (metadata) {
            metadata.remove(key);
        }
    }

    /**
     * Removes all track metadata.
     */

    public void clearMetadata() {
        synchronized (metadata) {
            metadata.clear();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "\n*****Track*****\n" +
                "id: " + id + "\n" +
                "title: " + title + "\n" +
                "description: " + description + "\n" +
                "format: " + format + "\n" +
                "duration: " + duration + "\n" +
                "language: " + language + "\n" +
                "tag: " + tag + "\n" +
                "payloadType: " + payloadType + "\n" +
                "**********************";
    }
}
