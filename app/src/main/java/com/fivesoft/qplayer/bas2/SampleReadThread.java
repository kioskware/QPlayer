package com.fivesoft.qplayer.bas2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.common.ThrowingRunnable;

import java.io.IOException;

public abstract class SampleReadThread extends Thread {

    private volatile boolean run = true;

    public SampleReadThread() {
        super();
    }

    public SampleReadThread(@NonNull String name) {
        super(name);
    }

    public SampleReadThread(@Nullable ThreadGroup group, @NonNull String name) {
        super(group, name);
    }

    @Override
    public void run() {
        //Source doesn't change during playback, so we can cache it.
        //If source changes, new thread is required.
        DataSource source = getCurrentSource();

        if(source == null) {
            //We have no source to read from. End thread.
            return;
        } else if(!source.isConnected()){
            Throwable res = ThrowingRunnable.run(source::connect);
            if(res != null) {
                //We failed to connect to source. End thread.
                //TODO: Handle exception
                return;
            }
        }

        MediaExtractor extractor = getCurrentExtractor(source);

        if(extractor == null) {
            return;
        }

        Sample sample;

        /*
        Run till not interrupted.
         */

        while (run && !isInterrupted()) {
            try {
                sample = extractor.nextSample();

                if(sample == null) {
                    //We have no more samples to read. End thread.
                    return;
                }

                //Handle sample
                if(receiveSample(sample)){
                    //Thread stop was requested. End thread.
                    return;
                }
            } catch (IOException e) {
                if(handleException(extractor, e)) {
                    return;
                }
            } catch (TimeoutException e) {
                //Ignore
            } catch (InterruptedException e) {
                return;
            }
        }

    }

    @Override
    public void interrupt() {
        super.interrupt();
    }

    private DataSource getCurrentSource() {
        return null;
    }

    private MediaExtractor getCurrentExtractor(@NonNull DataSource source) {
        return null;
    }

    public abstract boolean receiveSample(@NonNull Sample sample);

    private boolean handleException(@NonNull Object src, @NonNull Exception e) {
        return false;
    }

}
