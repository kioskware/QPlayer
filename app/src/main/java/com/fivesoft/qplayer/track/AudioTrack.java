package com.fivesoft.qplayer.track;

import androidx.annotation.NonNull;

public class AudioTrack extends Track {

    public int sampleRate, channels;

    public AudioTrack(String title, String format, long duration, int sampleRate, int channels) {
        super(title, format, duration);
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public AudioTrack() {}

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
                "sampleRate: " + sampleRate + "\n" +
                "channels: " + channels + "\n" +
                "**********************";
    }

}
