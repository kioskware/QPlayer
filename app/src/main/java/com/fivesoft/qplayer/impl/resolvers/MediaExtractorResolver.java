package com.fivesoft.qplayer.impl.resolvers;

import com.fivesoft.qplayer.base.QMediaExtractor;
import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.resolver.Resolver;

public class MediaExtractorResolver extends Resolver<ByteArray, QMediaExtractor> {

    private final static MediaExtractorResolver instance = new MediaExtractorResolver();

    private MediaExtractorResolver() {
    }

    public static MediaExtractorResolver getDefault() {
        return instance;
    }



}
