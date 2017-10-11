package com.sanousun.wheel.sample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by dashu on 2017/10/10.
 * 滚轮条目的适配器
 */

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemHolder> {

    private Context mContext;
    private List<String> mDataList;

    public ItemAdapter(Context context, List<String> dataList) {
        mContext = context;
        mDataList = dataList;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_item, parent, false);
        return new ItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        holder.mItemTxt.setText(mDataList.get(position));
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    static class ItemHolder extends RecyclerView.ViewHolder {

        TextView mItemTxt;

        ItemHolder(View itemView) {
            super(itemView);
            mItemTxt = itemView.findViewById(R.id.tv_item);
        }
    }
}
