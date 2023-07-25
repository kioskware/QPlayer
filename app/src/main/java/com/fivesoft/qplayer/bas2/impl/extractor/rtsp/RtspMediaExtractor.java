package com.fivesoft.qplayer.bas2.impl.extractor.rtsp;

import static com.fivesoft.qplayer.bas2.common.Util.checkInterrupted;
import static com.fivesoft.qplayer.bas2.common.Util.getBytesFromHexString;
import static com.fivesoft.qplayer.impl.RtspUtil.RTSP_CAPABILITY_GET_PARAMETER;
import static com.fivesoft.qplayer.impl.RtspUtil.hasCapability;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.Authentication;
import com.fivesoft.qplayer.bas2.DataSource;
import com.fivesoft.qplayer.bas2.MediaExtractor;
import com.fivesoft.qplayer.bas2.Sample;
import com.fivesoft.qplayer.bas2.TimeoutException;
import com.fivesoft.qplayer.bas2.common.ByteUtil;
import com.fivesoft.qplayer.bas2.common.Constants;
import com.fivesoft.qplayer.bas2.common.Util;
import com.fivesoft.qplayer.bas2.decoder.MediaDecoder;
import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.common.Header;
import com.fivesoft.qplayer.common.Headers;
import com.fivesoft.qplayer.impl.RtspUtil;
import com.fivesoft.qplayer.impl.mediasource.rtsp.NetUtils;
import com.fivesoft.qplayer.impl.mediasource.rtsp.RtpParser;
import com.fivesoft.qplayer.track.AudioTrack;
import com.fivesoft.qplayer.track.SubtitleTrack;
import com.fivesoft.qplayer.track.Track;
import com.fivesoft.qplayer.track.Tracks;
import com.fivesoft.qplayer.track.VideoTrack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/*
    Based on https://github.com/alexeyvasilyev/rtsp-client-android
 */

public class RtspMediaExtractor extends MediaExtractor {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public static final int MIN_SESSION_TIMEOUT = 15;
    public static final int MAX_SESSION_TIMEOUT = 60;

    public static final String DEFAULT_USER_AGENT = Constants.Q_PLAYER_USER_AGENT;

    private volatile boolean opened = true;
    private final String uri;

    private String userAgent = DEFAULT_USER_AGENT;

    private final Queue<Sample> sampleQueue = new ArrayDeque<>(5);
    private byte[] readBuffer = new byte[0];

    private final Object bufferLock = new Object();

    private final Tracks tracks = new Tracks();

    private volatile long sessionTimeout = 0;
    private volatile long lastKeepAliveSent = Integer.MIN_VALUE;

    private volatile long sampleIndex = Constants.UNKNOWN_VALUE;

    private volatile long lastTimestamp = Constants.UNKNOWN_VALUE;

    private volatile long totalDuration = Constants.UNKNOWN_VALUE;

    private volatile long sampleCount = Constants.UNKNOWN_VALUE;

    private volatile boolean prepared = false;
    private volatile Authentication auth;
    private volatile RtspSession rtspSession;

    public RtspMediaExtractor(@NonNull DataSource dataSource, @NonNull String uri, boolean audio, boolean video, boolean subtitle, int flags) {
        super(dataSource, audio, video, subtitle, flags);
        this.uri = Objects.requireNonNull(uri, "uri is null");
    }

    public RtspMediaExtractor(@NonNull DataSource dataSource, @NonNull String uri, boolean audio, boolean video, boolean subtitle) {
        this(dataSource, uri, audio, video, subtitle, 0);
    }

    public void setUserAgent(@Nullable String userAgent) {
        this.userAgent = userAgent;
    }

