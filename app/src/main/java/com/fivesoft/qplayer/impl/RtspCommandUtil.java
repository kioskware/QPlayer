package com.fivesoft.qplayer.impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;

public class RtspCommandUtil {

    private static final boolean DEBUG = false;
    private static final String CRLF = "\r\n";
    private static final String TAG = RtspCommandUtil.class.getSimpleName();

    public static void sendSimpleCommand(@NonNull String command, @NonNull OutputStream outputStream,
                                          @NonNull String request, int cSeq, @Nullable String userAgent,
                                          @Nullable String session, @Nullable String authToken) throws IOException {
        outputStream.write((command + " " + request + " RTSP/1.0" + CRLF).getBytes());
        if (authToken != null) {
            outputStream.write(("Authorization: " + authToken + CRLF).getBytes());
        }
        outputStream.write(("CSeq: " + cSeq + CRLF).getBytes());
        if (userAgent != null) {
            outputStream.write(("User-Agent: " + userAgent + CRLF).getBytes());
        }
        if (session != null) {
            outputStream.write(("Session: " + session + CRLF).getBytes());
        }
        outputStream.write(CRLF.getBytes());
        outputStream.flush();
    }

    public static void sendOptionsCommand(@NonNull OutputStream outputStream, @NonNull String request, int cSeq,
                                           @Nullable String userAgent, @Nullable String authToken) throws IOException {
        if (DEBUG) {
            Log.v(TAG, "sendOptionsCommand(request=\"" + request + "\", cSeq=" + cSeq + ")");
        }
        sendSimpleCommand("OPTIONS", outputStream, request, cSeq, userAgent, null, authToken);
    }

    public static void sendGetParameterCommand(@NonNull OutputStream outputStream, @NonNull String request, int cSeq,
                                                @Nullable String userAgent, @Nullable String session, @Nullable String authToken) throws IOException {
        if (DEBUG) {
            Log.v(TAG, "sendGetParameterCommand(request=\"" + request + "\", cSeq=" + cSeq + ")");
        }
        sendSimpleCommand("GET_PARAMETER", outputStream, request, cSeq, userAgent, session, authToken);
    }

    public static void sendDescribeCommand(@NonNull OutputStream outputStream, @NonNull String request, int cSeq,
                                            @Nullable String userAgent, @Nullable String authToken) throws IOException {
        if (DEBUG) {
            Log.v(TAG, "sendDescribeCommand(request=\"" + request + "\", cSeq=" + cSeq + ")");
        }
        outputStream.write(("DESCRIBE " + request + " RTSP/1.0" + CRLF).getBytes());
        outputStream.write(("Accept: application/sdp" + CRLF).getBytes());
        if (authToken != null) {
            outputStream.write(("Authorization: " + authToken + CRLF).getBytes());
        }
        outputStream.write(("CSeq: " + cSeq + CRLF).getBytes());
        if (userAgent != null) {
            outputStream.write(("User-Agent: " + userAgent + CRLF).getBytes());
        }
        outputStream.write(CRLF.getBytes());
        outputStream.flush();
    }

    public static void sendTeardownCommand(@NonNull OutputStream outputStream, @NonNull String request, int cSeq,
                                            @Nullable String userAgent, @Nullable String authToken, @Nullable String session) throws IOException {
        if (DEBUG) {
            Log.v(TAG, "sendTeardownCommand(request=\"" + request + "\", cSeq=" + cSeq + ")");
        }
        outputStream.write(("TEARDOWN " + request + " RTSP/1.0" + CRLF).getBytes());
        if (authToken != null) {
            outputStream.write(("Authorization: " + authToken + CRLF).getBytes());
        }
        outputStream.write(("CSeq: " + cSeq + CRLF).getBytes());
        if (userAgent != null) {
            outputStream.write(("User-Agent: " + userAgent + CRLF).getBytes());
        }
        if (session != null) outputStream.write(("Session: " + session + CRLF).getBytes());
        outputStream.write(CRLF.getBytes());
        outputStream.flush();
    }

    public static void sendSetupCommand(@NonNull OutputStream outputStream, @NonNull String request, int cSeq,
                                         @Nullable String userAgent, @Nullable String authToken, @Nullable String session,
                                         @NonNull String interleaved) throws IOException {
        if (DEBUG) {
            Log.v(TAG, "sendSetupCommand(request=\"" + request + "\", cSeq=" + cSeq + ")");
        }
        outputStream.write(("SETUP " + request + " RTSP/1.0" + CRLF).getBytes());
        outputStream.write(("Transport: RTP/AVP/TCP;unicast;interleaved=" + interleaved + CRLF).getBytes());
        if (authToken != null) {
            outputStream.write(("Authorization: " + authToken + CRLF).getBytes());
        }
        outputStream.write(("CSeq: " + cSeq + CRLF).getBytes());
        if (userAgent != null) {
            outputStream.write(("User-Agent: " + userAgent + CRLF).getBytes());
        }
        if (session != null) {
            outputStream.write(("Session: " + session + CRLF).getBytes());
        }
        outputStream.write(CRLF.getBytes());
        outputStream.flush();
    }

    public static void sendPlayCommand(@NonNull OutputStream outputStream, @NonNull String request, int cSeq,
                                        @Nullable String userAgent, @Nullable String authToken, @NonNull String session) throws IOException {
        if (DEBUG) {
            Log.v(TAG, "sendPlayCommand(request=\"" + request + "\", cSeq=" + cSeq + ")");
        }

        outputStream.write(("PLAY " + request + " RTSP/1.0" + CRLF).getBytes());
        outputStream.write(("Range: npt=0.000-" + CRLF).getBytes());

        if (authToken != null) {
            outputStream.write(("Authorization: " + authToken + CRLF).getBytes());
        }

        outputStream.write(("CSeq: " + cSeq + CRLF).getBytes());
        if (userAgent != null) {
            outputStream.write(("User-Agent: " + userAgent + CRLF).getBytes());
        }
        outputStream.write(("Session: " + session + CRLF).getBytes());
        outputStream.write(CRLF.getBytes());
        outputStream.flush();
    }

}
