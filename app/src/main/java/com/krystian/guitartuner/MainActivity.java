package com.krystian.guitartuner;

import android.media.AudioRecord;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button tuneButton;
    SignalProcessing signal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tuneButton = findViewById(R.id.tune_button);
        tuneButton.setOnClickListener(this);
        signal = new SignalProcessing();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.tune_button:
                tune();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void tune() {
        signal.startRecording();
        boolean isRecording = false;
        while(!isRecording) {
            if (signal.listen() > SignalProcessing.THRESHOLD) {//if there is a sound loud enough -> calculate Fourier transform
                signal.record();
                //isRecording = true;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(signal.getState() == AudioRecord.STATE_INITIALIZED) signal.stopRecording();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(signal.getState() == AudioRecord.STATE_INITIALIZED) signal.stopRecording();
    }
}
