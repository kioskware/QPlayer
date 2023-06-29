package com.fivesoft.qplayer;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.fivesoft.qplayer.impl.mediasource.rtsp.RTSPMediaSource;

import org.jetbrains.annotations.Nullable;

public final class MainActivity extends AppCompatActivity {

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(() -> {

            RTSPMediaSource source = new RTSPMediaSource("rtsp://192.168.88.59:554/mainstream");

            source.setListener("test", packet -> {
                Log.println(Log.ASSERT, "test", "data available: " + packet.length());
            });

            Log.println(Log.ASSERT, "test", "start: " + source.start());

            source.start();

        }).start();

    }

}
