package com.krystian.guitartuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class SignalProcessing {

    private static final int SAMPLE_RATE = 8000; //Hz
    private static final int MIN_BUFFER = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    private static final AudioRecord AUDIO_RECORD = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, MIN_BUFFER);
    private static final int THRESHOLD = 500; /*start calculating Fourier transform if a sound is louder
                                                than threshold; in 16-bit encoding  */

    public SignalProcessing() {
    }

    public int getState() {
        return AUDIO_RECORD.getState();
    }

    public void startRecording() {
        AUDIO_RECORD.startRecording();
    }

    public boolean isSound() {
        short[] listeningBuffer = new short[32];
        short maxValue = 0;
        AUDIO_RECORD.read(listeningBuffer, 0, listeningBuffer.length);
        for(short buff : listeningBuffer) {
            if(Math.abs(buff) > maxValue) maxValue = (short) Math.abs(buff);
        }
        return maxValue > THRESHOLD;
    }

    public void record() {

        short[] buffer = new short[100]; //small chunk - no risk to get zeros in 300+ elements
        int numberOfBufferChunks = 10; //1000 samples of a signal is enough (given 8k frequency rate) to transform it properly
        short[] signal = new short[buffer.length*numberOfBufferChunks];
        for(int i=0; i<numberOfBufferChunks; i++) {
            AUDIO_RECORD.read(buffer, 0, buffer.length);
            System.arraycopy(buffer, 0, signal, i*100, buffer.length); //1000 samples of a signal
        }

        short[] fullSignal = new short[SAMPLE_RATE]; //8000 samples needed to get amplitude every 1 Hz
        int numberOfCopies = fullSignal.length / signal.length;
        for(int i=0; i<numberOfCopies; i++)
            System.arraycopy(signal, 0, fullSignal, 0, signal.length);
        calculateDFT(fullSignal);
    }

    public void calculateDFT(short[] signal) {
        int minFourier = 0, maxFourier = 400; //there's nothing interesting above 400 Hz - last string's frequency is 330 Hz
        double[] fourier = new double[maxFourier - minFourier];
/*
        double[] signal = new double[SAMPLE_RATE];  DFT works
        double[] t = new double[signal.length];
        double f1 = 300;
        for(int i=0; i<t.length; i++) {
            t[i] = (double) i / SAMPLE_RATE;
            signal[i] = 1*Math.sin(2*Math.PI*f1*t[i]); //signal
        }
*/
        double max = 0;
        int maxFrequency = 0;
        for(int k=minFourier; k<maxFourier; k++) { //number of fourier lines
            double re = 0;
            double im = 0;
            for(int n=0; n<maxFourier; n++) { //number of samples for one fourier line
                double fi = -2*Math.PI*n*k / signal.length; //imaginary part of a |z|exp(-i*fi) complex number
                re += signal[n] * Math.cos(fi); //sum of n 'a' values in a+bi complex number;
                im += signal[n] * Math.sin(fi); //sum of n 'b' values in a+bi complex number;
            }
            fourier[k] = Math.sqrt(re*re + im*im); //amplitude of k-frequency; 0 <= k <= 400
            if(fourier[k] > max) {
                max = fourier[k]; //highest amplitude yet
                maxFrequency = k; //frequency with highest amplitude yet
            }
        }

        detectPeaks(fourier, maxFrequency);
    }

    public void detectPeaks(double[] fftResult, int hpf) { //highestPeakFrequency (with highest amplitude)
        int maxFrequencyMeasured = 400;
        int[] peaks = new int[4]; //max four peaks in 0-400 Hz (main one and higher harmonics) to detect which string is it
        WhichString whichString;

        for(int k=90; k < maxFrequencyMeasured-40; k++) {
            int thisMayBePeak = 0;
            for(int i = 1; i <= 40; i++) { //peak width (to eliminate places with slightly louder noise)
                if( fftResult[k] > fftResult[k-i] && fftResult[k] > fftResult[k+i]) thisMayBePeak++;
            }
            if(thisMayBePeak == 40) {
                if(peaks[0]==0) peaks[0] = k;
                else {
                    if(peaks[1]==0) peaks[1] = k;
                    else {
                        if(peaks[2]==0) peaks[2] = k;
                        else {
                            peaks[3] = k;
                        }
                    }
                }
            }
        }
        Log.v("Peaks", ""+peaks[0]+ " "+peaks[1]+ " "+peaks[2]+" "+peaks[3]);
        if( peaks[0] > 0.9*164 && peaks[0] < 1.1*164 && peaks[1] > 0.9*246 && peaks[1] < 1.1*246)
            whichString = WhichString.E; //(M)164, (M)246, (328)
        else if( peaks[0] > 0.9*110 && peaks[0] < 1.1*110 && peaks[1] > 0.9*220 && peaks[1] < 1.1*220 && hpf < 240)
            whichString = WhichString.A; //110, M220, (330)
        else if( peaks[0] > 0.9*147 && peaks[0] < 1.1*147 && ((peaks[1] > 0.9*294 && peaks[1] < 1.1*294) || (peaks[1] > 0.9*215 && peaks[1] < 1.1*215)))
            whichString = WhichString.d; //(M)147, (220), (M)294
        else if( ( (hpf > 0.9 * 197 && hpf < 1.1*197) || (hpf > 0.9 * 2*197 && hpf < 1.1*2*197) ) && (peaks[0] > 130 || peaks[1] > 130))
            whichString = WhichString.g; //(M)196, (M)392
        else if( hpf > 0.9 * 247 && hpf < 1.1*247 && (peaks[0] > 200 || peaks[1]>200))
            whichString = WhichString.h; //(110) M247
        else if( hpf > 0.9*330 && hpf < 1.1*330)
            whichString = WhichString.e1; //(110) 220 M330
        else whichString = WhichString.none;
        tune(whichString, hpf);
    }

    public void tune(WhichString whichString, int hpf) {

        int desiredFrequency;
        int detuning = 0;
        switch(whichString) {
            case e1:
                desiredFrequency = 330;
                detuning = desiredFrequency - hpf;
                //e1String.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case h:
                desiredFrequency = 247;
                detuning = desiredFrequency - hpf;
                //hString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case g:
                desiredFrequency = 197;
                if(hpf <240) detuning = desiredFrequency - hpf;
                else detuning = desiredFrequency - hpf/2;
                //gString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case d:
                desiredFrequency = 147;
                //detuning = desiredFrequency - peaks[0];
                //dString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case A:
                desiredFrequency = 110;
                //detuning = desiredFrequency - peaks[0];
                //AString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case E:
                desiredFrequency = 82;
                //detuning = desiredFrequency - peaks[0]/2;
                //EString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case none:
                desiredFrequency = 0;
                detuning = 0;
        }

        Log.v("Which string", ""+whichString);
        Log.v("Detuning", ""+detuning);
/*
        if(detuning>0) arrowText.setText(getString(R.string.arrow_up));//strzałka w górę
        else if(detuning < 0) arrowText.setText(getString(R.string.arrow_down));//strzałka w dół
        else arrowText.setText("");

        detuning = Math.abs(detuning);

        whichStringText.setText(getString(R.string.which_string_text, whichString));
        whichStringText.setTextColor(getResources().getColor(R.color.buttonGradientEnd));
        detuningValueText.setText(getString(R.string.detuning_value, detuning));
        switch(detuning) {
            case 0:
                detuningValueText.setTextColor(getResources().getColor(R.color.detuning_0));
                break;
            case 1:
            case 2:
                detuningValueText.setTextColor(getResources().getColor(R.color.detuning_2));
                break;
            case 3:
            case 4:
                detuningValueText.setTextColor(getResources().getColor(R.color.detuning_4));
                break;
            case 5:
            case 6:
                detuningValueText.setTextColor(getResources().getColor(R.color.detuning_6));
                break;
            case 7:
            case 8:
            default:
                detuningValueText.setTextColor(getResources().getColor(R.color.detuning_8));
                break;
        }
*/
    }

    public void stopRecording() {
        AUDIO_RECORD.stop();
        AUDIO_RECORD.release();
    }
}


