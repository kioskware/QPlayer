package com.fivesoft.qplayer.bas2;

import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.decoder.MediaDecoderOutput;
import com.fivesoft.qplayer.track.Track;
import com.fivesoft.qplayer.track.Tracks;

import java.net.URI;

public interface QPlayer<VideoRendererType, AudioRendererType, SubtitleRenderType> {

    /**
     * Sets media source to be played with specified track selector.<br>
     * @param uri Media source to be played or null to release current source.
     * @param selector The track selector to select tracks to play. If null, all tracks will be selected.
     */

    void setMediaSource(@Nullable URI uri, @Nullable TrackSelector selector);

    /**
     * Sets media source to be played with default track selector.<br>
     * @param uri Media source to be played or null to release current source.
     * @return 0 if the source was set successfully, otherwise error code.
     */

    default void setMediaSource(@Nullable URI uri){
        setMediaSource(uri, null);
    }

    /**
     * Sets authentication for receiving data from the data source.<br>
     * This is necessary for some stream types (e.g. RTSP with authentication).<br>
     * @param auth The authentication or null if no authentication is needed.
     */

    void setAuthentication(@Nullable Authentication auth);

    /**
     * Returns authentication for receiving data from the data source.<br>
     * This is necessary for some stream types (e.g. RTSP with authentication).<br>
     * @return The authentication or null if no authentication is needed.
     */

    Authentication getAuthentication();

    /**
     * If you want player to play, call this method.<br>
     * Player will play always when it's possible.<br>
     * When any error occurs, player will try to play again.<br>
     * To stop player, call {@link #stop()}.
     */

    void start();

    /**
     * If you want player to not play, call this method.<br>
     * Player will stop playback as soon as possible.<br>
     * If you want to play again, call {@link #start()}.
     */

    void stop();

    /**
     * Returns whether player is started or not.<br>
     * Started means that player is playing or trying to play.<br>
     * @return true if player is started, otherwise false.
     * @see #start()
     * @see #stop()
     */

    boolean isStarted();

    /**
     * Sets volume of all audio tracks.<br>
     * @param volume The volume to be set in range 0 - 1 (0 = mute, 1 = max volume)
     */

    void setVolume(float volume);

    /**
     * Returns volume of all audio tracks.<br>
     * @return The volume in range 0 - 1 (0 = mute, 1 = max volume)
     */

    float getVolume();

    /**
     * Sets the buffer latency in milliseconds.<br>
     * <b>Note that</b>, exact latency will be greater than the specified value.<br>
     * This is because exact latency is typically equal to:
     * <code>HDMI_latency + encoder_latency + transport_latency + <b>buffer_latency</b> + decoding_latency + rendering_latency</code>
     * @param latency The buffer latency in milliseconds.
     * @return 0 if the latency was set successfully, otherwise error code.
     */

    int setBufferLatency(long latency);

     /**
     * Sets {@link TrackSelector} to be used for selecting tracks.<br>
     * @param selector The track selector or null to use default track selector.
     */

    void setTrackSelector(@Nullable TrackSelector selector);

    /**
     * Sets {@link MediaDecoderOutput.Creator} for creating video output based on {@link Track} object.<br>
     * This must be set to render video.
     * @param creator The creator or null to not render video.
     */

    void setVideoOutputCreator(@Nullable MediaDecoderOutput.Creator<VideoRendererType> creator);

    /**
     * Sets {@link MediaDecoderOutput.Creator} for creating audio output based on {@link Track} object.<br>
     * This must be set to render audio.
     * @param creator The creator or null to not render audio.
     */

    void setAudioOutputCreator(@Nullable MediaDecoderOutput.Creator<AudioRendererType> creator);

    /**
     * Sets {@link MediaDecoderOutput.Creator} for creating subtitle output based on {@link Track} object.<br>
     * This must be set to render subtitles.
     * @param creator The creator or null to not render subtitles.
     */

    void setSubtitleOutputCreator(@Nullable MediaDecoderOutput.Creator<SubtitleRenderType> creator);

    /**
     * Gets current tracks including both playing and not playing ones.<br>
     * @return Current tracks or null if no tracks are available.
     */

    @Nullable
    Tracks getTracks();

    /**
     * If you won't use player anymore, call this method.<br>
     * This will release all resources used by the player.<br>
     * After calling this method, you can't use this player anymore.<br>
     */

    void release();

    /**
     * Returns whether player is released or not.<br>
     * @return true if player is released, otherwise false.
     */

    boolean isReleased();

}
