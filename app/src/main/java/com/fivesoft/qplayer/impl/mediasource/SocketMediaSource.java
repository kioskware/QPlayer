package com.fivesoft.qplayer.impl.mediasource;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.common.Flow;
import com.fivesoft.qplayer.common.ParamExecutor;
import com.fivesoft.qplayer.common.Task;
import com.fivesoft.qplayer.source.QMediaSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;

public abstract class SocketMediaSource extends QMediaSource {

    public static final long DEFAULT_TIMEOUT = 10000;
    public static final int ERROR_CLOSING_SOCKET = -999992;

    public static final int ERROR_INVALID_ADDRESS = -999991;

    private final SocketAddress address;
    protected final Uri uri;
    private volatile long readTimeout, connectTimeout;

    private int bufferSize = 4096;

    private volatile Socket socket;
    private volatile boolean isConnecting = false;
    private volatile boolean isStarted = false;

    private final Object socketLock = new Object();
    private final Object readThreadLock = new Object();

    private volatile SocketReadThread readThread;

    public SocketMediaSource(@NonNull SocketAddress address, long timeout) {
        this.address = Objects.requireNonNull(address);
        this.readTimeout = timeout;
        this.connectTimeout = timeout;
        this.uri = null;
    }

    public SocketMediaSource(@NonNull String uri, long timeout) {
        this.uri = Uri.parse(uri);
        this.address = new InetSocketAddress(this.uri.getHost(), this.uri.getPort());
        this.readTimeout = timeout;
        this.connectTimeout = timeout;
    }

    public SocketMediaSource(@NonNull SocketAddress address) {
        this(address, DEFAULT_TIMEOUT);
    }

    /**
     * Called when data is received from socket.<br>
     * @param data received data
     * @param offset offset where data starts
     * @param length length of data
     */

    public abstract void onDataReceived(byte[] data, int offset, int length);

    /**
     * Called when nothing is received from socket. (timeout is reached)<br>
     * This can be called multiple times, once per timeout milliseconds, but only when socket is opened.<br>
     */

    public abstract void onNothingReceived();

    /**
     * Called when error occurred while reading data from socket.<br>
     * @param e exception
     */

    public abstract void onReadError(Exception e);

    /**
     * Called when error occurred while connecting to socket.<br>
     * @param e exception
     */

    public abstract void onConnectError(Exception e);


    /**
     * Called when socket is connected.<br>
     * @param socket connected socket
     */

    public abstract void onSocketConnected(@NonNull Socket socket) throws IOException;

    /**
     * Called when socket is closed.<br>
     */

    public abstract void onSocketClosed();

    /**
     * Connects socket to the address specified in constructor.<br>
     * If socket is already connected, then does nothing and returns {@link #ERROR_ALREADY_OPENED}.<br>
     * @return 0 if success, error code otherwise
     */

    @Override
    public int open() {
        return createSocket(false, true);
    }

    /**
     * Stops listening for incoming data and closes socket.<br>
     * @return 0 if success, error code otherwise
     */

    @Override
    public int close() {
        Integer res = performOnSocket(socket -> {
            try {
                socket.close();
            } catch (IOException e) {
                return ERROR_CLOSING_SOCKET;
            }
            return SUCCESS;
        });
        //If null, then socket is already closed (even not initialized)
        return res == null ? SUCCESS : res;
    }

    /**
     * Opens socket if it is not opened yet, and starts listening for incoming data.<br>
     * If socket is already opened, then just starts listening if not started yet.
     * @return 0 if success, error code otherwise
     */

    @Override
    public int start() {
        isStarted = true;
        synchronized (readThreadLock) {
            if (readThread == null) {
                synchronized (socketLock) {
                    if (socket == null){
                        int res = open();
                        if (res != SUCCESS) {
                            return res;
                        }
                    }
                    readThread = new SocketReadThread(socket);
                    readThread.start();
                }
            }
        }
        return 0;
    }

    /**
     * Stops listening for incoming data. This does not close socket,
     * so you can call start() again to resume listening for incoming data.
     * @return 0 if success, error code otherwise
     */

    @Override
    public int stop() {
        isStarted = false;
        synchronized (readThreadLock) {
            if (readThread != null) {
                readThread.interrupt();
                readThread = null;
            }
        }
        return SUCCESS;
    }

    /**
     * Closes socket and opens it again.
     * @return 0 if success, error code otherwise
     */

    @Override
    public int restart() {
        stop();
        close();
        int res = open();
        if (res != SUCCESS)
            return res;
        return start();
    }

    /**
     * Checks if socket is connected and not closed.
     * @return true if socket is connected and not closed, false otherwise
     */

    @Override
    public boolean isRunning() {
        Socket socket = this.socket;
        return socket.isConnected() && !socket.isClosed();
    }

    /**
     * Not supported by this implementation.
     * Subclasses should override this method if they support seeking.
     * @return {@link Task#UNKNOWN_VALUE}
     */

    @Override
    public long getPosition() {
        return Task.UNKNOWN_VALUE;
    }

    /**
     * Not supported by this implementation. Subclasses should override this method if they support seeking.
     * @return {@link Task#UNKNOWN_VALUE}
     */

    @Override
    public long getLength() {
        return Task.UNKNOWN_VALUE;
    }

