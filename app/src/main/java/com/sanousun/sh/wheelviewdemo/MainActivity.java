package com.sanousun.sh.wheelviewdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    WView mWView;
    TextView mWText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWView = (WView) findViewById(R.id.view_wheel);
        mWText = (TextView) findViewById(R.id.tv_wheel);
        mWView.setOnWheelChangeListener(new WView.OnWheelChangeListener() {
            @Override
            public void onWheelChange(int index, Object object) {
                mWText.setText("index：" + index + "，string：" + object.toString());
            }
        });
        List<Object> data = new ArrayList<>();
        data.add("10-20台");
        data.add("20-30台");
        data.add("30-40台");
        data.add("40-50台");
        data.add("50-60台");
        data.add("70-80台");
        data.add("80-90台");
        data.add("100-110台");
        data.add("110台以上");
        data.add("10-20台");
        data.add("20-30台");
        data.add("30-40台");
        data.add("40-50台");
        data.add("50-60台");
        data.add("70-80台");
        data.add("80-90台");
        data.add("100-110台");
        mWView.setData(data);

    }
}
