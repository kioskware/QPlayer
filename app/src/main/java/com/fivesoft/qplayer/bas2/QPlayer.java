package com.fivesoft.qplayer.bas2;

import androidx.annotation.Nullable;

import com.fivesoft.qplayer.common.Credentials;

public interface QPlayer {

    /**
     * Sets media source to be played or null to release current source.<br>
     * If source is not null, it will be connected automatically and old source will be released.
     * @param source Media source to be played or null to release current source.
     * @return 0 if the source was set successfully, otherwise error code.
     */

    int setMediaSource(@Nullable DataSource source);

    int setMediaSource(@Nullable String uri);

    int setAuthentication(@Nullable Authentication auth);

    int play();

    int seekTo(long position);

    int pause();

    int resume();

    int stop();

    int setVolume(float volume);

    int setPlaybackSpeed(float speed);

    int setBufferLatency(long latency);

    void release();

}
