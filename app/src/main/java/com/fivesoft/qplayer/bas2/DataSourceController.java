package com.fivesoft.qplayer.bas2;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.common.ReadOnlyByteArray;
import com.fivesoft.qplayer.track.Track;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public abstract class DataSourceController implements AutoCloseable, Timeoutable {

    /**
     * Constant used as a return value of {@link #onMainLoopEnd(DataSource, int, Exception)} method.<br>
     * If returned, the main loop won't be restarted and the both controller and data source will be closed.
     */

    protected static final int LOOP_RESTART_BEHAVIOUR_NONE = 0;

    /**
     * Constant used as a return value of {@link #onMainLoopEnd(DataSource, int, Exception)} method.<br>
     * If returned, the main loop will be restarted, but media source connection won't be established again.<br>
     * This will only have effect if the controller is in continuous mode, otherwise {@link #LOOP_RESTART_BEHAVIOUR_NONE} will be used.
     *
     * @see #setContinuousMode(boolean)
     */

    protected static final int LOOP_RESTART_BEHAVIOUR_RESTART = 1;

    /**
     * Constant used as a return value of {@link #onMainLoopEnd(DataSource, int, Exception)} method.<br>
     * If returned, the main loop will be restarted and media source connection will be established again.<br>
     * This will only have effect if the controller is in continuous mode, otherwise {@link #LOOP_RESTART_BEHAVIOUR_NONE} will be used.
     *
     * @see #setContinuousMode(boolean)
     */

    protected static final int LOOP_RESTART_BEHAVIOUR_RECONNECT = 2;

    public static final int LOOP_END_REASON_NORMAL = 0;
    public static final int LOOP_END_REASON_ERROR = 1;
    public static final int LOOP_END_REASON_INTERRUPTED = 2;

    public static final int DEFAULT_CONTINUOUS_MODE_DELAY = 1000;

    /**
     * Data source associated with this controller.
     */

    protected final DataSource source;

    // Started flag is used to indicate that the controller has been started using start() method.
    private volatile boolean started;

    //Thread that runs the main loop
    private MainLoopThread mainLoopThread;

    private final Object mainLoopThreadLock = new Object();

    /*

    Continuous mode causes the controller to read data from the data source continuously, even
    if end of data has been reached or error occurred. In such case media source will be reopened and
    playback will continue from the beginning of the source.

    The mode has been designed for cases like digital signage or kiosk applications, where
    media should be played without user interaction.

     */

    private volatile boolean continuousMode = false;
    private volatile int continuousModeDelay = DEFAULT_CONTINUOUS_MODE_DELAY;

    private volatile long lastHeartBeat = SystemClock.elapsedRealtime();

    public DataSourceController(@NonNull DataSource source) {
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Connects to the data source with the specified timeout and make initial setup.
     *
     * @param timeout the timeout value to be used in milliseconds
     * @throws IOException if an I/O error occurs
     */
    public void connect(int timeout) throws IOException {
        source.connect(timeout);
        onSetup(source, source.getInputStream(), source.getOutputStream());
        startMainLoop();
    }

    /**
     * Connects to the data source with the default timeout and make initial setup.
     *
     * @throws IOException if an I/O error occurs
     */

    public final void connect() throws IOException {
        connect(DataSource.DEFAULT_CONNECT_TIMEOUT);
    }

    /**
     * Checks if the media source associated with this controller is connected.
     * @return true if the media source is connected, false otherwise
     */

    public boolean isConnected() {
        return source.isConnected();
    }

    /**
     * Called just after the data source is connected.<br>
     * The implementation should make here any necessary negotiations/setup with the data source to make it ready to use.
     *
     * @param source the data source associated with this controller
     * @param in     the input stream of the data source
     * @param out    the output stream of the data source, may be null if the data source does not support output
     * @throws IOException if an I/O error occurs
     */
    protected abstract void onSetup(@NonNull DataSource source, @NonNull InputStream in, @Nullable OutputStream out) throws IOException;

    /**
     * <p>
     * This is the heart of the controller - this is where the data source is being read and controlled.<br>
     * Implementation should provide a loop which will read data from the data source till it is closed.<br>
     * There should be also start/stop logic implemented here.
     * </p>
     * <p>
     * This method is run in a separate thread after successful connection to the data source.
     * </p>
     *
     * @param source the data source associated with this controller
     * @param in     the input stream of the data source
     * @param out    the output stream of the data source, may be null if the data source does not support output
     * @return result code indicating the reason why the main loop ended
     * @throws IOException if an I/O error occurs
     */

    protected abstract int onMainLoopStart(@NonNull DataSource source, @NonNull InputStream in, @Nullable OutputStream out) throws Exception;

    /**
     * Called when the main loop ends. This may be either a normal behaviour (i.e. end of the stream) or an error.
     * In case of an error, the error parameter will be not null.
     *
     * @param source the data source associated with this controller
     * @param error  the error that caused the main loop to end, or null if the main loop ended normally
     * @return result code indicating the behaviour after the main loop ends. One of:
     * <ul>
     *      <li>{@link #LOOP_RESTART_BEHAVIOUR_NONE}</li>
     *      <li>{@link #LOOP_RESTART_BEHAVIOUR_RESTART}</li>
     *      <li>{@link #LOOP_RESTART_BEHAVIOUR_RECONNECT}</li>
     * </ul>
     */

    protected abstract int onMainLoopEnd(@NonNull DataSource source, int reason, @Nullable Exception error);

    /**
     * Called when the data source is about to be closed.
     *
     * @throws IOException if an I/O error occurs
     */

    protected void onBeforeClose() throws IOException {}

    /**
     * Called just after the data source is closed.
     *
     * @throws IOException if an I/O error occurs
     */

    protected void onAfterClose() throws IOException {}

    protected void returnMediaPayload(@NonNull ReadOnlyByteArray payload, @NonNull Track track, long timestamp) {
        
    }

    protected ReadOnlyByteArray returnMediaPayload(@NonNull byte[] payload, int off, int len, @NonNull Track track, long timestamp) {
        ReadOnlyByteArray ret = new ReadOnlyByteArray(payload, off, len);
        returnMediaPayload(ret, track, timestamp);
        return ret;
    }

    /**
     * Marks controller as started.<br>
     * When controller is marked as started, it will always play when it is ready.
     * If controller was stopped, starting it will cause it to start playing as soon as possible.
     *
     * @throws IOException if an I/O error occurs
     */

    public final void start() {
        started = true;
    }

    /**
     * Checks if controller is marked as started.<br>
     * When controller is marked as started, it will always play when it is ready.
     *
     * @return true if started
     */

    public final boolean isStarted() {
        return started;
    }

    /**
     * Marks controller as stopped.<br>
     * When controller is marked as stopped, it will not play, even if it is ready.<br>
     * If controller is playing now, stopping it will cause it to stop playing as soon as possible.
     *
     * @throws IOException if an I/O error occurs
     */

    public final void stop() throws IOException {
        started = false;
    }

    /**
     * Gets current media position in milliseconds.
     *
     * @return the current media position in milliseconds
     * @throws UnsupportedOperationException if the data source does not support position tracking
     */

    public long getPosition() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets current media position in milliseconds.
     *
     * @param position the position in milliseconds
     * @throws UnsupportedOperationException if the data source does not support position tracking
     */

    public long setPosition(long position) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Makes heartbeat instantly.<br>
     * Heartbeats help to detect thread blocks, which is done by QPlayer API.
     */

    protected void heartbeat(){
        lastHeartBeat = SystemClock.elapsedRealtime();
    }

    /**
     * Gets time since last heartbeat in milliseconds.
     * @return time since last heartbeat in milliseconds
     */

    public long getTimeSinceHeartbeat(){
        return SystemClock.elapsedRealtime() - lastHeartBeat;
    }

    /**
     * Closes the data source and releases any system resources associated with it.
     *
     * @throws IOException if an I/O error occurs
     */

    public void close() throws IOException {
        onBeforeClose();
        source.close();
        stopMainLoop();
        onAfterClose();
    }

    /**
     * Reconnects the data source and the controller.
     */

    public void reconnect() throws IOException {
        close();
        connect();
    }

    //Continuous mode

    /**
     * Checks if continuous mode is enabled.
     * <p>
     * Continuous mode causes the controller to read data from the data source continuously, even
     * if end of data has been reached or error occurred. In such case media source will be reopened and
     * playback will continue from the beginning of the source.
     * <p>
     * The mode has been designed for cases like digital signage or kiosk applications, where
     * media should be played without user interaction.
     * </p>
     *
     * @return true if continuous mode is enabled
     */

    public final boolean isContinuousMode() {
        return continuousMode;
    }

    /**
     * Enables or disables continuous mode.
     *
     * <p>
     * Continuous mode causes the controller to read data from the data source continuously, even
     * if end of data has been reached or error occurred. In such case media source will be reopened and
     * playback will continue from the beginning of the source.
     * <p>
     * The mode has been designed for cases like digital signage or kiosk applications, where
     * media should be played without user interaction.
     * </p>
     *
     * @param enabled true to enable continuous mode, false to disable
     */

    public final void setContinuousMode(boolean enabled) {
        this.continuousMode = enabled;
    }

    /**
     * Sets the delay in milliseconds between the end of main loop and reopening the data source in continuous mode.
     * <p>
     * Continuous mode causes the controller to read data from the data source continuously, even
     * if end of data has been reached or error occurred. In such case media source will be reopened and
     * playback will continue from the beginning of the source.
     * This method sets the delay between the end of main loop and reopening the data source.
     * </p>
     *
     * @param delay the delay in milliseconds, default is 1000
     */

    public final void setContinuousModeDelay(int delay) {
        this.continuousModeDelay = delay;
    }

    /**
     * Gets the delay in milliseconds between the end of main loop and reopening the data source in continuous mode.
     * <p>
     * Continuous mode causes the controller to read data from the data source continuously, even
     * if end of data has been reached or error occurred. In such case media source will be reopened and
     * playback will continue from the beginning of the source.
     * This method gets the delay between the end of main loop and reopening the data source.
     * </p>
     *
     * @return the delay in milliseconds, default is 1000
     */

    public final int getContinuousModeDelay() {
        return continuousModeDelay;
    }

    //Timeoutable implementation

    @Override
    public int getTimeout() {
        return source.getTimeout();
    }

    @Override
    public void setTimeout(int timeout) {
        source.setTimeout(timeout);
    }

    private void startMainLoop() {
        synchronized (mainLoopThreadLock) {
            if (mainLoopThread != null) {
                mainLoopThread.interrupt();
            }
            mainLoopThread = new MainLoopThread();
            mainLoopThread.start();
        }
    }

    private void stopMainLoop() {
        synchronized (mainLoopThreadLock) {
            if (mainLoopThread != null) {
                mainLoopThread.interrupt();
                mainLoopThread = null;
            }
        }
    }

    private class MainLoopThread extends Thread {

        @Override
        public void run() {
            Exception error = null;
            int res = LOOP_END_REASON_NORMAL;
            heartbeat();

            try {
                res = onMainLoopStart(source, source.getInputStream(), source.getOutputStream());
            } catch (Exception e) {
                error = e;
            } finally {
                heartbeat();

                int b;
                if (error instanceof InterruptedException) {
                    b = onMainLoopEnd(source, LOOP_END_REASON_INTERRUPTED, error);
                } else if (error != null) {
                    b = onMainLoopEnd(source, LOOP_END_REASON_ERROR, error);
                } else {
                    b = onMainLoopEnd(source, res,
                            res == LOOP_END_REASON_ERROR ? new Exception("Unknown error") : null); //Make sure that when error is reported, exception is not null
                }

                if (isContinuousMode() && !isInterrupted()) {
                    //Check how to behave
                    if (b > 0) {
                        if (b == LOOP_RESTART_BEHAVIOUR_RECONNECT) {
                            reconnect();
                        }
                        if (!isInterrupted()) {
                            try {
                                heartbeat();
                                //Run again after set delay
                                Thread.sleep(Math.max(1, continuousModeDelay));
                                //noinspection CallToThreadRun
                                run(); //We can start thread again...
                            } catch (InterruptedException ex) {
                                //ignore
                            }
                        }
                    }
                }
            }

        }

        private void reconnect() {
            heartbeat();
            try {
                source.reconnect();
            } catch (IOException e) {
                //Handle exception
                if (!isInterrupted() && isContinuousMode()) {
                    try {
                        //Try again after set delay
                        Thread.sleep(Math.max(1, continuousModeDelay));
                        reconnect();
                    } catch (InterruptedException ex) {
                        //ignore
                    }
                }
            }
        }
    }

}
