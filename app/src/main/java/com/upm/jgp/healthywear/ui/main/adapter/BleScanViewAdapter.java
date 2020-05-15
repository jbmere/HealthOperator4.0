package com.upm.jgp.healthywear.ui.main.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.inuker.bluetooth.library.search.SearchResult;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.activity.MainActivity;

import java.util.List;


/**
 * Bluetooth scanViewAdapter used for the Smartband device. It shows a star next to the Mac if the device is stored as favourite
 * Created by timaimee on 2016/7/25.
 * Modified by yoryidan 2020
 */
public class BleScanViewAdapter extends RecyclerView.Adapter<BleScanViewAdapter.NormalTextViewHolder> {
    private final LayoutInflater mLayoutInflater;
    List<SearchResult> itemData;
    OnRecycleViewClickCallback mBleCallback;

    public BleScanViewAdapter(Context context, List<SearchResult> data) {
        this.itemData = data;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public NormalTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NormalTextViewHolder(mLayoutInflater.inflate(R.layout.adapter_item_main, parent, false));
    }

    @Override
    public void onBindViewHolder(NormalTextViewHolder holder, int position) {
        String mac = itemData.get(position).getAddress();
        //holder.mBleRssi.setText(itemData.get(position).getName() + "-" + itemData.get(position).getAddress() + "-" + itemData.get(position).rssi);
        //Checks if the MAC is stored on the favourite devices' list before adding it in to the list.
        if (MainActivity.checkFavouriteDevice(mac)) {
            holder.mBleRssi.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, android.R.drawable.star_big_on, 0);
            holder.mBleRssi.setCompoundDrawablePadding(25);
        }
        holder.mBleRssi.setText("MAC: " + itemData.get(position).getAddress());
    }


    @Override
    public int getItemCount() {
        return itemData == null ? 0 : itemData.size();
    }


    public void setBleItemOnclick(OnRecycleViewClickCallback bleCallback) {
        this.mBleCallback = bleCallback;
    }


    public class NormalTextViewHolder extends RecyclerView.ViewHolder {

        TextView mBleRssi;


        NormalTextViewHolder(View view) {
            super(view);
            mBleRssi = (TextView) view.findViewById(R.id.tv);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBleCallback.OnRecycleViewClick(getPosition());
                    Log.d("NormalTextViewHolder", "onClick--> position = " + getPosition());
                }
            });
        }
    }
}