package com.fivesoft.qplayer.bas2.impl.extractor.rtsp;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.common.Headers;
import com.fivesoft.qplayer.common.ResponseHeader;

import java.util.Objects;

public class RtspResponse {

    @NonNull
    public final ResponseHeader header;

    @NonNull
    public final byte[] content;

    public RtspResponse(@NonNull ResponseHeader header, @NonNull byte[] content) {
        this.header = Objects.requireNonNull(header);
        this.content = Objects.requireNonNull(content);
    }

    public String getContentAsText() {
        return new String(content);
    }

    public String getHeader(String name) {
        return getHeaders().get(name);
    }

    public Headers getHeaders() {
        return header.headers;
    }

    public int getCode() {
        return header.code;
    }

    public String getMessage() {
        return header.message;
    }

    public boolean isOk() {
        return getCode() == 200;
    }

}
