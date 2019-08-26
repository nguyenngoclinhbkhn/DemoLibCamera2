package com.cpr.lib_camera2.activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.TonemapCurve;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cpr.lib_camera2.R;
import com.cpr.lib_camera2.customview.AutoFitTextureView;
import com.cpr.lib_camera2.customview.FocusView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView imgActionVolum;
    private ImageView imgActionTimer;
    private ImageView imgActionFlash;

    private SensorManager sensorManager;
    private Sensor sensor;

    private ImageView imgActionGallery;
    private ImageView imgActionCapture;
    private ImageView imgActionSwitchCamera;
    private AutoFitTextureView textureView;
    private boolean swapDimension = false;
    private File fileImage;
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private String camera = "Back";
    CameraCharacteristics characteristics;
    private float finger_spacing = 0;
    private int zoom_level = 1;

    private boolean isTouchFocus;

    private SeekBar seekBarWhite;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;


    private static final SparseArray ORIENTATIONS = new SparseArray();

    //    private AutoFitTextureView mTextureView;
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private int countTimer = 0;

    private static final int REQUESTE_CAMERA = 111;
    private static final int REQUEST_GALLERY = 222;
    private String CAMERA_FRONT = "1";
    private String CAMERA_BACK = "0";
    private Size largest;
    private String namePhoto;
    private Camera.CameraInfo cameraInfo;
    private Rect m = null;
    private float maxzoom;
    private boolean mManualFocusEngaged;
    private SeekBar seekBar;

    private boolean isTorchOn;
    private CameraCharacteristics charsBrightness;

    public static final int REQUEST_IMAGE = 333;

    private CameraCaptureSession captureSession;


    private MediaActionSound sound;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mState;

    private Rect sensorArraySize;
    private boolean areWeFocused = false;
    private boolean shouldCapture = false;
    private int timer = 0;
    private int countUp = 0;
    private int countDown = 0;
    private double timeKeyDown = 0;
    private double timeKeyUp = 0;
    private double keyActionCamera = 0;
    private boolean isVolumn = true;
    private boolean isFlash = false;

    private int totalOrientation = 0;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void process(CaptureResult result) {
            int afState = result.get(CaptureResult.CONTROL_AF_STATE);
            if (CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
                areWeFocused = true;

            } else {
                areWeFocused = false;

            }

            if (shouldCapture) {
                if (areWeFocused) {
                    shouldCapture = false;
                    captureStillPicture();
                }
            }
        }

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Toast.makeText(CameraActivity.this, "Lock fail", Toast.LENGTH_SHORT).show();
        }
    };

    private void captureStillPicture() {
//        final CaptureRequest.Builder captureBuilder;
//        try {
//            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(mImageReader.getSurface());
//            captureBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
//            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_HIGH_QUALITY);
//
//            // Use the same AE and AF modes as the preview.
//            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }

    }

    private CaptureRequest previewCaptureRequest;


    private FrameLayout frameLayout;
    private TextView txtTimer;
    private FocusView focusView;

    public static String Name_File_Photo = "CPRPhoto";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        imgActionVolum = findViewById(R.id.imgActionVolumn);
        imgActionTimer = findViewById(R.id.imgActionTimer);
        imgActionFlash = findViewById(R.id.imgActionFlash);
        imgActionGallery = findViewById(R.id.imgActionGalleryActivityCamera);
        imgActionSwitchCamera = findViewById(R.id.imgActionSwitchCamera);
        imgActionCapture = findViewById(R.id.imgActionCamera);
        seekBar = findViewById(R.id.seekbarZoom);
        seekBarWhite = findViewById(R.id.seekbarWhiteBlance);
        frameLayout = findViewById(R.id.frameTextureView);
        txtTimer = findViewById(R.id.txtTimer);


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        textureView = findViewById(R.id.texttureView);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        imgActionSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                switchCamera();

            }
        });


