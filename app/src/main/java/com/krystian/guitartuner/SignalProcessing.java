package com.krystian.guitartuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class SignalProcessing {

    private static final int SAMPLE_RATE = 8000; //Hz
    private static final int MIN_BUFFER = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    public static final AudioRecord AUDIO_RECORD = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, MIN_BUFFER);
    private static final int THRESHOLD = 500; /*start calculating Fourier transform if a sound is louder
                                                than threshold; in 16-bit encoding  */

    public SignalProcessing() {
    }

    public void startRecording() {
        AUDIO_RECORD.startRecording();
    }

    public boolean isSound() {
        short[] listeningBuffer = new short[32];
        AUDIO_RECORD.read(listeningBuffer, 0, listeningBuffer.length);
        for(short buff : listeningBuffer) {
            if(Math.abs(buff) > THRESHOLD) return true;
        }
        return false;
    }

    public void getRecorderState() {
        int state = AUDIO_RECORD.getState();
        //AUDIO_RECORD.getState() = AUDIO_RECORD.
        Log.v("State", String.valueOf(state));
    }

    public short[] record() {

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
        return fullSignal;
        //calculateDFT(fullSignal);
    }

    public Object[] calculateDFT(short[] signal) {

        /*
        int minFourier = 0, maxFourier = 400; //there's nothing interesting above 400 Hz - last string's frequency is 330 Hz
        double[] fourier = new double[maxFourier - minFourier];

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
        */
        int maxFrequencyMeasured = 400;
        double[][] fftArg = new double[maxFrequencyMeasured][maxFrequencyMeasured];
        double[] realisPart = new double[maxFrequencyMeasured];
        double[] imaginarisPart = new double[maxFrequencyMeasured];

        double[] fftResult = new double[maxFrequencyMeasured];
        int[] frequencies = new int[maxFrequencyMeasured];
        //peaks[0] = 0; peaks[1] = 0; peaks[2] = 0; peaks[3] = 0;
        double maxAmplitude = 0;
        int hpf = 0;
        for( int k = 0; k < maxFrequencyMeasured ; k++) {
            realisPart[k] = 0;
            imaginarisPart[k] = 0;

            for (int n = 0; n < maxFrequencyMeasured; n++) {
                fftArg[k][n] = -2 * Math.PI * k * n / signal.length; //bigBufferSize
                realisPart[k] += signal[n] * Math.cos(fftArg[k][n]);  //bigBuffer
                imaginarisPart[k] += signal[n] * Math.sin(fftArg[k][n]); //bigBuffer
            }

            fftResult[k] = Math.sqrt(realisPart[k] * realisPart[k] + imaginarisPart[k] * imaginarisPart[k]);
            frequencies[k] = k;

            //Log.v("fftResult " + k, "" + fftResult[k]);

            if (fftResult[k] > maxAmplitude) {
                maxAmplitude = fftResult[k];
                hpf = frequencies[k];
            }
        }
        return new Object[]{fftResult, hpf};
    }

    public int[] detectPeaks(double[] fftResult) { //highestPeakFrequency (frequency with highest amplitude)
        int maxFrequencyMeasured = 400;
        int[] peaks = new int[4]; //max four peaks in 0-400 Hz (main one and higher harmonics) to detect which string is it


        for (int k = 90; k < maxFrequencyMeasured - 40; k++) {
            int thisMayBePeak = 0;
            for (int i = 1; i <= 40; i++) { //peak width (to eliminate places with slightly louder noise)
                if (fftResult[k] > fftResult[k - i] && fftResult[k] > fftResult[k + i])
                    thisMayBePeak++;
            }
            if (thisMayBePeak == 40) {
                if (peaks[0] == 0) peaks[0] = k;
                else {
                    if (peaks[1] == 0) peaks[1] = k;
                    else {
                        if (peaks[2] == 0) peaks[2] = k;
                        else {
                            peaks[3] = k;
                        }
                    }
                }
            }
        }
        return peaks;
    }

    public WhichString detectString(int hpf, int[] peaks) {
        WhichString whichString;

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

        return whichString;
    }

    public int tune(WhichString whichString, int hpf, int[] peaks) {

        int desiredFrequency;
        int detuning = 0;
        switch(whichString) {
            case e1:
                desiredFrequency = 330;
                detuning = desiredFrequency - hpf;
                break;
            case h:
                desiredFrequency = 247;
                detuning = desiredFrequency - hpf;
                break;
            case g:
                desiredFrequency = 197;
                if(hpf <240) detuning = desiredFrequency - hpf;
                else detuning = desiredFrequency - hpf/2;
                break;
            case d:
                desiredFrequency = 147;
                detuning = desiredFrequency - peaks[0];
                break;
            case A:
                desiredFrequency = 110;
                detuning = desiredFrequency - peaks[0];
                break;
            case E:
                desiredFrequency = 82;
                detuning = desiredFrequency - peaks[0]/2;
                break;
            case none:
                desiredFrequency = 0;
                detuning = 0;
        }
        return detuning;
    }

    public void stopRecording() {
        AUDIO_RECORD.stop();
    }

    public void releaseRecorder() {
        AUDIO_RECORD.release();
    }
}


