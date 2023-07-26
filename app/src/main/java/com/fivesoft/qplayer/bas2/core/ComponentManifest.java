package com.fivesoft.qplayer.bas2.core;

import com.fivesoft.qplayer.bas2.DataSource;
import com.fivesoft.qplayer.bas2.MediaExtractor;
import com.fivesoft.qplayer.bas2.core.resolvers.DataSourceResolver;
import com.fivesoft.qplayer.bas2.core.resolvers.MediaExtractorResolver;
import com.fivesoft.qplayer.bas2.core.resolvers.VideoDecoderResolver;
import com.fivesoft.qplayer.bas2.decoder.MediaDecoder;
import com.fivesoft.qplayer.bas2.impl.decoder.video.h264.H264Decoder;
import com.fivesoft.qplayer.bas2.impl.extractor.rtsp.RtspMediaExtractor;
import com.fivesoft.qplayer.bas2.impl.source.FileDataSource;
import com.fivesoft.qplayer.bas2.impl.source.SocketDataSource;

/**
 * This class is used to register all components of the api like:
 * <ul>
 *     <li>{@link DataSource}s</li>
 *     <li>{@link MediaExtractor}s</li>
 *     <li>{@link MediaDecoder}s</li>
 *     </ul>
 */

class ComponentManifest {

    private ComponentManifest() {
        //Prevent instantiation
    }

    /*
     * All component's creators must be registered here in static block.
     */

    static {
        //Register sources
        DataSourceResolver.getInstance()
                .registerCreator(SocketDataSource.CREATOR)
                .registerCreator(FileDataSource.CREATOR);

        //Register extractors
        MediaExtractorResolver.getInstance()
                .registerCreator(RtspMediaExtractor.CREATOR);

        //Register decoders
        VideoDecoderResolver.getInstance()
                .register(H264Decoder.CREATOR);
    }

    public static void main(String[] args) {
        //Do nothing
    }

}
