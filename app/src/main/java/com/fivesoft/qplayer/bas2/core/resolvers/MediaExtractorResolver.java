package com.fivesoft.qplayer.bas2.core.resolvers;

import androidx.annotation.NonNull;

import com.fivesoft.qplayer.bas2.MediaExtractor;
import com.fivesoft.qplayer.bas2.resolver.Creator;
import com.fivesoft.qplayer.bas2.resolver.Resolver;

public class MediaExtractorResolver extends Resolver<MediaExtractor.Descriptor, MediaExtractor> {

    private static final MediaExtractorResolver INSTANCE = new MediaExtractorResolver();

    public static MediaExtractorResolver getInstance() {
        return INSTANCE;
    }

    private MediaExtractorResolver() {
        //Prevent instantiation
    }

    public MediaExtractorResolver registerCreator(@NonNull Creator<MediaExtractor.Descriptor, MediaExtractor> creator) {
        register(creator);
        return this;
    }

    public static MediaExtractor resolveExtractor(@NonNull MediaExtractor.Descriptor descriptor) {
        return getInstance().resolve(descriptor);
    }

}