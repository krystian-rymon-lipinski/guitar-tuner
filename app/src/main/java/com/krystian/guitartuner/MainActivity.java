package com.krystian.guitartuner;

import android.media.AudioRecord;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button tuneButton;
    SignalProcessing signal;

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

        whichStringText = (TextView) findViewById(R.id.which_string_text);
        detuningValueText = (TextView) findViewById(R.id.detuning_text);
        arrowText = (TextView) findViewById(R.id.arrow);
        tuneButton = (Button) findViewById(R.id.start_tuning);

        e1String = findViewById(R.id.e1_string);
        hString = findViewById(R.id.h_string);
        gString = findViewById(R.id.g_string);
        dString = findViewById(R.id.d_string);
        AString = findViewById(R.id.A_string);
        EString = findViewById(R.id.E_string);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.start_tuning:
                signal.startRecording();
                tune();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void tune() {

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (signal.isSound()) {//if there is a sound loud enough -> start tuning
                    signal.record();

                }
                else {
                    e1String.setBackgroundColor(getResources().getColor(R.color.stringView));
                    hString.setBackgroundColor(getResources().getColor(R.color.stringView));
                    gString.setBackgroundColor(getResources().getColor(R.color.stringView));
                    dString.setBackgroundColor(getResources().getColor(R.color.stringView));
                    AString.setBackgroundColor(getResources().getColor(R.color.stringView));
                    EString.setBackgroundColor(getResources().getColor(R.color.stringView));

                    whichStringText.setTextColor(getResources().getColor(R.color.guitarNeck));
                }

                handler.postDelayed(this, 50);
            }
        });

    }

/*
    @Override
    protected void onPause() {
        super.onPause();
        if(signal.getState() == AudioRecord.STATE_INITIALIZED) signal.stopRecording();
    }
*/
}
