package com.fivesoft.qplayer.bas2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.common.ThrowingRunnable;

import java.io.IOException;
import java.util.Objects;

/**
 * Thread that reads samples from {@link MediaExtractor} and handles them.<br>
 * This is simple class that does not handle any errors, just passes them forward.<br>
 */

public abstract class SampleReadThread extends Thread {

    private volatile boolean run = true;
    @NonNull
    private final DataSource source;
    @NonNull
    private final MediaExtractor extractor;

    /**
     * Creates new SampleReadThread.
     * @param source Data source to read samples from.
     * @param extractor Media extractor to read samples with.
     *                  Must be associated with source passed with source parameter,
     *                  {@link IllegalArgumentException} will be thrown otherwise.
     */

    public SampleReadThread(@NonNull DataSource source, @NonNull MediaExtractor extractor) {
        super();
        this.source = Objects.requireNonNull(source);
        this.extractor = Objects.requireNonNull(extractor);
        //Check if extractor is associated with source object
        if(extractor.getDataSource() != source) {
            throw new IllegalArgumentException("Extractor is not associated with source");
        }
    }

    @Override
    public void run() {
        //Connect to source if not connected
        if(!source.isConnected()){
            Throwable res = ThrowingRunnable.run(source::connect);
            if(res != null) {
                //We failed to connect to source. End thread.
                onDataSourceConnectError(source, res);
                return;
            }
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
                    onEndOfData();
                    return;
                }

                //Handle sample
                if(onSampleRead(sample)){
                    //Thread stop was requested. End thread.
                    return;
                }
            } catch (IOException e) {
                if(onIOException(source, extractor, e)) {
                    //Thread stop was requested. End thread.
                    return;
                }
            } catch (TimeoutException e) {
                if(onSampleReadTimeout()) {
                    //Thread stop was requested. End thread.
                    return;
                }
            } catch (InterruptedException e) {
                //Thread stop was requested. End thread.
                return;
            } catch (Throwable e) {
                if(onOtherError(extractor, e)) {
                    //Thread stop was requested. End thread.
                    return;
                }
            }
        }

    }

    public abstract void onDataSourceConnectError(@NonNull DataSource source, @NonNull Throwable e);

    public abstract boolean onIOException(@NonNull DataSource source, @NonNull MediaExtractor extractor, @NonNull Exception e);

    public abstract boolean onOtherError(@NonNull MediaExtractor extractor, @NonNull Throwable e);

    public abstract boolean onSampleRead(@NonNull Sample sample);

    public abstract boolean onSampleReadTimeout();

    public abstract void onEndOfData();

    public abstract void onReadThreadEnd();

}
