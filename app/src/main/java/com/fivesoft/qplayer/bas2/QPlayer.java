package com.fivesoft.qplayer.bas2;

import androidx.annotation.Nullable;

public interface QPlayer {

    int setSource(@Nullable String uri);

    int start();

    int seekTo(long position);

    int stop();

    int setVolume(float volume);

    int setPlaybackSpeed(float speed);

    int setBufferLatency(long latency);

    void release();

}
