package com.fivesoft.qplayer.bas2.core;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.bas2.Frame;
import com.fivesoft.qplayer.bas2.decoder.MediaDecoder;

import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class MediaDecoderThread extends Thread {

    private final BlockingQueue<Frame> frameQueue = new ArrayBlockingQueue<>(10);

    private final MediaDecoder<?, ?> decoder;

    public MediaDecoderThread(@NonNull MediaDecoder<?, ?> decoder) {
        this.decoder = Objects.requireNonNull(decoder);
    }

    @Override
    public void run() {

        while (!isInterrupted()) {
            try {
                decoder.decode(frameQueue.take());
            } catch (InterruptedException e) {
                onThreadInterrupted(decoder);
                break;
            } catch (Exception e) {
                onThreadException(decoder, e);
                return;
            }
        }

    }

    public boolean feed(@NonNull Frame frame) {
        return frameQueue.offer(frame);
    }

    public abstract void onThreadInterrupted(@NonNull MediaDecoder<?, ?> decoder);

    public abstract void onThreadException(@NonNull MediaDecoder<?, ?> decoder, @NonNull Exception e);

}
