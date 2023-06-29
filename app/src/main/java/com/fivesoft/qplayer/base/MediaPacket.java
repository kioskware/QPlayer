package com.fivesoft.qplayer.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.common.ReadOnlyByteArray;
import com.fivesoft.qplayer.track.Track;

import java.util.Objects;

public class MediaPacket {

    @Nullable
    public final Track track;
    @NonNull
    public final ReadOnlyByteArray payload;
    public final long timestamp;

    public MediaPacket(@Nullable Track track, @NonNull ReadOnlyByteArray payload, long timestamp) {
        this.track = track;
        this.timestamp = timestamp;
        this.payload = Objects.requireNonNull(payload);
    }

}
