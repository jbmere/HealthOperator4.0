package com.upm.jgp.healthywear.ui.main.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.DataModule.FavouriteObject;
import com.upm.jgp.healthywear.ui.main.activity.MainActivity;

import java.util.List;
import java.util.Map;

/**
 * This Activity contains the adapter to list the stored favourite devices with its MAC address and its type
 *
 * @author Jorge Garcia Paredes (yoryidan)
 * @version 175
 * @since 2020
 */
public class FavDevAdapter extends ArrayAdapter {

    public static class ViewHolder{
        ImageView iv_type;
        TextView tv_mac;
        int favDev_type = 0;
    }

    public FavDevAdapter(Context context, int textViewResourceId, List<Map.Entry<String, Object>> objects) {
        super(context, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder m_viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapter_item_fav_devices, parent, false);
            m_viewHolder = new ViewHolder();
            m_viewHolder.iv_type = convertView.findViewById(R.id.iv_favDev_type);
            m_viewHolder.tv_mac = convertView.findViewById(R.id.favDev_mac);
            convertView.setTag(m_viewHolder);
        } else {
            m_viewHolder = (ViewHolder) convertView.getTag();
        }

        Map.Entry<String, FavouriteObject> entry = (Map.Entry<String, FavouriteObject>) this.getItem(position);

        m_viewHolder.tv_mac.setText(entry.getKey());

        m_viewHolder.favDev_type = entry.getValue().getmWearableType();
        switch(m_viewHolder.favDev_type){
            case 1: //SmartBand
                m_viewHolder.iv_type.setImageDrawable(getContext().getResources().getDrawable(R.drawable.smartband_square));
                break;

            case 2: //MMR
                m_viewHolder.iv_type.setImageDrawable(getContext().getResources().getDrawable(R.drawable.mmr_square_rotated_hq));
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + m_viewHolder.favDev_type);
        }

        return convertView;
    }

    @Override
    public void remove(@Nullable Object object) {
        super.remove(object);
        MainActivity.deleteFavouriteDevice(((Map.Entry<String, FavouriteObject>) object).getKey());
    }
}
