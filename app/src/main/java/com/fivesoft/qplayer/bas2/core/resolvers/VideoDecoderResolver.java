package com.fivesoft.qplayer.bas2.core.resolvers;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.bas2.decoder.MediaDecoder;
import com.fivesoft.qplayer.bas2.decoder.VideoDecoder;
import com.fivesoft.qplayer.bas2.resolver.Creator;
import com.fivesoft.qplayer.bas2.resolver.Resolver;

public class VideoDecoderResolver extends Resolver<MediaDecoder.Descriptor, VideoDecoder> {

    private final static VideoDecoderResolver instance = new VideoDecoderResolver();

    private VideoDecoderResolver() {
        //Prevent instantiation
    }

    public static VideoDecoderResolver getInstance() {
        return instance;
    }

    public VideoDecoderResolver registerCreator(@NonNull Creator<MediaDecoder.Descriptor, VideoDecoder> creator) {
        register(creator);
        return this;
    }

    public static VideoDecoder resolveDecoder(@NonNull MediaDecoder.Descriptor descriptor) {
        return getInstance().resolve(descriptor);
    }

}
