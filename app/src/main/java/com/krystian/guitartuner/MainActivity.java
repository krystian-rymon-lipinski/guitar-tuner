package com.krystian.guitartuner;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button tuneButton;
    SignalProcessing signal;
    Handler handler;
    boolean isRecording = false;

    private TextView whichStringText;
    private TextView detuningValueText;
    private TextView arrowText;
    private View e1String, hString, gString, dString, AString, EString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tuneButton = findViewById(R.id.start_tuning);
        tuneButton.setOnClickListener(this);
        signal = new SignalProcessing();
        handler = new Handler();

        whichStringText =  findViewById(R.id.which_string_text);
        detuningValueText =  findViewById(R.id.detuning_text);
        arrowText = findViewById(R.id.arrow);
        tuneButton = findViewById(R.id.start_tuning);

        e1String = findViewById(R.id.e1_string);
        hString = findViewById(R.id.h_string);
        gString = findViewById(R.id.g_string);
        dString = findViewById(R.id.d_string);
        AString = findViewById(R.id.A_string);
        EString = findViewById(R.id.E_string);

        whichStringText.setText(getString(R.string.which_string_text, WhichString.none));
        detuningValueText.setText(getString(R.string.detuning_value, 0));
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.start_tuning:
                tuneButton.setVisibility(View.INVISIBLE);
                isRecording = true;
                signal.startRecording();
                tune();
                break;
        }
    }

    public void tune() {
        handler.post(new Runnable() {
            @Override
            public void run() {

            boolean calculated = false;
            while (!calculated) {
                if(signal.isSound()) { //signal is loud enough
                    calculated = true;
                    short[] sig = signal.record(); //get samples of a signal to transform
                    Object[] fourierData = signal.calculateDFT(sig);
                    double[] fourier = (double[]) fourierData[0]; //transformed signal
                    int maxFreq = (int) fourierData[1];
                    int peaks[] = signal.detectPeaks(fourier);
                    WhichString string = signal.detectString(maxFreq, peaks);
                    int detuning = signal.tune(string, maxFreq, peaks); //the difference between ideal and calculated value (in Hz)
                    showResults(string, detuning);
                } else {
                    e1String.setBackgroundColor(getResources().getColor(R.color.stringView));
                    hString.setBackgroundColor(getResources().getColor(R.color.stringView));
                    gString.setBackgroundColor(getResources().getColor(R.color.stringView));
                    dString.setBackgroundColor(getResources().getColor(R.color.stringView));
                    AString.setBackgroundColor(getResources().getColor(R.color.stringView));
                    EString.setBackgroundColor(getResources().getColor(R.color.stringView));

                    whichStringText.setTextColor(getResources().getColor(R.color.guitarNeck));
                }
            }

            if(isRecording) handler.postDelayed(this, 50);
            else handler.removeCallbacks(this);
            }
        });
    }

    public void showResults(WhichString string, int detuning) {
        switch(string) {
            case e1:
                e1String.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case h:
                hString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case g:
                gString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case d:
                dString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case A:
                AString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case E:
                EString.setBackgroundColor(getResources().getColor(R.color.buttonGradientEnd));
                break;
            case none:
                detuning = 0;
        }

        if(detuning>0) arrowText.setText(getString(R.string.arrow_up));
        else if(detuning < 0) arrowText.setText(getString(R.string.arrow_down));
        else arrowText.setText("");

        detuning = Math.abs(detuning);

        whichStringText.setText(getString(R.string.which_string_text, string));
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
    }


    @Override
    protected void onPause() {
        super.onPause();
        signal.stopRecording();
        isRecording = false; //cancel Handler messages
    }

    @Override
    protected void onResume() {
        super.onResume();
        tuneButton.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        signal.releaseRecorder();
    }
}
