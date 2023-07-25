package com.fivesoft.qplayer.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VideoTrack extends Track {

    private int width = UNKNOWN, height = UNKNOWN;
    private float fps = UNKNOWN;

    public VideoTrack(@NonNull String id, int payloadType, int width, int height, float fps) {
        super(id, payloadType);
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public VideoTrack(@NonNull String id, int payloadType) {
        super(id, payloadType);
    }

    /**
     * Returns the width of the video track (in pixels) or {@link Track#UNKNOWN} if the width is unknown.<br>
     * @return The width of the video track or {@link Track#UNKNOWN} if the width is unknown.
     */

    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of the video track (in pixels).<br> Negative values will be treated as unknown.<br>
     * @param width The width of the video track. (in pixels)
     */

    public void setWidth(int width) {
        this.width = Math.max(Track.UNKNOWN, width);
    }

    /**
     * Returns the height of the video track (in pixels) or {@link Track#UNKNOWN} if the height is unknown.<br>
     * @return The height of the video track or {@link Track#UNKNOWN} if the height is unknown.
     */

    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the video track (in pixels).<br> Negative values will be treated as unknown.<br>
     * @param height The height of the video track. (in pixels)
     */

    public void setHeight(int height) {
        this.height = Math.max(Track.UNKNOWN, height);
    }

    /**
     * Returns the frame rate of the video track (in frames per second) or {@link Track#UNKNOWN} if the frame rate is unknown.<br>
     * @return The frame rate of the video track or {@link Track#UNKNOWN} if the frame rate is unknown.
     */

    public float getFps() {
        return Math.max(Track.UNKNOWN, fps);
    }

    /**
     * Sets the frame rate of the video track (in frames per second).<br> Negative values will be treated as unknown.<br>
     * @param fps The frame rate of the video track. (in frames per second)
     */

    public void setFps(float fps) {
        this.fps = fps;
    }

}
