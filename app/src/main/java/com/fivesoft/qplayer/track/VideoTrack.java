package com.fivesoft.qplayer.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VideoTrack extends Track {

    public int width, height;
    public float fps;

    public VideoTrack(@Nullable String title, @Nullable String format, long duration, int width, int height, float fps) {
        super(title, format, duration);
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public VideoTrack(@NonNull String id, int width, int height, float fps) {
        super();
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public VideoTrack() {

    }

    @NonNull
    @Override
    public String toString() {
        return "\n*****Track*****\n" +
                "id: " + id + "\n" +
                "title: " + title + "\n" +
                "description: " + description + "\n" +
                "format: " + format + "\n" +
                "duration: " + duration + "\n" +
                "language: " + language + "\n" +
                "tag: " + tag + "\n" +
                "payloadType: " + payloadType + "\n" +
                "width: " + width + "\n" +
                "height: " + height + "\n" +
                "fps: " + fps + "\n" +
                "**********************";
    }

}
