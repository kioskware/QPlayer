package com.fivesoft.qplayer.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.Csd;
import com.fivesoft.qplayer.bas2.common.Constants;
import com.fivesoft.qplayer.common.ListenerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Base class for all tracks classes.<br>
 * Tracks are used to store information about tracks of media stream like its id, title, description, format, duration,
 * language, tag, payload type, clock rate, codec specific data (CSD) and metadata.<br>
 * <p>
 *     More specific track classes are {@link AudioTrack}, {@link VideoTrack} and {@link SubtitleTrack}.
 * </p>
 */

public abstract class Track extends ListenerManager<TrackChangeListener> {

    /*
     * Field names used in onTrackFieldChanged method of TrackChangeListener.
     */
    public static final String FIELD_TITLE = "title", FIELD_DESCRIPTION = "description", FIELD_FORMAT = "format",
            FIELD_DURATION = "duration", FIELD_LANGUAGE = "language", FIELD_TAG = "tag", FIELD_PAYLOAD_TYPE = "payloadType",
            FIELD_CLOCK_RATE = "clockRate", FIELD_CSD = "csd", FIELD_METADATA = "metadata";

    public static final int AUDIO = 0, VIDEO = 1, SUBTITLE = 2, UNKNOWN = Constants.UNKNOWN_VALUE;

    private final String id;
    @Nullable
    private String title;
    @Nullable
    private String description;
    private String format;
    private long duration;
    @Nullable
    private String language;
    private int tag;
    private final int payloadType;
    private int clockRate;

    private final Csd csd = new Csd();

    private final HashMap<String, Object> metadata = new HashMap<>();

    /**
     * Creates track with specified id and payload type.
     * @param id track id
     * @param payloadType track payload type
     */

    public Track(@NonNull String id, int payloadType) {
        this(id, null, null, null, UNKNOWN, null, 0, payloadType, UNKNOWN);
    }

    /**
     * Creates track with specified id, title, format, duration and payload type.
     * @param id track id
     * @param title track title
     * @param description track description
     * @param format track format name
     * @param duration track duration in milliseconds
     * @param language track language in ISO 639-1 format. Example: "en"
     * @param tag track tag (used for any purpose)
     * @param payloadType track payload type
     * @param clockRate track clock rate in Hz
     * @throws IllegalArgumentException if language is not in ISO 639-1 format
     */

