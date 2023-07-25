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

    /**
     * Sets media source to be played or null to release current source.<br>
     * This must be valid URI. (e.g. http://example.com/video.mp4)<br>
     * @param uri Media source to be played or null to release current source.
     * @return 0 if the source was set successfully, otherwise error code.
     */

    int setMediaSource(@Nullable String uri);

    /**
     * Sets authentication for receiving data from the data source.<br>
     * This is necessary for some stream types (e.g. RTSP with authentication).<br>
     * @param auth The authentication or null if no authentication is needed.
     */

    void setAuthentication(@Nullable Authentication auth);

    /**
     * If you want player to play, call this method.<br>
     * Player will play always when it's possible.<br>
     * When any error occurs, player will try to play again.<br>
     * To stop player, call {@link #stop()}.
     * @return 0 if the player was started successfully, otherwise error code.
     */

    int start();

    /**
     * If you want player to not play, call this method.<br>
     * Player will stop playback as soon as possible.<br>
     * If you want to play again, call {@link #start()}.
     * @return 0 if the player was stopped successfully, otherwise error code.
     */

    int stop();

    /**
     * Sets volume of all audio tracks.<br>
     * @param volume The volume to be set in range 0 - 1 (0 = mute, 1 = max volume)
     * @return 0 if the volume was set successfully, otherwise error code.
     */

    int setVolume(float volume);

    /**
     * Sets the buffer latency in milliseconds.<br>
     * <b>Note that</b>, exact latency will be greater than the specified value.<br>
     * This is because exact latency is equal to:
     * <code>HDMI_latency + encoder_latency + transport_latency + <b>buffer_latency</b> + decoding_latency + rendering_latency</code>
     * @param latency The buffer latency in milliseconds.
     * @return 0 if the latency was set successfully, otherwise error code.
     */

    int setBufferLatency(long latency);

    /**
     * If you won't use player anymore, call this method.<br>
     * This will release all resources used by the player.<br>
     * After calling this method, you can't use this player anymore.<br>
     */

    void release();

}
