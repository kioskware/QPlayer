package com.fivesoft.qplayer.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An interface for handling track changes.
 */

public interface TrackChangeListener {

    /**
     * Called when a field of track is changed.<br>
     * @param track The track.
     * @param field The field that is changed.
     * @param value The new value of the field.
     */

    default void onTrackFieldChanged(@NonNull Track track, @NonNull String field, @Nullable Object value) {}

}
