package com.fivesoft.qplayer.impl.mediasource;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.source.QMediaSource;

import java.net.Socket;

public class TcpMediaSource extends QMediaSource {

    public TcpMediaSource() {

    }

    @Override
    public int open() {
        return 0;
    }

    @Override
    public int close() {
        return 0;
    }

    @Override
    public int start() {
        return 0;
    }

    @Override
    public int stop() {
        return 0;
    }

    @Override
    public int restart() {
        return 0;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public long getPosition() {
        return 0;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    protected int onMessageSend(@NonNull byte[] data, int offset, int length) {
        return 0;
    }

    @Override
    public boolean isSendMessageSupported() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public int reopen() {
        return 0;
    }

    @Override
    public void setReadTimeout(int timeout) {

    }

    @Override
    public void setConnectTimeout(int timeout) {

    }
}
