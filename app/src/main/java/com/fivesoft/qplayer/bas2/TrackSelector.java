package com.fivesoft.qplayer.bas2;

import com.fivesoft.qplayer.track.AudioTrack;
import com.fivesoft.qplayer.track.SubtitleTrack;
import com.fivesoft.qplayer.track.Track;
import com.fivesoft.qplayer.track.VideoTrack;

/**
 * Interface for deciding whether a track should be selected for playback.<br>
 * Track selectors are used i.e. by {@link MediaExtractor}s to select tracks to extract.
 */

public interface TrackSelector {

    /**
     * Returns true if the specified track should be selected for playback.
     * In some cases this method may be called very often, so it should be fast.
     * @param track The track to select.
     * @return True if the track should be selected, false otherwise.
     */

    boolean selectTrack(Track track);

    /**
     * A track selector that selects all tracks.
     */
    TrackSelector ALL = track -> true;

    /**
     * A track selector that selects only audio tracks.
     */

    TrackSelector AUDIO = track -> track instanceof AudioTrack;

    /**
     * A track selector that selects only video tracks.
     */

    TrackSelector VIDEO = track -> track instanceof VideoTrack;

    /**
     * A track selector that selects only subtitle tracks.
     */

    TrackSelector SUBTITLES = track -> track instanceof SubtitleTrack;

    /**
     * A track selector that selects only audio and video tracks.
     */

    TrackSelector AUDIO_AND_VIDEO = track -> track instanceof AudioTrack || track instanceof VideoTrack;

    /**
     * A track selector that selects no tracks.
     */

    TrackSelector NONE = track -> false;

}
