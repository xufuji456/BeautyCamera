package com.frank.camera.beauty.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.frank.beautyfilter.BeautyEngine;
import com.frank.beautyfilter.filter.helper.BeautyFilterType;
import com.frank.beautyfilter.util.BeautyParams;
import com.frank.beautyfilter.widget.BeautyCameraView;
import com.frank.camera.R;
import com.frank.camera.beauty.adapter.FilterAdapter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BeautyFilterActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView btnMode;
    private LinearLayout mFilterLayout;

    private BeautyEngine mBeautyEngine;
    private BeautyCameraView mCameraView;

    private boolean isRecording = false;

    private ObjectAnimator mObjectAnimator;

    private final static int MODE_PICTURE = 1;
    private final static int MODE_VIDEO   = 2;
    private int mBeautyMode = MODE_PICTURE;

    private final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private final static BeautyFilterType[] types = new BeautyFilterType[] {
            BeautyFilterType.NONE,
            BeautyFilterType.ANTIQUE,
            BeautyFilterType.BEAUTY,
            BeautyFilterType.BRIGHTNESS,
            BeautyFilterType.CONTRAST,
            BeautyFilterType.COOL,
            BeautyFilterType.CRAYON,
            BeautyFilterType.EXPOSURE,
            BeautyFilterType.FAIRYTALE,
            BeautyFilterType.HEALTHY,
            BeautyFilterType.HUDSON,
            BeautyFilterType.HUE,
            BeautyFilterType.INKWELL,
            BeautyFilterType.IMAGE_ADJUST,
            BeautyFilterType.PIXAR,
            BeautyFilterType.ROMANCE,
            BeautyFilterType.SATURATION,
            BeautyFilterType.SHARPEN,
            BeautyFilterType.SIERRA,
            BeautyFilterType.SKETCH,
            BeautyFilterType.SKINWHITEN,
            BeautyFilterType.SUNRISE,
            BeautyFilterType.TENDER,
            BeautyFilterType.WARM,
            BeautyFilterType.WHITECAT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_beauty_camera);
        mBeautyEngine = new BeautyEngine((BeautyCameraView)findViewById(R.id.glsurfaceview_camera));
        initView();
    }

    private void initView() {
        btnMode = findViewById(R.id.btn_camera_mode);
        mFilterLayout = findViewById(R.id.layout_filter);
        ImageView btnShutter = findViewById(R.id.btn_camera_shutter);
        RecyclerView mFilterListView = findViewById(R.id.filter_list_view);

        findViewById(R.id.btn_camera_mode).setOnClickListener(this);
        findViewById(R.id.btn_camera_filter).setOnClickListener(this);
        findViewById(R.id.btn_camera_filter).setOnClickListener(this);
        findViewById(R.id.btn_camera_beauty).setOnClickListener(this);
        findViewById(R.id.btn_camera_shutter).setOnClickListener(this);
        findViewById(R.id.btn_camera_switch).setOnClickListener(this);
        findViewById(R.id.btn_camera_closefilter).setOnClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mFilterListView.setLayoutManager(layoutManager);
        FilterAdapter mFilterAdapter = new FilterAdapter(this);
        mFilterAdapter.setFilterTypes(types);
        mFilterListView.setAdapter(mFilterAdapter);
        mFilterAdapter.setOnFilterChangeListener(mOnFilterChangeListener);

        mObjectAnimator = ObjectAnimator.ofFloat(btnShutter, "rotation", 0, 360);
        mObjectAnimator.setDuration(500);
        mObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        mCameraView = findViewById(R.id.glsurfaceview_camera);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mCameraView.getLayoutParams();
        params.width = size.x;
        params.height = size.x * 4 / 3;
        mCameraView.setLayoutParams(params);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_camera_beauty:
                new AlertDialog.Builder(this)
                        .setSingleChoiceItems(new String[] { "close", "1", "2", "3", "4", "5"}, BeautyParams.beautyLevel,
                                (dialog, which) -> {
                                    mBeautyEngine.setBeautyLevel(which);
                                    dialog.dismiss();
                                })
                        .setNegativeButton("cancel", null)
                        .show();
                break;
            case R.id.btn_camera_closefilter:
                hideFilter();
                break;
            case R.id.btn_camera_filter:
                showFilter();
                break;
            case R.id.btn_camera_mode:
                switchMode();
                break;
            case R.id.btn_camera_shutter:
                if (PermissionChecker.checkSelfPermission(this, permissions[0]) != PermissionChecker.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, view.getId());
                } else {
                    if(mBeautyMode == MODE_PICTURE) {
                        takePicture();
                    } else {
                        takeVideo();
                    }
                }
                break;
            case R.id.btn_camera_switch:
                mCameraView.switchCamera();
                break;
            default:
                break;
        }
    }

    private final FilterAdapter.OnFilterChangeListener mOnFilterChangeListener = new FilterAdapter.OnFilterChangeListener() {

        @Override
        public void onFilterChanged(BeautyFilterType filterType, int position) {
            mBeautyEngine.setFilter(filterType);
        }

    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mBeautyMode == MODE_PICTURE) {
                takePicture();
            } else {
                takeVideo();
            }
        }
    }

    private void switchMode() {
        if (mBeautyMode == MODE_PICTURE) {
            mBeautyMode = MODE_VIDEO;
            btnMode.setImageResource(R.drawable.ic_camera);
        } else {
            mBeautyMode = MODE_PICTURE;
            btnMode.setImageResource(R.drawable.ic_video);
        }
    }

    private void takePicture() {
        mBeautyEngine.savePicture(getOutputFile(), null);
    }

    private void takeVideo() {
        if (isRecording) {
            mObjectAnimator.end();
            mBeautyEngine.stopRecord();
        } else {
            mObjectAnimator.start();
            mBeautyEngine.startRecord();
        }
        isRecording = !isRecording;
    }

    private File getOutputFile() {
        String root;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            root = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            root = getCacheDir().getAbsolutePath();
        }
        File dir = new File(root, "BeautyCamera");
        if (!dir.exists()) {
            if (!dir.mkdir())
                return null;
        }
        String timestamp = new SimpleDateFormat("yyyy:MM:dd_HH:mm:ss", Locale.getDefault()).format(new Date());
        return new File(dir.getPath() + File.separator + timestamp + ".jpg");
    }

    private void showFilter() {
        mFilterLayout.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_camera_shutter).setClickable(false);
    }

    private void hideFilter() {
        mFilterLayout.setVisibility(View.INVISIBLE);
        findViewById(R.id.btn_camera_shutter).setClickable(true);
    }

}