    public Track(String id, @Nullable String title, @Nullable String description, String format, long duration,
                 @Nullable String language, int tag, int payloadType, int clockRate) {

        this.id = id;
        setTitle(title);
        setDescription(description);
        setFormat(format);
        setDuration(duration);
        setLanguage(language);
        setTag(tag);
        this.payloadType = payloadType;
        setClockRate(clockRate);
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
     * Copies all track metadata to target map.
     * @param target target map
     */
    public void getAllMetadata(@NonNull Map<String, Object> target) {
        synchronized (metadata) {
            Objects.requireNonNull(target).putAll(metadata);
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

    /**
     * Gets codec specific data for the track.<br>
     * @return codec specific data of this track.
     */

    @NonNull
    public Csd getCsd() {
        return csd;
    }

    /**
     * Gets unique track id among all tracks in the player.
     * @return track id
     */

    public String getId() {
        return id;
    }

    public int getPayloadType() {
        return payloadType;
    }

    /**
     * Gets track title. May be null.
     * @return track title or null
     */

    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * Sets track title. May be null.
     * @param title track title
     */

    public void setTitle(@Nullable String title) {
        if(!Objects.equals(this.title, title)){
            this.title = title;
            notifyUpdated(FIELD_TITLE, title);
        }
    }

    /**
     * Gets description of the track. May be null.
     * @return description of the track
     */

    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Sets description oi the track. May be null.
     * @param description description of the track
     */

    public void setDescription(@Nullable String description) {
        if(!Objects.equals(this.description, description)){
            this.description = description;
            notifyUpdated(FIELD_DESCRIPTION, description);
        }
    }

    /**
     * Gets track format. This is not a mime type, it is a codec name.<br>
     * Example: "h264", "mpeg4-generic"
     * @return track format
     */

    @Nullable
    public String getFormat() {
        return format;
    }

    /**
     * Sets track format. This is not a mime type, it is a codec name.<br>
     * Example: "h264", "mpeg4-generic"
     * @param format track format
     */

    public void setFormat(@Nullable String format) {
        if(!Objects.equals(this.format, format)){
            this.format = format;
            notifyUpdated(FIELD_FORMAT, format);
        }
    }

    /**
     * Gets track duration in milliseconds if known.
     * @return track duration in milliseconds or {@link #UNKNOWN}
     */

    public long getDuration() {
        return duration;
    }

    /**
     * Sets track duration in milliseconds. Marks duration as unknown if it is negative.
     * @param duration track duration in milliseconds
     */

    public void setDuration(long duration) {
        long newDuration = Math.max(UNKNOWN, duration);
        if(this.duration != newDuration){
            this.duration = newDuration;
            notifyUpdated(FIELD_DURATION, duration);
        }
    }

    /**
     * Track language code in ISO 639-1 format. May be null. Example: "en", "ru"
     * @return track language code in ISO 639-1 format or null
     */

    @Nullable
    public String getLanguage() {
        return language;
    }

    /**
     * Sets track language code in ISO 639-1 format. May be null. Example: "en", "ru"
     * @param language track language code in ISO 639-1 format or null
     */

    public void setLanguage(@Nullable String language) {
        if(language != null){
            language = language.toLowerCase();
            if(language.length() != 2 || !language.matches("[a-z]+")){
                throw new IllegalArgumentException("language must be in ISO 639-1 format");
            }
        }

        if(!Objects.equals(this.language, language)){
            this.language = language;
            notifyUpdated(FIELD_LANGUAGE, language);
        }
    }

    /**
     * Gets track tag. May be used for any purpose.<br>
     * Tag is not used by core library, so it may be changed safely.
     * @return track tag
     */

    public int getTag() {
        return tag;
    }

    /**
     * Sets track tag. May be used for any purpose.<br>
     * Tag is not used by core library, so it may be changed safely.
     * @param tag track tag
     */

    public void setTag(int tag) {
        if(this.tag != tag){
            this.tag = tag;
            notifyUpdated(FIELD_TAG, tag);
        }
    }

    /**
     * Gets track clock rate in Hz or {@link #UNKNOWN} if unknown.
     * @return track clock rate in Hz
     */

    public int getClockRate() {
        return clockRate;
    }

    /**
     * Sets track clock rate in Hz. Marks clock rate as unknown if it is negative.
     * @param clockRate track clock rate in Hz
     */

    public void setClockRate(int clockRate) {
        int newClockRate = Math.max(Constants.UNKNOWN_VALUE, clockRate);
        if(this.clockRate != newClockRate){
            this.clockRate = newClockRate;
            notifyUpdated(FIELD_CLOCK_RATE, clockRate);
        }
    }

    @Override
    public String toString() {
        return "Track{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", format='" + format + '\'' +
                ", duration=" + duration +
                ", language='" + language + '\'' +
                ", tag=" + tag +
                ", payloadType=" + payloadType +
                ", clockRate=" + clockRate +
                ", metadata=" + metadata +
                '}';
    }

    /**
     * Notifies all listeners about track field change.
     * @param field field name
     * @param value new field value
     * @throws NullPointerException if field is null
     */

    protected void notifyUpdated(@NonNull String field, @Nullable Object value) {
        Objects.requireNonNull(field);
        forEachListener((s, l) -> l.onTrackFieldChanged(Track.this, field, value));
    }

    public static final class Builder {

        private String id;
        private String title;
        private String description;
        private String format;
        private long duration;
        private String language;
        private int tag;
        private int payloadType;

        private int clockRate;

        private final int type;

        private int width;
        private int height;

        private int channels;

        private float fps;

        private String mode;

        private final Csd csd = new Csd();

        private final HashMap<String, Object> metadata = new HashMap<>();

        public Builder(int type) {
            this.type = type;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setFormat(String format) {
            this.format = format;
            return this;
        }

        public Builder setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }

        public Builder setTag(int tag) {
            this.tag = tag;
            return this;
        }

        public Builder setPayloadType(int payloadType) {
            this.payloadType = payloadType;
            return this;
        }

        public Builder setMetadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public Builder setMetadata(HashMap<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setChannels(int channels) {
            this.channels = channels;
            return this;
        }

        public Builder setFps(float fps) {
            this.fps = fps;
            return this;
        }

        public Builder setMode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder setClockRate(int clockRate) {
            this.clockRate = clockRate;
            return this;
        }

        public Builder setCsd(int index, byte[] csd) {
            this.csd.setCsd(index, csd);
            return this;
        }

        @Nullable
        public Track build() {
            if(id == null)
                return null;

            Track res;
            if(type == VIDEO){
                VideoTrack video = new VideoTrack(id, payloadType);
                res = video;
                video.setWidth(width);
                video.setHeight(height);
                video.setFps(fps);
            } else if(type == AUDIO){
                AudioTrack audio = new AudioTrack(id, payloadType);
                res = audio;
                audio.setChannels(channels);
                audio.setMode(mode);
            } else if(type == SUBTITLE){
                res = new SubtitleTrack(id, payloadType);
            } else {
                return null;
            }
            res.setTitle(title);
            res.setDescription(description);
            res.setFormat(format);
            res.setDuration(duration);
            res.setLanguage(language);
            res.setTag(tag);
            res.metadata.putAll(metadata);
            res.setClockRate(clockRate);
            csd.writeTo(res.getCsd());

            return res;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getFormat() {
            return format;
        }

        public long getDuration() {
            return duration;
        }

        public String getLanguage() {
            return language;
        }

        public int getTag() {
            return tag;
        }

        public int getPayloadType() {
            return payloadType;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getChannels() {
            return channels;
        }

        public float getFps() {
            return fps;
        }

        public HashMap<String, Object> getMetadata() {
            return metadata;
        }

        public int getType() {
            return type;
        }

        public Csd getCsd() {
            return csd;
        }

        public String getMode() {
            return mode;
        }

        public int getClockRate() {
            return clockRate;
        }

    }

}
