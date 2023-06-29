package com.fivesoft.qplayer.impl.mediasource.rtsp;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.common.Credentials;
import com.fivesoft.qplayer.common.Header;
import com.fivesoft.qplayer.common.Headers;
import com.fivesoft.qplayer.common.ResponseHeader;
import com.fivesoft.qplayer.impl.RtspCommandUtil;
import com.fivesoft.qplayer.impl.mediasource.SocketMediaSource;
import com.fivesoft.qplayer.track.AudioTrack;
import com.fivesoft.qplayer.track.Track;
import com.fivesoft.qplayer.track.VideoTrack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Based on https://github.com/alexeyvasilyev/rtsp-client-android/blob/master/library-client-rtsp/src/main/java/com/alexvas/rtsp/RtspClient.java
 */

public class RTSPMediaSource extends SocketMediaSource {

    public static final String DEFAULT_USER_AGENT = "Lavf58.29.100";

    private volatile boolean opened = true;

    public final static int RTSP_CAPABILITY_NONE = 0;
    public final static int RTSP_CAPABILITY_OPTIONS = 1 << 1;
    public final static int RTSP_CAPABILITY_DESCRIBE = 1 << 2;
    public final static int RTSP_CAPABILITY_ANNOUNCE = 1 << 3;
    public final static int RTSP_CAPABILITY_SETUP = 1 << 4;
    public final static int RTSP_CAPABILITY_PLAY = 1 << 5;
    public final static int RTSP_CAPABILITY_RECORD = 1 << 6;
    public final static int RTSP_CAPABILITY_PAUSE = 1 << 7;
    public final static int RTSP_CAPABILITY_TEARDOWN = 1 << 8;
    public final static int RTSP_CAPABILITY_SET_PARAMETER = 1 << 9;
    public final static int RTSP_CAPABILITY_GET_PARAMETER = 1 << 10;
    public final static int RTSP_CAPABILITY_REDIRECT = 1 << 11;

    private static final String CRLF = "\r\n";

    // Size of buffer for reading from the connection
    private final static int MAX_LINE_SIZE = 4098;

    @Nullable
    private final Credentials credentials;

    private String userAgent = DEFAULT_USER_AGENT;

    private final HashMap<Integer, Track> tracks = new HashMap<>();

    private volatile Runnable keepAliveRunnable;
    private volatile long sessionTimeout = 0;
    private volatile long lastKeepAliveSent = Integer.MIN_VALUE;

    public RTSPMediaSource(@NonNull String uri, long timeout, @Nullable Credentials credentials) {
        super(uri, timeout);
        this.credentials = credentials;
    }

    public RTSPMediaSource(@NonNull String uri, @Nullable Credentials credentials) {
        this(uri, DEFAULT_TIMEOUT, credentials);
    }

    public RTSPMediaSource(@NonNull String uri) {
        this(uri, DEFAULT_TIMEOUT, null);
    }