//        handler = new Handler();


        imgActionFlash.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                Boolean isFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                boolean isSupport = isFlash == null ? false : isFlash;
                try {
                    if (cameraID.equals(CAMERA_BACK)) {
                        if (isFlash) {
                            if (isTorchOn) {
                                imgActionFlash.setBackgroundResource(R.drawable.ic_flash_clicked_01);
                                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                                captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                                isTorchOn = false;
                            } else {
                                imgActionFlash.setBackgroundResource(R.drawable.ic_flash_unclicked_01);

                                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                                captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                                isTorchOn = true;
                            }
                        }
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });


        imgActionCapture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                imgActionGallery.setEnabled(false);
                imgActionFlash.setEnabled(false);
                imgActionVolum.setEnabled(false);
                imgActionSwitchCamera.setEnabled(false);
                keyActionCamera = System.currentTimeMillis();

                if (timer == 0) {
                    takePicture();
                } else {
                    imgActionTimer.setEnabled(false);
                    txtTimer.setVisibility(View.VISIBLE);
                    Timer timer1 = new Timer();
                    timer1.execute(timer);
                }
                imgActionCapture.setEnabled(false);
//                takePicture();

            }
        });

        seekBarWhite.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                int value = getBrightnessValue() + progress;
//                setBrightness(value);

                try {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, progress * 20 / 100);
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    Log.e("TAG", "value " + progress);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        focusView = new FocusView(CameraActivity.this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                zoom_level = progress / 2;
                int minW = (int) (m.width() / maxzoom);
                int minH = (int) (m.height() / maxzoom);
                int difW = m.width() - minW;
                int difH = m.height() - minH;
                int cropW = difW / 100 * (int) zoom_level;
                int cropH = difH / 100 * (int) zoom_level;
                cropW -= cropW & 3;
                cropH -= cropH & 3;
                Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);


                try {
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        imgActionVolum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isVolumn = !isVolumn;
                if (isVolumn == false) {
                    imgActionVolum.setBackgroundResource(R.drawable.ic_volum_camera_clicked_01);
                } else {
                    imgActionVolum.setBackgroundResource(R.drawable.ic_volum_camera_unclicked_01);
                }
            }
        });
        imgActionGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CameraActivity.this, GalleryActivity.class);
                intent.putExtra("NameFolder", Name_File_Photo);
                startActivity(intent);
            }
        });

        imgActionTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countTimer++;
                if (timer == 0 && countTimer == 1) {
                    timer = 3;
                    txtTimer.setVisibility(View.VISIBLE);
                    txtTimer.setText(String.valueOf(timer));
                    imgActionTimer.setBackgroundResource(R.drawable.ic_time_camera_clicked_3s_01);
                }
                if (timer == 3 && countTimer == 2) {
                    timer = 5;

                    txtTimer.setVisibility(View.VISIBLE);
                    txtTimer.setText(String.valueOf(timer));
                    imgActionTimer.setBackgroundResource(R.drawable.ic_time_camera_clicked_5s_01);
                }
                if (timer == 5 && countTimer == 3) {
                    timer = 10;
                    txtTimer.setVisibility(View.VISIBLE);
                    txtTimer.setText(String.valueOf(timer));
                    imgActionTimer.setBackgroundResource(R.drawable.ic_time_camera_clicked_10s_01);
                }
                if (timer == 10 && countTimer == 4) {
                    timer = 0;
                    countTimer = 0;
                    txtTimer.setVisibility(View.GONE);
                    imgActionTimer.setBackgroundResource(R.drawable.ic_time_camera_unclicked_1);
                }

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Range<Integer> getRange() {
        Range<Integer>[] ranges = charsBrightness.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Range<Integer> result = null;
        for (Range<Integer> range : ranges) {
            int upper = range.getUpper();
            // 10 - min range upper for my needs
            if (upper >= 10) {
                if (result == null || upper < result.getUpper().intValue()) {
                    result = range;
                }
            }
        }
        if (result == null) {
            result = ranges[0];
        }
        return result;
    }


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            focusView.setColor(Color.WHITE);
            SystemClock.sleep(1000);
            if (focusView.getParent() == frameLayout) {
                frameLayout.removeView(focusView);
            }
        }
    };
    private Handler handler;


    //Determine the space between the first two fingers
    @SuppressWarnings("deprecation")
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        View view = getWindow().getDecorView();
//        if (hasFocus) {
//            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        }
//    }

    private CaptureRequest.Builder captureRequestBuilder;
    private CameraDevice cameraDevice;
    private String cameraID;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Size previewSize;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            startPreview();

        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            cameraDevice = null;

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void lockFocus() {
        try {
            mState = STATE_WAIT_LOCK;
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            captureSession.capture(captureRequestBuilder.build(),
                    captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void unLockFocus() {
        try {
            mState = STATE_WAIT_LOCK;
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

            captureSession.capture(captureRequestBuilder.build(),
                    captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean isMeteringAreaAFSupported = false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupCamera(int with, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                Integer maxAFRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
                if (maxAFRegions != null) {
                    isMeteringAreaAFSupported = maxAFRegions >= 1;
                }

                m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                maxzoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
                Range<Integer> range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                maxCompensationRange = range.getUpper();
                minCompensationRange = range.getLower();


//                captureSession = cameraDevice.createca
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
//                int totalRotation = sensorDeviceRotation(characteristics, deviceOrientation);
                int totalRotation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                switch (deviceOrientation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (totalRotation == 90 || totalRotation == 270) {
                            swapDimension = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (totalRotation == 0 || totalRotation == 180) {
                            swapDimension = true;
                        }
                        break;
                    default:
                }
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = with;
                int rotatedHeight = height;
                Point displaySize = new Point();
                int maxPreviewWidth = getSize().widthPixels;
                int maxPreviewHeight = getSize().heightPixels;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = with;
                    maxPreviewWidth = getSize().heightPixels;
                    maxPreviewHeight = getSize().widthPixels;
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        getSize().widthPixels, getSize().heightPixels, maxPreviewWidth, maxPreviewHeight, largest);
                cameraID = cameraId;
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(
                            previewSize.getWidth() * getSize().heightPixels / getSize().widthPixels,
                            getSize().heightPixels);
                } else {
                    textureView.setAspectRatio(
                            previewSize.getHeight(), previewSize.getWidth());
                }

                totalOrientation = sensorDeviceRotation(cameraManager.getCameraCharacteristics(cameraManager.getCameraIdList()[1])
                        , deviceOrientation);
                Log.e("TAG", "orientation " + orientation);
                Log.e("TAG", "swapRotation " + swapRotation);
                Log.e("TAG", "Device orientation " + deviceOrientation);
                Log.e("TAG", "total rotation " + totalRotation);

                Log.e("TAG", "sensor device " +
                        sensorDeviceRotation(cameraManager.getCameraCharacteristics(cameraManager.getCameraIdList()[1])
                        , deviceOrientation));
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    class Timer extends AsyncTask<Integer, Integer, Integer> {
        private MediaPlayer mediaPlayer;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mediaPlayer = MediaPlayer.create(CameraActivity.this, R.raw.timer);
            mediaPlayer.start();
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            int integer1 = integers[0];
            int result = 0;
            for (int i = integer1; i >= 0; i--) {
                publishProgress(i);
                SystemClock.sleep(1000);
                if (i == 0) {
                    result = i;
                    break;
                }
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int value = values[0];
            txtTimer.setText(String.valueOf(value));
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if (integer == 0) {
                takePicture();
                mediaPlayer.pause();
                txtTimer.setVisibility(View.GONE);
                imgActionTimer.setEnabled(true);
                imgActionTimer.setBackgroundResource(R.drawable.ic_time_camera_unclicked_1);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static Size chooseOptimalSize2(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        double ratio = (double) h / w;
        for (Size option : choices) {
            double optionRatio = (double) option.getHeight() / option.getWidth();
            if (ratio == optionRatio) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[1];
        }
    }

    private DisplayMetrics getSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(cameraID, cameraStateCallback, backgroundHandler);

                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "App required acess to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUESTE_CAMERA);
                }
            } else {
                cameraManager.openCamera(cameraID, cameraStateCallback, backgroundHandler);

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void switchCamera() {
        if (cameraID.equals(CAMERA_FRONT)) {
            cameraID = CAMERA_BACK;
            camera = "Back";
            closeCamera();
            reopenCamera();
        } else if (cameraID.equals(CAMERA_BACK)) {
            cameraID = CAMERA_FRONT;
            closeCamera();
            reopenCamera();
            camera = "Front";
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void reopenCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (textureView.isAvailable()) {
//            setupCamera(textureView.getWidth(), textureView.getHeight());
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUESTE_CAMERA) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Can't use camera", Toast.LENGTH_SHORT).show();
            } else {

            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.addTarget(previewSurface);
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }
                            try {

                                previewCaptureRequest = captureRequestBuilder.build();
//                                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());
                                session.setRepeatingRequest(captureRequestBuilder.build(),
                                        captureCallback, backgroundHandler);
                                captureSession = session;
                                previewCaptureRequest = captureRequestBuilder.build();
                                captureSession.setRepeatingRequest(previewCaptureRequest, captureCallback, backgroundHandler);
//                                textureView.setOnTouchListener(new CameraFocusOnTouchHandler
//                                        (characteristics, captureRequestBuilder, session, backgroundHandler));
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_AUTO);
                                textureView.setOnTouchListener(new CameraFocusOnTouchHandler(characteristics,
                                        captureRequestBuilder, captureSession, backgroundHandler));

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(CameraActivity.this, "preview failed", Toast.LENGTH_SHORT).show();
                        }
                    }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CPRCamera");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static int sensorDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceRotation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceRotation = (int) ORIENTATIONS.get(deviceRotation);
        return (sensorOrientation + deviceRotation + 360) % 360;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getWidth() * o1.getHeight() /
                    (long) o2.getWidth() * o2.getHeight());
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static Size chooseOptimalSize1(Size[] choices, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                sizeList.add(option);
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBackgroundThread();
        closeCamera();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void takePicture() {
        if (null == cameraDevice) {
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }

            // CAPTURE IMAGE với tuỳ chỉnh kích thước
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_HIGH_QUALITY);

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // kiểm tra orientation tuỳ thuộc vào mỗi device khác nhau như có nói bên trên
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, (Integer) ORIENTATIONS.get(rotation));
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
            File imageFileFolder = new File(Environment.getExternalStorageDirectory(), Name_File_Photo);
            if (!imageFileFolder.exists()) {
                imageFileFolder.mkdir();
            }
            namePhoto = String.valueOf(System.currentTimeMillis() + ".jpg");

            fileImage = new File(imageFileFolder, namePhoto);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                // Lưu ảnh
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(fileImage);
                        output.write(bytes);
                        Intent intent = new Intent(CameraActivity.this, ShowImageActivity.class);
                        intent.putExtra("path", fileImage.getAbsolutePath());
                        Log.e("TAG", "path " + fileImage.getAbsolutePath());
                        intent.putExtra("camera", camera);
                        intent.putExtra("rotation", String.valueOf(totalOrientation));
                        Log.e("TAG", "rotation " + totalOrientation);
                        startActivity(intent);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, backgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

//                    Toast.makeText(CameraActivity.this, "Saved:" + fileImage, Toast.LENGTH_SHORT).show();
//                    startPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        if (isVolumn){
                            sound = new MediaActionSound();
                            sound.play(MediaActionSound.SHUTTER_CLICK);
                        }else{
                            sound.release();
                        }
                        session.capture(captureBuilder.build(), captureListener, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onResume() {
        super.onResume();
        if (ShowImageActivity.pathImage.length() > 0) {
            Intent intent1 = new Intent();
            intent1.setData(Uri.parse(ShowImageActivity.pathImage));
            ShowImageActivity.pathImage = "";
            setResult(RESULT_OK, intent1);
            finish();
        } else {
            reopenCamera();
            imgActionCapture.setEnabled(true);
            imgActionSwitchCamera.setEnabled(true);
            imgActionGallery.setEnabled(true);
            imgActionFlash.setEnabled(true);
            imgActionTimer.setEnabled(true);
            imgActionVolum.setEnabled(true);
            countDown = 0;
            countUp = 0;
            timeKeyDown = 0;
            timeKeyUp = 0;
            keyActionCamera = 0;
            countTimer = 0;
            timer = 0;
            Intent intent12 = getIntent();
            if (intent12.getStringExtra("NameFolder") != null) {
                String path = intent12.getStringExtra("NameFolder");
                if (path.length() > 0) {
                    Name_File_Photo = path;
                } else {
                    Name_File_Photo = "CPRPhoto";
                }
            } else {
                Name_File_Photo = "CPRPhoto";
            }
//            startBackgroundThread();
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
//            if (textureView.isAvailable()) {
//                setupCamera(textureView.getWidth(), textureView.getHeight());
//                connectCamera();
//            } else {
//                textureView.setSurfaceTextureListener(surfaceTextureListener);
//            }
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onSensorChanged(SensorEvent event) {
        float lux = event.values[0];
        if (Math.abs(lux) >= 1.5 && isTouchFocus == true) {

            try {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                captureSession.capture(captureRequestBuilder.build(), captureCallback,
//                        backgroundHandler);

                mState = STATE_PREVIEW;
                captureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback,
                        backgroundHandler);
                isTouchFocus = false;
//                focusView.setX(frameLayout.getWidth() / 2 - focusView.getWidth() / 2);
//                focusView.setY(frameLayout.getHeight() / 2 - focusView.getHeight() / 2);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("TAG", "ac move");
    }

    @Override
    protected void onPause() {
//        closeCamera();
//        stopBackgroundThread();
        super.onPause();
        sensorManager.unregisterListener(this);

    }


    private int maxCompensationRange;
    private int minCompensationRange;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int getBrightnessValue() {
        int absBRange = maxCompensationRange - minCompensationRange;
        int value = captureRequestBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
        return 100 * (value - minCompensationRange) / absBRange;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setBrightness(int value) {
        int brightness = (int) (minCompensationRange + (maxCompensationRange - minCompensationRange) * (value / 100f));
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, brightness);
        try {
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                timeKeyUp = System.currentTimeMillis();
                countUp++;
                if (timeKeyDown == 0 && keyActionCamera == 0) {
                    if ((action == MotionEvent.ACTION_DOWN && event.getRepeatCount() == 0 && countUp == 1)) {
                        imgActionGallery.setEnabled(false);
                        imgActionSwitchCamera.setEnabled(false);
                        imgActionFlash.setEnabled(false);
                        imgActionTimer.setEnabled(false);
                        imgActionVolum.setEnabled(false);
                        imgActionCapture.setEnabled(false);
                        if (timer == 0) {
                            takePicture();
                        } else {
                            imgActionTimer.setEnabled(false);
                            txtTimer.setVisibility(View.VISIBLE);
                            Timer timer1 = new Timer();
                            timer1.execute(timer);
                        }
                    } else {
                    }
                }

            }
            break;
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                timeKeyDown = System.currentTimeMillis();
                countUp = 0;
                countDown++;
                if (timeKeyUp == 0 && keyActionCamera == 0) {
                    if (action == MotionEvent.ACTION_DOWN && event.getRepeatCount() == 0 && countDown == 1) {
                        imgActionGallery.setEnabled(false);
                        imgActionSwitchCamera.setEnabled(false);
                        imgActionFlash.setEnabled(false);
                        imgActionTimer.setEnabled(false);
                        imgActionVolum.setEnabled(false);
                        imgActionCapture.setEnabled(false);

                        if (timer == 0) {
                            takePicture();
                        } else {
                            imgActionTimer.setEnabled(false);
                            txtTimer.setVisibility(View.VISIBLE);
                            Timer timer1 = new Timer();
                            timer1.execute(timer);
                        }
                    }
                }
            }
            break;
            default:
                return super.dispatchKeyEvent(event);
        }
        return true;


    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setContrast(int value) {
        final int minContrast = 0;
        final int maxContrast = 1;
        float[][] channels = null;
        TonemapCurve tc = captureRequestBuilder.get(CaptureRequest.TONEMAP_CURVE);
        if (tc != null) {
            channels = new float[3][];
            for (int chanel = TonemapCurve.CHANNEL_RED; chanel <= TonemapCurve.CHANNEL_BLUE; chanel++) {
                float[] array = new float[tc.getPointCount(chanel) * 2];
                tc.copyColorCurve(chanel, array, 0);
                channels[chanel] = array;
            }
        }
        if (channels == null || value > 100 || value < 0) {
            return;
        }

        float contrast = minContrast + (maxContrast - minContrast) * (value / 100f);

        float[][] newValues = new float[3][];
        for (int chanel = TonemapCurve.CHANNEL_RED; chanel <= TonemapCurve.CHANNEL_BLUE; chanel++) {
            float[] array = new float[channels[chanel].length];
            System.arraycopy(channels[chanel], 0, array, 0, array.length);
            for (int i = 0; i < array.length; i++) {
                array[i] *= contrast;
            }
            newValues[chanel] = array;
        }
        TonemapCurve tc1 = new TonemapCurve(newValues[TonemapCurve.CHANNEL_RED], newValues[TonemapCurve.CHANNEL_GREEN], newValues[TonemapCurve.CHANNEL_BLUE]);
        captureRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
        captureRequestBuilder.set(CaptureRequest.TONEMAP_CURVE, tc1);
        try {
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
            float maxzoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int action = event.getAction();
            float current_finger_spacing;

            if (event.getPointerCount() > 1) {

//            ZOOM

                // Multi touch logic
//                current_finger_spacing = getFingerSpacing(event);
//                if (finger_spacing != 0) {
//                    if (current_finger_spacing > finger_spacing && maxzoom > zoom_level) {
//                        zoom_level++;
//                    } else if (current_finger_spacing < finger_spacing && zoom_level > 1) {
//                        zoom_level--;
//                    }
//                    int minW = (int) (m.width() / maxzoom);
//                    int minH = (int) (m.height() / maxzoom);
//                    int difW = m.width() - minW;
//                    int difH = m.height() - minH;
//                    int cropW = difW / 100 * (int) zoom_level;
//                    int cropH = difH / 100 * (int) zoom_level;
//                    cropW -= cropW & 3;
//                    cropH -= cropH & 3;
//                    Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
//                    captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
//                }
//                finger_spacing = current_finger_spacing;
//                try {
//                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                }
//                Log.e("TAG", "zoom");
            } else {
                if (action == MotionEvent.ACTION_DOWN) {
//                    handleFocus(event);
//                    float x = event.getX();
//                    float y = event.getRawY();
//                    if (focusView.getParent() == frameLayout) {
//                        frameLayout.removeView(focusView);
//                    }
//                    int width = frameLayout.getWidth() / 4;
//                    int height = frameLayout.getHeight() / 4;
//                    frameLayout.addView(focusView, width, width);
//                    focusView.setColor(Color.GREEN);
////                    focusView.setX(x - 0);
////                    focusView.setY(y - 0);
//
//                    Log.e("TAG", "x " + focusView.getX());
//                    Log.e("TAG", "y " + focusView.getY());
//                    focusView.setX(x - width / 2);
//                    focusView.setY(y - width - width / 2);
                    //single touch logic
                } else if (action == MotionEvent.ACTION_UP) {
//                    SystemClock.sleep(2000);
//                    if (focusView.getParent() == frameLayout){
//                        frameLayout.removeView(focusView);
//                    }
                }
            }


//            try {
//                mCaptureSession
//                        .setRepeatingRequest(captureRequestBuilder.build(), mCaptureCallback, null);
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            } catch (NullPointerException ex) {
//                ex.printStackTrace();
//            }
        } catch (CameraAccessException e) {
            throw new RuntimeException("can not access camera.", e);
        }
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void handleFocus(MotionEvent event) {
//        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//        Log.i("onAreaTouchEvent", "SENSOR_INFO_ACTIVE_ARRAY_SIZE,,,,,,,,rect.left--->" + rect.left + ",,,rect.top--->" + rect.top + ",,,,rect.right--->" + rect.right + ",,,,rect.bottom---->" + rect.bottom);
//        Size size = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
//        Log.i("onAreaTouchEvent", "mCameraCharacteristics,,,,size.getWidth()--->" + size.getWidth() + ",,,size.getHeight()--->" + size.getHeight());
        int areaSize = 200;
        int right = sensorArraySize.right;
        int bottom = sensorArraySize.bottom;
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        int ll, rr;
        Rect newRect;
        int centerX = (int) event.getX();
        int centerY = (int) event.getY();
        ll = ((centerX * right) - areaSize) / viewWidth;
        rr = ((centerY * bottom) - areaSize) / viewHeight;
        int focusLeft = clamp(ll, 0, right);
        int focusBottom = clamp(rr, 0, bottom);
        Log.i("focus_position", "focusLeft--->" + focusLeft + ",,,focusTop--->" + focusBottom + ",,,focusRight--->" + (focusLeft + areaSize) + ",,,focusBottom--->" + (focusBottom + areaSize));
        newRect = new Rect(focusLeft, focusBottom, focusLeft + areaSize, focusBottom + areaSize);
        MeteringRectangle meteringRectangle = new MeteringRectangle(newRect, 500);
        MeteringRectangle[] meteringRectangleArr = {meteringRectangle};
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangleArr);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        try {
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("updatePreview", "ExceptionExceptionException");
        }
    }

    private int clamp(int x, int min, int max) {
        if (x < min) {
            return min;
        } else if (x > max) {
            return max;
        } else {
            return x;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public class CameraFocusOnTouchHandler implements View.OnTouchListener {

        private static final String TAG = "FocusOnTouchHandler";

        private CameraCharacteristics mCameraCharacteristics;
        private CaptureRequest.Builder mPreviewRequestBuilder;
        private CameraCaptureSession mCaptureSession;
        private Handler mBackgroundHandler;

        private boolean mManualFocusEngaged = false;

        public CameraFocusOnTouchHandler(
                CameraCharacteristics cameraCharacteristics,
                CaptureRequest.Builder previewRequestBuilder,
                CameraCaptureSession captureSession,
                Handler backgroundHandler
        ) {
            mCameraCharacteristics = cameraCharacteristics;
            mPreviewRequestBuilder = previewRequestBuilder;
            mCaptureSession = captureSession;
            mBackgroundHandler = backgroundHandler;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            //Override in your touch-enabled view (this can be different than the view you use for displaying the cam preview)
            isTouchFocus = true;
            final int actionMasked = motionEvent.getActionMasked();
            if (actionMasked != MotionEvent.ACTION_DOWN) {

                return false;
            }
            if (mManualFocusEngaged) {
                Log.d(TAG, "Manual focus already engaged");
                return true;
            }


            //ok
//            if (focusView.getParent() == frameLayout) {
//                frameLayout.removeView(focusView);
//            }
//            int width = frameLayout.getWidth() / 4;
//            int height = frameLayout.getHeight() / 4;
//            frameLayout.addView(focusView, width, width);
//            focusView.setColor(Color.GREEN);
////                    focusView.setX(x - 0);
////                    focusView.setY(y - 0);
//
//            Log.e("TAG", "x " + focusView.getX());
//            Log.e("TAG", "y " + focusView.getY());
//            focusView.setX(motionEvent.getX() - width / 2);
//            focusView.setY(motionEvent.getRawY() - width - width / 2);
//            focusView.setColor(Color.GREEN);
////                    focusView.setX(x - 0);
////                    focusView.setY(y - 0);
//
//            Log.e("TAG", "x " + focusView.getX());
//            Log.e("TAG", "y " + focusView.getY());
//            focusView.setX(motionEvent.getX() - width / 2);
//            focusView.setY(motionEvent.getRawY() - width - width / 2);
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    focusView.setVisibility(View.GONE);
//                }
//            }, 3000);
            final Rect sensorArraySize = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            //TODO: here I just flip x,y, but this needs to correspond with the sensor orientation (via SENSOR_ORIENTATION)
            final int y = (int) ((motionEvent.getX() / (float) view.getWidth()) * (float) sensorArraySize.height());
            final int x = (int) ((motionEvent.getY() / (float) view.getHeight()) * (float) sensorArraySize.width());
            final int halfTouchWidth = 50; //(int)motionEvent.getTouchMajor(); //TODO: this doesn't represent actual touch size in pixel. Values range in [3, 10]...
            final int halfTouchHeight = 50; //(int)motionEvent.getTouchMinor();
            MeteringRectangle focusAreaTouch = new MeteringRectangle(Math.max(x - halfTouchWidth, 0),
                    Math.max(y - halfTouchHeight, 0),
                    halfTouchWidth * 2,
                    halfTouchHeight * 2,
                    MeteringRectangle.METERING_WEIGHT_MAX - 1);

            CameraCaptureSession.CaptureCallback captureCallbackHandler = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    mManualFocusEngaged = false;

                    if (request.getTag() == "FOCUS_TAG") {
                        //the focus trigger is complete - resume repeating (preview surface will get frames), clear AF trigger
//                        captureRequestBuilder.remo
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
//                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                        try {
                            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Log.e(TAG, "Manual AF failure: " + failure);
                    mManualFocusEngaged = false;
                }
            };

            //first stop the existing repeating request
            try {
                mCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            //cancel any existing AF trigger (repeated touches, etc.) test
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            try {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), captureCallbackHandler, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            //Now add a new AF trigger with focus region
            if (isMeteringAreaAFSupported()) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
            }
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mPreviewRequestBuilder.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

            //then we ask for a single request (not repeating!)
            try {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), captureCallbackHandler, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mManualFocusEngaged = true;

            return true;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private boolean isMeteringAreaAFSupported() {
            Integer value = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
            if (value != null) {
                return value >= 1;
            } else {
                return false;
            }
        }

    }


}

