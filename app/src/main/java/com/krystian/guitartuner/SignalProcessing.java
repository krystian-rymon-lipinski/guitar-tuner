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
    public static final int THRESHOLD = 600; /*start calculating Fourier transform if a sound is louder
                                                than threshold (actually it's a mic value) */

    public SignalProcessing() {
    }

    public int getState() {
        return AUDIO_RECORD.getState();
    }

    public void startRecording() {
        AUDIO_RECORD.startRecording();
    }

    public short listen() {
        short[] listeningBuffer = new short[32];
        short maxValue = 0;
        Log.v("State", "Listening");
        AUDIO_RECORD.read(listeningBuffer, 0, listeningBuffer.length);
        for(short buff : listeningBuffer) {
            if(buff > maxValue) maxValue = buff;
        }
        Log.v("Max", ""+maxValue);
        return maxValue;
    }

    public void record() {

        int bufferSize = 441;
        int numberOfBufferChunks = 2;
        short[] signal = new short[bufferSize*numberOfBufferChunks];
        Log.v("RECORDING!", "Fuck!");
        for(int i=0; i<numberOfBufferChunks; i++) {
            AUDIO_RECORD.read(signal, i*bufferSize, bufferSize);
        }
        Log.v("RECORDING!", "Fuck!");
        calculateDFT(signal);
    }

    public double[] calculateDFT(short[] signal) {
        Log.v("RECORDING!", "Fuck!");
        double start = System.currentTimeMillis();
        int minFourier = 0, maxFourier = 400; //there's nothing interesting above 400 Hz - last string's frequency is 330 Hz
        double[] fourier = new double[maxFourier - minFourier];
        /*
        double[] signal = new double[SAMPLE_RATE];
        double[] t = new double[signal.length];
        double f1 = 300;
        for(int i=0; i<t.length; i++) {
            t[i] = (double) i / SAMPLE_RATE;
            signal[i] = 1*Math.sin(2*Math.PI*f1*t[i]); //signal
        }
        */

        int numberOfCopies = SAMPLE_RATE / signal.length; //how many copies of this signal to add to dft in one multiplying


        double max = 0;
        for(int k=minFourier; k<maxFourier; k++) { //number of fourier lines
            double re = 0;
            double im = 0;
            for(int n=0; n<signal.length; n++) { //number of samples for one fourier line
                double fi = -2*Math.PI*n*k / signal.length; //imaginary part of a |z|exp(-i*fi) complex number
                re += signal[n] * Math.cos(fi); //sum of n 'a' values in a+bi complex number;
                im += signal[n] * Math.sin(fi); //sum of n 'b' values in a+bi complex number;
            }
            fourier[k] = Math.sqrt(re*re + im*im);
            Log.v("Fourier", "Value ["+k+"]: "+fourier[k]);
            if(fourier[k] > max) max = k;
        }
        double stop = System.currentTimeMillis();
        double time = stop - start;
        Log.v("DFT time", ""+time);
        Log.v("Max amplitude", ""+max);
        return fourier;
    }

    public void stopRecording() {
        AUDIO_RECORD.stop();
        AUDIO_RECORD.release();
    }
}


