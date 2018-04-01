package com.sanousun.wheel.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dashu
 * @date 2017/10/10.
 * 使用recyclerView的方式去实现滚轮
 */

public class SecondActivity extends AppCompatActivity {

    private RecyclerView mWheelRecycler;
    private WheelLayoutManager mWheelLayoutManager;

    private EditText mJmpEdit;
    private Button mJmpBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        mWheelRecycler = findViewById(R.id.recycler_wheel);
        mWheelLayoutManager = new WheelLayoutManager();
        mWheelRecycler.setLayoutManager(mWheelLayoutManager);
        List<String> dataSource = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            dataSource.add("我是item " + i);
        }
        ItemAdapter adapter = new ItemAdapter(this, dataSource);
        mWheelRecycler.setAdapter(adapter);
        mJmpEdit = findViewById(R.id.et_jmp);
        mJmpBtn = findViewById(R.id.btn_jmp);
        mJmpBtn.setOnClickListener(v ->
                mWheelRecycler.scrollToPosition(Integer.valueOf(mJmpEdit.getText().toString())));
    }
}
