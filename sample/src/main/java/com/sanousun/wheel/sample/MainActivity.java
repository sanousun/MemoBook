package com.sanousun.wheel.sample;

import android.content.Intent;
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
        mWheelView = findViewById(R.id.view_wheel);
        mWheelText = findViewById(R.id.tv_wheel);
        mWheelView.setOnWheelChangeListener((index, object) ->
                mWheelText.setText("index：" + index + "，string：" + object.toString()));
        List<Object> data = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            data.add(String.valueOf(i));
        }
        mWheelView.setData(data);
        findViewById(R.id.btn_skip).setOnClickListener(view -> startActivity(new Intent(this, SecondActivity.class)));
    }
}
