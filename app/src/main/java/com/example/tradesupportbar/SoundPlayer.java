package com.example.tradesupportbar;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundPlayer {

    private static SoundPool soundPool;
    private static int upSound;
    private static int downSound;

    private AudioAttributes audioAttributes;

    public SoundPlayer(Context context) {

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);

        upSound = soundPool.load(context, R.raw.uprock, 1);
        downSound = soundPool.load(context, R.raw.downrock, 1);
    }

    public void playUpSound() {
        soundPool.play(upSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playDownSound() {
        soundPool.play(downSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }
    public void stop() {
        soundPool.autoPause();
    }
}