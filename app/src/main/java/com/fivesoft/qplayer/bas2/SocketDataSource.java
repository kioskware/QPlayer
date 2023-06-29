package com.fivesoft.qplayer.bas2;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Objects;

public class SocketDataSource extends DataSource {

    private final Object sl = new Object();
    private volatile Socket socket;

    private final SocketAddress address;

    public SocketDataSource(@NonNull SocketAddress address) {
        this.address = Objects.requireNonNull(address, "Socket address is null");
    }

    public SocketDataSource(@NonNull String host, int port) {
        this(new InetSocketAddress(host, port));
    }

    public SocketDataSource(@NonNull Uri uri) {
        this(uri.getHost(), uri.getPort());
    }

    public SocketDataSource(@NonNull String uri) {
        this(Uri.parse(uri));
    }

    @Override
    public long getLength() {
        return UNKNOWN_LENGTH;
    }

    @Override
    public void connect(int timeout) throws IOException {
        synchronized (sl) {
            if (isConnected()) {
                throw new IllegalStateException("Already connected");
            } else {
                //Create a new socket
                socket = new Socket();
                socket.setSoTimeout(getTimeout());
                //Connect to the server
                socket.connect(address, timeout);
            }
        }
    }

    @Override
    public boolean isConnected() {
        Socket socket = this.socket;
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @NonNull
    @Override
    public InputStream getInputStream() throws IOException {
        Socket socket = this.socket;
        if (socket != null) {
            return socket.getInputStream();
        } else {
            throw new IllegalStateException("Not connected");
        }
    }

    @Override
    public boolean isOutSupported() {
        return true; //Socket supports output stream
    }

    @Nullable
    @Override
    public OutputStream getOutputStream() throws IllegalStateException, IOException {
        Socket socket = this.socket;
        if (socket != null) {
            return socket.getOutputStream();
        } else {
            throw new IllegalStateException("Not connected");
        }
    }

    @Override
    public void close() throws IOException {
        Socket socket = this.socket;
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    protected void onTimeoutSet(int timeout) throws IOException {
        Socket socket = this.socket;
        if (socket != null) {
            socket.setSoTimeout(timeout);
        }
    }
}
