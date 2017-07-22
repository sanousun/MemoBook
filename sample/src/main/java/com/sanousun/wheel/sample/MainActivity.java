package com.sanousun.wheel.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.sanousun.wheel.WheelView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    WheelView mWheelView;
    TextView mWheelText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWheelView = (WheelView) findViewById(R.id.view_wheel);
        mWheelText = (TextView) findViewById(R.id.tv_wheel);
        mWheelView.setOnWheelChangeListener(new WheelView.OnWheelChangeListener() {
            @Override
            public void onWheelChange(int index, Object object) {
                mWheelText.setText("index：" + index + "，string：" + object.toString());
            }
        });
        List<Object> data = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            data.add(String.valueOf(i));
        }
        mWheelView.setData(data);
    }
}
