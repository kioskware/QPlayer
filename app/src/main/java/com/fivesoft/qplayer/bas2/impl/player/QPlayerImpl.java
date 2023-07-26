package com.fivesoft.qplayer.bas2.impl.player;

import android.view.Surface;

import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.Authentication;
import com.fivesoft.qplayer.bas2.DataSource;
import com.fivesoft.qplayer.bas2.MediaExtractor;
import com.fivesoft.qplayer.bas2.QPlayer;
import com.fivesoft.qplayer.bas2.Sample;
import com.fivesoft.qplayer.bas2.TimeoutException;
import com.fivesoft.qplayer.bas2.TrackSelector;
import com.fivesoft.qplayer.bas2.common.Constants;
import com.fivesoft.qplayer.bas2.common.Util;
import com.fivesoft.qplayer.bas2.core.FrameBuilder;
import com.fivesoft.qplayer.bas2.core.resolvers.DataSourceResolver;
import com.fivesoft.qplayer.bas2.core.resolvers.MediaExtractorResolver;
import com.fivesoft.qplayer.bas2.decoder.MediaDecoder;
import com.fivesoft.qplayer.bas2.decoder.MediaDecoderOutput;
import com.fivesoft.qplayer.bas2.decoder.SubtitleReceiver;
import com.fivesoft.qplayer.track.AudioTrack;
import com.fivesoft.qplayer.track.Track;
import com.fivesoft.qplayer.track.Tracks;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/*
 * Android QPlayer core implementation.
 * This is for internal use only.
 * For public API, see QPlayerFactory.getDefault() method.
 */

class QPlayerImpl implements QPlayer<Surface, AudioTrack, SubtitleReceiver> {

    public static final int DEFAULT_BUFFER_LATENCY = 500;

    //Internal params
    private final Object mainThreadLock = new Object();
    private volatile MainThread mainThread;
    private volatile boolean started = false;
    private boolean isReleased = false;

    //Config
    private volatile long bufferLatency = DEFAULT_BUFFER_LATENCY;
    private volatile float volume = 0.5f;

    private MediaDecoderOutput.Creator<Surface> videoOutputCreator;
    private MediaDecoderOutput.Creator<AudioTrack> audioOutputCreator;
    private MediaDecoderOutput.Creator<SubtitleReceiver> subtitleOutputCreator;

    private TrackSelector trackSelector;
    private Authentication authentication;

    private volatile URI uri;

    //Components

    private volatile DataSource dataSource;
    private volatile MediaExtractor mediaExtractor;

    //Caches

    private volatile Tracks tracks;

    @Override
    public void setMediaSource(@Nullable URI uri, @Nullable TrackSelector selector) {
        if(!Objects.equals(this.uri, uri)) {
            this.uri = uri;
            onURIChanged(uri);
        }
        if(trackSelector != selector) {
            this.trackSelector = selector;
            onTrackSelectorChanged(selector);
        }
    }

    @Override
    public void setAuthentication(@Nullable Authentication auth) {
        this.authentication = auth;
    }

