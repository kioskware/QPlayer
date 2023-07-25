package com.fivesoft.qplayer.bas2.impl.extractor.rtsp;

import static com.fivesoft.qplayer.bas2.common.Constants.CRLF;
import static com.fivesoft.qplayer.bas2.common.Util.checkInterrupted;
import static com.fivesoft.qplayer.impl.RtspUtil.RTSP_CAPABILITY_GET_PARAMETER;
import static com.fivesoft.qplayer.impl.RtspUtil.getBasicAuthHeader;
import static com.fivesoft.qplayer.impl.RtspUtil.getHeaderWwwAuthenticateBasicRealm;
import static com.fivesoft.qplayer.impl.RtspUtil.getHeaderWwwAuthenticateDigestRealmAndNonce;
import static com.fivesoft.qplayer.impl.RtspUtil.readResponseHeader;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.qplayer.bas2.Authentication;
import com.fivesoft.qplayer.common.Credentials;
import com.fivesoft.qplayer.common.ResponseHeader;
import com.fivesoft.qplayer.impl.RtspUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class RtspSession {

    private final InputStream in;
    private final OutputStream out;
    private final String userAgent;
    private final String uri;

    private final AtomicLong cSeq = new AtomicLong(0);

    private final Authentication auth;
    private String authToken;
    private String session;

    private int capabilities = 0;

    public RtspSession(@NonNull InputStream in, @NonNull OutputStream out,
                       @Nullable String userAgent, @NonNull String uri, @Nullable Authentication auth) {
        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
        this.userAgent = userAgent;
        this.uri = Objects.requireNonNull(uri);
        this.auth = auth;
    }

    public int getCapabilities() {
        return capabilities;
    }

    public boolean hasCapability(int capability) {
        return (capabilities & capability) != 0;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getSession() {
        return session;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    public RtspResponse sendKeepAlive() throws IOException {
        if (hasCapability(RTSP_CAPABILITY_GET_PARAMETER)) {
            return sendCommandBasic("GET_PARAMETER", session, authToken, uri, false);
        } else {
            return sendCommandBasic("OPTIONS", session, authToken, uri, false);
        }
    }

    public RtspResponse options() throws IOException {
        return options(uri);
    }

    public RtspResponse options(@NonNull String uri) throws IOException {
        return options(uri, session);
    }

    public RtspResponse options(@NonNull String uri, @Nullable String session) throws IOException {
        return sendCommand("OPTIONS", session, uri);
    }

    public RtspResponse setup(int clientChannel, int serverChannel) throws IOException {
        return setup(uri, clientChannel, serverChannel);
    }

    public RtspResponse setup(@NonNull String uri, int clientChannel, int serverChannel) throws IOException {
        return setup(uri, clientChannel, serverChannel, session);
    }

    public RtspResponse setup(@NonNull String uri, int clientChannel, int serverChannel, @Nullable String session) throws IOException {
        return sendCommand("SETUP", session, uri,
                "Transport", "RTP/AVP/TCP;unicast;interleaved=" + clientChannel + "-" + serverChannel);
    }

    public RtspResponse setup(int clientChannel, int serverChannel, @Nullable String session) throws IOException {
        return setup(uri, clientChannel, serverChannel, session);
    }

    public RtspResponse describe() throws IOException {
        return describe(uri);
    }

    public RtspResponse describe(@NonNull String uri) throws IOException {
        return describe(uri, session);
    }

    public RtspResponse describe(@NonNull String uri, @NonNull String session) throws IOException {
        return sendCommand("DESCRIBE", session, uri,
                "Accept", "application/sdp");
    }

    public RtspResponse play(@NonNull String session) throws IOException {
        return play(uri, session);
    }

    public RtspResponse play(@NonNull String session, long startTimeMs) throws IOException {
        return play(uri, session, startTimeMs);
    }

    public RtspResponse play(@NonNull String uri, @NonNull String session) throws IOException {
        return play(uri, session, 0);
    }

    public RtspResponse play(@NonNull String uri, @NonNull String session, long startTimeMs) throws IOException {
        double startTimeSec = startTimeMs / 1000.0;
        return sendCommand("PLAY", Objects.requireNonNull(session), uri,
                "Range", "npt=" + startTimeSec + "-");
    }

    public RtspResponse pause(@NonNull String uri, @NonNull String session) throws IOException {
        return sendCommand("PAUSE", Objects.requireNonNull(session), uri);
    }

    public RtspResponse teardown(@NonNull String uri, @NonNull String session) throws IOException {
        return sendCommand("TEARDOWN", session, uri);
    }

    public RtspResponse getParameter() throws IOException {
        return getParameter(uri);
    }

    public RtspResponse getParameter(@NonNull String uri) throws IOException {
        return getParameter(uri, session);
    }

    public RtspResponse getParameter(@NonNull String uri, @Nullable String session) throws IOException {
        return sendCommand("GET_PARAMETER", session, uri);
    }

    public long getCSeq() {
        return cSeq.incrementAndGet();
    }

    private RtspResponse readResponse(@NonNull InputStream in) throws IOException {
        checkInterrupted();

        ResponseHeader header = readResponseHeader(in);
        int cl = RtspUtil.getHeaderContentLength(header.headers);
        byte[] content = new byte[Math.max(0, cl)];

        if(cl > 0) {
            int read = in.read(content);
            if(read != cl) {
                throw new IOException("Unexpected end of stream");
            }
        }

        session = header.headers.get("Session");

        return new RtspResponse(header, content);
    }

    private RtspResponse sendCommandBasic(@NonNull String command, @Nullable String session,
                                          @Nullable String authToken, @NonNull String uri, boolean waitForResponse,
                                          @NonNull Object... headers) throws IOException {

        checkInterrupted();

        StringBuilder sb = new StringBuilder();

        sb.append(command).append(" ").append(uri).append(" RTSP/1.0").append(CRLF);

        if(authToken != null) {
            sb.append("Authorization: ").append(authToken).append(CRLF);
        }

        if(userAgent != null) {
            sb.append("User-Agent: ").append(userAgent).append(CRLF);
        }

        sb.append("CSeq: ").append(cSeq.incrementAndGet()).append(CRLF);

        if (session != null) {
            sb.append("Session: ").append(session).append(CRLF);
        }

        for(int i = 0; i < headers.length; i += 2) {
            checkInterrupted();

            String key = String.valueOf(headers[i]);

            if(key.equals("null") || key.equalsIgnoreCase("User-Agent") ||
                    key.equalsIgnoreCase("CSeq") ||
                    key.equalsIgnoreCase("Session")) {
                continue;
            }

            sb.append(headers[i]).append(": ").append(headers[i + 1]).append(CRLF);
        }

        out.write(sb.toString().getBytes());
        out.flush();

        if (waitForResponse) {
            return readResponse(in);
        } else {
            return null;
        }
    }

    public RtspResponse sendCommand(@NonNull String command, @Nullable String session, @NonNull String uri,
                                    @NonNull Object... headers) throws IOException {

        //Try with current auth token
        RtspResponse res = sendCommandBasic(command, session, authToken, uri, true, headers);

        //If auth is ok, return
        if(Objects.requireNonNull(res).header.code != 401) {
            return res;
        }

        //Try with auth
        Authentication auth = this.auth;

        //If auth is null, return
        if(auth == null) {
            return res;
        }

        Credentials credentials = auth.credentials;
        RealmNonce rn = getHeaderWwwAuthenticateDigestRealmAndNonce(res.header.headers);

        if(rn == null) {
            String basicRealm = getHeaderWwwAuthenticateBasicRealm(res.header.headers);
            if (TextUtils.isEmpty(basicRealm)) {
                throw new IOException("Unknown authentication type");
            }
            // Basic auth
            authToken = getBasicAuthHeader(credentials);
        } else {
            // Digest auth
            authToken = RtspUtil.getDigestAuthHeader(credentials, command, uri, rn);
        }

        Integer capabilities = RtspUtil.getSupportedCapabilities(res.getHeaders());

        if(capabilities != null) {
            this.capabilities = capabilities;
        }

        //Try with new auth token
        return sendCommandBasic(command, session, authToken, uri, true, headers);
    }

    public RtspResponse sendCommand(@NonNull String command, @NonNull String uri,
                                    @NonNull Object... headers) throws IOException {
        return sendCommand(command, session, uri, headers);
    }

}