    /**
     * Sends message to socket. (Writes data to socket's output stream)
     * @param data data to send
     * @param offset offset where data starts
     * @param length length of data
     * @return 0 if success, error code otherwise
     */

    @Override
    protected int onMessageSend(@NonNull byte[] data, int offset, int length) {

        if (!isSendMessageSupported()) return QMediaSource.ERROR_SEND_MESSAGE_UNSUPPORTED;

        Integer res = performOnSocket(socket -> {
            try {
                socket.getOutputStream().write(data, offset, length);
            } catch (IOException e) {
                return UNKNOWN_VALUE;
            }
            return SUCCESS;
        });

        return res == null ? Flow.ERROR_FLOW_CLOSED : res;
    }

    /**
     * Checks if socket supports sending messages.<br>
     * @return true if socket supports sending messages, false otherwise
     */

    @Override
    public boolean isSendMessageSupported() {
        Boolean res = performOnSocket(socket -> {
            if (socket.isOutputShutdown())
                return false;
            try {
                socket.getOutputStream().write(new byte[0]);
            } catch (Exception e) {
                return false;
            }
            return true;
        });
        return res != null && res;
    }

    /**
     * Checks if socket is connected. If socket is connecting, then returns false.<br>
     * Connected means that incoming data will be received.
     * @return true if socket is connected, false otherwise
     */

    @Override
    public boolean isConnected() {
        return isRunning();
    }

    /**
     * Checks if socket is connecting now. If socket is connected, then returns false.
     * @return true if socket is connecting, false otherwise
     */

    @Override
    public boolean isConnecting() {
        return isConnecting;
    }

    /**
     * In this implementation, this method is equivalent to {@link #restart()}.
     * @return 0 if success, error code otherwise
     */

    @Override
    public int reopen() {
        return restart();
    }

    /**
     * Sets read timeout for socket.
     * @param timeout The timeout in milliseconds.
     */

    @Override
    public void setReadTimeout(int timeout) {
        readTimeout = timeout;
        performOnSocket(socket -> {
            try {
                socket.setSoTimeout(timeout);
            } catch (SocketException e) {
                return false;
            }
            return true;
        });
    }

    /**
     * Sets connect timeout for socket.
     * @param timeout The timeout in milliseconds.
     */

    @Override
    public void setConnectTimeout(int timeout) {
        connectTimeout = timeout;
    }

    /**
     * Performs an action on socket if it is not null.
     *
     * @param executor action to perform
     * @param <T>      type of result
     * @return result of action or null if socket is null
     */

    private <T> T performOnSocket(ParamExecutor<T, Socket> executor) {
        Socket socket = this.socket;
        if (socket == null) {
            return null;
        }
        return executor.execute(socket);
    }

    private int createSocket(boolean allowOverride, boolean connect) {
        synchronized (socketLock) {

            if ((isSocketOk() && !allowOverride) || isConnecting)
                return Flow.ERROR_ALREADY_OPENED;

            isConnecting = true;
            Socket socket = null;
            boolean error = true;
            try {

                socket = new Socket(){
                    @Override
                    public synchronized void close() throws IOException {
                        boolean closed = isClosed();
                        super.close();
                        //Guarantee that onSocketClosed() will be called only once for each socket
                        if (!closed) {
                            onSocketClosed();
                        }
                    }
                };

                socket.setSoTimeout((int) readTimeout);

                if (connect) {
                    socket.connect(address, (int) connectTimeout);
                }

                this.socket = socket;
                error = false;

                onSocketConnected(socket);
                return SUCCESS;
            } catch (SocketTimeoutException e) {
                onConnectError(e);
                return ERROR_TIMEOUT;
            } catch (IOException e) {
                e.printStackTrace();
                onConnectError(e);
                return ERROR_IO_EXCEPTION;
            } catch (IllegalArgumentException e) {
                onConnectError(e);
                return ERROR_INVALID_ADDRESS;
            } finally {
                isConnecting = false;
                if (error) {
                    if(socket != null){
                        //Close socket if error occurred, but socket was created
                        try {
                            socket.close();
                        } catch (IOException ignored) {}
                    }
                    this.socket = null;
                }
            }
        }
    }

    private boolean isSocketOk() {
        Socket socket = this.socket;
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private class SocketReadThread extends Thread {

        private final Socket socket;

        public SocketReadThread(@NonNull Socket socket) {
            this.socket = Objects.requireNonNull(socket);
        }

        @Override
        public void run() {
            byte[] buffer = new byte[bufferSize];
            InputStream in;
            try {
                in = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                markAsStopped();
                onReadError(e);
                Log.println(Log.ASSERT, "SocketMediaSource", "Failed to get input stream from socket");
                return;
            }
            while (!isInterrupted() && socket.isConnected() && !socket.isClosed()) {
                try {
                    int read = in.read(buffer, 0, bufferSize);

                    if(read == -1) {
                        //End of stream
                        Log.println(Log.ASSERT, "SocketMediaSource", "End of stream");
                        notifyEndOfData();
                        break;
                    }

                    if(read > 0) {
                        onDataReceived(buffer, 0, read);
                    }

                } catch (IOException e) {
                    //In most cases because of timeout
                    onNothingReceived();
                }
            }

            markAsStopped();
        }

        private void markAsStopped() {
            synchronized (socketLock) {
                if (socket == SocketMediaSource.this.socket) {
                    isStarted = false;
                }
            }
        }

    }

}
