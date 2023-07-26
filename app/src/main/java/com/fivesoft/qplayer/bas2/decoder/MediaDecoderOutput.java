package com.fivesoft.qplayer.bas2.decoder;

import androidx.annotation.Nullable;

import com.fivesoft.qplayer.track.Track;

public class MediaDecoderOutput<RendererType> {

    @Nullable
    private volatile RendererType renderer;
    @Nullable
    private volatile BufferReceiver bufferReceiver;

    public MediaDecoderOutput(@Nullable RendererType renderer, @Nullable BufferReceiver bufferReceiver) {
        this.renderer = renderer;
        this.bufferReceiver = bufferReceiver;
    }

    public MediaDecoderOutput(@Nullable RendererType renderer) {
        this(renderer, null);
    }

    public void setRenderer(@Nullable RendererType renderer) {
        this.renderer = renderer;
    }

    public void setBufferReceiver(@Nullable BufferReceiver bufferReceiver) {
        this.bufferReceiver = bufferReceiver;
    }

    @Nullable
    public RendererType getRenderer() {
        return renderer;
    }

    @Nullable
    public BufferReceiver getBufferReceiver() {
        return bufferReceiver;
    }

    public static abstract class Creator<RendererType>
            implements com.fivesoft.qplayer.bas2.resolver.Creator<Track, RendererType> {

        @Override
        public int accept(Track t) {
            return t == null ? 0 : 1;
        }
    }

}
