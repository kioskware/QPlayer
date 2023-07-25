package com.fivesoft.qplayer.bas2.decoder;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.track.SubtitleTrack;

public abstract class SubtitleDecoder extends MediaDecoder<SubtitleTrack, SubtitleDecoder.SubtitleListener> {

    /**
     * Creates a new media decoder for the specified subtitle track.
     * @param track The track that the decoder decodes. Cannot be null.
     */

    public SubtitleDecoder(@NonNull SubtitleTrack track, int sampleFormat, int maxEncodedFrameSize) {
        super(track, sampleFormat, maxEncodedFrameSize);
    }

    /**
     * An interface for handling subtitle changes.
     */
    public interface SubtitleListener {

        /**
         * Called when the subtitle is changed.
         * @param subtitle The new subtitle.
         */

        void onSubtitleChanged(String subtitle);
    }

}
