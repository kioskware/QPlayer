package com.fivesoft.qplayer;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fivesoft.qplayer.bas2.Authentication;
import com.fivesoft.qplayer.bas2.Frame;
import com.fivesoft.qplayer.bas2.Sample;
import com.fivesoft.qplayer.bas2.common.ByteUtil;
import com.fivesoft.qplayer.bas2.common.StackTraceUtil;
import com.fivesoft.qplayer.bas2.core.MediaDecoderThread;
import com.fivesoft.qplayer.bas2.decoder.MediaDecoder;
import com.fivesoft.qplayer.bas2.impl.decoder.video.h264.H264Decoder;
import com.fivesoft.qplayer.bas2.impl.decoder.video.h264.VideoRtpParser;
import com.fivesoft.qplayer.bas2.impl.extractor.rtsp.RtspMediaExtractor;
import com.fivesoft.qplayer.bas2.impl.source.SocketDataSource;
import com.fivesoft.qplayer.buffer.Buffer;
import com.fivesoft.qplayer.buffer.Bufferable;
import com.fivesoft.qplayer.common.ByteArray;
import com.fivesoft.qplayer.track.VideoTrack;

import org.jetbrains.annotations.Nullable;

import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicReference;

public final class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static MainActivity activity;

    private SurfaceView surfaceView;
    private boolean prepareOnly = false;

    private volatile int surfaceWidth = 1920;
    private volatile int surfaceHeight = 1080;

    private boolean firstStart = true;

    private H264Decoder decoder;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        surfaceView = findViewById(R.id.svVideo);
        surfaceView.getHolder().addCallback(this);

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (firstStart) {
            startDecoding(holder.getSurface(), surfaceWidth, surfaceHeight);
            firstStart = false;
        } else {
            if (decoder != null) {
                decoder.setOutput(holder.getSurface());
            }
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    private void startDecoding(Surface surface, int width, int height){
        new Thread(() -> {

            Uri uri = Uri.parse("rtsp://192.168.88.59:554/mainstream");

            try(SocketDataSource dataSource = new SocketDataSource(uri.getHost(), uri.getPort());
                RtspMediaExtractor extractor = new RtspMediaExtractor(dataSource, uri.toString(), false, true, false)){

                dataSource.connect();
                extractor.setAuthentication(new Authentication("admin", "admin"));
                extractor.prepare(5000);

                VideoTrack videoTrack = extractor.getTracks().getVideoTrack();

                if (videoTrack != null) {
//                    Log.println(Log.ASSERT, "QPlayer", "csd-0" +
//                            ByteUtil.toReadableBytes(videoTrack.getCsd().getCsd(0)));
//                    Log.println(Log.ASSERT, "QPlayer", "csd-1" +
//                            ByteUtil.toReadableBytes(videoTrack.getCsd().getCsd(1)));
                }

                if(videoTrack == null || prepareOnly){
                    Log.println(Log.ASSERT, "QPlayer", "Video track is null");
                    return;
                }

                decoder = new H264Decoder(videoTrack, extractor.getSampleFormat());

                decoder.setOutput(surface);
                decoder.setCsd(videoTrack.getCsd());

                AtomicReference<MediaDecoderThread> decoderThread = new AtomicReference<>();
                decoderThread.set(new MediaDecoderThread(decoder) {

                    @Override
                    public void onThreadInterrupted(@NonNull MediaDecoder<?, ?> decoder) {

                    }

                    @Override
                    public void onThreadException(@NonNull MediaDecoder<?, ?> decoder, @NonNull Exception e) {
                        Log.println(Log.ASSERT, "MediaDecoderThread", "Exception in decoder thread: \n\n" +
                                StackTraceUtil.getStackTrace(e) + "\n\n");
                    }
                });

                videoTrack.setWidth(1920);
                videoTrack.setHeight(1080);

                decoderThread.get().start();

                Buffer<Frame> buffer = new Buffer<Frame>(){
                    @Override
                    protected void onFrame(Frame frame) {
                        try {
                            decoderThread.get().feed(frame);
                        } catch (InterruptedIOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

                buffer.setLatency(200);

                for(;;){
                    Sample sample = extractor.nextSample();

                    if(sample == null) {
                        Log.println(Log.ASSERT, "QPlayer", "Sample is null");
                        break;
                    }

                    if (sample.track instanceof VideoTrack) {

                        //Log.println(Log.ASSERT, "H264Decoder", "sample read: " + sample.timestamp);

//                        byte[] nalUnit = parser.processRtpPacketAndGetNalUnit(sample.getArray(), sample.getLength());
//
//                        if(nalUnit != null){
//                            Frame frame = decoder.feed(new Sample(nalUnit, sample.timestamp, sample.track));
//                            if(frame != null){
//                                decoderThread.feed(frame);
//                            }
//                        }

                            Frame frame = decoder.feed(sample);
                            if(frame != null){
                                buffer.receive(frame);
                            }

                    }

//                    Log.println(Log.ASSERT, "QPlayer", "Sample: timestamp=" + sample.timestamp +
//                            ", size=" + sample.data.getLength() + " payload=" + sample.track.getPayloadType());
                }

                Log.println(Log.ASSERT, "QPlayer", "Done");


            } catch (Exception e) {
                Log.println(Log.ASSERT, "QPlayer", "Error: " + e);
                e.printStackTrace();
            }

        }).start();
    }

}
