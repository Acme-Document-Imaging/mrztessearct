package com.cbms.tesseractdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Camera2RawActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_camera2_raw);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2RawFragment.newInstance())
                    .commit();
        }
    }
}
