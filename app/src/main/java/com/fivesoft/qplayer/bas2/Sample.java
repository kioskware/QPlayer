package com.fivesoft.qplayer.bas2;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.buffer.Bufferable;
import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.track.Track;

public class Sample extends ByteArray implements Bufferable {

    public long timestamp;
    @NonNull
    public Track track;

    public Sample(@NonNull byte[] data, int off, int len, long timestamp, @NonNull Track track) {
        super(data, off, len);
        this.timestamp = timestamp;
        this.track = track;
    }

    public Sample(@NonNull byte[] data, long timestamp, @NonNull Track track) {
        super(data);
        this.timestamp = timestamp;
        this.track = track;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

}