    @Override
    public void onSocketConnected(@NonNull Socket socket) throws IOException {
        Log.println(Log.ASSERT, "RTSPMediaSource", "onSocketConnected");
        opened = true;
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        AtomicInteger cSeq = new AtomicInteger(0);

        String[] authToken = new String[1];
        //noinspection unchecked
        Pair<String, String>[] digestRealmNonce = new Pair[1];
        String uriRtsp = uri.toString();

        //Try with no auth
        RtspCommandUtil.sendOptionsCommand(out, uriRtsp, cSeq.addAndGet(1), userAgent, null);
        ResponseHeader res = readResponseHeader(in);

        Log.println(Log.ASSERT, "RTSPMediaSource", "Options response: " + res.code + " " + res.message + "\n" + res.headers);

        //Try with auth if needed
        if (res.code == 401) {
            res = tryWithAuth(res, false, in, out, cSeq, uriRtsp, credentials, userAgent, authToken, digestRealmNonce);
        }

        Log.println(Log.ASSERT, "RTSPMediaSource", "Options response: " + res.code + "" + res.message + "\n" + res.headers);

        // Check if status is OK
        //checkStatusCode(res.code);
        int capabilities = getSupportedCapabilities(res.headers);

        Log.println(Log.ASSERT, "RTSPMediaSource", "Capabilities: " + capabilities);

        // Check if DESCRIBE works
        RtspCommandUtil.sendDescribeCommand(out, uriRtsp, cSeq.addAndGet(1), userAgent, authToken[0]);
        res = readResponseHeader(in);

        Log.println(Log.ASSERT, "RTSPMediaSource", "Describe response: " + res.code + " " + res.message + "\n" + res.headers);

        // Try once again with credentials. OPTIONS command can be accepted without authentication.
        if (res.code == 401) {
            res = tryWithAuth(res, true, in, out, cSeq, uriRtsp, credentials, userAgent, authToken, digestRealmNonce);
        }

        // Check if status is OK
        checkStatusCode(res.code);

        int contentLength = getHeaderContentLength(res.headers);
        if (contentLength > 0) {
            String content = readContentAsText(in, contentLength);
            Log.println(Log.ASSERT, "RTSPMediaSource", "Content: " + content);
            try {
                Headers params = getDescribeParams(content);
                updateTracks(params);
                Log.println(Log.ASSERT, "RTSPMediaSource", "Tracks: " + tracks);
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        String session = null, uriForSetup = null;
        Pair<String, String> realmNonce = digestRealmNonce[0];
        int sessionTimeout = 0;

        synchronized (tracks) {
            for (Track track : tracks.values()) {
                if(track == null)
                    continue; //Just in case

                uriForSetup = getUriForSetup(uriRtsp, track);

                if(uriForSetup == null) {
                    continue;
                }

                if (realmNonce != null) {
                    if (credentials != null) {
                        authToken[0] = getDigestAuthHeader(credentials.username, credentials.password, "SETUP", uriForSetup, realmNonce.first, realmNonce.second);
                    } else {
                        authToken[0] = getDigestAuthHeader(null, null, "SETUP", uriForSetup, realmNonce.first, realmNonce.second);
                    }
                }

                RtspCommandUtil.sendSetupCommand(out, uriForSetup, cSeq.addAndGet(1), userAgent, authToken[0], session, track.tag + "-" + (track.tag + 1));
                res = readResponseHeader(in);

                checkStatusCode(res.code);

                session = res.headers.get("Session");

                if (!TextUtils.isEmpty(session)) {
                    // ODgyODg3MjQ1MDczODk3NDk4Nw;timeout=30
                    String[] params = TextUtils.split(session, ";");
                    session = params[0];
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
            }
        }

        if (TextUtils.isEmpty(session)) {
            throw new IOException("Failed to get RTSP session");
        }

        if (realmNonce != null) {
            if (credentials != null) {
                authToken[0] = getDigestAuthHeader(credentials.username, credentials.password, "PLAY", uriRtsp, realmNonce.first, realmNonce.second);
            } else {
                authToken[0] = getDigestAuthHeader(null, null, "PLAY", uriRtsp, realmNonce.first, realmNonce.second);
            }
        }

        RtspCommandUtil.sendPlayCommand(out, uriRtsp, cSeq.addAndGet(1), userAgent, authToken[0], session);
        res = readResponseHeader(in);

        checkStatusCode(res.code);

        Log.println(Log.ASSERT, "RTSPMediaSource", "Play response: " + res.code + " " + res.message + "\n" + res.headers);

        String method = hasCapability(RTSP_CAPABILITY_GET_PARAMETER, capabilities) ? "GET_PARAMETER" : "OPTIONS";
        if (realmNonce != null) {
            if (credentials != null) {
                authToken[0] = getDigestAuthHeader(credentials.username, credentials.password, method, uriRtsp, realmNonce.first, realmNonce.second);
            } else {
                authToken[0] = getDigestAuthHeader(null, null, method, uriRtsp, realmNonce.first, realmNonce.second);
            }
        }

        this.sessionTimeout = sessionTimeout;
        String finalSession = session;

        //Create keep alive runnable
        keepAliveRunnable = () -> {
            try {
                if (hasCapability(RTSP_CAPABILITY_GET_PARAMETER, capabilities)) {
                    RtspCommandUtil.sendGetParameterCommand(out, uriRtsp, cSeq.addAndGet(1), userAgent, finalSession, authToken[0]);
                } else {
                    RtspCommandUtil.sendOptionsCommand(out, uriRtsp, cSeq.addAndGet(1), userAgent, authToken[0]);
                }
                // Do not read response right now, since it may contain unread RTP frames.
                // RtpHeader.searchForNextRtpHeader will handle that.
            } catch (IOException e) {
                //Failed to send keep alive command
                throw new RuntimeException(e);
            }
        };

    }

    @Override
    public void onDataReceived(byte[] data, int offset, int length) {



        sendKeepAliveIfNeeded();
    }

    @Override
    public void onNothingReceived() {
        //Log.println(Log.ASSERT, "RTSPMediaSource", "Nothing received") ;
        sendKeepAliveIfNeeded();
    }

    @Override
    public void onReadError(Exception e) {

    }

    @Override
    public void onConnectError(Exception e) {

    }

    @Override
    public void onSocketClosed() {
        opened = false;
    }

    @Nullable
    public Credentials getCredentials() {
        return credentials;
    }

    private void sendKeepAliveIfNeeded(){
        if(Math.abs(SystemClock.elapsedRealtime() - lastKeepAliveSent) < (sessionTimeout * 1000 / 2))
            return;

        Runnable keepAliveRunnable = this.keepAliveRunnable;
        if(keepAliveRunnable != null) {
            try {
                keepAliveRunnable.run();
                lastKeepAliveSent = SystemClock.elapsedRealtime();
            } catch (Exception e) {
                //Failed to send keep alive command
            }
        }
    }

    @Nullable
    private static String getUriForSetup(@NonNull String uriRtsp, @Nullable Track track) {
        if (track == null || TextUtils.isEmpty(track.id)) return null;

        String uriRtspSetup = uriRtsp;
        if (track.id.startsWith("rtsp://") || track.id.startsWith("rtsps://")) {
            // Absolute URL
            uriRtspSetup = track.id;
        } else {
            // Relative URL
            if (!track.id.startsWith("/")) {
                track.id = "/" + track.id;
            }
            uriRtspSetup += track.id;
        }
        return uriRtspSetup;
    }

    private static boolean hasCapability(int capability, int capabilitiesMask) {
        return (capabilitiesMask & capability) != 0;
    }

    private ResponseHeader tryWithAuth(ResponseHeader res, boolean describe, InputStream in, OutputStream out, AtomicInteger cSeq,
                                       String uriRtsp, Credentials credentials, String userAgent,
                                       @Nullable String[] authTokenOut, @Nullable Pair<String, String>[] digestRealmNonceOut) throws IOException {

        Pair<String, String> digestRealmNonce = getHeaderWwwAuthenticateDigestRealmAndNonce(res.headers);
        String authToken;
        if (digestRealmNonce == null) {
            String basicRealm = getHeaderWwwAuthenticateBasicRealm(res.headers);

            if (TextUtils.isEmpty(basicRealm)) {
                throw new IOException("Unknown authentication type");
            }
            // Basic auth
            authToken = getBasicAuthHeader(credentials);
        } else {
            // Digest auth
            if (describe) {
                authToken = getDigestAuthHeader(credentials, "DESCRIBE", uriRtsp, digestRealmNonce.first, digestRealmNonce.second);
            } else {
                authToken = getDigestAuthHeader(credentials, "OPTIONS", uriRtsp, digestRealmNonce.first, digestRealmNonce.second);
            }
        }

        if (describe) {
            RtspCommandUtil.sendDescribeCommand(out, uriRtsp, cSeq.addAndGet(1), userAgent, authToken);
        } else {
            RtspCommandUtil.sendOptionsCommand(out, uriRtsp, cSeq.addAndGet(1), userAgent, authToken);
        }
        if(authTokenOut != null) {
            authTokenOut[0] = authToken;
        }
        if(digestRealmNonceOut != null) {
            digestRealmNonceOut[0] = digestRealmNonce;
        }
        return readResponseHeader(in);
    }

    private int readResponseStatusCode(@NonNull InputStream inputStream) throws IOException {
        String line;
        byte[] rtspHeader = "RTSP/1.0 ".getBytes();
        // Search fpr "RTSP/1.0 "
        while (opened && readUntilBytesFound(inputStream, rtspHeader) && (line = readLine(inputStream)) != null) {
            int indexCode = line.indexOf(' ');
            String code = line.substring(0, indexCode);
            try {
                return Integer.parseInt(code);
            } catch (NumberFormatException e) {
                // Does not fulfill standard "RTSP/1.1 200 OK" token
                // Continue search for
            }
        }
        return -1;
    }

    @NonNull
    private Headers readResponseHeaders(@NonNull InputStream inputStream) throws IOException {
        Headers headers = new Headers();
        String line;
        while (opened && !TextUtils.isEmpty(line = readLine(inputStream))) {
            if (CRLF.equals(line)) {
                return headers;
            } else {
                String[] pairs = TextUtils.split(line, ":");
                if (pairs.length == 2) {
                    headers.add(pairs[0].trim(), pairs[1].trim());
                }
            }
        }
        return headers;
    }

    private ResponseHeader readResponseHeader(@NonNull InputStream inputStream) throws IOException {
        int status = readResponseStatusCode(inputStream);
        Headers headers = readResponseHeaders(inputStream);
        return new ResponseHeader(status, null, headers);
    }

    private static int getHeaderContentLength(@NonNull Headers headers) {
        String length = headers.get("content-length");
        if (!TextUtils.isEmpty(length)) {
            try {
                return Integer.parseInt(length);
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private static int getSupportedCapabilities(@NonNull Headers headers) {
        for (Header header : headers) {
            String h = header.name.toLowerCase();
            if ("public".equals(h)) {
                int mask = 0;
                String[] tokens = TextUtils.split(header.value.toLowerCase(), ",");
                for (String token : tokens) {
                    switch (token.trim()) {
                        case "options":
                            mask |= RTSP_CAPABILITY_OPTIONS;
                            break;
                        case "describe":
                            mask |= RTSP_CAPABILITY_DESCRIBE;
                            break;
                        case "announce":
                            mask |= RTSP_CAPABILITY_ANNOUNCE;
                            break;
                        case "setup":
                            mask |= RTSP_CAPABILITY_SETUP;
                            break;
                        case "play":
                            mask |= RTSP_CAPABILITY_PLAY;
                            break;
                        case "record":
                            mask |= RTSP_CAPABILITY_RECORD;
                            break;
                        case "pause":
                            mask |= RTSP_CAPABILITY_PAUSE;
                            break;
                        case "teardown":
                            mask |= RTSP_CAPABILITY_TEARDOWN;
                            break;
                        case "set_parameter":
                            mask |= RTSP_CAPABILITY_SET_PARAMETER;
                            break;
                        case "get_parameter":
                            mask |= RTSP_CAPABILITY_GET_PARAMETER;
                            break;
                        case "redirect":
                            mask |= RTSP_CAPABILITY_REDIRECT;
                            break;
                    }
                }
                return mask;
            }
        }
        return RTSP_CAPABILITY_NONE;
    }

    @Nullable
    private static Pair<String, String> getHeaderWwwAuthenticateDigestRealmAndNonce(@NonNull Headers headers) {
        for (Header header: headers) {
            String h = header.name.toLowerCase();
            if ("www-authenticate".equals(h) && header.value.toLowerCase().startsWith("digest")) {
                String v = header.value.substring(7).trim();
                int begin, end;

                begin = v.indexOf("realm=");
                begin = v.indexOf('"', begin) + 1;
                end = v.indexOf('"', begin);
                String digestRealm = v.substring(begin, end);

                begin = v.indexOf("nonce=");
                begin = v.indexOf('"', begin) + 1;
                end = v.indexOf('"', begin);
                String digestNonce = v.substring(begin, end);
                return Pair.create(digestRealm, digestNonce);
            }
        }
        return null;
    }

    @Nullable
    private static String getHeaderWwwAuthenticateBasicRealm(@NonNull Headers headers) {
        for (Header head : headers) {
            String h = head.name.toLowerCase();
            String v = head.value.toLowerCase();
            if ("www-authenticate".equals(h) && v.startsWith("basic")) {
                v = v.substring(6).trim();
                String[] tokens = TextUtils.split(v, "\"");
                if (tokens.length > 2) {
                    return tokens[1];
                }
            }
        }
        return null;
    }

    // Basic authentication
    @NonNull
    private static String getBasicAuthHeader(@Nullable String username, @Nullable String password) {
        String auth = (username == null ? "" : username) + ":" + (password == null ? "" : password);
        return "Basic " + new String(Base64.encode(auth.getBytes(StandardCharsets.ISO_8859_1), Base64.NO_WRAP));
    }

    @NonNull
    private static String getBasicAuthHeader(@Nullable Credentials credentials) {
        if (credentials == null) {
            return getBasicAuthHeader(null, null);
        }
        return getBasicAuthHeader(credentials.username, credentials.password);
    }

    // Digest authentication
    @Nullable
    private static String getDigestAuthHeader(@Nullable String username, @Nullable String password, @NonNull String method,
                                              @NonNull String digestUri, @NonNull String realm, @NonNull String nonce) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] ha1;

            if (username == null) username = "";
            if (password == null) password = "";

            // calc A1 digest
            md.update(username.getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            md.update(realm.getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            md.update(password.getBytes(StandardCharsets.ISO_8859_1));
            ha1 = md.digest();

            // calc A2 digest
            md.reset();
            md.update(method.getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            md.update(digestUri.getBytes(StandardCharsets.ISO_8859_1));
            byte[] ha2 = md.digest();

            // calc response
            md.update(getHexStringFromBytes(ha1).getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            md.update(nonce.getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            // TODO add support for more secure version of digest auth
            //md.update(nc.getBytes(StandardCharsets.ISO_8859_1));
            //md.update((byte) ':');
            //md.update(cnonce.getBytes(StandardCharsets.ISO_8859_1));
            //md.update((byte) ':');
            //md.update(qop.getBytes(StandardCharsets.ISO_8859_1));
            //md.update((byte) ':');
            md.update(getHexStringFromBytes(ha2).getBytes(StandardCharsets.ISO_8859_1));
            String response = getHexStringFromBytes(md.digest());

            return "Digest username=\"" + username + "\", realm=\"" + realm + "\", nonce=\"" + nonce + "\", uri=\"" + digestUri + "\", response=\"" + response + "\"";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private static String getDigestAuthHeader(@Nullable Credentials credentials, @NonNull String method,
                                              @NonNull String digestUri, @NonNull String realm, @NonNull String nonce) {
        if (credentials == null) {
            return getDigestAuthHeader(null, null, method, digestUri, realm, nonce);
        }
        return getDigestAuthHeader(credentials.username, credentials.password, method, digestUri, realm, nonce);
    }

    @NonNull
    private static String getHexStringFromBytes(@NonNull byte[] bytes) {
        StringBuilder buf = new StringBuilder();
        for (byte b : bytes)
            buf.append(String.format("%02x", b));
        return buf.toString();
    }

    @NonNull
    private static String readContentAsText(@NonNull InputStream inputStream, int length) throws IOException {
        if (length <= 0) return "";
        byte[] b = new byte[length];
        int read = readData(inputStream, b, 0, length);
        return new String(b, 0, read);
    }

    public static boolean memcmp(@NonNull byte[] source1, int offsetSource1, @NonNull byte[] source2, int offsetSource2, int num) {
        if (source1.length - offsetSource1 < num) return false;
        if (source2.length - offsetSource2 < num) return false;

        for (int i = 0; i < num; i++) {
            if (source1[offsetSource1 + i] != source2[offsetSource2 + i]) return false;
        }
        return true;
    }

    private static void shiftLeftArray(@NonNull byte[] array, int num) {
        // ABCDEF -> BCDEF
        if (num - 1 >= 0) System.arraycopy(array, 1, array, 0, num - 1);
    }

    private boolean readUntilBytesFound(@NonNull InputStream inputStream, @NonNull byte[] array) throws IOException {
        byte[] buffer = new byte[array.length];

        // Fill in buffer
        if (NetUtils.readData(inputStream, buffer, 0, buffer.length) != buffer.length)
            return false; // EOF

        while (opened) {
            // Check if buffer is the same one
            if (memcmp(buffer, 0, array, 0, buffer.length)) {
                return true;
            }
            // ABCDEF -> BCDEFF
            shiftLeftArray(buffer, buffer.length);
            // Read 1 byte into last buffer item
            if (NetUtils.readData(inputStream, buffer, buffer.length - 1, 1) != 1) {
                return false; // EOF
            }
        }
        return false;
    }

    @Nullable
    private String readLine(@NonNull InputStream inputStream) throws IOException {
        byte[] bufferLine = new byte[MAX_LINE_SIZE];
        int offset = 0;
        int readBytes;
        do {
            // Didn't find "\r\n" within 4K bytes
            if (offset >= MAX_LINE_SIZE) {
                throw new NoResponseHeadersException();
            }

            // Read 1 byte
            readBytes = inputStream.read(bufferLine, offset, 1);
            if (readBytes == 1) {
                // Check for EOL
                // Some cameras like Linksys WVC200 do not send \n instead of \r\n
                if (offset > 0 && /*bufferLine[offset-1] == '\r' &&*/ bufferLine[offset] == '\n') {
                    // Found empty EOL. End of header section
                    if (offset == 1) return "";//break;

                    // Found EOL. Add to array.
                    return new String(bufferLine, 0, offset - 1);
                } else {
                    offset++;
                }
            }
        } while (readBytes > 0 && opened);
        return null;
    }

    private static int readData(@NonNull InputStream inputStream, @NonNull byte[] buffer, int offset, int length) throws IOException {
        int readBytes;
        int totalReadBytes = 0;
        do {
            readBytes = inputStream.read(buffer, offset + totalReadBytes, length - totalReadBytes);
            if (readBytes > 0) totalReadBytes += readBytes;
        } while (readBytes >= 0 && totalReadBytes < length);
        return totalReadBytes;
    }

    private static void checkStatusCode(int code) throws IOException {
        switch (code) {
            case 200:
                break;
            case 401:
                throw new UnauthorizedException();
            default:
                throw new IOException("Invalid status code " + code);
        }
    }

    private void updateTracks(@NonNull Headers params) {
        Track currentTrack = null;
        int trackCount = 0;
        for (Header param : params) {
            switch (param.name) {
                case "m": {
                    if (param.value.startsWith("video")) {
                        currentTrack = new VideoTrack();
                    } else if (param.value.startsWith("audio")) {
                        currentTrack = new AudioTrack();
                    } else {
                        currentTrack = null;
                    }
                    if (currentTrack != null) {
                        String[] values = TextUtils.split(param.value, " ");
                        try {
                            currentTrack.payloadType = (values.length > 3 ? Integer.parseInt(values[3]) : -1);
                        } catch (Exception e) {
                            currentTrack.payloadType = -1;
                        }
                    }
                    break;
                }
                case "a": {
                    // a=control:trackID=1
                    if (currentTrack != null) {
                        if (param.value.startsWith("control:")) {
                            currentTrack.id = param.value.substring(8);
                            currentTrack.tag = trackCount++;
                            synchronized (tracks) {
                                tracks.put(currentTrack.payloadType, currentTrack);
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
                            parseRtspParamString(temp, currentTrack);
                        } else if (param.value.startsWith("rtpmap:")) {
                            // a=rtpmap:96 H264/90000
                            // a=rtpmap:97 mpeg4-generic/16000/1
                            // a=rtpmap:97 MPEG4-GENERIC/16000
                            // a=rtpmap:97 G726-32/8000
                            // a=rtpmap:96 mpeg4-generic/44100/2
                            String[] values = TextUtils.split(param.value, " ");
                            if (currentTrack instanceof VideoTrack) {
                                // Video
                                if (values.length > 1) {
                                    values = TextUtils.split(values[1], "/");
                                    if (values.length > 0) {
                                        currentTrack.format = values[0].toLowerCase();
                                    }
                                }
                            } else {
                                // Audio
                                if (values.length > 1) {
                                    AudioTrack track = ((AudioTrack) currentTrack);
                                    values = TextUtils.split(values[1], "/");
                                    if (values.length > 1) {
                                        track.format = values[0].toLowerCase();
                                        track.sampleRate = Integer.parseInt(values[1]);
                                        // If no channels specified, use mono, e.g. "a=rtpmap:97 MPEG4-GENERIC/8000"
                                        track.channels = values.length > 2 ? Integer.parseInt(values[2]) : 1;
                                    }
                                }

                            }
                        }
                    }
                    break;
                }
                case "s": {
                    if (currentTrack != null) {
                        currentTrack.title = param.value;
                    }
                    break;
                }
                case "i": {
                    if (currentTrack != null) {
                        currentTrack.description = param.value;
                    }
                    break;
                }
            }
        }
    }

    private static void parseRtspParamString(@NonNull String input, @NonNull Track target) {
        // Split the input string by semicolon (;)
        String[] keyValuePairs = input.split(";");

        // Iterate through each key-value pair
        for (String pair : keyValuePairs) {
            // Split each pair by equals (=)
            String[] keyValue = pair.trim().split("=");

            // Ensure the pair has a key and a value
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                // Add the key-value pair to metadata
                target.setMetadata(key, value);
            }
        }
    }

    @NonNull
    private static byte[] getBytesFromHexString(@NonNull String config) {
        // "1210fff1" -> [12, 10, ff, f1]
        return new BigInteger(config, 16).toByteArray();
    }

    @NonNull
    private static Headers getDescribeParams(@NonNull String text) {
        Headers res = new Headers();
        String[] params = TextUtils.split(text, "\r\n");
        int i;
        for (String param : params) {
            i = param.indexOf('=');
            if (i > 0) {
                res.add(param.substring(0, i).trim(), param.substring(i + 1));
            }
        }
        return res;
    }

    public static class UnauthorizedException extends IOException {
        UnauthorizedException() {
            super("Unauthorized");
        }
    }

    public final static class NoResponseHeadersException extends IOException {
    }

}
