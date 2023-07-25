package com.fivesoft.qplayer.bas2.decoder;

import android.view.Surface;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.bas2.common.Constants;
import com.fivesoft.qplayer.track.VideoTrack;

public abstract class VideoDecoder extends MediaDecoder<VideoTrack, Surface> {

    /**
     * Creates a new media decoder for the specified video track.
     * @param track The track that the decoder decodes. Cannot be null.
     */
    public VideoDecoder(@NonNull VideoTrack track, int sampleFormat, int maxEncodedFrameSize) {
        super(track, sampleFormat, maxEncodedFrameSize);
    }

    /**
     * Returns the width of the video.<br>
     * The value is available only after the first frame is decoded.
     * @return The width of the video in pixels or {@link Constants#UNKNOWN_VALUE} if the width is unknown.
     */

    public abstract int getVideoWidth();

    /**
     * Returns the height of the video.<br>
     * The value is available only after the first frame is decoded.
     * @return The height of the video in pixels or {@link Constants#UNKNOWN_VALUE} if the height is unknown.
     */

    public abstract int getVideoHeight();

    /**
     * Returns the frame rate of the video.<br>
     * The value is available only after the first frame is decoded.
     * @return The frame rate of the video in frames per second or {@link Constants#UNKNOWN_VALUE} if the frame rate is unknown.
     */

    public abstract float getVideoFrameRate();

}
