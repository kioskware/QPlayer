package com.fivesoft.qplayer.bas2;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.track.Track;

public class Frame extends Sample {

    public volatile boolean isSyncFrame;

    public Frame(@NonNull ByteArray data, long timestamp, @NonNull Track track, boolean isSyncFrame) {
        super(data, timestamp, track);
        this.isSyncFrame = isSyncFrame;
    }

}
