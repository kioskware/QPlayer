package com.fivesoft.qplayer;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.base.FrameExtractor;
import com.fivesoft.qplayer.base.MediaPacket;
import com.fivesoft.qplayer.base.MediaExtractListener;
import com.fivesoft.qplayer.base.QMediaExtractor;
import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.common.Flow;
import com.fivesoft.qplayer.common.ParamExecutor;
import com.fivesoft.qplayer.common.Task;
import com.fivesoft.qplayer.frame.Frame;
import com.fivesoft.qplayer.frame.FrameExtractListener;
import com.fivesoft.qplayer.impl.resolvers.FrameExtractorResolver;
import com.fivesoft.qplayer.impl.resolvers.MediaExtractorResolver;
import com.fivesoft.qplayer.impl.resolvers.MediaSourceResolver;
import com.fivesoft.qplayer.resolver.Resolver;
import com.fivesoft.qplayer.source.MediaSourceListener;
import com.fivesoft.qplayer.source.QMediaSource;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class QPlayer implements Flow, Task {

    public static final String TAG = QPlayer.class.getSimpleName();

    public static final int ERROR_NO_MEDIA_SOURCE = -5720;

    public static final int MAX_EXTRACTOR_FAILS = 5;

    public static final int MAX_FRAME_EXTRACTOR_FAILS = 5;

    public static final int MAX_FRAME_DECODER_FAILS = 5;

    private final Object mediaSrcLock = new Object();
    private final Object extractorLock = new Object();
    private final String listenerId = "QPlayer_" + hashCode();

    private QMediaSource source;
    private QMediaExtractor extractor;
    private FrameExtractor frameExtractor;
    private AtomicInteger mediaExtractorFails = new AtomicInteger();
    private AtomicInteger frameExtractorFails = new AtomicInteger();
    private Surface outputSurface;

    //Listener for data returned from the media source. (ex. rtsp packet)
    private final MediaSourceListener mediaSourceListener = new MediaSourceListener() {

        @Override
        public void onConnecting() {

        }

        @Override
        public void onConnected() {

        }

        @Override
        public void onDataAvailable(@NonNull ByteArray packet) {
            //Pass data to media extractor
            QMediaExtractor temp = getMediaExtractorForPacket(packet);
            if(temp != null) {
                //Everything is ok, pass the packet to the extractor
                temp.onDataAvailable(packet);
            } else {
                //Format not supported
                onUnsupportedPacketReceived(packet);
            }
        }

        @Override
        public void onEndOfData() {

        }

        @Override
        public void onDisconnected() {

        }

        @Override
        public void onError(@NonNull Exception e) {

        }

    };

    //Listener for media packets returned from the extractor. (ex. annex B stream fragment)
    private final MediaExtractListener mediaExtractListener = new MediaExtractListener() {
        @Override
        public void onTrackDataAvailable(@NonNull MediaPacket packet) {
            mediaExtractorFails.set(0);
            //Pass media packet to frame extractor
            FrameExtractor temp = getFrameExtractorForPacket(packet);
            if(temp != null) {
                //Everything is ok, pass the packet to the extractor
                temp.onMediaPacketAvailable(packet);
            } else {
                //Format not supported
                onUnsupportedMediaPacketReceived(packet);
            }
        }

        @Override
        public void onError(@NonNull Exception e) {
            mediaExtractorFails.incrementAndGet();
        }
    };

    //Listener for encoded frames returned from the frame extractor. (ex. h264 frame)
    private final FrameExtractListener frameExtractListener = new FrameExtractListener() {

        @Override
        public void onFrameAvailable(Frame frame) {
            frameExtractorFails.set(0);
            //Pass the frame to the buffer

        }

        @Override
        public void onError(@NonNull Exception e) {
            frameExtractorFails.incrementAndGet();
        }
    };

    @NonNull
    protected final Resolver<String, QMediaSource> mediaSourceResolver;

    @NonNull
    protected final Resolver<String, FrameExtractor> frameExtractorResolver;

    @NonNull
    protected final Resolver<ByteArray, QMediaExtractor> mediaExtractorResolver;

    /**
     * Creates a new QPlayer instance using the specified resolvers.
     * @param mediaSourceResolver to be used for resolving media sources.
     * @param frameExtractorResolver to be used for resolving frame extractors.
     * @param mediaExtractorResolver to be used for resolving media extractors.
     */

    public QPlayer(@NonNull Resolver<String, QMediaSource> mediaSourceResolver,
                   @NonNull Resolver<ByteArray, QMediaExtractor> mediaExtractorResolver,
                   @NonNull Resolver<String, FrameExtractor> frameExtractorResolver
                   ) {

        this.mediaSourceResolver = Objects.requireNonNull(mediaSourceResolver, "Media source resolver cannot be null");
        this.frameExtractorResolver = Objects.requireNonNull(frameExtractorResolver, "Frame extractor resolver cannot be null");
        this.mediaExtractorResolver = Objects.requireNonNull(mediaExtractorResolver, "Media extractor resolver cannot be null");
    }

    /**
     * Creates a new QPlayer instance using the default resolvers.
     */

    public QPlayer() {
        this(MediaSourceResolver.getDefault(), MediaExtractorResolver.getDefault(), FrameExtractorResolver.getDefault());
    }

    /**
     * Sets the media source for this player.
     * @param source The media source to set.
     * @return This player.
     */

    public QPlayer setMediaSource(@Nullable QMediaSource source) {
        synchronized (mediaSrcLock) {
            QMediaSource oldSource = this.source;
            if(oldSource == source) {
                return this;
            }
            if(oldSource != null) {
                oldSource.removeListener(listenerId);
                oldSource.close();
            }
            this.source = source;
            if(source != null) {
                source.setListener(listenerId, mediaSourceListener);
            }
            return this;
        }
    }

    /**
     * Sets the media source for this player.<br>
     * The source will be resolved using the registered resolvers.
     * @param address The address of the media source.
     * @return This player.
     */

    public QPlayer setMediaSource(@Nullable String address) {
        if(address == null) {
            return setMediaSource((QMediaSource) null);
        }
        setMediaSource(mediaSourceResolver.resolve(address));
        return this;
    }

    /**
     * Sets the output surface for this player.<br>
     * On this surface the video will be rendered.
     * @param surface The output surface to render video on.
     * @return This player.
     */

    public QPlayer setOutputSurface(@Nullable Surface surface) {
        this.outputSurface = surface;
        return this;
    }

    /**
     * Returns the output surface currently set for this player.
     * @return The output surface currently set for this player.
     * May be null if no surface is set.
     */

    @Nullable
    public Surface getOutputSurface() {
        return outputSurface;
    }

    @Override
    public int open() {
        return openInternal();
    }

    @Override
    public int close() {
        return closeInternal();
    }

    @Override
    public int start() {
        return startInternal();
    }

    @Override
    public int stop() {
        return stopInternal();
    }

    @Override
    public int restart() {
        return 0;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    /**
     * Returns the position of the media source.
     * @return The position of the media source in media source specific units.
     */

    @Override
    public long getPosition() {
        QMediaSource source = this.source;
        if(source != null) {
            return source.getPosition();
        }
        return -1;
    }

    /**
     * Returns the length of the media source.
     * @return The length of the media source in media source specific units.
     */

    @Override
    public long getLength() {
        QMediaSource source = this.source;
        if(source != null) {
            return source.getLength();
        }
        return -1;
    }

    private void onUnsupportedPacketReceived(@NonNull ByteArray packet) {

    }

    private void onUnsupportedMediaPacketReceived(@NonNull MediaPacket packet) {

    }

    private void onUnsupportedFrameReceived(@NonNull Frame frame) {

    }

    private QMediaExtractor getMediaExtractorForPacket(@NonNull ByteArray packet) {
        QMediaExtractor temp;
        if(mediaExtractorFails.get() >= MAX_EXTRACTOR_FAILS || extractor == null) {
            //Current extractor is not working or not set, try to resolve a new one
            temp = mediaExtractorResolver.resolve(packet);
            if (temp != null) {
                temp.setListener(listenerId, mediaExtractListener);
            }
            extractor = temp;
            return temp;
        } else {
            //We are using currently set extractor
            return extractor;
        }
    }

    private FrameExtractor getFrameExtractorForPacket(MediaPacket packet){
        FrameExtractor temp;

        if(packet.track == null) {
            return null;
        }

        if(frameExtractorFails.get() >= MAX_FRAME_EXTRACTOR_FAILS || frameExtractor == null) {
            //Current extractor is not working or not set, try to resolve a new one
            String format = packet.track.format;
            if(format == null) {
                return null;
            }

            temp = frameExtractorResolver.resolve(format);
            if (temp != null) {
                temp.setListener(listenerId, frameExtractListener);
            }

            frameExtractor = temp;
            return temp;
        } else {
            //We are using currently set extractor
            return frameExtractor;
        }
    }

    private int openInternal(){
        return executeOnMediaSource(Flow::open);
    }

    private int startInternal(){
        return executeOnMediaSource(Task::start);
    }

    private int stopInternal(){
        return executeOnMediaSource(Task::stop);
    }

    private int closeInternal(){
        return executeOnMediaSource(Flow::close);
    }

    private int executeOnMediaSource(@NonNull ParamExecutor<Integer, QMediaSource> task){
        synchronized (mediaSrcLock){
            QMediaSource source = this.source;
            if(source != null){
                return task.execute(source);
            } else {
                return ERROR_NO_MEDIA_SOURCE;
            }
        }
    }

}
