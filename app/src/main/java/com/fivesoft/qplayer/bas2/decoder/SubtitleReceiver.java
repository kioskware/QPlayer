package com.fivesoft.qplayer.bas2.decoder;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.track.SubtitleTrack;

public interface SubtitleReceiver {

    void onSubtitleReceived(@NonNull SubtitleTrack track, @NonNull String text, long duration);

}
