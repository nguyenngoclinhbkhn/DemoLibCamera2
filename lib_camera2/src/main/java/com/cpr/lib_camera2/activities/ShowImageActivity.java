package com.cpr.lib_camera2.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cpr.lib_camera2.R;
import com.cpr.lib_camera2.utils.FileUtils;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import dmax.dialog.SpotsDialog;

public class ShowImageActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView btnOk;
    private ImageView btnNo;
    private PhotoView photoView;
    private String path = "";
    private FileUtils fileUtils;
    public static String pathImage = "";
    private File imageFileFolder;
    private File imageFileName;
    private MediaScannerConnection msConn;
    private ImageView imageViewLoading;
    private String camera;
    private Bitmap bitmapSum;
    private int rotation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_image);
        btnOk = findViewById(R.id.btnOk);
        btnNo = findViewById(R.id.btnNo);

        Intent intent = getIntent();
        path = intent.getStringExtra("path");
        camera = intent.getStringExtra("camera");
        if (!TextUtils.isEmpty(intent.getStringExtra("rotation"))) {
            rotation = Integer.parseInt(intent.getStringExtra("rotation"));
            Log.e("TAG", "rotation not null " + rotation);

        } else {
            rotation = 0;
            Log.e("TAG", "rotation null" + rotation);

        }
        Log.e("TAG", "path showimage " + path);
        Log.e("TAG", "camera " + camera);
        photoView = findViewById(R.id.photoView);
        fileUtils = new FileUtils(this);
        btnOk.setOnClickListener(this);
        btnNo.setOnClickListener(this);
        new ShowPicture().execute(path);
//        photoView.setImageBitmap(getBitmap(path));

//        getBitmap(path);
//        Glide.with(this).load(path).into(photoView);
//        if (camera.equals("Front")) {
//            btnOk.setEnabled(false);
//            btnNo.setEnabled(false);
//            new ShowPicture().execute(path);
//        } else {
//////
//            photoView.setImageBitmap(getBitmap(path));
////            Glide.with(this).load(path).into(photoView);
//        }
//        Bitmap bitmap = BitmapFactory.decodeFile(path);

//        ShowPicture show = new ShowPicture(idCamera);
//        show.execute(bitmap);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnNo) {
            new File(path).delete();
            finish();
        } else if (view.getId() == R.id.btnOk) {
            if (camera.equals("Front")) {
                savePhoto(bitmapSum, path);
                pathImage = path;
            }
            pathImage = path;
            finish();
        }

    }

    class ShowPicture extends AsyncTask<String, Void, Bitmap> {
        private String idCamera;
        private SpotsDialog dialog;




        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bitmap = getBitmap(strings[0]);
            savePhoto(bitmap, path);
            return bitmap;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new SpotsDialog(ShowImageActivity.this, R.style.Wait);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            bitmapSum = bitmap;
            photoView.setImageBitmap(bitmap);
            btnNo.setEnabled(true);
            btnOk.setEnabled(true);
            dialog.cancel();

        }
    }

    public void savePhoto(Bitmap bmp, String path) {
        imageFileName = new File(path);
        FileOutputStream out;
        try {
            out = new FileOutputStream(imageFileName);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            scanPhoto(imageFileName.toString());
            out = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getRotationBitmap(int currentCameraId1) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(currentCameraId1, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;

            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public String fromInt(int val) {
        return String.valueOf(val);
    }

    public void scanPhoto(final String imageFileName) {
        msConn = new MediaScannerConnection(this, new MediaScannerConnection.MediaScannerConnectionClient() {
            public void onMediaScannerConnected() {
                msConn.scanFile(imageFileName, null);
            }

            public void onScanCompleted(String path, Uri uri) {
                msConn.disconnect();
            }
        });
        msConn.connect();
    }

    @Override
    public void onBackPressed() {
        pathImage = "";
        new File(path).delete();
        //        File f = new File(path);
//        File f2 = new File(imageFileName.getAbsolutePath());
//        if (f.exists()){
//            fileUtils.deleteImage(f);
//        }
//        if (f2.exists()){
//            fileUtils.deleteImage(f2);
//        }
////        fileUtils.deleteImage(new File(path));
////        fileUtils.deleteImage(new File(imageFileName.getAbsolutePath()));
        finish();
    }


    private Bitmap getBitmap(String path) {
        ExifInterface ei = null;
        Bitmap rotatedBitmap = null;
        Bitmap bitmapSum = BitmapFactory.decodeFile(path);
        try {
            ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            Log.e("TAG", "ei " + ei);
            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
//                    rotatedBitmap = rotateImage(bitmapSum, 180);

                    if (camera.equals("Front")) {
                        int angle = rotation - 90;
                        rotatedBitmap = rotateImage(bitmapSum, angle);
                    }else {
                        rotatedBitmap = rotateImage(bitmapSum, 90);
                    }
                    Log.e("TAG", "orientation 90 " + ExifInterface.ORIENTATION_ROTATE_90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmapSum, 180);
                    Log.e("TAG", "orientation 180 " + ExifInterface.ORIENTATION_ROTATE_180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmapSum, 270);
                    Log.e("TAG", "orientation 270 " + ExifInterface.ORIENTATION_ROTATE_270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                    Log.e("TAG", " orientation normal");
                    rotatedBitmap = bitmapSum;
                    break;
                default:
                    Log.e("TAG", " orientation not detect");
                    if (camera.equals("Front")){
                        int angle = -rotation - 180;
                        rotatedBitmap = rotateImage(bitmapSum, angle);
                    }else {
//                    rotatedBitmap = rotateImage(bitmapSum, -90);
                        rotatedBitmap = bitmapSum;
                    }
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }


        return rotatedBitmap;

    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.setRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public void deleteImage(File file) {
        // Set up the projection (we only need the ID)
        String[] projection = {MediaStore.Images.Media._ID};

        // Match on the file path
        String selection = MediaStore.Images.Media.DATA + " = ?";
        String[] selectionArgs = new String[]{file.getAbsolutePath()};

        // Query for the ID of the media matching the file path
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        if (c.moveToFirst()) {
            // We found the ID. Deleting the item via the content provider will also remove the file
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            contentResolver.delete(deleteUri, null, null);
        } else {
            // File not found in media store DB
        }
        c.close();
    }

}
