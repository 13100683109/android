package com.example.a22749.myapplication;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);

        final AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioManager.setMicrophoneMute(false);
                audioManager.setSpeakerphoneOn(true);//使用扬声器外放，即使已经插入耳机
                setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
                audioManager.setMode(AudioManager.STREAM_MUSIC);

                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setSpeakerphoneOn(true);

                TextView textView = findViewById(R.id.textview);
                textView.setText("已开启外音");
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioManager.setMicrophoneMute(false);
                audioManager.setSpeakerphoneOn(true);//使用扬声器外放，即使已经插入耳机
                setVolumeControlStream(AudioManager.STREAM_SYSTEM);//控制声音的大小
                audioManager.setMode(AudioManager.MODE_NORMAL);

                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setSpeakerphoneOn(false);
                TextView textView = findViewById(R.id.textview);
                textView.setText("已开启耳机模式");
            }
        });

    }





}