    public void setBufferSize(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive");
        }
        synchronized (bufferLock) {
            readBuffer = new byte[bufferSize];
        }
    }

    @Override
    public void prepare(int timeout) throws IOException, TimeoutException, SecurityException {
        if (prepared) {
            throw new IllegalStateException("Already prepared");
        }

        InputStream in = dataSource.getInputStream();
        OutputStream out = dataSource.getOutputStream();

        if (out == null) {
            throw new IOException("Output is not supported by this data source");
        }

        rtspSession = new RtspSession(in, out, userAgent, uri, auth);

        //Get capabilities
        RtspResponse res = rtspSession.options();

        if (!res.isOk()) {
            //Throw if failed to authenticate
            throw new IOException("RTSP options failed: " + res.getCode() + " " + res.getMessage());
        }

        //Get tracks
        res = rtspSession.describe();

        if (!res.isOk()) {
            //Throw if failed to authenticate
            throw new IOException("RTSP describe failed: " + res.getCode() + " " + res.getMessage());
        }

        try {
            Headers params = RtspUtil.getDescribeParams(res.getContentAsText());
            updateTracks(params);
            Log.println(Log.ASSERT, "RTSPMediaSource", "Tracks: " + tracks);
        } catch (Exception e) {
            throw new IOException("Failed to parse tracks", e);
        }

        String session = null, uriForSetup;
        String cachedToken = rtspSession.getAuthToken();

        synchronized (tracks) {

            //Sort tracks by tags
            Track[] tracks = this.tracks.toArray();
            Arrays.sort(tracks, Comparator.comparingInt(Track::getTag));

            for (Track track : tracks) {
                if (track == null)
                    continue; //Just in case

                if (!extractAudio && track instanceof AudioTrack) {
                    continue;
                }

                if (!extractVideo && track instanceof VideoTrack) {
                    continue;
                }

                if (!extractSubtitle && track instanceof SubtitleTrack) {
                    continue;
                }

                uriForSetup = getUriForSetup(uri, track);

                if (uriForSetup == null) {
                    continue;
                }

                int clientChannel = (track.getTag() * 2);
                int serverChannel = (track.getTag() * 2 + 1);

                res = rtspSession.setup(uriForSetup, clientChannel, serverChannel, session);

                if (!res.isOk()) {
                    //Throw if failed to setup
                    throw new IOException("RTSP setup failed: " + res.getCode() + " " + res.getMessage());
                }

                session = res.getHeader("Session");

                Log.println(Log.ASSERT, "RTSPMediaSource", "Setup track: " + track);

            }
        }

        if (TextUtils.isEmpty(session)) {
            throw new IOException("Failed to get RTSP session");
        }

        res = rtspSession.play(session);
        rtspSession.setAuthToken(cachedToken);

        if (!res.isOk()) {
            //Throw if failed to play
            throw new IOException("RTSP play failed: " + res.getCode() + " " + res.getMessage());
        }

        updateSessionTimeout(session);
        lastKeepAliveSent = System.currentTimeMillis();

        prepared = true;
    }

    @Override
    public synchronized Sample nextSample() throws IOException, TimeoutException, IllegalStateException, InterruptedException {

        if (!prepared) {
            throw new IllegalStateException("Not prepared, call prepare() first");
        }

        if (sampleQueue.size() == 0) {
            //There is no samples in queue, read next samples
            readNextSample();
        }

        sendKeepAliveIfNeeded();
        sampleIndex++;

        return sampleQueue.poll();
    }

    private void readNextSample() throws IOException, TimeoutException {
        //Read samples
        synchronized (bufferLock) {
            InputStream inputStream = dataSource.getInputStream();
            RtpParser.RtpHeader header = RtpParser.readHeader(inputStream);

            if (header == null) {
                return;
            }

            if (header.payloadSize > readBuffer.length || true) {
                readBuffer = new byte[header.payloadSize];
            }

            NetUtils.readData(inputStream, readBuffer, 0, header.payloadSize);

            Track track = tracks.getByPayloadType(header.payloadType);

            if (track == null) {
                return;
            }

            long timestamp = header.timestamp;
            if (track.getClockRate() > 0) {
                timestamp = (long) (((double) header.timestamp / (double) track.getClockRate()) * 1000.0);
            }

            lastTimestamp = timestamp;
            sampleQueue.add(new Sample(readBuffer, timestamp, track));
        }
    }

    private void sendKeepAliveIfNeeded() {

        if (Math.abs(System.currentTimeMillis() - lastKeepAliveSent) <
                (Util.limit(sessionTimeout, MIN_SESSION_TIMEOUT, MAX_SESSION_TIMEOUT) * 1000 / 2))
            return;

        try {
            Log.println(Log.ASSERT, "RTSPMediaSource", "Sending keep alive timeout: " + sessionTimeout + "s");
            rtspSession.sendKeepAlive();
            lastKeepAliveSent = System.currentTimeMillis();
        } catch (Exception e) {
            //Failed to send keep alive command
        }

    }

    @NonNull
    @Override
    public Tracks getTracks() throws IllegalStateException {
        if (!prepared) {
            throw new IllegalStateException("Not prepared, call prepare() first");
        }
        return tracks;
    }

    @Override
    public long getPosition() {
        return lastTimestamp;
    }

    @Override
    public long getSampleIndex() {
        return sampleIndex;
    }

    @Override
    public int getSampleFormat() {
        return MediaDecoder.FORMAT_RTP;
    }

    @Override
    public void close() throws IOException {
        dataSource.close();
    }

    @Override
    public void setAuthentication(@Nullable Authentication authentication) {
        this.auth = authentication;
    }

    @Override
    protected void onTimeoutSet(int timeout) throws IOException {

    }

    private void updateSessionTimeout(@Nullable String session) {

        if (!TextUtils.isEmpty(session)) {
            // ODgyODg3MjQ1MDczODk3NDk4Nw;timeout=30
            String[] params = TextUtils.split(session, ";");
            // Getting session timeout
            if (params.length > 1) {
                params = TextUtils.split(params[1], "=");
                if (params.length > 1) {
                    try {
                        sessionTimeout = Integer.parseInt(params[1]);
                    } catch (Exception e) {
                        Log.e("RTSPMediaSource", "Failed to parse RTSP session timeout");
                    }
                }
            }
        }
        Log.println(Log.ASSERT, "RTSPMediaSource", "Session timeout: " + sessionTimeout);

    }

    @Nullable
    private static String getUriForSetup(@NonNull String uri, @Nullable Track track) {
        if (track == null || TextUtils.isEmpty(track.getId()))
            return null;

        String res = uri;
        if (track.getId().startsWith("rtsp://") ||
                track.getId().startsWith("rtsps://")) {
            // Absolute URL
            res = track.getId();
        } else {
            // Relative URL
            if (!track.getId().startsWith("/")) {
                res += "/";
            }
            res += track.getId();
        }
        return res;
    }

    private void updateTracks(@NonNull Headers params) throws InterruptedIOException {
        Track.Builder cb = null;
        int trackCount = 0;
        for (Header param : params) {
            checkInterrupted();
            switch (param.name) {
                case "m": {
                    if (param.value.startsWith("video")) {
                        cb = new Track.Builder(Track.VIDEO);
                    } else if (param.value.startsWith("audio")) {
                        cb = new AudioTrack.Builder(Track.AUDIO);
                    } else {
                        cb = null;
                    }
                    if (cb != null) {
                        String[] values = TextUtils.split(param.value, " ");
                        try {
                            cb.setPayloadType(values.length > 3 ? Integer.parseInt(values[3]) : -1);
                        } catch (Exception e) {
                            cb.setPayloadType(-1);
                        }
                        cb.setTag(trackCount++);
                    }
                    break;
                }
                case "a": {
                    // a=control:trackID=1
                    if (cb != null) {
                        if (param.value.startsWith("control:")) {
                            cb.setId(param.value.substring(8));
                            synchronized (tracks) {
                                Track track = cb.build();
                                if (track != null) {
                                    tracks.put(track);
                                }
                            }
                        } else if (param.value.startsWith("fmtp:")) {
                            // a=fmtp:96 packetization-mode=1; profile-level-id=4D4029; sprop-parameter-sets=Z01AKZpmBkCb8uAtQEBAQXpw,aO48gA==
                            // a=fmtp:97 streamtype=5; profile-level-id=15; mode=AAC-hbr; config=1408; sizeLength=13; indexLength=3; indexDeltaLength=3; profile=1; bitrate=32000;
                            // a=fmtp:97 streamtype=5;profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3;config=1408
                            // a=fmtp:96 streamtype=5; profile-level-id=14; mode=AAC-lbr; config=1388; sizeLength=6; indexLength=2; indexDeltaLength=2; constantDuration=1024; maxDisplacement=5
                            // a=fmtp:96 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3;config=1210fff15081ffdffc
                            // a=fmtp:96
                            String temp = param.value.substring(5).trim();
                            int index = temp.indexOf(" ");
                            if (index != -1) {
                                temp = temp.substring(0, index);
                            }
                            temp = temp.trim();

                            String payloadType = temp;
                            String paramsString;

                            if(index != -1) {
                                paramsString = param.value.substring(index + 1).trim();
                            } else {
                                paramsString = "";
                            }

                            parseRtspParamString(paramsString, cb);
                        } else if (param.value.startsWith("rtpmap:")) {
                            // a=rtpmap:96 H264/90000
                            // a=rtpmap:97 mpeg4-generic/16000/1
                            // a=rtpmap:97 MPEG4-GENERIC/16000
                            // a=rtpmap:97 G726-32/8000
                            // a=rtpmap:96 mpeg4-generic/44100/2
                            String[] values = TextUtils.split(param.value, " ");
                            if (cb.getType() == Track.VIDEO) {
                                // Video
                                if (values.length > 1) {
                                    values = TextUtils.split(values[1], "/");
                                    if (values.length > 0) {
                                        cb.setFormat(values[0].toLowerCase());
                                    }
                                    if (values.length > 1) {
                                        try {
                                            cb.setClockRate(Integer.parseInt(values[1]));
                                        } catch (Exception e) {
                                            cb.setClockRate(Constants.UNKNOWN_VALUE);
                                        }
                                    }
                                }
                            } else if (cb.getType() == Track.AUDIO) {
                                // Audio
                                if (values.length > 1) {
                                    values = TextUtils.split(values[1], "/");
                                    if (values.length > 1) {
                                        cb.setFormat(values[0].toLowerCase());
                                        try {
                                            cb.setClockRate(Integer.parseInt(values[1]));
                                        } catch (NumberFormatException e) {
                                            cb.setClockRate(Constants.UNKNOWN_VALUE);
                                        }
                                        // If no channels specified, use mono, e.g. "a=rtpmap:97 MPEG4-GENERIC/8000"
                                        cb.setChannels(values.length > 2 ? Integer.parseInt(values[2]) : 1);
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case "s": {
                    if (cb != null) {
                        cb.setTitle(param.value);
                    }
                    break;
                }
                case "i": {
                    if (cb != null) {
                        cb.setDescription(param.value);
                    }
                    break;
                }
            }
        }
    }

    private static void parseRtspParamString(@NonNull String input, @NonNull Track.Builder target) {
        Log.println(Log.ASSERT, "", "parseRtspParamString: " + input);
        // Split the input string by semicolon (;)
        String[] keyValuePairs = input.split(";");

        // Iterate through each key-value pair
        for (String pair : keyValuePairs) {
            Log.println(Log.ASSERT, "", "pair: " + pair);
            // Split each pair by equals (=)
            String[] keyValue = pair.trim().split("=");

            // Ensure the pair has a key and a value
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                if ("sprop-parameter-sets".equalsIgnoreCase(key)) {
                    String[] paramsSpsPps = TextUtils.split(value, ",");
                    try {
                        if (paramsSpsPps.length > 1) {
                            byte[] sps = Base64.decode(paramsSpsPps[0], Base64.NO_WRAP);
                            byte[] pps = Base64.decode(paramsSpsPps[1], Base64.NO_WRAP);
                            target.setCsd(0, sps);
                            target.setCsd(1, pps);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if ("config".equalsIgnoreCase(key)) {
                    try {
                        target.setCsd(0, getBytesFromHexString(value));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if ("mode".equalsIgnoreCase(key)) {
                    target.setMode(value);
                } else {
                    // Add the key-value pair to metadata
                    target.setMetadata(key, value);
                }
            }
        }
    }

}
