package com.cpr.demolibcamera2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.cpr.lib_camera2.activities.CameraActivity;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {
    ImageView img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fabric.with(this, new Crashlytics());
        img = findViewById(R.id.img);
    }

    public void capture(View view) {
        startActivityForResult(new Intent(MainActivity.this, CameraActivity.class), CameraActivity.REQUEST_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CameraActivity.REQUEST_IMAGE ){
            if (resultCode == RESULT_OK){
                String path = data.getData().toString();
                img.setImageBitmap(BitmapFactory.decodeFile(path));

            }
        }
    }
}


