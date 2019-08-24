package com.cpr.lib_camera2.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;

import com.bumptech.glide.Glide;
import com.cpr.lib_camera2.R;
import com.cpr.lib_camera2.adapter.AdapterImage;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private GridView gridView;
    private AdapterImage adapterImage;
    private List<String> list;
    public static String nameFolder = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        gridView = findViewById(R.id.gridView);
        Intent intent = getIntent();
        nameFolder = intent.getStringExtra("NameFolder");
        list = new ArrayList<>();
        list = getListImageByFolder("/storage/emulated/0/" + nameFolder);
        adapterImage = new AdapterImage(list, this);
        Log.e("TAG", "size " + list.size());
        gridView.setAdapter(adapterImage);
        adapterImage.notifyDataSetChanged();


        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String path = list.get(position);
                showImage(path);
            }
        });
    }

    public List<String> getListImageByFolder(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles();
        List<String> list = new ArrayList<>();
        Log.d("Files", "Size: " + files.length);
        for (int i = 0; i < files.length; i++) {
            list.add(files[i].getAbsolutePath());
//            Image image = new Image("",0.0, files[i].getAbsolutePath());
        }

        return list;
    }

    @Override
    public void onBackPressed() {
        finish();

    }

    private void showImage(String path) {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.library_anim_gallery_activity);
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(R.layout.dialog_show_image);
        PhotoView photoView = dialog.findViewById(R.id.photoView);
        Glide.with(this).load(path).into(photoView);
        dialog.setCanceledOnTouchOutside(true);
        photoView.startAnimation(animation);
        dialog.show();
    }
}