package com.fivesoft.qplayer.bas2.impl.player;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.bas2.Sample;
import com.fivesoft.qplayer.track.Track;
import com.fivesoft.qplayer.track.Tracks;

public class DecodersManager {



    public void addDecoderForTracks(@NonNull Tracks tracks, int sampleFormat, int maxFrameSize){
        for (Track track : tracks) {
            addDecoderForTrack(track, sampleFormat, maxFrameSize);
        }
    }

    public void addDecoderForTrack(@NonNull Track track, int sampleFormat, int maxFrameSize){
        //TODO
    }

    public void releaseAll(){
        //TODO
    }

    public boolean feed(@NonNull Sample sample){
        //TODO
        return false;
    }

}
