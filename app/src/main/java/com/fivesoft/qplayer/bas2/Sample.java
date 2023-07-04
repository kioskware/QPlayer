package com.fivesoft.qplayer.bas2;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.buffer.Bufferable;
import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.track.Track;

public class Sample implements Bufferable {

    @NonNull
    public volatile ByteArray data;
    public volatile long timestamp;
    @NonNull
    public volatile Track track;

    public Sample(@NonNull ByteArray data, long timestamp, @NonNull Track track) {
        this.data = data;
        this.timestamp = timestamp;
        this.track = track;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