    @Override
    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public void start() {
        started = true;
        ensureMainThread();
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void setVolume(float volume) {
        this.volume = volume;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public int setBufferLatency(long latency) {
        bufferLatency = (int) latency;
        return 0;
    }

    @Override
    public void setTrackSelector(@Nullable TrackSelector selector) {
        this.trackSelector = selector;
    }

    @Override
    public void setVideoOutputCreator(@Nullable MediaDecoderOutput.Creator<Surface> creator) {
        this.videoOutputCreator = creator;
    }

    @Override
    public void setAudioOutputCreator(@Nullable MediaDecoderOutput.Creator<AudioTrack> creator) {
        this.audioOutputCreator = creator;
    }

    @Override
    public void setSubtitleOutputCreator(@Nullable MediaDecoderOutput.Creator<SubtitleReceiver> creator) {
        this.subtitleOutputCreator = creator;
    }

    @Nullable
    @Override
    public Tracks getTracks() {
        return tracks;
    }

    @Override
    public void release() {
        isReleased = true;
        started = false;
        synchronized (mainThreadLock) {
            if(mainThread != null) {
                mainThread.interrupt();
            }
            mainThread = null;
        }
    }

    @Override
    public boolean isReleased() {
        return isReleased;
    }

    //Internal methods

    private void onURIChanged(@Nullable URI uri) {
        if(uri == null) {
            dataSource = null;
            mediaExtractor = null;
        } else {
            DataSource dataSource = DataSourceResolver.resolveSource(uri);
            if(dataSource != null){
                MediaExtractor.Descriptor descriptor = new MediaExtractor.Descriptor(dataSource, trackSelector, uri);
                MediaExtractor extractor = MediaExtractorResolver.resolveExtractor(descriptor);
                if(extractor != null) {
                    this.dataSource = dataSource;
                    this.mediaExtractor = extractor;
                }
            }
        }
    }

    private void onTrackSelectorChanged(@Nullable TrackSelector selector) {
        //Restart data source and extractor to select another tracks
        this.trackSelector = selector;
        onURIChanged(uri);
    }

    private void ensureMainThread() {
        synchronized (mainThreadLock) {
            if(mainThread == null || !mainThread.isAlive() || mainThread.isInterrupted()) {
                mainThread = new MainThread();
                mainThread.start();
            }
        }
    }

    private class MainThread extends Thread {

        private DataSource cDataSource;
        private MediaExtractor cExtractor;

        private Tracks cTracks;

        private int sampleFormat;

        @Override
        public void run() {
            cDataSource = dataSource;
            cExtractor = mediaExtractor;

            boolean cmpChanged;
            Sample sample;
            DecodersManager decodersManager = new DecodersManager();
            while (!isInterrupted() && !isReleased) {
                //Check if components were changed
                cmpChanged = checkComponentsReferences();

                if(!checkDataSource()) {
                    //DataSource is not ready
                    Util.sleep(1000);
                    continue;
                }

                if(checkExtractor()) {
                    cTracks = cExtractor.getTracks();
                    tracks = cTracks;
                } else {
                    //Extractor is not ready
                    Util.sleep(1000);
                    continue;
                }

                if(cmpChanged) {
                    //Components were changed, we need to close old decoders
                    decodersManager.releaseAll();
                    decodersManager.addDecoderForTracks(cTracks, sampleFormat, FrameBuilder.DEFAULT_MAX_FRAME_SIZE);
                    continue;
                }

                //Components are ready, we can read sample
                try {
                    sample = cExtractor.nextSample();
                    if(sample != null) {
                        //Pass sample to decoders
                        decodersManager.feed(sample);
                    }
                } catch (IOException | TimeoutException e) {
                    //Try reconnecting
                } catch (InterruptedException e) {
                    break;
                }

            }
        }

        //Returns true if components were changed
        private boolean checkComponentsReferences() {
            boolean dataSourceClosed;
            if(cDataSource != dataSource) {
                dataSourceClosed = true;
                Util.closeQuietly(cDataSource);
                this.cDataSource = dataSource;
            } else {
                dataSourceClosed = false;
            }
            if(cExtractor != mediaExtractor || dataSourceClosed) {
                Util.closeQuietly(cExtractor);
                this.cExtractor = mediaExtractor;
                return true;
            }
            return false;
        }

        //Returns true if dataSource is ready to use
        private boolean checkDataSource() {
            if(cDataSource == null) {
                return false;
            } else if(dataSource.isConnected()){
                return true;
            } else {
                try {
                    cDataSource.connect();
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }

        //Returns true if extractor is ready to use
        private boolean checkExtractor() {
            if(cExtractor == null) {
                return false;
            } else if(cExtractor.isPrepared()) {
                return true;
            } else {
                try {
                    cExtractor.setAuthentication(authentication);
                    cExtractor.prepare(1000);
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }

    }

}