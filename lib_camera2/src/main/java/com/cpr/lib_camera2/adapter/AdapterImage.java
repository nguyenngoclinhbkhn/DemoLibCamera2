package com.cpr.lib_camera2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cpr.lib_camera2.R;

import java.util.List;

public class AdapterImage extends BaseAdapter {
    private List<String> list;
    private Context context;
    private LayoutInflater inflater;

    public AdapterImage(List<String> list, Context context) {
        this.list = list;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Animation a = AnimationUtils.loadAnimation(context, R.anim.library_anim_gallery_activity);
        ViewHolder viewHolder;
        if (convertView == null){
            convertView = inflater.inflate(R.layout.item_image, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.imgPhoto);
            viewHolder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String image = list.get(position);
        int width = parent.getMeasuredWidth();
        viewHolder.imageView.startAnimation(a);
        convertView.startAnimation(a);
        convertView.getLayoutParams().width = width / 3;
        convertView.getLayoutParams().height = width / 3;
        convertView.requestLayout();
        Glide.with(context).load(image).into(viewHolder.imageView);
        return convertView;
    }

    private class ViewHolder{
        private ImageView imageView;
    }
}
