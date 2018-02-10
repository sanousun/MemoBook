package com.sanousun.wheel.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dashu
 * @date 2017/10/10.
 * 使用recyclerView的方式去实现滚轮
 */

public class SecondActivity extends AppCompatActivity {

    private RecyclerView mWheelRecycler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        mWheelRecycler = findViewById(R.id.recycler_wheel);
        mWheelRecycler.setLayoutManager(new WheelLayoutManager());
        List<String> dataSource = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            dataSource.add("我是item " + i);
        }
        ItemAdapter adapter = new ItemAdapter(this, dataSource);
        mWheelRecycler.setAdapter(adapter);
        new LinearSnapHelper().attachToRecyclerView(mWheelRecycler);
    }
}
