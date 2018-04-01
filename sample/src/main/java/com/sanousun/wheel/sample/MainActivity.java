package com.sanousun.wheel.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.sanousun.wheel.WheelBean;
import com.sanousun.wheel.ScrollWheelView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dashu
 * @date 2017/10/10
 * 主入口
 */
public class MainActivity extends AppCompatActivity {

    ScrollWheelView mWheelView;
    TextView mWheelText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWheelView = findViewById(R.id.view_wheel);
        mWheelText = findViewById(R.id.tv_wheel);
        mWheelView.setOnWheelChangeListener((index, wheelBean) ->
                mWheelText.setText("index：" + index + "，wheelBean：" + wheelBean.getShowText()));
        List<WheelBean> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add(new WheelDao("我是item " + i));
        }
        mWheelView.setData(data);
        findViewById(R.id.btn_skip).setOnClickListener(view -> startActivity(new Intent(this, SecondActivity.class)));
    }

    public static class WheelDao implements WheelBean {

        private String text;

        public WheelDao(String text) {
            this.text = text;
        }

        @Override
        public String getShowText() {
            return text;
        }
    }
}
