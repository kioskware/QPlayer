package com.fivesoft.qplayer.bas2.core;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.bas2.Frame;
import com.fivesoft.qplayer.bas2.decoder.MediaDecoder;

import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class MediaDecoderThread extends Thread {

    private final BlockingQueue<Frame> frameQueue = new ArrayBlockingQueue<>(100);

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

    public void feed(@NonNull Frame frame) throws InterruptedIOException {
        try {
            frameQueue.put(Objects.requireNonNull(frame));
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    public abstract void onThreadInterrupted(@NonNull MediaDecoder<?, ?> decoder);

    public abstract void onThreadException(@NonNull MediaDecoder<?, ?> decoder, @NonNull Exception e);

}
