package com.fivesoft.qplayer.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AudioTrack extends Track {

    private int channels = Track.UNKNOWN;
    private String mode;

    public AudioTrack(@NonNull String id, int payloadType) {
        super(id, payloadType);
    }

    /**
     * Creates an audio track with the specified parameters.<br>
     * @param id The id of the audio track.
     * @param payloadType The payload type of the audio track.
     * @param channels The number of channels of the audio track. Negative values will be treated as unknown.
     * @param mode The mode of the audio track.
     */

    public AudioTrack(@NonNull String id, int payloadType, int channels, String mode) {
        super(id, payloadType);
        setChannels(channels);
        setMode(mode);
    }

    /**
     * Returns the number of channels of the audio track or {@link Track#UNKNOWN} if the number of channels is unknown.<br>
     * @return The number of channels of the audio track or {@link Track#UNKNOWN} if the number of channels is unknown.
     */

    public int getChannels() {
        return channels;
    }

    /**
     * Returns the mode of the audio track or null if the mode is unknown.<br>
     * @return The mode of the audio track or null if the mode is unknown.
     */

    @Nullable
    public String getMode() {
        return mode;
    }

    /**
     * Sets the number of channels of the audio track.<br>
     * Marks channels as unknown if the number of channels is less than 0.<br>
     * @param channels The number of channels of the audio track.
     */

    public void setChannels(int channels) {
        this.channels = Math.max(Track.UNKNOWN, channels);
    }

    /**
     * Sets the mode of the audio track.<br>
     * @param mode The mode of the audio track.
     */

    public void setMode(@Nullable String mode) {
        this.mode = mode;
    }

}
