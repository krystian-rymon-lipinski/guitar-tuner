package com.krystian.guitartuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class SignalProcessing {

    private static final int SAMPLE_RATE = 44100; //Hz
    private static final int MIN_BUFFER = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    private static final AudioRecord AUDIO_RECORD = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, MIN_BUFFER);

    private short[] buffer = new short[256];
    private short[] fullTable = new short[8000]; //

    public SignalProcessing() {
    }

    public void record() {
        int numberOfChunks = fullTable.length / buffer.length;
        AUDIO_RECORD.startRecording();



        for(int i=0; i<numberOfChunks; i++) {
            AUDIO_RECORD.read(buffer, 0, buffer.length);
            for(int j=0; j<buffer.length; j++) {
                fullTable[i*buffer.length+j] = buffer[j];
            }
        }
        for(int i=0; i<fullTable.length; i++) {
            if(fullTable[i] > 0)
                Log.v("Array", "Array["+i+"]"+fullTable[i]);
        }
    }

    public void calculateDFT() {

    }
}


